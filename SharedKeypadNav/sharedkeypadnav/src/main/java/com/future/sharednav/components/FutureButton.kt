package com.future.sharednav.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
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
 * ארבעת סוגי הכפתור של המערכת, לפי components/core/Button.jsx: גלולה מלאה
 * בגובה קבוע של 44dp, ריפוד צד 24dp ורוחב מינימלי 88dp, 16sp.
 *
 * המילוי תמיד אטום, ואותו מילוי במנוחה ובפוקוס - בלי דהייה, כך שהתווית
 * נשארת בניגודיות מלאה. פוקוס = טבעת 2dp בצבע של הכפתור עצמו, מחוץ לגלולה
 * אחרי מרווח 2dp (כמו outline + outline-offset - לא משנה layout), ועוד
 * הגדלה ל-1.02. המשני (ביטול) הוא גוון 20% של צבע הטקסט עם תווית בצבע
 * הטקסט; השקט גוון 10% במשקל בינוני. אין מצב מושבת.
 *
 * [diameter] - אותו כפתור בדיוק כעיגול (גלולה שרוחבה שווה לגובהה), בשביל
 * כפתורי פעולה גדולים כמו התחל/עצור בשעון. המילוי, הטבעת וההגדלה זהים.
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
    diameter: Dp? = null,
) {
    val accent = LocalFutureAccent.current ?: theme.readableAccentColor
    val fill = when (variant) {
        FutureButtonVariant.Primary -> accent
        FutureButtonVariant.Destructive -> theme.dangerColor
        FutureButtonVariant.Secondary -> theme.textAlpha(20)
        FutureButtonVariant.Quiet -> theme.textAlpha(10)
    }
    val content = when (variant) {
        // נגזר מהמילוי שבאמת מצויר - ההדגשה של המסך אם יש כזו - ולא מההדגשה
        // של הערכה, כדי שהטקסט יתאים לרקע שמתחתיו גם כששתיהן שונות.
        FutureButtonVariant.Primary -> FutureContrast.onColor(accent)
        FutureButtonVariant.Destructive -> theme.onStatusColor(theme.dangerColor)
        FutureButtonVariant.Secondary, FutureButtonVariant.Quiet -> theme.textColor
    }
    FutureButtonCore(
        text = text,
        fill = fill,
        contentColor = content,
        onClick = onClick,
        modifier = modifier,
        quiet = variant == FutureButtonVariant.Quiet,
        fillMaxWidth = fillMaxWidth,
        focusRequester = focusRequester,
        enabled = enabled,
        diameter = diameter,
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    quiet: Boolean = false,
    fillMaxWidth: Boolean = false,
    focusRequester: FocusRequester? = null,
    enabled: Boolean = true,
    diameter: Dp? = null,
) {
    val type = rememberFutureType()
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // הטבעת בצבע הכפתור; רק השקיפות שלה וההגדלה מונפשות, ושתיהן נקראות
    // בשלב הציור בלבד - בלי recomposition בכל פריים.
    val ringAlpha = animateFloatAsState(if (isFocused) 1f else 0f, FutureMotion.fast(), label = "buttonRing")
    val scale = animateFloatAsState(
        if (isFocused) FutureDimens.focusScale else 1f,
        FutureMotion.focusScaleSpec,
        label = "buttonScale",
    )
    val shape = FutureShapes.pill

    Box(
        modifier = modifier
            .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
            .then(
                if (diameter != null) Modifier.size(diameter)
                else Modifier.height(FutureDimens.rowHeightDialogButton).widthIn(min = FutureDimens.rowHeightDialogButton * 2)
            )
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .drawWithCache {
                val outline = shape.createOutline(size, layoutDirection, this)
                val strokePx = FutureDimens.focusBorderControl.toPx()
                val ringPath = outsetPath(outline, FutureDimens.spacingXxs.toPx() + strokePx / 2f)
                val stroke = Stroke(width = strokePx)
                onDrawWithContent {
                    drawOutline(outline, fill)
                    drawContent()
                    val a = ringAlpha.value
                    if (a > 0f) drawPath(ringPath, fill.copy(alpha = fill.alpha * a), style = stroke)
                }
            }
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            // כפתור שאי אפשר להפעיל גם לא מקבל פוקוס - "Anything that cannot
            // receive focus cannot be activated at all" (README של הדיזיין סיסטם).
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .bringIntoViewOnFocus()
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = if (diameter != null) FutureDimens.spacingSm else FutureDimens.spacingXl),
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

/** קו המתאר של הכפתור מורחב החוצה ב-[outset] - המסלול של טבעת הפוקוס. */
private fun outsetPath(outline: Outline, outset: Float): Path = Path().apply {
    when (outline) {
        is Outline.Rectangle -> addRect(outline.rect.inflate(outset))
        is Outline.Rounded -> {
            val r = outline.roundRect
            fun CornerRadius.grow() = CornerRadius(x + outset, y + outset)
            addRoundRect(
                RoundRect(
                    left = r.left - outset,
                    top = r.top - outset,
                    right = r.right + outset,
                    bottom = r.bottom + outset,
                    topLeftCornerRadius = r.topLeftCornerRadius.grow(),
                    topRightCornerRadius = r.topRightCornerRadius.grow(),
                    bottomRightCornerRadius = r.bottomRightCornerRadius.grow(),
                    bottomLeftCornerRadius = r.bottomLeftCornerRadius.grow(),
                ),
            )
        }
        is Outline.Generic -> addPath(outline.path)
    }
}
