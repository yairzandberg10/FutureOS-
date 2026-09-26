package com.future.keyboard

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.future.sharednav.theme.ThemeClient

/**
 * שירות שיטת קלט (IME) אמיתי: המכשיר הוא מקלדת T9 פיזית בלי מסך מגע (ראו
 * ההערות המקבילות ב-dialer וב-FutureUI), ולכן אין כאן מקלדת מגע על המסך -
 * במקום זאת השירות מיירט את לחיצות מקשי הספרות הפיזיים (onKeyDown) ומתרגם
 * אותן לטקסט אמיתי לפי [T9Engine], בדיוק כמו בטלפונים פיזיים ישנים.
 *
 * מקשי הבקרה (ראו [InputMode] ו-[onKeyDown]):
 * - ספרות 2-9: אותיות T9 (לפי [T9DigitMap][com.future.sharednav.t9.T9DigitMap]) או ניבוי מילון.
 * - חצים (כל 4 הכיוונים): מעבר בין מועמדות הניבוי כשיש יותר ממילה אחת מתאימה.
 * - # קצר: מעביר בין מצבי הקלט במחזור קבוע - עברית (ללא ניבוי) → עברית
 *   (עם ניבוי) → אנגלית ABC → אנגלית Abc → אנגלית abc → מספרים - ובחזרה.
 * - * קצר: פותח תפריט סימני פיסוק (ניווט בחצים, אישור במרכז, ביטול בחזור/מחיקה).
 *   בתוכו 1 פותח את הלוח: העתק, גזור, הדבק, בחר הכל, והעתקות אחרונות.
 * - 0 קצר: רווח. 0 ארוך (מוחזק): תמלול קולי.
 * - מחיקה (DEL): מוחקת אות אחרונה מהמילה בהרכבה, ואז תווים מהטקסט שכבר הוצב.
 * - Options (מקש פיזי, דרך שידור גלובלי - ר' optionsKeyReceiver): פותח/סוגר את
 *   הלוח. כשבהרכבה מילה בלי התאמה במילון - מוסיף אותה לניבוי במקום.
 * - ניבוי הטקסט ניתן לכיבוי/הדלקה ממרכז הבקרה של FutureUI (ר' KeyboardSettingsProvider).
 */
class KeyboardService : InputMethodService() {

    private enum class InputMode {
        HEBREW_MULTITAP, HEBREW_PREDICTIVE, ENGLISH_UPPER, ENGLISH_CAPITALIZE, ENGLISH_LOWER,
        SPANISH, FRENCH, GERMAN, ITALIAN, PORTUGUESE, DUTCH, POLISH, TURKISH,
        ROMANIAN, CZECH, SWEDISH, NORWEGIAN, DANISH, FINNISH, RUSSIAN,
        NUMERIC;

        companion object {
            // # קצר עדיין עובר על כל הרשימה אחד-אחד (התנהגות קיימת, לא שוברים אותה),
            // אבל עם 15 שפות נוספות זו דרך איטית להגיע לשפה ספציפית - ר' openLanguageMenu
            // (החזקת # ארוכה) לגישה ישירה בלי לעבור על כל הרשימה.
            val CYCLE_ORDER = listOf(
                HEBREW_MULTITAP, HEBREW_PREDICTIVE, ENGLISH_UPPER, ENGLISH_CAPITALIZE, ENGLISH_LOWER,
                SPANISH, FRENCH, GERMAN, ITALIAN, PORTUGUESE, DUTCH, POLISH, TURKISH,
                ROMANIAN, CZECH, SWEDISH, NORWEGIAN, DANISH, FINNISH, RUSSIAN,
                NUMERIC,
            )
        }
    }

    companion object {
        // רשימת סימני הפיסוק הזמינים בתפריט * - אין ייצוג ויזואלי שלהם על המקשים
        // עצמם, ולכן הם מרוכזים בתפריט אחד שנפתח בלחיצה קצרה על הכוכבית.
        private val PUNCTUATION_SYMBOLS = listOf(
            '.', ',', '?', '!', '\'', '"', '-', '@', '/', ':', ';', '(', ')',
            '+', '=', '_', '%', '&', '*', '#', '$', '€', '₪', '~', '\\', '|',
            '<', '>', '[', ']', '{', '}', '^', '`'
        )
        // מספר העמודות ברשת תפריט הפיסוק - קבוע כדי שהניווט האנכי (חצים
        // למעלה/למטה) יוכל לחשב שורה/עמודה מהאינדקס השטוח ברשימה.
        private const val PUNCTUATION_COLUMNS = 6

        // תקרה על מספר המועמדות המוצגות בו-זמנית בשורת הניבוי - מילון שמחזיר
        // עשרות התאמות לרצף ספרות קצר לא אמור לנפח את השורה לאינסוף. "חלון"
        // נגלל סביב המועמדת הנבחרת (ראו renderCandidateChips) כדי שגם מועמדת
        // מעבר לתקרה תמיד תהיה נגישה בלחיצת חץ.
        private const val MAX_VISIBLE_CANDIDATES = 12

        // אם אחרי שחרור 0 התמלול לא החזיר תוצאה בזמן הזה - סוגרים בכל זאת,
        // כדי שהפאנל לא יישאר תקוע במצב "מתמלל". Whisper המקומי (Assistant)
        // מתמלל אחרי השחרור ועל המכשיר זה לוקח יותר מ-9 שניות למשפט ארוך -
        // ה-timeout הקודם חתך תמלולים תקינים באמצע.
        private const val VOICE_RESULT_TIMEOUT_MS = 40_000L

        private const val ASSISTANT_PACKAGE = "com.future.assistant"

        private const val MAX_CLIP_HISTORY = 8
        /** העתקות ישנות נמחקות מההיסטוריה אחרי 10 דקות - סיסמה שהועתקה פעם
         *  לא אמורה לחכות בלוח שעות לכל מי שמרים את המכשיר. */
        private const val CLIP_TTL_MS = 10 * 60_000L
        /** ClipDescription.EXTRA_IS_SENSITIVE (API 33) - כמחרוזת כי minSdk הוא 31. */
        private const val EXTRA_CLIP_SENSITIVE = "android.content.extra.IS_SENSITIVE"
        private const val MAX_FIELD_CHARS = 100_000
    }

    private lateinit var prefs: SharedPreferences

    // מילון עברי מלא (ראו HebrewDictionaryDb) - נטען פעם אחת ב-onCreate ומוזן
    // לכל T9Engine עברי שנוצר לאחר מכן (גם במעברי מצב), כדי שהעתקת/פתיחת
    // ה-DB לא תקרה בכל לחיצת #. @Volatile כי ההקצאה קורית ב-thread ברקע
    // (ראו onCreate) והקריאה ב-buildEngine קורית ב-thread הראשי.
    @Volatile
    private var hebrewDb: HebrewDictionaryDb? = null
    private var engine: T9Engine = T9Engine(T9Engine.Language.HEBREW, buildIndexInBackground = true)

    // כשה-DB עוד לא מוכן (null) מחזירים null, לא רשימה ריקה - כדי ש-T9Engine
    // ייפול חזרה למילון הפנימי הקטן במקום להציג "אין ניבוי" בחלון הקצר של
    // ההעתקה הראשונית (ראו hebrewDb ו-externalCandidates).
    private fun buildEngine(language: T9Engine.Language): T9Engine =
        if (language == T9Engine.Language.HEBREW) {
            T9Engine(language, buildIndexInBackground = true) { digits -> hebrewDb?.candidatesFor(digits) }
        } else {
            T9Engine(language, buildIndexInBackground = true)
        }

    // רצף הספרות שנלחצו עד כה - מפתח החיפוש במילון (מיקום אחד לכל אות, בלי
    // תלות באיזו אות נבחרה בפועל בתוך אותו מקש).
    private var digitSequence: String = ""
    private var candidateIndex: Int = 0
    private var candidates: List<String> = emptyList()
    private var isPredictiveField = true
    // שדה פרטי: סיסמה/PIN, או שהאפליקציה ביקשה IME_FLAG_NO_PERSONALIZED_LEARNING
    // (למשל גלישה בסתר). בשדה כזה המקלדת לא לומדת מילים, לא שומרת העתקות
    // להיסטוריה ולא מחממת את מנוע הדיבור.
    private var isPrivateField = false

    // מצב multi-tap (כשאין התאמה במילון): האות שנבחרה בפועל בכל מיקום (מקביל
    // ל-digitSequence), האינדקס הנוכחי בתוך אותיות המקש האחרון, וזמן הלחיצה
    // האחרונה כדי לזהות "לחיצה חוזרת מהירה על אותו מקש" מול תחילת אות חדשה.
    private val fallbackLetters = StringBuilder()
    private var lastTapDigit: Char? = null
    private var lastTapLetterIndex: Int = 0
    private var lastTapTimeMillis: Long = 0L
    private val multiTapTimeoutMillis = 900L

    // כמה פעמים המשתמש עצמו כבר בחר/הקליד כל מילה - נטען/נשמר לכל שפה בנפרד,
    // ומשמש להטיית סדר המועמדים (candidatesFor) לכיוון מילים שכבר נלמדו בפועל.
    private var wordFrequency: MutableMap<String, Int> = mutableMapOf()

    // מילים שהמשתמש הוסיף בעצמו לניבוי (מקש Options כשאין התאמה במילון - ר'
    // addCurrentWordToDictionary) - digitSequence -> מילים, לכל שפה בנפרד. בניגוד
    // ל-wordFrequency (רק מסדר מחדש מילים שכבר במילון), אלה מילים שלא קיימות
    // במילון המובנה בכלל, אז הן ממוזגות בנפרד ב-refreshComposing.
    private var customWords: MutableMap<String, MutableList<String>> = mutableMapOf()

    // נקרא פעם אחת ב-onStartInput (לא בכל לחיצה) כדי לא לשלם עלות IPC של
    // ContentResolver.query על כל הקשה - נסגר/נפתח דרך המתג במרכז הבקרה של
    // FutureUI (ר' KeyboardSettingsProvider), נכנס לתוקף בכניסה הבאה לשדה קלט.
    private var predictiveEnabled = true

    // השפות שבהן מקלידים (# ותפריט השפה עוברים רק עליהן) והשפות שבהן יש ניבוי -
    // נשלטות מההגדרות וממרכז הבקרה (ר' KeyboardLanguages / KeyboardSettingsProvider).
    private var enabledLanguages: Set<T9Engine.Language> = emptySet()
    private var predictionLanguages: Set<T9Engine.Language> = emptySet()

    private fun loadLanguageSettings() {
        enabledLanguages = KeyboardLanguages.enabled(prefs)
        predictionLanguages = KeyboardLanguages.predicted(prefs)
    }

    /** המצבים ש-# עובר עליהם: רק שפות פעילות, ועברית עם ניבוי רק אם הוא מופעל לה. */
    private fun activeCycle(): List<InputMode> = InputMode.CYCLE_ORDER.filter { mode ->
        val lang = engineLanguageFor(mode) ?: return@filter true
        when {
            lang !in enabledLanguages -> false
            mode == InputMode.HEBREW_PREDICTIVE -> lang in predictionLanguages
            else -> true
        }
    }

    private fun isPredictingIn(mode: InputMode): Boolean {
        val lang = engineLanguageFor(mode) ?: return false
        return predictiveEnabled && mode != InputMode.HEBREW_MULTITAP && lang in predictionLanguages
    }

