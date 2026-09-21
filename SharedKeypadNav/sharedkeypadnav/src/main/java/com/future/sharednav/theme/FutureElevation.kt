package com.future.sharednav.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * עומק במערכת הזו נבנה מגוון, לא מצל. בכל הקוד קיימים בדיוק שני צללים
 * אמיתיים, שניהם על כרטיס ההגדרות - וזה לא מקרי אלא כלל
 * (guidelines/elevation.html): משטח "זכוכית" מוגבה מסומן בגוון בהיר יותר
 * ([elevatedSurfaceColor]) ובלי צל כלל, ודיאלוג מסומן בהכהיית הרקע
 * שמאחוריו ([scrimColor]) ולא בהגבהה.
 *
 * שש הדרגות: 0 רקע המסך · 1-2 משטח (כרטיס, עם הצל שלו) · 3 זכוכית
 * (גוון בלבד) · 4 דיאלוג (משטח + הכהיה) · 5 התראה צפה.
 */
object FutureElevation {

    /** רקע המסך, וכל מה שמצויר ישירות עליו. */
    val none: Dp = 0.dp

    /**
     * כרטיס. הצל היחיד במערכת, והוא חזק יותר במצב כהה מאשר בבהיר -
     * במצב בהיר הכרטיס הלבן כבר מובחן מהרקע האפור בלי עזרה.
     */
    fun card(isDarkMode: Boolean): Dp = if (isDarkMode) 4.dp else 1.dp

    /**
     * משטח "זכוכית". בלי צל בכוונה - ההגבהה נמסרת בגוון
     * ([elevatedSurfaceColor]) ולא בהצללה. אין טשטוש רקע במערכת הזו.
     */
    val glass: Dp = 0.dp

    /**
     * דיאלוג ותפריט. הצל לא נדרש כי [scrimColor] כבר מפריד אותם
     * מהמסך שמאחור.
     */
    val dialog: Dp = 0.dp

    /** התראה צפה - הדבר היחיד שמרחף מעל כל השאר. */
    val headsUp: Dp = 8.dp
}
