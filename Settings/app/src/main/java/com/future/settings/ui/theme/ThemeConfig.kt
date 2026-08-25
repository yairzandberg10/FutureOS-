package com.future.settings.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ThemeConfig(
    val primaryColor: Color = Color.White,
    val isDarkMode: Boolean = true,
    val fontSizeMultiplier: Float = 1.0f,
    val itemSpacing: Dp = 12.dp,
    val borderRadius: Dp = 22.dp
) {
    // הרקע וה"כרטיסים" חייבים להיות שני גוונים שונים בבירור זה מזה (לא שני
    // גוונים כמעט-זהים על שקיפות) כדי שאפשר יהיה להבדיל בין פאנל לפאנל.
    val backgroundColor: Color = if (isDarkMode) Color.Black else Color(0xFFF2F2F7)
    val surfaceColor: Color = if (isDarkMode) Color(0xFF1C1C1E) else Color.White
    val glassColor: Color = if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFEDEDF2)
    val textColor: Color = if (isDarkMode) Color.White else Color.Black
    val dividerColor: Color = if (isDarkMode) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.1f)

    val baseFontSize: TextUnit = (16 * fontSizeMultiplier).sp
    val titleFontSize: TextUnit = (17 * fontSizeMultiplier).sp
    val summaryFontSize: TextUnit = (13 * fontSizeMultiplier).sp
    val headerFontSize: TextUnit = (34 * fontSizeMultiplier).sp
}
