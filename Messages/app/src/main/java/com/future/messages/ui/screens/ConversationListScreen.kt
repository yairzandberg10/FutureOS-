package com.future.messages.ui.screens
import com.future.sharednav.theme.onAccentColor
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.components.MarqueeText
import com.future.sharednav.focus.bringIntoViewOnFocus

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
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.messages.data.Conversation
import com.future.sharednav.components.AppDialog
import com.future.sharednav.components.ConfirmDialog
import com.future.sharednav.theme.FutureTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ConversationListScreen(
    conversations: List<Conversation>,
    theme: FutureTheme,
    // רשימה ריקה ורשימה שעדיין נטענת נראו עד כה בדיוק אותו דבר. הטעינה עצמה
    // רצה ב-Dispatchers.IO (ראו MainActivity), אז המסך מצויר לפני שיש נתונים.
    isLoading: Boolean = false,
    onConversationClick: (Conversation) -> Unit,
    onComposeClick: () -> Unit,
    onGroupComposeClick: () -> Unit,
    onCallConversation: (Conversation) -> Unit,
    onAddToContacts: (Conversation) -> Unit,
    onDeleteConversation: (Conversation) -> Unit,
    // מתעדכן עם מספר הטלפון של השיחה הממוקדת כרגע ברשימה (או null כשאין) -
    // כדי ש-MainActivity יוכל לחייג אליה ישירות במקש החיוג הפיזי (KEYCODE_CALL),
    // שמגיע ברמת ה-Activity/View ולא כ-KeyEvent של Compose (ראו onOptionsKeyPress
    // באותה בעיה בדיוק, למקש Options).
    onFocusedConversationChanged: (String?) -> Unit = {},
    // השיחה שממנה נכנסנו לאחרונה למסך ה-thread - כשחוזרים "אחורה" הפוקוס
    // צריך לשוב לשורה הזו בדיוק, לא תמיד לשורה הראשונה ברשימה.
    lastSelectedThreadId: Long? = null,
) {
    val rowFocusRequesters = remember { mutableMapOf<Long, FocusRequester>() }
    val composeButtonFocusRequester = remember { FocusRequester() }
    var focusedConversation by remember { mutableStateOf<Conversation?>(null) }
    var menuFor by remember { mutableStateOf<Conversation?>(null) }
    var pendingDelete by remember { mutableStateOf<Conversation?>(null) }

    LaunchedEffect(focusedConversation) {
        onFocusedConversationChanged(focusedConversation?.contact?.phoneNumber)
    }
    // כשעוזבים את מסך הרשימה (למשל נכנסים לשיחה ספציפית), חייבים לנקות את
    // המספר הממוקד - אחרת מקש החיוג הפיזי במסך אחר עלול לחייג בטעות למספר
    // ישן מרשימה שכבר לא מוצגת (ראו MainActivity.focusedConversationPhone).
    DisposableEffect(Unit) {
        onDispose { onFocusedConversationChanged(null) }
    }
    // מקש Options הפיזי נחסם ברמת המערכת ולא מגיע כ-Key.Menu לאפליקציה -
    // זו הדרך האמיתית שהוא פותח את תפריט הפעולות של השיחה הממוקדת (ראו
    // אותו דפוס ב-Contact/ContactsScreens.kt וב-Files/FilesScreen.kt).
    com.future.sharednav.nav.onOptionsKeyPress { if (focusedConversation != null) menuFor = focusedConversation }

    LaunchedEffect(conversations.isEmpty()) {
        if (conversations.isEmpty()) {
            focusedConversation = null
            composeButtonFocusRequester.requestFocus()
        } else {
            val target = conversations.firstOrNull { it.threadId == lastSelectedThreadId } ?: conversations.first()
            rowFocusRequesters.getOrPut(target.threadId) { FocusRequester() }.requestFocus()
        }
    }

    menuFor?.let { conversation ->
        ConversationOptionsMenu(
            conversation = conversation,
            theme = theme,
            onDismiss = { menuFor = null },
            onCall = { onCallConversation(conversation); menuFor = null },
            onAddToContacts = if (conversation.contact.name == conversation.contact.phoneNumber) {
                { onAddToContacts(conversation); menuFor = null }
            } else null,
            onDelete = { pendingDelete = conversation; menuFor = null },
        )
    }

    pendingDelete?.let { conversation ->
        ConfirmDialog(
            message = "למחוק את השיחה עם \"${conversation.contact.name}\"?",
            surfaceColor = theme.surfaceColor,
            textColor = theme.textColor,
            dangerColor = theme.dangerColor,
            onCancel = { pendingDelete = null },
            onConfirm = {
                onDeleteConversation(conversation)
                pendingDelete = null
            },
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("הודעות", fontSize = FutureTypography.headline, fontWeight = FontWeight.Bold, color = theme.textColor)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        com.future.sharednav.components.TopBarIconButton(
                            Icons.Rounded.Groups, "הודעה קבוצתית", theme.textColor, theme.accentColor, onGroupComposeClick
                        )
                        ComposeButton(theme, onClick = onComposeClick, focusRequester = composeButtonFocusRequester)
                    }
                }

                if (isLoading && conversations.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = theme.accentColor)
                    }
                } else if (conversations.isEmpty()) {
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
                                onFocused = { focusedConversation = conversation },
                                focusRequester = rowFocusRequesters.getOrPut(conversation.threadId) { FocusRequester() },
                                // בשורה העליונה, מקש למעלה תמיד קופץ במפורש לכפתור "הודעה חדשה" -
                                // לא מסתמכים על חיפוש פוקוס גיאומטרי שנשבר אחרי גלילה למטה ואז למעלה
                                onNavigateUpFromTop = if (index == 0) {
                                    { composeButtonFocusRequester.requestFocus() }
                                } else null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: Conversation,
    theme: FutureTheme,
    onClick: () -> Unit,
    onFocused: () -> Unit = {},
    focusRequester: FocusRequester? = null,
    onNavigateUpFromTop: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    LaunchedEffect(isFocused) { if (isFocused) onFocused() }
    val shape = FutureShapes.lg
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
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
            .then(
                if (onNavigateUpFromTop != null) {
                    Modifier.onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                            onNavigateUpFromTop()
                            true
                        } else false
                    }
                } else Modifier
            )
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
                fontSize = FutureTypography.bodyLarge
            )
            MarqueeText(
                text = conversation.lastMessageText,
                color = theme.textColor.copy(alpha = if (hasUnread) 0.9f else 0.55f),
                isFocused = isFocused,
                style = androidx.compose.ui.text.TextStyle(fontSize = FutureTypography.summary),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatTime(conversation.lastMessageTimestamp),
                color = theme.textColor.copy(alpha = 0.5f),
                fontSize = FutureTypography.caption
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
                        fontSize = FutureTypography.caption,
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
    val tint by animateColorAsState(if (isFocused) theme.onAccentColor else theme.textColor, label = "composeTint")

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(bgColor)
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus(),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = "הודעה חדשה", tint = tint, modifier = Modifier.size(20.dp))
    }
}

