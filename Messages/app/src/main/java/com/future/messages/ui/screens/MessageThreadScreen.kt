package com.future.messages.ui.screens
import com.future.sharednav.systemui.StatusBarInset
import androidx.compose.material.icons.rounded.ErrorOutline

import com.future.sharednav.icons.FutureIcons
import com.future.messages.ui.components.MessageComposeBar
import androidx.compose.ui.unit.em
import com.future.sharednav.theme.mutedTextColor
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

    val imagePicker = com.future.messages.ui.components.rememberGalleryImagePicker { uri -> attachedImageUri = uri }

    // הכתבה - המיקרופון שבתוך שורת הכתיבה (templates/message-compose), דרך
    // התמלול המקומי של Assistant. בלי Assistant במכשיר הכפתור לא מוצג בכלל.
    val context = LocalContext.current
    val dictation = com.future.messages.ui.components.rememberLocalDictation { spoken ->
        textState = if (textState.isBlank()) spoken else "$textState $spoken"
        textFieldFocusRequester.requestFocus()
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
        Column(modifier = Modifier.fillMaxSize().background(theme.backgroundColor).imePadding().padding(top = StatusBarInset.TITLE_GAP_DP.dp)) {
            ScreenTopBar(
                title = conversation.contact.name,
                textColor = theme.textColor,
                accentColor = theme.accentColor,
                onBack = onBack,
                trailingIcon = FutureIcons.Call,
                trailingContentDescription = "התקשר",
                onTrailingClick = onCall,
            )
            // "מקליד..." - מגיע רק מצ'אט FutureOS, כשהצד השני כותב עכשיו.
            val typingMap by com.future.messages.chat.FutureChat.typing.collectAsState()
            val peerTyping = remember(typingMap) {
                com.future.messages.chat.FutureChat.isTyping(context, conversation.contact.phoneNumber)
            }
            if (peerTyping) {
                Text(
                    "מקליד…",
                    color = theme.readableAccentColor,
                    fontSize = FutureTypography.caption,
                    fontWeight = FutureTypography.weightMedium,
                    modifier = Modifier.padding(horizontal = FutureDimens.spacingLg),
                )
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
                contentPadding = PaddingValues(start = FutureDimens.screenPadding, end = FutureDimens.screenPadding, top = FutureDimens.spacingMd, bottom = FutureDimens.spacingXs)
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

            MessageComposeBar(
                theme = theme,
                recipient = conversation.contact.name,
                text = textState,
                onTextChange = {
                    textState = it
                    if (it.isNotEmpty()) com.future.messages.chat.FutureChat.onTyping(context, conversation.contact.phoneNumber)
                },
                canSend = textState.isNotBlank() || attachedImageUri != null,
                onSend = {
                    if (textState.isNotBlank() || attachedImageUri != null) {
                        onSend(textState, attachedImageUri)
                        textState = ""
                        attachedImageUri = null
                        textFieldFocusRequester.requestFocus()
                    }
                },
                onAttach = imagePicker,
                onDictate = if (dictation.available) dictation::toggle else null,
                fieldFocusRequester = textFieldFocusRequester,
                modifier = Modifier.escapeTextFieldFocusTrap(),
                dictationListening = dictation.listening,
                dictationProcessing = dictation.processing,
            )
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
            // פתק ששותף מ-FuturePhone אחר מגיע כהודעה - נשמר באפליקציית הפתקים.
            onSaveToNotes = if (message.text.isNotBlank()) {
                {
                    actionMenuMessage = null
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
                        .setType("text/plain")
                        .setPackage("com.future.notes")
                        .putExtra(android.content.Intent.EXTRA_TEXT, message.text)
                    runCatching { context.startActivity(intent) }.onFailure {
                        android.widget.Toast.makeText(context, "אפליקציית הפתקים לא מותקנת", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } else null,
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
        TopBarIconButton(FutureIcons.Close, "הסר תמונה", theme.textColor, theme.accentColor, onRemove)
    }
}

/**
 * בועת הודעה. שלי = ההדגשה המתוקנת עם הדיו שמתאים לה (היה Color.Black קבוע,
 * בלתי קריא על הדגשה כהה); של הצד השני = 12% מהטקסט. פוקוס = מסגרת 1.5dp של
 * שורת רשימה - קודם הוא סומן רק בהחלשת המילוי ל-70%, שכמעט לא נראתה.
 * הפינות על הסקאלה: 16dp, והפינה ה"זנב" 4dp.
 */
/**
 * בועת הודעה (templates/message-compose): של הצד השני על המשטח, בצד ימין;
 * שלי בהדגשה המתוקנת עם הדיו שמתאים לה, בצד שמאל. פינות 20dp, והפינה של
 * ה"זנב" - הפינה התחתונה שפונה לדובר - 6dp. רוחב עד 220dp, טקסט 15sp.
 * פוקוס = מסגרת 1.5dp של שורת רשימה.
 *
 * מתחת לבועה: שלי - סטטוס השליחה עם אייקון ("שולח", "נשלח · 14:03", "לא
 * נשלח" באדום); של הצד השני - השעה.
 */
@Composable
private fun MessageBubble(message: Message, theme: FutureTheme, onClick: () -> Unit, onFocused: () -> Unit = {}) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    LaunchedEffect(isFocused) { if (isFocused) onFocused() }
    val bitmap = message.imageUri?.let { rememberMmsBitmap(it) }
    val accent = theme.readableAccentColor
    val fill = if (message.isFromMe) accent else theme.surfaceColor
    val ink = if (message.isFromMe) theme.onReadableAccentColor else theme.textColor
    val ring by animateColorAsState(
        if (isFocused) (if (message.isFromMe) theme.textColor else accent) else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "bubbleRing",
    )
    // RTL: start = ימין. ההודעה של הצד השני יושבת בימין והזנב שלה בפינה
    // הימנית-תחתונה (bottomStart); שלי בשמאל, והזנב בפינה השמאלית (bottomEnd).
    val shape = RoundedCornerShape(
        topStart = BubbleRadius,
        topEnd = BubbleRadius,
        bottomStart = if (message.isFromMe) BubbleRadius else BubbleTailRadius,
        bottomEnd = if (message.isFromMe) BubbleTailRadius else BubbleRadius,
    )
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = BubbleGap / 2),
        horizontalAlignment = if (message.isFromMe) Alignment.End else Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = BubbleMaxWidth)
                .clip(shape)
                .background(fill)
                .border(FutureDimens.focusBorderItem, ring, shape)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Column {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "תמונה",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(FutureShapes.md)
                    )
                    if (message.text.isNotBlank()) Spacer(modifier = Modifier.height(FutureDimens.spacingSm))
                }
                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        color = ink,
                        fontSize = FutureTypography.dialog,
                        lineHeight = 1.35.em,
                    )
                }
            }
        }
        MessageMeta(message, time, theme)
    }
}

