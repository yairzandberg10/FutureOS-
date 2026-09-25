package com.future.contact.ui

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.future.contact.data.Contact
import com.future.contact.data.ContactDetails
import com.future.contact.data.ContactsRepository
import com.future.contact.util.T9Search
import com.future.sharednav.components.ConfirmDialog
import com.future.sharednav.components.EmptyState
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureButtonVariant
import com.future.sharednav.components.FutureCard
import com.future.sharednav.components.FutureDialog
import com.future.sharednav.components.FutureDivider
import com.future.sharednav.components.FutureFormField
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureSettingItem
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.components.TopBarIconButton
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.nav.digitForKey
import com.future.sharednav.nav.onOptionsKeyPress
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTransitions
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.favoriteColor
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.rememberFutureType
import com.future.sharednav.theme.subtleTextColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** שלוש הלשוניות בסרגל התחתון, בסדר RTL (הראשונה מימין). */
enum class ContactsTab(val title: String) { FAVORITES("מועדפים"), CONTACTS("אנשי קשר"), BLOCKED("חסומים") }

/** פעולות על איש קשר - MainActivity מממש (Intent-ים, הרשאות, בוחר תמונות). */
class ContactActions(
    val call: (String) -> Unit,
    val message: (String) -> Unit,
    val share: (Contact) -> Unit,
    val pickPhoto: (Contact) -> Unit,
    val removePhoto: (Contact) -> Unit,
    val toggleFavorite: (Contact) -> Unit,
    val toggleBlocked: (Contact) -> Unit,
    val delete: (Contact) -> Unit,
    val add: (name: String, number: String) -> Unit,
    val cycleSort: () -> Unit,
)

/**
 * רשימת אנשי הקשר של לשונית אחת: כותרת עם כפתור חיפוש שנפתח לשדה, רשימה
 * (T9 מהספרות גם בלי לפתוח את השדה), ומקש Options לפעולות.
 */
