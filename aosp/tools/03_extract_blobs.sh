#!/usr/bin/env bash
# שלב 3: פירוק ה-dump וחילוץ blobs. בלי טלפון. רץ ב-Linux / WSL2 (על ext4, לא /mnt/c).
# כלים: lpunpack (android-sdk-libsparse-utils / otatools), simg2img, erofs-utils (fsck.erofs),
#       e2fsprogs (debugfs), python3, avbtool (אופציונלי).
#   sudo apt install android-sdk-libsparse-utils erofs-utils e2fsprogs python3
#   lpunpack: מ-otatools של AOSP, או https://github.com/LonelyFool/lpunpack_and_lpmake
# הרצה: ./03_extract_blobs.sh /path/to/dump-YYYYMMDD-HHMM
source "$(dirname "$0")/common.sh"
dump="${1:?usage: $0 <dump dir>}"
work="$dump/extracted"
mkdir -p "$work/img" "$work/fs"

echo "[1/5] super -> מחיצות לוגיות"
super="$(ls "$dump"/super*.bin "$dump"/super*.img 2>/dev/null | head -1 || true)"
if [[ -n "$super" ]]; then
    if file "$super" | grep -q "Android sparse"; then simg2img "$super" "$work/super.raw"; super="$work/super.raw"; fi
    lpunpack "$super" "$work/img"
else
    echo "  אין super: מכשיר בלי dynamic partitions, משתמשים ב-system/vendor ישירות"
    cp "$dump"/{system,vendor,product,odm}*.bin "$work/img/" 2>/dev/null || true
fi

echo "[2/5] חילוץ מערכות קבצים (בלי mount, בלי sudo)"
for img in "$work"/img/*; do
    name="$(basename "$img" | sed 's/\.\(img\|bin\)$//; s/_[ab]$//')"
    out="$work/fs/$name"; mkdir -p "$out"
    if fsck.erofs --extract="$out" "$img" >/dev/null 2>&1; then
        echo "  erofs  $name"
    elif debugfs -R "rdump / $out" "$img" >/dev/null 2>&1; then
        echo "  ext4   $name"
    else
        echo "  ??     $name (לא erofs/ext4 - דילוג)"; rmdir "$out"
    fi
done

echo "[3/5] boot / dtbo / vbmeta"
for p in boot vendor_boot dtbo vbmeta vbmeta_system vbmeta_vendor; do
    f="$(ls "$dump/$p".bin "$dump/${p}_a".bin 2>/dev/null | head -1 || true)"
    [[ -n "$f" ]] && cp "$f" "$work/$p.img"
done
command -v avbtool >/dev/null && [[ -f "$work/vbmeta.img" ]] && avbtool info_image --image "$work/vbmeta.img" > "$work/vbmeta-info.txt"

echo "[4/5] build.prop + רשימת HALs"
find "$work/fs" -name build.prop -exec sh -c 'echo "== $1"; cat "$1"' _ {} \; > "$work/all-build-props.txt"
find "$work/fs" -path '*/etc/vintf/manifest*.xml' -o -path '*/etc/init/*.rc' | sort > "$work/hal-files.txt"

echo "[5/5] proprietary-files.txt"
python3 "$TOOLS_DIR/gen_proprietary_files.py" "$work/fs" > "$work/proprietary-files.txt"
wc -l "$work/proprietary-files.txt"
echo "הבא: להעתיק את proprietary-files.txt ל-device/qin/f22pro, לעבור עליו ידנית, ולהריץ extract-files.sh של LineageOS עם --path $work/fs"
