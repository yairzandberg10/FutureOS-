#!/usr/bin/env bash
# שלב 0: מידע על המכשיר דרך ADB רגיל (בלי root, בלי reboot, בלי שינוי).
# עונה על: גרסת קושחה (1.0.x או 1.1.x+?), A/B, dynamic partitions, מצב bootloader ו-AVB.
# הרצה: ./00_device_info.sh --i-hold-the-phone-lock
source "$(dirname "$0")/common.sh"
require_phone_lock "${1:-}"
mkdir -p "$OUT"
f="$OUT/device-info.txt"

adb get-state >/dev/null || die "אין מכשיר ב-adb"
{
    echo "# $(date -Iseconds)"
    for p in ro.build.display.id ro.build.version.incremental ro.build.fingerprint ro.build.version.release \
             ro.build.version.security_patch ro.vendor.build.fingerprint ro.product.model ro.product.device \
             ro.board.platform ro.hardware ro.boot.slot_suffix ro.boot.dynamic_partitions \
             ro.virtual_ab.enabled ro.boot.verifiedbootstate ro.boot.flash.locked ro.boot.vbmeta.device_state \
             ro.oem_unlock_supported sys.oem_unlock_allowed ro.treble.enabled ro.vndk.version \
             ro.product.first_api_level gsm.version.baseband persist.vendor.vilte_support \
             persist.vendor.volte_support ro.vendor.mtk_ims_support; do
        printf '%-38s %s\n' "$p" "$(adb shell getprop "$p" | tr -d '\r')"
    done
    echo; echo "# uname"; adb shell uname -a
    echo; echo "# partitions (by-name)"
    adb shell 'ls -l /dev/block/by-name 2>/dev/null || ls -l /dev/block/platform/*/by-name 2>/dev/null' | tr -d '\r'
    echo; echo "# OEM unlocking toggle (Settings.Global)"; adb shell settings get global oem_unlock_allowed
} | tee "$f"

echo
ver="$(adb shell getprop ro.build.display.id | tr -d '\r')"
echo ">>> גרסה: $ver"
echo ">>> לפי Hovatek, מגרסה 1.1.0 Qin חסמו את פתיחת ה-bootloader. אם זו 1.1.x: לא לעשות downgrade"
echo "    (משתמש שם עשה downgrade ל-1.0.7 עם format, והטלפון נהרס). רק גיבוי (01) ו-dump (02)."
echo "נשמר: $f"
