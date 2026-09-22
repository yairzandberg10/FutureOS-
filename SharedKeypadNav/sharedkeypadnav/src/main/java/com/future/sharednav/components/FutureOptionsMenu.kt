package com.future.sharednav.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.future.sharednav.focus.bringIntoViewOnFocus
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.focusFillMenuColor
import com.future.sharednav.theme.rememberFutureType
import com.future.sharednav.theme.textAlpha

/**
 * תפריט האפשרויות - נפתח במקש התפריט, ומחליף בבת אחת את מגירת הניווט,
 * התפריט הנפתח ותפריט ההקשר (components/navigation/OptionsMenu.jsx).
 *
 * 85% מרוחב המסך, רדיוס 20dp, תלוי 40dp מראש המסך. כותרת אופציונלית
 * (שם הפריט שעליו התפריט נפתח) ב-12sp ו-50%. השורה הראשונה מקבלת פוקוס
 * עם הפתיחה.
 *
 * לפני הרכיב הזה היו שבעה תפריטים נפרדים באפליקציות, כל אחד עם רדיוס של
 * כרטיס (22dp) במקום של דיאלוג, תווית 16sp במקום 15sp, ומילוי פוקוס
 * שנע בין 12% מהטקסט ל-25% מההדגשה.
 */
@Composable
fun FutureOptionsMenu(
    theme: FutureTheme,
    onDismissRequest: () -> Unit,
    header: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val type = rememberFutureType()
    val firstRow = remember { FirstRowClaim() }
    AppDialog(onDismissRequest = onDismissRequest, anchorTop = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(FutureShapes.dialog)
                .background(theme.surfaceColor)
                .padding(vertical = FutureDimens.spacingSm)
                .verticalScroll(rememberScrollState()),
        ) {
            if (header != null) {
                Text(
                    header,
                    color = theme.textAlpha(50),
                    fontSize = type.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(
                        start = MenuRowHorizontalPadding,
                        end = MenuRowHorizontalPadding,
                        top = MenuHeaderTopPadding,
                        bottom = FutureDimens.spacingSm,
                    ),
                )
            }
            CompositionLocalProvider(LocalFirstRowClaim provides firstRow) {
                content()
            }
        }
    }
}

/**
 * שורה בתפריט האפשרויות: אייקון 20dp, 14dp רווח, תווית 15sp, גובה 50dp.
 * הפוקוס הוא רקע של 12% מהטקסט **בלבד** - בלי מסגרת ובלי הגדלה; זה הרכיב
 * היחיד שחורג מהכלל של מסגרת-בפוקוס. שורה הרסנית (אחרונה, באדום) עדיין
 * עוברת דרך [ConfirmDialog] לפני שמשהו קורה.
 *
 * האייקון בצבע הטקסט ולא בצבע ההדגשה: "An icon never carries its own brand
 * color" (README של הדיזיין סיסטם, Iconography).
 */
@Composable
fun FutureMenuRow(
    label: String,
    icon: ImageVector?,
    theme: FutureTheme,
    onClick: () -> Unit,
    destructive: Boolean = false,
    focusRequester: FocusRequester? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val type = rememberFutureType()
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val background by animateColorAsState(
        if (isFocused) theme.focusFillMenuColor else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "menuRowBg",
    )
    val contentColor = if (destructive) theme.dangerColor else theme.textColor

    // השורה הראשונה בתפריט תופסת את הפוקוס ההתחלתי. הבקשה נעשית רק אחרי
    // שהשורה נמדדה: Dialog רץ בחלון נפרד, ובקשה מוקדמת יותר נבלעת בשקט.
    val claim = LocalFirstRowClaim.current
    val isFirst = remember { claim?.claim() ?: false }
    val ownRequester = remember { FocusRequester() }
    val requester = focusRequester ?: if (isFirst) ownRequester else null
    var hasAutoFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(FutureDimens.rowHeightMenu)
            .background(background)
            .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
            .then(
                if (isFirst && requester != null) Modifier.onGloballyPositioned {
                    if (!hasAutoFocused) {
                        hasAutoFocused = true
                        runCatching { requester.requestFocus() }
                    }
                } else Modifier
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .bringIntoViewOnFocus()
            .padding(horizontal = MenuRowHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MenuRowGap),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(FutureDimens.iconMenuRow))
        }
        Text(
            label,
            color = contentColor,
            fontSize = type.dialog,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke(this)
    }
}

/** 20dp - הריפוד האופקי של שורת תפריט (--fos-space-8). */
private val MenuRowHorizontalPadding = 20.dp

/** 14dp - בין האייקון לתווית (--fos-space-6). */
private val MenuRowGap = 14.dp

/** 6dp - מעל כותרת התפריט (--fos-space-2). */
private val MenuHeaderTopPadding = 6.dp

private class FirstRowClaim {
    private var claimed = false
    fun claim(): Boolean {
        if (claimed) return false
        claimed = true
        return true
    }
}

private val LocalFirstRowClaim = compositionLocalOf<FirstRowClaim?> { null }
