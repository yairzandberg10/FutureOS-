"""List native libs per APK and whether they are stored (uncompressed) or deflated.
Usage: python inspect_apks.py <ready-apks dir>"""
import sys, zipfile, pathlib, collections

d = pathlib.Path(sys.argv[1])
for apk in sorted(d.glob("*.apk")):
    z = zipfile.ZipFile(apk)
    abis = collections.Counter()
    deflated = 0
    for i in z.infolist():
        if i.filename.startswith("lib/") and i.filename.endswith(".so"):
            abis[i.filename.split("/")[1]] += 1
            if i.compress_type != zipfile.ZIP_STORED:
                deflated += 1
    print(f"{apk.name:36} libs={dict(abis)} deflated={deflated}")
