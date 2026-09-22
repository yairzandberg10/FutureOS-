package com.future.sharednav.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.future.sharednav.focus.FocusableItem
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.rememberFutureType

/**
 * שורת הרשימה התקנית - אנשי קשר, הודעות, קבצים, פתקים, כל דבר שנגלל
 * (components/core/ListItem.jsx): שורה בגובה 56dp לפחות, ריפוד 12dp,
 * כותרת 17sp בינונית, ושורת סיכום 13sp ב-60% מתחתיה. הפוקוס הוא של
 * [FocusableItem]: 14% הדגשה, מסגרת 1.5dp והגדלה 1.02.
 *
 * השורות מופרדות ב-12dp על רקע המסך ([FutureDimens.itemSpacing]) - אף פעם
 * לא בקו מפריד.
 *
 * [leading] - אייקון/אווטאר בתחילת השורה. [trailing] - תג, חץ, תיבת סימון.
 */
@Composable
fun FutureListItem(
    title: String,
    theme: FutureTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    summaryMaxLines: Int = 1,
    titleColor: Color = theme.textColor,
    titleDecoration: TextDecoration? = null,
    idleBackgroundColor: Color = Color.Transparent,
    focusRequester: FocusRequester? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val type = rememberFutureType()
    FocusableItem(
        onClick = onClick,
        accentColor = theme.accentColor,
        modifier = modifier.fillMaxWidth(),
        idleBackgroundColor = idleBackgroundColor,
        contentPadding = 0.dp,
        focusRequester = focusRequester,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = FutureDimens.rowHeightList)
                .padding(horizontal = FutureDimens.spacingMd, vertical = FutureDimens.spacingSm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingMd),
        ) {
            leading?.invoke()
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = titleColor,
                    fontSize = type.title,
                    fontWeight = FutureTypography.weightMedium,
                    textDecoration = titleDecoration,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (summary != null) {
                    Text(
                        summary,
                        color = theme.mutedTextColor,
                        fontSize = type.summary,
                        maxLines = summaryMaxLines,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = FutureDimens.spacingXxs),
                    )
                }
            }
            trailing?.invoke(this)
        }
    }
}
