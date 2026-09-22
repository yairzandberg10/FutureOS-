package com.future.sharednav.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.future.sharednav.focus.bringIntoViewOnFocus
import com.future.sharednav.theme.FutureContrast
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureAccent
import com.future.sharednav.theme.focusFillChipColor
import com.future.sharednav.theme.idleFieldColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.rememberFutureType

/**
 * שורת לשוניות / בורר מצב (components/navigation/TabRow.jsx): פריטים
 * ברוחב שווה, רדיוס 12dp, תווית 13sp בינונית. נבחר = מילוי מלא בהדגשה;
 * ממוקד = 18% מהטקסט; במנוחה = 8% מהטקסט. בלי מסגרת - כמו צ'יפ.
 *
 * כל פריט מקבל פוקוס בעצמו, כך שהמעבר בין הלשוניות הוא חץ ימינה/שמאלה
 * והבחירה היא OK - בדיוק כמו כל פקד אחר במסך.
 *
 * [padded] = false משמיט את הריפוד החיצוני (16/6dp), למי שכבר ממקם את
 * השורה בתוך מכל עם ריפוד משלו.
 */
@Composable
fun FutureTabRow(
    items: List<String>,
    selectedIndex: Int,
    theme: FutureTheme,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    padded: Boolean = true,
    firstItemFocusRequester: FocusRequester? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (padded) Modifier.padding(horizontal = FutureDimens.spacingLg, vertical = TabRowVerticalPadding)
                else Modifier
            ),
        horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
    ) {
        items.forEachIndexed { index, label ->
            FutureTab(
                label = label,
                selected = index == selectedIndex,
                theme = theme,
                onClick = { onSelect(index) },
                focusRequester = if (index == 0) firstItemFocusRequester else null,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FutureTab(
    label: String,
    selected: Boolean,
    theme: FutureTheme,
    onClick: () -> Unit,
    focusRequester: FocusRequester?,
    modifier: Modifier = Modifier,
) {
    val type = rememberFutureType()
    val accent = LocalFutureAccent.current ?: theme.readableAccentColor
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val background by animateColorAsState(
        when {
            selected -> accent
            isFocused -> theme.focusFillChipColor
            else -> theme.idleFieldColor
        },
        FutureMotion.focusColorSpec,
        label = "tabBg",
    )
    Box(
        modifier = modifier
            .clip(FutureShapes.md)
            .background(background)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .bringIntoViewOnFocus()
            .padding(vertical = FutureDimens.spacingSm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) FutureContrast.onColor(accent) else theme.textColor,
            fontSize = type.summary,
            fontWeight = FutureTypography.weightMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 6dp - הריפוד האנכי של שורת הלשוניות (--fos-space-2). */
private val TabRowVerticalPadding = 6.dp
