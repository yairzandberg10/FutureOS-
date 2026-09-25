package com.future.sharednav.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.rememberFutureType
import com.future.sharednav.theme.textAlpha

/**
 * SoftKeyBar (components/navigation/SoftKeyBar.jsx): שלוש התוויות בתחתית המסך
 * שאומרות מה עושים שני המקשים הרכים ומקש OK במסך הזה - המקבילה של סרגל
 * תחתון במכשיר מקשים, והמקרא היחיד שנשאר גלוי כל הזמן. [left] ו-[right]
 * צמודים לקצוות (ב-RTL: [left] מימין), התווית האמצעית היא פעולת OK בצבע
 * ההדגשה. מקש בלי פעולה נשאר ריק, אף פעם לא "—".
 *
 * 36dp גובה (72px), ריפוד 16dp, רווח 12dp, קו עליון 1dp ב-10% מהטקסט.
 */
@Composable
fun FutureSoftKeyBar(
    theme: FutureTheme,
    left: String? = null,
    center: String? = null,
    right: String? = null,
    modifier: Modifier = Modifier,
    backgroundColor: Color = theme.backgroundColor,
) {
    val type = rememberFutureType()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(modifier = modifier.fillMaxWidth().background(backgroundColor)) {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(theme.textAlpha(10)))
            Row(
                modifier = Modifier.fillMaxWidth().height(SoftKeyBarHeight).padding(horizontal = FutureDimens.spacingLg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingMd),
            ) {
                Text(
                    left.orEmpty(),
                    color = theme.textAlpha(60),
                    fontSize = type.summary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    center.orEmpty(),
                    color = theme.readableAccentColor,
                    fontSize = type.summary,
                    fontWeight = FutureTypography.weightBold,
                    maxLines = 1,
                )
                Text(
                    right.orEmpty(),
                    color = theme.textAlpha(60),
                    fontSize = type.summary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private val SoftKeyBarHeight = 36.dp
