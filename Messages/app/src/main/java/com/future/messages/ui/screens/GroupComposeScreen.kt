package com.future.messages.ui.screens

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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Person
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
        Column(modifier = Modifier.fillMaxSize().background(theme.backgroundColor).imePadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.future.sharednav.components.TopBarIconButton(
                    Icons.AutoMirrored.Rounded.ArrowBack, "ביטול", theme.textColor, theme.accentColor, onCancel
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("הודעה קבוצתית", color = theme.textColor, fontWeight = FontWeight.Bold, fontSize = FutureTypography.title)
                    if (selectedContacts.isNotEmpty()) {
                        Text(
                            "${selectedContacts.size} נבחרו",
                            color = theme.accentColor,
                            fontSize = FutureTypography.label
                        )
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .escapeTextFieldFocusTrap()
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .focusRequester(queryFocusRequester),
                placeholder = { Text("חיפוש איש קשר", color = theme.textColor.copy(alpha = 0.4f)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = theme.textColor,
                    unfocusedTextColor = theme.textColor,
                    focusedBorderColor = theme.accentColor,
                    unfocusedBorderColor = theme.textColor.copy(alpha = 0.3f),
                    cursorColor = theme.accentColor
                ),
                shape = FutureShapes.xl
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredContacts.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("אין אנשי קשר", color = theme.textColor.copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
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
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.escapeTextFieldFocusTrap().weight(1f),
                    placeholder = { Text("הודעה לכל הנבחרים...", color = theme.textColor.copy(alpha = 0.4f)) },
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = theme.textColor,
                        unfocusedTextColor = theme.textColor,
                        focusedBorderColor = theme.accentColor,
                        unfocusedBorderColor = theme.textColor.copy(alpha = 0.3f),
                        cursorColor = theme.accentColor
                    ),
                    shape = FutureShapes.xl
                )

                Spacer(modifier = Modifier.width(8.dp))

                GroupSendButton(
                    theme = theme,
                    enabled = selectedContacts.isNotEmpty() && messageText.isNotBlank(),
                    onClick = { onSend(selectedContacts, messageText) }
                )
            }
        }
    }
}

@Composable
private fun GroupContactRow(contact: Contact, isSelected: Boolean, theme: FutureTheme, onToggle: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(
        if (isFocused) theme.accentColor.copy(alpha = 0.18f) else Color.Transparent,
        label = "groupContactRowBg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FutureShapes.md)
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onToggle)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) theme.accentColor else theme.textColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Icon(Icons.Rounded.Person, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(contact.name, color = theme.textColor, fontWeight = FontWeight.Medium, fontSize = FutureTypography.bodyLarge)
            Text(contact.phoneNumber, color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.label)
        }
    }
}

@Composable
private fun GroupSendButton(theme: FutureTheme, enabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(
        when {
            !enabled -> theme.textColor.copy(alpha = 0.1f)
            isFocused -> theme.accentColor
            else -> theme.textColor.copy(alpha = 0.25f)
        },
        label = "groupSendBg"
    )
    val tint by animateColorAsState(
        if (isFocused && enabled) Color.Black else theme.textColor.copy(alpha = if (enabled) 1f else 0.4f),
        label = "groupSendTint"
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick)
            .focusable(interactionSource = interactionSource, enabled = enabled).bringIntoViewOnFocus(),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "שלח לכולם", tint = tint, modifier = Modifier.size(20.dp))
    }
}