    /** אם השפה השמורה כובתה בהגדרות - עוברים לשפה הפעילה הראשונה. */
    private fun ensureModeIsActive() {
        val cycle = activeCycle()
        val mode = currentMode()
        if (mode !in cycle) {
            val next = cycle.firstOrNull() ?: InputMode.NUMERIC
            prefs.edit().putString("input_mode", next.name).apply()
            engineLanguageFor(next)?.let { engine = buildEngine(it); loadFrequencies(); loadCustomWords() }
        }
    }

    // תפריט סימני הפיסוק (מקש * קצר): פתוח/סגור והאינדקס הנבחר כרגע בתוכו.
    private var isPunctuationMenuOpen = false
    private var punctuationIndex = 0

    // תפריט בחירת שפה (החזקת # ארוכה) - גישה ישירה לכל שפה בלי לעבור עליהן
    // אחת-אחת עם # קצר (ר' InputMode.CYCLE_ORDER, שגדל מאוד עם 15 השפות החדשות).
    private var isLanguageMenuOpen = false
    private var languageMenuIndex = 0
    private var poundArmedForLanguageMenu = false

    // הלוח (1 בתוך תפריט הפיסוק): פעולות העתק/גזור/הדבק על השדה, ומתחתן
    // ההעתקות האחרונות להדבקה. ההיסטוריה בזיכרון בלבד - היא יכולה להכיל
    // סיסמאות, ולכן לא נשמרת לדיסק ונעלמת כשהמקלדת נסגרת.
    private var isClipboardOpen = false
    private var clipboardIndex = 0
    private val clipHistory = ArrayDeque<String>()
    private val clipboardManager by lazy { getSystemService(android.content.ClipboardManager::class.java) }
    private val clipListener = android.content.ClipboardManager.OnPrimaryClipChangedListener { rememberCurrentClip() }

    // חלקי פאנל המקלדת (ר' onCreateInputView). כל ארבעת המצבים בנויים מראש
    // ומוחלפים בהצגה/הסתרה (showOnly), ולא נבנים מחדש בכל מעבר מצב.
    private lateinit var panelRoot: LinearLayout
    private lateinit var typeRail: LinearLayout
    private lateinit var langBadge: TextView
    private lateinit var modeTagView: TextView
    private lateinit var digitsView: TextView
    private lateinit var candidateCounter: TextView
    private lateinit var candidatesScroll: HorizontalScrollView
    private lateinit var candidatesRow: LinearLayout
    private lateinit var emptyHintView: TextView
    private lateinit var punctuationRail: LinearLayout
    private lateinit var punctuationPreview: TextView
    private lateinit var punctuationCounter: TextView
    private lateinit var punctuationGrid: GridLayout
    private lateinit var languageRail: LinearLayout
    private lateinit var languageCounter: TextView
    private lateinit var languageMenuList: LinearLayout
    private lateinit var languageMenuScroll: ScrollView
    private lateinit var clipboardRail: LinearLayout
    private lateinit var clipboardCounter: TextView
    private lateinit var clipboardList: LinearLayout
    private lateinit var clipboardScroll: ScrollView
    private lateinit var voiceRow: LinearLayout
    private lateinit var micView: ImageView
    private lateinit var voiceTitle: TextView
    private lateinit var voiceSubtitle: TextView
    private lateinit var voiceWave: VoiceWaveView
    private lateinit var legendBar: LinearLayout
    private lateinit var legendRow: LinearLayout

    // הודעה חד-פעמית שמוצגת במקום שורת המועמדות (למשל "אין הרשאת מיקרופון") -
    // נמחקת בלחיצת המקש הבאה, ר' onKeyDown.
    private var panelMessage: String? = null

    private var textColor: Int = Color.BLACK
    private var mutedTextColor: Int = Color.DKGRAY
    private var faintTextColor: Int = Color.GRAY
    private var accentColor: Int = Color.BLUE
    private var backgroundColor: Int = 0xFFEFEFEF.toInt()
    private var chipBackgroundColor: Int = Color.WHITE
    private var elevatedColor: Int = 0xFFE5E5EA.toInt()
    private var hairlineColor: Int = 0x1F000000

    // צבע ההדגשה כפי שהוא משמש בפועל בפאנל, וצבע הדיו שמונח עליו - שניהם
    // נגזרים מ-primary_color של המערכת ב-applyTheme (ר' KeyboardPalette).
    private var selectionColor: Int = Color.BLUE
    private var onSelectionColor: Int = Color.WHITE

    // תמלול קולי בהחזקת מקש 0: voiceInputArmed מונע הפעלה חוזרת של ההאזנה כל
    // עוד המקש נשאר לחוץ (Android שולח onKeyDown חוזר ונשנה עם repeatCount
    // עולה כל עוד המקש מוחזק).
    private val longPressRepeatThreshold = 6
    private var voiceInputArmed = false
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    // לחיצה קצרה על 0 (רווח / הספרה 0) מבוצעת בשחרור ולא בלחיצה - אחרת כל
    // החזקה לתמלול הייתה מכניסה קודם רווח מיותר לפני הטקסט המתומלל.
    private var zeroPending = false

    // אחרי שחרור 0: ההקלטה נעצרה והמנוע מסיים לתמלל. הפאנל מראה "מתמלל…"
    // ונסגר מעצמו כשמגיעה תוצאה, שגיאה, או אחרי VOICE_RESULT_TIMEOUT_MS.
    private var isVoiceProcessing = false
    private var voicePartial: String? = null
    private val voiceHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val voiceTimeout = Runnable {
        if (isListening) {
            stopListening()
            showPanelMessage("התמלול לא הסתיים - נסו שוב")
        }
    }

