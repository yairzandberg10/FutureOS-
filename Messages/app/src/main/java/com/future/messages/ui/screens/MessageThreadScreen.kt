package com.future.messages.ui.screens
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.components.TopBarIconButton
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.idleFieldColor
import com.future.sharednav.theme.secondaryTextColor
import com.future.sharednav.theme.subtleTextColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.onReadableAccentColor
import com.future.sharednav.theme.textAlpha
import androidx.compose.foundation.border
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
        //
        // הסדר כאן חשוב: background *לפני* imePadding. הפוך, הרקע צויר רק
        // באזור שנשאר אחרי הריפוד, והשטח שמאחורי המקלדת - כולל מה שנראה דרך
        // עיגול הפינות שלה - נשאר רקע החלון, שקבוע לשחור ב-themes.xml.
        Column(modifier = Modifier.fillMaxSize().background(theme.backgroundColor).imePadding()) {
            ScreenTopBar(
                title = conversation.contact.name,
                textColor = theme.textColor,
                accentColor = theme.accentColor,
                onBack = onBack,
                trailingIcon = Icons.Rounded.Call,
                trailingContentDescription = "התקשר",
                onTrailingClick = onCall,
            )

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
                contentPadding = PaddingValues(horizontal = FutureDimens.spacingMd, vertical = FutureDimens.spacingSm)
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
                    .padding(horizontal = FutureDimens.spacingMd, vertical = FutureDimens.spacingSm)
                    .escapeTextFieldFocusTrap(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
            ) {
                TopBarIconButton(Icons.Rounded.AttachFile, "צרף תמונה", theme.textColor, theme.accentColor, { imagePicker.launch("image/*") })

                FutureTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    theme = theme,
                    placeholder = "הודעה",
                    singleLine = false,
                    maxLines = 4,
                    focusRequester = textFieldFocusRequester,
                    modifier = Modifier.weight(1f),
                )


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
private fun AttachmentPreview(uri: Uri, theme: FutureTheme, onRemove: () -> Unit) {
    val bitmap = rememberMmsBitmap(uri)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.spacingMd, vertical = FutureDimens.spacingXs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingMd),
    ) {
        Box(
            modifier = Modifier.size(56.dp).clip(FutureShapes.sm).background(theme.idleFieldColor)
        ) {
            if (bitmap != null) {
                Image(bitmap = bitmap, contentDescription = "תמונה מצורפת", modifier = Modifier.fillMaxSize())
            }
        }
        Text("תמונה מצורפת MMS", color = theme.secondaryTextColor, fontSize = FutureTypography.summary, modifier = Modifier.weight(1f))
        TopBarIconButton(Icons.Rounded.Close, "הסר תמונה", theme.textColor, theme.accentColor, onRemove)
    }
}

/** שליחה - כפתור אייקון; בלי טקסט ובלי צירוף הוא לא מקבל פוקוס (אין מה לשלוח). */
@Composable
private fun SendButton(theme: FutureTheme, enabled: Boolean, onClick: () -> Unit) {
    TopBarIconButton(Icons.AutoMirrored.Rounded.Send, "שלח", theme.textColor, theme.accentColor, onClick, enabled = enabled)
}

/**
 * בועת הודעה. שלי = ההדגשה המתוקנת עם הדיו שמתאים לה (היה Color.Black קבוע,
 * בלתי קריא על הדגשה כהה); של הצד השני = 12% מהטקסט. פוקוס = מסגרת 1.5dp של
 * שורת רשימה - קודם הוא סומן רק בהחלשת המילוי ל-70%, שכמעט לא נראתה.
 * הפינות על הסקאלה: 16dp, והפינה ה"זנב" 4dp.
 */
@Composable
private fun MessageBubble(message: Message, theme: FutureTheme, onClick: () -> Unit, onFocused: () -> Unit = {}) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    LaunchedEffect(isFocused) { if (isFocused) onFocused() }
    val bitmap = message.imageUri?.let { rememberMmsBitmap(it) }
    val accent = theme.readableAccentColor
    val fill = if (message.isFromMe) accent else theme.textAlpha(12)
    val ink = if (message.isFromMe) theme.onReadableAccentColor else theme.textColor
    val ring by animateColorAsState(
        if (isFocused) (if (message.isFromMe) theme.textColor else accent) else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "bubbleRing",
    )
    val shape = RoundedCornerShape(
        topStart = FutureShapes.radiusLg,
        topEnd = FutureShapes.radiusLg,
        bottomStart = if (message.isFromMe) FutureShapes.radiusLg else FutureShapes.radiusXs,
        bottomEnd = if (message.isFromMe) FutureShapes.radiusXs else FutureShapes.radiusLg,
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = FutureDimens.spacingXs),
        contentAlignment = if (message.isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (message.isFromMe) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(fill)
                    .border(FutureDimens.focusBorderItem, ring, shape)
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
                            modifier = Modifier.padding(horizontal = FutureDimens.spacingSm, vertical = FutureDimens.spacingXs),
                            color = ink,
                            fontSize = FutureTypography.body
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingXs),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = FutureDimens.spacingXxs)
            ) {
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                    color = theme.subtleTextColor,
                    fontSize = FutureTypography.caption,
                )
                // סטטוס שליחה אמיתי - משוב אם ההודעה יצאה מהמכשיר בפועל, ולא רק
                // ש"נשלחה" באופן אופטימי (ראו MessageStatus/SmsSentReceiver).
                messageStatusLabel(message.status)?.let { (label, isFailure) ->
                    Text(
                        text = label,
                        color = if (isFailure) theme.dangerColor else theme.subtleTextColor,
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
    com.future.messages.data.MessageStatus.SENDING -> "שולח" to false
    com.future.messages.data.MessageStatus.SENT -> "נשלח" to false
    com.future.messages.data.MessageStatus.DELIVERED -> "נמסר" to false
    com.future.messages.data.MessageStatus.FAILED -> "השליחה נכשלה" to true
    null -> null
}

@Composable
private fun MessageActionDialog(theme: FutureTheme, onForward: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    FutureOptionsMenu(theme = theme, onDismissRequest = onDismiss, header = "הודעה") {
        FutureMenuRow("העבר הודעה", Icons.AutoMirrored.Rounded.Send, theme, onForward)
        FutureMenuRow("מחק הודעה", Icons.Rounded.Delete, theme, onDelete, destructive = true)
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
