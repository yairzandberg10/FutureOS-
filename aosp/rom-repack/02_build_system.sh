#!/bin/bash
# FutureOS ROM repack - step 2 (runs in WSL as root, no phone needed).
# Takes the stock system_a dump and bakes every FutureOS app (as installed on the phone)
# into /system/app/<Name>/ - additive only, no stock package is removed.
# Edits the image file in place with debugfs (no loop mount), so all work stays on D:.
#
#   wsl -d Ubuntu -u root -- bash 02_build_system.sh
set -euo pipefail
# /mnt/d (9p) is ~3MB/s for WSL, so the work happens on WSL's own ext4 ($W).
# Copy in/out from Windows (fast):  backup\images\system_a.img + SHA256SUMS -> \\wsl.localhost\Ubuntu\root\fos-work\
#                                    backup\apps -> \\wsl.localhost\Ubuntu\root\fos-work\apps
W="${W:-/root/fos-work}"
IN="$W/system_a.orig.img"
APPS="$W/apps"
OUTD="$W/out"
IMG="$OUTD/images/system_a.img"
LOG="$OUTD/build.log"
CTX="u:object_r:system_file:s0"
mkdir -p "$OUTD/images"
exec > >(tee "$LOG") 2>&1

echo "== input"; sha256sum "$IN"
exp=$(grep "  system_a.img" "$W/SHA256SUMS" | awk '{print $1}')
[ "$(sha256sum "$IN" | awk '{print $1}')" = "$exp" ] || { echo "input hash mismatch"; exit 1; }

echo "== copy"; cp "$IN" "$IMG"
FEAT=$(dumpe2fs -h "$IMG" 2>/dev/null | grep -i 'features' || true); echo "$FEAT"

apps_bytes=$(du -sb "$APPS" | cut -f1)
cur=$(stat -c %s "$IMG")
# apps + 25% + 256 MiB headroom, rounded up to 1 MiB
new=$(( (cur + apps_bytes * 5 / 4 + 256*1024*1024 + 1048575) / 1048576 * 1048576 ))
echo "== grow $cur -> $new bytes (apps $apps_bytes)"
truncate -s "$new" "$IMG"
if echo "$FEAT" | grep -q shared_blocks; then
  # stock Android ext4 is deduplicated read-only; resize first so unshare has room
  resize2fs -f "$IMG" || true
  e2fsck -fy -E unshare_blocks "$IMG" || [ $? -le 1 ]
fi
resize2fs -f "$IMG"
e2fsck -fy "$IMG" || [ $? -le 1 ]

# reference: label and owner of an existing stock app dir
ref=$(debugfs -R "ls -p /system/app" "$IMG" 2>/dev/null | awk -F/ '$6!="." && $6!=".."{print $6; exit}')
echo "== reference stock app: $ref"
debugfs -R "ea_get /system/app/$ref security.selinux" "$IMG" 2>/dev/null || true

CMDS=$(mktemp)
printf '%s\0' "$CTX" > /tmp/selinux_ctx   # SELinux xattrs are NUL-terminated
label() { # path mode
  echo "sif $1 mode $2"; echo "sif $1 uid 0"; echo "sif $1 gid 0"
  echo "ea_set -f /tmp/selinux_ctx $1 security.selinux"
}
count=0
for pdir in "$APPS"/*/; do
  pkg=$(basename "$pdir")
  last=${pkg##*.}
  case "$pkg" in
    com.android.sistemui) name=SistemUI ;;
    com.future.futurelauncher) name=FutureLauncher ;;
    com.future.futureui) name=FutureUI ;;
    *) name="Future$(echo "${last:0:1}" | tr a-z A-Z)${last:1}" ;;
  esac
  d="/system/priv-app/$name"
  echo "mkdir $d" >> "$CMDS"; label "$d" 040755 >> "$CMDS"
  echo "cd $d" >> "$CMDS"
  for apk in "$pdir"*.apk; do
    f=$(basename "$apk"); [ "$f" = base.apk ] && f="$name.apk"
    echo "write $apk $f" >> "$CMDS"; label "$d/$f" 0100644 >> "$CMDS"
  done
  echo "cd /" >> "$CMDS"
  echo "  + $pkg -> $d ($(ls "$pdir" | wc -l) apk)"
  count=$((count+1))
done
# system files from gen_privapp.py: priv-app allowlist (enforce mode) + touch panel exclusion
SYS="$W/sysfiles"
for rel in etc/permissions/privapp-permissions-futureos.xml etc/excluded-input-devices.xml; do
  [ -f "$SYS/$rel" ] || { echo "missing $SYS/$rel (run gen_privapp.py)"; exit 1; }
  sed -i 's/\r$//' "$SYS/$rel"
  echo "cd /system/$(dirname $rel)" >> "$CMDS"
  echo "write $SYS/$rel $(basename $rel)" >> "$CMDS"
  label "/system/$rel" 0100644 >> "$CMDS"
  echo "  + /system/$rel"
done
echo "cd /" >> "$CMDS"
echo "== writing $count apps"
debugfs -w -f "$CMDS" "$IMG" > "$OUTD/debugfs.log" 2>&1
grep -iE "error|fail|no space|not found" "$OUTD/debugfs.log" && { echo "debugfs reported errors"; exit 1; } || true

echo "== verify"
e2fsck -fn "$IMG"
debugfs -R "ls -l /system/priv-app" "$IMG" 2>/dev/null | grep -E "Future|SistemUI" | wc -l
dumpe2fs -h "$IMG" 2>/dev/null | grep -E "Block count|Free blocks|Block size"
(cd "$OUTD/images" && sha256sum system_a.img > SHA256SUMS)
echo "== done: $IMG ($(stat -c %s "$IMG") bytes)"