    // מקש Options הפיזי נחסם ברמת המערכת (StatusBarAccessibilityService של
    // FutureUI צורך אותו) ולעולם לא מגיע ל-onKeyDown כאן - בדיוק כמו בכל שאר
    // האפליקציות בסוויטה, הדרך האמיתית שהוא עובד היא האזנה לשידור הגלובלי.
    // כאן הוא פותח/סוגר את הלוח (ר' onOptionsShortPress).
    private val optionsKeyReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            onOptionsShortPress()
        }
    }

    // * ו-# נחסמים ברמת המערכת בדיוק כמו מקש Options: מרכז הבקרה של FutureUI
    // תופס את * ומרכז ההתראות תופס את #, ושניהם מחזירים true גם ללחיצה קצרה,
    // כך שהמקשים האלה לא הגיעו ל-onKeyDown כאן בכלל והתפריטים שהמקרא בתחתית
    // הפאנל מבטיח פשוט לא נפתחו. שני השירותים משדרים עכשיו את הלחיצה הקצרה
    // (ר' FutureUIActions), וזו הדרך שבה המקלדת מקבלת אותה בפועל על המכשיר.
    private val starKeyReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            onStarShortPress()
        }
    }

    private val poundKeyReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            onPoundShortPress()
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("t9_keyboard_prefs", MODE_PRIVATE)
        engineLanguageFor(currentMode())?.let { engine = buildEngine(it) }
        loadFrequencies()
        loadCustomWords()
        predictiveEnabled = prefs.getBoolean("predictive_enabled", true)
        loadLanguageSettings()
        // בפעם הראשונה בלבד, פתיחת HebrewDictionaryDb מעתיקה קובץ של מאות MB
        // מה-assets לאחסון הפנימי - רצה ברקע כדי לא לחסום את onCreate. עד
        // שהיא מסתיימת, buildEngine נופל חזרה למילון הפנימי הקטן.
        //
        // ה-try/catch הוא העיקר כאן: בלעדיו, כשל בהעתקה (דיסק מלא, asset
        // פגום) מפיל את תהליך ה-IME כולו - ובמכשיר בלי מסך מגע, אובדן
        // ה-IME הוא אובדן כל יכולת הקלט. הניבוי פשוט נשאר על המילון
        // הפנימי הקטן, והמשתמש מקבל הודעה בשורת המועמדות במקום כלום.
        Thread {
            try {
                hebrewDb = HebrewDictionaryDb(this)
            } catch (e: Throwable) {
                android.util.Log.e("KeyboardService", "Hebrew dictionary unavailable", e)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    panelMessage = "המילון המלא לא נטען - ניבוי מצומצם"
                }
            }
        }.start()

        registerSystemKeyReceiver(optionsKeyReceiver, com.future.sharednav.actions.FutureUIActions.ACTION_OPTIONS_SHORT_PRESS)
        registerSystemKeyReceiver(starKeyReceiver, com.future.sharednav.actions.FutureUIActions.ACTION_STAR_SHORT_PRESS)
        registerSystemKeyReceiver(poundKeyReceiver, com.future.sharednav.actions.FutureUIActions.ACTION_POUND_SHORT_PRESS)
        runCatching { clipboardManager.addPrimaryClipChangedListener(clipListener) }
    }

    private fun registerSystemKeyReceiver(receiver: android.content.BroadcastReceiver, action: String) {
        val filter = android.content.IntentFilter(action)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(receiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseRecognizer()
        unregisterReceiver(optionsKeyReceiver)
        unregisterReceiver(starKeyReceiver)
        unregisterReceiver(poundKeyReceiver)
        runCatching { clipboardManager.removePrimaryClipChangedListener(clipListener) }
    }

    private fun currentMode(): InputMode {
        val stored = prefs.getString("input_mode", InputMode.HEBREW_PREDICTIVE.name)
        return try { InputMode.valueOf(stored ?: InputMode.HEBREW_PREDICTIVE.name) } catch (e: Exception) { InputMode.HEBREW_PREDICTIVE }
    }

    private fun engineLanguageFor(mode: InputMode): T9Engine.Language? = when (mode) {
        InputMode.HEBREW_MULTITAP, InputMode.HEBREW_PREDICTIVE -> T9Engine.Language.HEBREW
        InputMode.ENGLISH_UPPER, InputMode.ENGLISH_CAPITALIZE, InputMode.ENGLISH_LOWER -> T9Engine.Language.ENGLISH
        InputMode.SPANISH -> T9Engine.Language.SPANISH
        InputMode.FRENCH -> T9Engine.Language.FRENCH
        InputMode.GERMAN -> T9Engine.Language.GERMAN
        InputMode.ITALIAN -> T9Engine.Language.ITALIAN
        InputMode.PORTUGUESE -> T9Engine.Language.PORTUGUESE
        InputMode.DUTCH -> T9Engine.Language.DUTCH
        InputMode.POLISH -> T9Engine.Language.POLISH
        InputMode.TURKISH -> T9Engine.Language.TURKISH
        InputMode.ROMANIAN -> T9Engine.Language.ROMANIAN
        InputMode.CZECH -> T9Engine.Language.CZECH
        InputMode.SWEDISH -> T9Engine.Language.SWEDISH
        InputMode.NORWEGIAN -> T9Engine.Language.NORWEGIAN
        InputMode.DANISH -> T9Engine.Language.DANISH
        InputMode.FINNISH -> T9Engine.Language.FINNISH
        InputMode.RUSSIAN -> T9Engine.Language.RUSSIAN
        InputMode.NUMERIC -> null
    }

    private fun modeLabel(mode: InputMode): String = when (mode) {
        InputMode.HEBREW_MULTITAP -> "עב"
        InputMode.HEBREW_PREDICTIVE -> "עב-ניבוי"
        InputMode.ENGLISH_UPPER -> "EN-ABC"
        InputMode.ENGLISH_CAPITALIZE -> "EN-Abc"
        InputMode.ENGLISH_LOWER -> "en-abc"
        InputMode.SPANISH -> "ES"
        InputMode.FRENCH -> "FR"
        InputMode.GERMAN -> "DE"
        InputMode.ITALIAN -> "IT"
        InputMode.PORTUGUESE -> "PT"
        InputMode.DUTCH -> "NL"
        InputMode.POLISH -> "PL"
        InputMode.TURKISH -> "TR"
        InputMode.ROMANIAN -> "RO"
        InputMode.CZECH -> "CS"
        InputMode.SWEDISH -> "SV"
        InputMode.NORWEGIAN -> "NO"
        InputMode.DANISH -> "DA"
        InputMode.FINNISH -> "FI"
        InputMode.RUSSIAN -> "RU"
        InputMode.NUMERIC -> "123"
    }

    /** קוד locale ל-SpeechRecognizer (ר' startVoiceTranscription) - שפה שהתמלול
     * הקולי לא בהכרח נתמך בה בפועל על המכשיר עדיין תיפול חזרה לזיהוי הכי קרוב. */
    private fun localeFor(language: T9Engine.Language): String = when (language) {
        T9Engine.Language.HEBREW -> "he-IL"
        T9Engine.Language.ENGLISH -> "en-US"
        T9Engine.Language.SPANISH -> "es-ES"
        T9Engine.Language.FRENCH -> "fr-FR"
        T9Engine.Language.GERMAN -> "de-DE"
        T9Engine.Language.ITALIAN -> "it-IT"
        T9Engine.Language.PORTUGUESE -> "pt-PT"
        T9Engine.Language.DUTCH -> "nl-NL"
        T9Engine.Language.POLISH -> "pl-PL"
        T9Engine.Language.TURKISH -> "tr-TR"
        T9Engine.Language.ROMANIAN -> "ro-RO"
        T9Engine.Language.CZECH -> "cs-CZ"
        T9Engine.Language.SWEDISH -> "sv-SE"
        T9Engine.Language.NORWEGIAN -> "nb-NO"
        T9Engine.Language.DANISH -> "da-DK"
        T9Engine.Language.FINNISH -> "fi-FI"
        T9Engine.Language.RUSSIAN -> "ru-RU"
    }

    /** שם מלא (לא הקיצור של modeLabel) לתצוגה בתפריט בחירת השפה. */
    private fun modeFullName(mode: InputMode): String = when (mode) {
        InputMode.HEBREW_MULTITAP -> "עברית"
        InputMode.HEBREW_PREDICTIVE -> "עברית (ניבוי)"
        InputMode.ENGLISH_UPPER -> "English (ABC)"
        InputMode.ENGLISH_CAPITALIZE -> "English (Abc)"
        InputMode.ENGLISH_LOWER -> "English (abc)"
        InputMode.SPANISH -> "Español"
        InputMode.FRENCH -> "Français"
        InputMode.GERMAN -> "Deutsch"
        InputMode.ITALIAN -> "Italiano"
        InputMode.PORTUGUESE -> "Português"
        InputMode.DUTCH -> "Nederlands"
        InputMode.POLISH -> "Polski"
        InputMode.TURKISH -> "Türkçe"
        InputMode.ROMANIAN -> "Română"
        InputMode.CZECH -> "Čeština"
        InputMode.SWEDISH -> "Svenska"
        InputMode.NORWEGIAN -> "Norsk"
        InputMode.DANISH -> "Dansk"
        InputMode.FINNISH -> "Suomi"
        InputMode.RUSSIAN -> "Русский"
        InputMode.NUMERIC -> "מספרים"
    }

    /** תג השפה הקצר שבפס המצב של הפאנל (ר' renderTypingState) ובשורות תפריט
     *  השפה. קצר יותר מ-[modeLabel] בכוונה: התג מציין את השפה, והתווית שלצידו
     *  ([modeTag]) כבר מציינת אם היא במצב ניבוי או אות-אות. */
    private fun modeBadge(mode: InputMode): String = when (mode) {
        InputMode.HEBREW_MULTITAP, InputMode.HEBREW_PREDICTIVE -> "עב"
        InputMode.ENGLISH_UPPER -> "ABC"
        InputMode.ENGLISH_CAPITALIZE -> "Abc"
        InputMode.ENGLISH_LOWER -> "abc"
        else -> modeLabel(mode)
    }

    /** תיאור אופן ההקלדה שליד תג השפה - מבדיל ניבוי מ-multi-tap. מכבד גם את
     *  כיבוי הניבוי הגלובלי ממרכז הבקרה (ר' predictiveEnabled). */
    private fun modeTag(mode: InputMode): String = when {
        mode == InputMode.NUMERIC -> "ספרות"
        !isPredictingIn(mode) -> "אות-אות"
        else -> "ניבוי"
    }

    /** היקף המילון של השפה, כפי שמוצג בתפריט בחירת השפה. עברית ואנגלית נשענות
     *  על קורפוס אמיתי; לשאר השפות יש מילון התחלה של כמה מאות מילים (ר' T9Engine). */
    private fun dictionaryLabel(mode: InputMode): String = when (engineLanguageFor(mode)) {
        null -> ""
        else -> if (!isPredictingIn(mode)) "בלי ניבוי" else dictionaryScope(mode)
    }

    private fun dictionaryScope(mode: InputMode): String = when (engineLanguageFor(mode)) {
        null -> ""
        T9Engine.Language.HEBREW, T9Engine.Language.ENGLISH -> "מילון מלא"
        else -> "מילון בסיסי"
    }

    /** מפעיל שיבוץ אותיות רישיות בהתאם למצב הנוכחי - רלוונטי לאנגלית בלבד. */
    private fun applyCase(text: String, mode: InputMode = currentMode()): String = when (mode) {
        InputMode.ENGLISH_UPPER -> text.uppercase()
        InputMode.ENGLISH_CAPITALIZE -> text.replaceFirstChar { it.uppercaseChar() }
        else -> text
    }

    /** מעביר למצב הבא במחזור הקבוע (ראו [InputMode.CYCLE_ORDER]) - מקש # קצר. */
    private fun advanceMode() {
        val order = activeCycle()
        val next = order[(order.indexOf(currentMode()) + 1).mod(order.size)]
        prefs.edit().putString("input_mode", next.name).apply()
        engineLanguageFor(next)?.let { engine = buildEngine(it); loadFrequencies(); loadCustomWords() }
        resetComposing()
        renderPanel()
    }

    // --- למידת תדירות מילים -------------------------------------------------

    private fun frequencyPrefsKey(language: T9Engine.Language = engineLanguageFor(currentMode()) ?: T9Engine.Language.HEBREW) = "freq_${language.name}"

    /** פורמט אחסון פשוט: "מילה1=מספר;מילה2=מספר;..." - מספיק כי אין ל-';'/'=' משמעות באף מילה במילונים. */
    private fun loadFrequencies() {
        val raw = prefs.getString(frequencyPrefsKey(), null).orEmpty()
        wordFrequency = raw.split(';')
            .mapNotNull { entry ->
                val parts = entry.split('=')
                val word = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val count = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                word to count
            }
            .toMap()
            .toMutableMap()
    }

    private fun saveFrequencies() {
        val serialized = wordFrequency.entries.joinToString(";") { (word, count) -> "$word=$count" }
        prefs.edit().putString(frequencyPrefsKey(), serialized).apply()
    }

    private fun recordWordUsage(word: String) {
        if (isPrivateField) return
        wordFrequency[word] = (wordFrequency[word] ?: 0) + 1
        saveFrequencies()
    }

    // --- מילים מותאמות-אישית -----------------------------------------------

    private fun customWordsPrefsKey(language: T9Engine.Language = engineLanguageFor(currentMode()) ?: T9Engine.Language.HEBREW) = "custom_${language.name}"

    /** פורמט: "digits1:word1,word2;digits2:word3;..." - כל מיקום יכול להצטבר כמה מילים. */
    private fun loadCustomWords() {
        val raw = prefs.getString(customWordsPrefsKey(), null).orEmpty()
        customWords = raw.split(';')
            .mapNotNull { entry ->
                val parts = entry.split(':')
                val digits = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val words = parts.getOrNull(1)?.split(',')?.filter { it.isNotBlank() } ?: return@mapNotNull null
                digits to words.toMutableList()
            }
            .toMap()
            .toMutableMap()
    }

    private fun saveCustomWords() {
        val serialized = customWords.entries.joinToString(";") { (digits, words) -> "$digits:${words.joinToString(",")}" }
        prefs.edit().putString(customWordsPrefsKey(), serialized).apply()
    }

    /** מוסיפה את המילה שבהרכבה כרגע לניבוי - רק כשאין לה כבר התאמה במילון
     * (אחרת אין מה להוסיף), נקראת ממקש Options הפיזי (ר' onCreate). */
    private fun addCurrentWordToDictionary() {
        val ic = currentInputConnection ?: return
        if (candidates.isNotEmpty() || digitSequence.isEmpty()) return
        val word = fallbackLetters.toString()
        if (word.isBlank()) return
        if (isPrivateField) {
            showPanelMessage("לא נשמרות מילים משדה פרטי")
            return
        }
        val list = customWords.getOrPut(digitSequence) { mutableListOf() }
        if (word !in list) {
            list.add(0, word)
            saveCustomWords()
        }
        refreshComposing(ic)
    }

    // --- UI -------------------------------------------------------------------
    //
    // הפאנל נבנה ידנית בקוד ולא ב-XML כי הוא ה-input view של InputMethodService.
    // המידות לקוחות מהעיצוב שב-design/keyboard-panel: הערכים שם בפיקסלים של מסך
    // 640x960 בצפיפות 2.0, ולכן כל ערך dp כאן הוא חצי מהערך שבעיצוב.
    //
    // לפאנל ארבעה מצבים (ר' Panel.dc.html), וכולם חולקים את אותו שלד: פס כותרת
    // בראש, אזור תוכן, ומקרא מקשים בתחתית מעל קו מפריד. המקרא הוא ההסבר היחיד
    // שיש למשתמש על מכשיר בלי מסך מגע - אין מקלדת גרפית ללחוץ עליה.

    override fun onCreateInputView(): View {
        applyTheme()

        // ---- מצב 1: הקלדה וניבוי -------------------------------------------
        langBadge = TextView(this).apply {
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(4), dp(10), dp(4))
            setTextColor(selectionColor)
            background = GradientDrawable().apply {
                cornerRadius = dp(22).toFloat()
                setColor(KeyboardPalette.withAlpha(selectionColor, 0.16f))
                setStroke(dp(1), selectionColor)
            }
            // רשת ביטחון בלבד - במכשיר היעד אין מגע, והדרך לפתוח את תפריט השפה
            // היא החזקת # (ר' openLanguageMenu).
            setOnClickListener { currentInputConnection?.let { ic -> openLanguageMenu(ic) } }
        }
        modeTagView = TextView(this).apply {
            textSize = 11f
            setTextColor(mutedTextColor)
        }
        digitsView = TextView(this).apply {
            textSize = 11f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.12f
            setTextColor(faintTextColor)
        }
        candidateCounter = counterChip()
        typeRail = rail().apply {
            addView(langBadge)
            addView(modeTagView, gap(dp(5)))
            addView(spacer(), spacerParams())
            addView(digitsView, gap(dp(6)))
            addView(candidateCounter, gap(dp(6)))
        }

        candidatesRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(16), 0)
        }
        // גלילה אופקית - בלעדיה מועמדות מעבר לרוחב המסך פשוט נחתכות. קצה
        // מתפוגג (fading edge) במקום חיתוך חד, כמו במעבר הצבע שבעיצוב.
        candidatesScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            isHorizontalFadingEdgeEnabled = true
            setFadingEdgeLength(dp(22))
            addView(
                candidatesRow,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT),
            )
        }
        emptyHintView = TextView(this).apply {
            textSize = 13f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(16), 0)
            setTextColor(faintTextColor)
        }

        // ---- מצב 2: סימני פיסוק ---------------------------------------------
        punctuationPreview = TextView(this).apply {
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(onSelectionColor)
            background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(selectionColor)
            }
        }
        punctuationCounter = counterChip()
        punctuationRail = rail().apply {
            addView(punctuationPreview, LinearLayout.LayoutParams(dp(28), dp(28)))
            addView(railTitle("סימני פיסוק"), gap(dp(7)))
            addView(spacer(), spacerParams())
            addView(punctuationCounter)
        }
        punctuationGrid = GridLayout(this).apply {
            columnCount = PUNCTUATION_COLUMNS
            // 12dp ולא 16dp כי לכל תא יש שוליים של 4dp מסביב - יחד הם מיישרים
            // את קצה הרשת לאותם 16dp של שאר שורות הפאנל.
            setPadding(dp(12), 0, dp(12), 0)
        }

        // ---- מצב 3: בחירת שפה ------------------------------------------------
        languageCounter = counterChip()
        languageRail = rail().apply {
            addView(railTitle("שפת הקלדה"))
            addView(spacer(), spacerParams())
            addView(languageCounter)
        }
        languageMenuList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(16), 0)
        }
        // תקרה על הגובה כדי שתפריט של 21 שפות לא יבלע את כל המסך - גולל
        // בתוך עצמו במקום זאת (ר' renderLanguageState שגם גוללת אל הבחירה).
        languageMenuScroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(
                languageMenuList,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
            )
        }

        // ---- מצב 5: לוח (העתק/גזור/הדבק) ----------------------------------------
        clipboardCounter = counterChip()
        clipboardRail = rail().apply {
            addView(railTitle("לוח"))
            addView(spacer(), spacerParams())
            addView(clipboardCounter)
        }
        clipboardList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(16), 0)
        }
        clipboardScroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(
                clipboardList,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
            )
        }

        // ---- מצב 4: תמלול קולי ------------------------------------------------
        micView = ImageView(this).apply {
            setImageResource(R.drawable.ic_panel_mic)
            imageTintList = ColorStateList.valueOf(selectionColor)
            setPadding(dp(11), dp(11), dp(11), dp(11))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(KeyboardPalette.withAlpha(selectionColor, 0.16f))
                setStroke(dp(2), selectionColor)
            }
        }
        voiceTitle = TextView(this).apply {
            textSize = 15f
            setTextColor(textColor)
            maxLines = 1
            text = getString(R.string.voice_listening)
        }
        voiceSubtitle = TextView(this).apply {
            textSize = 11f
            maxLines = 1
            setTextColor(mutedTextColor)
        }
        val voiceTexts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(voiceTitle)
            addView(
                voiceSubtitle,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = dp(3) },
            )
        }
        voiceWave = VoiceWaveView(this).apply { color = selectionColor }
        voiceRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(4), dp(16), 0)
            addView(micView, LinearLayout.LayoutParams(dp(42), dp(42)))
            // עמודת הטקסט היא זו שמותחת את השורה (ולא רווח נפרד בין הטקסט
            // לוויזואלייזר) - אחרת היא מצטמצמת לרוחב שם השפה הקצר שמתחתיה
            // וכותרת "מקשיב…" נחתכת אחרי שתי אותיות.
            addView(
                voiceTexts,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { marginStart = dp(10); marginEnd = dp(10) },
            )
            addView(voiceWave, LinearLayout.LayoutParams(dp(72), dp(32)))
        }

        // ---- מקרא המקשים -------------------------------------------------------
        legendRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(7), dp(16), 0)
        }
        legendBar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                View(this@KeyboardService).apply { setBackgroundColor(hairlineColor) },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)),
            )
            addView(
                legendRow,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
            )
        }

        panelRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // הפאנל כולו בעברית ונקרא מימין לשמאל, גם כששדה הקלט עצמו באנגלית.
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = panelBackground()
            setPadding(0, dp(9), 0, dp(11))
            addView(typeRail, panelChildParams(dp(28), 0))
            addView(punctuationRail, panelChildParams(dp(28), 0))
            addView(languageRail, panelChildParams(dp(28), 0))
            addView(clipboardRail, panelChildParams(dp(28), 0))
            addView(voiceRow, panelChildParams(ViewGroup.LayoutParams.WRAP_CONTENT, 0))
            addView(candidatesScroll, panelChildParams(dp(42), dp(7)))
            addView(emptyHintView, panelChildParams(dp(42), dp(7)))
            addView(punctuationGrid, panelChildParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(7)))
            addView(languageMenuScroll, panelChildParams(dp(178), dp(7)))
            addView(clipboardScroll, panelChildParams(dp(178), dp(7)))
            addView(legendBar, panelChildParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(7)))
        }
        renderPanel()
        return panelRoot
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    // פס המקלדת מוצג תמיד כשיש שדה קלט פעיל - גם בלי מועמדות ניבוי - כי פס
    // המצב שבראש הפאנל (ר' renderTypingState) הוא הדרך היחידה של המשתמש לראות
    // באיזו שפה/מצב הקלדה הוא נמצא כרגע (עב/Abc/123 וכו') על מכשיר בלי מסך
    // מגע ובלי מקלדת גרפית. הסתרתו כשאין מועמדות השאירה את המשתמש בלי שום
    // אינדיקציה לשפה הנוכחית רוב הזמן.
    override fun onEvaluateInputViewShown(): Boolean = true

    private fun applyTheme() {
        val theme = ThemeClient.getTheme(this)
        if (theme.isDarkMode) {
            backgroundColor = 0xFF1C1C1E.toInt()
            textColor = Color.WHITE
            mutedTextColor = 0xFFB0B0B0.toInt()
            faintTextColor = 0x6BFFFFFF
            chipBackgroundColor = 0xFF2C2C2E.toInt()
            elevatedColor = 0xFF3A3A3C.toInt()
            hairlineColor = 0x1AFFFFFF
        } else {
            backgroundColor = 0xFFEFEFEF.toInt()
            textColor = Color.BLACK
            mutedTextColor = 0xFF444444.toInt()
            faintTextColor = 0x66000000
            chipBackgroundColor = Color.WHITE
            elevatedColor = 0xFFE5E5EA.toInt()
            hairlineColor = 0x1F000000
        }
        accentColor = theme.primaryColor
        selectionColor = KeyboardPalette.accentOn(accentColor, backgroundColor, textColor)
        onSelectionColor = KeyboardPalette.onAccent(selectionColor)
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        resetComposing()
        isPunctuationMenuOpen = false
        isLanguageMenuOpen = false
        isClipboardOpen = false
        panelMessage = null
        predictiveEnabled = prefs.getBoolean("predictive_enabled", true)
        loadLanguageSettings()
        ensureModeIsActive()
        val inputClass = attribute?.inputType?.and(InputType.TYPE_MASK_CLASS)
        isPredictiveField = inputClass == InputType.TYPE_CLASS_TEXT
        isPrivateField = isPrivateInput(attribute)
        if (isPredictiveField && !isPrivateField) warmUpVoiceEngine()
        renderPanel()
    }

    private fun isPrivateInput(attribute: EditorInfo?): Boolean {
        if (attribute == null) return false
        if (attribute.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0) return true
        val type = attribute.inputType
        val variation = type and InputType.TYPE_MASK_VARIATION
        return when (type and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_TEXT -> variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            else -> false
        }
    }

    private fun resetComposing() {
        digitSequence = ""
        candidates = emptyList()
        candidateIndex = 0
        fallbackLetters.clear()
        lastTapDigit = null
    }

    // --- לבני הבנייה של הפאנל -------------------------------------------------

    /** שורת כותרת: ריפוד אופקי אחיד של 16dp, תוכן ממורכז אנכית. */
    private fun rail(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(16), 0, dp(16), 0)
    }

    private fun railTitle(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 13f
        setTextColor(textColor)
    }

    /** מונה "3/34" - אותו צ'יפ קטן בשלושת מצבי הפאנל שיש בהם ניווט. */
    private fun counterChip(): TextView = TextView(this).apply {
        textSize = 11f
        typeface = Typeface.MONOSPACE
        gravity = Gravity.CENTER
        setPadding(dp(6), dp(2), dp(6), dp(2))
        setTextColor(mutedTextColor)
        background = GradientDrawable().apply {
            cornerRadius = dp(8).toFloat()
            setColor(chipBackgroundColor)
        }
    }

    private fun spacer(): View = View(this)

    private fun spacerParams(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(0, 1, 1f)

    private fun gap(startMargin: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { marginStart = startMargin }

    private fun panelChildParams(height: Int, topMargin: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height)
            .apply { this.topMargin = topMargin }

    /** רקע הפאנל: פינות עליונות מעוגלות וקו מפריד דק בקצה העליון בלבד. */
    private fun panelBackground(): Drawable {
        val radius = dp(16).toFloat()
        val radii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
        val hairline = GradientDrawable().apply {
            cornerRadii = radii
            setColor(hairlineColor)
        }
        val face = GradientDrawable().apply {
            cornerRadii = radii
            setColor(backgroundColor)
        }
        return LayerDrawable(arrayOf(hairline, face)).apply { setLayerInset(1, 0, dp(1), 0, 0) }
    }

    /** רקע של פריט נבחר/לא-נבחר - צ'יפ מועמדת, תא פיסוק ושורת שפה חולקים אותו. */
    private fun itemDrawable(cornerRadius: Int, highlighted: Boolean): GradientDrawable = GradientDrawable().apply {
        this.cornerRadius = dp(cornerRadius).toFloat()
        setColor(if (highlighted) selectionColor else chipBackgroundColor)
    }

    /** מקש במקרא: ריבוע קטן עם הספרה/הסימן שעליו. מסגרת מקווקוות = החזקה ארוכה. */
    private fun legendKey(label: String, longPress: Boolean): TextView = TextView(this).apply {
        text = label
        textSize = 11f
        gravity = Gravity.CENTER
        minWidth = dp(20)
        setPadding(dp(4), 0, dp(4), 0)
        setTextColor(textColor)
        background = GradientDrawable().apply {
            cornerRadius = dp(6).toFloat()
            if (longPress) setStroke(dp(1), elevatedColor, dp(2).toFloat(), dp(2).toFloat())
            else setColor(chipBackgroundColor)
        }
    }

    /** בונה את מקרא המקשים מחדש לפי המצב הנוכחי של הפאנל. */
    // המקרא שכבר מוצג, ובאיזו שורה. renderPanel רץ בכל לחיצת מקש, והמקרא
    // של מצב ההקלדה זהה בכל הלחיצות - קודם הוא נבנה מחדש כל פעם (חמישה
    // LinearLayout, עשרה TextView ו-GradientDrawable), כלומר הקצאות ומדידה
    // מחדש של הפאנל כולו על כל ספרה שהוקלדה. השורה עצמה נבנית מחדש ב-
    // onCreateInputView (למשל אחרי החלפת ערכת צבעים), ואז ההשוואה לפי
    // זהות השורה מאלצת בנייה.
    private var renderedLegend: List<LegendItem>? = null
    private var renderedLegendRow: View? = null

    private fun renderLegend(vararg items: LegendItem) {
        val requested = items.toList()
        if (renderedLegendRow === legendRow && renderedLegend == requested) return
        renderedLegend = requested
        renderedLegendRow = legendRow
        legendRow.removeAllViews()
        items.forEachIndexed { index, item ->
            val entry = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(
                    legendKey(item.key, item.longPress),
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(20)),
                )
                addView(
                    TextView(this@KeyboardService).apply {
                        text = item.label
                        textSize = 10.5f
                        setTextColor(mutedTextColor)
                    },
                    gap(dp(4)),
                )
            }
            legendRow.addView(entry, gap(if (index == 0) 0 else dp(9)))
        }
    }

    private data class LegendItem(val key: String, val label: String, val longPress: Boolean = false)

    // --- ציור הפאנל ------------------------------------------------------------

    /** הודעה חד-פעמית במקום שורת המועמדות (חוסר הרשאת מיקרופון וכדומה). */
    private fun showPanelMessage(text: String) {
        panelMessage = text
        renderPanel()
    }

    private var renderedPanelState: Int = -1

    private fun renderPanel() {
        if (!::panelRoot.isInitialized) return
        val state = when {
            isListening -> 3
            isClipboardOpen -> 4
            isLanguageMenuOpen -> 2
            isPunctuationMenuOpen -> 1
            else -> 0
        }
        // רק במעבר בין מצבים (לא בכל הקשה): הופעה רכה וקצרה של החלקים החדשים.
        if (renderedPanelState != -1 && state != renderedPanelState && panelRoot.isAttachedToWindow) {
            android.transition.TransitionManager.beginDelayedTransition(
                panelRoot,
                android.transition.AutoTransition().setDuration(140),
            )
        }
        renderedPanelState = state
        if (state != 3) {
            voiceWave.active = false
            micView.animate().cancel()
            micView.scaleX = 1f
            micView.scaleY = 1f
        }
        when {
            isListening -> renderVoiceState()
            isClipboardOpen -> renderClipboardState()
            isLanguageMenuOpen -> renderLanguageState()
            isPunctuationMenuOpen -> renderPunctuationState()
            else -> renderTypingState()
        }
        updateInputViewShown()
    }

    /** מציג רק את החלקים ששייכים למצב הנוכחי - שאר חלקי הפאנל חיים אך מוסתרים. */
    private fun showOnly(vararg visible: View) {
        for (view in panelParts) {
            view.visibility = if (visible.any { it === view }) View.VISIBLE else View.GONE
        }
    }

    private val panelParts: List<View>
        get() = listOf(
            typeRail, punctuationRail, languageRail, voiceRow,
            candidatesScroll, emptyHintView, punctuationGrid, languageMenuScroll,
            clipboardRail, clipboardScroll,
        )

    private fun renderTypingState() {
        val mode = currentMode()
        // במצב multi-tap (אין התאמה במילון) מוצגות האותיות שנבחרו בפועל כצ'יפ
        // אחד לא-נבחר, כדי שגם שם יהיה חיווי ויזואלי ולא רק טקסט בשדה עצמו.
        val shown = when {
            candidates.isNotEmpty() -> candidates
            fallbackLetters.isNotEmpty() -> listOf(fallbackLetters.toString())
            else -> emptyList()
        }
        val hasChips = panelMessage == null && shown.isNotEmpty()
        showOnly(typeRail, if (hasChips) candidatesScroll else emptyHintView)

        langBadge.text = modeBadge(mode)
        modeTagView.text = modeTag(mode)
        digitsView.text = digitSequence.toCharArray().joinToString(" ")
        digitsView.visibility = if (digitSequence.isEmpty()) View.GONE else View.VISIBLE

        val selectedIndex = candidateIndex.coerceIn(0, (candidates.size - 1).coerceAtLeast(0))
        candidateCounter.text = "${selectedIndex + 1}/${candidates.size}"
        candidateCounter.visibility = if (candidates.size > 1) View.VISIBLE else View.GONE

        if (hasChips) {
            renderCandidateChips(shown, if (candidates.isEmpty()) -1 else selectedIndex, mode)
        } else {
            emptyHintView.text = panelMessage
                ?: if (mode == InputMode.NUMERIC) "הקש ספרות" else "הקש ספרות כדי לכתוב"
        }

        renderLegend(
            LegendItem("#", "שפה"),
            LegendItem("*", "פיסוק"),
            LegendItem("0", "רווח"),
            LegendItem("‹›", "מועמדות"),
            LegendItem("0", "קול", longPress = true),
        )
    }

    private fun renderCandidateChips(words: List<String>, selectedIndex: Int, mode: InputMode) {
        candidatesRow.removeAllViews()
        // "חלון" של עד MAX_VISIBLE_CANDIDATES מועמדות סביב המועמדת הנבחרת - כדי
        // שהשורה לא תתפח כשהמילון מחזיר עשרות התאמות, בלי לאבד גישה למועמדות
        // שנבחרות בעזרת החיצים מעבר לתקרה.
        val total = words.size
        val windowStart = if (total <= MAX_VISIBLE_CANDIDATES) {
            0
        } else {
            (selectedIndex.coerceAtLeast(0) - MAX_VISIBLE_CANDIDATES / 2).coerceIn(0, total - MAX_VISIBLE_CANDIDATES)
        }
        val windowEnd = (windowStart + MAX_VISIBLE_CANDIDATES).coerceAtMost(total)

        if (windowStart > 0) candidatesRow.addView(ellipsisChip(), gap(dp(4)))
        var selectedChip: View? = null
        for (index in windowStart until windowEnd) {
            val isSelected = index == selectedIndex
            val chip = TextView(this).apply {
                text = applyCase(words[index], mode)
                textSize = if (isSelected) 16f else 15f
                setPadding(dp(15), dp(7), dp(15), dp(7))
                setTextColor(if (isSelected) onSelectionColor else textColor)
                typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                background = itemDrawable(12, isSelected)
                // אין מסך מגע במכשיר היעד, כך שבחירה בפועל תמיד עוברת דרך מקשי
                // החיצים הפיזיים (cycleCandidate) - ה-click listener כאן הוא רשת
                // ביטחון בלבד למקרה שהשירות רץ תחת קלט מגע/עכבר (למשל באמולטור).
                setOnClickListener { selectCandidate(index) }
            }
            candidatesRow.addView(chip, gap(if (index == windowStart) 0 else dp(8)))
            // בלי אנימציית "קפיצה" - השורה נבנית מחדש בכל הקשה, והאנימציה רצה
            // על כל אות והרגישה כמו תקיעה.
            if (isSelected) selectedChip = chip
        }
        if (windowEnd < total) candidatesRow.addView(ellipsisChip(), gap(dp(4)))

        // גוללת את שורת המועמדות כדי שהמועמדת הנבחרת תמיד תהיה גלויה - בלעדיה,
        // מעבר בין מועמדות בקצה השורה היה משאיר את הבחירה מחוץ לתצוגה.
        selectedChip?.let { chip ->
            candidatesRow.post {
                candidatesScroll.requestChildRectangleOnScreen(
                    chip,
                    android.graphics.Rect(0, 0, chip.width, chip.height),
                    false,
                )
            }
        }
    }

    private fun renderPunctuationState() {
        showOnly(punctuationRail, punctuationGrid)
        val index = punctuationIndex.coerceIn(0, PUNCTUATION_SYMBOLS.size - 1)
        punctuationPreview.text = PUNCTUATION_SYMBOLS[index].toString()
        punctuationCounter.text = "${index + 1}/${PUNCTUATION_SYMBOLS.size}"

        punctuationGrid.removeAllViews()
        PUNCTUATION_SYMBOLS.forEachIndexed { cellIndex, symbol ->
            val isSelected = cellIndex == index
            val cell = TextView(this).apply {
                text = symbol.toString()
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(if (isSelected) onSelectionColor else textColor)
                typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                background = itemDrawable(8, isSelected)
                setOnClickListener { insertPunctuation(cellIndex) }
            }
            val params = GridLayout.LayoutParams(
                GridLayout.spec(cellIndex / PUNCTUATION_COLUMNS),
                GridLayout.spec(cellIndex % PUNCTUATION_COLUMNS, 1f),
            ).apply {
                width = 0
                height = dp(32)
                setMargins(dp(4), dp(4), dp(4), dp(4))
            }
            punctuationGrid.addView(cell, params)
        }

        renderLegend(
            LegendItem("●", "הוסף"),
            LegendItem("+", "ניווט בחצים"),
            LegendItem("1", "לוח"),
            LegendItem("*", "סגור"),
        )
    }

    private fun renderLanguageState() {
        showOnly(languageRail, languageMenuScroll)
        val cycle = activeCycle()
        languageCounter.text = "${languageMenuIndex + 1}/${cycle.size}"

        languageMenuList.removeAllViews()
        var selectedRow: View? = null
        cycle.forEachIndexed { index, candidateMode ->
            val isSelected = index == languageMenuIndex
            val ink = if (isSelected) onSelectionColor else textColor
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(11), 0, dp(11), 0)
                background = itemDrawable(12, isSelected)
                setOnClickListener { selectLanguageMenuItem(index) }
                addView(
                    TextView(this@KeyboardService).apply {
                        text = modeFullName(candidateMode)
                        textSize = 13f
                        setTextColor(ink)
                        typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    },
                )
                addView(spacer(), spacerParams())
                val dictionary = dictionaryLabel(candidateMode)
                if (dictionary.isNotEmpty()) {
                    addView(
                        TextView(this@KeyboardService).apply {
                            text = dictionary
                            textSize = 10f
                            alpha = 0.7f
                            setTextColor(ink)
                        },
                        gap(dp(7)),
                    )
                }
                addView(
                    TextView(this@KeyboardService).apply {
                        text = modeBadge(candidateMode)
                        textSize = 11f
                        typeface = Typeface.MONOSPACE
                        gravity = Gravity.CENTER
                        setPadding(dp(6), dp(2), dp(6), dp(2))
                        setTextColor(ink)
                        background = GradientDrawable().apply {
                            cornerRadius = dp(6).toFloat()
                            setColor(
                                if (isSelected) KeyboardPalette.withAlpha(onSelectionColor, 0.14f) else elevatedColor,
                            )
                        }
                    },
                    gap(dp(7)),
                )
            }
            languageMenuList.addView(
                row,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(38)).apply { bottomMargin = dp(4) },
            )
            if (isSelected) selectedRow = row
        }
        selectedRow?.let { row ->
            languageMenuList.post {
                languageMenuScroll.requestChildRectangleOnScreen(
                    row,
                    android.graphics.Rect(0, 0, row.width, row.height),
                    false,
                )
            }
        }

        renderLegend(
            LegendItem("↕", "ניווט"),
            LegendItem("●", "בחר"),
            LegendItem("←", "ביטול"),
        )
    }

    /** שורה בלוח: פעולה על השדה, או העתקה קודמת להדבקה. */
    private data class ClipAction(val title: String, val detail: String?, val run: (InputConnection) -> Unit)

    private fun clipActions(): List<ClipAction> {
        val current = currentClipText()
        val actions = mutableListOf<ClipAction>()
        if (current != null) actions += ClipAction("הדבק", preview(current)) { ic -> ic.commitText(current, 1) }
        actions += ClipAction("העתק", "המסומן, או את כל הטקסט") { ic -> copyFromField(ic, cut = false) }
        actions += ClipAction("גזור", "המסומן, או את כל הטקסט") { ic -> copyFromField(ic, cut = true) }
        actions += ClipAction("בחר הכל", null) { ic -> ic.performContextMenuAction(android.R.id.selectAll) }
        expireClipHistory()
        clipHistory.filter { it != current }.forEach { text ->
            actions += ClipAction(preview(text), "הדבק העתקה קודמת") { ic -> ic.commitText(text, 1) }
        }
        return actions
    }

    private fun renderClipboardState() {
        showOnly(clipboardRail, clipboardScroll)
        val actions = clipActions()
        clipboardIndex = clipboardIndex.coerceIn(0, actions.size - 1)
        clipboardCounter.text = "${clipboardIndex + 1}/${actions.size}"

        clipboardList.removeAllViews()
        var selectedRow: View? = null
        actions.forEachIndexed { index, action ->
            val isSelected = index == clipboardIndex
            val ink = if (isSelected) onSelectionColor else textColor
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(11), 0, dp(11), 0)
                background = itemDrawable(12, isSelected)
                setOnClickListener { runClipAction(index) }
                addView(
                    TextView(this@KeyboardService).apply {
                        text = action.title
                        textSize = 13f
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                        setTextColor(ink)
                        typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    },
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
                )
                action.detail?.let { detail ->
                    addView(
                        TextView(this@KeyboardService).apply {
                            text = detail
                            textSize = 10f
                            maxLines = 1
                            ellipsize = android.text.TextUtils.TruncateAt.END
                            alpha = 0.7f
                            setTextColor(ink)
                        },
                        gap(dp(7)),
                    )
                }
            }
            clipboardList.addView(
                row,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(38)).apply { bottomMargin = dp(4) },
            )
            if (isSelected) selectedRow = row
        }
        selectedRow?.let { row ->
            clipboardList.post {
                clipboardScroll.requestChildRectangleOnScreen(row, android.graphics.Rect(0, 0, row.width, row.height), false)
            }
        }

        renderLegend(
            LegendItem("↕", "ניווט"),
            LegendItem("●", "בחר"),
            LegendItem("←", "ביטול"),
        )
    }

    private fun renderVoiceState() {
        showOnly(voiceRow)
        voiceTitle.text = if (isVoiceProcessing) "מתמלל…" else getString(R.string.voice_listening)
        voiceSubtitle.text = voicePartial?.takeLast(42) ?: modeFullName(currentMode())
        voiceWave.processing = isVoiceProcessing
        if (!voiceWave.active) voiceWave.active = true
        // בלי "נשימה" קבועה של המיקרופון - הוא מגיב רק לקול עצמו (updateVoiceLevel).
        if (isVoiceProcessing) {
            micView.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
        }
        if (isVoiceProcessing) {
            renderLegend(LegendItem("←", "ביטול"))
        } else {
            renderLegend(LegendItem("0", "שחרר את המקש כדי לסיים", longPress = true))
        }
    }

    /** עוצמת הקול בפועל (ר' onRmsChanged) - מזינה את גלי הקול. */
    private fun updateVoiceLevel(rmsdB: Float) {
        if (!::voiceWave.isInitialized || !isListening || isVoiceProcessing) return
        val level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        voiceWave.setLevel(level)
        val scale = 1f + 0.12f * level
        micView.animate().scaleX(scale).scaleY(scale).setDuration(90).start()
    }

    private fun ellipsisChip(): TextView = TextView(this).apply {
        text = "…"
        textSize = 15f
        setPadding(dp(4), dp(7), dp(4), dp(7))
        setTextColor(mutedTextColor)
    }

    /** ממיר ערך dp לפיקסלים לפי צפיפות המסך של המכשיר - כדי שריווח/מרווחים ייראו עקביים בכל רזולוציה. */
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun digitCharFor(keyCode: Int): Char? = when (keyCode) {
        KeyEvent.KEYCODE_0 -> '0'
        KeyEvent.KEYCODE_1 -> '1'
        KeyEvent.KEYCODE_2 -> '2'
        KeyEvent.KEYCODE_3 -> '3'
        KeyEvent.KEYCODE_4 -> '4'
        KeyEvent.KEYCODE_5 -> '5'
        KeyEvent.KEYCODE_6 -> '6'
        KeyEvent.KEYCODE_7 -> '7'
        KeyEvent.KEYCODE_8 -> '8'
        KeyEvent.KEYCODE_9 -> '9'
        else -> null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val ic = currentInputConnection ?: return super.onKeyDown(keyCode, event)

        // הודעה חד-פעמית בפאנל (ר' showPanelMessage) נעלמת ברגע שהמשתמש ממשיך
        // להקליד - כל מסלול למטה מצייר את הפאנל מחדש ממילא.
        panelMessage = null

        // # (מחליף שפה) ו-* (פותח תפריט פיסוק) חייבים לעבוד בכל שדה קלט - כולל
        // שדות מספריים/טלפון (isPredictiveField=false) - בדיוק כמו בטלפון T9
        // אמיתי. לכן מטופלים *לפני* הבדיקה למטה שחוסמת המשך טיפול בשדות
        // לא-טקסטואליים. NUMPAD_MULTIPLY/MENU הם מיפויים חלופיים ל-fallback
        // במקרה שהחומרה שולחת קוד שונה מ-KEYCODE_STAR/KEYCODE_POUND הצפויים
        // (יש לאמת מול המכשיר עם adb shell getevent -l; אין קבוע KEYCODE_NUMPAD_POUND
        // ב-Android - לוח מספרי לא כולל #).
        if (isListening) {
            // בזמן תמלול: BACK/מחיקה מבטלים בלי להכניס טקסט; שאר המקשים נבלעים.
            if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_DEL) stopListening()
            return true
        }

        if (isLanguageMenuOpen) {
            // כשתפריט השפה פתוח, רק הניווט/האישור/הביטול פעילים - אותו עיקרון
            // בדיוק כמו תפריט הפיסוק למטה.
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> moveLanguageSelection(-1)
                KeyEvent.KEYCODE_DPAD_DOWN -> moveLanguageSelection(1)
                KeyEvent.KEYCODE_DPAD_CENTER -> selectLanguageMenuItem(languageMenuIndex)
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_DEL -> closeLanguageMenu()
                else -> { /* נבלע - שום פעולה */ }
            }
            return true
        }

        if (isClipboardOpen) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> moveClipboardSelection(-1)
                KeyEvent.KEYCODE_DPAD_DOWN -> moveClipboardSelection(1)
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> runClipAction(clipboardIndex)
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_DEL,
                KeyEvent.KEYCODE_STAR, KeyEvent.KEYCODE_NUMPAD_MULTIPLY -> closeClipboard()
                else -> { /* נבלע - שום פעולה */ }
            }
            return true
        }

        if (isPunctuationMenuOpen) {
            // כשתפריט הפיסוק פתוח, רק הניווט/האישור/הביטול פעילים - כל מקש אחר
            // נבלע כדי שלא יקרו פעולות לא צפויות על טקסט שכבר הורכב לפני הפתיחה.
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> movePunctuationSelection(keyCode)
                KeyEvent.KEYCODE_DPAD_CENTER -> insertPunctuation(punctuationIndex)
                KeyEvent.KEYCODE_1 -> openClipboard()
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_DEL,
                KeyEvent.KEYCODE_STAR, KeyEvent.KEYCODE_NUMPAD_MULTIPLY -> closePunctuationMenu()
                else -> { /* נבלע - שום פעולה */ }
            }
            return true
        }

        when (keyCode) {
            KeyEvent.KEYCODE_STAR, KeyEvent.KEYCODE_NUMPAD_MULTIPLY, KeyEvent.KEYCODE_MENU -> {
                if (event.repeatCount == 0) onStarShortPress()
                return true
            }
            KeyEvent.KEYCODE_POUND -> {
                // לחיצה קצרה (כמו קודם) - קפיצה אחת קדימה במחזור הקבוע. החזקה
                // ארוכה - תפריט בחירת שפה לגישה ישירה לכל שפה (ר' openLanguageMenu),
                // נחוץ במיוחד עכשיו עם 15 שפות נוספות ברשימה. אותה תבנית armed
                // בדיוק כמו voiceInputArmed במקש 0, כדי שההחזקה לא תפעיל את
                // התפריט שוב ושוב כל עוד המקש לחוץ.
                if (event.repeatCount == 0) {
                    poundArmedForLanguageMenu = false
                    onPoundShortPress()
                } else if (!poundArmedForLanguageMenu && event.repeatCount > longPressRepeatThreshold) {
                    poundArmedForLanguageMenu = true
                    openLanguageMenu(ic)
                }
                return true
            }
        }

        if (!isPredictiveField) return super.onKeyDown(keyCode, event)

        val mode = currentMode()

        if (mode == InputMode.NUMERIC) {
            if (keyCode == KeyEvent.KEYCODE_0) {
                // לחיצה קצרה מציבה את הספרה (בשחרור - ר' onKeyUp), ארוכה מפעילה תמלול.
                if (event.repeatCount == 0) {
                    voiceInputArmed = false
                    zeroPending = true
                } else if (!voiceInputArmed && event.repeatCount > longPressRepeatThreshold) {
                    voiceInputArmed = true
                    zeroPending = false
                    startVoiceTranscription()
                }
                return true
            }
            val digit = digitCharFor(keyCode)
            if (digit != null) {
                ic.commitText(digit.toString(), 1)
                return true
            }
            if (keyCode == KeyEvent.KEYCODE_DEL) {
                ic.deleteSurroundingText(1, 0)
                return true
            }
            // "חזור" (Back) פועל כמחיקה גם כאן - במכשירים בלי מקש DEL פיזי נפרד,
            // זו הדרך היחידה בפועל למחוק תו. אם אין כלום למחוק, המקש לא נבלע
            // כדי ש"חזור" עדיין יוכל לסגור את המקלדת כרגיל.
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (!ic.getTextBeforeCursor(1, 0).isNullOrEmpty()) {
                    ic.deleteSurroundingText(1, 0)
                    return true
                }
                return super.onKeyDown(keyCode, event)
            }
            return super.onKeyDown(keyCode, event)
        }

        val digit = digitCharFor(keyCode)
        if (digit != null && digit != '0' && digit != '1') {
            handleDigitPress(digit, ic)
            return true
        }

        when (keyCode) {
            // חצים שמאלה/ימינה/למעלה/למטה - מעבר בין מועמדות הניבוי כשיש יותר
            // ממילה אחת מתאימה (למעלה/למטה כי חלון המועמדות מוצג כשורה מעל שדה
            // הטקסט - ניווט אנכי טבעי לתוכו, בנוסף לחצים האופקיים); אם אין כמה
            // מועמדות, מתנהג כניווט רגיל (ברירת המחדל של המערכת).
            // שורת המועמדות מסודרת מימין לשמאל (הפאנל RTL): הראשונה בימין, ולכן
            // חץ שמאל מתקדם למועמדת הבאה וחץ ימין חוזר - כמו כיוון התנועה על המסך.
            // קודם זה היה הפוך. למעלה/למטה: הקודמת/הבאה.
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (candidates.size > 1) {
                    cycleCandidate(ic, 1)
                    return true
                }
                return super.onKeyDown(keyCode, event)
            }
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP -> {
                if (candidates.size > 1) {
                    cycleCandidate(ic, -1)
                    return true
                }
                return super.onKeyDown(keyCode, event)
            }
            KeyEvent.KEYCODE_DEL -> {
                if (digitSequence.isNotEmpty()) {
                    digitSequence = digitSequence.dropLast(1)
                    if (fallbackLetters.isNotEmpty()) fallbackLetters.deleteCharAt(fallbackLetters.length - 1)
                    candidateIndex = 0
                    lastTapDigit = null
                    refreshComposing(ic)
                    return true
                }
                // אין מילה בהרכבה (composing) פעילה - המחיקה מתייחסת לטקסט שכבר הוצב
                // בשדה. בלי הקריאה המפורשת הזו ל-deleteSurroundingText, מקש המחיקה
                // עלול לא לעשות כלום ברגע שהסמן נמצא אחרי מילה שכבר אושרה.
                ic.deleteSurroundingText(1, 0)
                return true
            }
            // "חזור" (Back) פועל כמחיקה בדיוק כמו KEYCODE_DEL - במכשירים בלי מקש
            // DEL פיזי נפרד, זו הדרך היחידה בפועל למחוק תו בזמן הקלדה. אם אין
            // כלום למחוק (לא מילה בהרכבה, לא טקסט לפני הסמן), המקש לא נבלע כדי
            // ש"חזור" עדיין יוכל לסגור את המקלדת כרגיל.
            KeyEvent.KEYCODE_BACK -> {
                if (digitSequence.isNotEmpty()) {
                    digitSequence = digitSequence.dropLast(1)
                    if (fallbackLetters.isNotEmpty()) fallbackLetters.deleteCharAt(fallbackLetters.length - 1)
                    candidateIndex = 0
                    lastTapDigit = null
                    refreshComposing(ic)
                    return true
                }
                if (!ic.getTextBeforeCursor(1, 0).isNullOrEmpty()) {
                    ic.deleteSurroundingText(1, 0)
                    return true
                }
                return super.onKeyDown(keyCode, event)
            }
            KeyEvent.KEYCODE_0 -> {
                // לחיצה קצרה - כמו ברוב מכשירי T9, 0 לא ממופה לאותיות ומציב רווח.
                // לחיצה ארוכה (repeatCount עולה כל עוד המקש מוחזק) - מפעילה תמלול קולי.
                if (event.repeatCount == 0) {
                    voiceInputArmed = false
                    zeroPending = true
                } else if (!voiceInputArmed && event.repeatCount > longPressRepeatThreshold) {
                    voiceInputArmed = true
                    zeroPending = false
                    startVoiceTranscription()
                }
                return true
            }
            KeyEvent.KEYCODE_1 -> {
                // 1 לא ממופה לאותיות (כמו ברוב מכשירי T9) - מציב כספרה גולמית
                if (digitSequence.isNotEmpty()) commitCurrentWord(ic, appendSpace = false)
                ic.commitText("1", 1)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_0) {
            val wasArmed = voiceInputArmed
            voiceInputArmed = false
            if (zeroPending) {
                // לחיצה קצרה: רווח (או הספרה 0 במצב מספרים).
                zeroPending = false
                val ic = currentInputConnection
                if (ic != null) {
                    if (currentMode() == InputMode.NUMERIC) {
                        ic.commitText("0", 1)
                    } else {
                        if (digitSequence.isNotEmpty()) commitCurrentWord(ic, appendSpace = false)
                        ic.commitText(" ", 1)
                    }
                }
                return true
            }
            // "שחרר את המקש כדי לסיים": SpeechRecognizer.stopListening מסיים את
            // ההקלטה והמנוע מתמלל את מה שנקלט (onResults). הפאנל עובר ל"מתמלל…"
            // ונסגר מעצמו - גם אם התוצאה לא מגיעה (voiceTimeout).
            if (isListening && !isVoiceProcessing) {
                isVoiceProcessing = true
                speechRecognizer?.stopListening()
                voiceHandler.postDelayed(voiceTimeout, VOICE_RESULT_TIMEOUT_MS)
                renderPanel()
                return true
            }
            if (wasArmed) return true
        }
        return super.onKeyUp(keyCode, event)
    }

    /** מאתר את AssistantRecognitionService של com.future.assistant, אם מותקן -
     * ה-query הכללי ל-android.speech.RecognitionService שכבר מוצהר במניפסט
     * (לצורך isRecognitionAvailable) חושף גם אותו, אין צורך בהצהרת <queries>
     * נוספת. מחזיר null אם העוזר הקולי לא מותקן בכלל (נופלים לברירת המחדל). */
    private fun findAssistantRecognitionService(): android.content.ComponentName? {
        return try {
            val intent = Intent("android.speech.RecognitionService").setPackage(ASSISTANT_PACKAGE)
            val resolved = packageManager.queryIntentServices(intent, 0).firstOrNull() ?: return null
            android.content.ComponentName(resolved.serviceInfo.packageName, resolved.serviceInfo.name)
        } catch (e: Exception) {
            null
        }
    }

    private fun startVoiceTranscription() {
        if (isListening) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            showPanelMessage(getString(R.string.voice_permission_missing))
            return
        }
        // מעדיפים את מנוע התמלול המקומי של העוזר הקולי (Whisper.cpp, ר.
        // AssistantRecognitionService) על פני שירות זיהוי הדיבור המובנה של
        // המערכת - האחרון עלול להיות חסום/לא זמין במכשיר הזה (ראו ההערה
        // המקבילה ב-LocalSpeechEngine), בעוד שהמנוע המקומי תמיד עובד.
        // isRecognitionAvailable בודקת רק את ברירת המחדל של המערכת, אז היא
        // רלוונטית רק בנתיב הנפילה חזרה (כשהעוזר הקולי לא מותקן בכלל).
        val assistantComponent = findAssistantRecognitionService()
        if (assistantComponent == null && !SpeechRecognizer.isRecognitionAvailable(this)) {
            showPanelMessage(getString(R.string.voice_unavailable))
            return
        }

        resetComposing()
        isListening = true
        isVoiceProcessing = false
        voicePartial = null
        renderPanel()

        // המזהה נשמר בין הקלטות (ר' stopListening): החיבור לשירות התמלול של
        // העוזר נשאר פתוח, התהליך שלו לא נהרג והמודל נשאר טעון בזיכרון.
        val recognizer = speechRecognizer ?: (if (assistantComponent != null) {
            SpeechRecognizer.createSpeechRecognizer(this, assistantComponent)
        } else {
            SpeechRecognizer.createSpeechRecognizer(this)
        }).also { speechRecognizer = it }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) = updateVoiceLevel(rmsdB)
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                val message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "לא זוהה דיבור"
                    SpeechRecognizer.ERROR_AUDIO -> "שגיאה במיקרופון"
                    else -> null
                }
                // שגיאה שאינה "לא זוהה דיבור" - החיבור עצמו אולי שבור, אז בפעם הבאה יוצרים חדש.
                if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) releaseRecognizer()
                stopListening()
                message?.let { showPanelMessage(it) }
            }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim()
                    ?.replace(Regex("\\s+"), " ")
                if (!text.isNullOrBlank()) {
                    val ic = currentInputConnection
                    // רווח לפני - רק אם הטקסט הקודם לא נגמר ברווח; ואחרי, כדי להמשיך להקליד.
                    val before = ic?.getTextBeforeCursor(1, 0)
                    val lead = if (before.isNullOrEmpty() || before.last().isWhitespace()) "" else " "
                    ic?.commitText("$lead$text ", 1)
                }
                stopListening()
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!partial.isNullOrBlank() && isListening) {
                    voicePartial = partial.trim()
                    renderPanel()
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val locale = localeFor(engineLanguageFor(currentMode()) ?: T9Engine.Language.HEBREW)
        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            // המשתמש מסיים בשחרור 0 - לא לעצור לבד באמצע משפט בגלל שתיקה קצרה.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 6_000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 4_000L)
        }
        recognizer.startListening(recognizerIntent)
    }

    private fun stopListening() {
        voiceHandler.removeCallbacks(voiceTimeout)
        // ביטול באמצע (חזרה/מחיקה, או תוצאה שלא הגיעה בזמן) - עוצרים את ההקלטה/התמלול
        // אבל לא הורסים את המזהה, כדי שההקלטה הבאה לא תתחיל מחיבור ומודל קרים.
        if (isListening) runCatching { speechRecognizer?.cancel() }
        isListening = false
        isVoiceProcessing = false
        voicePartial = null
        renderPanel()
    }

    private fun releaseRecognizer() {
        runCatching { speechRecognizer?.destroy() }
        speechRecognizer = null
    }

    private var lastAsrWarmUp = 0L

    /** מבקש מהעוזר לטעון את מודל התמלול כבר עכשיו (ר' AsrWarmupReceiver), כדי
     *  שהחזקת 0 לא תחכה לטעינה של 264MB. זול כשהמודל כבר טעון. */
    private fun warmUpVoiceEngine() {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastAsrWarmUp < 30_000L) return
        lastAsrWarmUp = now
        runCatching {
            sendBroadcast(Intent("com.future.assistant.action.WARM_UP_ASR").setPackage(ASSISTANT_PACKAGE))
        }
    }

    /**
     * לחיצה קצרה על * - פותחת וסוגרת את תפריט הפיסוק. מגיעה לכאן משני מקורות:
     * המקש עצמו (onKeyDown, כשאין System UI שחוסם אותו) והשידור של FutureUI
     * (ר' starKeyReceiver). שניהם עוברים דרך הפונקציה הזו כדי שההתנהגות תהיה
     * זהה ולא תיפרד לשתי גרסאות.
     */
    private fun onStarShortPress() {
        val ic = currentInputConnection ?: return
        // כשתפריט השפה פתוח, * לא עושה כלום - בדיוק כמו במסלול המקש עצמו.
        if (isLanguageMenuOpen) return
        if (isClipboardOpen) {
            closeClipboard()
            return
        }
        if (isPunctuationMenuOpen) closePunctuationMenu() else openPunctuationMenu(ic)
    }

    /**
     * לחיצה קצרה על Options - פותחת וסוגרת את הלוח ישירות, בלי לעבור דרך
     * תפריט הפיסוק. חריג אחד: כשבהרכבה מילה שאין לה התאמה במילון, Options
     * עדיין מוסיף אותה לניבוי (ר' addCurrentWordToDictionary) - זה המקרה היחיד
     * שבו לפעולה הזו יש משמעות, ובלעדיו לא הייתה דרך להוסיף מילים.
     */
    private fun onOptionsShortPress() {
        val ic = currentInputConnection ?: return
        if (isListening) return
        if (isClipboardOpen) {
            closeClipboard()
            return
        }
        if (!isLanguageMenuOpen && !isPunctuationMenuOpen &&
            candidates.isEmpty() && digitSequence.isNotEmpty() && fallbackLetters.isNotBlank()
        ) {
            addCurrentWordToDictionary()
            return
        }
        isLanguageMenuOpen = false
        if (digitSequence.isNotEmpty()) commitCurrentWord(ic, appendSpace = false)
        openClipboard()
    }

    /** לחיצה קצרה על # - השפה הבאה במחזור, או סגירת תפריט השפה אם הוא פתוח. */
    private fun onPoundShortPress() {
        val ic = currentInputConnection ?: return
        if (isPunctuationMenuOpen || isClipboardOpen) return
        if (isLanguageMenuOpen) {
            closeLanguageMenu()
            return
        }
        if (isPredictiveField && digitSequence.isNotEmpty()) commitCurrentWord(ic, appendSpace = false)
        advanceMode()
    }

    private fun openPunctuationMenu(ic: InputConnection) {
        if (digitSequence.isNotEmpty()) commitCurrentWord(ic, appendSpace = false)
        isPunctuationMenuOpen = true
        punctuationIndex = 0
        renderPanel()
    }

    private fun closePunctuationMenu() {
        isPunctuationMenuOpen = false
        renderPanel()
    }

    /** ניווט דו-מימדי (4 כיוונים) בתפריט הפיסוק - האינדקס השטוח ב-PUNCTUATION_SYMBOLS מתורגם לשורה/עמודה לפי PUNCTUATION_COLUMNS. */
    private fun movePunctuationSelection(keyCode: Int) {
        val count = PUNCTUATION_SYMBOLS.size
        val columns = PUNCTUATION_COLUMNS
        val rowCount = (count + columns - 1) / columns
        val row = punctuationIndex / columns
        val col = punctuationIndex % columns
        var newRow = row
        var newCol = col
        when (keyCode) {
            // רשת הפיסוק RTL: עמודה 0 בימין, ולכן שמאל = העמודה הבאה.
            KeyEvent.KEYCODE_DPAD_LEFT -> newCol = (col + 1) % columns
            KeyEvent.KEYCODE_DPAD_RIGHT -> newCol = (col - 1 + columns) % columns
            KeyEvent.KEYCODE_DPAD_UP -> newRow = (row - 1 + rowCount) % rowCount
            KeyEvent.KEYCODE_DPAD_DOWN -> newRow = (row + 1) % rowCount
        }
        var newIndex = newRow * columns + newCol
        // השורה האחרונה עלולה להיות חלקית - נצמד לפריט האחרון הקיים במקום ליפול מחוץ לרשימה.
        if (newIndex >= count) newIndex = count - 1
        punctuationIndex = newIndex
        renderPanel()
    }

    private fun insertPunctuation(index: Int) {
        val ic = currentInputConnection ?: return
        if (index !in PUNCTUATION_SYMBOLS.indices) return
        ic.commitText(PUNCTUATION_SYMBOLS[index].toString(), 1)
        closePunctuationMenu()
    }

    private fun openClipboard() {
        isPunctuationMenuOpen = false
        isClipboardOpen = true
        clipboardIndex = 0
        renderPanel()
    }

    private fun closeClipboard() {
        isClipboardOpen = false
        renderPanel()
    }

    private fun moveClipboardSelection(direction: Int) {
        val count = clipActions().size
        clipboardIndex = (clipboardIndex + direction + count) % count
        renderPanel()
    }

    private fun runClipAction(index: Int) {
        val ic = currentInputConnection ?: return
        val action = clipActions().getOrNull(index) ?: return
        isClipboardOpen = false
        ic.beginBatchEdit()
        runCatching { action.run(ic) }
        ic.endBatchEdit()
        renderPanel()
    }

    /** העתק/גזור: הטקסט המסומן, ואם אין סימון - כל הטקסט בשדה. */
    private fun copyFromField(ic: InputConnection, cut: Boolean) {
        val selected = ic.getSelectedText(0)?.toString()
        if (!selected.isNullOrEmpty()) {
            setClip(selected)
            if (cut) ic.commitText("", 1)
            showPanelMessage(if (cut) "נגזר ללוח" else "הועתק ללוח")
            return
        }
        val before = ic.getTextBeforeCursor(MAX_FIELD_CHARS, 0)?.toString().orEmpty()
        val after = ic.getTextAfterCursor(MAX_FIELD_CHARS, 0)?.toString().orEmpty()
        val all = before + after
        if (all.isEmpty()) {
            showPanelMessage("אין טקסט להעתקה")
            return
        }
        setClip(all)
        if (cut) ic.deleteSurroundingText(before.length, after.length)
        showPanelMessage(if (cut) "כל הטקסט נגזר ללוח" else "כל הטקסט הועתק ללוח")
    }

    private fun setClip(text: String) {
        val clip = android.content.ClipData.newPlainText("FutureOS", text)
        if (isPrivateField) {
            // טקסט משדה סיסמה: מסומן רגיש (לא מוצג בתצוגה המקדימה של המערכת)
            // ולא נכנס להיסטוריית ההעתקות.
            clip.description.extras = android.os.PersistableBundle().apply { putBoolean(EXTRA_CLIP_SENSITIVE, true) }
        }
        runCatching { clipboardManager.setPrimaryClip(clip) }
        if (!isPrivateField) rememberClip(text)
    }

    private fun currentClipText(): String? = runCatching {
        clipboardManager.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()
    }.getOrNull()?.takeIf { it.isNotEmpty() }

    private fun rememberCurrentClip() {
        val sensitive = runCatching {
            clipboardManager.primaryClipDescription?.extras?.getBoolean(EXTRA_CLIP_SENSITIVE, false) == true
        }.getOrDefault(false)
        if (sensitive || isPrivateField) return
        currentClipText()?.let(::rememberClip)
    }

    private var clipHistoryTouchedAt = 0L

    /** מוחק את ההיסטוריה אם עברו יותר מ-[CLIP_TTL_MS] מההעתקה האחרונה. */
    private fun expireClipHistory() {
        if (clipHistory.isNotEmpty() && android.os.SystemClock.elapsedRealtime() - clipHistoryTouchedAt > CLIP_TTL_MS) {
            clipHistory.clear()
        }
    }

    private fun rememberClip(text: String) {
        clipHistoryTouchedAt = android.os.SystemClock.elapsedRealtime()
        clipHistory.remove(text)
        clipHistory.addFirst(text)
        while (clipHistory.size > MAX_CLIP_HISTORY) clipHistory.removeLast()
    }

    private fun preview(text: String): String =
        text.replace('\n', ' ').trim().let { if (it.length > 40) it.take(40) + "…" else it }

    /** תפריט בחירת שפה (החזקת # ארוכה) - גישה ישירה לכל שפה, בלי לעבור עליהן
     * אחת-אחת עם # קצר. נפתח על השפה הנוכחית כדי שאפשר יהיה לבטל בלי לזוז. */
    private fun openLanguageMenu(ic: InputConnection) {
        if (digitSequence.isNotEmpty()) commitCurrentWord(ic, appendSpace = false)
        isLanguageMenuOpen = true
        languageMenuIndex = activeCycle().indexOf(currentMode()).coerceAtLeast(0)
        renderPanel()
    }

    private fun closeLanguageMenu() {
        isLanguageMenuOpen = false
        renderPanel()
    }

    private fun moveLanguageSelection(direction: Int) {
        val count = activeCycle().size
        languageMenuIndex = (languageMenuIndex + direction + count) % count
        renderPanel()
    }

    private fun selectLanguageMenuItem(index: Int) {
        val cycle = activeCycle()
        if (index !in cycle.indices) return
        val selected = cycle[index]
        prefs.edit().putString("input_mode", selected.name).apply()
        engineLanguageFor(selected)?.let { engine = buildEngine(it); loadFrequencies(); loadCustomWords() }
        resetComposing()
        closeLanguageMenu()
    }

    private fun handleDigitPress(digit: Char, ic: InputConnection) {
        val now = System.currentTimeMillis()
        val isSameKeyRepeat = lastTapDigit == digit && (now - lastTapTimeMillis) < multiTapTimeoutMillis
        lastTapTimeMillis = now

        val letters = engine.lettersFor(digit)

        if (candidates.isEmpty() && digitSequence.isNotEmpty() && isSameKeyRepeat) {
            // multi-tap: עדיין באותו מיקום בלי התאמה במילון - עוברים לאות הבאה על אותו מקש,
            // בלי להוסיף מיקום חדש לרצף (digitSequence כבר מסתיים באותה ספרה).
            if (letters.isNotEmpty()) {
                lastTapLetterIndex = (lastTapLetterIndex + 1) % letters.length
                if (fallbackLetters.isNotEmpty()) {
                    fallbackLetters.setCharAt(fallbackLetters.length - 1, letters[lastTapLetterIndex])
                }
            }
        } else {
            digitSequence += digit
            fallbackLetters.append(letters.firstOrNull() ?: digit)
            lastTapDigit = digit
            lastTapLetterIndex = 0
        }
        candidateIndex = 0
        refreshComposing(ic)
    }

    private fun refreshComposing(ic: InputConnection) {
        val mode = currentMode()
        // במצב "עברית ללא ניבוי", וגם כשהניבוי כבוי כולו ממרכז הבקרה, מדלגים על
        // חיפוש המילון לגמרי - מוצגות תמיד האותיות שנבחרו בפועל ב-multi-tap, גם
        // אם יש התאמה במילון לרצף.
        val dictionaryCandidates = if (!isPredictingIn(mode)) emptyList()
            else engine.candidatesFor(digitSequence) { word -> wordFrequency[word] ?: 0 }
        // מילים שהמשתמש הוסיף בעצמו (ר' addCurrentWordToDictionary) מוצגות ראשונות.
        val custom = customWords[digitSequence].orEmpty()
        candidates = (custom + dictionaryCandidates).distinct()
        val rawDisplay = when {
            candidates.isNotEmpty() -> candidates[candidateIndex.coerceIn(0, candidates.size - 1)]
            digitSequence.isEmpty() -> ""
            else -> fallbackLetters.toString()
        }
        ic.setComposingText(applyCase(rawDisplay, mode), 1)
        renderPanel()
    }

    private fun cycleCandidate(ic: InputConnection, direction: Int) {
        if (candidates.size <= 1) return
        candidateIndex = (candidateIndex + direction + candidates.size) % candidates.size
        ic.setComposingText(applyCase(candidates[candidateIndex]), 1)
        renderPanel()
    }

    /** רשת ביטחון עבור לחיצה ישירה על מועמדת (ראו ההערה ב-renderCandidateChips). */
    private fun selectCandidate(index: Int) {
        val ic = currentInputConnection ?: return
        if (index !in candidates.indices) return
        candidateIndex = index
        ic.setComposingText(applyCase(candidates[candidateIndex]), 1)
        renderPanel()
    }

    private fun commitCurrentWord(ic: InputConnection, appendSpace: Boolean) {
        val chosenFromDictionary = candidates.isNotEmpty()
        val canonicalWord = when {
            chosenFromDictionary -> candidates[candidateIndex.coerceIn(0, candidates.size - 1)]
            digitSequence.isNotEmpty() -> fallbackLetters.toString()
            else -> ""
        }
        val displayWord = applyCase(canonicalWord)
        if (displayWord.isNotEmpty()) {
            ic.commitText(if (appendSpace) "$displayWord " else displayWord, 1)
            if (chosenFromDictionary) recordWordUsage(canonicalWord)
        } else if (appendSpace) {
            ic.commitText(" ", 1)
        }
        resetComposing()
        renderPanel()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        resetComposing()
        isPunctuationMenuOpen = false
        isLanguageMenuOpen = false
        stopListening()
    }
}
