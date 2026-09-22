package com.future.sharednav.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.future.sharednav.theme.FutureContrast
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.subtleTextColor
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
 * מתג (components/forms/Switch.jsx). מסילה 52×30dp עם מסגרת 2dp.
 * כבוי = מסילה ריקה, מסגרת ב-30% מהטקסט וכפתור 14dp ב-40%.
 * דלוק = מסילה מלאה בהדגשה, וכפתור 18dp בצבע הרקע של המסך.
 *
 * הכפתור *אינו* לבן קבוע: עם ההדגשה הלבנה של ברירת המחדל, כפתור לבן על
 * מסילה לבנה הוא בדיוק הפגם שהדיזיין סיסטם מסמן ("a white-on-white switch
 * thumb is a real defect"). כאן שום דבר לא תלוי בכך שההדגשה צבעונית.
 * המיקום לוגי - ב-RTL הכפתור נח מימין ונוסע שמאלה.
 */
@Composable
fun FutureSwitch(
    checked: Boolean,
    theme: FutureTheme,
    modifier: Modifier = Modifier,
) {
    val accent = LocalFutureAccent.current ?: theme.readableAccentColor
    val track by animateColorAsState(
        if (checked) accent else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "switchTrack",
    )
    val border by animateColorAsState(
        if (checked) accent else theme.textAlpha(30),
        FutureMotion.focusColorSpec,
        label = "switchBorder",
    )
    val thumb by animateColorAsState(
        if (checked) theme.backgroundColor else theme.textAlpha(40),
        FutureMotion.focusColorSpec,
        label = "switchThumb",
    )
    val thumbSize by animateDpAsState(
        if (checked) SwitchThumbOn else SwitchThumbOff,
        FutureMotion.fast(),
        label = "switchThumbSize",
    )
    val thumbStart by animateDpAsState(
        if (checked) SwitchTrackInner - SwitchThumbOn - SwitchThumbInset else SwitchThumbInset,
        FutureMotion.fast(),
        label = "switchThumbStart",
    )
    Box(
        modifier = modifier
            .size(width = SwitchTrackWidth, height = SwitchTrackHeight)
            .clip(FutureShapes.pill)
            .background(track)
            .border(FutureDimens.focusBorderControl, border, FutureShapes.pill)
            .padding(FutureDimens.focusBorderControl),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbStart)
                .size(thumbSize)
                .clip(FutureShapes.pill)
                .background(thumb),
        )
    }
}

// הגיאומטריה של Switch.jsx, בחצי מהפיקסלים: מסילה 104×60px, כפתור 28/36px,
// מרווח 8px מהשפה הפנימית.
private val SwitchTrackWidth = 52.dp
private val SwitchTrackHeight = 30.dp
private val SwitchTrackInner = SwitchTrackWidth - 4.dp
private val SwitchThumbOff = 14.dp
private val SwitchThumbOn = 18.dp
private val SwitchThumbInset = 4.dp

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
    // צ'יפ לחיץ מקבל פוקוס בעצמו ויודע מתי הוא ממוקד; [focused] נשאר בשביל
    // צ'יפ לא-לחיץ שהפוקוס שלו מנוהל מבחוץ (רשימה עם keypadListNav).
    val interactionSource = remember { MutableInteractionSource() }
    val ownFocus by interactionSource.collectIsFocusedAsState()
    val background by animateColorAsState(
        when {
            selected -> accent
            focused || ownFocus -> theme.focusFillChipColor
            else -> theme.idleChipColor
        },
        FutureMotion.focusColorSpec,
        label = "chipBg",
    )
    Text(
        text,
        color = if (selected) FutureContrast.onColor(accent) else theme.textColor,
        fontSize = type.summary,
        fontWeight = FutureTypography.weightMedium,
        modifier = modifier
            .clip(FutureShapes.chip)
            .background(background)
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick,
                        )
                        .focusable(interactionSource = interactionSource)
                        .bringIntoViewOnFocus()
                } else Modifier
            )
            .padding(horizontal = ChipHorizontalPadding, vertical = FutureDimens.spacingSm),
    )
}

/** 18dp - הריפוד האופקי של צ'יפ (GalleryTabChip). */
private val ChipHorizontalPadding = 18.dp

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
 * ספינר - ההמתנה בלי אורך ידוע (סריקה, התחברות, שליחה). המקבילה העגולה של
 * [FutureProgressBar]: מסילה ב-10% מהטקסט, קשת של רבע עיגול בהדגשה, סיבוב
 * אחד ב-900ms (components/feedback/Spinner.jsx). מחליף את
 * CircularProgressIndicator של Material, שקשת שלו מתארכת ומתקצרת ואין לו
 * מסילה.
 */
@Composable
fun FutureSpinner(
    theme: FutureTheme,
    modifier: Modifier = Modifier,
    size: Dp = SpinnerSize,
    label: String? = null,
) {
    val type = rememberFutureType()
    val accent = LocalFutureAccent.current ?: theme.readableAccentColor
    val track = theme.textAlpha(10)
    val rotation by rememberInfiniteTransition(label = "spinner").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(FutureMotion.SpinnerRotationMillis, easing = LinearEasing)),
        label = "spinnerRotation",
    )
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(FutureDimens.spacingMd),
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = SpinnerThickness.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            drawArc(accent, rotation - 135f, 90f, false, topLeft, arcSize, style = Stroke(stroke))
        }
        if (label != null) {
            Text(label, color = theme.mutedTextColor, fontSize = type.body)
        }
    }
}

/** 36dp / 4dp - הגודל והעובי של הספינר (72px / 8px ב-Spinner.jsx). */
private val SpinnerSize = 36.dp
private val SpinnerThickness = 4.dp

/**
 * תיבת סימון לבחירה מרובה, לחלק האחרון של שורת הגדרה או שורת רשימה
 * (components/forms/Checkbox.jsx). מסומנת = מילוי מלא בהדגשה עם סימן
 * בצבע הדיו שעליה - אותו זוג של הצ'יפ הנבחר. לא מסומנת = ריבוע ריק עם
 * מסגרת של 40% מהטקסט, שנשאר גלוי גם כשההדגשה לבנה.
 */
@Composable
fun FutureCheckbox(
    checked: Boolean,
    theme: FutureTheme,
    modifier: Modifier = Modifier,
) {
    val accent = LocalFutureAccent.current ?: theme.readableAccentColor
    val fill by animateColorAsState(
        if (checked) accent else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "checkboxFill",
    )
    val border by animateColorAsState(
        if (checked) accent else theme.subtleTextColor,
        FutureMotion.focusColorSpec,
        label = "checkboxBorder",
    )
    Box(
        modifier = modifier
            .size(CheckboxSize)
            .clip(FutureShapes.sm)
            .background(fill)
            .border(FutureDimens.focusBorderControl, border, FutureShapes.sm),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = FutureContrast.onColor(accent),
                modifier = Modifier.size(CheckboxSize * 0.72f),
            )
        }
    }
}

/** 24dp - הצלע של תיבת הסימון (48px ב-Checkbox.jsx). */
private val CheckboxSize = 24.dp

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
