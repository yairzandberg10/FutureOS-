package com.future.messages.ui.screens
import com.future.sharednav.systemui.StatusBarInset

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.components.TopBarIconButton
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.FutureCheckbox
import com.future.sharednav.components.EmptyState
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.mutedTextColor
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.messages.data.Contact
import com.future.messages.data.SmsRepository
import com.future.sharednav.focus.bringIntoViewOnFocus
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** מסך "הודעה קבוצתית" - בחירת כמה אנשי קשר בסימון (checkbox) מתוך כל
 * אנשי הקשר, עם חיפוש חי לסינון, וכתיבת הודעה אחת שנשלחת לכולם. השליחה
 * בפועל (הודעת SMS נפרדת לכל נמען) קורית ב-MainActivity.onSend, בדיוק כמו
 * שיחה רגילה - המסך הזה רק אוסף את הנמענים והטקסט. */
@Composable
fun GroupComposeScreen(
    theme: FutureTheme,
    repository: SmsRepository,
    onCancel: () -> Unit,
    onSend: (List<Contact>, String) -> Unit,
) {
    var allContacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    LaunchedEffect(Unit) {
        allContacts = withContext(Dispatchers.IO) { repository.getAllContacts() }
    }

    var query by remember { mutableStateOf("") }
    var selectedNumbers by remember { mutableStateOf(setOf<String>()) }
    var messageText by remember { mutableStateOf("") }
    val queryFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { queryFocusRequester.requestFocus() }

    val filteredContacts = remember(allContacts, query) {
        if (query.isBlank()) allContacts
        else allContacts.filter { it.name.contains(query, ignoreCase = true) || it.phoneNumber.contains(query) }
    }
    val selectedContacts = remember(allContacts, selectedNumbers) {
        allContacts.filter { selectedNumbers.contains(it.phoneNumber) }
    }

    fun toggle(contact: Contact) {
        selectedNumbers = if (selectedNumbers.contains(contact.phoneNumber)) {
            selectedNumbers - contact.phoneNumber
        } else {
            selectedNumbers + contact.phoneNumber
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        // ראו MessageThreadScreen: edge-to-edge מנטרל את adjustResize, ובלי
        // imePadding שדה החיפוש/הנמען נחבא מתחת למקלדת. background לפני
        // imePadding כדי שהרקע ימלא גם את השטח שמאחורי המקלדת.
        Column(modifier = Modifier.fillMaxSize().background(theme.backgroundColor).imePadding().padding(top = StatusBarInset.TITLE_GAP_DP.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.spacingLg, vertical = FutureDimens.spacingMd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("הודעה קבוצתית", color = theme.textColor, fontWeight = FontWeight.Bold, fontSize = FutureTypography.screenTitle)
                    if (selectedContacts.isNotEmpty()) {
                        Text(
                            "${selectedContacts.size} נבחרו",
                            color = theme.mutedTextColor,
                            fontSize = FutureTypography.summary
                        )
                    }
                }
            }

            FutureTextField(
                value = query,
                onValueChange = { query = it },
                theme = theme,
                placeholder = "חיפוש איש קשר",
                focusRequester = queryFocusRequester,
                modifier = Modifier.escapeTextFieldFocusTrap().fillMaxWidth().padding(horizontal = FutureDimens.screenPadding),
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredContacts.isEmpty()) {
                EmptyState(
                    icon = FutureIcons.Person,
                    title = "אין אנשי קשר",
                    textColor = theme.textColor,
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingXs),
                    verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingXs),
                ) {
                    items(filteredContacts, key = { it.phoneNumber }) { contact ->
                        GroupContactRow(
                            contact = contact,
                            isSelected = selectedNumbers.contains(contact.phoneNumber),
                            theme = theme,
                            onToggle = { toggle(contact) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FutureDimens.spacingMd, vertical = FutureDimens.spacingSm)
                    .escapeTextFieldFocusTrap(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
            ) {
                FutureTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    theme = theme,
                    placeholder = "הודעה לכל הנבחרים",
                    singleLine = false,
                    maxLines = 4,
                    modifier = Modifier.weight(1f),
                )

                GroupSendButton(
                    theme = theme,
                    enabled = selectedContacts.isNotEmpty() && messageText.isNotBlank(),
                    onClick = { onSend(selectedContacts, messageText) }
                )
            }
        }
    }
}

/** בחירת נמען - שורת רשימה עם אווטאר, ותיבת סימון בסופה (Checkbox.jsx). */
@Composable
private fun GroupContactRow(contact: Contact, isSelected: Boolean, theme: FutureTheme, onToggle: () -> Unit) {
    FutureListItem(
        title = contact.name,
        summary = contact.phoneNumber,
        theme = theme,
        onClick = onToggle,
        leading = { FutureAvatar(theme = theme, name = contact.name) },
        trailing = { FutureCheckbox(isSelected, theme) },
    )
}

@Composable
private fun GroupSendButton(theme: FutureTheme, enabled: Boolean, onClick: () -> Unit) {
    TopBarIconButton(FutureIcons.AutoMirrored.Send, "שלח לכולם", theme.textColor, theme.accentColor, onClick, enabled = enabled)
}
