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
import com.future.sharednav.focus.escapeTextFieldFocusTrap

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
    onChatSetupClick: () -> Unit,
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
    // ארכיון: השיחות שהועברו אליו לא מופיעות ברשימה הראשית, ו"ארכיון" בכותרת
    // מציג רק אותן (ר' ArchiveStore).
    archivedThreadIds: Set<Long> = emptySet(),
    showingArchive: Boolean = false,
    onShowArchive: (Boolean) -> Unit = {},
    onToggleArchive: (Conversation) -> Unit = {},
    onMarkAllRead: () -> Unit = {},
) {
    val rowFocusRequesters = remember { mutableMapOf<Long, FocusRequester>() }
    val composeButtonFocusRequester = remember { FocusRequester() }
    var focusedConversation by remember { mutableStateOf<Conversation?>(null) }
    var menuFor by remember { mutableStateOf<Conversation?>(null) }
    var listMenuOpen by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Conversation?>(null) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    androidx.activity.compose.BackHandler(enabled = searchOpen || showingArchive) {
        if (searchOpen) {
            searchOpen = false
            searchText = ""
        } else {
            onShowArchive(false)
        }
    }

    // מה שמוצג בפועל: הרשימה הראשית או הארכיון, מסוננים לפי החיפוש (שם,
    // מספר או תוכן ההודעה האחרונה).
    val shown = remember(conversations, archivedThreadIds, showingArchive, searchText) {
        val query = searchText.trim()
        val digits = query.filter(Char::isDigit)
        conversations
            .filter { (it.threadId in archivedThreadIds) == showingArchive }
            .filter { c ->
                query.isEmpty() ||
                    c.contact.name.contains(query, ignoreCase = true) ||
                    c.lastMessageText.contains(query, ignoreCase = true) ||
                    (digits.isNotEmpty() && c.contact.phoneNumber.filter(Char::isDigit).contains(digits))
            }
    }
    val archivedCount = remember(conversations, archivedThreadIds) { conversations.count { it.threadId in archivedThreadIds } }
    val unreadCount = remember(conversations) { conversations.sumOf { it.unreadCount } }

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
    // שיחה ממוקדת - תפריט השיחה (עם פעולות הרשימה בסופו); אחרת - תפריט הרשימה.
    com.future.sharednav.nav.onOptionsKeyPress {
        val focused = focusedConversation?.takeIf { f -> shown.any { it.threadId == f.threadId } }
        if (focused != null) menuFor = focused else listMenuOpen = true
    }

    LaunchedEffect(shown.isEmpty(), showingArchive) {
        if (shown.isEmpty()) {
            focusedConversation = null
            if (!searchOpen) runCatching { composeButtonFocusRequester.requestFocus() }
        } else if (!searchOpen) {
            val target = shown.firstOrNull { it.threadId == lastSelectedThreadId } ?: shown.first()
            runCatching { rowFocusRequesters.getOrPut(target.threadId) { FocusRequester() }.requestFocus() }
        }
    }

    if (listMenuOpen) {
        FutureOptionsMenu(theme = theme, onDismissRequest = { listMenuOpen = false }, header = if (showingArchive) "ארכיון" else "הודעות") {
            ListMenuRows(
                theme = theme,
                unreadCount = unreadCount,
                archivedCount = archivedCount,
                showingArchive = showingArchive,
                onPick = { listMenuOpen = false },
                onCompose = onComposeClick,
                onSearch = { searchOpen = true },
                onMarkAllRead = onMarkAllRead,
                onShowArchive = onShowArchive,
            )
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
            isArchived = conversation.threadId in archivedThreadIds,
            onToggleArchive = { onToggleArchive(conversation); menuFor = null },
            onDelete = { pendingDelete = conversation; menuFor = null },
        ) {
            ListMenuRows(
                theme = theme,
                unreadCount = unreadCount,
                archivedCount = archivedCount,
                showingArchive = showingArchive,
                onPick = { menuFor = null },
                onCompose = onComposeClick,
                onSearch = { searchOpen = true },
                onMarkAllRead = onMarkAllRead,
                onShowArchive = onShowArchive,
            )
        }
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
            Column(modifier = Modifier.fillMaxSize().padding(top = com.future.sharednav.systemui.StatusBarInset.HEIGHT_DP.dp)) {
                // השורה העליונה של הדיזיין סיסטם (TopBar: 16/12dp, כותרת 20sp). מתחתיה
                // כפתור "הודעה חדשה" ראשי ברוחב מלא - קודם הוא היה אייקון אחד מתוך
                // שלושה בפינה, וקשה היה למצוא אותו.
                androidx.compose.animation.AnimatedContent(
                    targetState = searchOpen,
                    transitionSpec = { com.future.sharednav.theme.FutureTransitions.appear() },
                    label = "messagesHeader",
                ) { open ->
                    if (open) {
                        com.future.sharednav.components.FutureTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            theme = theme,
                            placeholder = if (showingArchive) "חיפוש בארכיון" else "חיפוש בהודעות",
                            autoFocus = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = FutureDimens.spacingLg, vertical = FutureDimens.spacingMd)
                                .escapeTextFieldFocusTrap(),
                            leading = { Icon(FutureIcons.Search, contentDescription = null, tint = theme.mutedTextColor, modifier = Modifier.size(FutureDimens.iconMenuRow)) },
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.spacingLg, vertical = FutureDimens.spacingMd),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (showingArchive) "ארכיון" else "הודעות", fontSize = FutureTypography.screenTitle, fontWeight = FontWeight.Bold, color = theme.textColor, modifier = Modifier.weight(1f))
                            Row(horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm)) {
                                TopBarIconButton(FutureIcons.Search, "חיפוש", theme.textColor, theme.accentColor, { searchOpen = true })
                                if (!showingArchive) {
                                    TopBarIconButton(FutureIcons.Folder, "ארכיון", theme.textColor, theme.accentColor, { onShowArchive(true) })
                                    TopBarIconButton(FutureIcons.Lock, "צ'אט FutureOS", theme.textColor, theme.accentColor, onChatSetupClick)
                                    TopBarIconButton(FutureIcons.Groups, "הודעה קבוצתית", theme.textColor, theme.accentColor, onGroupComposeClick)
                                }
                            }
                        }
                    }
                }
                if (!showingArchive) {
                    com.future.sharednav.components.FutureButton(
                        text = "הודעה חדשה",
                        theme = theme,
                        onClick = onComposeClick,
                        fillMaxWidth = true,
                        focusRequester = composeButtonFocusRequester,
                        modifier = Modifier.padding(horizontal = FutureDimens.screenPadding).padding(bottom = FutureDimens.spacingSm),
                    )
                }

                if (isLoading && conversations.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        FutureSpinner(theme = theme)
                    }
                } else if (shown.isEmpty()) {
                    EmptyState(
                        icon = when {
                            searchText.isNotBlank() -> FutureIcons.SearchOff
                            showingArchive -> FutureIcons.Folder
                            else -> FutureIcons.AutoMirrored.Chat
                        },
                        title = when {
                            searchText.isNotBlank() -> "לא נמצאו הודעות"
                            showingArchive -> "הארכיון ריק"
                            else -> "אין הודעות"
                        },
                        subtitle = when {
                            searchText.isNotBlank() -> null
                            showingArchive -> "Options על שיחה - \"העבר לארכיון\""
                            else -> "\"הודעה חדשה\" למעלה מתחיל שיחה"
                        },
                        textColor = theme.textColor,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
                        verticalArrangement = Arrangement.spacedBy(FutureDimens.itemSpacing),
                    ) {
                        itemsIndexed(shown, key = { _, it -> it.threadId }) { index, conversation ->
                            ConversationRow(
                                conversation,
                                theme,
                                onClick = { onConversationClick(conversation) },
                                onFocused = { focusedConversation = conversation },
                                focusRequester = rowFocusRequesters.getOrPut(conversation.threadId) { FocusRequester() },
                                // בשורה העליונה, מקש למעלה תמיד קופץ במפורש לכפתור "הודעה חדשה" -
                                // לא מסתמכים על חיפוש פוקוס גיאומטרי שנשבר אחרי גלילה למטה ואז למעלה
                                onNavigateUpFromTop = if (index == 0 && !showingArchive && !searchOpen) {
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

/** פעולות על כל הרשימה - בתפריט הרשימה, ובסוף תפריט השיחה. */
@Composable
private fun ListMenuRows(
    theme: FutureTheme,
    unreadCount: Int,
    archivedCount: Int,
    showingArchive: Boolean,
    onPick: () -> Unit,
    onCompose: () -> Unit,
    onSearch: () -> Unit,
    onMarkAllRead: () -> Unit,
    onShowArchive: (Boolean) -> Unit,
) {
    if (!showingArchive) FutureMenuRow("הודעה חדשה", FutureIcons.Edit, theme, { onPick(); onCompose() })
    FutureMenuRow("חיפוש", FutureIcons.Search, theme, { onPick(); onSearch() })
    if (unreadCount > 0) FutureMenuRow("סמן הכל כנקרא ($unreadCount)", FutureIcons.MarkEmailRead, theme, { onPick(); onMarkAllRead() })
    if (showingArchive) {
        FutureMenuRow("חזרה להודעות", FutureIcons.AutoMirrored.Chat, theme, { onPick(); onShowArchive(false) })
    } else {
        FutureMenuRow(if (archivedCount > 0) "ארכיון ($archivedCount)" else "ארכיון", FutureIcons.Folder, theme, { onPick(); onShowArchive(true) })
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
    isArchived: Boolean,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit,
    listRows: @Composable () -> Unit,
) {
    FutureOptionsMenu(theme = theme, onDismissRequest = onDismiss, header = conversation.contact.name) {
        FutureMenuRow("התקשר", FutureIcons.Call, theme, onCall)
        if (onAddToContacts != null) {
            FutureMenuRow("הוסף לאנשי קשר", FutureIcons.PersonAdd, theme, onAddToContacts)
        }
        FutureMenuRow(if (isArchived) "הוצא מהארכיון" else "העבר לארכיון", FutureIcons.Folder, theme, onToggleArchive)
        FutureMenuRow("מחק שיחה", FutureIcons.Delete, theme, onDelete, destructive = true)
        listRows()
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
