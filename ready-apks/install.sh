#!/bin/bash
# מתקין את כל ה-APK-ים המוכנים בתיקייה הזו על המכשיר, אחד אחרי השני:
# adb install -r (מעל הגרסה הקיימת, בלי לאבד נתונים), קומפילציה מראש של ART
# (cmd package compile -m speed -f), ושורה ב-/data/local/tmp/installs.log.
#
# שימוש (מ-Git Bash, כשהטלפון מחובר ב-ADB):
#   bash ready-apks/install.sh            # הכל
#   bash ready-apks/install.sh com.future.clock com.future.tools   # רק אלה
#
# ה-APK-ים נבנו מהענף qa-install (התיקונים מדוח ה-QA + motion-system), חתומים
# במפתח ה-debug כמו כל גרסת release בפרויקט, ולכן מתקינים מעל ההתקנה הקיימת.
export MSYS_NO_PATHCONV=1
# pwd -W (Git Bash) נותן נתיב Windows - adb.exe לא מבין /c/Users/...
DIR="$(cd "$(dirname "$0")" && (pwd -W 2>/dev/null || pwd))"
ADB="${ADB:-$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe}"
[ -x "$ADB" ] || ADB=adb

# סדר: המקלדת והלאנצ'ר קודם (משפיעים על כל השאר), FutureUI והעוזר בסוף
# (FutureUI מאתחלת את שורת המצב, והעוזר הוא APK של ~300MB).
ORDER="keyboard futurelauncher clock recorder terminal notes tasks tools contact
flashlight remote translate music files gallery navigation dialer messages camera
wallpapers calendar calculator settings bluetooth fitness guide sfarim futureui assistant"

if ! "$ADB" get-state >/dev/null 2>&1; then
  echo "אין מכשיר מחובר ב-ADB. חבר את הטלפון ונסה שוב."
  exit 1
fi

if [ $# -gt 0 ]; then
  packages="$*"
else
  packages=""
  for name in $ORDER; do
    [ -f "$DIR/com.future.$name.apk" ] && packages="$packages com.future.$name"
  done
fi

ok=0; failed=""
for pkg in $packages; do
  apk="$DIR/$pkg.apk"
  if [ ! -f "$apk" ]; then echo "$pkg: אין APK"; failed="$failed $pkg"; continue; fi
  echo "מתקין $pkg..."
  if "$ADB" install -r "$apk" >/tmp/install-$pkg.log 2>&1 && grep -q Success /tmp/install-$pkg.log; then
    "$ADB" shell cmd package compile -m speed -f "$pkg" >/dev/null 2>&1
    "$ADB" shell "echo \"\$(date +%H:%M:%S) $pkg installed (qa-install, ready-apks)\" >> /data/local/tmp/installs.log"
    echo "  הותקן"
    ok=$((ok + 1))
  else
    echo "  נכשל:"; cat /tmp/install-$pkg.log
    failed="$failed $pkg"
  fi
done

echo
echo "הותקנו: $ok"
[ -n "$failed" ] && echo "נכשלו:$failed"
