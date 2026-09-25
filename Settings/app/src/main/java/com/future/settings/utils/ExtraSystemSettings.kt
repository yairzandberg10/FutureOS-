package com.future.settings.utils

/**
 * קטלוג הגדרות מערכת "מתקדמות" - כל הגדרה היא נתונים בלבד: איך קוראים את
 * הערך הנוכחי (פקודת shell), אילו ערכים אפשריים יש, ואילו פקודות כותבות ערך
 * חדש. ככה עשרות הגדרות חיות במקום אחד, והמסכים (ExtraSettingsScreens) רק
 * מציגים אותן - בלי עוד state ופונקציית toggle ב-ViewModel לכל אחת.
 *
 * הקריאה והכתיבה עוברות דרך root (`settings`/`setprop`/`cmd`), כמו שאר
 * המתגים שכבר קיימים ב-SystemInteractor: באנדרואיד 12 אפליקציה רגילה לא
 * יכולה לקרוא מפתחות @hide של Settings.Secure/System, ובוודאי לא לכתוב אותם.
 */
data class SysOption(val value: String, val label: String)

class SysSetting(
    val id: String,
    val title: String,
    val summary: String,
    val options: List<SysOption>,
    val defaultValue: String,
    val readCmd: String,
    val parse: (String) -> String,
    val write: (String) -> List<String>,
) {
    /** מתג כבוי/פועל מוצג כ-SettingSwitch; כל השאר - שורה שמחליפה ערך ב-OK. */
    val isSwitch: Boolean get() = options.map { it.value } == listOf(OFF, ON)

    fun labelFor(value: String): String =
        (options.firstOrNull { it.value == value } ?: options.first { it.value == defaultValue }).label

    fun next(value: String): String {
        val index = options.indexOfFirst { it.value == value }
        return options[(index + 1).mod(options.size)].value
    }

    companion object {
        const val OFF = "0"
        const val ON = "1"
    }
}

data class SysSection(val title: String, val settings: List<SysSetting>)

/** מסך הגדרות שלם - הכותרת והקטעים שלו. route משמש גם לחיפוש בהגדרות. */
data class SysScreen(val route: String, val title: String, val keywords: String, val sections: List<SysSection>) {
    val allSettings: List<SysSetting> get() = sections.flatMap { it.settings }
}

object ExtraSystemSettings {

    private val BOOL = listOf(SysOption(SysSetting.OFF, "כבוי"), SysOption(SysSetting.ON, "פועל"))

    /** מעיר את כל התהליכים לקרוא מחדש מאפייני debug.* (SYSPROPS_TRANSACTION) -
     *  אותו דבר שמסך "אפשרויות למפתחים" של אנדרואיד עושה אחרי setprop. */
    private const val POKE_SYSPROPS = "service call activity 1599295570"

    private fun get(ns: String, key: String) = "settings get $ns $key"
    private fun put(ns: String, key: String, value: String) = "settings put $ns $key $value"

    /** "1.0" ו-"1" הם אותו ערך - float נשמר לפעמים עם נקודה ולפעמים בלי. */
    private fun matchOption(raw: String, options: List<SysOption>, default: String): String {
        options.firstOrNull { it.value == raw }?.let { return it.value }
        val number = raw.toFloatOrNull() ?: return default
        return options.firstOrNull { it.value.toFloatOrNull() == number }?.value ?: default
    }

    private fun toggle(
        id: String, title: String, summary: String, ns: String, key: String,
        defaultOn: Boolean = false, on: String = "1", off: String = "0",
        extra: (Boolean) -> List<String> = { emptyList() },
    ) = SysSetting(
        id, title, summary, BOOL,
        defaultValue = if (defaultOn) SysSetting.ON else SysSetting.OFF,
        readCmd = get(ns, key),
        parse = { raw ->
            when (raw) {
                on -> SysSetting.ON
                off -> SysSetting.OFF
                else -> if (defaultOn) SysSetting.ON else SysSetting.OFF
            }
        },
        write = { v -> listOf(put(ns, key, if (v == SysSetting.ON) on else off)) + extra(v == SysSetting.ON) },
    )

