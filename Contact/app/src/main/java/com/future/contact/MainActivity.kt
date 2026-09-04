package com.future.contact

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.future.contact.data.Contact
import com.future.contact.data.ContactsRepository
import com.future.sharednav.theme.ThemeClient
import com.future.contact.ui.ContactDetailScreen
import com.future.contact.ui.ContactsListScreen
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית. לא פוגע בניווט/הפעלה במקשים -
    // dispatchKeyEvent הוא נתיב נפרד לגמרי מ-dispatchTouchEvent.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val repository = remember { ContactsRepository(this) }
            var hasPermission by remember { mutableStateOf(repository.hasContactsPermission()) }
            var contacts by remember { mutableStateOf(listOf<Contact>()) }
            var selectedContact by remember { mutableStateOf<Contact?>(null) }
            // נשמר גם אחרי selectedContact חוזר ל-null (בניגוד ל-selectedContact
            // עצמו) - כדי שכשחוזרים "אחורה" מהפרטים, רשימת אנשי הקשר תדע איזו
            // שורה למקד בחזרה, במקום תמיד לקפוץ לשורה הראשונה.
            var lastSelectedContactId by remember { mutableStateOf<String?>(null) }
            var theme by remember {
                mutableStateOf(
                    ThemeClient.getTheme(this@MainActivity).let {
                        FutureTheme(isDarkMode = it.isDarkMode, accentColor = Color(it.primaryColor))
                    }
                )
            }

            val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

            // מבקשים גם WRITE_CONTACTS (לא רק READ) - בלי זה עריכת/מחיקת אנשי
            // קשר יכולה להיכשל בשקט בגרסאות/הגדרות שבהן הרשאת כתיבה בזמן ריצה
            // באמת נדרשת, למרות שה-manifest כבר מכריז עליה.
            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
                hasPermission = results[android.Manifest.permission.READ_CONTACTS] == true || repository.hasContactsPermission()
                if (hasPermission) {
                    coroutineScope.launch {
                        val loaded = withContext(kotlinx.coroutines.Dispatchers.IO) { repository.getAllContacts() }
                        contacts = loaded
                    }
                }
            }

            LaunchedEffect(hasPermission) {
                if (hasPermission) {
                    val loaded = withContext(kotlinx.coroutines.Dispatchers.IO) { repository.getAllContacts() }
                    contacts = loaded
                }
            }

            BackHandler(enabled = selectedContact != null) { selectedContact = null }

            // מרענן את הרשימה בכל חזרה למסך (למשל אחרי הוספת איש קשר חדש דרך
            // עורך המערכת) בלי לבנות מחדש את כל ה-Activity.
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        if (hasPermission) {
                            coroutineScope.launch {
                                val loaded = withContext(kotlinx.coroutines.Dispatchers.IO) { repository.getAllContacts() }
                                contacts = loaded
                            }
                        }
                        val shared = ThemeClient.getTheme(this@MainActivity)
                        theme = FutureTheme(isDarkMode = shared.isDarkMode, accentColor = Color(shared.primaryColor))
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            Surface(modifier = Modifier.fillMaxSize(), color = theme.backgroundColor) {
                val current = selectedContact
                if (current != null) {
                    ContactDetailScreen(contact = current, onBack = { selectedContact = null }, theme = theme)
                } else {
                    ContactsListScreen(
                        contacts = contacts,
                        hasPermission = hasPermission,
                        theme = theme,
                        onRequestPermission = {
                            permissionLauncher.launch(
                                arrayOf(android.Manifest.permission.READ_CONTACTS, android.Manifest.permission.WRITE_CONTACTS)
                            )
                        },
                        onContactClick = { selectedContact = it; lastSelectedContactId = it.id },
                        lastSelectedContactId = lastSelectedContactId,
                        onAddContact = {
                            val intent = Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        },
                        onEditContact = { contact ->
                            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contact.id)
                            val intent = Intent(Intent.ACTION_EDIT)
                            intent.setDataAndType(uri, ContactsContract.Contacts.CONTENT_ITEM_TYPE)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        },
                        onDeleteContact = { contact ->
                            coroutineScope.launch {
                                try {
                                    val loaded = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contact.id)
                                        contentResolver.delete(uri, null, null)
                                        repository.getAllContacts()
                                    }
                                    contacts = loaded
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(this@MainActivity, "לא ניתן למחוק את איש הקשר", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onToggleFavorite = { contact ->
                            coroutineScope.launch {
                                val loaded = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    repository.setFavorite(contact.id, !contact.isFavorite)
                                    repository.getAllContacts()
                                }
                                contacts = loaded
                            }
                        }
                    )
                }
            }
        }
    }

}
