package com.future.messages
import com.future.sharednav.components.FutureButton

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.future.messages.data.Conversation
import com.future.messages.data.SmsRepository
import com.future.sharednav.theme.ThemeClient
import com.future.messages.ui.screens.ComposeScreen
import com.future.messages.ui.screens.ConversationListScreen
import com.future.messages.ui.screens.GroupComposeScreen
import com.future.messages.ui.screens.MessageThreadScreen
import com.future.sharednav.theme.FutureTheme
import com.future.messages.ui.theme.MessagesTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// הרשאות ליבה - בלעדיהן האפליקציה לא יכולה לתפקד בכלל, ולכן חוסמות שימוש.
private val CORE_PERMISSIONS = listOf(
    android.Manifest.permission.SEND_SMS,
    android.Manifest.permission.READ_SMS,
    android.Manifest.permission.RECEIVE_SMS
)

// הרשאות משלימות - בלעדיהן חלק מהתכונות נחלשות (שם איש קשר, התראות, חיוג
// ישיר ממקש CALL) אבל האפליקציה עדיין עובדת. לא חוסמות את המסך הראשי.
private val OPTIONAL_PERMISSIONS = buildList {
    add(android.Manifest.permission.READ_CONTACTS)
    add(android.Manifest.permission.CALL_PHONE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(android.Manifest.permission.POST_NOTIFICATIONS)
    }
}

private val REQUIRED_PERMISSIONS = CORE_PERMISSIONS + OPTIONAL_PERMISSIONS

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית. לא פוגע בניווט/הפעלה במקשים -
    // dispatchKeyEvent הוא נתיב נפרד לגמרי מ-dispatchTouchEvent.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    // מספר הטלפון של השיחה הממוקדת ברשימה הראשית כרגע - מתעדכן מ-Compose
    // (ראו onFocused ב-ConversationListScreen) ונקרא כאן, ברמת ה-Activity/View,
    // כי מקש החיוג הפיזי (KEYCODE_CALL) מגיע ל-onKeyDown ולא ל-onKeyEvent של
    // Compose. null כשלא ברשימת השיחות (למשל בתוך שיחה ספציפית) כדי שהמקש לא
    // יחייג בטעות למספר ישן מרשימה שכבר לא מוצגת.
    var focusedConversationPhone: String? = null

    /**
     * נמען (וטקסט) מ-Intent של sms:/smsto: - "שלח הודעה" מאנשי קשר/חייגן/שיחה
     * ולחיצה על התראה. קודם ה-Intent לא נקרא בכלל: Messages נפתחה ברשימה והמספר
     * אבד, והמשתמש נאלץ לחפש את השיחה בעצמו.
     */
    val pendingSendTo = mutableStateOf<Pair<String, String>?>(null)

    private fun readSendTo(intent: Intent?) {
        val data = intent?.data ?: return
        if (intent.action != Intent.ACTION_SENDTO && intent.action != Intent.ACTION_VIEW) return
        if (data.scheme?.lowercase() !in setOf("sms", "smsto", "mms", "mmsto")) return
        val address = Uri.decode(data.schemeSpecificPart.orEmpty().substringBefore('?')).trim()
        val body = intent.getStringExtra("sms_body")
            ?: intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: data.schemeSpecificPart.orEmpty().substringAfter("?body=", "").let(Uri::decode)
        pendingSendTo.value = address to body.orEmpty()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readSendTo(intent)
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_CALL) {
            val phone = focusedConversationPhone
            if (phone != null) {
                val hasCallPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.CALL_PHONE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                val action = if (hasCallPermission) Intent.ACTION_CALL else Intent.ACTION_DIAL
                startActivity(Intent(action, Uri.parse("tel:$phone")))
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) readSendTo(intent)
        // צ'אט RCS - לא עושה כלום כשאין הרשאת IMS (כלומר מחוץ לתמונת מערכת).
        com.future.messages.rcs.RcsService.start(this)
        // צ'אט FutureOS - מעדכן מפתחות/טוקן ומושך מה שהצטבר. לא עושה כלום בלי Firebase.
        com.future.messages.chat.FutureChat.refreshRegistration(this)
        setContent {
            var sharedTheme by remember { mutableStateOf(ThemeClient.getTheme(this)) }
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        sharedTheme = ThemeClient.getTheme(this@MainActivity)
                        // צ'אט: האזנה חיה בזמן שהמסך פתוח ("מקליד...", קבלות).
                        com.future.messages.chat.FutureChat.attach(this@MainActivity)
                    } else if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                        com.future.messages.chat.FutureChat.detach()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            MessagesTheme(isDarkMode = sharedTheme.isDarkMode, accentColor = Color(sharedTheme.primaryColor)) {
                MessagesApp(theme = FutureTheme(isDarkMode = sharedTheme.isDarkMode, accentColor = Color(sharedTheme.primaryColor)))
            }
        }
    }
}

