package com.future.sharednav.theme

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * סקאלת טיפוגרפיה אחת למערכת - עד כה קיימת רק בתוך ThemeConfig של
 * אפליקציית ההגדרות (baseFontSize/titleFontSize/summaryFontSize/
 * headerFontSize, כולם תלויים ב-fontSizeMultiplier של המשתמש). כתוצאה מכך
 * מחוון גודל הגופן שבהגדרות היה משפיע רק על מסך ההגדרות עצמו ולא על שאר
 * המערכת. FutureType חושף את אותה סקאלה כפונקציה של multiplier כדי שכל
 * אפליקציה תוכל להשתמש בה ולקרוא את המכפיל בפועל מ-SystemUiSettingsClient
 * (ראו Settings/app/.../theme/SystemUiSettingsClient.kt).
 */
data class FutureType(val fontSizeMultiplier: Float = 1.0f) {
    val baseFontSize: TextUnit = (16 * fontSizeMultiplier).sp
    val titleFontSize: TextUnit = (17 * fontSizeMultiplier).sp
    val summaryFontSize: TextUnit = (13 * fontSizeMultiplier).sp
    val headerFontSize: TextUnit = (34 * fontSizeMultiplier).sp
    val screenTitleFontSize: TextUnit = (20 * fontSizeMultiplier).sp
}
