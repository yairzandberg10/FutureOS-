package com.future.sharednav.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.future.sharednav.focus.animatedFocusSurface
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureElevation
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.dividerColor
import com.future.sharednav.theme.sectionHeaderColor
import com.future.sharednav.theme.rememberFutureType

/**
 * כרטיס - משטח מלא ברדיוס 22dp, מוסט 16dp מדפנות המסך, שמחזיק שורות
 * *שקופות* המופרדות בקו שיער. אין לו מסגרת ואין לו פס צבעוני בצד.
 *
 * זה הצל היחיד במערכת (4dp כהה / 1dp בהיר). כל שאר ה"עומק" נבנה מגוון:
 * משטח מוגבה הוא טון בהיר יותר, ודיאלוג מופרד בהכהיית הרקע שמאחוריו.
 *
 * השורות שבתוכו לומדות איפה הן יושבות ([cardRowFocus]): הפוקוס של שורה
 * ממלא את כל רוחב הכרטיס, השורה הראשונה שומרת על הפינות העליונות שלו,
 * האחרונה על התחתונות, ושורה באמצע בלי פינות (Card.jsx + SettingItem.jsx).
 */
@Composable
fun FutureCard(
    theme: FutureTheme,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val card = remember { FutureCardGeometry() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FutureDimens.spacingLg, vertical = CardVerticalInset)
            .shadow(FutureElevation.card(theme.isDarkMode), FutureShapes.xl)
            .clip(FutureShapes.xl)
            .background(theme.surfaceColor)
            .onGloballyPositioned { card.onPlaced(it) }
            .padding(vertical = CardContentInset),
    ) {
        CompositionLocalProvider(LocalFutureCard provides card) {
            content()
        }
    }
}

/** 6dp - המרווח האנכי של הכרטיס, בין שתי דרגות הסקאלה. */
private val CardVerticalInset = 6.dp

/** 4dp - הריפוד האנכי בתוך הכרטיס (Card.jsx: --fos-space-1). */
private val CardContentInset = FutureDimens.spacingXs

/**
 * המידות של הכרטיס שהשורות צריכות כדי לדעת איפה הן יושבות. הגובה הוא
 * state: כשנוספת שורה בסוף, השורה שהייתה אחרונה מצוירת מחדש ומאבדת את
 * הפינות התחתונות, גם אם המיקום שלה עצמה לא זז.
 */
@Stable
class FutureCardGeometry internal constructor() {
    internal var coordinates: LayoutCoordinates? = null
        private set
    internal var height by mutableIntStateOf(0)
        private set

    internal fun onPlaced(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
        height = coordinates.size.height
    }
}

/** הכרטיס שמסביב, או null מחוץ לכרטיס (דיאלוג, רשימה על רקע המסך). */
val LocalFutureCard = staticCompositionLocalOf<FutureCardGeometry?> { null }

/**
 * משטח הפוקוס של שורה בתוך [FutureCard] - מילוי ומסגרת, ממלאים את כל רוחב
 * הכרטיס (SettingItem.jsx). השורה הראשונה מתפשטת מעל הריפוד העליון של
 * הכרטיס ושומרת על הפינות העליונות שלו (22dp), האחרונה מתחת לריפוד
 * התחתון עם הפינות התחתונות, ושורה באמצע היא מלבן. כך הפוקוס נראה כמו
 * חלק מהכרטיס ולא כמו גלולה שצפה בתוכו.
 *
 * המיקום נמדד בפועל ולא מועבר כפרמטר, כמו ש-Card.jsx מוצא את השורות שלו
 * גם דרך עטיפות: שורה בתוך תנאי, לולאה או Box יודעת איפה היא בלי שהקורא
 * יספור. הפוקוס מצויר מאחורי התוכן ואינו משנה את גובה השורה.
 *
 * מחוץ לכרטיס - [fallbackShape] רגיל, כמו קודם.
 */
@Composable
fun Modifier.cardRowFocus(
    background: Color,
    border: Color,
    borderWidth: Dp = FutureDimens.focusBorderControl,
    fallbackShape: Shape = FutureShapes.lg,
): Modifier = cardRowFocus({ background }, { border }, borderWidth, fallbackShape)

/**
 * אותו משטח פוקוס, עם הצבעים כ-lambda שנקראת בשלב הציור בלבד. זו הגרסה
 * לצבעים מונפשים: `cardRowFocus({ bg.value }, { ring.value })` על ה-State
 * שחוזר מ-animateColorAsState, בלי `by`. כך כל פריים של אנימציית הפוקוס
 * מצייר מחדש את השורה בלי להריץ את ה-composition שלה (הכותרת, התקציר,
 * האייקון) - ברשימת הגדרות שמגללים בה עם חץ מוחזק זה ההבדל בין פריים
 * חלק לפריים שנופל.
 */
