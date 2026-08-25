package com.future.keyboard

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.future.keyboard.theme.ThemeClient

/**
 * שירות שיטת קלט (IME) אמיתי: המכשיר הוא מקלדת T9 פיזית בלי מסך מגע (ראו
 * ההערות המקבילות ב-dialer וב-FutureUI), ולכן אין כאן מקלדת מגע על המסך -
 * במקום זאת השירות מיירט את לחיצות מקשי הספרות הפיזיים (onKeyDown) ומתרגם
 * אותן לטקסט אמיתי לפי [T9Engine], בדיוק כמו בטלפונים פיזיים ישנים.
 *
 * מקשי הבקרה (ראו [InputMode] ו-[onKeyDown]):
 * - ספרות 2-9: אותיות T9 (לפי [T9DigitMap][com.future.sharednav.t9.T9DigitMap]) או ניבוי מילון.
 * - חצים שמאלה/ימינה: מעבר בין מועמדות הניבוי כשיש יותר ממילה אחת מתאימה.
 * - # קצר: מעביר בין מצבי הקלט במחזור קבוע - עברית (ללא ניבוי) → עברית
 *   (עם ניבוי) → אנגלית ABC → אנגלית Abc → אנגלית abc → מספרים - ובחזרה.
 * - * קצר: פותח תפריט סימני פיסוק (ניווט בחצים, אישור במרכז, ביטול בחזור/מחיקה).
 * - 0 קצר: רווח. 0 ארוך (מוחזק): תמלול קולי.
 * - מחיקה (DEL): מוחקת אות אחרונה מהמילה בהרכבה, ואז תווים מהטקסט שכבר הוצב.
 */
class KeyboardService : InputMethodService() {

    private enum class InputMode {
        HEBREW_MULTITAP, HEBREW_PREDICTIVE, ENGLISH_UPPER, ENGLISH_CAPITALIZE, ENGLISH_LOWER, NUMERIC;

        companion object {
            val CYCLE_ORDER = listOf(HEBREW_MULTITAP, HEBREW_PREDICTIVE, ENGLISH_UPPER, ENGLISH_CAPITALIZE, ENGLISH_LOWER, NUMERIC)
        }
    }

    companion object {
        // רשימת סימני הפיסוק הזמינים בתפריט * - אין ייצוג ויזואלי שלהם על המקשים
        // עצמם, ולכן הם מרוכזים בתפריט אחד שנפתח בלחיצה קצרה על הכוכבית.
        private val PUNCTUATION_SYMBOLS = listOf(
            '.', ',', '?', '!', '\'', '"', '-', '@', '/', ':', ';', '(', ')', '+', '=', '_', '%', '&'
        )
    }

    private lateinit var prefs: SharedPreferences
    private var engine: T9Engine = T9Engine(T9Engine.Language.HEBREW)

    // רצף הספרות שנלחצו עד כה - מפתח החיפוש במילון (מיקום אחד לכל אות, בלי
    // תלות באיזו אות נבחרה בפועל בתוך אותו מקש).
    private var digitSequence: String = ""
    private var candidateIndex: Int = 0
    private var candidates: List<String> = emptyList()
    private var isPredictiveField = true

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

    // תפריט סימני הפיסוק (מקש * קצר): פתוח/סגור והאינדקס הנבחר כרגע בתוכו.
    private var isPunctuationMenuOpen = false
    private var punctuationIndex = 0

    private lateinit var hintView: TextView
    private lateinit var candidatesRow: LinearLayout
    private lateinit var nextHintChip: TextView

    private var textColor: Int = Color.BLACK
    private var mutedTextColor: Int = Color.DKGRAY
    private var accentColor: Int = Color.BLUE
    private var backgroundColor: Int = 0xFFEFEFEF.toInt()
    private var chipBackgroundColor: Int = Color.WHITE

