package com.future.sharednav.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.future.sharednav.focus.focusMotion
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.focusFillChipColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.rememberFutureType
import com.future.sharednav.theme.textAlpha

/** פעולה אחת בסרגל הפעולות: אייקון מעל תווית. [destructive] = בצבע הסכנה. */
class FutureAction(
    val label: String,
    val icon: ImageVector,
    val destructive: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * סרגל פעולות תחתון - רכיב חדש שלא היה בדיזיין סיסטם. שורה של 2-5 פעולות
 * שוות רוחב (אייקון 22dp מעל תווית caption) על משטח בפינות "זכוכית" עליונות
 * (radiusXl), כמו סרגל הפעולות של צפייה בתמונה בטלפונים (שיתוף, עריכה,
 * פרטים, מחיקה).
 *
 * ניווט במקשים: הפוקוס נכנס לסרגל מבחוץ ([firstFocusRequester], בדרך כלל
 * בחץ למטה), ימינה/שמאלה בין הפעולות, OK מפעיל. הפוקוס הוא מילוי של שבב
 * (focusFillChipColor, 18%) ומסגרת בקרה 2dp בהדגשה, והפריט גדל מעט -
 * אותה שפה של פוקוס של כל שאר הרכיבים. [onFocusChanged] מדווח אם משהו
 * בסרגל ממוקד, כדי שהמסך ידע לא לפרש את החצים בעצמו.
 */
@Composable
fun FutureActionBar(
    theme: FutureTheme,
    actions: List<FutureAction>,
    modifier: Modifier = Modifier,
    firstFocusRequester: FocusRequester? = null,
    onFocusChanged: (Boolean) -> Unit = {},
    backgroundColor: Color = theme.surfaceColor,
) {
    val type = rememberFutureType()
    val shape = RoundedCornerShape(topStart = FutureShapes.radiusXl, topEnd = FutureShapes.radiusXl)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .background(backgroundColor)
                .onFocusChanged { onFocusChanged(it.hasFocus) },
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(theme.textAlpha(8)))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FutureDimens.spacingSm, vertical = FutureDimens.spacingSm),
                horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingXs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                actions.forEachIndexed { index, action ->
                    ActionBarItem(
                        action = action,
                        theme = theme,
                        labelSize = type.caption,
                        focusRequester = if (index == 0) firstFocusRequester else null,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionBarItem(
    action: FutureAction,
    theme: FutureTheme,
    labelSize: androidx.compose.ui.unit.TextUnit,
    focusRequester: FocusRequester?,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = FutureShapes.lg
    val fill by animateColorAsState(
        if (focused) theme.focusFillChipColor else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "actionBarFill",
    )
    val ring by animateColorAsState(
        if (focused) theme.readableAccentColor else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "actionBarRing",
    )
    val tint = when {
        !action.enabled -> theme.textAlpha(30)
        action.destructive -> theme.dangerColor
        else -> theme.textColor
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusMotion(interaction, focusedScale = 1.04f)
            .clip(shape)
            .background(fill)
            .border(FutureDimens.focusBorderControl, ring, shape)
            .clickable(interactionSource = interaction, indication = null, enabled = action.enabled, onClick = action.onClick)
            .padding(vertical = FutureDimens.spacingSm),
    ) {
        Icon(action.icon, contentDescription = null, tint = tint, modifier = Modifier.size(ActionIconSize))
        Text(
            action.label,
            color = if (focused && !action.destructive) theme.readableAccentColor else tint,
            fontSize = labelSize,
            fontWeight = if (focused) FutureTypography.weightBold else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = FutureDimens.spacingXs),
        )
    }
}

/** 22dp - אייקון פעולה בסרגל (בין iconMenuRow 20 ל-iconSettingRow 22). */
private val ActionIconSize = 22.dp
