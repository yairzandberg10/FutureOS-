package com.future.camera.data

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.extensions.ExtensionMode

enum class AspectOption(val label: String) { RATIO_4_3("4:3"), RATIO_16_9("16:9") }

enum class PhotoQualityOption(val label: String) { QUALITY("איכות מרבית"), SPEED("צילום מהיר") }

enum class VideoQualityOption(val label: String) { SD("480p"), HD("720p"), FHD("1080p") }

/**
 * מצבי צילום מיוחדים של CameraX Extensions. מוצגים בהגדרות רק אלה שהחומרה
 * באמת תומכת בהם (ExtensionsManager.isExtensionAvailable) - לא כל מכשיר
 * תומך באף אחד מהם.
 */
enum class SceneOption(val mode: Int, val label: String) {
    NONE(ExtensionMode.NONE, "רגיל"),
    AUTO(ExtensionMode.AUTO, "אוטומטי"),
    HDR(ExtensionMode.HDR, "HDR"),
    NIGHT(ExtensionMode.NIGHT, "לילה"),
    BOKEH(ExtensionMode.BOKEH, "פורטרט"),
    FACE_RETOUCH(ExtensionMode.FACE_RETOUCH, "ריכוך פנים"),
}

/** ההגדרות של המצלמה, נשמרות בין הפעלות. */
data class CameraOptions(
    val aspect: AspectOption = AspectOption.RATIO_4_3,
    val photoQuality: PhotoQualityOption = PhotoQualityOption.QUALITY,
    val videoQuality: VideoQualityOption = VideoQualityOption.HD,
    val scene: SceneOption = SceneOption.NONE,
    val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    val timerSeconds: Int = 0,
    val showGrid: Boolean = false,
) {
    companion object {
        private const val PREFS = "camera_options"

        fun load(context: Context): CameraOptions {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            fun <T : Enum<T>> read(key: String, values: Array<T>, default: T): T =
                values.firstOrNull { it.name == p.getString(key, null) } ?: default
            return CameraOptions(
                aspect = read("aspect", AspectOption.entries.toTypedArray(), AspectOption.RATIO_4_3),
                photoQuality = read("photo_quality", PhotoQualityOption.entries.toTypedArray(), PhotoQualityOption.QUALITY),
                videoQuality = read("video_quality", VideoQualityOption.entries.toTypedArray(), VideoQualityOption.HD),
                scene = read("scene", SceneOption.entries.toTypedArray(), SceneOption.NONE),
                flashMode = p.getInt("flash", ImageCapture.FLASH_MODE_OFF),
                timerSeconds = p.getInt("timer", 0),
                showGrid = p.getBoolean("grid", false),
            )
        }
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("aspect", aspect.name)
            .putString("photo_quality", photoQuality.name)
            .putString("video_quality", videoQuality.name)
            .putString("scene", scene.name)
            .putInt("flash", flashMode)
            .putInt("timer", timerSeconds)
            .putBoolean("grid", showGrid)
            .apply()
    }
}
