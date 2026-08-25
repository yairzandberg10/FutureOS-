package com.future.sharednav.focus

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * שורת רשימה עם פוקוס D-pad אמיתי - הגרסה המשותפת של רכיב שהיה קיים
 * בנפרד (ושונה מעט) בשלוש אפליקציות (dialer, Music, Sfarim). כל הקלט
 * הצבעוני/צורני הוא פרמטרים פרימיטיביים (Color/Dp/Boolean) ולא תלוי בשום
 * FutureTheme ספציפי-לאפליקציה, כדי שהמודול הזה יישאר ללא תלות בחבילת
 * theme של אף צרכן. כל אפליקציה מעבירה את צבעי ה-theme שלה עצמה.
 *
 * ברירות המחדל משמרות בדיוק את ההתנהגות הקודמת של dialer/Sfarim (בורדר
 * 1.5dp, פינות 8dp, אנימציית scale ל-1.02, ריפוד 4dp). אפליקציות עם
 * מראה שונה (למשל Music: בלי scale, פינות 16dp, בורדר 2dp, בלי ריפוד)
 * דורסות את הפרמטרים המתאימים בקריאה שלהן - ראו קריאות ה-call site.
 */
@Composable
fun FocusableItem(
    onClick: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
    idleBackgroundColor: Color = Color.Transparent,
    focusedBackgroundColor: Color = accentColor.copy(alpha = 0.14f),
    borderColor: Color = accentColor,
    borderWidth: Dp = 1.5.dp,
    cornerRadius: Dp = 8.dp,
    scaleOnFocus: Boolean = true,
    focusedScale: Float = 1.02f,
    showBorderOnFocus: Boolean = true,
    contentPadding: Dp = 4.dp,
    focusRequester: FocusRequester? = null,
    content: @Composable BoxScope.(isFocused: Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(cornerRadius)

    val scale by animateFloatAsState(
        if (scaleOnFocus && isFocused) focusedScale else 1.0f,
        label = "focusableItemScale",
    )
    val backgroundColor by animateColorAsState(
        if (isFocused) focusedBackgroundColor else idleBackgroundColor,
        label = "focusableItemBg",
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(backgroundColor)
            .then(
                if (showBorderOnFocus && isFocused) {
                    Modifier.border(width = borderWidth, color = borderColor, shape = shape)
                } else Modifier
            )
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(contentPadding),
        content = { content(isFocused) },
    )
}
