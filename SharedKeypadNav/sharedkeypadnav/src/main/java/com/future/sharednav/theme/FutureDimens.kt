package com.future.sharednav.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * סקאלת מרווחים/רדיוסים אחת למערכת - עד כה הטוקנים האלה היו קיימים רק
 * בתוך ThemeConfig של אפליקציית ההגדרות (itemSpacing/borderRadius), וכל
 * אפליקציה אחרת קבעה ריפוד/רדיוס ידנית וללא עקביות. הערכים כאן זהים
 * לערכי ברירת המחדל שהיו כבר ב-ThemeConfig, ולא ערכים חדשים.
 */
object FutureDimens {
    val itemSpacing: Dp = 12.dp
    val borderRadius: Dp = 22.dp

    // רדיוסים/ריפודים נוספים שחזרו כערכים חופשיים במסכים שונים (8dp/16dp
    // לפריטי רשימה, 16dp ריפוד מסך) - מאוחדים כאן כטוקנים בעלי שם.
    val itemCornerRadius: Dp = 8.dp
    val cardCornerRadius: Dp = 16.dp
    val screenPadding: Dp = 16.dp
    val glassColorRadius: Dp = 22.dp
}
