#!/system/bin/sh
# FutureOS privacy defaults / activity cleanup. Runs as root from init (see
# init.futureos.security.rc). Usage: futureos-privacy.sh defaults|clear-activity
# Not tested on a device. Every command is best-effort: failures are logged, never fatal.

# ---- Product decisions: fill in or leave empty ------------------------------
PRIVATE_DNS_HOST=""       # 497/496: e.g. a DoT hostname with tracker blocking. Empty = leave Android 12 default (opportunistic).
DEFAULT_LOCATION_OFF=1    # 491/520: 1 = location services off on first boot.
NTP_SERVER=""             # 520/483: empty = leave AOSP default (Google time server).
CAPTIVE_HTTP_URL=""       # 520/483: connectivity-check endpoints; empty = AOSP default (Google).
CAPTIVE_HTTPS_URL=""
CAPTIVE_FALLBACK_URL=""
# -----------------------------------------------------------------------------

TAG=futureos-privacy
STATE=/data/misc/futureos
MARK=$STATE/privacy_defaults_v1

say() { log -t $TAG "$*"; }
run() { "$@" >/dev/null 2>&1 || say "failed: $*"; }
sput() { run cmd settings --user 0 put "$@"; }

do_defaults() {
    [ -e "$MARK" ] && { say "defaults already applied"; return 0; }

    # 494: hide notification content on the lock screen.
    sput secure lock_screen_allow_private_notifications 0
    # 489: notification history (activity trail) off.
    sput secure notification_history_enabled 0
    # 513/514: no "send feedback" action in the app-crash dialog.
    sput global send_action_app_error 0

    # 375/376/515: camera/microphone indicators (status bar chips) and location indicator.
    run cmd device_config put privacy camera_mic_icons_enabled true
    run cmd device_config put privacy location_indicators_enabled true
    # 398/500: clipboard read toast (backup for the framework config bool).
    run cmd device_config put clipboard show_access_notifications true
    # 519: auto-reset of permissions for unused apps (app hibernation).
    run cmd device_config put app_hibernation app_hibernation_enabled true

    # 491/520: location off until the user turns it on.
    [ "$DEFAULT_LOCATION_OFF" = "1" ] && run cmd location set-location-enabled false

    # 497/496: strict private DNS only when a provider was chosen.
    if [ -n "$PRIVATE_DNS_HOST" ]; then
        sput global private_dns_mode hostname
        sput global private_dns_specifier "$PRIVATE_DNS_HOST"
    fi

    # 520/483: replace phone-home defaults only when the product chose endpoints.
    [ -n "$NTP_SERVER" ] && sput global ntp_server "$NTP_SERVER"
    [ -n "$CAPTIVE_HTTP_URL" ] && sput global captive_portal_http_url "$CAPTIVE_HTTP_URL"
    [ -n "$CAPTIVE_HTTPS_URL" ] && sput global captive_portal_https_url "$CAPTIVE_HTTPS_URL"
    [ -n "$CAPTIVE_FALLBACK_URL" ] && sput global captive_portal_fallback_url "$CAPTIVE_FALLBACK_URL"

    touch "$MARK" && say "defaults applied"
}

do_clear_activity() {
    # 489/512: logs and crash artifacts only. Usage stats, recents and DropBox are
    # owned by system_server (in-memory indexes) and must be cleared through their
    # own APIs from the Settings app, not by deleting files here.
    run logcat -b all -c
    rm -rf /data/anr/* /data/tombstones/* 2>/dev/null
    setprop sys.futureos.clear_activity 0
    say "activity artifacts cleared"
}

case "$1" in
    defaults) do_defaults ;;
    clear-activity) do_clear_activity ;;
    *) echo "usage: $0 defaults|clear-activity" >&2; exit 2 ;;
esac
