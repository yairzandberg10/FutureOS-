package com.future.messages.ui.screens
import androidx.compose.material.icons.rounded.AttachFile

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.components.TopBarIconButton
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureButtonVariant
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.idleFieldColor
import androidx.compose.foundation.layout.Arrangement

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
        // imePadding שדה החיפוש/הנמען נחבא מתחת למקלדת. background לפני
        // imePadding כדי שהרקע ימלא גם את השטח שמאחורי המקלדת.
        Column(modifier = Modifier.fillMaxSize().background(theme.backgroundColor).imePadding().escapeTextFieldFocusTrap()) {
            ScreenTopBar(
                title = "הודעה חדשה",
                textColor = theme.textColor,
                accentColor = theme.accentColor,
                onBack = onCancel,
                trailingIcon = Icons.Rounded.AttachFile,
                trailingContentDescription = "צרף קובץ",
                onTrailingClick = { imagePicker.launch("image/*") },
            )

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm)) {
                FutureTextField(
                    value = query,
                    onValueChange = { query = it },
                    theme = theme,
                    placeholder = "שם איש קשר או מספר טלפון",
                    focusRequester = focusRequester,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )

                val currentImageUri = attachedImageUri
                if (currentImageUri != null) {
                    Row(
                        modifier = Modifier.padding(top = FutureDimens.spacingMd),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingMd),
                    ) {
                        val bitmap = rememberMmsBitmap(currentImageUri)
                        Box(modifier = Modifier.size(64.dp).clip(FutureShapes.sm).background(theme.idleFieldColor)) {
                            if (bitmap != null) {
                                Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize())
                            }
                        }
                        TopBarIconButton(FutureIcons.Close, "הסר צירוף", theme.textColor, theme.accentColor, { attachedImageUri = null })
                    }
                }

                Spacer(modifier = Modifier.height(FutureDimens.spacingLg))

                if (suggestions.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingXs),
                    ) {
                        items(suggestions, key = { it.phoneNumber }) { contact ->
                            ContactSuggestionRow(contact = contact, theme = theme, onClick = { startWith(contact) })
                        }
                    }
                } else {
                    FutureButton(
                        "המשך",
                        theme,
                        { if (query.isNotBlank()) startWith(repository.resolveContact(query.trim())) },
                        fillMaxWidth = true,
                        enabled = query.isNotBlank(),
                    )
                    // מוצג רק כשהוקלד מספר בלי איש קשר תואם (לא כשהוקלד שם שלא
                    // נמצאה לו התאמה) - כדי שאפשר יהיה לשמור אותו לפני שממשיכים לשיחה.
                    if (query.any { it.isDigit() }) {
                        Spacer(modifier = Modifier.height(FutureDimens.spacingMd))
                        val context = androidx.compose.ui.platform.LocalContext.current
                        FutureButton(
                            "הוסף לאנשי קשר",
                            theme,
                            {
                                val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
                                    type = android.provider.ContactsContract.Contacts.CONTENT_TYPE
                                    putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, query.trim())
                                }
                                context.startActivity(intent)
                            },
                            fillMaxWidth = true,
                            variant = FutureButtonVariant.Secondary,
                            enabled = query.isNotBlank(),
                        )
                    }
                }
            }
        }
    }
}

/** הצעת איש קשר - שורת רשימה של הדיזיין סיסטם, עם אייקון בעיגול בתחילתה. */
@Composable
private fun ContactSuggestionRow(contact: Contact, theme: FutureTheme, onClick: () -> Unit) {
    FutureListItem(
        title = contact.name,
        summary = contact.phoneNumber,
        theme = theme,
        onClick = onClick,
        leading = { FutureAvatar(theme = theme, name = contact.name) },
    )
}
