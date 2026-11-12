#!/usr/bin/env bash
# Read-only verification of the security/privacy items on a booted FutureOS device.
# Usage: verify-on-device.sh   (needs adb, one device). Prints findings; changes nothing.
a() { adb shell "$@" 2>&1 | tr -d '\r'; }
h() { printf '\n== %s\n' "$*"; }

h "365 SELinux enforcing"; a getenforce
h "362/363/397 user build, no su, not debuggable"; a getprop ro.build.type; a getprop ro.debuggable; a "ls /system/bin/su /system/xbin/su 2>&1 | head -2"
h "382/421 dm-verity / AVB / verified boot state"; a getprop ro.boot.verifiedbootstate; a getprop ro.boot.vbmeta.device_state; a getprop ro.boot.veritymode
h "455/459 file-based encryption"; a getprop ro.crypto.state; a getprop ro.crypto.type
h "442 AES hardware"; a "grep -m1 Features /proc/cpuinfo"
h "418 kernel info-leak sysctls"; a cat /proc/sys/kernel/kptr_restrict /proc/sys/kernel/dmesg_restrict /proc/sys/kernel/perf_event_paranoid
h "417 /dev/mem"; a "ls -l /dev/mem 2>&1"
h "425/516/517 keystore / gatekeeper / biometrics HAL"; a getprop ro.hardware.keystore; a "pm list features | grep -Ei 'fingerprint|face|keystore|strongbox'"
h "384 gatekeeper present"; a "dumpsys -l | grep -Ei 'gatekeeper|lock_settings'"
h "428/429 listening sockets"; a "netstat -tulpn 2>/dev/null || ss -tulpn"
h "486/487/488/485 no GMS, no ad/partner packages"; a "pm list packages -s | grep -Ei 'gms|gsf|vending|ads|partner|setupwizard|recommend' || echo none-found"
h "375/376/515 indicators"; a "cmd device_config get privacy camera_mic_icons_enabled"; a "cmd device_config get privacy location_indicators_enabled"
h "398/500 clipboard toast"; a "cmd device_config get clipboard show_access_notifications"
h "380/501 permission usage (privacy dashboard / appops history)"; a "pm list packages | grep -i permissioncontroller"; a "cmd appops get-historical-ops --help 2>&1 | head -3"
h "494 private notifications / 489 history"; a "cmd settings --user 0 get secure lock_screen_allow_private_notifications"; a "cmd settings --user 0 get secure notification_history_enabled"
h "497 private DNS"; a "settings get global private_dns_mode"; a "settings get global private_dns_specifier"
h "499 MAC randomization"; a "dumpsys wifi | grep -i -m5 random"
h "509 device name"; a "settings get global device_name"; a getprop net.hostname
h "491 location"; a "cmd location is-location-enabled"
h "519 app hibernation"; a "cmd device_config get app_hibernation app_hibernation_enabled"
h "contract props 482/503/514"; a getprop persist.sys.futureos.telemetry; a getprop persist.sys.futureos.cloud_consent; a getprop persist.sys.futureos.crash_upload
h "513 error receiver (must be empty)"; a getprop ro.error.receiver.default; a getprop ro.error.receiver.system.apps
h "512 READ_LOGS holders (review)"; a "dumpsys package | grep -B2 -i 'android.permission.READ_LOGS: granted=true' | head -20"
h "467 conscrypt APEX"; a "ls /apex | grep -i conscrypt"
h "privacy service ran"; a "ls -l /data/misc/futureos 2>&1"; a "logcat -d -s futureos-privacy:*"
h "SELinux denials for the privacy domain"; a "logcat -d | grep -i 'avc.*futureos' | head -20"
