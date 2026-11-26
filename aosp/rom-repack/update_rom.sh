#!/bin/bash
# FutureOS - rebuild the flashable system_a from the NEWEST build of every app (Git Bash on this PC).
# Run after ANY app update:   bash aosp/rom-repack/update_rom.sh
#
# Source of each app, newest wins (by file time):
#   1. the copy pulled from the phone   D:\futureos-rom\backup\apps\<pkg>\*.apk   (01_dump.sh APPS_ONLY=1)
#   2. a release build in the repo      apps/*/app/build/outputs/apk/release/app-release.apk
#      (this checkout and C:\Users\yairz\Downloads\FutureOS--main; worktrees are skipped - they are WIP)
# Then: gen_privapp.py -> 02_build_system.sh -> 03_verify.sh (WSL) -> kit images\ + SHA256SUMS + VERSION.txt.
# Never touches the phone. FROM_PHONE=1 first refreshes (1) with 01_dump.sh APPS_ONLY=1 (read-only adb).
set -euo pipefail
export MSYS_NO_PATHCONV=1
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"
ROM=/d/futureos-rom
KIT="$ROM/FutureOS-flash"
STAGE="$ROM/stage"
WSLW=//wsl.localhost/Ubuntu/root/fos-work
AAPT=$(ls -d "$LOCALAPPDATA"/Android/Sdk/build-tools/*/aapt.exe 2>/dev/null | sort -V | tail -1)
[ -n "$AAPT" ] || AAPT=$(ls -d /c/Users/yairz/AppData/Local/Android/Sdk/build-tools/*/aapt.exe | sort -V | tail -1)

[ "${FROM_PHONE:-0}" = 1 ] && APPS_ONLY=1 bash "$HERE/01_dump.sh"

echo "== collect newest APK per app"
rm -rf "$STAGE"; mkdir -p "$STAGE/apps"
: > "$STAGE/sources.tsv"
for d in "$ROM/backup/apps"/*/; do
  pkg=$(basename "$d"); mkdir -p "$STAGE/apps/$pkg"; cp "$d"*.apk "$STAGE/apps/$pkg/"
  printf '%s\tphone\t%s\n' "$pkg" "$(date -r "$d/base.apk" +%s)" >> "$STAGE/sources.tsv"
done
for root in "$REPO" /c/Users/yairz/Downloads/FutureOS--main; do
  for apk in "$root"/apps/*/app/build/outputs/apk/release/app-release.apk; do
    [ -f "$apk" ] || continue
    cp "$apk" "$ROM/aapt-probe.apk"   # aapt cannot open paths with Hebrew characters
    pkg=$( ("$AAPT" dump badging "$(cygpath -w "$ROM/aapt-probe.apk")" 2>/dev/null || true) | sed -n "s/^package: name='\([^']*\)'.*/\1/p" | tr -d '\r')
    [ -n "$pkg" ] || continue
    t=$(date -r "$apk" +%s)
    cur=$(awk -F'\t' -v p="$pkg" '$1==p{print $3}' "$STAGE/sources.tsv" | tail -1)
    if [ -z "$cur" ] || [ "$t" -gt "$cur" ]; then
      rm -rf "$STAGE/apps/$pkg"; mkdir -p "$STAGE/apps/$pkg"; cp "$apk" "$STAGE/apps/$pkg/base.apk"   # universal build: no splits
      grep -v "^$pkg	" "$STAGE/sources.tsv" > "$STAGE/s.tmp" || true; mv "$STAGE/s.tmp" "$STAGE/sources.tsv"
      printf '%s\t%s\t%s\n' "$pkg" "${apk#$root/}" "$t" >> "$STAGE/sources.tsv"
    fi
  done
done
sort -o "$STAGE/sources.tsv" "$STAGE/sources.tsv"
awk -F'\t' '{printf "   %-28s %s\n", $1, $2}' "$STAGE/sources.tsv"

echo "== privapp allowlist + touch exclusion"
python "$(cygpath -w "$HERE/gen_privapp.py")" "$(cygpath -w "$STAGE/apps")" "$(cygpath -w "$ROM/backup/permissions.txt")" "$(cygpath -w "$STAGE/sysfiles")" >/dev/null

echo "== copy into WSL"
wsl -d Ubuntu -u root -- bash -c "rm -rf /root/fos-work/apps /root/fos-work/sysfiles /root/fos-work/out && mkdir -p /root/fos-work && chmod 777 /root/fos-work"
cp -r "$STAGE/apps" "$STAGE/sysfiles" "$WSLW/"
for s in 02_build_system.sh 03_verify.sh mount_ro.sh; do cp "$HERE/$s" "$WSLW/$s"; done
[ -f "$WSLW/system_a.orig.img" ] || cp "$ROM/backup/images/system_a.img" "$WSLW/system_a.orig.img"
[ -f "$WSLW/SHA256SUMS" ] || cp "$ROM/backup/images/SHA256SUMS" "$WSLW/SHA256SUMS"

echo "== build + verify (WSL)"
wsl -d Ubuntu -u root -- bash -c "cd /root/fos-work && sed -i 's/\r$//' *.sh && bash 02_build_system.sh >/dev/null && bash 03_verify.sh | tee out/verify.log | tail -4"
grep -q "ALL CHECKS PASSED" "$WSLW/out/verify.log" || { echo "VERIFY FAILED - kit not changed"; exit 1; }

echo "== publish to kit"
prev=$(sed -n 's/^version: //p' "$KIT/VERSION.txt" 2>/dev/null || echo 2)
ver=$((prev + 1))
cp "$WSLW/out/images/system_a.img" "$KIT/images/system_a.img.new"
exp=$(awk '{print $1}' "$WSLW/out/images/SHA256SUMS")
got=$(sha256sum "$KIT/images/system_a.img.new" | awk '{print $1}')
[ "$exp" = "$got" ] || { rm -f "$KIT/images/system_a.img.new"; echo "copy hash mismatch - kit not changed"; exit 1; }
mv -f "$KIT/images/system_a.img.new" "$KIT/images/system_a.img"
echo "$exp  system_a.img" > "$KIT/images/SHA256SUMS"
cp "$WSLW/out/build.log" "$WSLW/out/verify.log" "$KIT/"
rm -rf "$KIT/sysfiles"; cp -r "$STAGE/sysfiles" "$KIT/sysfiles"
mkdir -p "$KIT/scripts"; cp "$HERE"/*.sh "$HERE"/*.py "$KIT/scripts/"
{ echo "version: $ver"; echo "built: $(date '+%Y-%m-%d %H:%M')"; echo "sha256: $exp"; echo "apps (package, source):"
  awk -F'\t' '{printf "  %s\t%s\n", $1, $2}' "$STAGE/sources.tsv"; } > "$KIT/VERSION.txt"
wsl -d Ubuntu -u root -- bash -c "rm -f /root/fos-work/out/images/system_a.img"   # C: is nearly full
echo "== done: FutureOS v$ver -> $KIT\\images\\system_a.img"
