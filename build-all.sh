#!/usr/bin/env bash
# בונה (ואופציונלית מתקינה) את כל אפליקציות FutureOS ברצף, אחת בכל פעם -
# לא במקביל, כדי לא לשחזר את בעיית ה-file lock המתועדת ב-CHANGELOG.md על
# ה-build/ המשותף של SharedKeypadNav כשכמה אפליקציות נבנות בו-זמנית.
#
# רשימת האפליקציות מתגלה מהדיסק ולא מקודדת קשיח. זו הסיבה: הגרסה הקודמת
# החזיקה רשימה של 25 שמות, ובזמן שהריפו גדל ל-28 אפליקציות היא פספסה את
# Flashlight, Frixa ו-SystemUI בשקט - בדיוק אותה תקלה שההערה כאן התלוננה
# עליה קודם לגבי DEVICE_SETUP.md §5/§6 (שתי לולאות שכל אחת פספסה 4
# אפליקציות). כל תיקייה ברמה העליונה שיש בה settings.gradle.kts היא
# פרויקט Gradle עצמאי, וזה הקריטריון - כך שהוספת אפליקציה חדשה לריפו
# מכניסה אותה לכאן אוטומטית ואי אפשר שהרשימה תתיישן שוב.
#
# שימוש:
#   ./build-all.sh              # assembleDebug בכל האפליקציות
#   ./build-all.sh --install    # גם adb install -r אחרי כל בנייה מוצלחת
#
# יוצא עם קוד שגיאה != 0 אם אפליקציה כלשהי נכשלה, אחרי שניסה את כולן
# (לא עוצר באפליקציה הראשונה שנכשלת) - כדי שריצה אחת תיתן תמונה מלאה.

set -uo pipefail

INSTALL=false
if [[ "${1:-}" == "--install" ]]; then
    INSTALL=true
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# גילוי האפליקציות: כל תיקייה ברמה העליונה שהיא פרויקט Gradle עצמאי.
# SharedKeypadNav אינו נכלל כי הוא ספריית com.android.library ללא
# settings.gradle.kts משלו - הוא נבנה כתלות של כל אפליקציה.
APPS=()
while IFS= read -r settings; do
    APPS+=("$(basename "$(dirname "$settings")")")
done < <(find "$SCRIPT_DIR" -maxdepth 2 -mindepth 2 -name settings.gradle.kts | sort)

if [[ ${#APPS[@]} -eq 0 ]]; then
    echo "!! לא נמצאה אף אפליקציה - האם הסקריפט רץ מתוך הריפו?" >&2
    exit 1
fi

# adb לא תמיד ב-PATH (למשל ב-Git Bash על Windows), ובלעדיו --install נכשל
# רק אחרי שכל הבניות הסתיימו. פותרים אותו פעם אחת, מראש.
ADB="adb"
if ! command -v adb >/dev/null 2>&1; then
    for candidate in \
        "${ANDROID_HOME:-}/platform-tools/adb" \
        "${ANDROID_SDK_ROOT:-}/platform-tools/adb" \
        "$HOME/AppData/Local/Android/Sdk/platform-tools/adb.exe" \
        "$HOME/Library/Android/sdk/platform-tools/adb" \
        "$HOME/Android/Sdk/platform-tools/adb"
    do
        if [[ -x "$candidate" ]]; then ADB="$candidate"; break; fi
    done
fi
if [[ "$INSTALL" == true ]] && ! command -v "$ADB" >/dev/null 2>&1 && [[ ! -x "$ADB" ]]; then
    echo "!! --install התבקש אבל adb לא נמצא. הגדר ANDROID_HOME או הוסף adb ל-PATH." >&2
    exit 1
fi

echo "נמצאו ${#APPS[@]} אפליקציות: ${APPS[*]}"

FAILED=()
SUCCEEDED=()

for app in "${APPS[@]}"; do
    echo ""
    echo "=== $app ==="
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
            "$ADB" install -r "$apk" || FAILED+=("$app (install)")
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
