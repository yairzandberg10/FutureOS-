package com.future.dialer
import androidx.compose.material.icons.rounded.StarBorder

import com.future.sharednav.icons.FutureIcons

import android.Manifest
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.future.dialer.data.repository.CallLogRepository
import com.future.dialer.data.repository.ContactRepository
import com.future.dialer.telecom.CallService
import com.future.dialer.ui.CallsViewModel
import com.future.dialer.ui.calllog.CallLogScreen
import com.future.dialer.ui.contact.ContactScreen
import com.future.dialer.ui.contacts.ContactsViewModel
import com.future.dialer.ui.contacts.SearchScreen
import com.future.dialer.ui.dialpad.DialpadScreen
import com.future.dialer.ui.contacts.ContactsTabScreen
import com.future.dialer.ui.CallFormat
import com.future.dialer.data.model.CallFilter
import com.future.dialer.ui.incall.InCallScreen
import com.future.dialer.ui.incall.InCallViewModel
import com.future.dialer.ui.navigation.Screen
import com.future.dialer.ui.navigation.decodeArg
import com.future.dialer.ui.theme.DialerTheme
import com.future.sharednav.components.ConfirmDialog
import com.future.sharednav.components.FutureBottomNav
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureNavItem
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.components.FutureSnackbarHost
import com.future.sharednav.components.rememberFutureSnackbarState
import com.future.sharednav.focus.FocusableItem
import com.future.sharednav.nav.onOptionsKeyPress
import com.future.sharednav.theme.FutureTransitions
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.ThemeClient
import com.future.sharednav.theme.onStatusColor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית. לא פוגע בניווט/הפעלה במקשים -
    // dispatchKeyEvent הוא נתיב נפרד לגמרי מ-dispatchTouchEvent.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    private val contactRepository by lazy { ContactRepository(this) }
    private val callLogRepository by lazy { CallLogRepository(this) }

    private val callsViewModel: CallsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return CallsViewModel(contactRepository, callLogRepository) as T
            }
        }
    }

    private val contactsViewModel: ContactsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ContactsViewModel(contactRepository) as T
            }
        }
    }

    private val inCallViewModel: InCallViewModel by viewModels()

    // המסך/טאב הפעיל כרגע - מתעדכן מ-MainScreen כדי ש-onKeyDown ידע לאן
    // שייכת ספרה. בלי זה, ספרות שהוקלדו בשדה החיפוש "דולפות" גם למקלדת
    // ברקע ומופיעות שם בטעות כשחוזרים אליה.
    @Volatile private var currentRoute: String? = Screen.CallLog.route

    // ספרה שהוקלדה ביומן או במועדפים מעבירה למקלדת עם הספרה - כמו בטלפון
    // מקשים רגיל, שבו מתחילים לחייג מכל מסך.
    private val _openDialpad = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // חץ ימין/שמאל בטאב שאין בו אף רכיב בפוקוס (מועדפים ריקים, או רשימה שעוד
    // לא נבנתה). ה-onKeyEvent של הטאבים מקבל מקשים רק כשמשהו בתוכו בפוקוס -
    // בלי זה המקשים הגיעו רק לכאן, אף אחד לא טיפל בהם, והטאב "נתקע".
    private val _tabStep = MutableSharedFlow<Int>(extraBufferCapacity = 1)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            callsViewModel.refresh()
            contactsViewModel.refresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // שיחה נכנסת יכולה להגיע כשהמסך כבוי/נעול - בלי זה המסך היה נשאר כבוי/נעול
        // והשיחה בלתי נגישה בפועל, גם אחרי ש-CallService פותח את ה-Activity הזה.
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        checkAndRequestPermissions()
        registerProviderObservers()

        // אם המספר הגיע מכוונה חיצונית (ACTION_DIAL, למשל לחיצה על מספר באנשי קשר)
        val prefillNumber = intentDialNumber(intent)
        if (prefillNumber != null) callsViewModel.setNumber(prefillNumber)

        setContent {
            var sharedTheme by remember { mutableStateOf(ThemeClient.getTheme(this)) }
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        sharedTheme = ThemeClient.getTheme(this@MainActivity)
                        callsViewModel.refresh()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            DialerTheme(isDarkMode = sharedTheme.isDarkMode, accentColor = Color(sharedTheme.primaryColor)) {
                MainScreen(
                    callsViewModel = callsViewModel,
                    contactsViewModel = contactsViewModel,
                    inCallViewModel = inCallViewModel,
                    openDialpadRequests = _openDialpad.asSharedFlow(),
                    tabStepRequests = _tabStep.asSharedFlow(),
                    startOnDialpad = prefillNumber != null,
                    actions = DialerActions(
                        placeCall = ::makeRealCall,
                        sendMessage = ::openMessage,
                        addContact = ::openAddContact,
                        openContactsApp = ::openContactsApp,
                        openCallSettings = ::openCallSettings,
                        ensureCallLogWrite = ::ensureCallLogWrite,
                    ),
                    checkIsDefaultDialer = { isDefaultDialer() },
                    onRequestDefaultDialer = { requestDefaultDialerRole() },
                    onRouteChanged = { route -> currentRoute = route },
                )
            }
        }
    }

    // launchMode="singleTop": מסך החיוג שכבר פתוח מקבל את הכוונה כאן במקום
    // להיבנות מחדש (שיחה נכנסת, ACTION_DIAL מאפליקציה אחרת).
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val number = intentDialNumber(intent) ?: return
        callsViewModel.setNumber(number)
        _openDialpad.tryEmit(Unit)
    }

    private fun intentDialNumber(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_DIAL && intent?.action != Intent.ACTION_VIEW) return null
        val data = intent.data ?: return null
        if (data.scheme != "tel") return null
        return data.schemeSpecificPart
    }

    private val providerObservers = mutableListOf<android.database.ContentObserver>()

    /**
     * היומן ואנשי הקשר נטענים מחדש ברגע שהם משתנים. בלי זה שיחה שהסתיימה
     * הופיעה ביומן רק אחרי יציאה וכניסה (Telecom כותב אותה שנייה אחרי שהמסך
     * כבר חזר), ואיש קשר שנוסף לא החליף את המספר בשם.
     */
    private fun registerProviderObservers() {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        fun observe(uri: Uri, onChange: () -> Unit) {
            val observer = object : android.database.ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) = onChange()
            }
            try {
                contentResolver.registerContentObserver(uri, true, observer)
                providerObservers += observer
            } catch (e: SecurityException) {
                // אין עדיין הרשאה - onResume טוען מחדש בכל מקרה.
            }
        }
        observe(android.provider.CallLog.Calls.CONTENT_URI) { callsViewModel.reloadCalls() }
        observe(ContactsContract.Contacts.CONTENT_URI) { callsViewModel.reloadContacts() }
    }

    override fun onDestroy() {
        providerObservers.forEach { contentResolver.unregisterContentObserver(it) }
        providerObservers.clear()
        super.onDestroy()
    }

    private fun isDefaultDialer(): Boolean {
        val roleManager = getSystemService(Context.ROLE_SERVICE) as? RoleManager ?: return false
        return roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
    }

    private fun requestDefaultDialerRole() {
        val roleManager = getSystemService(Context.ROLE_SERVICE) as? RoleManager ?: return
        if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
            startActivity(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            // "נקה יומן" מוחק מהיומן של המערכת.
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.RECORD_AUDIO,
        )
        // בלי זה, החל מאנדרואיד 13, התראת השיחה הנכנסת (כולל ה-fullScreenIntent שמעיר
        // את המסך) לא מוצגת בכלל - ראו CallService.notifyCallRinging.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) requestPermissionLauncher.launch(missing.toTypedArray())
    }

    /** true כשיש הרשאה למחוק מהיומן; אחרת מבקש אותה ומחזיר false. */
    private fun ensureCallLogWrite(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        requestPermissionLauncher.launch(arrayOf(Manifest.permission.WRITE_CALL_LOG))
        return false
    }

    private fun makeRealCall(phoneNumber: String) {
        if (phoneNumber.isBlank()) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            // ישירות ל-Telecom. ACTION_CALL דרך startActivity חזר ל-Activity הזה
            // עצמו (אפליקציית החיוג ברירת המחדל), והשיחה לא יצאה.
            val telecom = getSystemService(TelecomManager::class.java)
            try {
                telecom.placeCall(Uri.fromParts("tel", phoneNumber, null), Bundle())
            } catch (e: SecurityException) {
                checkAndRequestPermissions()
                return
            }
            // מנקים את שדה החיוג אחרי שהשיחה יצאה, כדי שמספר ישן לא יישאר "תקוע"
            // בשדה ויחטוף בטעות לחיצת DPAD_CENTER/ENTER/CALL הבאה (ראו onKeyDown).
            callsViewModel.clearNumber()
        } else {
            checkAndRequestPermissions()
        }
    }

    private fun openMessage(phoneNumber: String) {
        startSafely(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(phoneNumber)}")))
    }

    private fun openAddContact(phoneNumber: String) {
        startSafely(
            Intent(ContactsContract.Intents.Insert.ACTION).apply {
                type = ContactsContract.RawContacts.CONTENT_TYPE
                putExtra(ContactsContract.Intents.Insert.PHONE, phoneNumber)
            }
        )
    }

    private fun openContactsApp() {
        val intent = packageManager.getLaunchIntentForPackage(CONTACTS_PACKAGE)
            ?: Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)
        startSafely(intent)
    }

    private fun openCallSettings() {
        startSafely(Intent(TelecomManager.ACTION_SHOW_CALL_SETTINGS))
    }

    private fun startSafely(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // אין מי שמטפל בזה במכשיר - אין לאן לפנות.
        }
    }

    // כשיש שיחה פעילה (לא מצלצלת), מקשי הספרות שולחים טוני DTMF לצד השני
    // במקום להקליד מספר חדש למסך החיוג - בדיוק כמו בטלפון אמיתי.
    private fun digitFor(keyCode: Int): Char? = when (keyCode) {
        in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> '0' + (keyCode - KeyEvent.KEYCODE_0)
        KeyEvent.KEYCODE_STAR -> '*'
        KeyEvent.KEYCODE_POUND -> '#'
        else -> null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val currentCallState = CallService.callState.value
        val isCallActive = currentCallState == android.telecom.Call.STATE_ACTIVE
        // שיחה נכנסת שעדיין לא נענתה - "מצלצלת" - חייבת להישאר עד למענה/דחייה
        // דרך מסך השיחה עצמו. בלי הבדיקה הזו, ספרות היו דולפות לשדה החיוג
        // וקיצור החיוג (DPAD_CENTER/ENTER/CALL) היה יכול "לחטוף" את הלחיצה
        // שאמורה לענות לשיחה ולחייג בטעות למספר ישן שנשאר בשדה.
        val isCallRinging = currentCallState == android.telecom.Call.STATE_RINGING
        if (isCallActive) {
            digitFor(keyCode)?.let { digit ->
                inCallViewModel.onDtmfDigitPressed(digit)
                return true
            }
        }

        // מקש הפעולה הפיזי (CALL) וניתוק/דחייה (ENDCALL) חייבים לעבוד גם כשמסך
        // השיחה הוא זה שממוקד - בטלפון מקשים אלה המקשים האינסטינקטיביים.
        when (keyCode) {
            KeyEvent.KEYCODE_CALL -> if (isCallRinging) {
                inCallViewModel.answer()
                return true
            }
            KeyEvent.KEYCODE_ENDCALL -> {
                if (isCallRinging) {
                    inCallViewModel.reject()
                    return true
                }
                if (isCallActive) {
                    inCallViewModel.hangUp()
                    return true
                }
            }
        }
        if (isCallRinging) return super.onKeyDown(keyCode, event)

        val route = currentRoute
        val isOnDialpadTab = route == Screen.Dialpad.route
        val digit = digitFor(keyCode)

        // ספרה ביומן או במועדפים: עוברים למקלדת עם הספרה.
        if (digit != null && (route == Screen.CallLog.route || route == Screen.Contacts.route)) {
            callsViewModel.setNumber(digit.toString())
            _openDialpad.tryEmit(Unit)
            return true
        }
        if (route in TabRoutes && (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
            // כמו ב-onKeyEvent של הטאבים: ימין = הטאב הקודם (RTL).
            _tabStep.tryEmit(if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) -1 else 1)
            return true
        }
        if (!isOnDialpadTab) return super.onKeyDown(keyCode, event)

        if (digit != null) {
            callsViewModel.onDigitPressed(digit.toString())
            return true
        }
        when (keyCode) {
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_DEL -> {
                if (callsViewModel.dialedNumber.value.isNotEmpty()) {
                    callsViewModel.onDeletePressed()
                    return true
                }
            }
            KeyEvent.KEYCODE_CALL, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                // לא מחייגים מעל שיחה פעילה - הלחיצה שייכת למסך השיחה.
                if (!isCallActive) {
                    val number = callsViewModel.dialedNumber.value
                    if (number.isNotEmpty()) {
                        makeRealCall(number)
                        return true
                    }
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (CallService.callState.value == android.telecom.Call.STATE_ACTIVE && digitFor(keyCode) != null) {
            inCallViewModel.onDtmfDigitReleased()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private companion object {
        const val CONTACTS_PACKAGE = "com.future.contact"
        val TabRoutes = setOf(Screen.CallLog.route, Screen.Contacts.route, Screen.Dialpad.route)
    }
}

/** פעולות שיוצאות מהאפליקציה - מתבצעות ב-Activity. */
class DialerActions(
    val placeCall: (String) -> Unit,
    val sendMessage: (String) -> Unit,
    val addContact: (String) -> Unit,
    val openContactsApp: () -> Unit,
    val openCallSettings: () -> Unit,
    val ensureCallLogWrite: () -> Boolean,
)

@Composable
fun MainScreen(
    callsViewModel: CallsViewModel,
    contactsViewModel: ContactsViewModel,
    inCallViewModel: InCallViewModel,
    openDialpadRequests: kotlinx.coroutines.flow.SharedFlow<Unit>,
    tabStepRequests: kotlinx.coroutines.flow.SharedFlow<Int>,
    startOnDialpad: Boolean,
    actions: DialerActions,
    checkIsDefaultDialer: () -> Boolean,
    onRequestDefaultDialer: () -> Unit,
    onRouteChanged: (String?) -> Unit = {},
) {
    // חוזרים מהדיאלוג של המערכת (בקשת ברירת מחדל) לא מפעילים מחדש את onCreate,
    // אז בלי לבדוק שוב ב-onResume נשארים תקועים במסך "הגדר כברירת מחדל" גם אחרי
    // שהמשתמש כן אישר.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isDefaultDialer by remember { mutableStateOf(checkIsDefaultDialer()) }
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isDefaultDialer = checkIsDefaultDialer()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!isDefaultDialer) {
        DefaultDialerRequiredScreen(onRequestDefaultDialer)
        return
    }

    val theme = LocalFutureTheme.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val contactRepository = remember { ContactRepository(context) }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    LaunchedEffect(currentRoute) { onRouteChanged(currentRoute) }

    val activeCall by CallService.activeCall.collectAsState()
    val callState by CallService.callState.collectAsState()
    val snackbar = rememberFutureSnackbarState()

    var menuOpen by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    // כל שיחה אמיתית - נכנסת או יוצאת - מגיעה מ-CallService. כשמופיעה שיחה חדשה,
    // עוברים אוטומטית למסך השיחה, בלי קשר לאיך היא הותחלה.
    LaunchedEffect(activeCall) {
        val call = activeCall
        if (call != null) {
            val number = call.details?.handle?.schemeSpecificPart ?: ""
            val name = contactRepository.findNameForNumber(number) ?: number
            menuOpen = false
            navController.navigate(Screen.InCall.createRoute(name, number)) { launchSingleTop = true }
        } else {
            // השיחה נכנסה ליומן של המערכת - טוענים אותו מחדש.
            callsViewModel.refresh()
        }
    }
    LaunchedEffect(Unit) {
        // מספר שהגיע מכוונה חיצונית (ACTION_DIAL) נפתח במקלדת, מוכן לחיוג.
        if (startOnDialpad) navController.switchTab(Screen.Dialpad.route)
        openDialpadRequests.collect { navController.switchTab(Screen.Dialpad.route) }
    }

    val isOnCallScreen = currentRoute?.startsWith("incall") == true
    // מקש התפריט: בזמן שיחה הוא פותח הודעה מהירה (גם כשהיא מצלצלת - ואז
    // ההודעה היא התשובה); בשאר המסכים את תפריט האפשרויות.
    onOptionsKeyPress {
        when {
            isOnCallScreen || callState == android.telecom.Call.STATE_RINGING -> inCallViewModel.toggleQuickMessage()
            currentRoute != null && currentRoute != Screen.Search.route -> menuOpen = !menuOpen
        }
    }

    // מימין לשמאל: יומן, אנשי קשר, מקלדת.
    val tabs = listOf(
        Triple(Screen.CallLog.route, "יומן", FutureIcons.Call),
        Triple(Screen.Contacts.route, "אנשי קשר", FutureIcons.Contacts),
        Triple(Screen.Dialpad.route, "מקלדת", FutureIcons.Dialpad),
    )
    val tabIndex = tabs.indexOfFirst { it.first == currentRoute }
    val isOnTab = tabIndex >= 0

    LaunchedEffect(Unit) {
        tabStepRequests.collect { step ->
            val current = tabs.indexOfFirst { it.first == navController.currentDestination?.route }
            val next = current + step
            if (current >= 0 && next in tabs.indices) navController.switchTab(tabs[next].first)
        }
    }

    // חזרה מהמקלדת (כשאין ספרות למחוק) או מאנשי הקשר מחזירה ליומן; מהיומן - יוצאים.
    BackHandler(enabled = isOnTab && tabIndex != 0) { navController.switchTab(Screen.CallLog.route) }

    val ongoingCall = activeCall
    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            if (ongoingCall != null && !isOnCallScreen) {
                val number = ongoingCall.details?.handle?.schemeSpecificPart ?: ""
                val callerName by produceState(number, number) {
                    value = contactRepository.findNameForNumber(number) ?: number
                }
                OngoingCallBanner(
                    name = callerName,
                    onReturn = {
                        navController.navigate(Screen.InCall.createRoute(callerName, number)) { launchSingleTop = true }
                    },
                )
            }
        },
        bottomBar = {
            if (isOnTab) {
                FutureBottomNav(
                    items = tabs.map { (_, label, icon) ->
                        FutureNavItem(label = label, icon = icon)
                    },
                    selectedIndex = tabIndex,
                    theme = theme,
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .then(
                    if (isOnTab) {
                        Modifier.onKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                            val direction = when (event.key) {
                                Key.DirectionRight -> androidx.compose.ui.focus.FocusDirection.Right
                                Key.DirectionLeft -> androidx.compose.ui.focus.FocusDirection.Left
                                else -> return@onKeyEvent false
                            }
                            // קודם בין צ'יפי הסינון; רק כשאין לאן לזוז - טאב.
                            if (focusManager.moveFocus(direction)) return@onKeyEvent true
                            val next = if (event.key == Key.DirectionRight) tabIndex - 1 else tabIndex + 1
                            if (next !in tabs.indices) return@onKeyEvent false
                            navController.switchTab(tabs[next].first)
                            true
                        }
                    } else Modifier
                ),
        ) {
            val tabRoutes = remember { tabs.map { it.first }.toSet() }
            fun isTabSwitch(from: String?, to: String?) = from in tabRoutes && to in tabRoutes
            NavHost(
                navController,
                startDestination = Screen.CallLog.route,
                enterTransition = {
                    if (isTabSwitch(initialState.destination.route, targetState.destination.route)) {
                        FutureTransitions.fadeThrough().targetContentEnter
                    } else FutureTransitions.navEnter
                },
                exitTransition = {
                    if (isTabSwitch(initialState.destination.route, targetState.destination.route)) {
                        FutureTransitions.fadeThrough().initialContentExit
                    } else FutureTransitions.navExit
                },
                popEnterTransition = { FutureTransitions.navPopEnter },
                popExitTransition = { FutureTransitions.navPopExit },
            ) {
                composable(Screen.CallLog.route) {
                    CallLogScreen(
                        viewModel = callsViewModel,
                        onOpen = { name, number -> navController.navigate(Screen.Contact.createRoute(name, number)) },
                    )
                }
                composable(Screen.Dialpad.route) {
                    DialpadScreen(callsViewModel, onCall = actions.placeCall)
                }
                composable(Screen.Contacts.route) {
                    ContactsTabScreen(
                        viewModel = callsViewModel,
                        onOpen = { contact -> navController.navigate(Screen.Contact.createRoute(contact.name, contact.phoneNumber)) },
                    )
                }
                composable(Screen.Search.route) {
                    SearchScreen(
                        viewModel = contactsViewModel,
                        onBack = { navController.popBackStack() },
                        onOpen = { contact ->
                            navController.navigate(Screen.Contact.createRoute(contact.name, contact.phoneNumber)) {
                                popUpTo(Screen.Search.route) { inclusive = true }
                            }
                        },
                    )
                }
                composable(
                    route = Screen.Contact.route,
                    arguments = listOf(
                        navArgument("name") { type = NavType.StringType },
                        navArgument("number") { type = NavType.StringType },
                    ),
                ) { entry ->
                    val name = decodeArg(entry.arguments?.getString("name"))
                    val number = decodeArg(entry.arguments?.getString("number"))
                    ContactScreen(
                        name = name,
                        number = number,
                        viewModel = callsViewModel,
                        onBack = { navController.popBackStack() },
                        onMenu = { menuOpen = true },
                        onCall = { actions.placeCall(number) },
                        onMessage = { actions.sendMessage(number) },
                        onAddContact = { actions.addContact(number) },
                    )
                }
                composable(
                    route = Screen.InCall.route,
                    arguments = listOf(
                        navArgument("name") { type = NavType.StringType },
                        navArgument("number") { type = NavType.StringType },
                    ),
                ) { entry ->
                    val number = decodeArg(entry.arguments?.getString("number"))
                    val name = decodeArg(entry.arguments?.getString("name")).ifEmpty { number.ifEmpty { stringResource(R.string.unknown) } }
                    InCallScreen(
                        name = name,
                        phoneNumber = number,
                        viewModel = inCallViewModel,
                        onCallEnded = { navController.popBackStack() },
                        onCallAgain = { again ->
                            navController.popBackStack()
                            actions.placeCall(again)
                        },
                    )
                }
            }

            FutureSnackbarHost(snackbar, theme)
        }
    }

    if (menuOpen) {
        FutureOptionsMenu(theme = theme, onDismissRequest = { menuOpen = false }, header = "שיחות") {
            fun pick(action: () -> Unit): () -> Unit = { menuOpen = false; action() }
            FutureMenuRow("חיפוש", FutureIcons.Search, theme, pick { navController.navigate(Screen.Search.route) })
            if (currentRoute == Screen.CallLog.route) {
                val filter = callsViewModel.filter.value
                val next = CallFilter.entries[(filter.ordinal + 1) % CallFilter.entries.size]
                FutureMenuRow("סינון · ${filter.label}", FutureIcons.Call, theme, pick { callsViewModel.setFilter(next) })
                FutureMenuRow("סטטיסטיקות", FutureIcons.TrendingUp, theme, pick { showStats = true })
            }
            FutureMenuRow("פתח את אנשי קשר", FutureIcons.Contacts, theme, pick(actions.openContactsApp))
            FutureMenuRow("הגדרות", FutureIcons.Settings, theme, pick(actions.openCallSettings))
            FutureMenuRow("נקה יומן", FutureIcons.Delete, theme, pick { confirmClear = true }, destructive = true)
        }
    }

    if (showStats) {
        val stats by callsViewModel.stats.collectAsState()
        com.future.sharednav.components.FutureDialog(
            theme = theme,
            onDismissRequest = { showStats = false },
            title = "סטטיסטיקות",
            buttons = { FutureButton("סגור", theme, { showStats = false }, fillMaxWidth = true) },
        ) {
            fun minutes(seconds: Long): String = "${(seconds + 59) / 60} דק׳"
            com.future.sharednav.components.FutureSettingItem(
                title = "התקבלו",
                summary = "${stats.incomingCount} שיחות",
                icon = FutureIcons.CallReceived,
                iconTint = CallFormat.colorOf(com.future.dialer.data.model.CallType.INCOMING, theme),
                theme = theme,
                onClick = null,
                trailing = { Text(minutes(stats.incomingSeconds), color = theme.textColor, fontWeight = FontWeight.SemiBold) },
            )
            com.future.sharednav.components.FutureSettingItem(
                title = "חויגו",
                summary = "${stats.outgoingCount} שיחות",
                icon = FutureIcons.CallMade,
                iconTint = CallFormat.colorOf(com.future.dialer.data.model.CallType.OUTGOING, theme),
                theme = theme,
                onClick = null,
                trailing = { Text(minutes(stats.outgoingSeconds), color = theme.textColor, fontWeight = FontWeight.SemiBold) },
            )
            com.future.sharednav.components.FutureSettingItem(
                title = "סה״כ דקות שיחה",
                summary = "${stats.missedCount} לא נענו",
                icon = FutureIcons.Call,
                theme = theme,
                onClick = null,
                trailing = { Text(minutes(stats.totalSeconds), color = theme.textColor, fontWeight = FontWeight.Bold) },
            )
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            message = "לנקות את יומן השיחות?",
            theme = theme,
            confirmLabel = "נקה",
            onCancel = { confirmClear = false },
            onConfirm = {
                confirmClear = false
                if (actions.ensureCallLogWrite()) {
                    callsViewModel.clearCallLog { ok -> snackbar.show(if (ok) "היומן נוקה" else "היומן לא נוקה") }
                } else {
                    snackbar.show("אשר את ההרשאה ונסה שוב")
                }
            },
        )
    }
}

/** מעבר בין טאבים: בלי להעמיס את מחסנית החזרה, כמו NavigationBar רגיל. */
private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId)
        launchSingleTop = true
    }
}

@Composable
private fun OngoingCallBanner(name: String, onReturn: () -> Unit) {
    val theme = LocalFutureTheme.current
    val onSuccess = theme.onStatusColor(theme.successColor)
    FocusableItem(
        onClick = onReturn,
        // שיחה פעילה היא סטטוס, ולכן בצבע ההצלחה של הפלטה ולא בצבע ההדגשה -
        // ההדגשה שמורה לפוקוס ולבחירה בלבד.
        accentColor = onSuccess,
        modifier = Modifier.fillMaxWidth(),
        idleBackgroundColor = theme.successColor,
        focusedBackgroundColor = theme.successColor,
        cornerRadius = 0.dp,
        scaleOnFocus = false,
        contentPadding = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = FutureIcons.Call, contentDescription = null, tint = onSuccess, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${stringResource(R.string.ongoing_call)} · $name",
                color = onSuccess,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.tap_to_return),
                color = onSuccess.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun DefaultDialerRequiredScreen(onRequest: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text(
                text = stringResource(R.string.default_dialer_required),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            FutureButton(
                text = stringResource(R.string.set_as_default_dialer),
                theme = LocalFutureTheme.current,
                onClick = onRequest,
            )
        }
    }
}