    private fun cycle(
        id: String, title: String, summary: String, ns: String, key: String,
        options: List<SysOption>, default: String,
        extra: (String) -> List<String> = { emptyList() },
    ) = SysSetting(
        id, title, summary, options, default,
        readCmd = get(ns, key),
        parse = { raw -> matchOption(raw, options, default) },
        write = { v -> listOf(put(ns, key, v)) + extra(v) },
    )

    private fun propToggle(id: String, title: String, summary: String, prop: String, on: String, off: String = "false") = SysSetting(
        id, title, summary, BOOL, SysSetting.OFF,
        readCmd = "getprop $prop",
        parse = { raw -> if (raw == on) SysSetting.ON else SysSetting.OFF },
        write = { v -> listOf("setprop $prop ${if (v == SysSetting.ON) on else off}", POKE_SYSPROPS) },
    )

    private val INTENSITY = listOf(
        SysOption("0", "כבוי"), SysOption("1", "חלש"), SysOption("2", "בינוני"), SysOption("3", "חזק")
    )

    // ---------------------------------------------------------------------
    // נגישות
    // ---------------------------------------------------------------------

    val ACCESSIBILITY = SysScreen(
        route = "extra_accessibility",
        title = "נגישות",
        keywords = "accessibility נגישות ניגודיות צבעים עיוורון צבעים מונו כתוביות הקראה",
        sections = listOf(
            SysSection("ראייה", listOf(
                toggle("high_contrast", "טקסט בניגודיות גבוהה", "מסגרת כהה או בהירה סביב אותיות", "secure", "high_text_contrast_enabled"),
                toggle("bold_text", "טקסט מודגש", "כל האותיות במערכת עבות יותר", "secure", "font_weight_adjustment", on = "300", off = "0"),
                SysSetting(
                    "daltonizer", "תיקון צבעים", "",
                    listOf(
                        SysOption("off", "כבוי"),
                        SysOption("0", "גווני אפור"),
                        SysOption("11", "אדום-ירוק (דאוטרנומליה)"),
                        SysOption("12", "אדום-ירוק (פרוטנומליה)"),
                        SysOption("13", "כחול-צהוב (טריטנומליה)"),
                    ),
                    defaultValue = "off",
                    readCmd = "if [ \"$(settings get secure accessibility_display_daltonizer_enabled)\" = \"1\" ]; then settings get secure accessibility_display_daltonizer; else echo off; fi",
                    parse = { raw -> if (raw in listOf("0", "11", "12", "13")) raw else "off" },
                    write = { v ->
                        if (v == "off") listOf(put("secure", "accessibility_display_daltonizer_enabled", "0"))
                        else listOf(put("secure", "accessibility_display_daltonizer", v), put("secure", "accessibility_display_daltonizer_enabled", "1"))
                    },
                ),
                SysSetting(
                    "extra_dim", "עמעום נוסף", "",
                    listOf(SysOption("off", "כבוי"), SysOption("20", "קל"), SysOption("50", "בינוני"), SysOption("80", "חזק")),
                    defaultValue = "off",
                    readCmd = "if [ \"$(settings get secure reduce_bright_colors_activated)\" = \"1\" ]; then settings get secure reduce_bright_colors_level; else echo off; fi",
                    parse = { raw -> if (raw in listOf("20", "50", "80")) raw else if (raw == "off") "off" else "50" },
                    write = { v ->
                        if (v == "off") listOf(put("secure", "reduce_bright_colors_activated", "0"))
                        else listOf(put("secure", "reduce_bright_colors_level", v), put("secure", "reduce_bright_colors_activated", "1"))
                    },
                ),
                cycle(
                    "ui_timeout", "משך הצגת הודעות", "", "secure", "accessibility_non_interactive_ui_timeout_ms",
                    listOf(
                        SysOption("0", "ברירת מחדל"), SysOption("10000", "10 שניות"), SysOption("30000", "30 שניות"),
                        SysOption("60000", "דקה"), SysOption("120000", "2 דקות"),
                    ),
                    default = "0",
                    extra = { v -> listOf(put("secure", "accessibility_interactive_ui_timeout_ms", v)) },
                ),
            )),
            SysSection("שמיעה", listOf(
                toggle("mono_audio", "אודיו מונו", "אותו צליל בשתי האוזניות", "system", "master_mono"),
                cycle(
                    "audio_balance", "איזון שמע", "", "system", "master_balance",
                    listOf(
                        SysOption("-0.5", "נוטה לשמאל"), SysOption("0.0", "מאוזן"), SysOption("0.5", "נוטה לימין"),
                    ),
                    default = "0.0",
                ),
                toggle("captions", "כתוביות", "כתוביות בסרטונים שתומכים בהן", "secure", "accessibility_captioning_enabled"),
                toggle("hearing_aid", "תאימות למכשירי שמיעה", "משפר שמע בשיחות עם מכשיר שמיעה", "system", "hearing_aid"),
            )),
            SysSection("מקשים והקלדה", listOf(
                toggle("power_ends_call", "מקש הפעלה מנתק שיחה", "לחיצה על מקש ההפעלה מסיימת שיחה", "secure", "incall_power_button_behavior", on = "2", off = "1"),
                cycle(
                    "long_press", "משך לחיצה ארוכה", "", "secure", "long_press_timeout",
                    listOf(SysOption("400", "קצר"), SysOption("1000", "בינוני"), SysOption("1500", "ארוך")),
                    default = "400",
                ),
                toggle("show_password", "הצגת סיסמאות בהקלדה", "האות האחרונה נראית לרגע", "system", "show_password", defaultOn = true),
            )),
            SysSection("הקראה", listOf(
                cycle(
                    "tts_rate", "קצב הקראה", "", "secure", "tts_default_rate",
                    listOf(SysOption("50", "איטי"), SysOption("100", "רגיל"), SysOption("150", "מהיר"), SysOption("200", "מהיר מאוד")),
                    default = "100",
                ),
                cycle(
                    "tts_pitch", "גובה קול ההקראה", "", "secure", "tts_default_pitch",
                    listOf(SysOption("75", "נמוך"), SysOption("100", "רגיל"), SysOption("125", "גבוה")),
                    default = "100",
                ),
            )),
        )
    )

