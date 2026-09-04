package com.future.sharednav

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * הרזולוציה הקבועה של כל מכשירי FutureOS (ר' CLAUDE.md). עד כה הערך הזה
 * לא היה קיים כקבוע יחיד באף מקום בקוד - הוא הופיע רק כטקסט חופשי בהערות
 * ב-4 קבצים שונים (CalculatorScreen.kt, LauncherDialogs.kt,
 * LockScreenScreen.kt) ובפרמטר בודד של @Preview אחד ב-ControlCenterScreen.
 * שימוש בקבוע הזה (למשל ב-@Preview(widthDp = FutureScreen.WIDTH_DP, ...))
 * מבטיח שכל תצוגה מקדימה ובדיקת פריסה תואמת בפועל את המכשיר היעד.
 */
object FutureScreen {
    const val WIDTH_DP: Int = 640
    const val HEIGHT_DP: Int = 960

    val width: Dp = WIDTH_DP.dp
    val height: Dp = HEIGHT_DP.dp
}
