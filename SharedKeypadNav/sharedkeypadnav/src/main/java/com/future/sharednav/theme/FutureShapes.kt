package com.future.sharednav.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * סקאלת הפינות של המערכת. בסריקה נמצאו 18 רדיוסים שונים בשימוש
 * (2,3,4,5,6,8,9,10,12,14,16,17,18,20,24,28,30,35), כלומר בפועל כל מסך
 * בחר לעצמו - ושני רכיבים באותו תפקיד (שורת רשימה, כרטיס, דיאלוג) יצאו
 * עם פינות שונות בשתי אפליקציות שונות.
 *
 * שש דרגות מכסות את כל השימושים האמיתיים. הערכים לא הומצאו: [lg] ו-[sm]
 * הם הטוקנים שכבר היו ב-FutureDimens (cardCornerRadius/itemCornerRadius),
 * ו-[xl] הוא borderRadius=22 שמופיע בטבלת המידות של העיצוב
 * (design/keyboard-panel/Tokens.dc.html, "תג השפה").
 */
object FutureShapes {

    /** פסי התקדמות, מדדים דקים, תגיות זעירות. */
    val radiusXs: Dp = 4.dp

    /** מקשים ופריטי רשת קטנים. */
    val radiusSm: Dp = 8.dp

    /** שדה קלט (BasicTextField). */
    val radiusTextField: Dp = 10.dp

    /** כפתורים, שורות רשימה, פריט בשורת לשוניות. */
    val radiusMd: Dp = 12.dp

    /** צ'יפ (GalleryTabChip). */
    val radiusChip: Dp = 14.dp

    /** כרטיסים ופאנלים. */
    val radiusLg: Dp = 16.dp

    /** דיאלוג ותפריט אפשרויות. */
    val radiusDialog: Dp = 20.dp

    /** משטחים מרכזיים - כרטיס ההגדרות, "זכוכית". */
    val radiusXl: Dp = 22.dp

    /** משטחי "זכוכית" של מעטפת המערכת (קונטרול סנטר, מרכז התראות, נעילה). */
    val radiusXxl: Dp = 28.dp

    // מוקלדים כ-RoundedCornerShape ולא כ-Shape כללי: קוד קיים שעבר מיגרציה
    // מ-RoundedCornerShape(16.dp) יכול להחזיק את הערך במשתנה מהסוג הזה או
    // לקרוא ל-copy() עליו, והחלפה לטוקן לא אמורה לשבור את זה.
    val xs: RoundedCornerShape = RoundedCornerShape(radiusXs)
    val sm: RoundedCornerShape = RoundedCornerShape(radiusSm)
    val textField: RoundedCornerShape = RoundedCornerShape(radiusTextField)
    val md: RoundedCornerShape = RoundedCornerShape(radiusMd)
    val chip: RoundedCornerShape = RoundedCornerShape(radiusChip)
    val lg: RoundedCornerShape = RoundedCornerShape(radiusLg)
    val dialog: RoundedCornerShape = RoundedCornerShape(radiusDialog)
    val xl: RoundedCornerShape = RoundedCornerShape(radiusXl)
    val xxl: RoundedCornerShape = RoundedCornerShape(radiusXxl)

    /** גלולה מלאה - נגזרת מהגובה בפועל, ולכן לא צריכה ערך ב-dp. */
    val pill: RoundedCornerShape = RoundedCornerShape(percent = 50)

    /**
     * מצמיד רדיוס חופשי לדרגה הקרובה בסקאלה. קיים בשביל מיגרציה של קוד
     * ישן ובשביל הבדיקה שמוודאת שכל ערך שהיה בקוד אכן ממופה לדרגה אחת -
     * קוד חדש אמור פשוט לבחור את הדרגה הנכונה לפי התפקיד.
     */
    fun snap(radius: Dp): Dp = when {
        radius <= 5.dp -> radiusXs
        radius <= 9.dp -> radiusSm
        radius <= 11.dp -> radiusTextField
        radius <= 13.dp -> radiusMd
        radius <= 15.dp -> radiusChip
        radius <= 18.dp -> radiusLg
        radius <= 21.dp -> radiusDialog
        radius <= 24.dp -> radiusXl
        else -> radiusXxl
    }
}
