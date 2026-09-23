package com.future.translate.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.future.translate.data.HistoryEntry
import com.future.translate.data.HistoryStore
import com.future.translate.data.Languages
import com.future.translate.data.TranslatePrefs
import com.future.translate.data.TranslationEngine
import com.future.translate.speech.Listener
import com.future.translate.speech.Speaker
import com.google.mlkit.common.MlKitException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** מה שמוצג בכרטיס התוצאה. */
sealed interface Result {
    data object Empty : Result
    data object Translating : Result
    data object Downloading : Result
    data class Done(val text: String) : Result
    data class Failed(val message: String) : Result
}

/** שורה במצב שיחה: מה נאמר, בשפה של מי, והתרגום שלו. */
data class TalkLine(val text: String, val language: String, val translation: String?, val translationLanguage: String)

class TranslateViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = TranslationEngine()
    private val prefs = TranslatePrefs(application)
    val history = HistoryStore(application)
    val speaker = Speaker(application)
    val listener = Listener(application)

    private val _from = MutableStateFlow(prefs.from)
    val from: StateFlow<String> = _from.asStateFlow()

    private val _to = MutableStateFlow(prefs.to)
    val to: StateFlow<String> = _to.asStateFlow()

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()

    private val _result = MutableStateFlow<Result>(Result.Empty)
    val result: StateFlow<Result> = _result.asStateFlow()

    /** השפה שזוהתה כשהמקור הוא "זיהוי שפה" - מוצגת בתווית של כרטיס הקלט. */
    private val _detected = MutableStateFlow<String?>(null)
    val detected: StateFlow<String?> = _detected.asStateFlow()

    private val _recentLanguages = MutableStateFlow(prefs.recentLanguages)
    val recentLanguages: StateFlow<List<String>> = _recentLanguages.asStateFlow()

    private val _downloaded = MutableStateFlow<Set<String>>(emptySet())
    val downloaded: StateFlow<Set<String>> = _downloaded.asStateFlow()

    private val _downloading = MutableStateFlow<Set<String>>(emptySet())
    val downloading: StateFlow<Set<String>> = _downloading.asStateFlow()

    private val _saveHistory = MutableStateFlow(prefs.saveHistory)
    val saveHistory: StateFlow<Boolean> = _saveHistory.asStateFlow()

    private val _wifiOnly = MutableStateFlow(prefs.wifiOnly)
    val wifiOnly: StateFlow<Boolean> = _wifiOnly.asStateFlow()

    private val _talk = MutableStateFlow<List<TalkLine>>(emptyList())
    val talk: StateFlow<List<TalkLine>> = _talk.asStateFlow()

    /** מי מדבר עכשיו במצב שיחה: false = שפת המקור, true = שפת היעד. */
    private val _talkSecondSpeaker = MutableStateFlow(false)
    val talkSecondSpeaker: StateFlow<Boolean> = _talkSecondSpeaker.asStateFlow()

    private var translateJob: Job? = null

    init {
        refreshDownloaded()
    }

    // ---- קלט ושפות ----

    fun setText(value: String) {
        val clipped = value.take(MaxInput)
        if (clipped == _text.value) return
        _text.value = clipped
        scheduleTranslate()
    }

    fun setFrom(code: String) {
        _from.value = code
        prefs.from = code
        prefs.rememberLanguage(code)
        _recentLanguages.value = prefs.recentLanguages
        scheduleTranslate(immediate = true)
    }

    fun setTo(code: String) {
        _to.value = code
        prefs.to = code
        prefs.rememberLanguage(code)
        _recentLanguages.value = prefs.recentLanguages
        scheduleTranslate(immediate = true)
    }

    /**
     * החלפת כיוון. כשהמקור הוא "זיהוי שפה", השפה שזוהתה (או עברית) היא
     * שנכנסת ליעד; והתרגום הקיים הופך לטקסט החדש, כמו בכל מתרגם.
     */
    fun swap() {
        val oldFrom = if (_from.value == Languages.AUTO) _detected.value ?: "he" else _from.value
        val oldTo = _to.value
        val done = _result.value as? Result.Done
        _from.value = oldTo
        _to.value = oldFrom
        prefs.from = oldTo
        prefs.to = oldFrom
        if (done != null) _text.value = done.text
        scheduleTranslate(immediate = true)
    }

    /** טוען רשומה מההיסטוריה חזרה למסך התרגום. */
    fun load(entry: HistoryEntry) {
        _from.value = entry.from
        _to.value = entry.to
        prefs.from = entry.from
        prefs.to = entry.to
        _text.value = entry.src
        _detected.value = null
        _result.value = Result.Done(entry.dst)
        translateJob?.cancel()
    }

    private fun scheduleTranslate(immediate: Boolean = false) {
        translateJob?.cancel()
        val source = _text.value.trim()
        if (source.isEmpty()) {
            _result.value = Result.Empty
            _detected.value = null
            return
        }
        translateJob = viewModelScope.launch {
            // המתנה קצרה בזמן הקלדה, כדי לא לתרגם כל אות בנפרד.
            if (!immediate) delay(TypingPauseMillis)
            _result.value = Result.Translating
            _result.value = translate(source, _from.value, _to.value) { detected -> _detected.value = detected }
        }
    }

    private suspend fun translate(
        source: String,
        fromCode: String,
        toCode: String,
        onDetected: (String?) -> Unit = {},
    ): Result {
        return try {
            val from = if (fromCode == Languages.AUTO) {
                val detected = engine.identify(source)
                onDetected(detected)
                detected ?: return Result.Failed("לא זוהתה שפה")
            } else fromCode
            val text = engine.translate(source, from, toCode, _wifiOnly.value) { phase ->
                if (phase == TranslationEngine.Phase.Downloading) _result.value = Result.Downloading
            }
            refreshDownloaded()
            Result.Done(text)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: MlKitException) {
            Result.Failed(
                when (e.errorCode) {
                    MlKitException.NETWORK_ISSUE, MlKitException.UNAVAILABLE -> "אין חיבור להורדת השפה"
                    MlKitException.NOT_ENOUGH_SPACE -> "אין מקום להורדת השפה"
                    else -> "התרגום נכשל"
                }
            )
        } catch (e: Exception) {
            Result.Failed("התרגום נכשל")
        }
    }

    // ---- היסטוריה ----

    /** רושם את התרגום הנוכחי בהיסטוריה (אם מופעלת). נקרא כשעוזבים את השדה. */
    fun commit(): HistoryEntry? {
        val done = _result.value as? Result.Done ?: return null
        val source = _text.value.trim()
        if (source.isEmpty()) return null
        val from = resolvedFrom() ?: return null
        val existing = history.find(source, from, _to.value)
        if (!_saveHistory.value && existing == null) return null
        return history.record(source, done.text, from, _to.value)
    }

    /** "שמור" - שומר את התרגום הנוכחי או מסיר אותו מהשמורים. true = נשמר. */
    fun toggleSaved(): Boolean? {
        val done = _result.value as? Result.Done ?: return null
        val source = _text.value.trim()
        val from = resolvedFrom() ?: return null
        val existing = history.find(source, from, _to.value)
        return if (existing?.saved == true) {
            history.setSaved(existing.id, false)
            false
        } else {
            history.record(source, done.text, from, _to.value, saved = true)
            true
        }
    }

    fun isCurrentSaved(entries: List<HistoryEntry>): Boolean {
        val from = resolvedFrom() ?: return false
        val source = _text.value.trim()
        return entries.any { it.saved && it.src == source && it.from == from && it.to == _to.value }
    }

    fun clearHistory() = history.clearUnsaved()

    private fun resolvedFrom(): String? =
        if (_from.value == Languages.AUTO) _detected.value else _from.value

    // ---- הורדת שפה והגדרות ----

    fun refreshDownloaded() {
        viewModelScope.launch {
            _downloaded.value = try {
                engine.downloadedLanguages()
            } catch (e: Exception) {
                _downloaded.value
            }
        }
    }

    /** מוריד מודל שפה. [onDone] מקבל false כשההורדה נכשלה. */
    fun download(code: String, onDone: (Boolean) -> Unit) {
        if (code in _downloading.value) return
        _downloading.value = _downloading.value + code
        viewModelScope.launch {
            val ok = try {
                engine.download(code, _wifiOnly.value)
                true
            } catch (e: Exception) {
                false
            }
            _downloading.value = _downloading.value - code
            refreshDownloaded()
            onDone(ok)
        }
    }

    fun delete(code: String) {
        viewModelScope.launch {
            try {
                engine.delete(code)
            } catch (e: Exception) {
                // מודל שכבר נמחק או בשימוש - הרשימה תתעדכן בכל מקרה.
            }
            refreshDownloaded()
        }
    }

    fun setSaveHistory(on: Boolean) {
        _saveHistory.value = on
        prefs.saveHistory = on
    }

    fun setWifiOnly(on: Boolean) {
        _wifiOnly.value = on
        prefs.wifiOnly = on
    }

    // ---- שיחה ----

    /** השפות של שני הצדדים. "זיהוי שפה" לא מתאים לשיחה - עברית במקומו. */
    fun talkLanguages(): Pair<String, String> {
        val a = if (_from.value == Languages.AUTO) "he" else _from.value
        return a to _to.value
    }

    fun switchTalkSpeaker() {
        _talkSecondSpeaker.value = !_talkSecondSpeaker.value
    }

    /** מוסיף את מה שנאמר (או הוקלד) בצד הנוכחי, מתרגם, ומעביר את התור לצד השני. */
    fun addTalkLine(text: String) {
        val (a, b) = talkLanguages()
        val second = _talkSecondSpeaker.value
        val lang = if (second) b else a
        val target = if (second) a else b
        val index = _talk.value.size
        _talk.value = _talk.value + TalkLine(text, lang, null, target)
        _talkSecondSpeaker.value = !second
        viewModelScope.launch {
            val translated = (translate(text, lang, target) as? Result.Done)?.text
            _talk.value = _talk.value.mapIndexed { i, line ->
                if (i == index) line.copy(translation = translated ?: "התרגום נכשל") else line
            }
            if (translated != null) speaker.speak(translated, target)
        }
    }

    override fun onCleared() {
        engine.close()
        speaker.shutdown()
        listener.stop()
    }

    private companion object {
        const val MaxInput = 1000
        const val TypingPauseMillis = 450L
    }
}
