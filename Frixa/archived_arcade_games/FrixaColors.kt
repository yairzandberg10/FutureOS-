package com.future.frixa.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * זהות עיצובית קבועה של פריקסה - "ארקייד רטרו": השראה ממכונות ארקייד ישנות
 * ומשחקי טלפוני-פיצ'ר קלאסיים (נחש/2048), עם סגול-מגנטה כהה ברקע וניאון
 * ציאן/צהוב/ורוד בולט. בכוונה לא תלוי ב-ThemeClient/FutureTheme המשותף כמו
 * שאר האפליקציות - זו זהות משחקית עצמאית, קבועה, לא כהה/בהיר לפי המערכת.
 */
object FrixaColors {
    val background = Color(0xFF120A24)
    val surface = Color(0xFF1C1038)
    val surfaceContainer = Color(0xFF241347)
    val surfaceContainerHigh = Color(0xFF2E1957)
    val surfaceContainerHighest = Color(0xFF3A2069)

    val onSurface = Color(0xFFF3EFFF)
    val onSurfaceVariant = Color(0xFFC3B6E8)
    val outline = Color(0xFF4A2E8A)

    // ציאן ניאון - פעולה ראשית, ניקוד, "משחק חדש"
    val primary = Color(0xFF00E5FF)
    val onPrimary = Color(0xFF00343D)

    // ורוד-מגנטה ניאון - אזהרות/game over, הדגשות משניות
    val secondary = Color(0xFFFF2E92)
    val onSecondary = Color(0xFF3D0021)

    // צהוב ניאון - שיאים/הישגים
    val tertiary = Color(0xFFFFD60A)
    val onTertiary = Color(0xFF3D2E00)
}
