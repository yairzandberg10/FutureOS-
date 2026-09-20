package com.future.messages.ui.screens
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.components.ConfirmDialog
import com.future.sharednav.focus.bringIntoViewOnFocus

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.sharednav.components.AppDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.future.messages.data.Conversation
import com.future.messages.data.Message
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessageThreadScreen(
    conversation: Conversation,
    messages: List<Message>,
    theme: FutureTheme,
    initialDraftText: String = "",
    initialDraftImageUri: Uri? = null,
    onBack: () -> Unit,
    onSend: (text: String, imageUri: Uri?) -> Unit,
    onCall: () -> Unit,
    onDeleteMessage: (Message) -> Unit,
    onForwardMessage: (Message) -> Unit
) {
    var textState by remember { mutableStateOf(initialDraftText) }
    var attachedImageUri by remember { mutableStateOf(initialDraftImageUri) }
    var actionMenuMessage by remember { mutableStateOf<Message?>(null) }
    // מחיקת הודעה היא בלתי הפיכה ואין undo בשום מקום במערכת - אישור לפני.
    var pendingDelete by remember { mutableStateOf<Message?>(null) }
    var focusedMessage by remember { mutableStateOf<Message?>(null) }
    val textFieldFocusRequester = remember { FocusRequester() }

    // מקש Options הפיזי נחסם ברמת המערכת ולא מגיע כ-Key.Menu לאפליקציה - זו
    // הדרך האמיתית שהוא פותח את תפריט הפעולות (העברה/מחיקה) של ההודעה הממוקדת,
    // באותו דפוס שקיים בכל שאר האפליקציות (ראו ConversationListScreen).
    com.future.sharednav.nav.onOptionsKeyPress { if (focusedMessage != null) actionMenuMessage = focusedMessage }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) attachedImageUri = uri
    }

    LaunchedEffect(Unit) {
        textFieldFocusRequester.requestFocus()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        // enableEdgeToEdge() מבטל את decorFitsSystemWindows, ואז
        // windowSoftInputMode="adjustResize" כבר לא מקטין את החלון כשהמקלדת עולה -
        // בלי imePadding שורת כתיבת ההודעה נשארת מתחת למקלדת ולא רואים מה מקלידים.
        Column(modifier = Modifier.fillMaxSize().imePadding().background(theme.backgroundColor)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "חזור", theme, onClick = onBack)
                Text(
                    text = conversation.contact.name,
                    color = theme.textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = FutureTypography.title,
                    modifier = Modifier.weight(1f)
                )
                HeaderIconButton(Icons.Rounded.Call, "התקשר", theme, onClick = onCall)
            }

            val messageListState = rememberLazyListState()
            val messageListScope = rememberCoroutineScope()
            LazyColumn(
                state = messageListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .focusable().bringIntoViewOnFocus()
                    .onKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                        when (event.key) {
                            // reverseLayout=true: אינדקס 0 הוא ההודעה החדשה ביותר בתחתית -
                            // "מעלה" (הודעות ישנות יותר) = גלילה לאינדקסים גבוהים יותר.
                            // מקש שלא יכול עוד לגלול בכיוון הזה (כבר בקצה) לא נבלע -
                            // אחרת אין דרך להזיז את הפוקוס אל מחוץ לרשימה (למשל חזרה
                            // לשדה כתיבת ההודעה למטה, אחרי שגללו למעלה להודעות ישנות).
                            Key.DirectionUp -> {
                                if (messageListState.canScrollForward) {
                                    messageListScope.launch { messageListState.animateScrollBy(150f) }
                                    true
                                } else false
                            }
                            Key.DirectionDown -> {
                                if (messageListState.canScrollBackward) {
                                    messageListScope.launch { messageListState.animateScrollBy(-150f) }
                                    true
                                } else false
                            }
                            else -> false
                        }
                    },
                reverseLayout = true,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                items(messages.reversed(), key = { "${it.isMms}_${it.id}" }) { message ->
                    MessageBubble(
                        message, theme,
                        onClick = { actionMenuMessage = message },
                        onFocused = { focusedMessage = message },
                    )
                }
            }

            if (attachedImageUri != null) {
                AttachmentPreview(
                    uri = attachedImageUri!!,
                    theme = theme,
                    onRemove = { attachedImageUri = null }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                AttachButton(theme = theme, onClick = { imagePicker.launch("image/*") })

                Spacer(modifier = Modifier.width(4.dp))

                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    modifier = Modifier
                        .escapeTextFieldFocusTrap()
                        .weight(1f)
                        .focusRequester(textFieldFocusRequester),
                    placeholder = { Text("הודעה...", color = theme.textColor.copy(alpha = 0.4f)) },
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

                SendButton(
                    theme = theme,
                    enabled = textState.isNotBlank() || attachedImageUri != null,
                    onClick = {
                        if (textState.isNotBlank() || attachedImageUri != null) {
                            onSend(textState, attachedImageUri)
                            textState = ""
                            attachedImageUri = null
                            textFieldFocusRequester.requestFocus()
                        }
                    }
                )
            }
        }
    }

    actionMenuMessage?.let { message ->
        MessageActionDialog(
            theme = theme,
            onForward = {
                actionMenuMessage = null
                onForwardMessage(message)
            },
            onDelete = {
                actionMenuMessage = null
                pendingDelete = message
            },
            onDismiss = { actionMenuMessage = null }
        )
    }

    pendingDelete?.let { message ->
        ConfirmDialog(
            message = "למחוק את ההודעה?",
            surfaceColor = theme.surfaceColor,
            textColor = theme.textColor,
            dangerColor = theme.dangerColor,
            onCancel = { pendingDelete = null },
            onConfirm = {
                onDeleteMessage(message)
                pendingDelete = null
            },
        )
    }
}

