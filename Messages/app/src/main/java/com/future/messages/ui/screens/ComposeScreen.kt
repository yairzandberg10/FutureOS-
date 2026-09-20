package com.future.messages.ui.screens

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.messages.data.Contact
import com.future.messages.data.SmsRepository
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** מסך התחלת שיחה חדשה - הזנת שם/מספר טלפון, עם חיפוש חי באנשי הקשר לפי שם
 * (לא רק מספר), וצירוף קובץ עוד לפני שיש thread אמיתי (עובר יחד עם ה-contact
 * שנבחר ישר למסך הצ'אט כטיוטה). */
@Composable
fun ComposeScreen(
    theme: FutureTheme,
    repository: SmsRepository,
    initialImageUri: Uri? = null,
    onCancel: () -> Unit,
    onStart: (Contact, Uri?) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var attachedImageUri by remember { mutableStateOf(initialImageUri) }
    val focusRequester = remember { FocusRequester() }

    // שאילתת ContentResolver לאנשי קשר יכולה לקחת זמן - לא רצים אותה על ה-UI thread
    // בכל הקשה, אחרת הקלדה בשדה יכולה לגמגם.
    var suggestions by remember { mutableStateOf(emptyList<Contact>()) }
    LaunchedEffect(query) {
        suggestions = if (query.isBlank()) {
            emptyList()
        } else {
            withContext(Dispatchers.IO) { repository.searchContacts(query) }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) attachedImageUri = uri
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    fun startWith(contact: Contact) = onStart(contact, attachedImageUri)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        // ראו MessageThreadScreen: edge-to-edge מנטרל את adjustResize, ובלי
        // imePadding שדה החיפוש/הנמען נחבא מתחת למקלדת.
        Column(modifier = Modifier.fillMaxSize().imePadding().background(theme.backgroundColor)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "ביטול", tint = theme.textColor)
                }
                Text("הודעה חדשה", color = theme.textColor, fontWeight = FontWeight.Bold, fontSize = FutureTypography.title, modifier = Modifier.weight(1f))
                IconButton(onClick = { imagePicker.launch("image/*") }) {
                    Icon(Icons.Rounded.AttachFile, contentDescription = "צרף קובץ", tint = theme.textColor)
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.escapeTextFieldFocusTrap().fillMaxWidth().focusRequester(focusRequester),
                    placeholder = { Text("שם איש קשר או מספר טלפון", color = theme.textColor.copy(alpha = 0.4f)) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = theme.textColor,
                        unfocusedTextColor = theme.textColor,
                        focusedBorderColor = theme.accentColor,
                        unfocusedBorderColor = theme.textColor.copy(alpha = 0.3f),
                        cursorColor = theme.accentColor
                    ),
                    shape = FutureShapes.xl
                )

                val currentImageUri = attachedImageUri
                if (currentImageUri != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val bitmap = rememberMmsBitmap(currentImageUri)
                    Box(modifier = Modifier.size(64.dp).clip(FutureShapes.md).background(theme.textColor.copy(alpha = 0.1f))) {
                        if (bitmap != null) {
                            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize())
                        }
                        IconButton(
                            onClick = { attachedImageUri = null },
                            modifier = Modifier.align(Alignment.TopEnd).size(22.dp).background(theme.backgroundColor.copy(alpha = 0.8f), CircleShape)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "הסר צירוף", tint = theme.textColor, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (suggestions.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                        items(suggestions, key = { it.phoneNumber }) { contact ->
                            ContactSuggestionRow(contact = contact, theme = theme, onClick = { startWith(contact) })
                        }
                    }
                } else {
                    Button(
                        onClick = { if (query.isNotBlank()) startWith(repository.resolveContact(query.trim())) },
                        enabled = query.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("המשך")
                    }
                    // מוצג רק כשהוקלד מספר בלי איש קשר תואם (לא כשהוקלד שם שלא
                    // נמצאה לו התאמה) - כדי שאפשר יהיה לשמור אותו לפני שממשיכים לשיחה.
                    if (query.any { it.isDigit() }) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val context = androidx.compose.ui.platform.LocalContext.current
                        OutlinedButton(
                            onClick = {
                                val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
                                    type = android.provider.ContactsContract.Contacts.CONTENT_TYPE
                                    putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, query.trim())
                                }
                                context.startActivity(intent)
                            },
                            enabled = query.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("הוסף לאנשי קשר")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactSuggestionRow(contact: Contact, theme: FutureTheme, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FutureShapes.md)
            .background(if (isFocused) theme.accentColor.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Person, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(contact.name, color = theme.textColor, fontWeight = FontWeight.Medium, fontSize = FutureTypography.bodyLarge)
            Text(contact.phoneNumber, color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.label)
        }
    }
}
