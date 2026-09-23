package com.future.dialer.ui.contact
import androidx.compose.material.icons.rounded.StarBorder

import com.future.sharednav.icons.FutureIcons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.future.dialer.ui.CallFormat
import com.future.dialer.ui.CallsViewModel
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.FutureCard
import com.future.sharednav.components.FutureDivider
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureSettingItem
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.rememberFutureType
import com.future.sharednav.theme.subtleTextColor

/**
 * מסך איש קשר (ui_kits/calls): אווטאר, שם ומספר במרכז; כרטיס פעולות (התקשר,
 * שלח הודעה, מועדפים); והשיחות האחרונות עם המספר. מספר שאינו איש קשר מקבל
 * "הוסף לאנשי קשר" במקום המועדפים.
 *
 * שורות ההיסטוריה הן מידע בלבד - בלי פוקוס, כי אין להן פעולה.
 */
@Composable
fun ContactScreen(
    name: String,
    number: String,
    viewModel: CallsViewModel,
    onBack: () -> Unit,
    onMenu: () -> Unit,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onAddContact: () -> Unit,
) {
    val theme = LocalFutureTheme.current
    val type = rememberFutureType()
    // נקרא מחדש כשאנשי הקשר או היומן מתעדכנים - מועדף שנוסף כאן משנה את השורה מיד.
    val contacts by viewModel.contacts.collectAsState()
    val calls by viewModel.recentCalls.collectAsState()
    val contact = remember(contacts, number) { viewModel.contactFor(number) }
    val history = remember(calls, number) { viewModel.historyFor(number) }
    val shownName = contact?.name ?: name.ifEmpty { number }

    val callRow = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { callRow.requestFocus() } }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = "איש קשר",
            textColor = theme.textColor,
            accentColor = theme.accentColor,
            onBack = onBack,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = FutureDimens.spacingLg),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = FutureDimens.spacingMd, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
            ) {
                // מספר בלי שם מקבל אייקון ולא "ראשי תיבות" של ספרות.
                val hasName = shownName != number
                FutureAvatar(
                    theme = theme,
                    name = if (hasName) shownName else null,
                    icon = if (hasName) null else FutureIcons.Person,
                    size = HeroAvatar,
                    photoUri = contact?.photoUri,
                )
                Text(
                    shownName,
                    color = theme.textColor,
                    fontSize = type.screenTitle,
                    fontWeight = FutureTypography.weightBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = FutureDimens.screenPadding),
                )
                if (shownName != number) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(number, color = theme.mutedTextColor, fontSize = type.body)
                    }
                }
            }

            FutureCard(theme = theme) {
                FutureSettingItem(
                    title = "התקשר",
                    icon = FutureIcons.Call,
                    theme = theme,
                    showChevron = false,
                    focusRequester = callRow,
                    onClick = onCall,
                )
                FutureDivider(theme = theme)
                FutureSettingItem(
                    title = "שלח הודעה",
                    icon = FutureIcons.AutoMirrored.Chat,
                    theme = theme,
                    showChevron = false,
                    onClick = onMessage,
                )
                FutureDivider(theme = theme)
                if (contact != null) {
                    FutureSettingItem(
                        title = if (contact.isFavorite) "הסר ממועדפים" else "הוסף למועדפים",
                        icon = if (contact.isFavorite) FutureIcons.Star else Icons.Rounded.StarBorder,
                        theme = theme,
                        showChevron = false,
                        onClick = { viewModel.toggleFavorite(contact) },
                    )
                } else {
                    FutureSettingItem(
                        title = "הוסף לאנשי קשר",
                        icon = FutureIcons.PersonAdd,
                        theme = theme,
                        showChevron = false,
                        onClick = onAddContact,
                    )
                }
            }

            if (history.isNotEmpty()) {
                FutureSectionHeader("היסטוריה", theme = theme)
                FutureCard(theme = theme) {
                    history.forEachIndexed { index, call ->
                        if (index > 0) FutureDivider(theme = theme)
                        FutureSettingItem(
                            title = CallFormat.labelOf(call.type),
                            summary = CallFormat.durationOf(call),
                            icon = CallFormat.iconOf(call.type),
                            iconTint = CallFormat.colorOf(call.type, theme),
                            theme = theme,
                            showChevron = false,
                            onClick = null,
                            trailing = {
                                Text(CallFormat.whenOf(call), color = theme.subtleTextColor, fontSize = type.summary)
                            },
                        )
                    }
                }
            }
        }
    }
}

/** 88dp - אווטאר גיבור (176px ב-Avatar.jsx). */
private val HeroAvatar = 88.dp
