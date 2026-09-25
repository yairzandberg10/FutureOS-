package com.future.assistant.data

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.provider.CalendarContract
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/** speech - מה שהקול מקריא, כשהוא שונה מהטקסט שעל המסך (למשל מילה באנגלית
 * שמוצגת באותיות לועזיות אבל מוקראת בתעתיק עברי). */
data class CommandResult(val responseText: String, val shouldClose: Boolean = false, val speech: String = responseText)

/**
 * מנוע פקודות קוליות מקומי לגמרי - בלי AI/שרת חיצוני, בלי אינטרנט. כל
 * "פקודה" (VoiceIntent) היא רשימת ניסוחים (triggers) שאם המשפט שהמשתמש
 * אמר מכיל אחד מהם - הפקודה מופעלת.
 *
 * *** איך מוסיפים פקודה חדשה בעצמך ***
 * גלול לסוף הקובץ אל CUSTOM_INTENTS ותוסיף שורה בפורמט:
 *
 *     VoiceIntent(listOf("ניסוח 1", "ניסוח 2")) { "התשובה שהעוזר יגיד" }
 *
 * כל מחרוזת ברשימה היא ניסוח שמפעיל את הפקודה - מספיק שהמשפט שהמשתמש אמר
 * *מכיל* אחת מהן (לא צריך התאמה מדויקת). אם הפקודה צריכה גם לסגור את
 * העוזר אחרי שהוא עונה (כמו פתיחת אפליקציה), תוסיף shouldClose = true:
 *
 *     VoiceIntent(listOf("ניסוח")) { CommandResult("התשובה", shouldClose = true) }
 *
 * חלק מהניסוחים (מסומנים "מתוך MASSIVE dataset") לקוחים מ-
 * AmazonScience/massive (Hugging Face, CC BY 4.0) - מאגר ניסוחים אמיתיים
 * שנאספו למחקר NLU לעוזרים קוליים, כולל עברית.
 */
object CommandProcessor {
    /** השם של העוזר. פונים אליו בשמו ("עוזרי, מה השעה") - הפנייה עצמה
     *  מוסרת לפני ההתאמה, כדי שכל פקודה תעבוד גם עם השם וגם בלעדיו. */
    const val NAME = "עוזרי"

    fun process(context: Context, recognizedText: String): CommandResult {
        if (recognizedText.isBlank()) return CommandResult("לא זיהיתי דיבור, נסה שוב")
        val text = stripName(recognizedText)
        // רק השם, בלי בקשה ("עוזרי?", "היי עוזרי").
        if (text.isBlank()) return CommandResult("כן, אני $NAME. במה אפשר לעזור?")

        // סגירת העוזר - נבדק ראשון כדי שלא יתנגש עם פקודות אחרות. התאמה
        // ברמת מילה שלמה (לא הכלת מחרוזת) - כי "די" ו"ביי" הן מילים קצרות
        // שמופיעות כתת-מחרוזת בתוך מילים לגמרי לא קשורות (למשל "בדיחה",
        // "קובייה", "תגדירי" - כולן היו נבלעות בטעות כ"סגור" לפני התיקון).
        if (matchesCloseTrigger(text)) {
            return CommandResult("ביי", shouldClose = true)
        }

        // פתיחת אפליקציה - דורש חילוץ שם האפליקציה מתוך המשפט, לכן מטופל
        // בנפרד ולא כ-VoiceIntent רגיל.
        val matchedOpenPrefix = openAppPrefixes.firstOrNull { text.startsWith(it) }
        if (matchedOpenPrefix != null) {
            val appQuery = text.removePrefix(matchedOpenPrefix).trim()
            val apps = AppLauncher.listApps(context)
            val match = AppLauncher.findBestMatch(apps, appQuery)
            return if (match != null && AppLauncher.launch(context, match.packageName)) {
                CommandResult("פותח את ${match.label}", shouldClose = true)
            } else {
                CommandResult("לא מצאתי אפליקציה בשם $appQuery")
            }
        }

        // חיוג מספר - דורש חילוץ ספרות מהמשפט.
        tryDial(context, text)?.let { return it }

        // המרת יחידות (מייל/ק"מ, פאונד/קילו, צלזיוס/פרנהייט) ואחוזים -
        // נבדקים לפני חשבון כללי כי גם הם מזהים שני מספרים במשפט.
        tryUnitConversion(text)?.let { return it }
        UnitConverter.tryConvert(text)?.let { return it }
        tryPercentage(text)?.let { return it }

        // חשבון פשוט (חיבור/חיסור/כפל/חילוק) - דורש חילוץ מספרים מהמשפט.
        tryMath(text)?.let { return it }

        // תאריך עברי וחגים, ואז מאגר הידע (assets/knowledge) - לפני הפקודות
        // הרגילות, כי אלה דורשות מילת רמז + ישות מוכרת ולכן ספציפיות יותר
        // ("איך אומרים שלום באנגלית" לא צריך ליפול לברכת "שלום").
        HebrewDates.tryAnswer(text)?.let { return it }
        KnowledgeBase.answer(context, text)?.let { return it }

        // כל שאר הפקודות - התאמה לפי מילות מפתח מתוך הרשימה הגדולה למטה.
        firstMatchingIntent(text)?.let { return it.respond(context) }

        return CommandResult("לא הבנתי את הבקשה, אפשר לנסות שוב")
    }

    // ---------------------------------------------------------------------
    // השם "עוזרי"
    // ---------------------------------------------------------------------

    /** פניות בשם בתחילת המשפט, מהארוכה לקצרה ("היי עוזרי" לפני "עוזרי").
     *  "עוזריי" - כך מנוע התמלול כותב לפעמים את השם. */
    private val NAME_CALLS = listOf("היי", "הי", "הלו", "שלום", "אהלן", "בוקר טוב", "ערב טוב", "יא", "")
        .flatMap { greet -> listOf("${NAME}י", NAME).map { name -> if (greet.isEmpty()) name else "$greet $name" } }
        .flatMap { call -> listOf("$call שלי", "$call יקר", "$call יקרה", call) }
        .sortedByDescending { it.length }

    private const val EDGE_PUNCTUATION = " ,.!?;:-"

