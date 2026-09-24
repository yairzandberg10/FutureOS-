package com.future.music.playback

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * אפקטי הסאונד של הנגן - אקולייזר מלא (כל הפסים של המכשיר), הגברת באס,
 * סאונד 3D (Virtualizer) והגברת עוצמה (LoudnessEnhancer), קשורים ל-audio
 * session של ExoPlayer ב-MusicPlaybackService. השירות והמסכים רצים באותו
 * תהליך, כך שהמסכים משנים את ההגדרות כאן ישירות.
 *
 * ההגדרות נשמרות לכל התקן פלט בנפרד (רמקול הטלפון, או כל אוזניות/בוקסה
 * לפי כתובת ה-Bluetooth שלה), ומוחלפות אוטומטית כשמתחברים להתקן אחר -
 * כמו פרופיל סאונד באפליקציות של יצרני האוזניות.
 */
object AudioFx {
    private const val TAG = "AudioFx"
    const val SPEAKER = "speaker"

    data class Settings(
        val enabled: Boolean = true,
        val bandLevels: List<Int> = emptyList(),
        val presetName: String = "רגיל",
        val bass: Int = 0,
        val virtualizer: Int = 0,
        val loudness: Int = 0,
        /** תקרת עוצמה באחוזים (100 = בלי הגבלה) - להגנה על השמיעה/הרמקול. */
        val volumeLimit: Int = 100,
        /** המשך ניגון אוטומטי כשההתקן מתחבר. */
        val autoResume: Boolean = false,
    )

    data class Band(val index: Int, val centerHz: Int)

    private var prefs: SharedPreferences? = null
    private var audioManager: AudioManager? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizerFx: Virtualizer? = null
    private var loudnessFx: LoudnessEnhancer? = null
    private var deviceCallback: AudioDeviceCallback? = null

    /** נקרא כשהתקן Bluetooth עם "המשך ניגון" מתחבר. */
    var onResumeRequested: (() -> Unit)? = null

    var bands: List<Band> = emptyList()
        private set
    /** טווח הפסים במילי-בל (בדרך כלל -1500..1500). */
    var levelRange: IntRange = -1500..1500
        private set

    private val _device = MutableStateFlow(SPEAKER)
    /** מזהה התקן הפלט הנוכחי: SPEAKER או כתובת Bluetooth. */
    val device: StateFlow<String> = _device

    private val _deviceName = MutableStateFlow("רמקול הטלפון")
    val deviceName: StateFlow<String> = _deviceName

    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings

    fun attach(context: Context, audioSessionId: Int) {
        val app = context.applicationContext
        prefs = app.getSharedPreferences("audio_fx", Context.MODE_PRIVATE)
        audioManager = app.getSystemService(AudioManager::class.java)
        release()
        equalizer = runCatching { Equalizer(0, audioSessionId) }.onFailure { Log.e(TAG, "eq", it) }.getOrNull()
        bassBoost = runCatching { BassBoost(0, audioSessionId) }.getOrNull()
        virtualizerFx = runCatching { Virtualizer(0, audioSessionId) }.getOrNull()
        loudnessFx = runCatching { LoudnessEnhancer(audioSessionId) }.getOrNull()
        equalizer?.let { eq ->
            val range = eq.bandLevelRange
            levelRange = range[0].toInt()..range[1].toInt()
            bands = (0 until eq.numberOfBands).map { Band(it, eq.getCenterFreq(it.toShort()) / 1000) }
        }
        detectDevice()
        registerDeviceCallback()
    }

    fun release() {
        listOf(equalizer, bassBoost, virtualizerFx, loudnessFx).forEach { runCatching { it?.release() } }
        equalizer = null; bassBoost = null; virtualizerFx = null; loudnessFx = null
        deviceCallback?.let { audioManager?.unregisterAudioDeviceCallback(it) }
        deviceCallback = null
    }

    val hasEqualizer get() = equalizer != null