@Composable
private fun AttachButton(theme: FutureTheme, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(if (isFocused) theme.accentColor.copy(alpha = 0.25f) else Color.Transparent, label = "attachBg")
    val tint by animateColorAsState(if (isFocused) theme.accentColor else theme.textColor.copy(alpha = 0.6f), label = "attachTint")

    Box(
        modifier = Modifier
            .padding(bottom = 2.dp)
            .size(44.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus(),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.AttachFile, contentDescription = "צרף תמונה", tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    theme: FutureTheme,
    onClick: () -> Unit
) {
    // עטיפה דקה סביב TopBarIconButton המשותף (מודול SharedKeypadNav) - חתימת
    // הקריאה נשארת זהה כדי שקריאות קיימות ב-Messages לא ישתנו.
    com.future.sharednav.components.TopBarIconButton(icon, contentDescription, theme.textColor, theme.accentColor, onClick)
}

@Composable
private fun AttachmentPreview(uri: Uri, theme: FutureTheme, onRemove: () -> Unit) {
    val bitmap = rememberMmsBitmap(uri)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(56.dp).clip(FutureShapes.md).background(theme.textColor.copy(alpha = 0.1f))
        ) {
            if (bitmap != null) {
                Image(bitmap = bitmap, contentDescription = "תמונה מצורפת", modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text("תמונה מצורפת (MMS)", color = theme.textColor.copy(alpha = 0.7f), fontSize = FutureTypography.summary, modifier = Modifier.weight(1f))
        val interactionSource = remember { MutableInteractionSource() }
        Icon(
            Icons.Rounded.Close,
            contentDescription = "הסר תמונה",
            tint = theme.textColor.copy(alpha = 0.6f),
            modifier = Modifier
                .size(20.dp)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onRemove)
                .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
        )
    }
}

@Composable
private fun SendButton(theme: FutureTheme, enabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(
        when {
            !enabled -> theme.textColor.copy(alpha = 0.1f)
            isFocused -> theme.accentColor
            else -> theme.textColor.copy(alpha = 0.25f)
        },
        label = "sendBg"
    )
    val tint by animateColorAsState(
        if (isFocused && enabled) Color.Black else theme.textColor.copy(alpha = if (enabled) 1f else 0.4f),
        label = "sendTint"
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
        Icon(
            Icons.AutoMirrored.Rounded.Send,
            contentDescription = "שלח",
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun MessageBubble(message: Message, theme: FutureTheme, onClick: () -> Unit, onFocused: () -> Unit = {}) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    LaunchedEffect(isFocused) { if (isFocused) onFocused() }
    val bitmap = message.imageUri?.let { rememberMmsBitmap(it) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        contentAlignment = if (message.isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (message.isFromMe) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (message.isFromMe) 18.dp else 4.dp,
                            bottomEnd = if (message.isFromMe) 4.dp else 18.dp
                        )
                    )
                    .background(
                        if (isFocused) theme.accentColor.copy(alpha = 0.7f)
                        else if (message.isFromMe) theme.accentColor else theme.textColor.copy(alpha = 0.12f)
                    )
                    .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                    .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "תמונה",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(FutureShapes.md)
                        )
                        if (message.text.isNotBlank()) Spacer(modifier = Modifier.height(6.dp))
                    }
                    if (message.text.isNotBlank()) {
                        Text(
                            text = message.text,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = if (message.isFromMe) Color.Black else theme.textColor,
                            fontSize = FutureTypography.body
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                    color = theme.textColor.copy(alpha = 0.4f),
                    fontSize = FutureTypography.caption,
                )
                // סטטוס שליחה אמיתי - משוב אם ההודעה יצאה מהמכשיר בפועל, ולא רק
                // ש"נשלחה" באופן אופטימי (ראו MessageStatus/SmsSentReceiver). צבעים
                // אך ורק מה-theme הקיים - דגני/הצלחה עם accentColor, כישלון עם
                // dangerColor - בלי להמציא צבעים חדשים מחוץ למערכת העיצוב.
                messageStatusLabel(message.status)?.let { (label, isFailure) ->
                    Text(
                        text = label,
                        color = if (isFailure) theme.dangerColor else theme.textColor.copy(alpha = 0.4f),
                        fontSize = FutureTypography.caption
                    )
                }
            }
        }
    }
}