    // תמלול קולי בהחזקת מקש 0: voiceInputArmed מונע הפעלה חוזרת של ההאזנה כל
    // עוד המקש נשאר לחוץ (Android שולח onKeyDown חוזר ונשנה עם repeatCount
    // עולה כל עוד המקש מוחזק).
    private val longPressRepeatThreshold = 6
    private var voiceInputArmed = false
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("t9_keyboard_prefs", MODE_PRIVATE)
        engineLanguageFor(currentMode())?.let { engine = T9Engine(it) }
        loadFrequencies()
    }

    private fun currentMode(): InputMode {
        val stored = prefs.getString("input_mode", InputMode.HEBREW_PREDICTIVE.name)
        return try { InputMode.valueOf(stored ?: InputMode.HEBREW_PREDICTIVE.name) } catch (e: Exception) { InputMode.HEBREW_PREDICTIVE }
    }

    private fun engineLanguageFor(mode: InputMode): T9Engine.Language? = when (mode) {
        InputMode.HEBREW_MULTITAP, InputMode.HEBREW_PREDICTIVE -> T9Engine.Language.HEBREW
        InputMode.ENGLISH_UPPER, InputMode.ENGLISH_CAPITALIZE, InputMode.ENGLISH_LOWER -> T9Engine.Language.ENGLISH
        InputMode.NUMERIC -> null
    }

    private fun modeLabel(mode: InputMode): String = when (mode) {
        InputMode.HEBREW_MULTITAP -> "עב"
        InputMode.HEBREW_PREDICTIVE -> "עב-ניבוי"
        InputMode.ENGLISH_UPPER -> "EN-ABC"
        InputMode.ENGLISH_CAPITALIZE -> "EN-Abc"
        InputMode.ENGLISH_LOWER -> "en-abc"
        InputMode.NUMERIC -> "123"
    }

    /** מפעיל שיבוץ אותיות רישיות בהתאם למצב הנוכחי - רלוונטי לאנגלית בלבד. */
    private fun applyCase(text: String, mode: InputMode = currentMode()): String = when (mode) {
        InputMode.ENGLISH_UPPER -> text.uppercase()
        InputMode.ENGLISH_CAPITALIZE -> text.replaceFirstChar { it.uppercaseChar() }
        else -> text
    }

    /** מעביר למצב הבא במחזור הקבוע (ראו [InputMode.CYCLE_ORDER]) - מקש # קצר. */
    private fun advanceMode() {
        val order = InputMode.CYCLE_ORDER
        val next = order[(order.indexOf(currentMode()) + 1) % order.size]
        prefs.edit().putString("input_mode", next.name).apply()
        engineLanguageFor(next)?.let { engine = T9Engine(it); loadFrequencies() }
        resetComposing()
        updateCandidatesView()
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
        wordFrequency[word] = (wordFrequency[word] ?: 0) + 1
        saveFrequencies()
    }

    // --- UI -------------------------------------------------------------------

    override fun onCreateInputView(): View {
        applyTheme()

        hintView = TextView(this).apply {
            textSize = 12f
            setTextColor(mutedTextColor)
            gravity = Gravity.END
        }

        candidatesRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        nextHintChip = TextView(this).apply {
            text = "◂ ▸"
            textSize = 13f
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setTextColor(accentColor)
        }

        val candidatesWithHint = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(candidatesRow, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(nextHintChip)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setPadding(dp(24), dp(16), dp(24), dp(16))
            addView(hintView)
            addView(candidatesWithHint)
        }
        updateCandidatesView()
        return container
    }

    private fun applyTheme() {
        val theme = ThemeClient.getTheme(this)
        if (theme.isDarkMode) {
            backgroundColor = 0xFF1C1C1E.toInt()
            textColor = Color.WHITE
            mutedTextColor = 0xFFB0B0B0.toInt()
            chipBackgroundColor = 0xFF2C2C2E.toInt()
        } else {
            backgroundColor = 0xFFEFEFEF.toInt()
            textColor = Color.BLACK
            mutedTextColor = Color.DKGRAY
            chipBackgroundColor = Color.WHITE
        }
        accentColor = theme.primaryColor
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        resetComposing()
        isPunctuationMenuOpen = false
        val inputClass = attribute?.inputType?.and(InputType.TYPE_MASK_CLASS)
        isPredictiveField = inputClass == InputType.TYPE_CLASS_TEXT
        updateCandidatesView()
    }

    private fun resetComposing() {
        digitSequence = ""
        candidates = emptyList()
        candidateIndex = 0
        fallbackLetters.clear()
        lastTapDigit = null
    }

    private fun chipDrawable(highlighted: Boolean): GradientDrawable = GradientDrawable().apply {
        cornerRadius = 24f
        setColor(if (highlighted) accentColor else chipBackgroundColor)
    }

    private fun showStatus(text: String) {
        hintView.text = text
        if (::candidatesRow.isInitialized) candidatesRow.removeAllViews()
        if (::nextHintChip.isInitialized) nextHintChip.visibility = View.GONE
    }

    private fun updateCandidatesView() {
        if (!::hintView.isInitialized) return
        val mode = currentMode()
        val label = modeLabel(mode)

        if (isListening) {
            showStatus("[$label] 🎤 ${getString(R.string.voice_listening)}")
            return
        }

        if (isPunctuationMenuOpen) {
            hintView.text = "[$label] סימני פיסוק - ◂/▸ לניווט, מרכז לבחירה"
            candidatesRow.removeAllViews()
            PUNCTUATION_SYMBOLS.forEachIndexed { index, symbol ->
                val isSelected = index == punctuationIndex
                val chip = TextView(this).apply {
                    text = symbol.toString()
                    textSize = 15f
                    setPadding(dp(20), dp(8), dp(20), dp(8))
                    setTextColor(if (isSelected) Color.WHITE else textColor)
                    typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    background = chipDrawable(isSelected)
                    setOnClickListener { insertPunctuation(index) }
                }
                val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                params.marginEnd = dp(8)
                candidatesRow.addView(chip, params)
            }
            nextHintChip.visibility = View.GONE
            return
        }

        val emptyHint = if (mode == InputMode.NUMERIC) "הקש ספרות" else "הקש ספרות כדי לכתוב מילים"
        hintView.text = when {
            digitSequence.isEmpty() -> "[$label] $emptyHint"
            candidates.isEmpty() -> "[$label] $digitSequence"
            else -> "[$label]"
        }

        candidatesRow.removeAllViews()
        candidates.forEachIndexed { index, word ->
            val isSelected = index == candidateIndex.coerceIn(0, (candidates.size - 1).coerceAtLeast(0))
            val chip = TextView(this).apply {
                text = applyCase(word, mode)
                textSize = 15f
                setPadding(dp(20), dp(8), dp(20), dp(8))
                setTextColor(if (isSelected) Color.WHITE else textColor)
                typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                background = chipDrawable(isSelected)
                // אין מסך מגע במכשיר היעד, כך שלחיצה בפועל תמיד עוברת דרך מקשי
                // החיצים הפיזיים (cycleCandidate) - ה-click listener כאן הוא רשת
                // ביטחון בלבד למקרה שהשירות רץ תחת קלט מגע/עכבר (למשל בדיקה באמולטור).
                setOnClickListener { selectCandidate(index) }
            }
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.marginEnd = dp(8)
            candidatesRow.addView(chip, params)
        }

        nextHintChip.visibility = if (candidates.size > 1) View.VISIBLE else View.GONE
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

        if (!isPredictiveField) return super.onKeyDown(keyCode, event)

        // כשתפריט הפיסוק פתוח, רק הניווט/האישור/הביטול פעילים - כל מקש אחר
        // נבלע כדי שלא יקרו פעולות לא צפויות על טקסט שכבר הורכב לפני הפתיחה.
        if (isPunctuationMenuOpen) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    punctuationIndex = (punctuationIndex - 1 + PUNCTUATION_SYMBOLS.size) % PUNCTUATION_SYMBOLS.size
                    updateCandidatesView()
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    punctuationIndex = (punctuationIndex + 1) % PUNCTUATION_SYMBOLS.size
                    updateCandidatesView()
                }
                KeyEvent.KEYCODE_DPAD_CENTER -> insertPunctuation(punctuationIndex)
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_STAR -> closePunctuationMenu()
                else -> { /* נבלע - שום פעולה */ }
            }
            return true
        }

        val mode = currentMode()

        if (mode == InputMode.NUMERIC) {
            if (keyCode == KeyEvent.KEYCODE_0) {
                // כמו במקשים אחרים - לחיצה קצרה מציבה את הספרה, ארוכה מפעילה תמלול.
                if (event.repeatCount == 0) {
                    voiceInputArmed = false
                    ic.commitText("0", 1)
                } else if (!voiceInputArmed && event.repeatCount > longPressRepeatThreshold) {
                    voiceInputArmed = true
                    startVoiceTranscription()
                }
                return true
            }
            val digit = digitCharFor(keyCode)
            if (digit != null) {
                ic.commitText(digit.toString(), 1)
                return true
            }
            if (keyCode == KeyEvent.KEYCODE_POUND && event.repeatCount == 0) {
                advanceMode()
                return true
            }
            if (keyCode == KeyEvent.KEYCODE_STAR && event.repeatCount == 0) {
                openPunctuationMenu(ic)
                return true
            }
            if (keyCode == KeyEvent.KEYCODE_DEL) {
                ic.deleteSurroundingText(1, 0)
                return true
            }
            return super.onKeyDown(keyCode, event)
        }

        val digit = digitCharFor(keyCode)
        if (digit != null && digit != '0' && digit != '1') {
            handleDigitPress(digit, ic)
            return true
        }

        when (keyCode) {
            // * קצר - פותח את תפריט סימני הפיסוק (ראו הטיפול למעלה כשהתפריט פתוח).
            KeyEvent.KEYCODE_STAR -> {
                if (event.repeatCount == 0) openPunctuationMenu(ic)
                return true
            }
            // חצים שמאלה/ימינה - מעבר בין מועמדות הניבוי כשיש יותר ממילה אחת
            // מתאימה; אם אין כמה מועמדות, מתנהג כניווט רגיל (ברירת המחדל של המערכת).
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (candidates.size > 1) {
                    cycleCandidate(ic, -1)
                    return true
                }
                return super.onKeyDown(keyCode, event)
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (candidates.size > 1) {
                    cycleCandidate(ic, 1)
                    return true
                }
                return super.onKeyDown(keyCode, event)
            }
            // # קצר - מאשר את המילה הנוכחית (אם יש) ועובר למצב הקלט הבא במחזור
            // הקבוע (ראו [InputMode]). פועל רק על ה-DOWN הראשון כדי ש-repeat
            // events נוספים בזמן החזקת המקש לא ימשיכו לקפוץ בין מצבים.
            KeyEvent.KEYCODE_POUND -> {
                if (event.repeatCount == 0) {
                    if (digitSequence.isNotEmpty()) commitCurrentWord(ic, appendSpace = false)
                    advanceMode()
                }
                return true
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
            KeyEvent.KEYCODE_0 -> {
                // לחיצה קצרה - כמו ברוב מכשירי T9, 0 לא ממופה לאותיות ומציב רווח.
                // לחיצה ארוכה (repeatCount עולה כל עוד המקש מוחזק) - מפעילה תמלול קולי.
                if (event.repeatCount == 0) {
                    voiceInputArmed = false
                    if (digitSequence.isNotEmpty()) commitCurrentWord(ic, appendSpace = false)
                    ic.commitText(" ", 1)
                } else if (!voiceInputArmed && event.repeatCount > longPressRepeatThreshold) {
                    voiceInputArmed = true
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
            voiceInputArmed = false
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun startVoiceTranscription() {
        if (isListening) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            showStatus(getString(R.string.voice_permission_missing))
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            showStatus(getString(R.string.voice_unavailable))
            return
        }

        resetComposing()
        isListening = true
        updateCandidatesView()

        val recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer = recognizer
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) = stopListening()
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    currentInputConnection?.commitText("$text ", 1)
                }
                stopListening()
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val voiceLanguage = engineLanguageFor(currentMode()) ?: T9Engine.Language.HEBREW
        val locale = if (voiceLanguage == T9Engine.Language.HEBREW) "he-IL" else "en-US"
        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
        }
        recognizer.startListening(recognizerIntent)
    }

    private fun stopListening() {
        isListening = false
        speechRecognizer?.destroy()
        speechRecognizer = null
        updateCandidatesView()
    }

    private fun openPunctuationMenu(ic: InputConnection) {
        if (digitSequence.isNotEmpty()) commitCurrentWord(ic, appendSpace = false)
        isPunctuationMenuOpen = true
        punctuationIndex = 0
        updateCandidatesView()
    }

    private fun closePunctuationMenu() {
        isPunctuationMenuOpen = false
        updateCandidatesView()
    }

    private fun insertPunctuation(index: Int) {
        val ic = currentInputConnection ?: return
        if (index !in PUNCTUATION_SYMBOLS.indices) return
        ic.commitText(PUNCTUATION_SYMBOLS[index].toString(), 1)
        closePunctuationMenu()
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
        // במצב "עברית ללא ניבוי" מדלגים על חיפוש המילון לגמרי - מוצגות תמיד
        // האותיות שנבחרו בפועל ב-multi-tap, גם אם יש התאמה במילון לרצף.
        candidates = if (mode == InputMode.HEBREW_MULTITAP) emptyList()
            else engine.candidatesFor(digitSequence) { word -> wordFrequency[word] ?: 0 }
        val rawDisplay = when {
            candidates.isNotEmpty() -> candidates[candidateIndex.coerceIn(0, candidates.size - 1)]
            digitSequence.isEmpty() -> ""
            else -> fallbackLetters.toString()
        }
        ic.setComposingText(applyCase(rawDisplay, mode), 1)
        updateCandidatesView()
    }

    private fun cycleCandidate(ic: InputConnection, direction: Int) {
        if (candidates.size <= 1) return
        candidateIndex = (candidateIndex + direction + candidates.size) % candidates.size
        ic.setComposingText(applyCase(candidates[candidateIndex]), 1)
        updateCandidatesView()
    }

    /** רשת ביטחון עבור לחיצה ישירה על מועמדת (ראו ההערה ב-updateCandidatesView). */
    private fun selectCandidate(index: Int) {
        val ic = currentInputConnection ?: return
        if (index !in candidates.indices) return
        candidateIndex = index
        ic.setComposingText(applyCase(candidates[candidateIndex]), 1)
        updateCandidatesView()
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
        updateCandidatesView()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        resetComposing()
        isPunctuationMenuOpen = false
        stopListening()
    }
}
