package com.future.settings.ui.theme
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureType
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.elevatedSurfaceColor
import com.future.sharednav.theme.dividerColor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ThemeConfig(
    val primaryColor: Color = Color.White,
    val isDarkMode: Boolean = true,
    val fontSizeMultiplier: Float = 1.0f,
    val itemSpacing: Dp = FutureDimens.itemSpacing,
    val borderRadius: Dp = FutureShapes.radiusXl
) {
    /**
     * אותה ערכה בדיוק כמו בשאר המערכת. הצבעים למטה היו העתק hex ידני של
     * FutureTheme - שני מקורות אמת שהיו צריכים להישאר מסונכרנים ידנית.
     */
    val futureTheme: FutureTheme = FutureTheme(isDarkMode = isDarkMode, accentColor = primaryColor)
    private val type = FutureType(fontSizeMultiplier)

    val backgroundColor: Color = futureTheme.backgroundColor
    val surfaceColor: Color = futureTheme.surfaceColor
    val glassColor: Color = futureTheme.elevatedSurfaceColor
    val textColor: Color = futureTheme.textColor
    val dividerColor: Color = futureTheme.dividerColor
    val dangerColor: Color = futureTheme.dangerColor

    val baseFontSize: TextUnit = type.bodyLarge
    val titleFontSize: TextUnit = type.title
    val summaryFontSize: TextUnit = type.summary
    val headerFontSize: TextUnit = type.display
}