    // ---------------------------------------------------------------------
    // תצוגה מתקדמת
    // ---------------------------------------------------------------------

    val DISPLAY = SysScreen(
        route = "extra_display",
        title = "תצוגה מתקדמת",
        keywords = "תאורת לילה night light אור כחול שומר מסך צבעים תצוגה פעילה",
        sections = listOf(
            SysSection("תאורת לילה", listOf(
                toggle("night_light", "תאורת לילה", "גוון חם שמקל על העיניים בחושך", "secure", "night_display_activated"),
                cycle(
                    "night_temp", "עוצמת תאורת הלילה", "", "secure", "night_display_color_temperature",
                    listOf(SysOption("4082", "עדינה"), SysOption("3500", "בינונית"), SysOption("2850", "חמה"), SysOption("2596", "חמה מאוד")),
                    default = "2850",
                ),
                cycle(
                    "night_schedule", "תזמון תאורת לילה", "", "secure", "night_display_auto_mode",
                    listOf(SysOption("0", "ידני"), SysOption("1", "22:00 עד 06:00"), SysOption("2", "משקיעה עד זריחה")),
                    default = "0",
                    extra = { v ->
                        if (v == "1") listOf(
                            put("secure", "night_display_custom_start_time", "79200000"),
                            put("secure", "night_display_custom_end_time", "21600000"),
                        ) else emptyList()
                    },
                ),
            )),
            SysSection("מסך", listOf(
                cycle(
                    "color_mode", "מצב צבעים", "", "system", "display_color_mode",
                    listOf(SysOption("0", "טבעי"), SysOption("1", "מוגבר"), SysOption("2", "רווי"), SysOption("3", "אוטומטי")),
                    default = "0",
                ),
                toggle(
                    "screensaver", "שומר מסך בטעינה", "שעון על המסך כשהמכשיר נטען", "secure", "screensaver_enabled",
                    extra = { on -> listOf(put("secure", "screensaver_activate_on_sleep", if (on) "1" else "0")) },
                ),
                toggle("doze", "הדלקת מסך בהתראה", "המסך נדלק לרגע כשמגיעה התראה", "secure", "doze_enabled", defaultOn = true),
                toggle("always_on", "תצוגה פעילה תמיד", "שעון עמום גם כשהמסך כבוי", "secure", "doze_always_on"),
            )),
        )
    )

