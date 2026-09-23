package com.future.sharednav.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * סקאלת הפינות של המערכת (tokens/shape.css). בסריקה נמצאו 18 רדיוסים
 * שונים בשימוש (2,3,4,5,6,8,9,10,12,14,16,17,18,20,24,28,30,35), כלומר
 * בפועל כל מסך בחר לעצמו - ושני רכיבים באותו תפקיד (שורת רשימה, כרטיס,
 * דיאלוג) יצאו עם פינות שונות בשתי אפליקציות שונות.
 *
 * כלל הבית של הדיזיין סיסטם: "no tight corners anywhere" - 12dp (24px) הוא
 * הרדיוס הקטן ביותר במערכת, ומשטחי טקסט ולשוניות יושבים על 16dp. לכן
 * [radiusXs] ו-[radiusSm] שווים: שניהם הרצפה. פס דק (4dp) שמקבל 12dp לא
 * "נשבר" - Compose חותך כל פינה לחצי מהצלע הקצרה, והפס יוצא גלולה, בדיוק
 * כמו ProgressBar.jsx (borderRadius: h / 2).
 */
object FutureShapes {

    /**
     * פסי התקדמות, מדדים דקים, תגיות זעירות. שווה ל-[radiusSm] - אין
     * במערכת פינה הדוקה יותר - ועל פס דק הוא נחתך לגלולה.
     */
    val radiusXs: Dp = 12.dp

    /** פקדים קטנים - תיבת סימון, מקש, אריח פעולה (--fos-radius-item). */
    val radiusSm: Dp = 12.dp

    /** שורת רשימה - FocusableItem, ListItem.jsx (--fos-radius-row). */
    val radiusRow: Dp = 20.dp

    /** שדה קלט ו-TextArea (--fos-radius-textfield). */
    val radiusTextField: Dp = 16.dp

    /** פריט בשורת לשוניות (--fos-radius-tab). */
    val radiusMd: Dp = 16.dp

    /** צ'יפ (--fos-radius-chip). */
    val radiusChip: Dp = 16.dp

    /** כרטיסים, פאנלים, שורת מחוון, תא ב-ActionGrid (--fos-radius-card). */
    val radiusLg: Dp = 16.dp

    /** דיאלוג ותפריט אפשרויות (--fos-radius-dialog). */
    val radiusDialog: Dp = 20.dp

    /** משטחים מרכזיים - כרטיס ההגדרות, "זכוכית" (--fos-radius-main). */
    val radiusXl: Dp = 22.dp

    /** התראה צפה ומשטחי "זכוכית" של מעטפת המערכת (--fos-radius-headsup). */
    val radiusXxl: Dp = 28.dp

    // מוקלדים כ-RoundedCornerShape ולא כ-Shape כללי: קוד קיים שעבר מיגרציה
    // מ-RoundedCornerShape(16.dp) יכול להחזיק את הערך במשתנה מהסוג הזה או
    // לקרוא ל-copy() עליו, והחלפה לטוקן לא אמורה לשבור את זה.
    val xs: RoundedCornerShape = RoundedCornerShape(radiusXs)
    val sm: RoundedCornerShape = RoundedCornerShape(radiusSm)
    val row: RoundedCornerShape = RoundedCornerShape(radiusRow)
    val textField: RoundedCornerShape = RoundedCornerShape(radiusTextField)
    val md: RoundedCornerShape = RoundedCornerShape(radiusMd)
    val chip: RoundedCornerShape = RoundedCornerShape(radiusChip)
    val lg: RoundedCornerShape = RoundedCornerShape(radiusLg)
    val dialog: RoundedCornerShape = RoundedCornerShape(radiusDialog)
    val xl: RoundedCornerShape = RoundedCornerShape(radiusXl)
    val xxl: RoundedCornerShape = RoundedCornerShape(radiusXxl)

    /** גלולה מלאה - כפתורים, מסילת מתג, צ'יפ יום (--fos-radius-full). */
    val pill: RoundedCornerShape = RoundedCornerShape(percent = 50)

    /**
     * מצמיד רדיוס חופשי לדרגה הקרובה בסקאלה. קיים בשביל מיגרציה של קוד
     * ישן - קוד חדש אמור פשוט לבחור את הדרגה הנכונה לפי התפקיד. כל מה
     * שקטן מ-12dp עולה לרצפה.
     */
    fun snap(radius: Dp): Dp = when {
        radius <= 13.dp -> radiusSm
        radius <= 18.dp -> radiusLg
        radius <= 21.dp -> radiusDialog
        radius <= 24.dp -> radiusXl
        else -> radiusXxl
    }
}
