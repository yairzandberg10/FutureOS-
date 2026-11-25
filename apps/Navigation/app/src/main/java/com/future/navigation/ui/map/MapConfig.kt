package com.future.navigation.ui.map

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.future.navigation.ui.settings.MapThemeStore
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.rememberFutureType
import java.util.Calendar

/** תצוגת המפה שהמשתמש בחר בהגדרות מפה: אוטומטי (לפי השעה), בהירה או כהה. */
enum class MapTheme(val key: String) {
    AUTO("auto"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        /** ערך לא מוכר (או חסר) נופל ל-AUTO. */
        fun fromKey(key: String?): MapTheme = entries.firstOrNull { it.key == key } ?: AUTO
    }
}

object MapConfig {
    /**
     * אריחי וקטור חינמיים, בלי מפתח, בלי חשבון, מיועדים לשימוש אמיתי (לא
     * "דמו" כמו אריחי ברירת המחדל של MapLibre עצמה) - OpenFreeMap.
     */
    const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

    /** אותו ספק, סגנון כהה (בלילה). */
    const val DARK_STYLE_URL = "https://tiles.openfreemap.org/styles/dark"

    /**
     * לילה = אחרי 19:00 או לפני 06:00 (שעון המכשיר). זה הגיבוי המוסכם; חישוב
     * שקיעה אמיתי לא נעשה כאן.
     */
    fun isNight(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val hour = Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.HOUR_OF_DAY)
        return hour >= 19 || hour < 6
    }

    /** הסגנון לפי בחירה. ב-AUTO: כהה בלילה, בהיר ביום. */
    fun styleUrl(theme: MapTheme, nowMillis: Long = System.currentTimeMillis()): String = when (theme) {
        MapTheme.LIGHT -> STYLE_URL
        MapTheme.DARK -> DARK_STYLE_URL
        MapTheme.AUTO -> if (isNight(nowMillis)) DARK_STYLE_URL else STYLE_URL
    }

    /** הסגנון לפי ההגדרה השמורה (SharedPreferences). מעדיפים את הגרסה הזו כשיש Context. */
    fun styleUrl(context: Context): String = styleUrl(MapThemeStore.get(context).theme)

    /**
     * בלי Context (NavMapView הקיים): משתמש בבחירה שכבר נטענה. הבחירה נטענת כש-
     * MapThemeStore.get נקרא לראשונה (מסך הבית קורא לו), לפני כן - AUTO.
     */
    fun styleUrl(): String = styleUrl(MapThemeStore.currentTheme())

    /**
     * ייחוס חובה: נתוני המפה של OpenStreetMap ברישיון ODbL, שמחייב ייחוס
     * גלוי על כל מפה שמוצגת. כפתור ה-"i" של MapLibre לא נלחץ במכשיר מקשים,
     * ולכן הטקסט מוצג תמיד.
     */
    const val ATTRIBUTION = "© OpenStreetMap · OpenFreeMap"
}

/** שורת הייחוס בפינת המפה. חייבת להופיע מעל כל MapView. */
@Composable
fun BoxScope.MapAttribution() {
    val theme = LocalFutureTheme.current
    val type = rememberFutureType()
    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(FutureDimens.spacingXs)
            .background(theme.surfaceColor.copy(alpha = 0.75f), FutureShapes.xs)
            .padding(horizontal = FutureDimens.spacingXs, vertical = FutureDimens.spacingXxs)
    ) {
        Text(text = MapConfig.ATTRIBUTION, color = theme.textColor, fontSize = type.caption)
    }
}
