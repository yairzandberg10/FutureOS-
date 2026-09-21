package com.future.sharednav.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.future.sharednav.focus.bringIntoViewOnFocus
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureAccent
import com.future.sharednav.theme.focusFillChipColor
import com.future.sharednav.theme.idleChipColor
import com.future.sharednav.theme.idleFieldColor
import com.future.sharednav.theme.onReadableAccentColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.switchTrackOffColor
import com.future.sharednav.theme.textAlpha
import com.future.sharednav.theme.rememberFutureType

/**
 * מתג. מסילה 52×32dp וכפתור 24dp דלוק / 16dp כבוי - הגיאומטריה של
 * Material 3, שהיא מה שהמערכת השתמשה בו מלכתחילה.
 *
 * הכפתור לבן תמיד. בקוד המקור זה יצר מתג דלוק שנקרא כמסילה לבנה ריקה
 * כשההדגשה לבנה (ברירת המחדל), והעיצוב מסמן את זה כפגם. כאן המסילה
 * נצבעת בהדגשה **המתוקנת**, כך שבמצב בהיר היא נשארת מובחנת.
 */
@Composable
fun FutureSwitch(
    checked: Boolean,
    theme: FutureTheme,
    modifier: Modifier = Modifier,
) {
    val accent = LocalFutureAccent.current ?: theme.readableAccentColor
    val track by animateColorAsState(
        if (checked) accent else theme.switchTrackOffColor,
        FutureMotion.focusColorSpec,
        label = "switchTrack",
    )
    val thumbSize by animateDpAsState(
        if (checked) 24.dp else 16.dp,
        FutureMotion.fast(),
        label = "switchThumbSize",
    )
    val thumbOffset by animateDpAsState(
        if (checked) 12.dp else 4.dp,
        FutureMotion.fast(),
        label = "switchThumbOffset",
    )
    Box(
        modifier = modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(FutureShapes.pill)
            .background(track),
        // הממשק RTL, ולכן "דלוק" מזיז את הכפתור שמאלה.
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(
            modifier = Modifier
                .padding(end = thumbOffset)
                .size(thumbSize)
                .clip(FutureShapes.pill)
                .background(Color.White),
        )
    }
}

/**
 * צ'יפ - מסנן או לשונית. רדיוס 14dp, 13sp בינוני.
 * נבחר = מילוי מלא בהדגשה; ממוקד = 18% מהטקסט; במנוחה = 6%.
 */
@Composable
fun FutureChip(
    text: String,
    theme: FutureTheme,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    focused: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val type = rememberFutureType()
    val accent = LocalFutureAccent.current ?: theme.readableAccentColor
    val background by animateColorAsState(
        when {
            selected -> accent
            focused -> theme.focusFillChipColor
            else -> theme.idleChipColor
        },
        FutureMotion.focusColorSpec,
        label = "chipBg",
    )
    val interactionSource = remember { MutableInteractionSource() }
    Text(
        text,
        color = if (selected) theme.onReadableAccentColor else theme.textColor,
        fontSize = type.summary,
        fontWeight = FutureTypography.weightMedium,
        modifier = modifier
            .clip(FutureShapes.chip)
            .background(background)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else Modifier
            )
            .padding(horizontal = 18.dp, vertical = FutureDimens.spacingSm),
    )
}

/**
 * עיגול של יום בשבוע, בבורר השעה של המעורר. 36dp, 14sp מודגש.
 * נבחר = מילוי בהדגשה; ממוקד = 20% מההדגשה עם טבעת; במנוחה = 8% מהטקסט.
 *
 * מקבל פוקוס בעצמו - במכשיר בלי מגע זו הדרך היחידה להגיע אליו.
 */
@Composable
fun FutureDayChip(
    text: String,
    theme: FutureTheme,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val type = rememberFutureType()
    val accent = LocalFutureAccent.current ?: theme.readableAccentColor
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val background by animateColorAsState(
        when {
            selected -> accent
            focused -> accent.copy(alpha = 0.20f)
            else -> theme.idleFieldColor
        },
        FutureMotion.focusColorSpec,
        label = "dayChipBg",
    )
    val ring by animateColorAsState(
        if (focused && !selected) accent else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "dayChipRing",
    )
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(FutureShapes.pill)
            .background(background)
            .border(FutureDimens.focusBorderControl, ring, FutureShapes.pill)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else Modifier
            )
            .focusable(interactionSource = interactionSource)
            .bringIntoViewOnFocus(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (selected) theme.onReadableAccentColor else theme.textColor,
            fontSize = type.body,
            fontWeight = FutureTypography.weightBold,
        )
    }
}

/**
 * פס התקדמות. 4dp (2dp בנגן), מסילה ב-10% מצבע הטקסט, והמילוי גדל
 * **מימין** - הממשק RTL, ולכן התקדמות מתחילה בצד ימין.
 */
@Composable
fun FutureProgressBar(
    progress: Float,
    theme: FutureTheme,
    modifier: Modifier = Modifier,
    mini: Boolean = false,
) {
    val accent = LocalFutureAccent.current ?: theme.readableAccentColor
    val height = if (mini) 2.dp else 4.dp
    val shape = RoundedCornerShape(height / 2)
    val fraction by animateFloatAsState(
        progress.coerceIn(0f, 1f),
        FutureMotion.standard(),
        label = "progress",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(theme.textAlpha(10)),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .clip(shape)
                .background(accent),
        )
    }
}
