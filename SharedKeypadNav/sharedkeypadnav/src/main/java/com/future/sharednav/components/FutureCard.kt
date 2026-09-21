package com.future.sharednav.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
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
 */
@Composable
fun FutureCard(
    theme: FutureTheme,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FutureDimens.spacingLg, vertical = CardVerticalInset)
            .shadow(FutureElevation.card(theme.isDarkMode), FutureShapes.xl)
            .clip(FutureShapes.xl)
            .background(theme.surfaceColor)
            .padding(vertical = FutureDimens.spacingXs),
        content = content,
    )
}

/** 6dp - המרווח האנכי של הכרטיס, בין שתי דרגות הסקאלה. */
private val CardVerticalInset = 6.dp

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
) {
    val type = rememberFutureType()
    Text(
        text,
        color = theme.sectionHeaderColor,
        fontSize = type.summary,
        fontWeight = FutureTypography.weightBold,
        letterSpacing = FutureTypography.trackingSection,
        modifier = modifier.padding(
            start = FutureDimens.spacingXl,
            end = FutureDimens.spacingXl,
            top = 20.dp,
            bottom = FutureDimens.spacingSm,
        ),
    )
}
