package com.future.sharednav.focus

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.LocalFutureAccent
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes

/**
 * שורת רשימה עם פוקוס D-pad אמיתי - הגרסה המשותפת של רכיב שהיה קיים
 * בנפרד (ושונה מעט) בשלוש אפליקציות (dialer, Music, Sfarim). כל הקלט
 * הצבעוני/צורני הוא פרמטרים פרימיטיביים (Color/Dp/Boolean) ולא תלוי בשום
 * FutureTheme ספציפי-לאפליקציה, כדי שכל אפליקציה תעביר את צבעיה.
 *
 * ברירות המחדל הן הטוקנים של המערכת: פינות [FutureShapes.radiusRow] -
 * 20dp, הדרגה שהדיזיין סיסטם קובע לשורת רשימה (--fos-radius-row ב-
 * tokens/shape.css, ו-components/core/ListItem.jsx) - ומסגרת
 * [FutureDimens.focusBorderItem], 1.5dp, העובי שמפרט הפוקוס מייחד לשורת
 * רשימה להבדיל מפקד. פריט שאינו שורה (מקש, אריח ברשת) מעביר את הדרגה
 * שלו ב-[cornerRadius].
 *
 * תנועה: הרקע והמסגרת נצבעים פנימה ב-[FutureMotion.focusColorSpec] (קודם
 * המסגרת קפצה בבת אחת, רק הרקע הונפש), הפריט גדל מעט בפוקוס ומתכווץ לרגע
 * בלחיצת OK (ר' [focusMotion]).
 *
 * כולל bringIntoViewOnFocus - כל מקום שמשתמש ב-FocusableItem בתוך
 * LazyColumn/LazyRow מקבל אוטומטית גלילה לפריט הממוקד.
 */
@Composable
fun FocusableItem(
    onClick: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
    idleBackgroundColor: Color = Color.Transparent,
    focusedBackgroundColor: Color = accentColor.copy(alpha = 0.14f),
    borderColor: Color = accentColor,
    borderWidth: Dp = FutureDimens.focusBorderItem,
    cornerRadius: Dp = FutureShapes.radiusRow,
    scaleOnFocus: Boolean = true,
    focusedScale: Float = FutureDimens.focusScale,
    showBorderOnFocus: Boolean = true,
    contentPadding: Dp = FutureDimens.spacingXs,
    focusRequester: FocusRequester? = null,
    content: @Composable BoxScope.(isFocused: Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    // ההדגשה המתוקנת של המסך, אם יש כזו. בלעדיה שורה ממוקדת במצב בהיר עם
    // הדגשה לבנה (ברירת המחדל) מקבלת מילוי לבן ומסגרת לבנה על כרטיס לבן -
    // כלומר שום סימון. מי שמעביר צבע מתוקן בעצמו מקבל אותו כמו שהוא, ומי
    // שקורא מחוץ ל-ScreenScaffold מקבל את ההתנהגות הקודמת בדיוק.
    val screenAccent = LocalFutureAccent.current
    val fill = if (screenAccent != null && focusedBackgroundColor == accentColor.copy(alpha = 0.14f)) {
        screenAccent.copy(alpha = 0.14f)
    } else {
        focusedBackgroundColor
    }
    val ring = if (screenAccent != null && borderColor == accentColor) screenAccent else borderColor

    // State ולא `by`: הערכים נקראים רק בשלב הציור (animatedFocusSurface), כך
    // שכל פריים של האנימציה מצייר מחדש בלי להריץ את ה-composition של הפריט.
    val backgroundColor = animateColorAsState(
        if (isFocused) fill else idleBackgroundColor,
        FutureMotion.focusColorSpec,
        label = "focusableItemBg",
    )
    val ringColor = animateColorAsState(
        if (showBorderOnFocus && isFocused) ring else ring.copy(alpha = 0f),
        FutureMotion.focusColorSpec,
        label = "focusableItemRing",
    )

    Box(
        modifier = modifier
            .bringIntoViewOnFocus()
            .focusMotion(interactionSource, focusedScale = if (scaleOnFocus) focusedScale else 1f)
            .clip(shape)
            .animatedFocusSurface(shape, borderWidth, fill = { backgroundColor.value }, ring = { ringColor.value })
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(contentPadding),
        content = { content(isFocused) },
    )
}