/**
 * השורה מתחת לבועה - 11sp בינוני, אייקון 13dp. הצבע נושא את הסטטוס: 60%
 * בזמן שליחה, הדגשה כשיצאה, אדום כשנכשלה.
 */
@Composable
private fun MessageMeta(message: Message, time: String, theme: FutureTheme) {
    val status = message.status
    // הודעה שעברה בצ'אט (FutureOS או RCS) מסומנת בסיומת - כדי שיהיה ברור מתי זה לא SMS.
    val via = when {
        message.isChat -> " · צ'אט"
        message.isRcs -> " · RCS"
        else -> ""
    }
    val (icon, label, color) = when (status) {
        null -> Triple(null, "$time$via", theme.subtleTextColor)
        com.future.messages.data.MessageStatus.SENDING -> Triple(FutureIcons.Schedule, "שולח", theme.mutedTextColor)
        com.future.messages.data.MessageStatus.SENT -> Triple(FutureIcons.Check, "נשלח · $time$via", theme.readableAccentColor)
        com.future.messages.data.MessageStatus.DELIVERED -> Triple(FutureIcons.Check, "נמסר · $time$via", theme.readableAccentColor)
        com.future.messages.data.MessageStatus.READ -> Triple(FutureIcons.Visibility, "נקרא · $time$via", theme.readableAccentColor)
        com.future.messages.data.MessageStatus.FAILED -> Triple(FutureIcons.Error, "לא נשלח", theme.dangerColor)
    }
    Row(
        modifier = Modifier.padding(horizontal = 3.dp, vertical = FutureDimens.spacingXs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        }
        Text(label, color = color, fontSize = FutureTypography.caption, fontWeight = FutureTypography.weightMedium)
    }
}

/** 40px / 12px / 440px / 20px - הפינות, פינת הזנב, הרוחב והמרווח של הבועות בתבנית. */
private val BubbleRadius = 20.dp
private val BubbleTailRadius = 6.dp
private val BubbleMaxWidth = 220.dp
private val BubbleGap = 10.dp

@Composable
private fun MessageActionDialog(
    theme: FutureTheme,
    onForward: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    onSaveToNotes: (() -> Unit)? = null,
) {
    FutureOptionsMenu(theme = theme, onDismissRequest = onDismiss, header = "הודעה") {
        FutureMenuRow("העבר הודעה", FutureIcons.AutoMirrored.Send, theme, onForward)
        if (onSaveToNotes != null) FutureMenuRow("שמירה בפתקים", FutureIcons.Description, theme, onSaveToNotes)
        FutureMenuRow("מחק הודעה", FutureIcons.Delete, theme, onDelete, destructive = true)
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