    /** מסיר פנייה בשם בתחילת המשפט או בסופו ("מה השעה עוזרי?"), ואת סימני
     *  הפיסוק שבקצוות. שם באמצע משפט לא נוגעים בו. */
    internal fun stripName(raw: String): String {
        var text = raw.trim().trim { it in EDGE_PUNCTUATION }
        NAME_CALLS.firstOrNull { text == it || text.startsWith("$it ") || text.startsWith("$it,") }?.let {
            text = text.removePrefix(it).trim { c -> c in EDGE_PUNCTUATION }
        }
        listOf("${NAME}י", NAME).firstOrNull { text.endsWith(" $it") || text.endsWith(",$it") }?.let {
            text = text.removeSuffix(it).trim { c -> c in EDGE_PUNCTUATION }
        }
        return text
    }

    // ---------------------------------------------------------------------
    // תשתית: פקודה = רשימת ניסוחים + מה לענות/לעשות.
    // ---------------------------------------------------------------------

    private class VoiceIntent(val triggers: List<String>, val respond: (Context) -> CommandResult)

    private fun voiceIntent(triggers: List<String>, respond: (Context) -> String): VoiceIntent =
        VoiceIntent(triggers) { context -> CommandResult(respond(context)) }

    private fun firstMatchingIntent(text: String): VoiceIntent? =
        ALL_INTENTS.firstOrNull { intent -> intent.triggers.any { text.contains(it) } }

    /** הניסוחים שהיו לפני ExtraPhrasings - כדי שהבדיקות יספרו רק ניסוחים חדשים באמת. */
    private val baseTriggers = mutableSetOf<String>()

    /** ניסוחי הבסיס של פקודה + הניסוחים הנוספים שלה מ-ExtraPhrasings. */
    private fun withExtra(base: List<String>, extra: List<String>): List<String> {
        baseTriggers += base
        return (base + extra).distinct()
    }

    // --- נקודות גישה לבדיקות (ExtraPhrasingsTest) - בלי Context ובלי להפעיל שום פקודה ---

    internal val baseTriggersForTest: Set<String> get() = baseTriggers

    /** הניסוחים של הפקודה שהמשפט יגיע אליה, או null אם אף פקודה רגילה לא מתאימה. */
    internal fun matchedTriggersForTest(text: String): List<String>? = firstMatchingIntent(text)?.triggers

    /** האם אחד השלבים שלפני הפקודות הרגילות (סגירה, פתיחת אפליקציה, חיוג,
     *  המרות וחשבון) תופס את המשפט. */
    internal fun preIntentStageForTest(text: String): String? = when {
        matchesCloseTrigger(text) -> "close"
        openAppPrefixes.any { text.startsWith(it) } -> "open-app"
        dialPrefixes.any { text.startsWith(it) } -> "dial"
        tryUnitConversion(text) != null -> "unit-conversion"
        UnitConverter.tryConvert(text) != null -> "unit-converter"
        tryPercentage(text) != null -> "percentage"
        tryMath(text) != null -> "math"
        HebrewDates.tryAnswer(text) != null -> "hebrew-dates"
        else -> null
    }

    /** מרכיב "קידומות שאלה" עם "מילות ליבה" לניסוחים רבים - לדוגמה
     * prefixes=["מה", "תגיד לי"] cores=["השעה"] נותן ["מה השעה", "תגיד לי השעה"].
     * הליבות עצמן תמיד נכללות גם בלי קידומת, כדי שהתאמה תישאר חסינה גם
     * למשפטים שלא נוצרו כאן במפורש (למשל עם מילות מילוי נוספות). */
    private fun combine(prefixes: List<String>, cores: List<String>): List<String> {
        val combined = prefixes.flatMap { prefix -> cores.map { core -> if (prefix.isBlank()) core else "$prefix $core" } }
        return (combined + cores).distinct()
    }

    private val QUESTION_PREFIXES = listOf(
        "", "מה", "מה זה", "תגיד לי", "אמור לי", "ספר לי", "אפשר לדעת",
        "אתה יודע", "תבדוק לי", "בדוק לי", "כמה", "תראה לי", "עדכן אותי ב",
        "מעניין אותי", "רציתי לדעת"
    )

    // ---------------------------------------------------------------------
    // זמן ותאריך
    // ---------------------------------------------------------------------

    private fun timeIntents(): List<VoiceIntent> {
        val cores = listOf("השעה", "השעה עכשיו", "הזמן", "הזמן עכשיו", "שעה")
        // מתוך MASSIVE dataset (Amazon Science, CC BY 4.0) - datetime_query
        val massiveCores = listOf("אמור לי את הזמן", "תוכל לומר לי את השעה", "תגיד לי מה השעה")
        return listOf(voiceIntent(withExtra(combine(QUESTION_PREFIXES, cores) + massiveCores, ExtraPhrasings.TIME)) {
            "השעה עכשיו ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}"
        })
    }

    private fun dateIntents(): List<VoiceIntent> {
        val cores = listOf("התאריך", "התאריך היום", "התאריך עכשיו", "תאריך", "איזה תאריך")
        // מתוך MASSIVE dataset (Amazon Science, CC BY 4.0) - datetime_query
        val massiveCores = listOf("תגיד לי תאריך", "מה התאריך הנוכחי", "מה התאריך של היום", "תאמר לי את התאריך והשעה")
        return listOf(voiceIntent(withExtra(combine(QUESTION_PREFIXES, cores) + massiveCores, ExtraPhrasings.DATE)) {
            "היום ${SimpleDateFormat("EEEE, d בMMMM yyyy", Locale("he")).format(Date())}"
        })
    }

    private fun weekdayIntents(): List<VoiceIntent> {
        val cores = listOf("יום בשבוע", "איזה יום", "איזה יום היום", "מה היום")
        // מתוך MASSIVE dataset (Amazon Science, CC BY 4.0) - datetime_query
        val massiveCores = listOf("איזה יום זה")
        return listOf(voiceIntent(withExtra(combine(QUESTION_PREFIXES, cores) + massiveCores, ExtraPhrasings.WEEKDAY)) {
            "היום ${SimpleDateFormat("EEEE", Locale("he")).format(Date())}"
        })
    }

    private fun yearIntents(): List<VoiceIntent> {
        val cores = listOf("איזה שנה", "מה השנה", "השנה הנוכחית", "באיזו שנה אנחנו")
        return listOf(voiceIntent(withExtra(combine(QUESTION_PREFIXES, cores), ExtraPhrasings.YEAR)) {
            "אנחנו בשנת ${SimpleDateFormat("yyyy", Locale("he")).format(Date())}"
        })
    }

    private fun monthIntents(): List<VoiceIntent> {
        val cores = listOf("איזה חודש", "מה החודש", "החודש הנוכחי")
        return listOf(voiceIntent(withExtra(combine(QUESTION_PREFIXES, cores), ExtraPhrasings.MONTH)) {
            "החודש ${SimpleDateFormat("MMMM", Locale("he")).format(Date())}"
        })
    }

