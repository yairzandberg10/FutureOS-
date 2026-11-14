package com.future.sharednav.focus

import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

/**
 * רקע + מסגרת של פוקוס שהצבעים שלהם מונפשים - נקראים בשלב הציור בלבד.
 *
 * זה המקום הכי חם בכל הממשק: כל לחיצת חץ מזיזה את הפוקוס, ושני פריטים
 * (זה שמאבד וזה שמקבל) מנפישים רקע ומסגרת במשך 90ms. קודם הצבעים נקראו ב-
 * composition (`val bg by animateColorAsState(...)` ואז `.background(bg)`),
 * ולכן כל פריים של האנימציה הריץ recomposition מלא של הפריט - כולל
 * התוכן שלו, שורת טקסט, אייקון וכו'. כשמחזיקים חץ ברשימה, זה recomposition
 * של שני פריטים בכל פריים ברציפות. כאן הערך נקרא בתוך הציור, כך שאנימציה
 * רק מסמנת את השכבה לציור מחדש - בלי composition ובלי layout.
 *
 * המראה זהה ל-`Modifier.background(color, shape).border(width, color, shape)`:
 * הרקע ממלא את צורת הרכיב, התוכן מצויר מעליו, והמסגרת מעל התוכן - מרכז
 * הקו חצי עובי פנימה מהקצה והפינות מתכווצות באותו חצי, כמו ש-
 * BorderModifierNode של Compose מצייר מסגרת לצורה מעוגלת.
 */
internal fun Modifier.animatedFocusSurface(
    shape: Shape,
    borderWidth: Dp,
    fill: () -> Color,
    ring: () -> Color,
): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val strokePx = borderWidth.toPx()
    val ringPath = if (strokePx > 0f) insetRingPath(outline, strokePx / 2f) else null
    val stroke = Stroke(width = strokePx)
    onDrawWithContent {
        val fillColor = fill()
        if (fillColor.alpha > 0f) drawOutline(outline, fillColor)
        drawContent()
        val ringColor = ring()
        if (ringPath != null && ringColor.alpha > 0f) drawPath(ringPath, ringColor, style = stroke)
    }
}

private fun insetRingPath(outline: Outline, inset: Float): Path = Path().apply {
    when (outline) {
        is Outline.Rectangle -> addRect(outline.rect.deflate(inset))
        is Outline.Rounded -> {
            val r = outline.roundRect
            addRoundRect(
                RoundRect(
                    left = r.left + inset,
                    top = r.top + inset,
                    right = r.right - inset,
                    bottom = r.bottom - inset,
                    topLeftCornerRadius = r.topLeftCornerRadius.shrink(inset),
                    topRightCornerRadius = r.topRightCornerRadius.shrink(inset),
                    bottomRightCornerRadius = r.bottomRightCornerRadius.shrink(inset),
                    bottomLeftCornerRadius = r.bottomLeftCornerRadius.shrink(inset),
                ),
            )
        }
        // צורה חופשית (לא בשימוש כרגע באף פריט פוקוס) - הקו על קו המתאר עצמו.
        is Outline.Generic -> addPath(outline.path)
    }
}

private fun CornerRadius.shrink(value: Float): CornerRadius =
    CornerRadius((x - value).coerceAtLeast(0f), (y - value).coerceAtLeast(0f))

/**
 * רקע בצבע מונפש שנקרא בשלב הציור - תחליף ל-`.background(color)` כשהצבע
 * מגיע מ-animateColorAsState. שקול ל-background מלבני; רכיב מעוגל חותך
 * לפניו (clip), כמו קודם.
 */
internal fun Modifier.animatedFill(color: () -> Color): Modifier = drawBehind {
    val fillColor = color()
    if (fillColor.alpha > 0f) drawRect(fillColor)
}