    // ---------------------------------------------------------------------
    // צלילים ורטט מתקדם
    // ---------------------------------------------------------------------

    val SOUND = SysScreen(
        route = "extra_sound",
        title = "צלילים ורטט מתקדם",
        keywords = "נא לא להפריע dnd רטט עוצמת רטט צלילי חיוג עגינה",
        sections = listOf(
            SysSection("נא לא להפריע", listOf(
                SysSetting(
                    "dnd", "נא לא להפריע", "",
                    listOf(
                        SysOption("0", "כבוי"), SysOption("1", "רק חשובים"),
                        SysOption("3", "רק שעון מעורר"), SysOption("2", "שקט מוחלט"),
                    ),
                    defaultValue = "0",
                    readCmd = get("global", "zen_mode"),
                    parse = { raw -> if (raw in listOf("0", "1", "2", "3")) raw else "0" },
                    write = { v ->
                        val mode = when (v) { "1" -> "priority"; "2" -> "none"; "3" -> "alarms"; else -> "off" }
                        listOf("cmd notification set_dnd $mode")
                    },
                ),
            )),
            SysSection("עוצמת רטט", listOf(
                cycle("ring_vib", "רטט בשיחה נכנסת", "", "system", "ring_vibration_intensity", INTENSITY, default = "2"),
                cycle("notif_vib", "רטט בהתראות", "", "system", "notification_vibration_intensity", INTENSITY, default = "2"),
                cycle("haptic_vib", "רטט במקשים", "", "system", "haptic_feedback_intensity", INTENSITY, default = "2"),
            )),
            SysSection("צלילים", listOf(
                toggle("dtmf_long", "צלילי חיוג ארוכים", "הצליל נמשך כל עוד המקש לחוץ", "system", "dtmf_tone_type"),
                toggle("dock_sounds", "צלילי עגינה", "צליל בחיבור לעמדת עגינה", "global", "dock_sounds_enabled"),
            )),
        )
    )

    // ---------------------------------------------------------------------
    // התראות מתקדמות
    // ---------------------------------------------------------------------

    val NOTIFICATIONS = SysScreen(
        route = "extra_notifications",
        title = "התראות מתקדמות",
        keywords = "התראות קופצות היסטוריית התראות נורית דחייה בועות נקודות",
        sections = listOf(
            SysSection("התראות", listOf(
                toggle("heads_up", "התראות קופצות", "התראה חשובה מופיעה בראש המסך", "global", "heads_up_notifications_enabled", defaultOn = true),
                toggle("notif_history", "היסטוריית התראות", "שומר התראות שנסגרו ב-24 השעות האחרונות", "secure", "notification_history_enabled"),
                toggle("notif_snooze", "דחיית התראות", "אפשר לדחות התראה לזמן מאוחר יותר", "secure", "show_notification_snooze"),
                toggle("notif_badges", "נקודות על אפליקציות", "סימון אפליקציה שיש לה התראה חדשה", "secure", "notification_badging", defaultOn = true),
                toggle("notif_bubbles", "בועות שיחה", "שיחות נפתחות בבועה צפה", "secure", "notification_bubbles", defaultOn = true),
                toggle("notif_led", "נורית מהבהבת", "הנורית מהבהבת כשיש התראה חדשה", "system", "notification_light_pulse", defaultOn = true),
            )),
        )
    )

    // ---------------------------------------------------------------------
    // רשת מתקדמת
    // ---------------------------------------------------------------------

