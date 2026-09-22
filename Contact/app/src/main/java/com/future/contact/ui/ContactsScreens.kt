package com.future.contact.ui
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.FutureDialog
import com.future.sharednav.components.FutureButtonVariant
import com.future.sharednav.components.FutureFormField
import com.future.sharednav.components.ConfirmDialog
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.theme.secondaryTextColor
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.idleChipColor
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.focus.focusMotion
import com.future.sharednav.components.FutureButton
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.focus.bringIntoViewOnFocus

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import com.future.sharednav.components.AppDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.contact.data.Contact
import com.future.contact.data.ContactDetails
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.nav.digitForKey
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.favoriteColor
import com.future.contact.util.T9Search
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ContactsListScreen(
    contacts: List<Contact>,
    hasPermission: Boolean,
    theme: FutureTheme,
    onRequestPermission: () -> Unit,
    onContactClick: (Contact) -> Unit,
    onAddContact: () -> Unit,
    onEditContact: (Contact) -> Unit = {},
    onDeleteContact: (Contact) -> Unit = {},
    onToggleFavorite: (Contact) -> Unit = {},
    // איש הקשר שממנו נכנסו למסך הפרטים לאחרונה - כשחוזרים "אחורה" הפוקוס
    // צריך לשוב לשורה הזו בדיוק, לא תמיד לשורה הראשונה ברשימה.
    lastSelectedContactId: String? = null,
) {
    val context = LocalContext.current
    var menuFor by remember { mutableStateOf<Contact?>(null) }
    var pendingDelete by remember { mutableStateOf<Contact?>(null) }
    // גישה חלופית לתפריט מחיקה/מועדפים (זהה בדיוק לפתרון של Files.FilesScreen)
    // עבור מכשירים בלי מקש Menu/Settings ייעודי - בלעדיה התפריט נגיש רק דרך
    // מקש חומרה ספציפי שאולי לא קיים במכשיר בפועל.
    var focusedContact by remember { mutableStateOf<Contact?>(null) }

    // מקש Options הפיזי נחסם ברמת המערכת ולא מגיע כ-Key.Menu לאפליקציה -
    // זו הדרך האמיתית שהוא פותח את תפריט הפעולות של איש הקשר הממוקד.
    com.future.sharednav.nav.onOptionsKeyPress { if (focusedContact != null) menuFor = focusedContact }

    // T9: מקשי הספרות הפיזיים בונים רצף שמסנן חי את רשימת אנשי הקשר לפי
    // תחילת שם פרטי/משפחה (ראה T9Search) - זו התכונה הכי בסיסית שחסרה
    // באפליקציית אנשי קשר שמבוססת מקלדת T9 בלבד. הרצף מתאפס אוטומטית אחרי
    // הפסקה קצרה בהקלדה, בדיוק כמו שמתבצע חיפוש T9 בשאר הסוויטה.
    var t9Query by remember { mutableStateOf("") }
    LaunchedEffect(t9Query) {
        if (t9Query.isNotEmpty()) {
            delay(2000)
            t9Query = ""
        }
    }
    val filteredContacts = remember(contacts, t9Query) {
        if (t9Query.isEmpty()) contacts else contacts.filter { T9Search.matchesAnyWord(it.name, t9Query) }
    }

    menuFor?.let { contact ->
        ContactOptionsMenu(
            contact = contact,
            theme = theme,
            onDismiss = { menuFor = null },
            onEdit = { onEditContact(contact); menuFor = null },
            onDelete = { pendingDelete = contact; menuFor = null },
            onToggleFavorite = { onToggleFavorite(contact); menuFor = null }
        )
    }

    pendingDelete?.let { contact ->
        DeleteConfirmationDialog(
            contactName = contact.name,
            theme = theme,
            onConfirm = { onDeleteContact(contact); pendingDelete = null },
            onCancel = { pendingDelete = null }
        )
    }

    val addContactFocusRequester = remember { FocusRequester() }
    // FocusRequester לפי מזהה איש קשר (לא רק לשורה הראשונה) - כדי שאפשר יהיה
    // למקד בחזרה בדיוק את השורה שממנה נכנסו למסך הפרטים.
    val rowFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    var initialFocusRequested by remember { mutableStateOf(false) }
    LaunchedEffect(hasPermission, contacts.isEmpty()) {
        if (!initialFocusRequested) {
            if (hasPermission && contacts.isNotEmpty()) {
                val target = filteredContacts.firstOrNull { it.id == lastSelectedContactId } ?: filteredContacts.firstOrNull()
                target?.let { rowFocusRequesters.getOrPut(it.id) { FocusRequester() }.requestFocus() }
                initialFocusRequested = true
            } else if (!hasPermission) {
                // אין עדיין רשימה שאפשר למקד אליה - נמקד את כפתור ההוספה כברירת
                // מחדל, כדי שהמסך לא יישאר בלי שום פוקוס D-pad.
                addContactFocusRequester.requestFocus()
                initialFocusRequested = true
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.backgroundColor)
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    if (pendingDelete != null || menuFor != null) return@onKeyEvent false
                    val digit = digitForKey(event.key)
                    if (digit != null) {
                        t9Query += digit
                        return@onKeyEvent true
                    }
                    if ((event.key == Key.Backspace || event.key == Key.Delete) && t9Query.isNotEmpty()) {
                        t9Query = t9Query.dropLast(1)
                        return@onKeyEvent true
                    }
                    false
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("אנשי קשר", fontSize = FutureTypography.headline, fontWeight = FontWeight.Bold, color = theme.textColor)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (focusedContact != null) {
                            FocusableIconButton(
                                icon = Icons.Rounded.MoreVert,
                                theme = theme,
                                onClick = { menuFor = focusedContact }
                            )
                        }
                        FocusableIconButton(
                            icon = Icons.Rounded.PersonAdd,
                            theme = theme,
                            onClick = onAddContact,
                            focusRequester = addContactFocusRequester
                        )
                    }
                }

                if (t9Query.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 8.dp)
                            .clip(FutureShapes.md)
                            .background(theme.textColor.copy(alpha = 0.1f))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(t9Query, color = theme.textColor, fontWeight = FontWeight.Bold, fontSize = FutureTypography.bodyLarge)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${filteredContacts.size} תוצאות", color = theme.mutedTextColor, fontSize = FutureTypography.label)
                    }
                }

                when {
                    !hasPermission -> {
                        PermissionRequiredMessage(theme = theme, onRequestPermission = onRequestPermission)
                    }
                    contacts.isEmpty() -> {
                        com.future.sharednav.components.EmptyState(
                            icon = Icons.Rounded.Person,
                            title = "אין אנשי קשר עדיין",
                            subtitle = "הוסיפו איש קשר עם כפתור ההוספה",
                            textColor = theme.textColor,
                        )
                    }
                    filteredContacts.isEmpty() -> {
                        com.future.sharednav.components.EmptyState(
                            icon = Icons.Rounded.Person,
                            title = "לא נמצאו אנשי קשר תואמים",
                            textColor = theme.textColor,
                        )
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(filteredContacts, key = { _, contact -> contact.id }) { _, contact ->
                                ContactRow(
                                    contact,
                                    theme = theme,
                                    onClick = { onContactClick(contact) },
                                    onMenu = { menuFor = contact },
                                    focusRequester = rowFocusRequesters.getOrPut(contact.id) { FocusRequester() },
                                    onFocused = { focusedContact = contact }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRequiredMessage(theme: FutureTheme, onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "כדי להציג אנשי קשר צריך לאשר הרשאה",
            color = theme.textColor.copy(alpha = 0.7f),
            fontSize = FutureTypography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        FutureButton("אשר הרשאה", theme, onRequestPermission)
    }
}

@Composable
private fun ContactRow(
    contact: Contact,
    theme: FutureTheme,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    focusRequester: FocusRequester? = null,
    onFocused: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    LaunchedEffect(isFocused) { if (isFocused) onFocused() }
    val shape = FutureShapes.sm
    val bgColor by animateColorAsState(
        if (isFocused) theme.readableAccentColor.copy(alpha = 0.14f) else theme.idleChipColor,
        FutureMotion.focusColorSpec,
        label = "rowBg"
    )
    val scale by animateFloatAsState(if (isFocused) 1.02f else 1f, label = "rowScale")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(bgColor)
            .then(if (isFocused) Modifier.border(FutureDimens.focusBorderItem, theme.readableAccentColor, shape) else Modifier)
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
            .onKeyEvent { event ->
                if (isFocused && event.type == KeyEventType.KeyUp && (event.key == Key.Menu || event.key == Key.Settings)) {
                    onMenu()
                    true
                } else false
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FutureAvatar(theme = theme, name = contact.name)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(contact.name, color = theme.textColor, fontWeight = FontWeight.SemiBold, fontSize = FutureTypography.bodyLarge)
            if (contact.phoneNumbers.isNotEmpty()) {
                Text(contact.phoneNumbers.first(), color = theme.mutedTextColor, fontSize = FutureTypography.label)
            }
        }
        if (contact.isFavorite) {
            Icon(Icons.Rounded.Star, contentDescription = "מועדף", tint = theme.favoriteColor, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun ContactDetailScreen(contact: Contact, theme: FutureTheme, onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { com.future.contact.data.ContactsRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    var details by remember(contact.id) { mutableStateOf(ContactDetails()) }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(contact.id) {
        details = withContext(kotlinx.coroutines.Dispatchers.IO) { repository.getContactDetails(contact.id) }
    }

    val backFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { backFocusRequester.requestFocus() }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FocusableIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    theme = theme,
                    onClick = onBack,
                    focusRequester = backFocusRequester
                )
                Spacer(modifier = Modifier.weight(1f))
                FocusableIconButton(icon = Icons.Rounded.Edit, theme = theme, onClick = { isEditing = true })
            }
            val detailsScrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(detailsScrollState)
                    // בלי זה, איש קשר בלי מספרי טלפון (רק אימייל/ארגון/כתובת/הערות)
                    // לא מכיל אף רכיב פוקוסבילי באזור הגלילה - ואין דרך במקלדת לגלול
                    // אליו אם התוכן חורג מגובה המסך.
                    .focusable().bringIntoViewOnFocus()
                    .onKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                        val step = 300f
                        when (event.key) {
                            Key.DirectionDown -> {
                                coroutineScope.launch { detailsScrollState.animateScrollBy(step) }
                                true
                            }
                            Key.DirectionUp -> {
                                coroutineScope.launch { detailsScrollState.animateScrollBy(-step) }
                                true
                            }
                            else -> false
                        }
                    }
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FutureAvatar(theme = theme, name = contact.name, size = 88.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(contact.name, fontSize = FutureTypography.headline, fontWeight = FontWeight.Bold, color = theme.textColor)
                Spacer(modifier = Modifier.height(24.dp))

                contact.phoneNumbers.forEach { number ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(number, color = theme.secondaryTextColor, fontSize = FutureTypography.bodyLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FocusableIconButton(icon = Icons.AutoMirrored.Rounded.Message, theme = theme, onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number"))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            })
                            FocusableIconButton(icon = Icons.Rounded.Call, theme = theme, onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            })
                        }
                    }
                }

                if (details.email.isNotBlank() || details.organization.isNotBlank() || details.address.isNotBlank() || details.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    if (details.email.isNotBlank()) DetailInfoRow(Icons.Rounded.Email, details.email, theme)
                    if (details.organization.isNotBlank() || details.jobTitle.isNotBlank()) {
                        DetailInfoRow(
                            Icons.Rounded.Business,
                            listOf(details.jobTitle, details.organization).filter { it.isNotBlank() }.joinToString(" · "),
                            theme
                        )
                    }
                    if (details.address.isNotBlank()) DetailInfoRow(Icons.Rounded.LocationOn, details.address, theme)
                    if (details.notes.isNotBlank()) DetailInfoRow(Icons.Rounded.Notes, details.notes, theme)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (isEditing) {
        ContactEditDetailsDialog(
            initial = details,
            theme = theme,
            onDismiss = { isEditing = false },
            onSave = { updated ->
                isEditing = false
                // מעדכנים את המסך רק אם הכתיבה בפועל הצליחה - לפני התיקון
                // ה-UI היה קופץ לערכים החדשים באופן אופטימי גם כשהכתיבה
                // נכשלה בשקט (חסרת הרשאת WRITE_CONTACTS למשל), והמשתמש היה
                // חושב שהשמירה הצליחה בעוד שהנתונים הישנים בלבד נשארו בפועל.
                coroutineScope.launch {
                    val success = withContext(kotlinx.coroutines.Dispatchers.IO) {
                        repository.updateContactDetails(contact.id, updated)
                    }
                    if (success) {
                        details = updated
                    } else {
                        android.widget.Toast.makeText(context, "לא ניתן לשמור — נדרשת הרשאה", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
private fun DetailInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, theme: FutureTheme) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = theme.textColor.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = theme.textColor, fontSize = FutureTypography.body)
    }
}

@Composable
private fun ContactEditDetailsDialog(initial: ContactDetails, theme: FutureTheme, onDismiss: () -> Unit, onSave: (ContactDetails) -> Unit) {
    var email by remember { mutableStateOf(initial.email) }
    var organization by remember { mutableStateOf(initial.organization) }
    var jobTitle by remember { mutableStateOf(initial.jobTitle) }
    var address by remember { mutableStateOf(initial.address) }
    var notes by remember { mutableStateOf(initial.notes) }

    FutureDialog(
        theme = theme,
        onDismissRequest = onDismiss,
        title = "עריכת פרטים",
        buttons = {
            FutureButton("ביטול", theme, onDismiss, variant = FutureButtonVariant.Secondary)
            FutureButton("שמור", theme, { onSave(ContactDetails(email, organization, jobTitle, address, notes)) })
        },
    ) {
        Column(modifier = Modifier.escapeTextFieldFocusTrap()) {
            EditField("אימייל", email, theme) { email = it }
            EditField("ארגון", organization, theme) { organization = it }
            EditField("תפקיד", jobTitle, theme) { jobTitle = it }
            EditField("כתובת", address, theme) { address = it }
            EditField("הערות", notes, theme) { notes = it }
        }
    }
}

@Composable
private fun EditField(label: String, value: String, theme: FutureTheme, onValueChange: (String) -> Unit) {
    FutureFormField(label, value, onValueChange, theme, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
}



/** דיאלוג אישור לפני מחיקת איש קשר - מחיקה היא פעולה בלתי הפיכה, ולפני
 * התיקון היא הייתה מתבצעת מיידית מתפריט האפשרויות בלי אף שלב אישור. ברירת
 * המחדל לפוקוס D-pad היא "ביטול", כדי שלחיצה בטעות על מרכז המקלדת לא תמחק. */
/** דיאלוג אישור לפני מחיקת איש קשר - ConfirmDialog של הדיזיין סיסטם; השאלה
 * בנוסח שלו ("למחוק את X?"), והתשובות בפעלים. */
@Composable
private fun DeleteConfirmationDialog(contactName: String, theme: FutureTheme, onConfirm: () -> Unit, onCancel: () -> Unit) {
    ConfirmDialog(message = "למחוק את \"$contactName\"?", theme = theme, onCancel = onCancel, onConfirm = onConfirm)
}

/** תפריט אפשרויות - נפתח בלחיצה על מקש Options כשאיש קשר בפוקוס. */
/** תפריט אפשרויות - נפתח בלחיצה על מקש Options כשאיש קשר בפוקוס. */
@Composable
private fun ContactOptionsMenu(
    contact: Contact,
    theme: FutureTheme,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    FutureOptionsMenu(theme = theme, onDismissRequest = onDismiss, header = contact.name) {
        FutureMenuRow(
            if (contact.isFavorite) "הסר ממועדפים" else "הוסף למועדפים",
            if (contact.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            theme,
            onToggleFavorite,
        )
        FutureMenuRow("ערוך איש קשר", Icons.Rounded.Edit, theme, onEdit)
        FutureMenuRow("מחק איש קשר", Icons.Rounded.Delete, theme, onDelete, destructive = true)
    }
}



@Composable
fun FocusableIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    theme: FutureTheme,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null
) {
    com.future.sharednav.components.TopBarIconButton(icon, "", theme.textColor, theme.accentColor, onClick, focusRequester)
}
