#!/usr/bin/env bash
# שלב 2: dump של כל המחיצות חוץ מ-userdata (קריאה בלבד, BROM).
# מקום: בערך 8-12GB (בעיקר super). לא על C.
# הרצה: ./02_dump_partitions.sh --i-hold-the-phone-lock
source "$(dirname "$0")/common.sh"
require_phone_lock "${1:-}"
ls -d "$OUT"/critical-* >/dev/null 2>&1 || die "אין גיבוי קריטי ב-$OUT - להריץ קודם 01_backup_critical.sh"

dst="$OUT/dump-$(date +%Y%m%d-%H%M)"
mkdir -p "$dst"
free_gb=$(df -BG --output=avail "$dst" | tail -1 | tr -dc 0-9)
[[ "$free_gb" -ge 15 ]] || die "רק ${free_gb}GB פנויים ב-$dst, צריך 15"

# userdata: נתונים אישיים, ענק, ולא צריך לפורט.
mtk_read rl "$dst" --skip userdata,cache,metadata
sha_all "$dst"
ls -la "$dst"
echo "נשמר: $dst. הבא: 03_extract_blobs.sh $dst (ב-Linux/WSL, בלי טלפון)."