/** תפריט אפשרויות לשיחה - נפתח במקש Options כשהשורה ממוקדת (ראו onOptionsKeyPress
 * למעלה). "הוסף לאנשי קשר" מוצג רק כשהמספר לא זוהה כאיש קשר קיים (onAddToContacts == null אחרת). */
@Composable
private fun ConversationOptionsMenu(
    conversation: Conversation,
    theme: FutureTheme,
    onDismiss: () -> Unit,
    onCall: () -> Unit,
    onAddToContacts: (() -> Unit)?,
    onDelete: () -> Unit,
) {
    AppDialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(FutureShapes.xl)
                    .background(theme.surfaceColor)
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    conversation.contact.name,
                    color = theme.textColor.copy(alpha = 0.5f),
                    fontSize = FutureTypography.label,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                ConversationMenuRow("התקשר", Icons.Rounded.Call, theme, onClick = onCall)
                if (onAddToContacts != null) {
                    ConversationMenuRow("הוסף לאנשי קשר", Icons.Rounded.PersonAdd, theme, onClick = onAddToContacts)
                }
                ConversationMenuRow("מחק שיחה", Icons.Rounded.Delete, theme, onClick = onDelete, isDestructive = true)
            }
        }
    }
}

@Composable
private fun ConversationMenuRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    theme: FutureTheme,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(if (isFocused) theme.accentColor.copy(alpha = 0.25f) else Color.Transparent, label = "convMenuRowBg")
    val contentColor = if (isDestructive) theme.dangerColor else theme.textColor
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, color = contentColor, fontSize = FutureTypography.bodyLarge, fontWeight = FontWeight.Medium)
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
