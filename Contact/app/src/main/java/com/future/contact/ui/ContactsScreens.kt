package com.future.contact.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Person
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.contact.data.Contact
import com.future.contact.data.ContactDetails
import com.future.contact.ui.theme.FutureTheme
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
    onToggleFavorite: (Contact) -> Unit = {}
) {
    val context = LocalContext.current
    var menuFor by remember { mutableStateOf<Contact?>(null) }
    var pendingDelete by remember { mutableStateOf<Contact?>(null) }

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
    val firstRowFocusRequester = remember { FocusRequester() }
    var initialFocusRequested by remember { mutableStateOf(false) }
    LaunchedEffect(hasPermission, contacts.isEmpty()) {
        if (!initialFocusRequested) {
            if (hasPermission && contacts.isNotEmpty()) {
                firstRowFocusRequester.requestFocus()
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
                    Text("אנשי קשר", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = theme.textColor)
                    FocusableIconButton(
                        icon = Icons.Rounded.PersonAdd,
                        theme = theme,
                        onClick = onAddContact,
                        focusRequester = addContactFocusRequester
                    )
                }

                if (t9Query.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(theme.textColor.copy(alpha = 0.1f))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(t9Query, color = theme.textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${filteredContacts.size} תוצאות", color = theme.textColor.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                }

                when {
                    !hasPermission -> {
                        PermissionRequiredMessage(theme = theme, onRequestPermission = onRequestPermission)
                    }
                    contacts.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("אין אנשי קשר עדיין", color = theme.textColor.copy(alpha = 0.5f), fontSize = 15.sp)
                        }
                    }
                    filteredContacts.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("לא נמצאו אנשי קשר תואמים", color = theme.textColor.copy(alpha = 0.5f), fontSize = 15.sp)
                        }
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(filteredContacts, key = { _, contact -> contact.id }) { index, contact ->
                                ContactRow(
                                    contact,
                                    theme = theme,
                                    onClick = { onContactClick(contact) },
                                    onMenu = { menuFor = contact },
                                    focusRequester = if (index == 0) firstRowFocusRequester else null
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
            fontSize = 15.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused by interactionSource.collectIsFocusedAsState()
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (isFocused) theme.accentColor else theme.accentColor.copy(alpha = 0.7f))
                .clickable(interactionSource = interactionSource, indication = null, onClick = onRequestPermission)
                .focusable(interactionSource = interactionSource)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("אשר הרשאה", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ContactRow(
    contact: Contact,
    theme: FutureTheme,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    focusRequester: FocusRequester? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)
    val bgColor by animateColorAsState(
        if (isFocused) theme.textColor.copy(alpha = 0.16f) else theme.textColor.copy(alpha = 0.06f),
        label = "rowBg"
    )
    val scale by animateFloatAsState(if (isFocused) 1.02f else 1f, label = "rowScale")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(bgColor)
            .then(if (isFocused) Modifier.border(2.dp, theme.accentColor, shape) else Modifier)
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { event ->
                if (isFocused && event.type == KeyEventType.KeyUp && (event.key == Key.Menu || event.key == Key.Settings)) {
                    onMenu()
                    true
                } else false
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(theme.textColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(contact.name.take(1).uppercase(), color = theme.textColor, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(contact.name, color = theme.textColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            if (contact.phoneNumbers.isNotEmpty()) {
                Text(contact.phoneNumbers.first(), color = theme.textColor.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        }
        if (contact.isFavorite) {
            Icon(Icons.Rounded.Star, contentDescription = "מועדף", tint = Color(0xFFFFC107), modifier = Modifier.size(18.dp))
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
                    icon = Icons.AutoMirrored.Rounded.ArrowForward,
                    theme = theme,
                    onClick = onBack,
                    focusRequester = backFocusRequester
                )
                Spacer(modifier = Modifier.weight(1f))
                FocusableIconButton(icon = Icons.Rounded.Edit, theme = theme, onClick = { isEditing = true })
            }
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(88.dp).clip(CircleShape).background(theme.textColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = theme.textColor, modifier = Modifier.size(44.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(contact.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = theme.textColor)
                Spacer(modifier = Modifier.height(24.dp))

                contact.phoneNumbers.forEach { number ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(number, color = theme.textColor.copy(alpha = 0.8f), fontSize = 15.sp)
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
        Text(text, color = theme.textColor.copy(alpha = 0.85f), fontSize = 14.sp)
    }
}

@Composable
private fun ContactEditDetailsDialog(initial: ContactDetails, theme: FutureTheme, onDismiss: () -> Unit, onSave: (ContactDetails) -> Unit) {
    var email by remember { mutableStateOf(initial.email) }
    var organization by remember { mutableStateOf(initial.organization) }
    var jobTitle by remember { mutableStateOf(initial.jobTitle) }
    var address by remember { mutableStateOf(initial.address) }
    var notes by remember { mutableStateOf(initial.notes) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(theme.surfaceColor)
                .padding(20.dp)
        ) {
            Text("עריכת פרטים", color = theme.textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            EditField("אימייל", email, theme) { email = it }
            EditField("ארגון", organization, theme) { organization = it }
            EditField("תפקיד", jobTitle, theme) { jobTitle = it }
            EditField("כתובת", address, theme) { address = it }
            EditField("הערות", notes, theme) { notes = it }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EditDialogButton("ביטול", theme.textColor.copy(alpha = 0.15f), theme.textColor, onClick = onDismiss)
                EditDialogButton("שמור", theme.accentColor, Color.Black, onClick = {
                    onSave(ContactDetails(email, organization, jobTitle, address, notes))
                })
            }
        }
    }
}

@Composable
private fun EditField(label: String, value: String, theme: FutureTheme, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, color = theme.textColor.copy(alpha = 0.5f), fontSize = 11.sp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = theme.textColor, fontSize = 14.sp),
            cursorBrush = SolidColor(theme.accentColor),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.textColor.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                .padding(10.dp)
        )
    }
}

@Composable
private fun EditDialogButton(text: String, bg: Color, fg: Color, focusRequester: FocusRequester? = null, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(if (isFocused) bg else bg.copy(alpha = bg.alpha * 0.7f), label = "editDialogBtnBg")
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(text, color = fg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

/** דיאלוג אישור לפני מחיקת איש קשר - מחיקה היא פעולה בלתי הפיכה, ולפני
 * התיקון היא הייתה מתבצעת מיידית מתפריט האפשרויות בלי אף שלב אישור. ברירת
 * המחדל לפוקוס D-pad היא "ביטול", כדי שלחיצה בטעות על מרכז המקלדת לא תמחק. */
@Composable
private fun DeleteConfirmationDialog(contactName: String, theme: FutureTheme, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val cancelFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { cancelFocusRequester.requestFocus() }

    Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(theme.surfaceColor)
                .padding(20.dp)
        ) {
            Text("מחיקת איש קשר", color = theme.textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "האם למחוק לצמיתות את \"$contactName\"?",
                color = theme.textColor.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EditDialogButton(
                    "ביטול",
                    theme.textColor.copy(alpha = 0.15f),
                    theme.textColor,
                    focusRequester = cancelFocusRequester,
                    onClick = onCancel
                )
                EditDialogButton("מחק", Color(0xFFFF6B6B), Color.White, onClick = onConfirm)
            }
        }
    }
}

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
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(theme.surfaceColor)
                .padding(vertical = 8.dp)
        ) {
            Text(
                contact.name,
                color = theme.textColor.copy(alpha = 0.5f),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            MenuOptionRow(
                if (contact.isFavorite) "הסר ממועדפים" else "הוסף למועדפים",
                if (contact.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                theme = theme,
                onClick = onToggleFavorite
            )
            MenuOptionRow("ערוך איש קשר", Icons.Rounded.Edit, theme = theme, onClick = onEdit)
            MenuOptionRow("מחק איש קשר", Icons.Rounded.Delete, theme = theme, onClick = onDelete, isDestructive = true)
        }
    }
}

@Composable
private fun MenuOptionRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, theme: FutureTheme, onClick: () -> Unit, isDestructive: Boolean = false) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(if (isFocused) theme.textColor.copy(alpha = 0.12f) else Color.Transparent, label = "menuRowBg")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (isDestructive) Color(0xFFFF6B6B) else theme.textColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, color = if (isDestructive) Color(0xFFFF6B6B) else theme.textColor, fontSize = 15.sp)
    }
}

@Composable
fun FocusableIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    theme: FutureTheme,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (isFocused) 1.15f else 1f, label = "iconScale")
    val bgColor by animateColorAsState(
        if (isFocused) theme.accentColor.copy(alpha = 0.3f) else theme.textColor.copy(alpha = 0.08f),
        label = "iconBg"
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .graphicsLayerScale(scale)
            .clip(CircleShape)
            .background(bgColor)
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = theme.textColor, modifier = Modifier.size(20.dp))
    }
}

private fun Modifier.graphicsLayerScale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
)