private sealed class MessagesScreen {
    object List : MessagesScreen()
    data class Thread(
        val conversation: Conversation,
        val draftText: String = "",
        val draftImageUri: Uri? = null
    ) : MessagesScreen()
    data class Compose(val forwardText: String = "", val forwardImageUri: Uri? = null) : MessagesScreen()
    object GroupCompose : MessagesScreen()
    object ChatSetup : MessagesScreen()
}

@Composable
fun MessagesApp(theme: FutureTheme) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? MainActivity
    val repository = remember { SmsRepository(context) }

    fun hasCorePermissions(): Boolean = CORE_PERMISSIONS.all {
        androidx.core.content.ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    var hasPermissions by remember { mutableStateOf(hasCorePermissions()) }
    var isDefaultSmsApp by remember {
        mutableStateOf(Telephony.Sms.getDefaultSmsPackage(context) == context.packageName)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        // גם אם הרשאה משלימה (אנשי קשר/התראות) נדחתה, ממשיכים - היא לא חוסמת שימוש.
        hasPermissions = hasCorePermissions()
    }
    val defaultAppLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefaultSmsApp = Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            permissionLauncher.launch(REQUIRED_PERMISSIONS.toTypedArray())
        }
    }

    var screen by remember { mutableStateOf<MessagesScreen>(MessagesScreen.List) }
    var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var messages by remember { mutableStateOf<List<com.future.messages.data.Message>>(emptyList()) }
    var isLoadingConversations by remember { mutableStateOf(true) }

    // כל גישה ל-content://sms היא I/O אמיתי: getConversations סורקת את כל
    // התיבה, ו-getMessages/deleteThread/sendMessage פונים לספק. עד כה הן נקראו
    // ישירות מה-main dispatcher (LaunchedEffect, callback של לחיצה, BackHandler) -
    // בתיבה עם כמה אלפי הודעות זה ANR ודאי. כל קריאה עוברת כאן דרך
    // Dispatchers.IO, ורק השמת ה-state חוזרת ל-main.
    val ioScope = rememberCoroutineScope()
    // נשמר גם אחרי שחוזרים ל-List - כדי שהפוקוס יחזור לשיחה שממנה נכנסנו,
    // לא תמיד לשורה הראשונה ברשימה.
    var lastSelectedThreadId by remember { mutableStateOf<Long?>(null) }
    val archiveStore = remember { com.future.messages.data.ArchiveStore(context) }
    var archivedThreadIds by remember { mutableStateOf(archiveStore.archivedIds()) }
    var showingArchive by remember { mutableStateOf(false) }

    fun refreshConversations() {
        if (!hasPermissions) return
        ioScope.launch {
            isLoadingConversations = true
            conversations = withContext(Dispatchers.IO) { repository.getConversations() }
            isLoadingConversations = false
        }
    }

    fun loadMessages(threadId: Long) {
        ioScope.launch {
            messages = withContext(Dispatchers.IO) { repository.getMessages(threadId) }
        }
    }

    BackHandler(enabled = screen !is MessagesScreen.List) {
        if (screen is MessagesScreen.Thread) refreshConversations()
        screen = MessagesScreen.List
    }

    LaunchedEffect(hasPermissions, isDefaultSmsApp) {
        if (hasPermissions && isDefaultSmsApp) refreshConversations()
    }

    // sms:/smsto: - פותחים ישר את השיחה עם הנמען (נוצרת אם אין), עם הטקסט כטיוטה.
    // כמה נמענים או בלי מספר - מסך הודעה חדשה.
    val sendTo = activity?.pendingSendTo?.value
    LaunchedEffect(sendTo, hasPermissions, isDefaultSmsApp) {
        val (address, body) = sendTo ?: return@LaunchedEffect
        if (!hasPermissions || !isDefaultSmsApp) return@LaunchedEffect
        activity?.pendingSendTo?.value = null
        if (address.isBlank() || address.contains(',') || address.contains(';')) {
            screen = MessagesScreen.Compose(forwardText = body)
            return@LaunchedEffect
        }
        val conversation = withContext(Dispatchers.IO) {
            runCatching {
                val threadId = Telephony.Threads.getOrCreateThreadId(context, address)
                com.future.messages.data.Conversation(threadId, repository.resolveContact(address), "", 0L, 0)
            }.getOrNull()
        }
        if (conversation == null) {
            screen = MessagesScreen.Compose(forwardText = body)
            return@LaunchedEffect
        }
        lastSelectedThreadId = conversation.threadId
        messages = emptyList()
        screen = MessagesScreen.Thread(conversation, draftText = body)
        messages = withContext(Dispatchers.IO) {
            repository.markThreadRead(conversation.threadId)
            repository.getMessages(conversation.threadId)
        }
    }

    when {
        !hasPermissions -> PermissionRequiredScreen(theme) { permissionLauncher.launch(REQUIRED_PERMISSIONS.toTypedArray()) }
        !isDefaultSmsApp -> DefaultAppRequiredScreen(theme) {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
            defaultAppLauncher.launch(intent)
        }
        // המסך נמסר כמצב ולא נקרא מהמשתנה: שיחה שיוצאת באנימציה ממשיכה לצייר
        // את השיחה שלה. המפתח הוא סוג המסך, כך שעדכון באותו מסך לא מנפיש אותו מחדש.
        else -> com.future.sharednav.components.AnimatedScreenHost(
            targetState = screen,
            depthOf = { if (it is MessagesScreen.List) 0 else 1 },
            contentKey = { it::class },
        ) { current -> when (current) {
            is MessagesScreen.List -> ConversationListScreen(
                conversations = conversations,
                theme = theme,
                isLoading = isLoadingConversations,
                onConversationClick = { conversation ->
                    lastSelectedThreadId = conversation.threadId
                    messages = emptyList()
                    screen = MessagesScreen.Thread(conversation)
                    ioScope.launch {
                        messages = withContext(Dispatchers.IO) {
                            repository.markThreadRead(conversation.threadId)
                            repository.getMessages(conversation.threadId)
                        }
                    }
                },
                onComposeClick = { screen = MessagesScreen.Compose() },
                onGroupComposeClick = { screen = MessagesScreen.GroupCompose },
                onChatSetupClick = { screen = MessagesScreen.ChatSetup },
                onCallConversation = { conversation ->
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${conversation.contact.phoneNumber}")))
                },
                onAddToContacts = { conversation ->
                    val intent = Intent(Intent.ACTION_INSERT).apply {
                        type = android.provider.ContactsContract.Contacts.CONTENT_TYPE
                        putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, conversation.contact.phoneNumber)
                    }
                    context.startActivity(intent)
                },
                onDeleteConversation = { conversation ->
                    ioScope.launch {
                        val deleted = withContext(Dispatchers.IO) { repository.deleteThread(conversation.threadId) }
                        if (!deleted) {
                            android.widget.Toast.makeText(context, "מחיקת השיחה נכשלה", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        refreshConversations()
                    }
                },
                onFocusedConversationChanged = { phone -> activity?.focusedConversationPhone = phone },
                lastSelectedThreadId = lastSelectedThreadId,
                archivedThreadIds = archivedThreadIds,
                showingArchive = showingArchive,
                onShowArchive = { showingArchive = it },
                onToggleArchive = { conversation ->
                    val archive = conversation.threadId !in archivedThreadIds
                    archivedThreadIds = archiveStore.setArchived(conversation.threadId, archive)
                    android.widget.Toast.makeText(
                        context,
                        if (archive) "השיחה הועברה לארכיון" else "השיחה הוצאה מהארכיון",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                },
                onMarkAllRead = {
                    val unread = conversations.filter { it.unreadCount > 0 }.map { it.threadId }
                    ioScope.launch {
                        withContext(Dispatchers.IO) { unread.forEach { repository.markThreadRead(it) } }
                        refreshConversations()
                    }
                },
            )
            is MessagesScreen.Thread -> {
                // סטטוס השליחה (SENDING/SENT/FAILED) מתעדכן אסינכרונית ע"י
                // SmsSentReceiver/MmsSentReceiver אחרי שהמסך הזה כבר נטען, והודעה
                // נכנסת יכולה להגיע בזמן שהשיחה פתוחה - בלי ContentObserver על
                // הספק, ה-UI היה נשאר תקוע על "שולח..." או מפספס הודעות נכנסות
                // עד חזרה/כניסה מחדש למסך.
                DisposableEffect(current.conversation.threadId) {
                    val handler = android.os.Handler(android.os.Looper.getMainLooper())
                    val observer = object : android.database.ContentObserver(handler) {
                        override fun onChange(selfChange: Boolean) {
                            loadMessages(current.conversation.threadId)
                        }
                    }
                    context.contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, observer)
                    context.contentResolver.registerContentObserver(Telephony.Mms.CONTENT_URI, true, observer)
                    onDispose { context.contentResolver.unregisterContentObserver(observer) }
                }
                MessageThreadScreen(
                conversation = current.conversation,
                messages = messages,
                theme = theme,
                initialDraftText = current.draftText,
                initialDraftImageUri = current.draftImageUri,
                onBack = {
                    refreshConversations()
                    screen = MessagesScreen.List
                },
                onSend = { text, imageUri -> ioScope.launch {
                    // אין יותר Toast על הצלחה - בועת ההודעה עצמה מציגה "שולח.../✓ נשלח/
                    // ⚠ נכשל" בזמן אמת (ראו MessageStatus). Toast נשאר רק לכישלון מיידי
                    // (לפני שיש בכלל שורה בהיסטוריה להראות עליה סטטוס).
                    val dispatchedOk = withContext(Dispatchers.IO) {
                        if (imageUri != null) {
                            repository.sendMmsMessage(current.conversation.contact.phoneNumber, text, imageUri)
                        } else {
                            repository.sendMessage(current.conversation.contact.phoneNumber, text) != null
                        }
                    }
                    if (!dispatchedOk) {
                        val message = if (imageUri != null) "שליחת ה-MMS נכשלה" else "שליחת ההודעה נכשלה"
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                    }
                    messages = withContext(Dispatchers.IO) { repository.getMessages(current.conversation.threadId) }
                } },
                onCall = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${current.conversation.contact.phoneNumber}"))
                    context.startActivity(intent)
                },
                onDeleteMessage = { message ->
                    ioScope.launch {
                        val deleted = withContext(Dispatchers.IO) { repository.deleteMessage(message) }
                        if (!deleted) {
                            android.widget.Toast.makeText(context, "מחיקת ההודעה נכשלה", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        messages = withContext(Dispatchers.IO) { repository.getMessages(current.conversation.threadId) }
                    }
                },
                onForwardMessage = { message ->
                    screen = MessagesScreen.Compose(forwardText = message.text, forwardImageUri = message.imageUri)
                }
                )
            }
            is MessagesScreen.Compose -> ComposeScreen(
                theme = theme,
                repository = repository,
                initialImageUri = current.forwardImageUri,
                onCancel = { screen = MessagesScreen.List },
                onStart = { contact, imageUri ->
                    ioScope.launch {
                        val threadId = withContext(Dispatchers.IO) {
                            Telephony.Threads.getOrCreateThreadId(context, contact.phoneNumber)
                        }
                        val conversation = Conversation(
                            threadId = threadId,
                            contact = contact,
                            lastMessageText = "",
                            lastMessageTimestamp = System.currentTimeMillis(),
                            unreadCount = 0
                        )
                        messages = withContext(Dispatchers.IO) { repository.getMessages(threadId) }
                        screen = MessagesScreen.Thread(
                            conversation,
                            draftText = current.forwardText,
                            draftImageUri = imageUri
                        )
                    }
                }
            )
            is MessagesScreen.ChatSetup -> com.future.messages.ui.screens.ChatSetupScreen(
                theme = theme,
                onBack = { screen = MessagesScreen.List },
            )
            is MessagesScreen.GroupCompose -> GroupComposeScreen(
                theme = theme,
                repository = repository,
                onCancel = { screen = MessagesScreen.List },
                onSend = { contacts, text -> ioScope.launch {
                    // "הודעה קבוצתית" בטלפוניית SMS אמיתית פירושה שליחת אותה הודעה
                    // כ-SMS נפרד לכל נמען (אין MMS-קבוצתי-אמיתי סטנדרטי חוצה-ספקים) -
                    // כל נמען מקבל thread רגיל משלו, בדיוק כמו שיחה ישירה איתו.
                    val failures = withContext(Dispatchers.IO) {
                        contacts.count { repository.sendMessage(it.phoneNumber, text) == null }
                    }
                    val summary = if (failures == 0) {
                        "ההודעה נשלחה ל-${contacts.size} אנשי קשר"
                    } else {
                        "נכשלה שליחה ל-$failures מתוך ${contacts.size} אנשי קשר"
                    }
                    android.widget.Toast.makeText(context, summary, android.widget.Toast.LENGTH_SHORT).show()
                    refreshConversations()
                    screen = MessagesScreen.List
                } }
            )
        } }
    }
}

@Composable
private fun PermissionRequiredScreen(theme: FutureTheme, onRequest: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Text("כדי להשתמש בהודעות, יש לאשר הרשאות SMS ואנשי קשר", color = theme.textColor, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                FutureButton("אשר הרשאות", theme, onRequest, focusRequester = focusRequester)
            }
        }
    }
}

@Composable
private fun DefaultAppRequiredScreen(theme: FutureTheme, onRequest: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Text("צריך להגדיר את Messages כאפליקציית ברירת המחדל למסרונים", color = theme.textColor, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                FutureButton("הגדר כברירת מחדל", theme, onRequest, focusRequester = focusRequester)
            }
        }
    }
}