    val NETWORK = SysScreen(
        route = "extra_network",
        title = "רשת מתקדמת",
        keywords = "wifi וויפיי dns פרטי חוסם פרסומות חיסכון בנתונים data saver רשת",
        sections = listOf(
            SysSection("רשת", listOf(
                SysSetting(
                    "wifi", "Wi-Fi", "חיבור לרשתות אלחוטיות", BOOL, SysSetting.OFF,
                    readCmd = get("global", "wifi_on"),
                    parse = { raw -> if (raw == "1" || raw == "2") SysSetting.ON else SysSetting.OFF },
                    write = { v -> listOf("svc wifi ${if (v == SysSetting.ON) "enable" else "disable"}") },
                ),
                SysSetting(
                    "data_saver", "חיסכון בנתונים", "אפליקציות ברקע לא משתמשות בנתונים", BOOL, SysSetting.OFF,
                    readCmd = "cmd netpolicy get restrict-background",
                    parse = { raw -> if (raw.contains("enabled") && !raw.contains("disabled")) SysSetting.ON else SysSetting.OFF },
                    write = { v -> listOf("cmd netpolicy set restrict-background ${v == SysSetting.ON}") },
                ),
                SysSetting(
                    "private_dns", "DNS פרטי", "",
                    listOf(
                        SysOption("off", "כבוי"),
                        SysOption("opportunistic", "אוטומטי"),
                        SysOption("dns.google", "Google"),
                        SysOption("one.one.one.one", "Cloudflare"),
                        SysOption("dns.adguard-dns.com", "AdGuard - חוסם פרסומות"),
                    ),
                    defaultValue = "opportunistic",
                    readCmd = "m=$(settings get global private_dns_mode); if [ \"\$m\" = \"hostname\" ]; then settings get global private_dns_specifier; else echo \"\$m\"; fi",
                    parse = { raw -> if (raw in listOf("off", "opportunistic", "dns.google", "one.one.one.one", "dns.adguard-dns.com")) raw else "opportunistic" },
                    write = { v ->
                        if (v == "off" || v == "opportunistic") listOf(put("global", "private_dns_mode", v))
                        else listOf(put("global", "private_dns_specifier", v), put("global", "private_dns_mode", "hostname"))
                    },
                ),
                toggle("data_always_on", "נתונים סלולריים תמיד פעילים", "מעבר מהיר יותר בין Wi-Fi לסלולרי", "global", "mobile_data_always_on", defaultOn = true),
                toggle("adaptive_conn", "קישוריות מותאמת", "בחירה אוטומטית של הרשת הטובה ביותר", "secure", "adaptive_connectivity_enabled", defaultOn = true),
            )),
            SysSection("Wi-Fi וסריקה", listOf(
                toggle("open_networks", "התראה על רשתות פתוחות", "הודעה כשיש רשת Wi-Fi ציבורית בסביבה", "global", "wifi_networks_available_notification_on", defaultOn = true),
                toggle("captive_portal", "בדיקת רשתות עם דף כניסה", "מזהה רשתות שדורשות התחברות בדפדפן", "global", "captive_portal_mode", defaultOn = true),
                toggle("wifi_scan", "סריקת Wi-Fi לשיפור מיקום", "סורק רשתות גם כשה-Wi-Fi כבוי", "global", "wifi_scan_always_enabled"),
                toggle("ble_scan", "סריקת Bluetooth לשיפור מיקום", "סורק מכשירים גם כשה-Bluetooth כבוי", "global", "ble_scan_always_enabled"),
            )),
        )
    )

    // ---------------------------------------------------------------------
    // חשמל מתקדם
    // ---------------------------------------------------------------------

    val POWER = SysScreen(
        route = "extra_power",
        title = "חשמל מתקדם",
        keywords = "סוללה מותאמת המתנה אפליקציות חיסכון battery",
        sections = listOf(
            SysSection("סוללה", listOf(
                toggle("adaptive_battery", "סוללה מותאמת", "מגביל אפליקציות שכמעט לא משתמשים בהן", "global", "adaptive_battery_management_enabled", defaultOn = true),
                toggle("app_standby", "המתנה לאפליקציות", "אפליקציות שלא נפתחו זמן רב נרדמות", "global", "app_standby_enabled", defaultOn = true),
                toggle("saver_auto_off", "כיבוי חיסכון אחרי טעינה", "חיסכון בחשמל נכבה לבד כשהסוללה מתמלאת", "global", "low_power_sticky_auto_disable_enabled", defaultOn = true),
            )),
        )
    )

    // ---------------------------------------------------------------------
    // כלי מפתחים
    // ---------------------------------------------------------------------

