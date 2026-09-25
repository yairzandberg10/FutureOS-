package com.future.contact
import com.future.sharednav.systemui.StatusBarInset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import com.future.contact.data.Contact
import com.future.contact.data.ContactSort
import com.future.contact.data.ContactsRepository
import com.future.contact.ui.ContactActions
import com.future.contact.ui.ContactDetailScreen
import com.future.contact.ui.ContactsListScreen
import com.future.contact.ui.ContactsTab
import com.future.sharednav.components.AnimatedScreenHost
import com.future.sharednav.components.FutureBottomNav
import com.future.sharednav.components.FutureNavItem
import com.future.sharednav.components.FutureSnackbarHost
import com.future.sharednav.components.rememberFutureSnackbarState
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.share.FutureShare
import com.future.sharednav.theme.FutureMaterialTheme
import com.future.sharednav.theme.rememberFutureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    // המכשיר הוא מקלדת בלי מסך מגע - מבטלים קלט מגע, המקשים עוברים בנתיב נפרד.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val repository = remember { ContactsRepository(this) }
            val theme = rememberFutureTheme()
            val scope = rememberCoroutineScope()
            val snackbar = rememberFutureSnackbarState()
            val prefs = remember { getSharedPreferences("contacts_ui", MODE_PRIVATE) }

            var hasPermission by remember { mutableStateOf(repository.hasContactsPermission()) }
            var sort by remember {
                mutableStateOf(ContactSort.entries.firstOrNull { it.name == prefs.getString("sort", null) } ?: ContactSort.FIRST_NAME)
            }
            var contacts by remember { mutableStateOf(listOf<Contact>()) }
            var tab by remember { mutableStateOf(ContactsTab.CONTACTS) }
            var selectedId by remember { mutableStateOf<String?>(null) }
            var lastSelectedContactId by remember { mutableStateOf<String?>(null) }
            var photoTarget by remember { mutableStateOf<Contact?>(null) }
            var pendingCall by remember { mutableStateOf<String?>(null) }
            // "הוסף לאנשי קשר" מהחייגן/מההודעות (ACTION_INSERT) - עורך אנשי הקשר של
            // המערכת מושבת במכשיר, אז הוא נפתח כאן כדיאלוג עם המספר מוכן.
            var insertNumber by remember {
                mutableStateOf(intent?.takeIf { it.action == Intent.ACTION_INSERT }?.let {
                    it.getStringExtra(ContactsContract.Intents.Insert.PHONE).orEmpty()
                })
            }

            fun reload() {
                if (!hasPermission) return
                scope.launch { contacts = withContext(Dispatchers.IO) { repository.getAllContacts(sort) } }
            }

            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
                hasPermission = results[Manifest.permission.READ_CONTACTS] == true || repository.hasContactsPermission()
                reload()
            }
            val callPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                pendingCall?.let { number -> placeCall(number, direct = granted) }
                pendingCall = null
            }
            // בחירת תמונה מהגלריה של FutureOS (PICK), ואם אין - מכל בורר תמונות.
            val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val uri = result.data?.data
                val contact = photoTarget
                photoTarget = null
                if (uri != null && contact != null) {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) { repository.setPhoto(contact.id, uri) }
                        snackbar.show(if (ok) "התמונה עודכנה" else "לא ניתן לשמור את התמונה")
                        reload()
                    }
                }
            }

            LaunchedEffect(hasPermission, sort) { reload() }

            val selected = contacts.firstOrNull { it.id == selectedId }
            BackHandler(enabled = selectedId != null) { selectedId = null }

            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) reload()
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val actions = remember {
                ContactActions(
                    call = { number ->
                        if (checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                            placeCall(number, direct = true)
                        } else {
                            pendingCall = number
                            callPermission.launch(Manifest.permission.CALL_PHONE)
                        }
                    },
                    message = { number ->
                        runCatching {
                            startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }.onFailure { snackbar.show("אין אפליקציית הודעות") }
                    },
                    share = { contact ->
                        val vcard = repository.vcardUri(contact)
                        val send = if (vcard != null) {
                            Intent(Intent.ACTION_SEND)
                                .setType(ContactsContract.Contacts.CONTENT_VCARD_TYPE)
                                .putExtra(Intent.EXTRA_STREAM, vcard)
                                .putExtra(Intent.EXTRA_SUBJECT, contact.name)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        } else {
                            Intent(Intent.ACTION_SEND).setType("text/plain")
                                .putExtra(Intent.EXTRA_TEXT, (listOf(contact.name) + contact.phoneNumbers).joinToString("\n"))
                        }
                        FutureShare.open(this@MainActivity, send, "שיתוף איש קשר")
                    },
                    pickPhoto = { contact ->
                        photoTarget = contact
                        val gallery = Intent(Intent.ACTION_PICK).setType("image/*").setPackage("com.future.gallery")
                        val any = Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
                        val intent = if (gallery.resolveActivity(packageManager) != null) gallery else any
                        runCatching { photoPicker.launch(intent) }.onFailure {
                            photoTarget = null
                            snackbar.show("אין אפליקציה לבחירת תמונה")
                        }
                    },
                    removePhoto = { contact ->
                        scope.launch {
                            withContext(Dispatchers.IO) { repository.removePhoto(contact.id) }
                            reload()
                        }
                    },
                    toggleFavorite = { contact ->
                        scope.launch {
                            withContext(Dispatchers.IO) { repository.setFavorite(contact.id, !contact.isFavorite) }
                            reload()
                        }
                    },
                    toggleBlocked = { contact ->
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) { repository.setBlocked(contact, !contact.isBlocked) }
                            snackbar.show(
                                when {
                                    !ok -> "לא ניתן לשנות את החסימה"
                                    contact.isBlocked -> "החסימה בוטלה"
                                    else -> "${contact.name} נחסם"
                                }
                            )
                            reload()
                        }
                    },
                    delete = { contact ->
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) { repository.deleteContact(contact.id) }
                            if (ok) {
                                if (selectedId == contact.id) selectedId = null
                                snackbar.show("איש הקשר נמחק")
                            } else {
                                snackbar.show("לא ניתן למחוק את איש הקשר")
                            }
                            reload()
                        }
                    },
                    add = { name, number ->
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) { repository.addContact(name, number) }
                            snackbar.show(if (ok) "$name נוסף" else "לא ניתן לשמור")
                            reload()
                        }
                    },
                    cycleSort = {
                        sort = ContactSort.entries[(sort.ordinal + 1) % ContactSort.entries.size]
                        prefs.edit().putString("sort", sort.name).apply()
                        snackbar.show("ממוין לפי ${sort.label}")
                    },
                )
            }

            FutureMaterialTheme(theme) {
                Surface(modifier = Modifier.fillMaxSize(), color = theme.backgroundColor) {
                    // ריווח קטן - הכותרות (אנשי קשר/אני/איש קשר) ישבו מתחת לשורת המצב.
                    Box(modifier = Modifier.fillMaxSize().padding(top = StatusBarInset.TITLE_GAP_DP.dp)) {
                        // איש הקשר נמסר כמצב: הכרטיס שיוצא באנימציה ממשיך לצייר את שלו.
                        AnimatedScreenHost(
                            targetState = selected,
                            depthOf = { if (it == null) 0 else 1 },
                            contentKey = { it?.id },
                        ) { current ->
                            if (current != null) {
                                ContactDetailScreen(contact = current, theme = theme, actions = actions, onBack = { selectedId = null })
                            } else {
                                ContactsHome(
                                    tab = tab,
                                    onTab = { tab = it },
                                    contacts = contacts,
                                    hasPermission = hasPermission,
                                    sortLabel = sort.label,
                                    theme = theme,
                                    actions = actions,
                                    onRequestPermission = {
                                        permissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS))
                                    },
                                    onContactClick = { selectedId = it.id; lastSelectedContactId = it.id },
                                    lastSelectedContactId = lastSelectedContactId,
                                )
                            }
                        }
                        FutureSnackbarHost(snackbar, theme)
                    }
                    insertNumber?.let { number ->
                        com.future.contact.ui.AddContactDialog(
                            theme = theme,
                            initialNumber = number,
                            onDismiss = { insertNumber = null },
                            onSave = { name, phone ->
                                insertNumber = null
                                actions.add(name, phone)
                            },
                        )
                    }
                }
            }
        }
    }

    private fun placeCall(number: String, direct: Boolean) {
        val action = if (direct) Intent.ACTION_CALL else Intent.ACTION_DIAL
        runCatching { startActivity(Intent(action, Uri.parse("tel:$number")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

/** הלשונית הנבחרת + הסרגל התחתון. ימינה/שמאלה מחליפים לשונית כשאין לאן לזוז בתוכה. */
@androidx.compose.runtime.Composable
private fun ContactsHome(
    tab: ContactsTab,
    onTab: (ContactsTab) -> Unit,
    contacts: List<Contact>,
    hasPermission: Boolean,
    sortLabel: String,
    theme: com.future.sharednav.theme.FutureTheme,
    actions: ContactActions,
    onRequestPermission: () -> Unit,
    onContactClick: (Contact) -> Unit,
    lastSelectedContactId: String?,
) {
    val focusManager = LocalFocusManager.current
    val tabs = ContactsTab.entries
    Column(
        modifier = Modifier.fillMaxSize().onKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
            val direction = when (event.key) {
                Key.DirectionRight -> FocusDirection.Right
                Key.DirectionLeft -> FocusDirection.Left
                else -> return@onKeyEvent false
            }
            if (focusManager.moveFocus(direction)) return@onKeyEvent true
            // RTL: ימינה - הלשונית הקודמת, שמאלה - הבאה.
            val next = tab.ordinal + if (event.key == Key.DirectionRight) -1 else 1
            if (next in tabs.indices) {
                onTab(tabs[next])
                true
            } else false
        }
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AnimatedScreenHost(targetState = tab, depthOf = { 0 }) { shownTab ->
                if (shownTab == ContactsTab.ME) {
                    com.future.contact.ui.MyProfileScreen(theme = theme)
                } else ContactsListScreen(
                    tab = shownTab,
                    contacts = contacts,
                    hasPermission = hasPermission,
                    sortLabel = sortLabel,
                    theme = theme,
                    actions = actions,
                    onRequestPermission = onRequestPermission,
                    onContactClick = onContactClick,
                    lastSelectedContactId = lastSelectedContactId,
                )
            }
        }
        FutureBottomNav(
            items = listOf(
                FutureNavItem("מועדפים", FutureIcons.Star),
                FutureNavItem("אנשי קשר", FutureIcons.Contacts),
                FutureNavItem("אני", FutureIcons.Person),
            ),
            selectedIndex = tab.ordinal,
            theme = theme,
        )
    }
}
