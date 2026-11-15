"""Print the exact method signatures of framework classes as they exist on THIS device (MTK/Duoqin may add
methods), read straight from the dex files in a jar. Used to write compileOnly stubs for FutureSystemUI.
Usage: python3 dex_contract.py <framework.jar> <class descriptor prefix> [...]
  e.g. python3 dex_contract.py framework.jar Lcom/android/internal/statusbar/IStatusBar;"""
import sys, struct, zipfile

def uleb(b, o):
    r = s = 0
    while True:
        x = b[o]; o += 1; r |= (x & 0x7f) << s; s += 7
        if x < 0x80: return r, o

def parse(b):
    u = lambda o: struct.unpack_from("<I", b, o)[0]
    sids_n, sids_o, tids_n, tids_o, pids_n, pids_o = u(56), u(60), u(64), u(68), u(72), u(76)
    mids_n, mids_o, cdef_n, cdef_o = u(88), u(92), u(96), u(100)
    def string(i):
        o = u(sids_o + 4 * i); _, o = uleb(b, o); e = b.index(0, o)
        return b[o:e].decode("utf-8", "replace")
    def typ(i): return string(u(tids_o + 4 * i))
    def proto(i):
        o = pids_o + 12 * i; ret = typ(u(o + 4)); po = u(o + 8); args = []
        if po:
            n = u(po); args = [typ(struct.unpack_from("<H", b, po + 4 + 2 * k)[0]) for k in range(n)]
        return ret, args
    methods = []
    for i in range(mids_n):
        c, p, n = struct.unpack_from("<HHI", b, mids_o + 8 * i)
        methods.append((typ(c), string(n), proto(p)))
    classes = {}
    for i in range(cdef_n):
        o = cdef_o + 32 * i
        cls, flags, sup = typ(u(o)), u(o + 4), u(o + 8)
        classes[cls] = (flags, typ(sup) if sup != 0xFFFFFFFF else None)
    return methods, classes

jar, prefixes = sys.argv[1], sys.argv[2:]
with zipfile.ZipFile(jar) as z:
    for name in sorted(n for n in z.namelist() if n.endswith(".dex")):
        methods, classes = parse(z.read(name))
        for pre in prefixes:
            for cls in sorted(c for c in classes if c == pre or c.startswith(pre.rstrip(";") + "$")):
                flags, sup = classes[cls]
                print(f"## {cls} flags=0x{flags:x} super={sup}  ({name})")
                seen = set()
                for c, n, (ret, args) in methods:
                    if c == cls and (n, ret, tuple(args)) not in seen:
                        seen.add((n, ret, tuple(args)))
                        print(f"   {n}({', '.join(args)}) -> {ret}")