@Composable
fun ContactsListScreen(
    tab: ContactsTab,
    contacts: List<Contact>,
    hasPermission: Boolean,
    sortLabel: String,
    theme: FutureTheme,
    actions: ContactActions,
    onRequestPermission: () -> Unit,
    onContactClick: (Contact) -> Unit,
    lastSelectedContactId: String?,
) {
    var searchOpen by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var t9Query by remember { mutableStateOf("") }
    var focusedContact by remember { mutableStateOf<Contact?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Contact?>(null) }
    var adding by remember { mutableStateOf(false) }

    onOptionsKeyPress { if (pendingDelete == null && !adding) menuOpen = !menuOpen }
    BackHandler(enabled = searchOpen || t9Query.isNotEmpty()) {
        searchOpen = false
        searchText = ""
        t9Query = ""
    }
    LaunchedEffect(t9Query) {
        if (t9Query.isNotEmpty()) {
            delay(2500)
            t9Query = ""
        }
    }

    val inTab = remember(contacts, tab) {
        when (tab) {
            ContactsTab.FAVORITES -> contacts.filter { it.isFavorite && !it.isBlocked }
            ContactsTab.CONTACTS -> contacts.filter { !it.isBlocked }
            ContactsTab.BLOCKED -> contacts.filter { it.isBlocked }
        }
    }
    val shown = remember(inTab, searchText, t9Query) {
        val text = searchText.trim()
        inTab.filter { c ->
            (text.isEmpty() || c.name.contains(text, ignoreCase = true) || c.phoneNumbers.any { it.filter(Char::isDigit).contains(text.filter(Char::isDigit).ifEmpty { "\u0000" }) }) &&
                (t9Query.isEmpty() || T9Search.matchesAnyWord(c.name, t9Query) || c.phoneNumbers.any { it.filter(Char::isDigit).contains(t9Query) })
        }
    }

    val rowFocus = remember { mutableMapOf<String, FocusRequester>() }
    fun focusFor(id: String) = rowFocus.getOrPut(id) { FocusRequester() }
    val searchButtonFocus = remember { FocusRequester() }
    LaunchedEffect(hasPermission, tab, inTab.isEmpty()) {
        val target = inTab.firstOrNull { it.id == lastSelectedContactId } ?: inTab.firstOrNull()
        runCatching { if (target != null) focusFor(target.id).requestFocus() else searchButtonFocus.requestFocus() }
    }

    val type = rememberFutureType()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.backgroundColor)
                .onKeyEvent { event ->
                    if (searchOpen || event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    digitForKey(event.key)?.let {
                        t9Query += it
                        return@onKeyEvent true
                    }
                    if ((event.key == Key.Backspace || event.key == Key.Delete) && t9Query.isNotEmpty()) {
                        t9Query = t9Query.dropLast(1)
                        true
                    } else false
                }
        ) {
            // כותרת: שם הלשונית וכפתור חיפוש, שנפתח לשדה חיפוש ברוחב מלא.
            AnimatedContent(targetState = searchOpen, transitionSpec = { FutureTransitions.appear() }, label = "searchHeader") { open ->
                if (open) {
                    FutureTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        theme = theme,
                        placeholder = "חיפוש ב${tab.title}",
                        autoFocus = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.spacingLg, vertical = FutureDimens.spacingMd).escapeTextFieldFocusTrap(),
                        leading = { Icon(FutureIcons.Search, contentDescription = null, tint = theme.mutedTextColor, modifier = Modifier.size(FutureDimens.iconMenuRow)) },
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.spacingLg, vertical = FutureDimens.spacingMd),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(tab.title, fontSize = type.screenTitle, fontWeight = FutureTypography.weightBold, color = theme.textColor, modifier = Modifier.weight(1f))
                        TopBarIconButton(FutureIcons.Search, "חיפוש", theme.textColor, theme.accentColor, { searchOpen = true }, focusRequester = searchButtonFocus)
                    }
                }
            }
            if (t9Query.isNotEmpty() && !searchOpen) {
                Text(
                    "$t9Query · ${shown.size} תוצאות",
                    color = theme.mutedTextColor,
                    fontSize = type.summary,
                    modifier = Modifier.padding(horizontal = FutureDimens.spacingXl, vertical = FutureDimens.spacingXs),
                )
            }

            when {
                !hasPermission -> Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("כדי להציג אנשי קשר צריך לאשר הרשאה", color = theme.mutedTextColor, fontSize = type.bodyLarge, textAlign = TextAlign.Center)
                    FutureButton("אשר הרשאה", theme, onRequestPermission, modifier = Modifier.padding(top = 16.dp))
                }
                inTab.isEmpty() -> EmptyState(
                    icon = when (tab) {
                        ContactsTab.FAVORITES -> FutureIcons.Star
                        ContactsTab.CONTACTS -> FutureIcons.Person
                        ContactsTab.BLOCKED -> FutureIcons.Block
                    },
                    title = when (tab) {
                        ContactsTab.FAVORITES -> "אין מועדפים"
                        ContactsTab.CONTACTS -> "אין אנשי קשר"
                        ContactsTab.BLOCKED -> "אין אנשי קשר חסומים"
                    },
                    subtitle = if (tab == ContactsTab.CONTACTS) "לחץ על מקש התפריט כדי להוסיף" else null,
                    textColor = theme.textColor,
                )
                shown.isEmpty() -> EmptyState(icon = FutureIcons.SearchOff, title = "לא נמצאו תוצאות", textColor = theme.textColor)
                else -> LazyColumn(contentPadding = PaddingValues(horizontal = FutureDimens.spacingMd, vertical = FutureDimens.spacingXs)) {
                    itemsIndexed(shown, key = { _, c -> c.id }) { _, contact ->
                        FutureListItem(
                            title = contact.name,
                            summary = contact.phoneNumbers.firstOrNull(),
                            theme = theme,
                            onClick = { onContactClick(contact) },
                            focusRequester = focusFor(contact.id),
                            modifier = Modifier.onFocusChanged { if (it.isFocused) focusedContact = contact },
                            leading = { FutureAvatar(theme = theme, name = contact.name, photoUri = contact.photoUri) },
                            trailing = {
                                when {
                                    contact.isBlocked -> Icon(FutureIcons.Block, contentDescription = "חסום", tint = theme.dangerColor, modifier = Modifier.size(FutureDimens.iconMenuRow))
                                    contact.isFavorite && tab != ContactsTab.FAVORITES -> Icon(FutureIcons.Star, contentDescription = "מועדף", tint = theme.favoriteColor, modifier = Modifier.size(FutureDimens.iconMenuRow))
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (menuOpen) {
        val target = focusedContact?.takeIf { f -> shown.any { it.id == f.id } }
        FutureOptionsMenu(theme = theme, onDismissRequest = { menuOpen = false }, header = target?.name ?: tab.title) {
            fun pick(action: () -> Unit): () -> Unit = { menuOpen = false; action() }
            if (target != null) {
                target.phoneNumbers.firstOrNull()?.let { number ->
                    FutureMenuRow("התקשר", FutureIcons.Call, theme, pick { actions.call(number) })
                    FutureMenuRow("שלח הודעה", FutureIcons.AutoMirrored.Chat, theme, pick { actions.message(number) })
                }
                ContactMenuRows(target, theme, actions, onDelete = { pendingDelete = target }, pick = ::pick)
            }
            FutureMenuRow("איש קשר חדש", FutureIcons.PersonAdd, theme, pick { adding = true })
            FutureMenuRow("מיון · $sortLabel", FutureIcons.AutoMirrored.Sort, theme, pick(actions.cycleSort))
            FutureMenuRow("חיפוש", FutureIcons.Search, theme, pick { searchOpen = true })
        }
    }
    pendingDelete?.let { contact ->
        ConfirmDialog(message = "למחוק את ${contact.name}?", theme = theme, confirmLabel = "מחק", onCancel = { pendingDelete = null }, onConfirm = {
            pendingDelete = null
            actions.delete(contact)
        })
    }
    if (adding) {
        AddContactDialog(theme = theme, onDismiss = { adding = false }, onSave = { name, number ->
            adding = false
            actions.add(name, number)
        })
    }
}

/** השורות של תפריט איש קשר שמשותפות לרשימה ולכרטיס. */
@Composable
private fun ContactMenuRows(contact: Contact, theme: FutureTheme, actions: ContactActions, onDelete: () -> Unit, pick: (() -> Unit) -> () -> Unit) {
    FutureMenuRow(
        if (contact.isFavorite) "הסר ממועדפים" else "הוסף למועדפים",
        if (contact.isFavorite) FutureIcons.Star else FutureIcons.StarBorder,
        theme,
        pick { actions.toggleFavorite(contact) },
    )
    FutureMenuRow("שתף איש קשר", FutureIcons.Share, theme, pick { actions.share(contact) })
    FutureMenuRow(if (contact.photoUri == null) "הגדר תמונה" else "החלף תמונה", FutureIcons.AddAPhoto, theme, pick { actions.pickPhoto(contact) })
    FutureMenuRow(if (contact.isBlocked) "בטל חסימה" else "חסום", FutureIcons.Block, theme, pick { actions.toggleBlocked(contact) }, destructive = !contact.isBlocked)
    FutureMenuRow("מחק איש קשר", FutureIcons.Delete, theme, pick(onDelete), destructive = true)
}

/**
 * כרטיס איש קשר - אותו מבנה של כרטיס איש הקשר בחייגן (ui_kits/calls):
 * אווטאר גדול (עם התמונה), שם ומספר; כרטיס פעולות; פרטים; וכרטיס ניהול.
 */
@Composable
fun ContactDetailScreen(contact: Contact, theme: FutureTheme, actions: ContactActions, onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { ContactsRepository(context) }
    val scope = rememberCoroutineScope()
    val type = rememberFutureType()
    var details by remember(contact.id) { mutableStateOf(ContactDetails()) }
    var editing by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf(false) }
    LaunchedEffect(contact.id) { details = withContext(Dispatchers.IO) { repository.getContactDetails(contact.id) } }
    onOptionsKeyPress { if (!editing && !pendingDelete) menuOpen = !menuOpen }

    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    val primary = contact.phoneNumbers.firstOrNull()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            ScreenTopBar(title = "איש קשר", textColor = theme.textColor, accentColor = theme.accentColor, onBack = onBack)
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = FutureDimens.spacingLg)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = FutureDimens.spacingMd, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
                ) {
                    FutureAvatar(theme = theme, name = contact.name, size = 88.dp, photoUri = contact.photoUri)
                    Text(contact.name, color = theme.textColor, fontSize = type.screenTitle, fontWeight = FutureTypography.weightBold, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = FutureDimens.screenPadding))
                    if (primary != null) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(primary, color = theme.mutedTextColor, fontSize = type.body)
                        }
                    }
                    if (contact.isBlocked) Text("חסום", color = theme.dangerColor, fontSize = type.summary)
                }

                FutureCard(theme = theme) {
                    if (contact.phoneNumbers.isEmpty()) {
                        FutureSettingItem(title = "אין מספר טלפון", theme = theme, showChevron = false, onClick = null)
                    }
                    contact.phoneNumbers.forEachIndexed { index, number ->
                        if (index > 0) FutureDivider(theme = theme)
                        FutureSettingItem(
                            title = if (contact.phoneNumbers.size > 1) "התקשר · $number" else "התקשר",
                            icon = FutureIcons.Call,
                            theme = theme,
                            showChevron = false,
                            focusRequester = if (index == 0) first else null,
                            onClick = { actions.call(number) },
                        )
                    }
                    if (primary != null) {
                        FutureDivider(theme = theme)
                        FutureSettingItem(title = "שלח הודעה", icon = FutureIcons.AutoMirrored.Chat, theme = theme, showChevron = false, onClick = { actions.message(primary) })
                    }
                    FutureDivider(theme = theme)
                    FutureSettingItem(
                        title = if (contact.isFavorite) "הסר ממועדפים" else "הוסף למועדפים",
                        icon = if (contact.isFavorite) FutureIcons.Star else FutureIcons.StarBorder,
                        theme = theme,
                        showChevron = false,
                        focusRequester = if (contact.phoneNumbers.isEmpty()) first else null,
                        onClick = { actions.toggleFavorite(contact) },
                    )
                }

                val info = listOfNotNull(
                    details.email.takeIf { it.isNotBlank() }?.let { FutureIcons.Email to it },
                    listOf(details.jobTitle, details.organization).filter { it.isNotBlank() }.joinToString(" · ").takeIf { it.isNotBlank() }?.let { FutureIcons.Business to it },
                    details.address.takeIf { it.isNotBlank() }?.let { FutureIcons.LocationOn to it },
                    details.notes.takeIf { it.isNotBlank() }?.let { Icons.AutoMirrored.Rounded.Notes to it },
                )
                FutureSectionHeader("פרטים", theme)
                FutureCard(theme = theme) {
                    info.forEachIndexed { index, (icon, text) ->
                        if (index > 0) FutureDivider(theme = theme)
                        FutureSettingItem(title = text, icon = icon, theme = theme, showChevron = false, onClick = null)
                    }
                    if (info.isNotEmpty()) FutureDivider(theme = theme)
                    FutureSettingItem(title = "ערוך פרטים", icon = FutureIcons.Edit, theme = theme, onClick = { editing = true })
                }

                FutureSectionHeader("ניהול", theme)
                FutureCard(theme = theme) {
                    FutureSettingItem(title = "שתף איש קשר", icon = FutureIcons.Share, theme = theme, showChevron = false, onClick = { actions.share(contact) })
                    FutureDivider(theme = theme)
                    FutureSettingItem(
                        title = if (contact.photoUri == null) "הגדר תמונה" else "החלף תמונה",
                        icon = FutureIcons.AddAPhoto,
                        theme = theme,
                        onClick = { actions.pickPhoto(contact) },
                    )
                    if (contact.photoUri != null) {
                        FutureDivider(theme = theme)
                        FutureSettingItem(title = "הסר תמונה", icon = FutureIcons.NoPhotography, theme = theme, showChevron = false, onClick = { actions.removePhoto(contact) })
                    }
                    FutureDivider(theme = theme)
                    FutureSettingItem(
                        title = if (contact.isBlocked) "בטל חסימה" else "חסום",
                        summary = if (contact.isBlocked) "שיחות והודעות ממנו חסומות" else null,
                        icon = FutureIcons.Block,
                        theme = theme,
                        showChevron = false,
                        onClick = { actions.toggleBlocked(contact) },
                    )
                    FutureDivider(theme = theme)
                    FutureSettingItem(title = "מחק איש קשר", icon = FutureIcons.Delete, theme = theme, showChevron = false, onClick = { pendingDelete = true })
                }
            }
        }
    }

    if (menuOpen) {
        FutureOptionsMenu(theme = theme, onDismissRequest = { menuOpen = false }, header = contact.name) {
            fun pick(action: () -> Unit): () -> Unit = { menuOpen = false; action() }
            ContactMenuRows(contact, theme, actions, onDelete = { pendingDelete = true }, pick = ::pick)
            FutureMenuRow("ערוך פרטים", FutureIcons.Edit, theme, pick { editing = true })
        }
    }
    if (pendingDelete) {
        ConfirmDialog(message = "למחוק את ${contact.name}?", theme = theme, confirmLabel = "מחק", onCancel = { pendingDelete = false }, onConfirm = {
            pendingDelete = false
            actions.delete(contact)
        })
    }
    if (editing) {
        ContactEditDetailsDialog(initial = details, theme = theme, onDismiss = { editing = false }, onSave = { updated ->
            editing = false
            scope.launch {
                val ok = withContext(Dispatchers.IO) { repository.updateContactDetails(contact.id, updated) }
                if (ok) details = updated
            }
        })
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
            FutureFormField("אימייל", email, { email = it }, theme, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
            FutureFormField("ארגון", organization, { organization = it }, theme, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
            FutureFormField("תפקיד", jobTitle, { jobTitle = it }, theme, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
            FutureFormField("כתובת", address, { address = it }, theme, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
            FutureFormField("הערות", notes, { notes = it }, theme, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
        }
    }
}

/** איש קשר חדש, בתוך האפליקציה - עורך אנשי הקשר של המערכת מושבת במכשיר. */
@Composable
fun AddContactDialog(theme: FutureTheme, initialNumber: String = "", onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf(initialNumber) }
    FutureDialog(
        theme = theme,
        onDismissRequest = onDismiss,
        title = "איש קשר חדש",
        buttons = {
            FutureButton("ביטול", theme, onDismiss, variant = FutureButtonVariant.Secondary)
            FutureButton("שמור", theme, { if (name.isNotBlank()) onSave(name.trim(), number.trim()) }, enabled = name.isNotBlank())
        },
    ) {
        Column(modifier = Modifier.escapeTextFieldFocusTrap()) {
            FutureFormField("שם", name, { name = it }, theme, autoFocus = true, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
            FutureFormField(
                "טלפון",
                number,
                { number = it },
                theme,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            )
        }
    }
}
