package com.future.messages

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
import com.future.messages.ui.screens.MessageThreadScreen
import com.future.sharednav.theme.FutureTheme
import com.future.messages.ui.theme.MessagesTheme

// הרשאות ליבה - בלעדיהן האפליקציה לא יכולה לתפקד בכלל, ולכן חוסמות שימוש.
private val CORE_PERMISSIONS = listOf(
    android.Manifest.permission.SEND_SMS,
    android.Manifest.permission.READ_SMS,
    android.Manifest.permission.RECEIVE_SMS
)

// הרשאות משלימות - בלעדיהן חלק מהתכונות נחלשות (שם איש קשר, התראות) אבל
// האפליקציה עדיין עובדת. לא חוסמות את המסך הראשי.
private val OPTIONAL_PERMISSIONS = buildList {
    add(android.Manifest.permission.READ_CONTACTS)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var sharedTheme by remember { mutableStateOf(ThemeClient.getTheme(this)) }
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        sharedTheme = ThemeClient.getTheme(this@MainActivity)
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
}

@Composable
fun MessagesApp(theme: FutureTheme) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? ComponentActivity
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
    // נשמר גם אחרי שחוזרים ל-List - כדי שהפוקוס יחזור לשיחה שממנה נכנסנו,
    // לא תמיד לשורה הראשונה ברשימה.
    var lastSelectedThreadId by remember { mutableStateOf<Long?>(null) }

    BackHandler(enabled = screen !is MessagesScreen.List) {
        if (screen is MessagesScreen.Thread) {
            if (hasPermissions) conversations = repository.getConversations()
        }
        screen = MessagesScreen.List
    }

    fun refreshConversations() {
        if (hasPermissions) conversations = repository.getConversations()
    }

    LaunchedEffect(hasPermissions, isDefaultSmsApp) {
        if (hasPermissions && isDefaultSmsApp) refreshConversations()
    }

    when {
        !hasPermissions -> PermissionRequiredScreen(theme) { permissionLauncher.launch(REQUIRED_PERMISSIONS.toTypedArray()) }
        !isDefaultSmsApp -> DefaultAppRequiredScreen(theme) {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
            defaultAppLauncher.launch(intent)
        }
        else -> when (val current = screen) {
            is MessagesScreen.List -> ConversationListScreen(
                conversations = conversations,
                theme = theme,
                onConversationClick = { conversation ->
                    repository.markThreadRead(conversation.threadId)
                    messages = repository.getMessages(conversation.threadId)
                    lastSelectedThreadId = conversation.threadId
                    screen = MessagesScreen.Thread(conversation)
                },
                onComposeClick = { screen = MessagesScreen.Compose() },
                lastSelectedThreadId = lastSelectedThreadId,
            )
            is MessagesScreen.Thread -> MessageThreadScreen(
                conversation = current.conversation,
                messages = messages,
                theme = theme,
                initialDraftText = current.draftText,
                initialDraftImageUri = current.draftImageUri,
                onBack = {
                    refreshConversations()
                    screen = MessagesScreen.List
                },
                onSend = { text, imageUri ->
                    val sent = if (imageUri != null) {
                        repository.sendMmsMessage(current.conversation.contact.phoneNumber, text, imageUri)
                    } else {
                        repository.sendMessage(current.conversation.contact.phoneNumber, text)
                    }
                    if (!sent) {
                        val message = if (imageUri != null) "שליחת ה-MMS נכשלה" else "שליחת ההודעה נכשלה"
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                    }
                    messages = repository.getMessages(current.conversation.threadId)
                },
                onCall = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${current.conversation.contact.phoneNumber}"))
                    context.startActivity(intent)
                },
                onDeleteMessage = { message ->
                    val deleted = repository.deleteMessage(message)
                    if (!deleted) {
                        android.widget.Toast.makeText(context, "מחיקת ההודעה נכשלה", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    messages = repository.getMessages(current.conversation.threadId)
                },
                onForwardMessage = { message ->
                    screen = MessagesScreen.Compose(forwardText = message.text, forwardImageUri = message.imageUri)
                }
            )
            is MessagesScreen.Compose -> ComposeScreen(
                theme = theme,
                onCancel = { screen = MessagesScreen.List },
                onStart = { address ->
                    val threadId = Telephony.Threads.getOrCreateThreadId(context, address)
                    val contact = repository.resolveContact(address)
                    val conversation = Conversation(
                        threadId = threadId,
                        contact = contact,
                        lastMessageText = "",
                        lastMessageTimestamp = System.currentTimeMillis(),
                        unreadCount = 0
                    )
                    messages = repository.getMessages(threadId)
                    screen = MessagesScreen.Thread(
                        conversation,
                        draftText = current.forwardText,
                        draftImageUri = current.forwardImageUri
                    )
                }
            )
        }
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
                Button(
                    onClick = onRequest,
                    modifier = Modifier.focusRequester(focusRequester),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor, contentColor = theme.backgroundColor),
                ) { Text("אשר הרשאות") }
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
                Button(
                    onClick = onRequest,
                    modifier = Modifier.focusRequester(focusRequester),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor, contentColor = theme.backgroundColor),
                ) { Text("הגדר כברירת מחדל") }
            }
        }
    }
}