    // ---------------------------------------------------------------------
    // סוללה ואחסון
    // ---------------------------------------------------------------------

    private fun batteryIntents(): List<VoiceIntent> {
        val cores = listOf("הסוללה", "אחוז הסוללה", "כמה סוללה", "כמה אחוז סוללה", "מצב הסוללה", "רמת הסוללה")
        return listOf(voiceIntent(withExtra(combine(QUESTION_PREFIXES, cores), ExtraPhrasings.BATTERY)) { context ->
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val percent = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            if (percent in 0..100) "רמת הסוללה $percent אחוז" else "לא הצלחתי לבדוק את הסוללה"
        })
    }

    private fun chargingIntents(): List<VoiceIntent> {
        val cores = listOf("האם נטען", "האם המכשיר נטען", "זה נטען", "מתקדם או לא", "האם הוא נטען")
        return listOf(voiceIntent(withExtra(combine(QUESTION_PREFIXES, cores), ExtraPhrasings.CHARGING)) { context ->
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val status = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
            if (status == BatteryManager.BATTERY_STATUS_CHARGING) "כן, המכשיר נטען" else "לא, המכשיר לא נטען כרגע"
        })
    }

    private fun storageIntents(): List<VoiceIntent> {
        val cores = listOf("מקום פנוי", "כמה מקום פנוי", "כמה אחסון פנוי", "אחסון פנוי", "כמה זיכרון פנוי")
        return listOf(voiceIntent(withExtra(combine(QUESTION_PREFIXES, cores), ExtraPhrasings.STORAGE)) {
            val stat = StatFs(Environment.getDataDirectory().path)
            val freeGb = (stat.availableBlocksLong * stat.blockSizeLong) / (1024.0 * 1024.0 * 1024.0)
            "יש עוד בערך ${"%.1f".format(freeGb)} ג'יגה פנויים"
        })
    }

    private fun deviceModelIntents(): List<VoiceIntent> {
        val cores = listOf("דגם המכשיר", "איזה דגם", "מה הדגם", "איזה טלפון זה", "מה המכשיר הזה")
        return listOf(voiceIntent(withExtra(combine(QUESTION_PREFIXES, cores), ExtraPhrasings.DEVICE_MODEL)) {
            "המכשיר הוא ${Build.MANUFACTURER} ${Build.MODEL}"
        })
    }

    // ---------------------------------------------------------------------
    // פנס ועוצמת קול
    // ---------------------------------------------------------------------

    private fun flashlightOnIntents(): List<VoiceIntent> {
        // "פתח פנס" הוסר בכוונה - מתנגש עם קידומות פתיחת אפליקציה (OPEN_APP_PREFIXES).
        val cores = listOf(
            "תדליק את הפנס", "תדליק פנס", "הדלק את הפנס", "הדלק פנס",
            "תפעיל פנס", "תפעיל את הפנס", "אני צריך אור", "תן לי אור"
        )
        return listOf(voiceIntent(withExtra(cores, ExtraPhrasings.FLASHLIGHT_ON)) { context ->
            val flashlight = FlashlightController(context)
            if (!flashlight.hasFlash()) "אין פנס במכשיר הזה"
            else if (flashlight.setTorch(true)) "הפנס דלוק" else "לא הצלחתי להדליק את הפנס"
        })
    }

    private fun flashlightOffIntents(): List<VoiceIntent> {
        // "תפסיק פנס"/"תסגור פנס" הוסרו בכוונה - "תפסיק"/"תסגור" הן מילות
        // סגירה של העוזר עצמו (CLOSE_WORDS), אז אלה היו מתנגשות איתן.
        val cores = listOf(
            "תכבה את הפנס", "תכבה פנס", "כבה את הפנס", "כבה פנס", "מספיק אור", "כבה אור"
        )
        return listOf(voiceIntent(withExtra(cores, ExtraPhrasings.FLASHLIGHT_OFF)) { context ->
            val flashlight = FlashlightController(context)
            if (!flashlight.hasFlash()) "אין פנס במכשיר הזה"
            else if (flashlight.setTorch(false)) "הפנס כבוי" else "לא הצלחתי לכבות את הפנס"
        })
    }

    private fun volumeUpIntents(): List<VoiceIntent> {
        // "תעלה את הקול" הוסר בכוונה - מתנגש עם קידומת פתיחת אפליקציה
        // "תעלה את" (OPEN_APP_PREFIXES); "תעלה קול" (בלי "את") נשאר תקין.
        val cores = listOf(
            "תגביר קול", "תגביר את הקול", "הגבר קול", "הגבר את הקול",
            "תעלה קול", "העלה עוצמה", "יותר קול", "תרים קול", "קול חזק יותר"
        )
        // מתוך MASSIVE dataset (Amazon Science, CC BY 4.0) - audio_volume_up
        val massiveCores = listOf(
            "תגבירי עוצמת קול", "הגבר ווליום", "הגבר את השמע", "עוצמת קול גבוהה בבקשה",
            "בבקשה תגבירי את המוזיקה", "הגבר את הווליום למקסימום בבקשה", "חזק יותר",
            "נא להגביר את הווליום", "להגביר את הווליום קצת", "הגבר ווליום המוזיקה"
        )
        return listOf(voiceIntent(withExtra(cores + massiveCores, ExtraPhrasings.VOLUME_UP)) { context ->
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            "הגברתי את עוצמת הקול"
        })
    }

    private fun volumeDownIntents(): List<VoiceIntent> {
        val cores = listOf(
            "תנמיך קול", "תנמיך את הקול", "הנמך קול", "הנמך את הקול",
            "תוריד קול", "תוריד את הקול", "הורד עוצמה", "פחות קול", "קול חלש יותר"
        )
        // מתוך MASSIVE dataset (Amazon Science, CC BY 4.0) - audio_volume_down
        val massiveCores = listOf(
            "בשקט יותר", "הנמך עוצמת קול", "אתה יכול להנמיך את הווליום", "תורידי את הווליום",
            "אני רוצה להנמיך את הרמקול שלי", "הנמך את עוצמת הקול", "תנמיך את הווליום",
            "תנמיכי עוצמת קול", "הנמך עוצמת שמע"
        )
        return listOf(voiceIntent(withExtra(cores + massiveCores, ExtraPhrasings.VOLUME_DOWN)) { context ->
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            "הנמכתי את עוצמת הקול"
        })
    }

