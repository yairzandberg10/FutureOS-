package com.future.messages.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.future.messages.data.Conversation
import com.future.messages.data.Message
import com.future.messages.ui.theme.FutureTheme
import kotlinx.coroutines.Dispatchers
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
    val textFieldFocusRequester = remember { FocusRequester() }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) attachedImageUri = uri
    }

    LaunchedEffect(Unit) {
        textFieldFocusRequester.requestFocus()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderIconButton(Icons.AutoMirrored.Rounded.ArrowForward, "חזור", theme, onClick = onBack)
                Text(
                    text = conversation.contact.name,
                    color = theme.textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f)
                )
                HeaderIconButton(Icons.Rounded.Call, "התקשר", theme, onClick = onCall)
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                reverseLayout = true,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                items(messages.reversed(), key = { "${it.isMms}_${it.id}" }) { message ->
                    MessageBubble(message, theme, onClick = { actionMenuMessage = message })
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
                    shape = RoundedCornerShape(20.dp)
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
                onDeleteMessage(message)
            },
            onDismiss = { actionMenuMessage = null }
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
            .focusable(interactionSource = interactionSource),
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
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(if (isFocused) theme.accentColor.copy(alpha = 0.25f) else Color.Transparent, label = "headerIconBg")
    val tint by animateColorAsState(if (isFocused) theme.accentColor else theme.textColor, label = "headerIconTint")

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun AttachmentPreview(uri: Uri, theme: FutureTheme, onRemove: () -> Unit) {
    val bitmap = rememberMmsBitmap(uri)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(theme.textColor.copy(alpha = 0.1f))
        ) {
            if (bitmap != null) {
                Image(bitmap = bitmap, contentDescription = "תמונה מצורפת", modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text("תמונה מצורפת (MMS)", color = theme.textColor.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.weight(1f))
        val interactionSource = remember { MutableInteractionSource() }
        Icon(
            Icons.Rounded.Close,
            contentDescription = "הסר תמונה",
            tint = theme.textColor.copy(alpha = 0.6f),
            modifier = Modifier
                .size(20.dp)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onRemove)
                .focusable(interactionSource = interactionSource)
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
            .focusable(interactionSource = interactionSource, enabled = enabled),
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
private fun MessageBubble(message: Message, theme: FutureTheme, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
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
                    .focusable(interactionSource = interactionSource)
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "תמונה",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                        )
                        if (message.text.isNotBlank()) Spacer(modifier = Modifier.height(6.dp))
                    }
                    if (message.text.isNotBlank()) {
                        Text(
                            text = message.text,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = if (message.isFromMe) Color.Black else theme.textColor,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            Text(
                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                color = theme.textColor.copy(alpha = 0.4f),
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun MessageActionDialog(theme: FutureTheme, onForward: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(theme.surfaceColor)
                    .padding(vertical = 8.dp)
            ) {
                ActionDialogRow(theme, icon = Icons.AutoMirrored.Rounded.Send, label = "העבר הודעה", onClick = onForward)
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
    isDestructive: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(if (isFocused) theme.accentColor.copy(alpha = 0.25f) else Color.Transparent, label = "actionRowBg")
    val contentColor = if (isDestructive) Color(0xFFFF453A) else theme.textColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, color = contentColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

/** טוען Bitmap מ-content Uri בלי ספריית טעינת תמונות חיצונית - אין כזו תלות בפרויקט. */
@Composable
private fun rememberMmsBitmap(uri: Uri): androidx.compose.ui.graphics.ImageBitmap? {
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
