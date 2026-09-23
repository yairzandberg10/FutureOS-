package com.future.dialer.ui.calllog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallMissed
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.future.dialer.ui.CallFormat
import com.future.dialer.ui.CallsViewModel
import com.future.sharednav.components.EmptyState
import com.future.sharednav.components.FutureCard
import com.future.sharednav.components.FutureChip
import com.future.sharednav.components.FutureDivider
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureSettingItem
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.rememberFutureType
import com.future.sharednav.theme.subtleTextColor

/**
 * יומן השיחות - הטאב הראשון (ui_kits/calls). השיחות מקובצות לפי יום, כל
 * יום הוא כרטיס אחד של שורות שקופות, ומעליהן שני צ'יפים שמראים את הסינון.
 * אישור על שורה פותח את מסך איש הקשר.
 *
 * הסינון מתחלף מתפריט האפשרויות, או במקש f כשמחוברת מקלדת מלאה - הצ'יפים
 * עצמם לא מקבלים פוקוס, כי חיצי ימין/שמאל כבר מחליפים טאב.
 */
@Composable
fun CallLogScreen(
    viewModel: CallsViewModel,
    showMissedOnly: Boolean,
    onOpen: (name: String, number: String) -> Unit,
    onMenu: () -> Unit,
) {
    val theme = LocalFutureTheme.current
    val type = rememberFutureType()
    // מקובץ ומפורמט מראש ב-ViewModel, ברקע.
    val allDays by viewModel.callDays.collectAsState()
    val missedDays by viewModel.missedCallDays.collectAsState()
    val groups = if (showMissedOnly) missedDays else allDays

    val firstRow = remember { FocusRequester() }
    LaunchedEffect(groups.isNotEmpty(), showMissedOnly) {
        if (groups.isEmpty()) return@LaunchedEffect
        // השורה הראשונה נבנית רק בשלב ה-layout של הרשימה העצלה, אחרי ה-
        // composition - מחכים פריים (או שניים) עד שהיא מחוברת.
        repeat(3) {
            // גרסאות Compose שונות: זורק כשהשורה לא מחוברת, או מחזיר false.
            val result = runCatching { firstRow.requestFocus() }.getOrNull()
            if (result != null && result != false) return@LaunchedEffect
            withFrameNanos { }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = "שיחות",
            textColor = theme.textColor,
            accentColor = theme.accentColor,
            trailingIcon = Icons.Rounded.MoreVert,
            trailingContentDescription = "אפשרויות",
            onTrailingClick = onMenu,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FutureDimens.spacingLg),
            horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
        ) {
            FutureChip("הכל", theme = theme, selected = !showMissedOnly)
            FutureChip("לא נענו", theme = theme, selected = showMissedOnly)
        }

        if (groups.isEmpty()) {
            EmptyState(
                icon = if (showMissedOnly) Icons.Rounded.CallMissed else Icons.Rounded.Call,
                title = if (showMissedOnly) "אין שיחות שלא נענו" else "אין שיחות",
                subtitle = if (showMissedOnly) "לחץ על מקש התפריט כדי לחזור לכל השיחות" else "הקלד מספר כדי להתקשר",
                textColor = theme.textColor,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            return@Column
        }

        // רשימה עצלה, יום לכל פריט: רק הימים שעל המסך נבנים. קודם כל היומן
        // (מאות שיחות, כל אחת שורה עם פוקוס ואנימציה) נבנה בבת אחת בכל כניסה
        // לטאב - זו הייתה עיקר האיטיות של האפליקציה. כל יום נשאר כותרת +
        // כרטיס אחד, כך שהמראה זהה.
        val firstId = groups.first().rows.first().call.id
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = FutureDimens.spacingLg),
        ) {
            items(groups, key = { it.title }) { day ->
                Column {
                    FutureSectionHeader(day.title, theme = theme)
                    FutureCard(theme = theme) {
                        day.rows.forEachIndexed { index, row ->
                            if (index > 0) FutureDivider(theme = theme)
                            val call = row.call
                            FutureSettingItem(
                                title = row.title,
                                summary = row.summary,
                                icon = CallFormat.iconOf(call.type),
                                theme = theme,
                                showChevron = false,
                                focusRequester = if (call.id == firstId) firstRow else null,
                                onClick = { onOpen(call.name.orEmpty(), call.phoneNumber) },
                                trailing = {
                                    Text(
                                        row.time,
                                        color = theme.subtleTextColor,
                                        fontSize = type.summary,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
