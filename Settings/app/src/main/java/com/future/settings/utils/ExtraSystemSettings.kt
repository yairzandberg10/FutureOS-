package com.future.settings.utils

import com.future.sharednav.i18n.tr

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

    private val BOOL = listOf(SysOption(SysSetting.OFF, tr("כבוי")), SysOption(SysSetting.ON, tr("פועל")))

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
        SysOption("0", tr("כבוי")), SysOption("1", tr("חלש")), SysOption("2", tr("בינוני")), SysOption("3", tr("חזק"))
    )

    // ---------------------------------------------------------------------
    // נגישות
    // ---------------------------------------------------------------------

    val ACCESSIBILITY = SysScreen(
        route = "extra_accessibility",
        title = tr("נגישות"),
        keywords = tr("accessibility נגישות ניגודיות צבעים עיוורון צבעים מונו כתוביות הקראה"),
        sections = listOf(
            SysSection(tr("ראייה"), listOf(
                toggle("high_contrast", tr("טקסט בניגודיות גבוהה"), tr("מסגרת כהה או בהירה סביב אותיות"), "secure", "high_text_contrast_enabled"),
                toggle("bold_text", tr("טקסט מודגש"), tr("כל האותיות במערכת עבות יותר"), "secure", "font_weight_adjustment", on = "300", off = "0"),
                SysSetting(
                    "daltonizer", tr("תיקון צבעים"), "",
                    listOf(
                        SysOption("off", tr("כבוי")),
                        SysOption("0", tr("גווני אפור")),
                        SysOption("11", tr("אדום-ירוק (דאוטרנומליה)")),
                        SysOption("12", tr("אדום-ירוק (פרוטנומליה)")),
                        SysOption("13", tr("כחול-צהוב (טריטנומליה)")),
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
                    "extra_dim", tr("עמעום נוסף"), "",
                    listOf(SysOption("off", tr("כבוי")), SysOption("20", tr("קל")), SysOption("50", tr("בינוני")), SysOption("80", tr("חזק"))),
                    defaultValue = "off",
                    readCmd = "if [ \"$(settings get secure reduce_bright_colors_activated)\" = \"1\" ]; then settings get secure reduce_bright_colors_level; else echo off; fi",
                    parse = { raw -> if (raw in listOf("20", "50", "80")) raw else if (raw == "off") "off" else "50" },
                    write = { v ->
                        if (v == "off") listOf(put("secure", "reduce_bright_colors_activated", "0"))
                        else listOf(put("secure", "reduce_bright_colors_level", v), put("secure", "reduce_bright_colors_activated", "1"))
                    },
                ),
                cycle(
                    "ui_timeout", tr("משך הצגת הודעות"), "", "secure", "accessibility_non_interactive_ui_timeout_ms",
                    listOf(
                        SysOption("0", tr("ברירת מחדל")), SysOption("10000", tr("10 שניות")), SysOption("30000", tr("30 שניות")),
                        SysOption("60000", tr("דקה")), SysOption("120000", tr("2 דקות")),
                    ),
                    default = "0",
                    extra = { v -> listOf(put("secure", "accessibility_interactive_ui_timeout_ms", v)) },
                ),
            )),
            SysSection(tr("שמיעה"), listOf(
                toggle("mono_audio", tr("אודיו מונו"), tr("אותו צליל בשתי האוזניות"), "system", "master_mono"),
                cycle(
                    "audio_balance", tr("איזון שמע"), "", "system", "master_balance",
                    listOf(
                        SysOption("-0.5", tr("נוטה לשמאל")), SysOption("0.0", tr("מאוזן")), SysOption("0.5", tr("נוטה לימין")),
                    ),
                    default = "0.0",
                ),
                toggle("captions", tr("כתוביות"), tr("כתוביות בסרטונים שתומכים בהן"), "secure", "accessibility_captioning_enabled"),
                toggle("hearing_aid", tr("תאימות למכשירי שמיעה"), tr("משפר שמע בשיחות עם מכשיר שמיעה"), "system", "hearing_aid"),
            )),
            SysSection(tr("מקשים והקלדה"), listOf(
                toggle("power_ends_call", tr("מקש הפעלה מנתק שיחה"), tr("לחיצה על מקש ההפעלה מסיימת שיחה"), "secure", "incall_power_button_behavior", on = "2", off = "1"),
                cycle(
                    "long_press", tr("משך לחיצה ארוכה"), "", "secure", "long_press_timeout",
                    listOf(SysOption("400", tr("קצר")), SysOption("1000", tr("בינוני")), SysOption("1500", tr("ארוך"))),
                    default = "400",
                ),
                toggle("show_password", tr("הצגת סיסמאות בהקלדה"), tr("האות האחרונה נראית לרגע"), "system", "show_password", defaultOn = true),
            )),
            SysSection(tr("הקראה"), listOf(
                cycle(
                    "tts_rate", tr("קצב הקראה"), "", "secure", "tts_default_rate",
                    listOf(SysOption("50", tr("איטי")), SysOption("100", tr("רגיל")), SysOption("150", tr("מהיר")), SysOption("200", tr("מהיר מאוד"))),
                    default = "100",
                ),
                cycle(
                    "tts_pitch", tr("גובה קול ההקראה"), "", "secure", "tts_default_pitch",
                    listOf(SysOption("75", tr("נמוך")), SysOption("100", tr("רגיל")), SysOption("125", tr("גבוה"))),
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
        title = tr("תצוגה מתקדמת"),
        keywords = tr("תאורת לילה night light אור כחול שומר מסך צבעים תצוגה פעילה"),
        sections = listOf(
            SysSection(tr("תאורת לילה"), listOf(
                toggle("night_light", tr("תאורת לילה"), tr("גוון חם שמקל על העיניים בחושך"), "secure", "night_display_activated"),
                cycle(
                    "night_temp", tr("עוצמת תאורת הלילה"), "", "secure", "night_display_color_temperature",
                    listOf(SysOption("4082", tr("עדינה")), SysOption("3500", tr("בינונית")), SysOption("2850", tr("חמה")), SysOption("2596", tr("חמה מאוד"))),
                    default = "2850",
                ),
                cycle(
                    "night_schedule", tr("תזמון תאורת לילה"), "", "secure", "night_display_auto_mode",
                    listOf(SysOption("0", tr("ידני")), SysOption("1", tr("22:00 עד 06:00")), SysOption("2", tr("משקיעה עד זריחה"))),
                    default = "0",
                    extra = { v ->
                        if (v == "1") listOf(
                            put("secure", "night_display_custom_start_time", "79200000"),
                            put("secure", "night_display_custom_end_time", "21600000"),
                        ) else emptyList()
                    },
                ),
            )),
            SysSection(tr("מסך"), listOf(
                cycle(
                    "color_mode", tr("מצב צבעים"), "", "system", "display_color_mode",
                    listOf(SysOption("0", tr("טבעי")), SysOption("1", tr("מוגבר")), SysOption("2", tr("רווי")), SysOption("3", tr("אוטומטי"))),
                    default = "0",
                ),
                toggle(
                    "screensaver", tr("שומר מסך בטעינה"), tr("שעון על המסך כשהמכשיר נטען"), "secure", "screensaver_enabled",
                    extra = { on -> listOf(put("secure", "screensaver_activate_on_sleep", if (on) "1" else "0")) },
                ),
                toggle("doze", tr("הדלקת מסך בהתראה"), tr("המסך נדלק לרגע כשמגיעה התראה"), "secure", "doze_enabled", defaultOn = true),
                toggle("always_on", tr("תצוגה פעילה תמיד"), tr("שעון עמום גם כשהמסך כבוי"), "secure", "doze_always_on"),
            )),
        )
    )

    // ---------------------------------------------------------------------
    // צלילים ורטט מתקדם
    // ---------------------------------------------------------------------

    val SOUND = SysScreen(
        route = "extra_sound",
        title = tr("צלילים ורטט מתקדם"),
        keywords = tr("נא לא להפריע dnd רטט עוצמת רטט צלילי חיוג עגינה"),
        sections = listOf(
            SysSection(tr("נא לא להפריע"), listOf(
                SysSetting(
                    "dnd", tr("נא לא להפריע"), "",
                    listOf(
                        SysOption("0", tr("כבוי")), SysOption("1", tr("רק חשובים")),
                        SysOption("3", tr("רק שעון מעורר")), SysOption("2", tr("שקט מוחלט")),
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
            SysSection(tr("עוצמת רטט"), listOf(
                cycle("ring_vib", tr("רטט בשיחה נכנסת"), "", "system", "ring_vibration_intensity", INTENSITY, default = "2"),
                cycle("notif_vib", tr("רטט בהתראות"), "", "system", "notification_vibration_intensity", INTENSITY, default = "2"),
                cycle("haptic_vib", tr("רטט במקשים"), "", "system", "haptic_feedback_intensity", INTENSITY, default = "2"),
            )),
            SysSection(tr("צלילים"), listOf(
                toggle("dtmf_long", tr("צלילי חיוג ארוכים"), tr("הצליל נמשך כל עוד המקש לחוץ"), "system", "dtmf_tone_type"),
                toggle("dock_sounds", tr("צלילי עגינה"), tr("צליל בחיבור לעמדת עגינה"), "global", "dock_sounds_enabled"),
            )),
        )
    )

    // ---------------------------------------------------------------------
    // התראות מתקדמות
    // ---------------------------------------------------------------------

    val NOTIFICATIONS = SysScreen(
        route = "extra_notifications",
        title = tr("התראות מתקדמות"),
        keywords = tr("התראות קופצות היסטוריית התראות נורית דחייה בועות נקודות"),
        sections = listOf(
            SysSection(tr("התראות"), listOf(
                toggle("heads_up", tr("התראות קופצות"), tr("התראה חשובה מופיעה בראש המסך"), "global", "heads_up_notifications_enabled", defaultOn = true),
                toggle("notif_history", tr("היסטוריית התראות"), tr("שומר התראות שנסגרו ב-24 השעות האחרונות"), "secure", "notification_history_enabled"),
                toggle("notif_snooze", tr("דחיית התראות"), tr("אפשר לדחות התראה לזמן מאוחר יותר"), "secure", "show_notification_snooze"),
                toggle("notif_badges", tr("נקודות על אפליקציות"), tr("סימון אפליקציה שיש לה התראה חדשה"), "secure", "notification_badging", defaultOn = true),
                toggle("notif_bubbles", tr("בועות שיחה"), tr("שיחות נפתחות בבועה צפה"), "secure", "notification_bubbles", defaultOn = true),
                toggle("notif_led", tr("נורית מהבהבת"), tr("הנורית מהבהבת כשיש התראה חדשה"), "system", "notification_light_pulse", defaultOn = true),
            )),
        )
    )

    // ---------------------------------------------------------------------
    // רשת מתקדמת
    // ---------------------------------------------------------------------

    val NETWORK = SysScreen(
        route = "extra_network",
        title = tr("רשת מתקדמת"),
        keywords = tr("wifi וויפיי dns פרטי חוסם פרסומות חיסכון בנתונים data saver רשת"),
        sections = listOf(
            SysSection(tr("רשת"), listOf(
                SysSetting(
                    "wifi", "Wi-Fi", tr("חיבור לרשתות אלחוטיות"), BOOL, SysSetting.OFF,
                    readCmd = get("global", "wifi_on"),
                    parse = { raw -> if (raw == "1" || raw == "2") SysSetting.ON else SysSetting.OFF },
                    write = { v -> listOf("svc wifi ${if (v == SysSetting.ON) "enable" else "disable"}") },
                ),
                SysSetting(
                    "data_saver", tr("חיסכון בנתונים"), tr("אפליקציות ברקע לא משתמשות בנתונים"), BOOL, SysSetting.OFF,
                    readCmd = "cmd netpolicy get restrict-background",
                    parse = { raw -> if (raw.contains("enabled") && !raw.contains("disabled")) SysSetting.ON else SysSetting.OFF },
                    write = { v -> listOf("cmd netpolicy set restrict-background ${v == SysSetting.ON}") },
                ),
                SysSetting(
                    "private_dns", tr("DNS פרטי"), "",
                    listOf(
                        SysOption("off", tr("כבוי")),
                        SysOption("opportunistic", tr("אוטומטי")),
                        SysOption("dns.google", "Google"),
                        SysOption("one.one.one.one", "Cloudflare"),
                        SysOption("dns.adguard-dns.com", tr("AdGuard - חוסם פרסומות")),
                    ),
                    defaultValue = "opportunistic",
                    readCmd = "m=$(settings get global private_dns_mode); if [ \"\$m\" = \"hostname\" ]; then settings get global private_dns_specifier; else echo \"\$m\"; fi",
                    parse = { raw -> if (raw in listOf("off", "opportunistic", "dns.google", "one.one.one.one", "dns.adguard-dns.com")) raw else "opportunistic" },
                    write = { v ->
                        if (v == "off" || v == "opportunistic") listOf(put("global", "private_dns_mode", v))
                        else listOf(put("global", "private_dns_specifier", v), put("global", "private_dns_mode", "hostname"))
                    },
                ),
                toggle("data_always_on", tr("נתונים סלולריים תמיד פעילים"), tr("מעבר מהיר יותר בין Wi-Fi לסלולרי"), "global", "mobile_data_always_on", defaultOn = true),
                toggle("adaptive_conn", tr("קישוריות מותאמת"), tr("בחירה אוטומטית של הרשת הטובה ביותר"), "secure", "adaptive_connectivity_enabled", defaultOn = true),
            )),
            SysSection(tr("Wi-Fi וסריקה"), listOf(
                toggle("open_networks", tr("התראה על רשתות פתוחות"), tr("הודעה כשיש רשת Wi-Fi ציבורית בסביבה"), "global", "wifi_networks_available_notification_on", defaultOn = true),
                toggle("captive_portal", tr("בדיקת רשתות עם דף כניסה"), tr("מזהה רשתות שדורשות התחברות בדפדפן"), "global", "captive_portal_mode", defaultOn = true),
                toggle("wifi_scan", tr("סריקת Wi-Fi לשיפור מיקום"), tr("סורק רשתות גם כשה-Wi-Fi כבוי"), "global", "wifi_scan_always_enabled"),
                toggle("ble_scan", tr("סריקת Bluetooth לשיפור מיקום"), tr("סורק מכשירים גם כשה-Bluetooth כבוי"), "global", "ble_scan_always_enabled"),
            )),
        )
    )

    // ---------------------------------------------------------------------
    // חשמל מתקדם
    // ---------------------------------------------------------------------

    val POWER = SysScreen(
        route = "extra_power",
        title = tr("חשמל מתקדם"),
        keywords = tr("סוללה מותאמת המתנה אפליקציות חיסכון battery"),
        sections = listOf(
            SysSection(tr("סוללה"), listOf(
                toggle("adaptive_battery", tr("סוללה מותאמת"), tr("מגביל אפליקציות שכמעט לא משתמשים בהן"), "global", "adaptive_battery_management_enabled", defaultOn = true),
                toggle("app_standby", tr("המתנה לאפליקציות"), tr("אפליקציות שלא נפתחו זמן רב נרדמות"), "global", "app_standby_enabled", defaultOn = true),
                toggle("saver_auto_off", tr("כיבוי חיסכון אחרי טעינה"), tr("חיסכון בחשמל נכבה לבד כשהסוללה מתמלאת"), "global", "low_power_sticky_auto_disable_enabled", defaultOn = true),
            )),
        )
    )

    // ---------------------------------------------------------------------
    // כלי מפתחים
    // ---------------------------------------------------------------------

    val DEVELOPER = SysScreen(
        route = "extra_developer",
        title = tr("כלי מפתחים"),
        keywords = tr("developer מפתחים אנימציות גבולות פריסה gpu מצב כהה לוג"),
        sections = listOf(
            // מה שקודם נפתח רק דרך "אפשרויות למפתחים" של אנדרואיד - עכשיו כאן,
            // כדי לא להיות תלויים באפליקציית ההגדרות האמיתית.
            SysSection(tr("USB וניפוי באגים"), listOf(
                toggle("dev_enabled", tr("אפשרויות מפתחים פעילות"), tr("מפעיל את מצב המפתחים של המערכת"), "global", "development_settings_enabled"),
                toggle("adb_usb", tr("ניפוי באגים ב-USB"), tr("חיבור ADB דרך כבל"), "global", "adb_enabled"),
                toggle("adb_wifi", tr("ניפוי באגים אלחוטי"), tr("חיבור ADB דרך Wi-Fi"), "global", "adb_wifi_enabled"),
                toggle("stay_awake", tr("מסך דלוק בזמן טעינה"), tr("המסך לא נכבה כשהמכשיר מחובר"), "global", "stay_on_while_plugged_in", on = "7"),
                toggle("verify_adb", tr("אימות אפליקציות דרך USB"), tr("בדיקת אפליקציות שמותקנות דרך ADB"), "global", "verifier_verify_adb_installs", defaultOn = true),
            )),
            SysSection(tr("אנימציות"), listOf(
                cycle(
                    "anim_speed", tr("מהירות אנימציות"), "", "global", "animator_duration_scale",
                    listOf(
                        SysOption("0", tr("כבויות")), SysOption("0.5", tr("מהירות (x0.5)")), SysOption("1", tr("רגילות")),
                        SysOption("1.5", tr("איטיות (x1.5)")), SysOption("2", tr("איטיות מאוד (x2)")),
                    ),
                    default = "1",
                    extra = { v -> listOf(put("global", "window_animation_scale", v), put("global", "transition_animation_scale", v)) },
                ),
            )),
            SysSection(tr("ציור ותצוגה"), listOf(
                propToggle("layout_bounds", tr("גבולות פריסה"), tr("מסגרת סביב כל רכיב על המסך"), "debug.layout", on = "true"),
                propToggle("gpu_bars", tr("פסי רינדור GPU"), tr("פסים שמראים כמה זמן לוקח לצייר כל פריים"), "debug.hwui.profile", on = "visual_bars"),
                propToggle("overdraw", tr("ציור-יתר"), tr("צובע אזורים שמצוירים כמה פעמים"), "debug.hwui.overdraw", on = "show"),
                propToggle("force_dark", tr("כפיית מצב כהה"), tr("גם אפליקציות בלי מצב כהה יוצגו כהות"), "debug.hwui.force_dark", on = "true"),
                propToggle("strict_mode", tr("הבהוב בפעולה איטית"), tr("המסך מהבהב כשאפליקציה נתקעת"), "persist.sys.strictmode.visual", on = "true"),
            )),
            SysSection(tr("אפליקציות"), listOf(
                toggle("dont_keep", tr("אל תשמור פעילויות"), tr("כל מסך נסגר ברגע שעוזבים אותו"), "global", "always_finish_activities"),
                toggle("all_anrs", tr("כל הודעות 'לא מגיב'"), tr("גם לאפליקציות שרצות ברקע"), "secure", "anr_show_background"),
                toggle("channel_warnings", tr("אזהרות ערוצי התראה"), tr("מתריע כשאפליקציה שולחת התראה לא תקינה"), "global", "show_notification_channel_warnings"),
                cycle(
                    "app_freezer", tr("הקפאת אפליקציות ברקע"), "", "global", "cached_apps_freezer",
                    listOf(SysOption("device_default", tr("ברירת מחדל")), SysOption("enabled", tr("פועל")), SysOption("disabled", tr("כבוי"))),
                    default = "device_default",
                ),
            )),
            SysSection(tr("מערכת"), listOf(
                SysSetting(
                    "logd_size", tr("גודל יומן מערכת"), "",
                    listOf(SysOption("64K", "64K"), SysOption("256K", "256K"), SysOption("1M", "1M"), SysOption("4M", "4M")),
                    defaultValue = "256K",
                    readCmd = "getprop persist.logd.size",
                    parse = { raw -> if (raw in listOf("64K", "256K", "1M", "4M")) raw else "256K" },
                    write = { v -> listOf("setprop persist.logd.size $v", "stop logd", "start logd") },
                ),
                propToggle("bt_abs_volume", tr("ביטול עוצמה מוחלטת ב-Bluetooth"), tr("לאוזניות שהעוצמה שלהן קופצת. אחרי הפעלה מחדש"), "persist.bluetooth.disableabsvol", on = "true"),
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