    val DEVELOPER = SysScreen(
        route = "extra_developer",
        title = "כלי מפתחים",
        keywords = "developer מפתחים אנימציות גבולות פריסה gpu מצב כהה לוג",
        sections = listOf(
            SysSection("אנימציות", listOf(
                cycle(
                    "anim_speed", "מהירות אנימציות", "", "global", "animator_duration_scale",
                    listOf(
                        SysOption("0", "כבויות"), SysOption("0.5", "מהירות (x0.5)"), SysOption("1", "רגילות"),
                        SysOption("1.5", "איטיות (x1.5)"), SysOption("2", "איטיות מאוד (x2)"),
                    ),
                    default = "1",
                    extra = { v -> listOf(put("global", "window_animation_scale", v), put("global", "transition_animation_scale", v)) },
                ),
            )),
            SysSection("ציור ותצוגה", listOf(
                propToggle("layout_bounds", "גבולות פריסה", "מסגרת סביב כל רכיב על המסך", "debug.layout", on = "true"),
                propToggle("gpu_bars", "פסי רינדור GPU", "פסים שמראים כמה זמן לוקח לצייר כל פריים", "debug.hwui.profile", on = "visual_bars"),
                propToggle("overdraw", "ציור-יתר", "צובע אזורים שמצוירים כמה פעמים", "debug.hwui.overdraw", on = "show"),
                propToggle("force_dark", "כפיית מצב כהה", "גם אפליקציות בלי מצב כהה יוצגו כהות", "debug.hwui.force_dark", on = "true"),
                propToggle("strict_mode", "הבהוב בפעולה איטית", "המסך מהבהב כשאפליקציה נתקעת", "persist.sys.strictmode.visual", on = "true"),
            )),
            SysSection("אפליקציות", listOf(
                toggle("dont_keep", "אל תשמור פעילויות", "כל מסך נסגר ברגע שעוזבים אותו", "global", "always_finish_activities"),
                toggle("all_anrs", "כל הודעות 'לא מגיב'", "גם לאפליקציות שרצות ברקע", "secure", "anr_show_background"),
                toggle("channel_warnings", "אזהרות ערוצי התראה", "מתריע כשאפליקציה שולחת התראה לא תקינה", "global", "show_notification_channel_warnings"),
                cycle(
                    "app_freezer", "הקפאת אפליקציות ברקע", "", "global", "cached_apps_freezer",
                    listOf(SysOption("device_default", "ברירת מחדל"), SysOption("enabled", "פועל"), SysOption("disabled", "כבוי")),
                    default = "device_default",
                ),
            )),
            SysSection("מערכת", listOf(
                SysSetting(
                    "logd_size", "גודל יומן מערכת", "",
                    listOf(SysOption("64K", "64K"), SysOption("256K", "256K"), SysOption("1M", "1M"), SysOption("4M", "4M")),
                    defaultValue = "256K",
                    readCmd = "getprop persist.logd.size",
                    parse = { raw -> if (raw in listOf("64K", "256K", "1M", "4M")) raw else "256K" },
                    write = { v -> listOf("setprop persist.logd.size $v", "stop logd", "start logd") },
                ),
                propToggle("bt_abs_volume", "ביטול עוצמה מוחלטת ב-Bluetooth", "לאוזניות שהעוצמה שלהן קופצת. אחרי הפעלה מחדש", "persist.bluetooth.disableabsvol", on = "true"),
            )),
        )
    )

    val ALL_SCREENS = listOf(ACCESSIBILITY, DISPLAY, SOUND, NOTIFICATIONS, NETWORK, POWER, DEVELOPER)

    /** קורא את כל ההגדרות בסשן root אחד: לפני כל פקודת קריאה מודפס סמן
     *  "@@id", כך שהפלט המשותף מתפרק חזרה לערך לכל הגדרה. */
    fun readAll(interactor: SystemInteractor, settings: List<SysSetting>): Map<String, String> {
        val commands = settings.flatMap { listOf("echo '@@${it.id}'", it.readCmd) }
        val output = interactor.runRootCommands(commands).output
        val raw = mutableMapOf<String, StringBuilder>()
        var current: String? = null
        for (line in output.lines()) {
            if (line.startsWith("@@")) {
                current = line.removePrefix("@@").trim()
                raw[current] = StringBuilder()
            } else if (current != null && line.isNotBlank()) {
                raw[current]!!.append(line.trim())
            }
        }
        return settings.associate { setting ->
            setting.id to setting.parse(raw[setting.id]?.toString()?.trim().orEmpty())
        }
    }

    fun write(interactor: SystemInteractor, setting: SysSetting, value: String): Boolean =
        interactor.runRootCommands(setting.write(value)).success
}
