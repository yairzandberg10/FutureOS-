package com.future.dialer

import android.Manifest
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.future.dialer.data.repository.CallLogRepository
import com.future.dialer.data.repository.ContactRepository
import com.future.dialer.telecom.CallService
import com.future.dialer.ui.contacts.ContactsScreen
import com.future.dialer.ui.contacts.ContactsViewModel
import com.future.dialer.ui.dialpad.DialpadScreen
import com.future.dialer.ui.dialpad.DialpadViewModel
import com.future.dialer.ui.incall.InCallScreen
import com.future.dialer.ui.incall.InCallViewModel
import com.future.dialer.ui.navigation.Screen
import com.future.dialer.ui.theme.DialerTheme
import com.future.sharednav.theme.ThemeClient

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית. לא פוגע בניווט/הפעלה במקשים -
    // dispatchKeyEvent הוא נתיב נפרד לגמרי מ-dispatchTouchEvent.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true


    private val contactRepository by lazy { ContactRepository(this) }
    private val callLogRepository by lazy { CallLogRepository(this) }

    private val dialpadViewModel: DialpadViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return DialpadViewModel(contactRepository, callLogRepository) as T
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

    // המסך/טאב הפעיל כרגע (Dialpad/Contacts/InCall) - מתעדכן מ-MainScreen כדי
    // ש-onKeyDown ידע אם מותר להעביר ספרות לשדה החיוג. בלי זה, ספרות שהוקלדו
    // בזמן שהמשתמש נמצא בטאב אנשי קשר (למשל בשדה החיפוש) "דולפות" גם לשדה
    // החיוג ברקע ומופיעות שם בטעות כשחוזרים לטאב החיוג.
    @Volatile private var currentRoute: String? = Screen.Dialpad.route

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            dialpadViewModel.refresh()
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

        // אם המספר הגיע מכוונה חיצונית (ACTION_DIAL, למשל לחיצה על מספר באנשי קשר)
        val prefillNumber = intentDialNumber(intent)
        if (prefillNumber != null) {
            prefillNumber.forEach { dialpadViewModel.onDigitPressed(it.toString()) }
        }

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

            // מקש MENU הפיזי תמיד נחסם ברמת המערכת (StatusBarAccessibilityService צורך
            // אותו ללחיצה ארוכה) ומשודר מחדש כלחיצה קצרה - ראו ההערה המקבילה ב-
            // Music/MusicNavHost.kt. בזמן שיחה מצלצלת, זו הדרך היחידה לפתוח "שליחת הודעה
            // מהירה" בלי לענות/לדחות קודם.
            DisposableEffect(Unit) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context?, intent: Intent?) {
                        if (CallService.callState.value == android.telecom.Call.STATE_RINGING) {
                            inCallViewModel.toggleQuickMessage()
                        }
                    }
                }
                val filter = IntentFilter("com.future.futureui.ACTION_OPTIONS_SHORT_PRESS")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
                } else {
                    @Suppress("UnspecifiedRegisterReceiverFlag")
                    registerReceiver(receiver, filter)
                }
                onDispose { unregisterReceiver(receiver) }
            }

            DialerTheme(isDarkMode = sharedTheme.isDarkMode, accentColor = Color(sharedTheme.primaryColor)) {
                MainScreen(
                    dialpadViewModel = dialpadViewModel,
                    contactsViewModel = contactsViewModel,
                    inCallViewModel = inCallViewModel,
                    checkIsDefaultDialer = { isDefaultDialer() },
                    onRequestDefaultDialer = { requestDefaultDialerRole() },
                    onMakeCall = { number -> makeRealCall(number) },
                    onRouteChanged = { route -> currentRoute = route }
                )
            }
        }
    }

    private fun intentDialNumber(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_DIAL && intent?.action != Intent.ACTION_VIEW) return null
        val data = intent.data ?: return null
        if (data.scheme != "tel") return null
        return data.schemeSpecificPart
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
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_CONTACTS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.WRITE_CONTACTS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_CALL_LOG)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CALL_PHONE)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        // בלי זה, החל מאנדרואיד 13, התראת השיחה הנכנסת (כולל ה-fullScreenIntent שמעיר
        // את המסך) לא מוצגת בכלל - ראו CallService.notifyCallRinging.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun makeRealCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            startActivity(intent)
            // מנקים את שדה החיוג אחרי שהשיחה יצאה, כדי שמספר ישן לא יישאר "תקוע"
            // בשדה ויחטוף בטעות לחיצת DPAD_CENTER/ENTER/CALL הבאה (ראו onKeyDown).
            dialpadViewModel.clearNumber()
        } else {
            checkAndRequestPermissions()
        }
    }

    // כשיש שיחה פעילה (לא מצלצלת), מקשי הספרות שולחים טוני DTMF לצד השני
    // במקום להקליד מספר חדש למסך החיוג - בדיוק כמו בטלפון אמיתי.
    private fun dtmfDigitFor(keyCode: Int): Char? = when (keyCode) {
        KeyEvent.KEYCODE_0 -> '0'
        KeyEvent.KEYCODE_1 -> '1'
        KeyEvent.KEYCODE_2 -> '2'
        KeyEvent.KEYCODE_3 -> '3'
        KeyEvent.KEYCODE_4 -> '4'
        KeyEvent.KEYCODE_5 -> '5'
        KeyEvent.KEYCODE_6 -> '6'
        KeyEvent.KEYCODE_7 -> '7'
        KeyEvent.KEYCODE_8 -> '8'
        KeyEvent.KEYCODE_9 -> '9'
        KeyEvent.KEYCODE_STAR -> '*'
        KeyEvent.KEYCODE_POUND -> '#'
        else -> null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val currentCallState = CallService.callState.value
        val isCallActive = currentCallState == android.telecom.Call.STATE_ACTIVE
        // שיחה נכנסת שעדיין לא נענתה - "מצלצלת" - חייבת להישאר עד למענה/דחייה
        // דרך מסך השיחה עצמו. בלי הבדיקה הזו, ספרות ה-D-pad היו דולפות לשדה
        // החיוג הרגיל וקיצור החיוג (DPAD_CENTER/ENTER/CALL) היה יכול "לחטוף"
        // את הלחיצה שאמורה לענות לשיחה ולחייג בטעות למספר ישן שנשאר בשדה.
        val isCallRinging = currentCallState == android.telecom.Call.STATE_RINGING
        if (isCallActive) {
            dtmfDigitFor(keyCode)?.let { digit ->
                inCallViewModel.onDtmfDigitPressed(digit)
                return true
            }
        }

        // מקש הפעולה הפיזי (CALL) וניתוק/דחייה (ENDCALL) חייבים לעבוד גם כשמסך
        // השיחה הוא זה שממוקד - בטלפון פיצ'ר אמיתי אלה המקשים האינסטינקטיביים
        // למענה/ניתוק, ולא רק כפתור על המסך.
        when (keyCode) {
            KeyEvent.KEYCODE_CALL -> {
                if (isCallRinging) {
                    inCallViewModel.answer()
                    return true
                }
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

        // ספרות/כוכבית/סולמית/מחיקה מיועדות אך ורק לשדה החיוג של טאב החיוג עצמו -
        // בטאב אנשי קשר (או כל מסך אחר) יש להן משמעות מקומית (חיפוש וכו') ואסור
        // שהן "ידלפו" ברקע לתוך dialpadViewModel וייצרו מספר-רוח-רפאים.
        val isOnDialpadTab = currentRoute == Screen.Dialpad.route

        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                // אי אפשר "לצאת" ממסך שיחה מצלצלת בלי לענות/לדחות - בדיוק כמו בטלפון אמיתי.
                // שיחה פעילה כן אפשר לעזוב (היא ממשיכה ברקע, עם פס תזכורת במסכים האחרים).
                if (isCallRinging) {
                    return true
                }
                if (isOnDialpadTab && dialpadViewModel.dialedNumber.value.isNotEmpty()) {
                    dialpadViewModel.onDeletePressed()
                    return true
                }
            }
            KeyEvent.KEYCODE_0 -> if (isOnDialpadTab && !isCallRinging) dialpadViewModel.onDigitPressed("0")
            KeyEvent.KEYCODE_1 -> if (isOnDialpadTab && !isCallRinging) dialpadViewModel.onDigitPressed("1")
            KeyEvent.KEYCODE_2 -> if (isOnDialpadTab && !isCallRinging) dialpadViewModel.onDigitPressed("2")
            KeyEvent.KEYCODE_3 -> if (isOnDialpadTab && !isCallRinging) dialpadViewModel.onDigitPressed("3")
            KeyEvent.KEYCODE_4 -> if (isOnDialpadTab && !isCallRinging) dialpadViewModel.onDigitPressed("4")
            KeyEvent.KEYCODE_5 -> if (isOnDialpadTab && !isCallRinging) dialpadViewModel.onDigitPressed("5")
            KeyEvent.KEYCODE_6 -> if (isOnDialpadTab && !isCallRinging) dialpadViewModel.onDigitPressed("6")
            KeyEvent.KEYCODE_7 -> if (isOnDialpadTab && !isCallRinging) dialpadViewModel.onDigitPressed("7")
            KeyEvent.KEYCODE_8 -> if (isOnDialpadTab && !isCallRinging) dialpadViewModel.onDigitPressed("8")
            KeyEvent.KEYCODE_9 -> if (isOnDialpadTab && !isCallRinging) dialpadViewModel.onDigitPressed("9")
            KeyEvent.KEYCODE_STAR -> if (isOnDialpadTab && !isCallRinging) dialpadViewModel.onDigitPressed("*")
            KeyEvent.KEYCODE_POUND -> if (isOnDialpadTab && !isCallRinging) dialpadViewModel.onDigitPressed("#")
            KeyEvent.KEYCODE_DEL -> if (isOnDialpadTab && !isCallRinging) dialpadViewModel.onDeletePressed()
            KeyEvent.KEYCODE_CALL, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                // הקיצור "חייג את המספר שבשדה" לא אמור לפעול כשיש שיחה מצלצלת/פעילה -
                // אחרת הוא חוטף את לחיצת המענה למסך השיחה הנכנסת ומחייג בטעות.
                if (isOnDialpadTab && !isCallRinging && !isCallActive) {
                    val currentNumber = dialpadViewModel.dialedNumber.value
                    if (currentNumber.isNotEmpty()) {
                        makeRealCall(currentNumber)
                        return true
                    }
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (CallService.callState.value == android.telecom.Call.STATE_ACTIVE && dtmfDigitFor(keyCode) != null) {
            inCallViewModel.onDtmfDigitReleased()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}

@Composable
fun MainScreen(
    dialpadViewModel: DialpadViewModel,
    contactsViewModel: ContactsViewModel,
    inCallViewModel: InCallViewModel,
    checkIsDefaultDialer: () -> Boolean,
    onRequestDefaultDialer: () -> Unit,
    onMakeCall: (String) -> Unit,
    onRouteChanged: (String?) -> Unit = {}
) {
    // חוזרים מהדיאלוג של המערכת (בקשת ברירת מחדל) לא מפעילים מחדש את onCreate,
    // אז בלי לבדוק שוב ב-onResume נשארים תקועים במסך "הגדר כברירת מחדל" גם אחרי
    // שהמשתמש כן אישר - זה בדיוק מה שגרם לרשימת אנשי הקשר להיראות "ריקה" (בפועל
    // המסך איתה אף פעם לא הוצג).
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

    val context = androidx.compose.ui.platform.LocalContext.current
    val contactRepository = remember { ContactRepository(context) }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    LaunchedEffect(currentRoute) { onRouteChanged(currentRoute) }

    val activeCall by CallService.activeCall.collectAsState()

    // כל שיחה אמיתית - נכנסת או יוצאת - מגיעה מ-CallService. כשמופיעה שיחה חדשה,
    // עוברים אוטומטית למסך השיחה, בלי קשר לאיך היא הותחלה.
    LaunchedEffect(activeCall) {
        val call = activeCall
        if (call != null) {
            val number = call.details?.handle?.schemeSpecificPart ?: ""
            val name = contactRepository.findNameForNumber(number) ?: number
            navController.navigate(Screen.InCall.createRoute(name, number)) {
                launchSingleTop = true
            }
        }
    }

    val navItems = listOf(
        Triple(Screen.Dialpad, stringResource(R.string.nav_dial), Icons.Rounded.Dialpad),
        Triple(Screen.Contacts, stringResource(R.string.nav_contacts), Icons.Rounded.Contacts)
    )

    val showBottomBar = currentRoute != null && !currentRoute.startsWith("incall")
    val isOnCallScreen = currentRoute?.startsWith("incall") == true

    // שיחה פעילה שהמשתמש יצא ממנה (מיזעור) ממשיכה ברקע - פס תזכורת דק בראש שאר
    // המסכים מאפשר לחזור אליה בלי לחפש אותה בהיסטוריה.
    val ongoingCall = activeCall
    Scaffold(
        topBar = {
            if (ongoingCall != null && !isOnCallScreen) {
                val number = ongoingCall.details?.handle?.schemeSpecificPart ?: ""
                val callerName = remember(number) { contactRepository.findNameForNumber(number) ?: number }
                OngoingCallBanner(
                    name = callerName,
                    onReturn = {
                        navController.navigate(Screen.InCall.createRoute(callerName, number)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                // עיצוב מותאם לשפת הזכוכית הכהה של המערכת (רקע שקוף לגמרי + מסמן
                // בצבע ההדגשה) במקום ברירת המחדל של NavigationBar - שם עם surface
                // אטום וצבעי M3 גנריים היה בולט כ"אנדרואיד סטנדרטי" על רקע שאר
                // האפליקציה.
                NavigationBar(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ) {
                    navItems.forEach { (screen, label, icon) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = currentRoute == screen.route,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                unselectedTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            ),
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .then(
                    if (showBottomBar) {
                        Modifier
                            .onKeyEvent { event ->
                                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                                val currentIndex = navItems.indexOfFirst { it.first.route == currentRoute }
                                if (currentIndex < 0) return@onKeyEvent false
                                val nextIndex = when (event.key) {
                                    Key.DirectionRight -> currentIndex - 1
                                    Key.DirectionLeft -> currentIndex + 1
                                    else -> return@onKeyEvent false
                                }
                                if (nextIndex !in navItems.indices) return@onKeyEvent false
                                navController.navigate(navItems[nextIndex].first.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                                true
                            }
                    } else Modifier
                )
        ) {
            NavHost(navController, startDestination = Screen.Dialpad.route) {
                composable(Screen.Dialpad.route) {
                    DialpadScreen(dialpadViewModel) { _, number ->
                        onMakeCall(number)
                    }
                }
                composable(Screen.Contacts.route) {
                    ContactsScreen(contactsViewModel) { _, number ->
                        onMakeCall(number)
                    }
                }

                composable(
                    route = Screen.InCall.route,
                    arguments = listOf(
                        navArgument("name") { type = NavType.StringType },
                        navArgument("number") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val name = backStackEntry.arguments?.getString("name") ?: stringResource(R.string.unknown)
                    val number = backStackEntry.arguments?.getString("number") ?: ""
                    InCallScreen(
                        name = name,
                        phoneNumber = number,
                        viewModel = inCallViewModel,
                        onCallEnded = { navController.popBackStack() },
                        onMinimize = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
private fun OngoingCallBanner(name: String, onReturn: () -> Unit) {
    com.future.sharednav.focus.FocusableItem(
        onClick = onReturn,
        accentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.fillMaxWidth(),
        idleBackgroundColor = MaterialTheme.colorScheme.primary,
        focusedBackgroundColor = MaterialTheme.colorScheme.primary,
        cornerRadius = 0.dp,
        scaleOnFocus = false,
        contentPadding = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Call,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${stringResource(R.string.ongoing_call)} · $name",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.tap_to_return),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun DefaultDialerRequiredScreen(onRequest: () -> Unit) {
    // היה משתמש ב-Color.Black/White קשיחים ו-Button ברירת מחדל בלי שום עיצוב -
    // המסך היחיד באפליקציה שעקף את DialerTheme לגמרי, אז לא עקב אחרי מצב
    // כהה/בהיר או צבע ההדגשה של המשתמש כמו כל שאר האפליקציה.
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text(
                text = stringResource(R.string.default_dialer_required),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRequest,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) { Text(stringResource(R.string.set_as_default_dialer)) }
        }
    }
}
