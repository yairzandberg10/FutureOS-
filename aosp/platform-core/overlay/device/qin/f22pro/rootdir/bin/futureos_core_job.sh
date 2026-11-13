#!/system/bin/sh
# FutureOS platform-core: משימות חד-פעמיות, נקרא מ-init.futureos_core.system.rc
# firstboot: ברירות מחדל ב-Settings.Global שאין להן משאב overlay (567). מסומן במפתח futureos_defaults_v.
# tmpclean: מחיקת קבצים ישנים מ-/data/local/tmp (273).
case $1 in
firstboot)
    [ "$(cmd settings get global futureos_defaults_v)" = 1 ] && exit 0
    cmd settings put global low_power_trigger_level 15
    cmd settings put global futureos_defaults_v 1
    ;;
tmpclean)
    find /data/local/tmp -mindepth 1 -mtime +7 -delete 2>/dev/null
    ;;
esac
exit 0
