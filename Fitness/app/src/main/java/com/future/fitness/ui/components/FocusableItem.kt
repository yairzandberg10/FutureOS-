package com.future.fitness.ui.components
import com.future.sharednav.theme.idleChipColor

import com.future.sharednav.theme.FutureShapes
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.focus.FocusableItem as SharedFocusableItem

/**
 * עטיפה דקה סביב שורת הרשימה המשותפת. אריח במנוחה (6% מהטקסט), ובפוקוס
 * בדיוק כמו כל שורת רשימה במערכת: 14% הדגשה, מסגרת 1.5dp והגדלה 1.02
 * (guidelines/focus-spec.html). קודם הפוקוס כאן היה מסגרת 2dp בלבד בלי
 * מילוי ובלי הגדלה, ברדיוס 16dp - כלומר שורה ממוקדת בכושר נראתה אחרת
 * משורה ממוקדת בכל אפליקציה אחרת.
 */
@Composable
fun FocusableItem(
    onClick: () -> Unit,
    theme: FutureTheme,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    content: @Composable BoxScope.(isFocused: Boolean) -> Unit,
) {
    SharedFocusableItem(
        onClick = onClick,
        accentColor = theme.accentColor,
        modifier = modifier,
        idleBackgroundColor = theme.idleChipColor,
        contentPadding = 0.dp,
        focusRequester = focusRequester,
        content = content,
    )
}
