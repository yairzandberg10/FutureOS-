package com.future.fitness.ui.components

import com.future.sharednav.theme.FutureShapes
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.focus.FocusableItem as SharedFocusableItem

/** עטיפה דקה סביב הרכיב המשותף (מודול SharedKeypadNav) - הרקע נשאר קבוע בין
 * מצב רגיל לפוקוס (בלי אנימציית צבע), כדי שהפוקוס ייראה בדיוק כמו באפליקציית
 * ההגדרות הרגילה של המערכת: אותה כרטיסייה בדיוק, רק עם מסגרת מתווספת -
 * בלי שכבת צבע נוספת שמציצה מתחת לתוכן ויוצרת מראה "כפול/מוזר" (בעיקר בולט
 * בפלחים קטנים כמו SegmentedControl, ששם שכבת המילוי הישנה של הפוקוס יצרה
 * תיבה כפולה בתוך התיבה החיצונית של הפלחים). בלי אנימציית scale, פינות
 * 16dp, בורדר 2dp בפוקוס, בלי ריפוד תוכן. */
@Composable
fun FocusableItem(
    onClick: () -> Unit,
    theme: FutureTheme,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    content: @Composable BoxScope.(isFocused: Boolean) -> Unit,
) {
    val constantBackground = theme.textColor.copy(alpha = 0.05f)
    SharedFocusableItem(
        onClick = onClick,
        accentColor = theme.accentColor,
        modifier = modifier,
        idleBackgroundColor = constantBackground,
        focusedBackgroundColor = constantBackground,
        borderColor = theme.accentColor,
        borderWidth = 2.dp,
        cornerRadius = FutureShapes.radiusLg,
        scaleOnFocus = false,
        contentPadding = 0.dp,
        focusRequester = focusRequester,
        content = content,
    )
}
