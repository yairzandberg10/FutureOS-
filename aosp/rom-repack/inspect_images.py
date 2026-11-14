"""Offline inspection of dumped images (no phone):
  - vbmeta*: flags (bit0 = hashtree/verity disabled, bit1 = verification disabled), rollback index
  - boot / vendor_boot: header version, ramdisk compression, whether 'fastbootd' is in a ramdisk
Usage: python3 inspect_images.py <images dir>"""
import sys, struct, pathlib, gzip, subprocess

d = pathlib.Path(sys.argv[1])

for n in ("vbmeta_a", "vbmeta_system_a", "vbmeta_vendor_a"):
    b = (d / f"{n}.img").read_bytes()
    if b[:4] != b"AVB0":
        print(f"{n}: no AVB0 magic"); continue
    rollback = struct.unpack(">Q", b[112:120])[0]
    flags = struct.unpack(">I", b[120:124])[0]
    alg = struct.unpack(">I", b[28:32])[0]
    print(f"{n}: flags={flags} (verity_off={bool(flags & 1)} verification_off={bool(flags & 2)}) "
          f"rollback={rollback} algorithm={alg}")

def decompress(blob):
    if blob[:2] == b"\x1f\x8b":
        return "gzip", gzip.decompress(blob)
    if blob[:4] in (b"\x02\x21\x4c\x18", b"\x04\x22\x4d\x18"):
        p = subprocess.run(["lz4", "-dc"], input=blob, capture_output=True)
        return "lz4", p.stdout
    return "raw?", blob

def page(x, ps):
    return (x + ps - 1) // ps * ps

b = (d / "boot_a.img").read_bytes()
assert b[:8] == b"ANDROID!", "boot: bad magic"
hv = struct.unpack("<I", b[40:44])[0]
if hv >= 3:
    kernel_size, ramdisk_size = struct.unpack("<II", b[8:16])
    ps = 4096
    rd = b[page(4096, ps) + page(kernel_size, ps):][:ramdisk_size]
else:
    kernel_size, _, ramdisk_size = struct.unpack("<III", b[8:20])
    ps = struct.unpack("<I", b[36:40])[0]
    rd = b[ps + page(kernel_size, ps):][:ramdisk_size]
kind, raw = decompress(rd)
print(f"boot_a: header v{hv} kernel={kernel_size} ramdisk={ramdisk_size} ({kind}) "
      f"fastbootd_in_ramdisk={b'fastbootd' in raw} magisk={b'magisk' in raw.lower()}")

vb = (d / "vendor_boot_a.img").read_bytes()
if vb[:8] == b"VNDRBOOT":
    vhv, ps = struct.unpack("<II", vb[8:16])
    vrd_size = struct.unpack("<I", vb[24:28])[0]
    hdr_size = struct.unpack("<I", vb[2112 - 4 + 4:2112 + 4])[0] if False else None
    # vendor_boot v3/v4 header is 2112 (v3) / 2128 (v4) bytes, ramdisk starts at page(header)
    hsz = 2128 if vhv >= 4 else 2112
    vrd = vb[page(hsz, ps):][:vrd_size]
    kind, raw = decompress(vrd)
    print(f"vendor_boot_a: v{vhv} ramdisk={vrd_size} ({kind}) fastbootd_in_ramdisk={b'fastbootd' in raw}")
