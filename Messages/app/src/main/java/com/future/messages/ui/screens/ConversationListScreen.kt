package com.future.messages.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.messages.data.Conversation
import com.future.messages.ui.theme.FutureTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ConversationListScreen(
    conversations: List<Conversation>,
    theme: FutureTheme,
    onConversationClick: (Conversation) -> Unit,
    onComposeClick: () -> Unit
) {
    val firstRowFocusRequester = remember { FocusRequester() }
    val composeButtonFocusRequester = remember { FocusRequester() }
    LaunchedEffect(conversations.isEmpty()) {
        if (conversations.isEmpty()) composeButtonFocusRequester.requestFocus() else firstRowFocusRequester.requestFocus()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("הודעות", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = theme.textColor)
                    ComposeButton(theme, onClick = onComposeClick, focusRequester = composeButtonFocusRequester)
                }

                if (conversations.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("אין הודעות עדיין", color = theme.textColor.copy(alpha = 0.5f))
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(conversations, key = { _, it -> it.threadId }) { index, conversation ->
                            ConversationRow(
                                conversation,
                                theme,
                                onClick = { onConversationClick(conversation) },
                                focusRequester = if (index == 0) firstRowFocusRequester else null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(conversation: Conversation, theme: FutureTheme, onClick: () -> Unit, focusRequester: FocusRequester? = null) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)
    val hasUnread = conversation.unreadCount > 0
    val bgColor by animateColorAsState(
        if (isFocused) theme.textColor.copy(alpha = 0.18f) else theme.textColor.copy(alpha = 0.06f),
        label = "rowBg"
    )
    val scale by animateFloatAsState(if (isFocused) 1.02f else 1f, label = "rowScale")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(bgColor)
            .then(if (isFocused) Modifier.border(2.dp, theme.accentColor, shape) else Modifier)
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(theme.accentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = conversation.contact.name.take(1).uppercase(),
                color = theme.accentColor,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.contact.name,
                color = theme.textColor,
                fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(
                text = conversation.lastMessageText,
                color = theme.textColor.copy(alpha = if (hasUnread) 0.9f else 0.55f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatTime(conversation.lastMessageTimestamp),
                color = theme.textColor.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
            if (hasUnread) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(theme.accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = conversation.unreadCount.toString(),
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ComposeButton(theme: FutureTheme, onClick: () -> Unit, focusRequester: FocusRequester? = null) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(if (isFocused) theme.accentColor else theme.textColor.copy(alpha = 0.15f), label = "composeBg")
    val tint by animateColorAsState(if (isFocused) Color.Black else theme.textColor, label = "composeTint")

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(bgColor)
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = "הודעה חדשה", tint = tint, modifier = Modifier.size(20.dp))
    }
}

private fun formatTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = timestamp }
    val pattern = if (now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR) && now.get(Calendar.YEAR) == then.get(Calendar.YEAR)) {
        "HH:mm"
    } else {
        "dd.MM"
    }
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
}
