#!/usr/bin/env bash
# FutureOS - יצירת חבילת release חתומה: מלאה ואינקרמנטלית (810, 815, 820, 935, 940)
# רץ על מחשב הבנייה (Linux/WSL2) אחרי `m dist`. לא נוגע במכשיר. לא מוצא מפתחות בעצמו.
#
# שימוש:
#   FUTUREOS_KEYS_DIR=/secure/keys ./make_release.sh <out_dir> <target_files.zip> [previous_signed_target_files.zip]
# דרישות: ANDROID_BUILD_TOP מוגדר (אחרי lunch), מפתחות ב-FUTUREOS_KEYS_DIR (מחוץ לגיט!).
set -euo pipefail

OUT="${1:?out dir}"; TF="${2:?target_files zip}"; PREV="${3:-}"
: "${ANDROID_BUILD_TOP:?run lunch first}"
: "${FUTUREOS_KEYS_DIR:?set to the directory with releasekey/platform/... (never in git)}"
BIN="$ANDROID_BUILD_TOP/out/host/linux-x86/bin"
mkdir -p "$OUT"

for k in releasekey platform shared media networkstack; do
  [[ -f "$FUTUREOS_KEYS_DIR/$k.pk8" ]] || { echo "missing key: $k.pk8" >&2; exit 1; }
done

SIGNED="$OUT/signed-target_files.zip"
echo ">> signing target files"
"$BIN/sign_target_files_apks" -o -d "$FUTUREOS_KEYS_DIR" \
  --avb_vbmeta_key "$FUTUREOS_KEYS_DIR/avb.pem" --avb_vbmeta_algorithm SHA256_RSA4096 \
  "$TF" "$SIGNED"

echo ">> full OTA"
"$BIN/ota_from_target_files" -k "$FUTUREOS_KEYS_DIR/releasekey" "$SIGNED" "$OUT/ota-full.zip"

if [[ -n "$PREV" ]]; then
  echo ">> incremental OTA (delta from previous release)"
  "$BIN/ota_from_target_files" -k "$FUTUREOS_KEYS_DIR/releasekey" -i "$PREV" "$SIGNED" "$OUT/ota-incremental.zip"
fi

# 935/940: שומרים את ה-target-files החתום (נדרש לדלתא הבא) ואת ה-symbols (נדרש לניתוח crash dumps)
SYM="$ANDROID_BUILD_TOP/out/target/product/${TARGET_PRODUCT#*_}/symbols"
[[ -d "$SYM" ]] && (cd "$SYM/.." && zip -qr "$OUT/symbols.zip" symbols) || echo "WARN: symbols dir not found, crash dumps from this build will not be symbolizable" >&2

( cd "$OUT" && sha256sum *.zip > SHA256SUMS )
echo "done. Keep $SIGNED and symbols.zip for every released build."
echo "Reminder: bump FUTUREOS_AVB_ROLLBACK_INDEX only for security fixes (decision: owner)."
