#!/bin/bash
# FutureOS ROM repack - step 1: READ-ONLY dump of the phone (root dd).
# Nothing is written to any partition. Output goes to $OUT (default D:\futureos-rom\backup),
# never to OneDrive: nvram/nvdata/proinfo hold the IMEI.
#
# Method: dd -> /data/local/tmp on the phone, sha256 of that copy, adb pull, sha256 on PC.
# (adb exec-out through su corrupted binaries in testing, so it is not used.)
# nvdata/nvcfg change while the phone runs, so we hash the phone-side copy, not the live block.
export MSYS_NO_PATHCONV=1
ADB="${ADB:-/c/Users/yairz/AppData/Local/Android/Sdk/platform-tools/adb.exe}"
OUT="${OUT:-/d/futureos-rom/backup}"
T=/data/local/tmp/fosdump
mkdir -p "$OUT/critical" "$OUT/images" "$OUT/apps"
"$ADB" shell "su -c 'rm -rf $T; mkdir -p $T'" </dev/null
fail=0
dump() { local n=$1 dev=$2 dst=$3
  "$ADB" shell "su -c 'dd if=$dev of=$T/$n.img bs=4M 2>/dev/null; chmod 644 $T/$n.img'" </dev/null
  local ph; ph=$("$ADB" shell "su -c 'sha256sum $T/$n.img'" </dev/null | awk '{print $1}')
  rm -f "$dst/$n.img"
  "$ADB" pull "$T/$n.img" "$(cygpath -w "$dst/$n.img")" >/dev/null 2>&1 </dev/null
  "$ADB" shell "su -c 'rm -f $T/$n.img'" </dev/null
  local pc; pc=$(sha256sum "$dst/$n.img" 2>/dev/null | awk '{print $1}')
  if [ -n "$pc" ] && [ "$ph" = "$pc" ]; then
    echo "OK   $n $(stat -c %s "$dst/$n.img")"; echo "$pc  $n.img" >> "$dst/SHA256SUMS"
  else echo "FAIL $n phone=$ph pc=$pc"; fail=1; fi
}
if [ "${APPS_ONLY:-0}" != 1 ]; then   # APPS_ONLY=1: only re-pull the FutureOS APKs
: > "$OUT/critical/SHA256SUMS"; : > "$OUT/images/SHA256SUMS"
for p in nvram nvdata nvcfg proinfo protect1 protect2 seccfg md1img_a lk_a; do dump $p /dev/block/by-name/$p "$OUT/critical"; done
dump preloader_a /dev/block/mmcblk0boot0 "$OUT/critical"
for p in boot_a vendor_boot_a dtbo_a vbmeta_a vbmeta_system_a vbmeta_vendor_a; do dump $p /dev/block/by-name/$p "$OUT/images"; done
for p in product_a vendor_a system_a; do dump $p /dev/block/mapper/$p "$OUT/images"; done
"$ADB" shell "su -c 'lpdump'" </dev/null | tr -d '\r' > "$OUT/images/lpdump.txt"
"$ADB" shell "getprop" </dev/null | tr -d '\r' > "$OUT/images/getprop.txt"
fi
# privileged/signature permission levels, for the privapp allowlist
"$ADB" shell "pm list permissions -f" </dev/null | tr -d '\r' > "$OUT/permissions.txt"
rm -rf "$OUT/apps"; mkdir -p "$OUT/apps"

# FutureOS apps exactly as installed and working on the phone now (base + splits)
"$ADB" shell "pm list packages -f" </dev/null | tr -d '\r' | grep -E '=com\.future\.|=com\.android\.sistemui$' > "$OUT/apps/packages.txt"
while IFS= read -r line; do
  p=${line#package:}; pkg=${p##*=}; dir=$(dirname "${p%=*}")
  mkdir -p "$OUT/apps/$pkg"
  for f in $("$ADB" shell "ls $dir/*.apk" </dev/null | tr -d '\r'); do
    dst="$OUT/apps/$pkg/$(basename $f)"
    "$ADB" pull "$f" "$(cygpath -w "$dst")" >/dev/null 2>&1 </dev/null
    ph=$("$ADB" shell "sha256sum $f" </dev/null | awk '{print $1}')
    [ "$ph" = "$(sha256sum "$dst" 2>/dev/null | awk '{print $1}')" ] || { echo "FAIL apk $pkg $f"; fail=1; }
  done
  echo "APK  $pkg $(ls "$OUT/apps/$pkg" | tr '\n' ' ')"
done < "$OUT/apps/packages.txt"
"$ADB" shell "su -c 'rm -rf $T'" </dev/null
[ $fail = 0 ] && echo "DONE all verified" || echo "DONE WITH FAILURES"
