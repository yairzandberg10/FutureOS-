#!/usr/bin/env bash
# בונה (ואופציונלית מתקינה) את כל 23 האפליקציות של FutureOS ברצף, אחת
# בכל פעם - לא במקביל, כדי לא לשחזר את בעיית ה-file lock המתועדת ב-
# CHANGELOG.md על ה-build/ המשותף של SharedKeypadNav כשכמה אפליקציות
# נבנות בו-זמנית. זה הפתרון ל-DEVICE_SETUP.md §5/§6 שהיו לו שתי לולאות
# נפרדות (בנייה, אז התקנה) שכל אחת פספסה 4 אפליקציות (Fitness, Calculator,
# Clock, Navigation) - כאן יש רשימה אחת, מקור אמת יחיד.
#
# שימוש:
#   ./build-all.sh              # assembleDebug בכל 23 האפליקציות
#   ./build-all.sh --install    # גם adb install -r אחרי כל בנייה מוצלחת
#
# יוצא עם קוד שגיאה != 0 אם אפליקציה כלשהי נכשלה, אחרי שניסה את כולן
# (לא עוצר באפליקציה הראשונה שנכשלת) - כדי שריצה אחת תיתן תמונה מלאה.

set -uo pipefail

INSTALL=false
if [[ "${1:-}" == "--install" ]]; then
    INSTALL=true
fi

APPS=(
    Assistant Calculator Calendar Camera Clock Contact dialer Files Fitness
    FutureLauncher FutureUI Gallery Guide Keyboard Messages Music Navigation
    notes Remote Settings Sfarim Terminal Tools
)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FAILED=()
SUCCEEDED=()

for app in "${APPS[@]}"; do
    echo ""
    echo "=== $app ==="
    if [[ ! -d "$SCRIPT_DIR/$app" ]]; then
        echo "!! תיקייה לא קיימת: $app - מדלג"
        FAILED+=("$app (missing)")
        continue
    fi
    (cd "$SCRIPT_DIR/$app" && ./gradlew assembleDebug)
    if [[ $? -ne 0 ]]; then
        echo "!! $app נכשל ב-assembleDebug"
        FAILED+=("$app")
        continue
    fi
    SUCCEEDED+=("$app")

    if [[ "$INSTALL" == true ]]; then
        apk="$SCRIPT_DIR/$app/app/build/outputs/apk/debug/app-debug.apk"
        if [[ -f "$apk" ]]; then
            adb install -r "$apk" || FAILED+=("$app (install)")
        else
            echo "!! לא נמצא APK עבור $app אחרי בנייה מוצלחת - נתיב לא צפוי?"
            FAILED+=("$app (apk missing)")
        fi
    fi
done

echo ""
echo "=== סיכום ==="
echo "הצליחו (${#SUCCEEDED[@]}/${#APPS[@]}): ${SUCCEEDED[*]:-none}"
if [[ ${#FAILED[@]} -gt 0 ]]; then
    echo "נכשלו (${#FAILED[@]}): ${FAILED[*]}"
    exit 1
fi
echo "כל האפליקציות נבנו בהצלחה."
