package com.future.messages.ui.screens
import com.future.sharednav.systemui.StatusBarInset

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.future.messages.data.Contact
import com.future.messages.data.NumberActions
import com.future.messages.data.SmsRepository
import com.future.messages.ui.components.rememberGalleryImagePicker
import com.future.sharednav.components.EmptyState
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.components.TopBarIconButton
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.idleFieldColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * מסך "הודעה חדשה". הפעולה העיקרית היא הקלדה בשדה "אל": שם - והתוצאות
 * מאנשי הקשר מופיעות מתחתיו; מספר - וגם הוא מחפש, ובראש הרשימה שורה של
 * המספר עצמו. OK על השורה (או מקש Options) פותח את הפעולות על המספר:
 * הודעה, חיוג, הוספה לאנשי קשר, מועדפים וחסימה. OK על איש קשר פותח שיחה.
 * צירוף תמונה אפשרי כבר כאן, ועובר עם הנמען לשיחה כטיוטה.
 */
@Composable
fun ComposeScreen(
    theme: FutureTheme,
    repository: SmsRepository,
    initialImageUri: Uri? = null,
    onCancel: () -> Unit,
    onStart: (Contact, Uri?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var attachedImageUri by remember { mutableStateOf(initialImageUri) }
    val focusRequester = remember { FocusRequester() }
    var menuNumber by remember { mutableStateOf<String?>(null) }
    var focusedNumber by remember { mutableStateOf<String?>(null) }
    var pendingBlock by remember { mutableStateOf<String?>(null) }
    val pendingFavorite = remember { mutableStateOf<String?>(null) }

    // שאילתת ContentResolver לאנשי קשר יכולה לקחת זמן - לא רצים אותה על ה-UI thread
    // בכל הקשה, אחרת הקלדה בשדה יכולה לגמגם.
    var suggestions by remember { mutableStateOf(emptyList<Contact>()) }
    LaunchedEffect(query) {
        suggestions = if (query.isBlank()) {
            emptyList()
        } else {
            withContext(Dispatchers.IO) { repository.searchContacts(query.trim()) }
        }
    }
    val typedNumber = query.trim().takeIf { NumberActions.looksLikePhoneNumber(it) }

    val imagePicker = rememberGalleryImagePicker { uri -> attachedImageUri = uri }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // Options על שורה - אותו תפריט פעולות כמו OK על שורת המספר.
    com.future.sharednav.nav.onOptionsKeyPress { focusedNumber?.let { menuNumber = it } }

    fun startWith(contact: Contact) = onStart(contact, attachedImageUri)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.backgroundColor)
                .imePadding()
                .escapeTextFieldFocusTrap()
                // מעט אוויר מעל הכותרת - בלי זה המסך נצמד לקצה העליון.
                .padding(top = StatusBarInset.TITLE_GAP_DP.dp),
        ) {
            ScreenTopBar(
                title = "הודעה חדשה",
                textColor = theme.textColor,
                accentColor = theme.accentColor,
                onBack = onCancel,
                trailingIcon = FutureIcons.AttachFile,
                trailingContentDescription = "צרף תמונה",
                onTrailingClick = imagePicker,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FutureDimens.screenPadding)
                    .padding(top = FutureDimens.spacingMd),
            ) {
                Text(
                    "למי לשלוח?",
                    color = theme.textColor,
                    fontSize = FutureTypography.title,
                    fontWeight = FutureTypography.weightMedium,
                    modifier = Modifier.padding(start = FutureDimens.spacingXs, bottom = FutureDimens.spacingSm),
                )
                FutureTextField(
                    value = query,
                    onValueChange = { query = it },
                    theme = theme,
                    placeholder = "הקלידו שם או מספר",
                    focusRequester = focusRequester,
                    leading = { FutureAvatar(theme = theme, icon = FutureIcons.Search, size = 28.dp) },
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
            }

            Spacer(modifier = Modifier.height(FutureDimens.spacingMd))

            when {
                query.isBlank() -> EmptyState(
                    icon = FutureIcons.Contacts,
                    title = "חיפוש נמען",
                    subtitle = "התחילו להקליד שם של איש קשר או מספר טלפון",
                    textColor = theme.textColor,
                )
                typedNumber == null && suggestions.isEmpty() -> EmptyState(
                    icon = FutureIcons.SearchOff,
                    title = "לא נמצאו אנשי קשר",
                    subtitle = "אפשר להקליד מספר טלפון במקום",
                    textColor = theme.textColor,
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(horizontal = FutureDimens.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingXs),
                ) {
                    if (typedNumber != null) {
                        item(key = "number") {
                            FutureListItem(
                                title = typedNumber,
                                summary = "OK לפעולות: הודעה, חיוג, שמירה, חסימה",
                                theme = theme,
                                onClick = { menuNumber = typedNumber },
                                leading = { FutureAvatar(theme = theme, icon = FutureIcons.Dialpad) },
                                modifier = Modifier.onFocusChanged {
                                    if (it.isFocused) focusedNumber = typedNumber
                                    else if (focusedNumber == typedNumber) focusedNumber = null
                                },
                            )
                        }
                    }
                    if (suggestions.isNotEmpty()) {
                        item(key = "header") { FutureSectionHeader("אנשי קשר", theme, inset = false) }
                    }
                    items(suggestions, key = { it.phoneNumber }) { contact ->
                        FutureListItem(
                            title = contact.name,
                            summary = contact.phoneNumber,
                            theme = theme,
                            onClick = { startWith(contact) },
                            leading = { FutureAvatar(theme = theme, name = contact.name) },
                            modifier = Modifier.onFocusChanged {
                                if (it.isFocused) focusedNumber = contact.phoneNumber
                                else if (focusedNumber == contact.phoneNumber) focusedNumber = null
                            },
                        )
                    }
                }
            }
        }
    }

    val writeContactsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val number = pendingFavorite.value
        pendingFavorite.value = null
        if (granted && number != null) {
            scope.launch { addFavorite(context, number) }
        } else if (!granted) {
            Toast.makeText(context, "צריך הרשאה לאנשי הקשר", Toast.LENGTH_SHORT).show()
        }
    }

    menuNumber?.let { number ->
        NumberActionsMenu(
            number = number,
            theme = theme,
            onDismiss = { menuNumber = null },
            onMessage = {
                menuNumber = null
                startWith(repository.resolveContact(number))
            },
            onCall = {
                menuNumber = null
                val canCall = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                val action = if (canCall) Intent.ACTION_CALL else Intent.ACTION_DIAL
                runCatching { context.startActivity(Intent(action, Uri.fromParts("tel", number, null))) }
            },
            onAddContact = {
                menuNumber = null
                val intent = Intent(Intent.ACTION_INSERT).apply {
                    type = android.provider.ContactsContract.Contacts.CONTENT_TYPE
                    putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, number)
                }
                runCatching { context.startActivity(intent) }
                    .onFailure { Toast.makeText(context, "אפליקציית אנשי הקשר לא זמינה", Toast.LENGTH_SHORT).show() }
            },
            onFavorite = {
                menuNumber = null
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    scope.launch { addFavorite(context, number) }
                } else {
                    pendingFavorite.value = number
                    writeContactsLauncher.launch(Manifest.permission.WRITE_CONTACTS)
                }
            },
            onBlock = {
                menuNumber = null
                pendingBlock = number
            },
        )
    }

    // שורה הרסנית בתפריט עוברת דרך אישור (DS).
    pendingBlock?.let { number ->
        com.future.sharednav.components.ConfirmDialog(
            message = "לחסום את $number?",
            surfaceColor = theme.surfaceColor,
            textColor = theme.textColor,
            dangerColor = theme.dangerColor,
            onCancel = { pendingBlock = null },
            confirmLabel = "חסום",
            onConfirm = {
                pendingBlock = null
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { NumberActions.block(context, number) }
                    Toast.makeText(context, if (ok) "המספר נחסם" else "החסימה נכשלה", Toast.LENGTH_SHORT).show()
                }
            },
        )
    }
}

private suspend fun addFavorite(context: android.content.Context, number: String) {
    val ok = withContext(Dispatchers.IO) { NumberActions.addToFavorites(context, number) }
    Toast.makeText(context, if (ok) "נוסף למועדפים" else "ההוספה למועדפים נכשלה", Toast.LENGTH_SHORT).show()
}

@Composable
private fun NumberActionsMenu(
    number: String,
    theme: FutureTheme,
    onDismiss: () -> Unit,
    onMessage: () -> Unit,
    onCall: () -> Unit,
    onAddContact: () -> Unit,
    onFavorite: () -> Unit,
    onBlock: () -> Unit,
) {
    FutureOptionsMenu(theme = theme, onDismissRequest = onDismiss, header = number) {
        FutureMenuRow("שליחת הודעה", FutureIcons.Send, theme, onMessage)
        FutureMenuRow("חיוג", FutureIcons.Call, theme, onCall)
        FutureMenuRow("הוספה לאנשי קשר", FutureIcons.PersonAdd, theme, onAddContact)
        FutureMenuRow("הוספה למועדפים", FutureIcons.Star, theme, onFavorite)
        FutureMenuRow("חסימה", FutureIcons.Lock, theme, onBlock, destructive = true)
    }
}
