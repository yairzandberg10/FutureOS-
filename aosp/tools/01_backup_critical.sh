#!/usr/bin/env bash
# שלב 1 - חובה לפני כל דבר אחר: גיבוי המחיצות שאי אפשר לשחזר ממקום אחר
# (IMEI, כיול רדיו, MAC, מספר סידורי, firmware של המודם).
# קריאה בלבד דרך mtkclient במצב BROM.
#
# הרצה:  ./01_backup_critical.sh --i-hold-the-phone-lock /path/to/second/copy
#   מכבים את הטלפון, מריצים, ואז מחברים USB תוך לחיצה על Vol+ ו-Vol- (BROM).
#   ב-Windows mtkclient צריך את הדרייבר UsbDk. אפשר גם מ-WSL עם usbipd.
source "$(dirname "$0")/common.sh"
require_phone_lock "${1:-}"
COPY2="${2:-}"
[[ -n "$COPY2" ]] || die "חסר יעד לעותק שני (דיסק אחר / USB). גיבוי אחד = אין גיבוי."

dst="$OUT/critical-$(date +%Y%m%d-%H%M)"
mkdir -p "$dst"

echo "[1/4] טבלת מחיצות (GPT)"
mtk_read printgpt | tee "$dst/gpt.txt"

# רק מה שקיים בטבלה. השמות משתנים בין יצרנים.
WANT=(nvram nvdata nvcfg persist proinfo protect1 protect2 md1img md1dsp spmfw seccfg lk lk2 preloader para expdb otp)
have=()
for p in "${WANT[@]}"; do
    grep -qiE "^\s*$p[[:space:]:]" "$dst/gpt.txt" && have+=("$p")
done
[[ ${#have[@]} -gt 0 ]] || die "לא נמצאה אף מחיצה מהרשימה ב-GPT - לבדוק את gpt.txt ידנית"
echo "    לגיבוי: ${have[*]}"

echo "[2/4] קריאה"
for p in "${have[@]}"; do
    mtk_read r "$p" "$dst/$p.bin"
done

echo "[3/4] אימות: קובץ שכולו אפסים לא שווה כלום"
bad=0
for p in "${have[@]}"; do
    f="$dst/$p.bin"
    if [[ ! -s "$f" ]]; then echo "  !!! $p: ריק"; bad=1; continue; fi
    nz=$(tr -d '\000' < "$f" | head -c 1 | wc -c)
    if [[ "$nz" -eq 0 ]]; then echo "  !!! $p: כולו אפסים"; bad=1; else echo "  ok  $p ($(du -h "$f" | cut -f1))"; fi
done
sha_all "$dst"

echo "[4/4] עותק שני"
mkdir -p "$COPY2"
cp -r "$dst" "$COPY2/"
(cd "$COPY2/$(basename "$dst")" && sha256sum -c --quiet SHA256SUMS) && echo "  העותק השני תואם"

[[ $bad -eq 0 ]] || die "יש מחיצות לא תקינות - לא ממשיכים לשלב 2 עד שמבינים למה"
echo "הגיבוי ב: $dst  +  $COPY2. מומלץ גם עותק שלישי בענן מוצפן (7z -p)."
