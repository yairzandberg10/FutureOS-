package com.future.sharednav.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import com.future.sharednav.focus.bringIntoViewOnFocus
import com.future.sharednav.theme.FutureContrast
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureAccent
import com.future.sharednav.theme.onStatusColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.textAlpha
import com.future.sharednav.theme.rememberFutureType

/**
 * ארבעת סוגי הכפתור של המערכת. לא היה כאן רכיב כפתור משותף - כל מסך
 * בנה אחד בעצמו, ולכן אותו תפקיד יצא בארבעה עיצובים שונים. הגיאומטריה
 * היא של components/core/Button.jsx: גלולה מלאה בגובה קבוע של 44dp,
 * ריפוד צד 24dp, 16sp, ומסגרת פוקוס 2dp בצבע הטקסט שמצוירת בתוך הכפתור -
 * כך שפוקוס אף פעם לא משנה את הגובה. כל ארבעת הסוגים באותה צורה ובאותו
 * גובה; השקט שונה רק במשקל (בינוני) ובהיעדר מסגרת.
 *
 * במנוחה הכפתור ב-70% אטימות ובפוקוס הוא מלא - כלומר הפוקוס נמסר גם
 * בעוצמת הצבע וגם במסגרת, ולא במסגרת בלבד.
 */
enum class FutureButtonVariant { Primary, Destructive, Secondary, Quiet }

@Composable
fun FutureButton(
    text: String,
    theme: FutureTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: FutureButtonVariant = FutureButtonVariant.Primary,
    fillMaxWidth: Boolean = false,
    focusRequester: FocusRequester? = null,
    enabled: Boolean = true,
) {
    val accent = LocalFutureAccent.current ?: theme.readableAccentColor
    val fill = when (variant) {
        FutureButtonVariant.Primary -> accent
        FutureButtonVariant.Destructive -> theme.dangerColor
        FutureButtonVariant.Secondary -> theme.textColor
        FutureButtonVariant.Quiet -> theme.textAlpha(10)
    }
    val content = when (variant) {
        // נגזר מהמילוי שבאמת מצויר - ההדגשה של המסך אם יש כזו - ולא מההדגשה
        // של הערכה, כדי שהטקסט יתאים לרקע שמתחתיו גם כששתיהן שונות.
        FutureButtonVariant.Primary -> FutureContrast.onColor(accent)
        FutureButtonVariant.Destructive -> theme.onStatusColor(theme.dangerColor)
        FutureButtonVariant.Secondary -> theme.onStatusColor(theme.textColor)
        FutureButtonVariant.Quiet -> theme.textColor
    }
    FutureButtonCore(
        text = text,
        fill = fill,
        contentColor = content,
        ringColor = theme.textColor,
        onClick = onClick,
        modifier = modifier,
        baseAlpha = if (variant == FutureButtonVariant.Secondary) 0.7f else 1f,
        quiet = variant == FutureButtonVariant.Quiet,
        fillMaxWidth = fillMaxWidth,
        focusRequester = focusRequester,
        enabled = enabled,
    )
}

/**
 * הגוף של [FutureButton], בצבעים גולמיים - בשביל רכיבים משותפים שמקבלים
 * צבעים בודדים ולא [FutureTheme] שלם (למשל [ConfirmDialog]). כך יש בדיוק
 * כפתור אחד במערכת, גם כשהקורא לא מחזיק ערכה.
 */
@Composable
internal fun FutureButtonCore(
    text: String,
    fill: Color,
    contentColor: Color,
    ringColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    baseAlpha: Float = 1f,
    quiet: Boolean = false,
    fillMaxWidth: Boolean = false,
    focusRequester: FocusRequester? = null,
    enabled: Boolean = true,
) {
    val type = rememberFutureType()
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val opacity by animateFloatAsState(
        if (quiet) 1f else if (isFocused) baseAlpha else baseAlpha * 0.7f,
        FutureMotion.fast(),
        label = "buttonOpacity",
    )
    val ring by animateColorAsState(
        if (isFocused && !quiet) ringColor else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "buttonRing",
    )
    val shape = FutureShapes.pill

    Box(
        modifier = modifier
            .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
            .height(FutureDimens.rowHeightDialogButton)
            .clip(shape)
            .alpha(opacity)
            .background(fill)
            .border(FutureDimens.focusBorderControl, ring, shape)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            // כפתור שאי אפשר להפעיל גם לא מקבל פוקוס - "Anything that cannot
            // receive focus cannot be activated at all" (README של הדיזיין סיסטם).
            // הוא נשאר באטימות המנוחה (70%), ולא מקבל עיצוב "מושבת" נפרד.
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .bringIntoViewOnFocus()
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = FutureDimens.spacingXl),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = contentColor,
            fontSize = type.bodyLarge,
            maxLines = 1,
            fontWeight = if (quiet) FutureTypography.weightMedium else FutureTypography.weightBold,
        )
    }
}