/** תווית לסטטוס שליחה + האם זו כשל (לצביעה) - null להודעות נכנסות (status == null),
 * שאין להן משמעות "האם נשלחה". */
private fun messageStatusLabel(status: com.future.messages.data.MessageStatus?): Pair<String, Boolean>? = when (status) {
    com.future.messages.data.MessageStatus.SENDING -> "שולח..." to false
    com.future.messages.data.MessageStatus.SENT -> "✓ נשלח" to false
    com.future.messages.data.MessageStatus.DELIVERED -> "✓✓ נמסר" to false
    com.future.messages.data.MessageStatus.FAILED -> "⚠ השליחה נכשלה" to true
    null -> null
}

@Composable
private fun MessageActionDialog(theme: FutureTheme, onForward: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val firstRowFocusRequester = remember { FocusRequester() }
    AppDialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(FutureShapes.xl)
                    .background(theme.surfaceColor)
                    .padding(vertical = 8.dp)
            ) {
                ActionDialogRow(theme, icon = Icons.AutoMirrored.Rounded.Send, label = "העבר הודעה", onClick = onForward, focusRequester = firstRowFocusRequester)
                ActionDialogRow(theme, icon = Icons.Rounded.Delete, label = "מחק הודעה", onClick = onDelete, isDestructive = true)
            }
        }
    }
}

@Composable
private fun ActionDialogRow(
    theme: FutureTheme,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    focusRequester: FocusRequester? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(if (isFocused) theme.accentColor.copy(alpha = 0.25f) else Color.Transparent, label = "actionRowBg")
    val contentColor = if (isDestructive) theme.dangerColor else theme.textColor
    // Dialog() מריץ את החלון שלו במעטפת נפרדת - אם מבקשים פוקוס לפני שהחלון
    // בכלל נדבק, הבקשה נבלעת בשקט. onGloballyPositioned מבטיח שהבקשה תקרה
    // ברגע שהשורה באמת נמדדת/מוצגת, בניגוד לדיאלוג הזה שלא ביקש פוקוס בכלל.
    var hasRequestedFocus by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .then(
                if (focusRequester != null) Modifier.onGloballyPositioned {
                    if (!hasRequestedFocus) {
                        hasRequestedFocus = true
                        focusRequester.requestFocus()
                    }
                } else Modifier
            )
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

/** טוען Bitmap מ-content Uri בלי ספריית טעינת תמונות חיצונית - אין כזו תלות בפרויקט. */
@Composable
internal fun rememberMmsBitmap(uri: Uri): androidx.compose.ui.graphics.ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(uri) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            } catch (e: Exception) {
                null
            }
        }
    }
    return bitmap?.asImageBitmap()
}
