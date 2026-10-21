package com.future.dialer.ui.calllog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.future.dialer.data.model.CallFilter
import com.future.dialer.ui.CallFormat
import com.future.dialer.ui.CallsViewModel
import com.future.dialer.ui.requestFocusWhenAttached
import com.future.sharednav.components.EmptyState
import com.future.sharednav.components.FutureCard
import com.future.sharednav.components.FutureChip
import com.future.sharednav.components.FutureDivider
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureSettingItem
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.rememberFutureType
import com.future.sharednav.theme.subtleTextColor

/**
 * יומן השיחות - הטאב הראשון (ui_kits/calls). מעל הרשימה שורת צ'יפים של
 * סינון (הכל / לא נענו / התקבלו / חויגו / נדחו / חסומות) - חץ למעלה מהשיחה
 * הראשונה מגיע אליהם, וימינה/שמאלה זזים ביניהם. השיחות מקובצות לימים, ולכל
 * שיחה חץ בצבע הסוג שלה. אישור על שורה פותח את מסך איש הקשר.
 */
@Composable
fun CallLogScreen(
    viewModel: CallsViewModel,
    onOpen: (name: String, number: String) -> Unit,
) {
    val theme = LocalFutureTheme.current
    val type = rememberFutureType()
    val groups by viewModel.callDays.collectAsState()
    val filter by viewModel.filter.collectAsState()

    // השיחה שנפתחה אחרונה: אחרי BACK מאיש הקשר הפוקוס חוזר אליה ולא לשורה הראשונה
    var lastOpenedCallId by rememberSaveable { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val targetRow = remember { FocusRequester() }
    val targetId = lastOpenedCallId?.takeIf { id -> groups.any { day -> day.rows.any { it.call.id == id } } }
        ?: groups.firstOrNull()?.rows?.firstOrNull()?.call?.id
    LaunchedEffect(groups.isNotEmpty(), filter) {
        if (groups.isNotEmpty()) {
            // יום רחוק עוד לא נבנה ברשימה העצלה - גוללים אליו לפני בקשת הפוקוס
            listState.scrollToItem(groups.indexOfFirst { day -> day.rows.any { it.call.id == targetId } }.coerceAtLeast(0))
            targetRow.requestFocusWhenAttached()
        }
    }
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(title = "שיחות", textColor = theme.textColor, accentColor = theme.accentColor)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = FutureDimens.spacingLg),
            horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
        ) {
            items(CallFilter.entries.toList()) { option ->
                FutureChip(
                    option.label,
                    theme = theme,
                    selected = option == filter,
                    onClick = {
                        lastOpenedCallId = null
                        viewModel.setFilter(option)
                    },
                )
            }
        }

        if (groups.isEmpty()) {
            EmptyState(
                icon = if (filter == CallFilter.MISSED) FutureIcons.CallMissed else FutureIcons.Call,
                title = emptyTitleOf(filter),
                subtitle = if (filter == CallFilter.ALL) "הקלד מספר כדי להתקשר" else "בחר הכל כדי לראות את כל השיחות",
                textColor = theme.textColor,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            return@Column
        }

        // רשימה עצלה, יום לכל פריט: רק הימים שעל המסך נבנים.
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(top = FutureDimens.spacingXs, bottom = FutureDimens.spacingLg),
        ) {
            items(groups, key = { it.title + it.rows.first().call.id }) { day ->
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
                                iconTint = CallFormat.colorOf(call.type, theme),
                                theme = theme,
                                showChevron = false,
                                focusRequester = if (call.id == targetId) targetRow else null,
                                onClick = {
                                    if (call.isPrivate || call.phoneNumber.isBlank()) {
                                        // אין מספר לפתוח - הלחיצה לא נבלעת בשקט
                                        android.widget.Toast.makeText(context, "המספר חסוי", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        lastOpenedCallId = call.id
                                        onOpen(call.name.orEmpty(), call.phoneNumber)
                                    }
                                },
                                trailing = {
                                    Text(row.time, color = theme.subtleTextColor, fontSize = type.summary)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** כותרת המסך הריק לכל פילטר - משפט שלם בעברית (לא "אין שיחות" + שם הפילטר). */
private fun emptyTitleOf(filter: CallFilter): String = when (filter) {
    CallFilter.ALL -> "אין שיחות"
    CallFilter.MISSED -> "אין שיחות שלא נענו"
    CallFilter.INCOMING -> "אין שיחות שהתקבלו"
    CallFilter.OUTGOING -> "אין שיחות שחויגו"
    CallFilter.REJECTED -> "אין שיחות שנדחו"
    CallFilter.BLOCKED -> "אין שיחות שנחסמו"
}