    /** שמות הפריסטים המובנים של המכשיר (Rock, Pop, Jazz...). */
    fun systemPresets(): List<String> = equalizer?.let { eq ->
        (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) }
    } ?: emptyList()

    fun update(transform: (Settings) -> Settings) {
        val next = transform(_settings.value)
        _settings.value = next
        save(next)
        apply(next)
    }

    fun setBand(index: Int, level: Int) = update { s ->
        val levels = currentLevels(s).toMutableList()
        if (index in levels.indices) levels[index] = level.coerceIn(levelRange)
        s.copy(bandLevels = levels, presetName = "מותאם אישית")
    }

    fun usePreset(name: String) {
        val eq = equalizer ?: return
        val index = systemPresets().indexOf(name)
        if (index < 0) return
        runCatching { eq.usePreset(index.toShort()) }
        val levels = bands.map { eq.getBandLevel(it.index.toShort()).toInt() }
        update { it.copy(bandLevels = levels, presetName = name) }
    }

    /** פריסטים "אפקטים" בסגנון אפליקציות אוזניות - שילוב של EQ, באס ו-3D. */
    fun useEffect(effect: Effect) {
        val max = levelRange.last
        val levels = bands.map { band ->
            val f = band.centerHz
            when (effect) {
                Effect.FLAT -> 0
                Effect.PARTY -> when { f < 250 -> max * 2 / 3; f > 6000 -> max / 2; else -> max / 6 }
                Effect.BASS -> if (f < 400) max else 0
                Effect.VOCAL -> if (f in 800..4000) max / 2 else -max / 6
                Effect.CLEAR -> if (f > 3000) max / 2 else 0
            }.coerceIn(levelRange)
        }
        update {
            it.copy(
                enabled = true,
                bandLevels = levels,
                presetName = effect.label,
                bass = when (effect) { Effect.PARTY -> 600; Effect.BASS -> 900; else -> 0 },
                virtualizer = when (effect) { Effect.PARTY -> 700; Effect.CLEAR -> 300; else -> 0 },
            )
        }
    }

    enum class Effect(val label: String) {
        FLAT("רגיל"), PARTY("מסיבה"), BASS("באס עמוק"), VOCAL("קולות"), CLEAR("צלול")
    }

    fun reset() = update { Settings(volumeLimit = it.volumeLimit, autoResume = it.autoResume) }

    private fun currentLevels(s: Settings): List<Int> =
        if (s.bandLevels.size == bands.size) s.bandLevels else List(bands.size) { 0 }

    private fun apply(s: Settings) {
        runCatching {
            equalizer?.let { eq ->
                eq.enabled = s.enabled
                currentLevels(s).forEachIndexed { i, level -> eq.setBandLevel(i.toShort(), level.toShort()) }
            }
            bassBoost?.let { it.enabled = s.enabled && s.bass > 0; if (it.strengthSupported) it.setStrength(s.bass.toShort()) }
            virtualizerFx?.let { it.enabled = s.enabled && s.virtualizer > 0; if (it.strengthSupported) it.setStrength(s.virtualizer.toShort()) }
            loudnessFx?.let { it.setTargetGain(s.loudness); it.enabled = s.enabled && s.loudness > 0 }
        }.onFailure { Log.e(TAG, "apply", it) }
        enforceVolumeLimit(s)
    }

    /** מוריד את העוצמה אם היא מעל התקרה של ההתקן הנוכחי. */
    fun enforceVolumeLimit(s: Settings = _settings.value) {
        val am = audioManager ?: return
        if (s.volumeLimit >= 100) return
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val cap = (max * s.volumeLimit / 100f).toInt().coerceAtLeast(1)
        if (am.getStreamVolume(AudioManager.STREAM_MUSIC) > cap) {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, cap, 0)
        }
    }

    // ---- התקן הפלט הנוכחי ----

    private fun detectDevice() {
        val am = audioManager ?: return
        val bt = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
        }
        val id = bt?.address?.takeIf { it.isNotBlank() } ?: SPEAKER
        val name = bt?.productName?.toString()?.takeIf { it.isNotBlank() } ?: "רמקול הטלפון"
        val changed = id != _device.value
        val connectedNow = changed && id != SPEAKER
        _device.value = id
        _deviceName.value = name
        _settings.value = load(id)
        apply(_settings.value)
        if (connectedNow && _settings.value.autoResume) onResumeRequested?.invoke()
    }

    private fun registerDeviceCallback() {
        val am = audioManager ?: return
        val cb = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = detectDevice()
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = detectDevice()
        }
        am.registerAudioDeviceCallback(cb, Handler(Looper.getMainLooper()))
        deviceCallback = cb
    }

    // ---- שמירה ----

    private fun save(s: Settings) {
        val p = prefs ?: return
        val k = _device.value
        p.edit()
            .putBoolean("$k.enabled", s.enabled)
            .putString("$k.bands", s.bandLevels.joinToString(","))
            .putString("$k.preset", s.presetName)
            .putInt("$k.bass", s.bass)
            .putInt("$k.virtualizer", s.virtualizer)
            .putInt("$k.loudness", s.loudness)
            .putInt("$k.volumeLimit", s.volumeLimit)
            .putBoolean("$k.autoResume", s.autoResume)
            .apply()
    }

    private fun load(k: String): Settings {
        val p = prefs ?: return Settings()
        return Settings(
            enabled = p.getBoolean("$k.enabled", true),
            bandLevels = p.getString("$k.bands", "")!!.split(',').mapNotNull { it.toIntOrNull() },
            presetName = p.getString("$k.preset", "רגיל")!!,
            bass = p.getInt("$k.bass", 0),
            virtualizer = p.getInt("$k.virtualizer", 0),
            loudness = p.getInt("$k.loudness", 0),
            volumeLimit = p.getInt("$k.volumeLimit", 100),
            autoResume = p.getBoolean("$k.autoResume", false),
        )
    }
}