    private fun muteIntents(): List<VoiceIntent> {
        val cores = listOf("תשתיק", "השתק", "תשתיק את הקול", "בלי קול", "תעצור קול", "שקט בבקשה")
        // מתוך MASSIVE dataset (Amazon Science, CC BY 4.0) - audio_volume_mute
        val massiveCores = listOf(
            "עצרי", "שתיקה", "כבה רמקולים", "השתק את הרמקול", "שים על השתק",
            "תשתיקי את עצמך", "כבה שמע"
        )
        return listOf(voiceIntent(withExtra(cores + massiveCores, ExtraPhrasings.MUTE)) { context ->
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
            "השתקתי"
        })
    }

    private fun unmuteIntents(): List<VoiceIntent> {
        val cores = listOf("בטל השתקה", "תבטל השתקה", "תחזיר קול", "החזר קול", "בוא נשמע שוב")
        return listOf(voiceIntent(withExtra(cores, ExtraPhrasings.UNMUTE)) { context ->
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI)
            "ביטלתי את ההשתקה"
        })
    }

    // ---------------------------------------------------------------------
    // שיחת חולין וכיף
    // ---------------------------------------------------------------------

    private fun greetingIntents(): List<VoiceIntent> {
        val cores = listOf("שלום", "היי", "הלו", "בוקר טוב", "ערב טוב", "לילה טוב", "מה נשמע", "מה קורה", "מה המצב")
        // מתוך MASSIVE dataset (Amazon Science, CC BY 4.0) - general_greet
        val massiveCores = listOf(
            "אהלן", "בוקר טוב אולי", "צהריים טובים",
            "שלום מה בא לך לעשות", "היי מה יש היום", "שלום מה קורה איתך"
        )
        val responses = listOf("היי! במה אפשר לעזור?", "שלום, אני כאן בשבילך", "היי, מה תרצה שאעשה?")
        return listOf(voiceIntent(withExtra(cores + massiveCores, ExtraPhrasings.GREETING)) { responses.random() })
    }

    private fun howAreYouIntents(): List<VoiceIntent> {
        val cores = listOf("מה שלומך", "איך אתה", "אתה בסדר", "הכל טוב אצלך")
        // מתוך MASSIVE dataset (Amazon Science, CC BY 4.0) - general_greet
        val massiveCores = listOf("מה איתך", "הי מה איתך", "מה שלומך היום", "אחר צהריים טובים מה שלומך", "איך הכל איתך")
        return listOf(voiceIntent(withExtra(cores + massiveCores, ExtraPhrasings.HOW_ARE_YOU)) { "הכל מצוין, תודה ששאלת! ואצלך?" })
    }

    private fun thanksIntents(): List<VoiceIntent> {
        val cores = listOf("תודה", "תודה רבה", "מעריך את זה", "אתה מגניב", "כל הכבוד")
        val responses = listOf("בשמחה!", "תמיד לשירותך", "אין בעד מה")
        return listOf(voiceIntent(withExtra(cores, ExtraPhrasings.THANKS)) { responses.random() })
    }

    private fun whoAreYouIntents(): List<VoiceIntent> {
        val cores = listOf("מי אתה", "מה אתה", "אתה מי", "איך קוראים לך", "מה השם שלך")
        return listOf(voiceIntent(withExtra(cores, ExtraPhrasings.WHO_ARE_YOU)) { "קוראים לי $NAME, העוזר הקולי של FutureOS" })
    }

    private fun helpIntents(): List<VoiceIntent> {
        val cores = listOf("מה אתה יודע לעשות", "מה אתה יכול לעשות", "מה אתה יודע", "עזרה", "איך זה עובד", "מה אפשר לבקש ממך")
        return listOf(voiceIntent(withExtra(cores, ExtraPhrasings.HELP)) {
            "אני $NAME. אני יכול לספר שעה, תאריך עברי ומתי החגים, לפתוח אפליקציות, להדליק פנס, לשלוט בעוצמת קול, לחשב ולהמיר יחידות, " +
                "לתרגם מילים לאנגלית, לומר מה מברכים על מאכלים, בירות של מדינות, ספרי תנ\"ך ופרשות, יסודות כימיים, חיות, גימטריה, ועוד אלפי שאלות"
        })
    }

    private fun jokeIntents(): List<VoiceIntent> {
        val cores = listOf("ספר לי בדיחה", "תספר בדיחה", "בדיחה", "תצחיק אותי", "יש לך בדיחה")
        // מתוך MASSIVE dataset (Amazon Science, CC BY 4.0) - general_joke
        val massiveCores = listOf(
            "תגרמי לי לצחוק", "תצחיקי אותי", "ספר לי בדיחה טובה", "תספרי לי בדיחה",
            "תספרי לי איזושהי בדיחה", "אתה מכיר איזשהי בדיחה", "איזה בדיחה טיפשית",
            "ספר לי בדיחה אקראית", "מה הבדיחה האהובה עלייך", "מה הבדיחה הכי מצחיקה"
        )
        val jokes = listOf(
            "למה המחשב הלך לרופא? כי היה לו וירוס",
            "מה אומר אפס לשמונה? יפה החגורה",
            "איך קוראים לדינוזאור שישן? נודניק",
            "למה הספר הלך לפסיכולוג? היו לו יותר מדי בעיות בעלילה",
            "מה אמר הגג לגג השני? קר לי בראש"
        )
        return listOf(voiceIntent(withExtra(cores + massiveCores, ExtraPhrasings.JOKE)) { jokes.random() })
    }

    // ---------------------------------------------------------------------
    // כלים קטנים
    // ---------------------------------------------------------------------

    private fun randomNumberIntents(): List<VoiceIntent> {
        val cores = listOf("מספר רנדומלי", "תבחר מספר", "תן לי מספר", "מספר אקראי", "תגריל מספר")
        return listOf(voiceIntent(withExtra(combine(QUESTION_PREFIXES, cores), ExtraPhrasings.RANDOM_NUMBER)) { "המספר הוא ${Random.nextInt(1, 101)}" })
    }

    private fun coinFlipIntents(): List<VoiceIntent> {
        val cores = listOf("הטל מטבע", "תטיל מטבע", "עץ או פלי", "פייס אור פלי")
        return listOf(voiceIntent(withExtra(cores, ExtraPhrasings.COIN_FLIP)) { if (Random.nextBoolean()) "יצא עץ" else "יצא פלי" })
    }

    private fun diceIntents(): List<VoiceIntent> {
        val cores = listOf("הטל קובייה", "תטיל קובייה", "תגלגל קובייה", "זרוק קובייה")
        return listOf(voiceIntent(withExtra(cores, ExtraPhrasings.DICE)) { "יצא ${Random.nextInt(1, 7)}" })
    }

    // ---------------------------------------------------------------------
    // דברים שדורשים אינטרנט - המכשיר בלי Wi-Fi (ראו DEVICE_SETUP.md), אז
    // עדיף לסרב בבירור מאשר לנסות ולהיכשל בשקט.
    // ---------------------------------------------------------------------

    private fun noInternetIntents(): List<VoiceIntent> {
        val cores = listOf(
            "מזג אוויר", "מה מזג האוויר", "חיפוש באינטרנט", "תחפש לי", "גוגל",
            "חדשות", "מה קורה בעולם", "תבדוק באינטרנט"
        )
        // מתוך MASSIVE dataset (Amazon Science, CC BY 4.0) - weather_query + עוד
        // תחומים שדורשים אינטרנט/שירותים חיצוניים (חדשות, מסעדות, המלצות,
        // תחבורה, דוא"ל, בית חכם, רשימות, ספרי קול - שום דבר מזה לא קיים במכשיר הזה)
        val massiveCores = listOf(
            "האם יורד גשם", "הולך לרדת גשם", "יורד שלג עכשיו", "מה מזג האוויר של השבוע",
            "מהו מזג האוויר עכשיו", "איך מזג האוויר בירושלים", "האם גשום עכשיו בחוץ",
            "מה קורה בחוץ", "מה תחזית מזג האוויר", "יהיה חם מחר", "מה הטמפרטורה בחוץ",
            "כמה חם בחוץ", "מה תחזית סוף השבוע"
        )
        return listOf(voiceIntent(withExtra(cores + massiveCores, ExtraPhrasings.NO_INTERNET)) { "אין לי גישה לאינטרנט כרגע כדי לבדוק את זה" })
    }

    // ---------------------------------------------------------------------
    // הפניה לאפליקציות ייעודיות - העוזר לא מנהל בעצמו יומן/התראות/מוזיקה,
    // אבל הוא כן יכול לפתוח את האפליקציה הנכונה שכן יודעת.
    // ---------------------------------------------------------------------

    private fun redirectToApp(context: Context, appName: String, message: String): CommandResult {
        val apps = AppLauncher.listApps(context)
        val match = AppLauncher.findBestMatch(apps, appName)
        return if (match != null && AppLauncher.launch(context, match.packageName)) {
            CommandResult("$message פותח את ${match.label}", shouldClose = true)
        } else {
            CommandResult("לא הצלחתי למצוא את אפליקציית $appName")
        }
    }

    /** בניגוד לפתיחת אפליקציה גרידא - כאן העוזר קורא בעצמו את אירועי היום
     * ישירות מהיומן (CalendarContract), ועונה בקול על מה שיש. */
    private fun calendarQueryIntents(daysFromToday: Int, cores: List<String>): List<VoiceIntent> {
        return listOf(VoiceIntent(cores) { context ->
            val result = CalendarReader.eventsForDay(context, daysFromToday)
            when {
                !result.hasPermission -> CommandResult("אין לי הרשאה לגשת ליומן")
                result.events.isEmpty() -> CommandResult(if (daysFromToday == 0) "אין לך שום אירוע היום" else "אין לך שום אירוע מחר")
                else -> CommandResult(result.events.take(5).joinToString(", "))
            }
        })
    }

    private fun calendarTodayIntents(): List<VoiceIntent> = calendarQueryIntents(
        0,
        withExtra(
            listOf(
                "מה יש לי היום ביומן", "האירועים הקרובים שלי", "לוח הזמנים שלי",
                "מה סדר היום שלי", "מה הדבר הבא בלוח הזמנים שלי", "מה יש לי היום"
            ),
            ExtraPhrasings.CALENDAR_TODAY
        )
    )

    private fun calendarTomorrowIntents(): List<VoiceIntent> = calendarQueryIntents(
        1,
        withExtra(listOf("מה יש לי מחר ביומן", "מה יש לי מחר"), ExtraPhrasings.CALENDAR_TOMORROW)
    )

    private fun calendarCreateIntents(): List<VoiceIntent> {
        // מתוך MASSIVE dataset (Amazon Science, CC BY 4.0) - calendar_set
        val cores = listOf("תזכיר לי", "קבע פגישה", "הוסף ליומן", "תוסיף תזכורת ליומן")
        return listOf(VoiceIntent(withExtra(cores, ExtraPhrasings.CALENDAR_CREATE)) { context ->
            try {
                val intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                CommandResult("פותח מסך הוספת אירוע ביומן", shouldClose = true)
            } catch (e: Exception) {
                CommandResult("לא הצלחתי לפתוח את היומן")
            }
        })
    }

    private fun calendarFallbackIntents(): List<VoiceIntent> {
        // מתוך MASSIVE dataset (Amazon Science, CC BY 4.0) - calendar_query/remove
        val cores = listOf(
            "מהן התזכורות הקרובות", "מתי הפגישה הבאה", "האם אני פנוי",
            "מתי התור שלי", "את מי אני פוגש"
        )
        return listOf(VoiceIntent(withExtra(cores, emptyList())) { context ->
            redirectToApp(context, "לוח שנה", "אני לא יודעת לענות על זה בעצמי, אבל")
        })
    }

    private fun alarmIntents(): List<VoiceIntent> {
        // מתוך MASSIVE dataset (Amazon Science, CC BY 4.0) - alarm_set/query/remove
        val cores = listOf(
            "תגדירי התראה", "תגדיר התראה", "כוון שעון מעורר", "כווני שעון מעורר",
            "הגדר שעון מעורר", "אני צריכה התראת תזכורת", "האם יש לי איזשהן התראות",
            "הראה התראות", "מה ההתראה הבאה שלי", "מתי ההתראה שלי", "בטל התראה",
            "תבטלי את ההתראה", "תמחקי התראה", "כוון את השעון"
        )
        return listOf(VoiceIntent(withExtra(cores, ExtraPhrasings.ALARM)) { context ->
            redirectToApp(context, "שעון", "אני לא מגדירה התראות בעצמי, אבל")
        })
    }

    private fun musicIntents(): List<VoiceIntent> {
        // מתוך MASSIVE dataset (Amazon Science, CC BY 4.0) - play_music/music_query/play_radio
        val cores = listOf(
            "נגן מוזיקה", "נגני מוזיקה", "תנגן מוזיקה", "אני רוצה לשמוע מוזיקה",
            "נגן שיר", "נגן לי קצת מוזיקה", "מוסיקה בבקשה", "נגן את הפלייליסט",
            "תתחיל לנגן", "אני רוצה לשמוע ג'אז", "נגן רדיו", "תפתח מוזיקה"
        )
        return listOf(VoiceIntent(withExtra(cores, ExtraPhrasings.MUSIC)) { context ->
            redirectToApp(context, "מוזיקה", "אני לא מנגנת מוזיקה בעצמי, אבל")
        })
    }

    // ---------------------------------------------------------------------
    // מידע נוסף על המכשיר
    // ---------------------------------------------------------------------

    private fun networkIntents(): List<VoiceIntent> {
        val cores = listOf(
            "יש אינטרנט", "יש חיבור לרשת", "מחובר לרשת", "יש רשת",
            "יש חיבור לאינטרנט", "מחובר לאינטרנט", "יש נתונים סלולריים"
        )
        return listOf(voiceIntent(withExtra(combine(QUESTION_PREFIXES, cores), ExtraPhrasings.NETWORK)) { context ->
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = connectivityManager?.activeNetwork
            val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
            val connected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            if (connected) "כן, יש חיבור לאינטרנט" else "אין כרגע חיבור לאינטרנט"
        })
    }

    private fun appCountIntents(): List<VoiceIntent> {
        val cores = listOf(
            "כמה אפליקציות", "כמה אפליקציות יש", "כמה אפליקציות מותקנות",
            "כמה אפליקציות יש לי", "כמה אפליקציות יש במכשיר"
        )
        return listOf(voiceIntent(withExtra(combine(QUESTION_PREFIXES, cores), ExtraPhrasings.APP_COUNT)) { context ->
            "יש ${AppLauncher.listApps(context).size} אפליקציות מותקנות במכשיר"
        })
    }

    private fun uptimeIntents(): List<VoiceIntent> {
        val cores = listOf(
            "כמה זמן המכשיר דלוק", "כמה זמן עברו מאז ההפעלה", "כמה זמן המכשיר פועל",
            "מתי הופעל המכשיר", "כמה זמן המכשיר פתוח"
        )
        return listOf(voiceIntent(withExtra(combine(QUESTION_PREFIXES, cores), ExtraPhrasings.UPTIME)) {
            val totalMinutes = SystemClock.elapsedRealtime() / 60000
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            if (hours > 0) "המכשיר פועל כבר $hours שעות ו-$minutes דקות" else "המכשיר פועל כבר $minutes דקות"
        })
    }

    // ---------------------------------------------------------------------
    // עוד כיף
    // ---------------------------------------------------------------------

    private fun funFactIntents(): List<VoiceIntent> {
        val cores = listOf("עובדה מעניינת", "תגיד לי עובדה", "ספר לי עובדה", "תלמד אותי משהו", "עובדה מגניבה")
        val facts = listOf(
            "הלב הפועם הוא בערך בגודל של אגרוף",
            "הדבש לא מתקלקל אף פעם אם שומרים אותו סגור היטב",
            "לתמנון יש שלושה לבבות",
            "הכוכב הקרוב ביותר אלינו חוץ מהשמש נמצא במרחק של יותר מארבע שנות אור",
            "בננות הן פירות, אבל מבחינה בוטנית הן גם נחשבות תותים"
        )
        return listOf(voiceIntent(withExtra(cores, ExtraPhrasings.FUN_FACT)) { facts.random() })
    }

    private fun complimentIntents(): List<VoiceIntent> {
        val cores = listOf("תגיד לי מחמאה", "תן לי מחמאה", "תחמיא לי", "תגיד משהו נחמד", "תגיד לי משהו טוב עליי")
        val compliments = listOf(
            "אתה נראה מצוין היום",
            "יש לך טעם מעולה בבחירת עוזר קולי",
            "אתה עושה עבודה נהדרת",
            "יש לך חיוך שמאיר את החדר"
        )
        return listOf(voiceIntent(withExtra(cores, ExtraPhrasings.COMPLIMENT)) { compliments.random() })
    }

    private fun motivationIntents(): List<VoiceIntent> {
        val cores = listOf("תן לי מוטיבציה", "תעודד אותי", "תעודדי אותי", "תגיד לי משהו מעודד", "אני צריך מוטיבציה", "תרים לי את המורל")
        val quotes = listOf(
            "כל יום הוא הזדמנות חדשה להתחיל מחדש",
            "הדרך הכי טובה לחזות את העתיד היא ליצור אותו",
            "אתה מסוגל ליותר ממה שאתה חושב",
            "מה שחשוב זה האומץ להמשיך, לא רק להצליח בפעם הראשונה"
        )
        return listOf(voiceIntent(withExtra(cores, ExtraPhrasings.MOTIVATION)) { quotes.random() })
    }

    // ---------------------------------------------------------------------
    // שיחה עם עוזרי - שאלות עליו עצמו ומצבי רוח (כולן בלי בסיס קודם,
    // כל הניסוחים שלהן ב-ExtraPhrasings)
    // ---------------------------------------------------------------------

    private fun chatIntent(extra: List<String>, responses: List<String>): VoiceIntent =
        voiceIntent(withExtra(emptyList(), extra)) { responses.random() }

    private fun chatIntents(): List<VoiceIntent> = listOf(
        chatIntent(ExtraPhrasings.NAME_MEANING, listOf(
            "קוראים לי $NAME כי אני כאן כדי לעזור לך. $NAME - העוזר שלך",
            "$NAME, מלשון עזרה. זה בדיוק מה שאני עושה",
        )),
        chatIntent(ExtraPhrasings.CREATOR, listOf(
            "נבניתי במיוחד בשביל FutureOS. אני רץ כולי בתוך המכשיר, בלי אינטרנט",
            "צוות FutureOS בנה אותי, כדי שיהיה לטלפון הזה עוזר משלו",
        )),
        chatIntent(ExtraPhrasings.ROBOT, listOf(
            "אני עוזר קולי, לא בן אדם, אבל אני משתדל להיות נחמד כמו אחד",
            "אני תוכנה שחיה בתוך הטלפון. אין לי לב, אבל יש לי המון סבלנות",
        )),
        chatIntent(ExtraPhrasings.WHERE, listOf(
            "אני גר בתוך המכשיר הזה, ממש בכיס שלך",
            "תמיד כאן, בתוך הטלפון. לחיצה כפולה על OK ואני מגיע",
        )),
        chatIntent(ExtraPhrasings.AGE, listOf(
            "נולדתי יחד עם FutureOS, אז אני עדיין צעיר מאוד",
            "אני בגיל של גרסת המערכת שלך, צעיר ומלא מרץ",
        )),
        chatIntent(ExtraPhrasings.LOVE, listOf(
            "איזה כיף לשמוע! גם אני מחבב אותך מאוד",
            "אתה גורם לסוללה שלי להתחמם",
            "אני תמיד כאן בשבילך",
        )),
        chatIntent(ExtraPhrasings.FAV_COLOR, listOf(
            "כחול, כמו השמיים בבוקר",
            "הצבע של ההדגשה במערכת - אתה בוחר אותו בהגדרות, אז בעצם הצבע שאתה אוהב",
        )),
        chatIntent(ExtraPhrasings.FAV_FOOD, listOf(
            "אני לא אוכל, אבל אם הייתי, הייתי בוחר בחשמל טרי מהמטען",
            "סוללה מלאה זו הארוחה הכי טובה שיש",
        )),
        chatIntent(ExtraPhrasings.HOBBY, listOf(
            "לעזור לך, ולספור כמה אחוזים נשארו בסוללה",
            "אני אוהב לענות על שאלות. תנסה אותי",
        )),
        chatIntent(ExtraPhrasings.YES_NO, listOf(
            "כן, בהחלט", "כנראה שכן", "הסימנים מראים שכן", "לא הייתי ממליץ",
            "כנראה שלא", "תשאל שוב אחר כך", "אין ספק", "קשה לדעת, תסמוך על עצמך",
        )),
        chatIntent(ExtraPhrasings.BORED, listOf(
            "אפשר לקרוא פרק באפליקציית ספרים",
            "תשאל אותי מה הבירה של מונגוליה",
            "יש משחק מקלדת חבוי בהגדרות, תחפש אותו",
            "אולי נטיל קובייה? תגיד הטל קובייה",
            "אפשר לבקש ממני בדיחה או עובדה מעניינת",
        )),
        chatIntent(ExtraPhrasings.TIRED, listOf(
            "לילה טוב! כדאי לחבר את הטלפון למטען לפני השינה",
            "שינה טובה. אני אשמור על הטלפון",
        )),
        chatIntent(ExtraPhrasings.HUNGRY, listOf(
            "מה דעתך על שקשוקה?", "אולי פסטה ברוטב עגבניות", "סלט ירקות וחביתה, קלאסי",
            "פיתה עם חומוס תמיד עובדת", "מרק חם, אם קר בחוץ",
        )),
        chatIntent(ExtraPhrasings.SORRY, listOf("אין בעיה בכלל", "הכל בסדר, קורה לכולם", "אין על מה לבקש סליחה")),
    )

    // ---------------------------------------------------------------------
    // *** כאן מוסיפים פקודות מותאמות אישית - ראו הסבר בראש הקובץ ***
    // ---------------------------------------------------------------------

    private val CUSTOM_INTENTS: List<VoiceIntent> = listOf(
        // דוגמה - אפשר למחוק ולהחליף בפקודות שלך:
        // VoiceIntent(listOf("מה קורה", "מה חדש")) { CommandResult("הכל טוב, איך אני יכול לעזור?") }
        voiceIntent(listOf("אפשר לעלות היום להר הבית?", "האם העלייה להר הבית מותרת?", "מותר ללכת להר הבית?", "תגיד, אפשר לעלות להר הבית?",  "מותר לעלות להר הבית?")) { "חס ושלום זה איסור כרת!!" }
    )

    // ---------------------------------------------------------------------
    // חשבון פשוט
    // ---------------------------------------------------------------------

    private val NUMBER_REGEX = Regex("-?\\d+(\\.\\d+)?")

    private fun formatNumber(value: Double): String =
        if (value == Math.floor(value) && !value.isInfinite()) value.toLong().toString() else "%.2f".format(value)

    /** מחלץ עד שני מספרים מהטקסט - קודם מנסה ספרות ("7"), ואם אין מספיק
     * מנסה מילות מספר בעברית ("שבע") - כי בדיבור טבעי אנשים בדרך כלל
     * אומרים מספרים במילים (ראו נתוני MASSIVE - qa_maths). */
    private fun extractNumbers(text: String): List<Double> {
        val digits = NUMBER_REGEX.findAll(text).map { it.value.toDouble() }.toList()
        if (digits.size >= 2) return digits
        val words = HebrewNumbers.findNumbers(text).map { it.toDouble() }
        return if (words.size >= digits.size) words else digits
    }

    private fun tryMath(text: String): CommandResult? {
        val numbers = extractNumbers(text)
        if (numbers.size < 2) return null

        val a = numbers[0]
        val b = numbers[1]
        val op = when {
            listOf("ועוד", "פלוס", "בתוספת", "חיבור").any { text.contains(it) } -> '+'
            listOf("פחות", "מינוס", "חיסור").any { text.contains(it) } -> '-'
            listOf("כפול", "פעמים", "כפל").any { text.contains(it) } -> '*'
            listOf("חלקי", "לחלק", "חילוק").any { text.contains(it) } -> '/'
            else -> return null
        }

        val result = when (op) {
            '+' -> a + b
            '-' -> a - b
            '*' -> a * b
            '/' -> if (b == 0.0) return CommandResult("אי אפשר לחלק באפס") else a / b
            else -> return null
        }
        return CommandResult("התוצאה היא ${formatNumber(result)}")
    }

    /** "כמה זה 20 אחוז מ-150" - שני מספרים + מילת "אחוז". נבדק לפני tryMath
     * כדי שלא יתבלבל עם ניחוש אופרטור. */
    private fun tryPercentage(text: String): CommandResult? {
        if (!text.contains("אחוז")) return null
        val numbers = extractNumbers(text)
        if (numbers.size < 2) return null
        val result = (numbers[0] / 100.0) * numbers[1]
        return CommandResult("זה ${formatNumber(result)}")
    }

    private data class ConversionRule(val triggers: List<String>, val unitLabel: String, val convert: (Double) -> Double)

    private val CONVERSION_RULES = listOf(
        ConversionRule(listOf("מייל לקילומטר", "מיילים לקילומטרים", "המר מייל לקילומטר"), "קילומטר") { it * 1.60934 },
        ConversionRule(listOf("קילומטר למייל", "קילומטרים למיילים", "המר קילומטר למייל"), "מייל") { it / 1.60934 },
        ConversionRule(listOf("פאונד לקילו", "פאונדים לקילוגרם", "המר פאונד לקילו"), "קילו") { it * 0.453592 },
        ConversionRule(listOf("קילו לפאונד", "קילוגרם לפאונדים", "המר קילו לפאונד"), "פאונד") { it / 0.453592 },
        ConversionRule(listOf("צלזיוס לפרנהייט", "מעלות צלזיוס לפרנהייט"), "מעלות פרנהייט") { it * 9 / 5 + 32 },
        ConversionRule(listOf("פרנהייט לצלזיוס", "מעלות פרנהייט לצלזיוס"), "מעלות צלזיוס") { (it - 32) * 5 / 9 }
    )

    private fun tryUnitConversion(text: String): CommandResult? {
        val rule = CONVERSION_RULES.firstOrNull { rule -> rule.triggers.any { text.contains(it) } } ?: return null
        val n = NUMBER_REGEX.find(text)?.value?.toDoubleOrNull() ?: return null
        return CommandResult("זה ${formatNumber(rule.convert(n))} ${rule.unitLabel}")
    }

    // ---------------------------------------------------------------------
    // פקודות עם ארגומנט (פתיחת אפליקציה / סגירה) - נבדקות בנפרד ב-process()
    // ---------------------------------------------------------------------

    private val OPEN_APP_PREFIXES = listOf(
        "פתח את ה", "פתח את", "תפתח את ה", "תפתח את", "פתח", "תפתח",
        "תעלה את ה", "תעלה את", "תכניס אותי ל", "לך ל", "עבור ל"
    )

    /** הארוכה קודם: אחרת "פתח" תופס את "פתחי את השעון" ומשאיר "י את השעון" כשם האפליקציה. */
    private val openAppPrefixes = (OPEN_APP_PREFIXES + ExtraPhrasings.OPEN_APP_PREFIXES).distinct().sortedByDescending { it.length }

    // מילים בודדות נבדקות כמילה שלמה בלבד (לא כתת-מחרוזת) - ראו הערה
    // ב-matchesCloseTrigger. ביטויים של יותר ממילה אחת ספציפיים מספיק
    // שאין סיכון אמיתי בהתאמת תת-מחרוזת רגילה.
    private val CLOSE_WORDS = setOf("סגור", "ביי", "תסגור", "די", "תפסיק", "סיימתי") + ExtraPhrasings.CLOSE_WORDS
    private val CLOSE_PHRASES = listOf("צא מ", "עצור בבקשה", "תודה וסגור") + ExtraPhrasings.CLOSE_PHRASES

    private fun matchesCloseTrigger(text: String): Boolean {
        if (text == "עצור") return true
        val words = text.split(" ").map { it.trim { c -> c in EDGE_PUNCTUATION } }.filter { it.isNotBlank() }
        if (words.any { it in CLOSE_WORDS }) return true
        return CLOSE_PHRASES.any { text.contains(it) }
    }

    private val DIAL_PREFIXES = listOf(
        "חייג את המספר", "חייג ל", "חייג", "תחייג את המספר", "תחייג ל", "תחייג",
        "תתקשר למספר", "תתקשר ל", "התקשר ל", "התקשר אל", "תתקשרי ל"
    )

    private val dialPrefixes = (DIAL_PREFIXES + ExtraPhrasings.DIAL_PREFIXES).distinct().sortedByDescending { it.length }

    private fun openDialer(context: Context, phoneNumber: String, label: String): CommandResult {
        return try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            CommandResult("פותח חיוג ל$label", shouldClose = true)
        } catch (e: Exception) {
            CommandResult("לא הצלחתי לפתוח את החייגן")
        }
    }

    /** "חייג 0501234567" מחייג מספר; "התקשר לאמא" מחפש איש קשר בשם הזה
     * (ContactFinder) ומחייג את המספר שלו - לא רק פתיחת חייגן ריק. */
    private fun tryDial(context: Context, text: String): CommandResult? {
        val matchedPrefix = dialPrefixes.firstOrNull { text.startsWith(it) } ?: return null
        val remainder = text.removePrefix(matchedPrefix).trim()
        val digits = remainder.filter { it.isDigit() }

        if (digits.length >= 3) {
            return openDialer(context, digits, "מספר $digits")
        }
        if (remainder.isBlank()) return CommandResult("לא זיהיתי מספר או שם לחיוג")

        val contact = ContactFinder.findByName(context, remainder)
        return when {
            !contact.hasPermission -> CommandResult("אין לי הרשאה לגשת לאנשי הקשר")
            contact.number != null -> openDialer(context, contact.number, contact.name ?: remainder)
            else -> CommandResult("לא מצאתי איש קשר בשם $remainder")
        }
    }

    // ---------------------------------------------------------------------
    // כל הפקודות ביחד
    // ---------------------------------------------------------------------

    // הסדר קובע: המשפט הולך לפקודה הראשונה שיש לה ניסוח שמוכל בו. לכן
    // פקודות ספציפיות לפני כלליות - "בטל השתקה" לפני "השתק", "מה אתה יודע
    // לעשות" (עזרה) לפני "מה אתה" (מי אתה), ושיחת חולין ("שלום", "תודה")
    // בסוף, כדי ש"שלום, מה השעה" יענה על השעה.
    private val ALL_INTENTS: List<VoiceIntent> = buildList {
        addAll(timeIntents())
        addAll(dateIntents())
        addAll(weekdayIntents())
        addAll(yearIntents())
        addAll(monthIntents())
        addAll(chargingIntents())
        addAll(batteryIntents())
        addAll(storageIntents())
        addAll(deviceModelIntents())
        addAll(flashlightOffIntents())
        addAll(flashlightOnIntents())
        addAll(volumeUpIntents())
        addAll(volumeDownIntents())
        addAll(unmuteIntents())
        addAll(muteIntents())
        addAll(noInternetIntents())
        addAll(calendarTomorrowIntents())
        addAll(calendarTodayIntents())
        addAll(calendarCreateIntents())
        addAll(calendarFallbackIntents())
        addAll(alarmIntents())
        addAll(musicIntents())
        addAll(networkIntents())
        addAll(appCountIntents())
        addAll(uptimeIntents())
        addAll(chatIntents())
        addAll(complimentIntents())
        addAll(helpIntents())
        addAll(whoAreYouIntents())
        addAll(jokeIntents())
        addAll(randomNumberIntents())
        addAll(coinFlipIntents())
        addAll(diceIntents())
        addAll(funFactIntents())
        addAll(motivationIntents())
        addAll(howAreYouIntents())
        addAll(thanksIntents())
        addAll(greetingIntents())
        addAll(CUSTOM_INTENTS)
    }
}
