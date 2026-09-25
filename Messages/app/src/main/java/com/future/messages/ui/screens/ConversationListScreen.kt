package com.future.messages.ui.screens

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.components.TopBarIconButton
import com.future.sharednav.components.FutureSpinner
import com.future.sharednav.components.EmptyState
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.FutureBadge
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.focus.FocusableItem
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.mutedTextColor
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
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
                // השורה העליונה של הדיזיין סיסטם (TopBar: 16/12dp, כותרת 20sp) עם שני
                // כפתורי אייקון. הכותרת הייתה 24sp בריפוד 20dp - אחרת מכל מסך אחר.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.spacingLg, vertical = FutureDimens.spacingMd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("הודעות", fontSize = FutureTypography.screenTitle, fontWeight = FontWeight.Bold, color = theme.textColor, modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm)) {
                        TopBarIconButton(FutureIcons.Groups, "הודעה קבוצתית", theme.textColor, theme.accentColor, onGroupComposeClick)
                        TopBarIconButton(FutureIcons.AutoMirrored.Chat, "הודעה חדשה", theme.textColor, theme.accentColor, onComposeClick, composeButtonFocusRequester)
                    }
                }

                if (isLoading && conversations.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        FutureSpinner(theme = theme)
                    }
                } else if (conversations.isEmpty()) {
                    EmptyState(
                        icon = FutureIcons.AutoMirrored.Chat,
                        title = "אין הודעות",
                        subtitle = "כפתור ההודעה החדשה נמצא בראש המסך",
                        textColor = theme.textColor,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
                        verticalArrangement = Arrangement.spacedBy(FutureDimens.itemSpacing),
                    ) {
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

/**
 * שורת שיחה - שורת הרשימה של הדיזיין סיסטם (FocusableItem: 14% הדגשה, מסגרת
 * 1.5dp, 1.02) עם אווטאר ראשי תיבות ותג מונה (Badge.jsx). קודם הפוקוס כאן היה
 * 18% מהטקסט עם מסגרת 2dp, והאווטאר היה ב-20% מההדגשה.
 */
@Composable
private fun ConversationRow(
    conversation: Conversation,
    theme: FutureTheme,
    onClick: () -> Unit,
    onFocused: () -> Unit = {},
    focusRequester: FocusRequester? = null,
    onNavigateUpFromTop: (() -> Unit)? = null,
) {
    val hasUnread = conversation.unreadCount > 0
    FocusableItem(
        onClick = onClick,
        accentColor = theme.accentColor,
        contentPadding = 0.dp,
        focusRequester = focusRequester,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onNavigateUpFromTop != null) {
                    Modifier.onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                            onNavigateUpFromTop()
                            true
                        } else false
                    }
                } else Modifier
            ),
    ) { isFocused ->
        LaunchedEffect(isFocused) { if (isFocused) onFocused() }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = FutureDimens.rowHeightList)
                .padding(horizontal = FutureDimens.spacingMd, vertical = FutureDimens.spacingSm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingMd),
        ) {
            FutureAvatar(theme = theme, name = conversation.contact.name)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.contact.name,
                    color = theme.textColor,
                    fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Medium,
                    fontSize = FutureTypography.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                MarqueeText(
                    text = conversation.lastMessageText,
                    color = if (hasUnread) theme.textColor else theme.mutedTextColor,
                    isFocused = isFocused,
                    style = androidx.compose.ui.text.TextStyle(fontSize = FutureTypography.summary),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingXs)) {
                Text(
                    text = formatTime(conversation.lastMessageTimestamp),
                    color = theme.mutedTextColor,
                    fontSize = FutureTypography.caption
                )
                if (hasUnread) {
                    FutureBadge(conversation.unreadCount, theme)
                }
            }
        }
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
    FutureOptionsMenu(theme = theme, onDismissRequest = onDismiss, header = conversation.contact.name) {
        FutureMenuRow("התקשר", FutureIcons.Call, theme, onCall)
        if (onAddToContacts != null) {
            FutureMenuRow("הוסף לאנשי קשר", FutureIcons.PersonAdd, theme, onAddToContacts)
        }
        FutureMenuRow("מחק שיחה", FutureIcons.Delete, theme, onDelete, destructive = true)
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
