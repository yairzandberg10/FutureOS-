#!/usr/bin/env bash
# FutureOS - אימות קריאה בלבד אחרי צריבה של build עם ה-overlay הזה.
# מכסה את הסעיפים שבסטטוס "אימות". לא משנה כלום במכשיר. שימוש: ./verify_updates_dev.sh   (ADB=adb)
set -u
ADB="${ADB:-adb}"; P=0; W=0; F=0
sh_() { "$ADB" shell "$@" 2>/dev/null | tr -d '\r'; }
ok()   { echo "PASS  $1"; P=$((P+1)); }
warn() { echo "WARN  $1"; W=$((W+1)); }
bad()  { echo "FAIL  $1"; F=$((F+1)); }
chk()  { # chk <desc> <actual> <expected>
  if [[ "$2" == "$3" ]]; then ok "$1 = $2"; else bad "$1 = '$2' (expected '$3')"; fi; }

echo "== בסיס =="
chk "ro.build.tags (810)"       "$(sh_ getprop ro.build.tags)" "release-keys"
chk "ro.debuggable (865)"       "$(sh_ getprop ro.debuggable)" "0"
chk "ro.adb.secure (865)"       "$(sh_ getprop ro.adb.secure)" "1"
echo "version: $(sh_ getprop ro.futureos.version) channel: $(sh_ getprop ro.futureos.channel) (986)"

echo "== OTA (815/816/817/935) =="
MODE="$(sh_ getprop ro.futureos.ota.mode)"; echo "ota.mode=$MODE"
if [[ "$MODE" == "vab" ]]; then
  chk "ro.virtual_ab.enabled" "$(sh_ getprop ro.virtual_ab.enabled)" "true"
fi
if [[ "$MODE" == "ab" || "$MODE" == "vab" ]]; then
  chk "ro.build.ab_update" "$(sh_ getprop ro.build.ab_update)" "true"
  [[ -n "$(sh_ getprop ro.boot.slot_suffix)" ]] && ok "slot suffix: $(sh_ getprop ro.boot.slot_suffix)" || bad "no slot suffix"
  sh_ 'service list' | grep -q update_engine && ok "update_engine service" || bad "update_engine service missing"
else
  warn "non-A/B: אין rollback אוטומטי (816) ואין snapshot (935)"
fi
sh_ 'ls /system/etc/security/otacerts.zip' | grep -q otacerts && ok "otacerts.zip present (810)" || bad "otacerts.zip missing"

echo "== יומנים וקריסות (939/963/964/966) =="
chk "ro.logd.size"                     "$(sh_ getprop ro.logd.size)" "1M"
chk "tombstoned.max_tombstone_count"   "$(sh_ getprop tombstoned.max_tombstone_count)" "20"
sh_ 'logcat -g' | head -3

echo "== גיבוי (923/955) =="
sh_ 'bmgr enabled' | head -2
sh_ 'bmgr list transports' | head -5

echo "== מערכת קבצים (944) =="
sh_ 'mount' | grep ' /data ' | head -1

echo "== תוכנות מותקנות מראש (840) =="
echo "חבילות system שאינן com.future/com.android (לבדוק ידנית מול הרשימה המותרת):"
sh_ 'pm list packages -s' | sed 's/package://' | grep -v -E '^(com\.future\.|com\.android\.|android$|android\.|com\.qualcomm|com\.mediatek|com\.qti)' | head -40

echo "== מצב בטוח (933) =="
echo "ידני: לאתחל ולהחזיק DPAD_CENTER או MENU בזמן האתחול. אמור להופיע 'מצב בטוח' ו-sys.safemode=1:"
echo "      adb shell getprop sys.safemode"

echo; echo "PASS=$P WARN=$W FAIL=$F"; exit $((F>0))