@Composable
fun Modifier.cardRowFocus(
    background: () -> Color,
    border: () -> Color,
    borderWidth: Dp = FutureDimens.focusBorderControl,
    fallbackShape: Shape = FutureShapes.lg,
): Modifier {
    val card = LocalFutureCard.current
        ?: return this
            .clip(fallbackShape)
            .animatedFocusSurface(fallbackShape, borderWidth, background, border)

    var top by remember { mutableFloatStateOf(Float.NaN) }
    var bottom by remember { mutableFloatStateOf(Float.NaN) }
    return this
        .onGloballyPositioned { row ->
            val parent = card.coordinates ?: return@onGloballyPositioned
            if (!parent.isAttached || !row.isAttached) return@onGloballyPositioned
            val y = parent.localPositionOf(row, Offset.Zero).y
            top = y
            bottom = y + row.size.height
        }
        .drawBehind {
            val fillColor = background()
            val ringColor = border()
            if (fillColor.alpha == 0f && ringColor.alpha == 0f) return@drawBehind
            // שורה שמרחקה משפת הכרטיס הוא בדיוק הריפוד היא הראשונה/האחרונה.
            // חצי פיקסל של סובלנות - מיקום מעוגל לפיקסלים לא נופל תמיד
            // בדיוק על הערך.
            val edge = CardContentInset.toPx() + 0.5f
            val first = !top.isNaN() && top <= edge
            val last = !bottom.isNaN() && bottom >= card.height - edge
            val bleedTop = if (first) top else 0f
            val bleedBottom = if (last) card.height - bottom else 0f
            val radius = FutureShapes.radiusXl.toPx()
            val topRadius = if (first) radius else 0f
            val bottomRadius = if (last) radius else 0f

            val fullSize = Size(size.width, size.height + bleedTop + bleedBottom)
            translate(top = -bleedTop) {
                drawOutline(
                    RoundedCornerShape(topRadius, topRadius, bottomRadius, bottomRadius)
                        .createOutline(fullSize, layoutDirection, this),
                    fillColor,
                )
                // המסגרת נמתחת בתוך הצורה, בפינות קונצנטריות, כך שהיא לא נחתכת
                // בשפת הכרטיס (Card חותך את כל מה שיוצא מהעיגול שלו).
                val stroke = borderWidth.toPx()
                val half = stroke / 2f
                translate(left = half, top = half) {
                    drawOutline(
                        RoundedCornerShape(
                            (topRadius - half).coerceAtLeast(0f),
                            (topRadius - half).coerceAtLeast(0f),
                            (bottomRadius - half).coerceAtLeast(0f),
                            (bottomRadius - half).coerceAtLeast(0f),
                        ).createOutline(
                            Size(fullSize.width - stroke, fullSize.height - stroke),
                            layoutDirection,
                            this,
                        ),
                        ringColor,
                        style = Stroke(stroke),
                    )
                }
            }
        }
}

/**
 * קו שיער בין שתי שורות בכרטיס. מוסט 16dp מכל צד כדי שלא ייגע בפינות
 * המעוגלות של הכרטיס.
 */
@Composable
fun FutureDivider(
    theme: FutureTheme,
    modifier: Modifier = Modifier,
    inset: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (inset) FutureDimens.spacingLg else 0.dp)
            .height(FutureDimens.dividerThickness)
            .background(theme.dividerColor),
    ) {}
}

/**
 * כותרת קטע ("תצוגה", "צליל"). 13sp מודגש ב-55% מצבע הטקסט, עם ריווח
 * אותיות של 1sp - בעברית אין אותיות גדולות, והריווח הוא מה שמסמן את
 * השורה ככותרת במקום הגדלה או הפיכה לאותיות רישיות.
 */
@Composable
fun FutureSectionHeader(
    text: String,
    theme: FutureTheme,
    modifier: Modifier = Modifier,
    inset: Boolean = true,
) {
    val type = rememberFutureType()
    Text(
        text,
        color = theme.sectionHeaderColor,
        fontSize = type.summary,
        fontWeight = FutureTypography.weightBold,
        letterSpacing = FutureTypography.trackingSection,
        // inset = false - בתוך רשימה שכבר מרופדת מהשוליים (contentPadding),
        // שם הריפוד האופקי של הכותרת היה מזיז אותה פנימה פעמיים.
        modifier = modifier.padding(
            start = if (inset) FutureDimens.spacingXl else 0.dp,
            end = if (inset) FutureDimens.spacingXl else 0.dp,
            top = if (inset) 20.dp else FutureDimens.spacingSm,
            bottom = if (inset) FutureDimens.spacingSm else FutureDimens.spacingXs,
        ),
    )
}
