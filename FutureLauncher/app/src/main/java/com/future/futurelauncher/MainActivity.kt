package com.future.futurelauncher
import com.future.sharednav.focus.bringIntoViewOnFocus

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent as AndroidKeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.future.futurelauncher.ui.*
import com.future.futurelauncher.ui.theme.FutureLauncherTheme
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית. לא פוגע בניווט/הפעלה במקשים -
    // dispatchKeyEvent הוא נתיב נפרד לגמרי מ-dispatchTouchEvent.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var appWidgetHost: AppWidgetHost
    private val APPWIDGET_HOST_ID = 1024

    private lateinit var pickWidgetLauncher: ActivityResultLauncher<Intent>
    private lateinit var configWidgetLauncher: ActivityResultLauncher<Intent>

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetHost = AppWidgetHost(this, APPWIDGET_HOST_ID)
        pruneOrphanedWidgetIds()

        pickWidgetLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val appWidgetId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
                if (appWidgetId != -1) {
                    configureWidget(appWidgetId)
                }
            } else {
                val appWidgetId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
                if (appWidgetId != -1) appWidgetHost.deleteAppWidgetId(appWidgetId)
            }
        }

        configWidgetLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val appWidgetId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
                if (appWidgetId != -1) {
                    completeAddWidget(appWidgetId)
                }
            } else {
                val appWidgetId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
                if (appWidgetId != -1) appWidgetHost.deleteAppWidgetId(appWidgetId)
            }
        }

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER,
            WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        
        setContent {
            FutureLauncherTheme {
                LauncherScreen(viewModel, onSelectWidget = { selectWidget() })
            }
        }
    }

    fun getAppWidgetHost() = appWidgetHost
    fun getAppWidgetManager() = appWidgetManager

    /**
     * מוחק מהמארח (AppWidgetHost) כל widgetId ש-AppWidgetManager כבר לא מכיר
     * (הספק שלו - App Widget Provider - הוסר או הושבת). "רפאים" כאלה נשארים
     * רשומים בשירות המערכת גם אחרי שהספק נעלם, ו-AppWidgetHost.stopListening/
     * startListening (שרצות בכל onStop/onStart, כלומר בכל פעם שנפתחת אפליקציה)
     * מנסות לחשב UID עבור כל widget רשום כולל הרפאים - NPE בצד השרת שמתפרץ
     * כ-RuntimeException לא-נתפסת וקורס את הלאנצ'ר (ראו onStop למטה).
     */
    private fun pruneOrphanedWidgetIds() {
        appWidgetHost.appWidgetIds.forEach { id ->
            if (appWidgetManager.getAppWidgetInfo(id) == null) {
                appWidgetHost.deleteAppWidgetId(id)
            }
        }
    }

    private var packageChangeReceiver: android.content.BroadcastReceiver? = null

    override fun onStart() {
        super.onStart()
        // הגנה משנית ל-pruneOrphanedWidgetIds (למקרה שספק נהיה "רפאים" בדיוק
        // בזמן שהלאנצ'ר ברקע, בין ה-onCreate האחרון להפעלה הבאה) - לא אמורה
        // לתפוס כלום בפועל, אבל startListening היא אותה קריאת בינדר בדיוק
        // שיכולה לקרוס באותה צורה אם בכל זאת נותר widget רפאים.
        try {
            appWidgetHost.startListening()
        } catch (e: Exception) {
            pruneOrphanedWidgetIds()
        }

        // אפליקציות שהותקנו/הוסרו/הוחלפו אחרי הטעינה הראשונית של הרשימה חייבות
        // לגרום לרענון שלה, אחרת אפליקציה חדשה לעולם לא תופיע ואפליקציה שהוסרה
        // תמשיך להיראות "מותקנת" (וקריסה בלחיצה עליה - ראו fix מס' 1).
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context?, intent: Intent?) {
                val packageName = intent?.data?.schemeSpecificPart
                if (intent?.action == Intent.ACTION_PACKAGE_REMOVED || intent?.action == Intent.ACTION_PACKAGE_REPLACED) {
                    if (packageName != null) {
                        com.future.futurelauncher.ui.evictAppIconCache(packageName)
                    }
                }
                viewModel.loadData()
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        registerReceiver(receiver, filter)
        packageChangeReceiver = receiver
    }

    override fun onStop() {
        super.onStop()
        // ראו pruneOrphanedWidgetIds - זו הייתה נקודת הקריסה בפועל: stopListening
        // רץ בכל onStop, כלומר כל פעם שנפתחת אפליקציה מהלאנצ'ר, ו-RemoteException
        // מ-widget רפאים הופכת ל-"FutureLauncher stopped working" בלתי נתפסת.
        try {
            appWidgetHost.stopListening()
        } catch (e: Exception) {
            pruneOrphanedWidgetIds()
        }
        packageChangeReceiver?.let { unregisterReceiver(it) }
        packageChangeReceiver = null
    }

    private fun selectWidget() {
        val appWidgetId = appWidgetHost.allocateAppWidgetId()
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }

        // כשההגבלה ל"אפליקציות ברירת מחדל בלבד" מופעלת, מגבילים את בורר
        // הווידג'טים של המערכת עצמו לרשימה מסוננת דרך EXTRA_CUSTOM_INFO/EXTRA_CUSTOM_EXTRAS
        // (ה-API הרשמי של AppWidgetManager.ACTION_APPWIDGET_PICK להצגת תת-קבוצה בלבד),
        // במקום לתת לבורר להציג ווידג'טים מכל אפליקציה מותקנת.
        if (viewModel.restrictToDefaultApps) {
            val allowedProviders = appWidgetManager.installedProviders.filter {
                com.future.futurelauncher.DefaultApps.isDefault(it.provider.packageName)
            }
            val customInfo = ArrayList(allowedProviders)
            val customExtras = ArrayList(allowedProviders.map { android.os.Bundle() })
            pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo)
            pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras)
        }

        pickWidgetLauncher.launch(pickIntent)
    }

    private fun configureWidget(appWidgetId: Int) {
        val appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId) ?: return
        if (appWidgetInfo.configure != null) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = appWidgetInfo.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            configWidgetLauncher.launch(intent)
        } else {
            completeAddWidget(appWidgetId)
        }
    }

    private fun completeAddWidget(appWidgetId: Int) {
        // Find the current page to add the widget to
        // This is a bit tricky without direct access to pager state here, but we can assume ViewModel knows or just pass 0
        // A better way is for LauncherScreen to observe a "PendingWidget" state in ViewModel.
        viewModel.addWidget(appWidgetId, viewModel.homePageIndex)
    }
}

@Composable
fun LauncherScreen(viewModel: LauncherViewModel, onSelectWidget: () -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager
    val focusRequester = remember { FocusRequester() }

    val appWidgetHost = remember { (context as MainActivity).getAppWidgetHost() }

    val pages = viewModel.pages
    val pagerState = rememberPagerState(
        initialPage = viewModel.homePageIndex.coerceIn(0, maxOf(0, pages.size - 1)),
        pageCount = { maxOf(1, pages.size) }
    )

    val editModeScale by animateFloatAsState(if (viewModel.isEditMode) 0.95f else 1f)
    val scope = rememberCoroutineScope()
    var menuClickJob by remember { mutableStateOf<Job?>(null) }
    var lastMenuKeyUpTime by remember { mutableStateOf(0L) }
    var centerLongPressJob by remember { mutableStateOf<Job?>(null) }

    val currentPage = pagerState.currentPage
    val currentItems = pages.getOrNull(currentPage) ?: emptyList()
    val theme = viewModel.theme

    // מרענן את העיצוב המשותף (כהה/בהיר, צבע הדגשה) בכל חזרה למסך, כדי
    // שלשינויים שנעשו באפליקציית ההגדרות תהיה השפעה מיידית על הלאנצ'ר.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadTheme()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // מקש Options הפיזי תמיד נחסם ברמת המערכת (FutureUI's StatusBarAccessibilityService
    // צורך אותו ללחיצה ארוכה ל"אפליקציות אחרונות") - שום KEYCODE_MENU לא באמת מגיע
    // לאפליקציה, אז הענף המקביל תחת onKeyEvent למטה אף פעם לא היה יורה (ראו אותה
    // הערה ב-Music/MusicNavHost.kt). לחיצה קצרה משודרת בשידור גלובלי
    // (ACTION_OPTIONS_SHORT_PRESS) - מכאן מגיע זיהוי הלחיצה הכפולה בפועל.
    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context?, intent: Intent?) {
                val now = SystemClock.elapsedRealtime()
                val isDoubleClick = now - lastMenuKeyUpTime < 350L
                lastMenuKeyUpTime = now

                if (isDoubleClick) {
                    menuClickJob?.cancel()
                    menuClickJob = null
                    lastMenuKeyUpTime = 0L
                    viewModel.isEditMode = !viewModel.isEditMode
                    if (viewModel.isEditMode) {
                        viewModel.editModeSelectedIndex = 0
                        viewModel.isTopBarFocused = false
                        viewModel.isLeftPlusFocused = false
                        viewModel.isRightPlusFocused = false
                        viewModel.isEditModeBottomBarFocused = true
                    } else {
                        viewModel.isTopBarFocused = false
                        viewModel.isLeftPlusFocused = false
                        viewModel.isRightPlusFocused = false
                        viewModel.isEditModeBottomBarFocused = false
                    }
                } else {
                    menuClickJob?.cancel()
                    menuClickJob = scope.launch {
                        delay(350)
                        if (!viewModel.isEditMode) {
                            val freshItems = viewModel.pages.getOrNull(pagerState.currentPage) ?: emptyList()
                            val item = freshItems.getOrNull(viewModel.focusedIndex)
                            if (item != null) viewModel.dialogState = LauncherDialog.AppOptions(item)
                        }
                        menuClickJob = null
                    }
                }
            }
        }
        val filter = android.content.IntentFilter("com.future.futureui.ACTION_OPTIONS_SHORT_PRESS")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, android.content.Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose { context.unregisterReceiver(receiver) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable().bringIntoViewOnFocus()
            .onKeyEvent { keyEvent ->
                val index = viewModel.focusedIndex

                if (keyEvent.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
                    keyEvent.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_ENTER) {
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        if (keyEvent.nativeKeyEvent.repeatCount == 0 && centerLongPressJob == null) {
                            centerLongPressJob = scope.launch {
                                delay(800)
                                if (!viewModel.isEditMode) {
                                    viewModel.isReorderMode = !viewModel.isReorderMode
                                }
                                centerLongPressJob = null
                            }
                        }
                    } else if (keyEvent.type == KeyEventType.KeyUp) {
                        if (centerLongPressJob?.isActive == true) {
                            centerLongPressJob?.cancel()
                            centerLongPressJob = null
                            
                            if (viewModel.isReorderMode) {
                                viewModel.isReorderMode = false
                            } else if (viewModel.isTopBarFocused) {
                                if (viewModel.topBarSelectedIndex == 0) { // Home
                                    viewModel.homePageIndex = currentPage
                                    viewModel.savePages()
                                } else { // Trash
                                    viewModel.removePage(currentPage)
                                }
                            } else if (viewModel.isLeftPlusFocused) {
                                viewModel.addPage(currentPage)
                            } else if (viewModel.isRightPlusFocused) {
                                viewModel.addPage(currentPage + 1)
                            } else if (viewModel.isEditMode && viewModel.isEditModeBottomBarFocused) {
                                when (viewModel.editModeSelectedIndex) {
                                    0 -> viewModel.dialogState = LauncherDialog.LauncherSettings
                                    1 -> viewModel.dialogState = LauncherDialog.Widgets
                                    2 -> {
                                        context.startActivity(Intent(Intent.ACTION_SET_WALLPAPER))
                                    }
                                    3 -> viewModel.dialogState = LauncherDialog.AppList
                                }
                            } else if (viewModel.isEditMode) {
                                // No functional button focused in Edit Mode, do nothing
                                // (grid interaction is intentionally disabled while editing)
                            } else {
                                val item = currentItems.getOrNull(index)
                                when (item) {
                                    is LauncherItem.App -> {
                                        try {
                                            context.startActivity(item.resolveInfo.launchIntent())
                                        } catch (e: android.content.ActivityNotFoundException) {
                                            Toast.makeText(context, context.getString(R.string.app_no_longer_installed), Toast.LENGTH_SHORT).show()
                                            viewModel.loadData()
                                        } catch (e: SecurityException) {
                                            Toast.makeText(context, context.getString(R.string.app_no_longer_installed), Toast.LENGTH_SHORT).show()
                                            viewModel.loadData()
                                        }
                                    }
                                    is LauncherItem.Folder -> viewModel.dialogState = LauncherDialog.FolderView(item)
                                    is LauncherItem.Empty -> {
                                        if (item.isOccupiedBy == null) {
                                            viewModel.dialogState = LauncherDialog.EmptySlotOptions(currentPage, index)
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        }
                        centerLongPressJob = null
                    }
                    return@onKeyEvent true
                }

                if (keyEvent.type == KeyEventType.KeyDown) {
                    if (viewModel.isEditMode) {
                        when (keyEvent.nativeKeyEvent.keyCode) {
                            AndroidKeyEvent.KEYCODE_BACK -> {
                                // חייב לאפס בדיוק כמו יציאה מאדיט מוד בלחיצה כפולה (למטה) -
                                // בלעדיו הדגלים האלה נשארים "תקועים" true, וב-DPAD_DOWN/UP/
                                // LEFT/RIGHT הבא במצב רגיל אחד מהם תופס במפתיע ובולם את
                                // הניווט (למשל isEditModeBottomBarFocused גורם ל"כבר בפס
                                // התחתון, לא לעשות כלום" גם כשכבר יצאנו מאדיט מוד).
                                viewModel.isEditMode = false
                                viewModel.isTopBarFocused = false
                                viewModel.isLeftPlusFocused = false
                                viewModel.isRightPlusFocused = false
                                viewModel.isEditModeBottomBarFocused = false
                                return@onKeyEvent true
                            }
                            // באדיט מוד הפוקוס נשאר לגמרי במסגרת סביב הרשת (Home/Trash,
                            // שני כפתורי ה-+, הפס התחתון) ולעולם לא נכנס לתוך הרשת עצמה -
                            // בכוונה, כדי שלא יהיה מוזר לראות מסגרת פוקוס על תוכן המסך
                            // (אפליקציה/וידג'ט/תיקייה) בזמן שהמשתמש בעצם עורך את הפריסה,
                            // לא בוחר להריץ אפליקציה.
                            AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                                when {
                                    viewModel.isTopBarFocused -> {
                                        val target = viewModel.topBarSelectedIndex
                                        viewModel.isTopBarFocused = false
                                        if (target == 0) viewModel.isLeftPlusFocused = true
                                        else viewModel.isLeftPlusFocused = true // Per user request: Down from Trash to Left Plus
                                    }
                                    viewModel.isLeftPlusFocused -> {
                                        viewModel.isLeftPlusFocused = false
                                        viewModel.isEditModeBottomBarFocused = true
                                        viewModel.editModeSelectedIndex = 3
                                    }
                                    viewModel.isRightPlusFocused -> {
                                        viewModel.isRightPlusFocused = false
                                        viewModel.isEditModeBottomBarFocused = true
                                        viewModel.editModeSelectedIndex = 0
                                    }
                                }
                                return@onKeyEvent true
                            }
                            AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                                when {
                                    viewModel.isEditModeBottomBarFocused -> {
                                        viewModel.isEditModeBottomBarFocused = false
                                        if (viewModel.editModeSelectedIndex >= 2) viewModel.isLeftPlusFocused = true
                                        else viewModel.isRightPlusFocused = true
                                    }
                                    viewModel.isLeftPlusFocused -> {
                                        viewModel.isLeftPlusFocused = false
                                        viewModel.isTopBarFocused = true
                                        viewModel.topBarSelectedIndex = 1
                                    }
                                    viewModel.isRightPlusFocused -> {
                                        viewModel.isRightPlusFocused = false
                                        viewModel.isTopBarFocused = true
                                        viewModel.topBarSelectedIndex = 0
                                    }
                                }
                                return@onKeyEvent true
                            }
                            AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                                when {
                                    viewModel.isTopBarFocused -> {
                                        if (viewModel.topBarSelectedIndex == 1) { // Trash
                                            viewModel.isTopBarFocused = false
                                            viewModel.isLeftPlusFocused = true
                                        } else { // Home
                                            viewModel.topBarSelectedIndex = 1
                                        }
                                    }
                                    viewModel.isEditModeBottomBarFocused -> {
                                        if (viewModel.editModeSelectedIndex < 3) viewModel.editModeSelectedIndex++
                                        else {
                                            viewModel.isEditModeBottomBarFocused = false
                                            viewModel.isLeftPlusFocused = true
                                        }
                                    }
                                    viewModel.isRightPlusFocused -> {
                                        viewModel.isRightPlusFocused = false
                                        viewModel.isTopBarFocused = true
                                        viewModel.topBarSelectedIndex = 0
                                    }
                                }
                                return@onKeyEvent true
                            }
                            AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                                when {
                                    viewModel.isTopBarFocused -> {
                                        if (viewModel.topBarSelectedIndex == 1) { // Trash
                                            viewModel.topBarSelectedIndex = 0
                                        } else { // Home
                                            viewModel.isTopBarFocused = false
                                            viewModel.isRightPlusFocused = true
                                        }
                                    }
                                    viewModel.isEditModeBottomBarFocused -> {
                                        if (viewModel.editModeSelectedIndex > 0) viewModel.editModeSelectedIndex--
                                        else {
                                            viewModel.isEditModeBottomBarFocused = false
                                            viewModel.isRightPlusFocused = true
                                        }
                                    }
                                    viewModel.isLeftPlusFocused -> {
                                        viewModel.isLeftPlusFocused = false
                                        viewModel.isTopBarFocused = true
                                        viewModel.topBarSelectedIndex = 1
                                    }
                                }
                                return@onKeyEvent true
                            }
                        }
                    }

                    when (keyEvent.nativeKeyEvent.keyCode) {
                        AndroidKeyEvent.KEYCODE_BACK -> {
                            if (viewModel.isReorderMode) {
                                viewModel.isReorderMode = false
                                return@onKeyEvent true
                            }
                            if (viewModel.dialogState !is LauncherDialog.None) {
                                viewModel.dialogState = LauncherDialog.None
                                return@onKeyEvent true
                            }
                            // הלאנצ'ר הוא "דף הבית" של המכשיר - אסור לצריכת המקש הזו
                            // ליפול ל-fallback של המערכת (שעלול לסיים/למזער את ה-Activity
                            // של הלאנצ'ר עצמו ולהשאיר את המשתמש בלי מסך בית). אם לא נמצאים
                            // בדף הבית המוגדר, "חזור" מחזיר אליו - בדיוק כמו ברוב הלאנצ'רים;
                            // אם כבר בדף הבית, המקש נבלע ולא עושה כלום.
                            if (currentPage != viewModel.homePageIndex) {
                                scope.launch { pagerState.animateScrollToPage(viewModel.homePageIndex) }
                            }
                            return@onKeyEvent true
                        }
                        AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (viewModel.isTopBarFocused) {
                                viewModel.isTopBarFocused = false
                                // Enter grid at roughly same horizontal position
                                viewModel.focusedIndex = if (viewModel.topBarSelectedIndex == 0) 1 else 2
                            } else if (viewModel.isLeftPlusFocused || viewModel.isRightPlusFocused) {
                                viewModel.isLeftPlusFocused = false
                                viewModel.isRightPlusFocused = false
                                viewModel.focusedIndex = 4 // Middle of grid
                            } else if (viewModel.isEditModeBottomBarFocused) {
                                // Already in bottom bar, do nothing or wrap
                            } else if (viewModel.isReorderMode) {
                                if (index + 4 < currentItems.size) viewModel.swapItems(currentPage, index, index + 4)
                            } else {
                                val item = currentItems.getOrNull(index)
                                val skip = if (item is LauncherItem.Widget) item.spanY else 1
                                var nextIndex = index + skip * 4
                                while (nextIndex < currentItems.size) {
                                    val nextItem = currentItems[nextIndex]
                                    if (nextItem is LauncherItem.Empty && nextItem.isOccupiedBy != null) nextIndex += 4
                                    else break
                                }
                                if (nextIndex < currentItems.size) {
                                    viewModel.focusedIndex = nextIndex
                                } else if (viewModel.isEditMode) {
                                    // At bottom row in edit mode, go to Top Bar
                                    viewModel.isTopBarFocused = true
                                    viewModel.topBarSelectedIndex = if (index % 4 < 2) 0 else 1
                                } else {
                                    // At bottom row in regular mode, wrap to top row
                                    val column = index % 4
                                    var wrapIndex = column
                                    while (wrapIndex < index) {
                                        val wrapItem = currentItems.getOrNull(wrapIndex)
                                        if (wrapItem is LauncherItem.Empty && wrapItem.isOccupiedBy != null) wrapIndex += 4
                                        else break
                                    }
                                    if (wrapIndex in currentItems.indices && wrapIndex < index) viewModel.focusedIndex = wrapIndex
                                }
                            }
                            return@onKeyEvent true
                        }
                        AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                            if (viewModel.isReorderMode) {
                                if (index - 4 >= 0) viewModel.swapItems(currentPage, index, index - 4)
                            } else if (currentItems.isNotEmpty()) {
                                var nextIndex = index - 4
                                while (nextIndex >= 0) {
                                    val nextItem = currentItems[nextIndex]
                                    if (nextItem is LauncherItem.Empty && nextItem.isOccupiedBy != null) nextIndex -= 4
                                    else break
                                }
                                if (nextIndex >= 0) {
                                    viewModel.focusedIndex = nextIndex
                                } else if (viewModel.isEditMode) {
                                    // At top row in edit mode, go to Top Bar
                                    viewModel.isTopBarFocused = true
                                    viewModel.topBarSelectedIndex = if (index % 4 < 2) 0 else 1
                                } else {
                                    // At top row in regular mode, wrap to bottom row
                                    val column = index % 4
                                    var wrapIndex = ((currentItems.size - 1) / 4) * 4 + column
                                    while (wrapIndex > index) {
                                        val wrapItem = currentItems.getOrNull(wrapIndex)
                                        if (wrapItem == null || (wrapItem is LauncherItem.Empty && wrapItem.isOccupiedBy != null)) wrapIndex -= 4
                                        else break
                                    }
                                    if (wrapIndex in currentItems.indices && wrapIndex > index) viewModel.focusedIndex = wrapIndex
                                }
                            }
                            return@onKeyEvent true
                        }
                        AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (viewModel.isTopBarFocused) {
                                if (viewModel.topBarSelectedIndex < 1) viewModel.topBarSelectedIndex++
                                else {
                                    viewModel.isTopBarFocused = false
                                    viewModel.isRightPlusFocused = true
                                }
                            } else if (viewModel.isLeftPlusFocused) {
                                viewModel.isLeftPlusFocused = false
                                viewModel.isTopBarFocused = true
                                viewModel.topBarSelectedIndex = 0
                            } else if (viewModel.isReorderMode) {
                                if (index % 4 == 3) {
                                    if (currentPage < pages.size - 1) {
                                        viewModel.moveItemBetweenPages(currentPage, index, currentPage + 1, index - 3)
                                        viewModel.focusedIndex = index - 3
                                        scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                                    }
                                } else if (index + 1 < currentItems.size) {
                                    viewModel.swapItems(currentPage, index, index + 1)
                                }
                            } else {
                                if (index % 4 == 3) {
                                    if (currentPage < pages.size - 1) scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                                } else if (index + 1 < currentItems.size) {
                                    val item = currentItems.getOrNull(index)
                                    val skip = if (item is LauncherItem.Widget) item.spanX else 1
                                    var nextIndex = index + skip
                                    while (nextIndex < currentItems.size && nextIndex % 4 != 0) {
                                        val nextItem = currentItems[nextIndex]
                                        if (nextItem is LauncherItem.Empty && nextItem.isOccupiedBy != null) nextIndex++
                                        else break
                                    }
                                    if (nextIndex < currentItems.size) viewModel.focusedIndex = nextIndex
                                }
                            }
                            return@onKeyEvent true
                        }
                        AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (viewModel.isTopBarFocused) {
                                if (viewModel.topBarSelectedIndex > 0) viewModel.topBarSelectedIndex--
                                else {
                                    viewModel.isTopBarFocused = false
                                    viewModel.isLeftPlusFocused = true
                                }
                            } else if (viewModel.isRightPlusFocused) {
                                viewModel.isRightPlusFocused = false
                                viewModel.isTopBarFocused = true
                                viewModel.topBarSelectedIndex = 1
                            } else if (viewModel.isReorderMode) {
                                if (index % 4 == 0) {
                                    if (currentPage > 0) {
                                        viewModel.moveItemBetweenPages(currentPage, index, currentPage - 1, index + 3)
                                        viewModel.focusedIndex = index + 3
                                        scope.launch { pagerState.animateScrollToPage(currentPage - 1) }
                                    }
                                } else if (index > 0) {
                                    viewModel.swapItems(currentPage, index, index - 1)
                                }
                            } else {
                                if (index % 4 == 0) {
                                    if (currentPage > 0) scope.launch { pagerState.animateScrollToPage(currentPage - 1) }
                                } else if (index > 0) {
                                    var nextIndex = index - 1
                                    while (nextIndex >= 0 && nextIndex % 4 != 3) {
                                        val nextItem = currentItems[nextIndex]
                                        if (nextItem is LauncherItem.Empty && nextItem.isOccupiedBy != null) nextIndex--
                                        else break
                                    }
                                    if (nextIndex >= 0) viewModel.focusedIndex = nextIndex
                                }
                            }
                            return@onKeyEvent true
                        }
                        else -> return@onKeyEvent false
                    }
                }
                return@onKeyEvent false
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (viewModel.isEditMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // תמיד Icons.Rounded.Home (לא Icons.Outlined) - הגרסה החלולה עם
                    // הקווים הדקים והפינות החדות בלטה לרעה ליד שאר האייקונים המעוגלים
                    // באפליקציה. "זה עמוד הבית הנוכחי" מסומן עכשיו באטימות מלאה במקום
                    // בהחלפת האייקון עצמו.
                    val isCurrentHome = currentPage == viewModel.homePageIndex
                    val isHomeFocused = viewModel.isTopBarFocused && viewModel.topBarSelectedIndex == 0
                    val homeColor = if (isHomeFocused) theme.accentColor else if (isCurrentHome) OnWallpaperColor else OnWallpaperColor.copy(alpha = 0.5f)
                    Icon(
                        imageVector = Icons.Rounded.Home,
                        contentDescription = stringResource(R.string.home), 
                        tint = homeColor, 
                        modifier = Modifier.size(32.dp).graphicsLayer {
                            scaleX = if (isHomeFocused) 1.2f else 1f
                            scaleY = if (isHomeFocused) 1.2f else 1f
                        }
                    )
                    Spacer(modifier = Modifier.width(64.dp))
                    val trashColor = if (viewModel.isTopBarFocused && viewModel.topBarSelectedIndex == 1) theme.accentColor else OnWallpaperColor
                    Icon(
                        imageVector = Icons.Rounded.Delete, 
                        contentDescription = stringResource(R.string.trash), 
                        tint = trashColor, 
                        modifier = Modifier.size(32.dp).graphicsLayer {
                            scaleX = if (viewModel.isTopBarFocused && viewModel.topBarSelectedIndex == 1) 1.2f else 1f
                            scaleY = if (viewModel.isTopBarFocused && viewModel.topBarSelectedIndex == 1) 1.2f else 1f
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (viewModel.isEditMode) {
                    val leftPlusColor = if (viewModel.isLeftPlusFocused) theme.accentColor else OnWallpaperColor
                    Icon(
                        Icons.Rounded.Add, 
                        contentDescription = null, 
                        tint = leftPlusColor,
                        modifier = Modifier.size(48.dp).padding(8.dp).graphicsLayer {
                            scaleX = if (viewModel.isLeftPlusFocused) 1.2f else 1f
                            scaleY = if (viewModel.isLeftPlusFocused) 1.2f else 1f
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(if (viewModel.isEditMode) 4.dp else 0.dp)
                        .then(
                            if (viewModel.isEditMode) Modifier.fillMaxWidth(0.85f).align(Alignment.CenterVertically)
                            else Modifier.fillMaxSize()
                        )
                        .graphicsLayer {
                            scaleX = editModeScale
                            scaleY = editModeScale
                            alpha = if (viewModel.isEditMode) 0.85f else 1f
                        }
                        .then(
                            if (viewModel.isEditMode) {
                                Modifier
                                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                                    .border(2.dp, OnWallpaperColor.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                            } else Modifier
                        )
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = false,
                        // מכין מראש את הדף הבא/הקודם (טעינת אייקונים כלולה) כדי
                        // שהמעבר לא "ייתקע" ברגע שהוא מתחיל - זו הייתה הסיבה
                        // המרכזית לתחושת האיטיות.
                        beyondViewportPageCount = 1
                    ) { page ->
                        val pageItems = pages.getOrNull(page) ?: emptyList()

                        FixedLauncherGrid(
                            items = pageItems,
                            contentPadding = PaddingValues(
                                top = if (viewModel.isEditMode) 7.dp else 30.dp,
                                start = if (viewModel.isEditMode) 4.dp else 16.dp,
                                end = if (viewModel.isEditMode) 4.dp else 16.dp,
                                bottom = if (viewModel.isEditMode) 4.dp else 30.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy((if (viewModel.isEditMode) 2.dp else 8.dp)),
                            verticalArrangement = Arrangement.spacedBy(if (viewModel.isEditMode) 7.dp else 20.dp),
                            modifier = Modifier.fillMaxSize()
                        ) { indexInPage, item ->
                            ItemPanel(
                                item = item,
                                pm = pm,
                                isEditMode = viewModel.isEditMode,
                                isFocused = indexInPage == viewModel.focusedIndex && !viewModel.isEditMode && !viewModel.isTopBarFocused && !viewModel.isLeftPlusFocused && !viewModel.isRightPlusFocused,
                                isMoving = (indexInPage == viewModel.focusedIndex && viewModel.isReorderMode),
                                appWidgetHost = appWidgetHost,
                                theme = theme
                            )
                        }
                    }
                }

                if (viewModel.isEditMode) {
                    val rightPlusColor = if (viewModel.isRightPlusFocused) theme.accentColor else OnWallpaperColor
                    Icon(
                        Icons.Rounded.Add, 
                        contentDescription = null, 
                        tint = rightPlusColor,
                        modifier = Modifier.size(48.dp).padding(8.dp).graphicsLayer {
                            scaleX = if (viewModel.isRightPlusFocused) 1.2f else 1f
                            scaleY = if (viewModel.isRightPlusFocused) 1.2f else 1f
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (viewModel.isEditMode) 80.dp else 50.dp),
                contentAlignment = Alignment.Center
            ) {
                if (viewModel.isEditMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color.Black.copy(alpha = 0.35f),
                                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            )
                            .border(
                                1.dp,
                                OnWallpaperColor.copy(alpha = 0.15f),
                                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            )
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EditModeButton(stringResource(R.string.settings), Icons.Rounded.Settings, viewModel.isEditModeBottomBarFocused && viewModel.editModeSelectedIndex == 0, theme = theme) {
                            viewModel.dialogState = LauncherDialog.LauncherSettings
                        }
                        EditModeButton(stringResource(R.string.widgets), Icons.Rounded.Widgets, viewModel.isEditModeBottomBarFocused && viewModel.editModeSelectedIndex == 1, theme = theme) {
                            viewModel.dialogState = LauncherDialog.Widgets
                        }
                        EditModeButton(stringResource(R.string.wallpaper), Icons.Rounded.Wallpaper, viewModel.isEditModeBottomBarFocused && viewModel.editModeSelectedIndex == 2, theme = theme) {
                            context.startActivity(Intent(Intent.ACTION_SET_WALLPAPER))
                        }
                        EditModeButton(stringResource(R.string.apps), Icons.Rounded.Apps, viewModel.isEditModeBottomBarFocused && viewModel.editModeSelectedIndex == 3, theme = theme) {
                            viewModel.dialogState = LauncherDialog.AppList
                        }
                    }
                } else if (pages.size > 1) {
                    Row(horizontalArrangement = Arrangement.Center) {
                        repeat(pages.size) { iteration ->
                            val color = if (pagerState.currentPage == iteration) OnWallpaperColor else OnWallpaperColor.copy(alpha = 0.4f)
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(color)
                                    .size(6.dp)
                            )
                        }
                    }
                }
            }
        }

        when (val dialog = viewModel.dialogState) {
            is LauncherDialog.AppOptions -> {
                AppOptionsDialog(
                    item = dialog.item,
                    onAddToFolder = { folderName ->
                        val newPages = viewModel.pages.toMutableList()
                        val currentPageItems = newPages[currentPage].toMutableList()
                        val itemToMove = dialog.item as? LauncherItem.App
                        if (itemToMove != null) {
                            val originalIndex = currentPageItems.indexOf(itemToMove)
                            val existingFolder = currentPageItems.find { it is LauncherItem.Folder && it.label == folderName } as? LauncherItem.Folder
                            if (existingFolder != null) {
                                existingFolder.apps.add(itemToMove)
                                currentPageItems[originalIndex] = LauncherItem.Empty()
                            } else {
                                val newFolder = LauncherItem.Folder(
                                    id = "folder:$folderName",
                                    label = folderName
                                )
                                newFolder.apps.add(itemToMove)
                                currentPageItems[originalIndex] = newFolder
                            }
                            newPages[currentPage] = currentPageItems
                            viewModel.pages = newPages
                            viewModel.savePages()
                        }
                        viewModel.dialogState = LauncherDialog.None
                    },
                    onRename = { newName ->
                        val newPages = viewModel.pages.toMutableList()
                        val currentPageItems = newPages[currentPage].toMutableList()
                        val itemToRename = dialog.item
                        itemToRename.customLabel = newName
                        newPages[currentPage] = currentPageItems
                        viewModel.pages = newPages
                        viewModel.savePages()
                        viewModel.dialogState = LauncherDialog.None
                    },
                    onResize = { spanX, spanY ->
                        val newPages = viewModel.pages.toMutableList()
                        val currentPageItems = newPages[currentPage].toMutableList()
                        val itemIndex = currentPageItems.indexOf(dialog.item)
                        if (itemIndex != -1 && dialog.item is LauncherItem.Widget) {
                            val widget = dialog.item
                            widget.spanX = spanX
                            widget.spanY = spanY
                            currentPageItems.forEachIndexed { idx, it ->
                                if (it is LauncherItem.Empty && it.isOccupiedBy == widget.id) {
                                    currentPageItems[idx] = LauncherItem.Empty()
                                }
                            }
                            for (y in 0 until spanY) {
                                for (x in 0 until spanX) {
                                    if (x == 0 && y == 0) continue
                                    val occupiedIndex = itemIndex + y * 4 + x
                                    if (occupiedIndex < currentPageItems.size) {
                                        currentPageItems[occupiedIndex] = LauncherItem.Empty(id = "occupied:${widget.id}:$occupiedIndex").apply {
                                            isOccupiedBy = widget.id
                                        }
                                    }
                                }
                            }
                            newPages[currentPage] = currentPageItems
                            viewModel.pages = newPages
                            viewModel.savePages()
                        }
                    },
                    onRemove = {
                        val indexToRemove = currentItems.indexOf(dialog.item)
                        if (indexToRemove != -1) {
                            viewModel.removeItem(currentPage, indexToRemove)
                        }
                    },
                    onDismiss = { viewModel.dialogState = LauncherDialog.None },
                    theme = theme
                )
            }
            is LauncherDialog.FolderView -> {
                FolderDialog(
                    folder = dialog.folder,
                    pm = pm,
                    onAppClick = { app ->
                        try {
                            context.startActivity(app.resolveInfo.launchIntent())
                        } catch (e: android.content.ActivityNotFoundException) {
                            Toast.makeText(context, context.getString(R.string.app_no_longer_installed), Toast.LENGTH_SHORT).show()
                            viewModel.loadData()
                        } catch (e: SecurityException) {
                            Toast.makeText(context, context.getString(R.string.app_no_longer_installed), Toast.LENGTH_SHORT).show()
                            viewModel.loadData()
                        }
                        viewModel.dialogState = LauncherDialog.None
                    },
                    onDismiss = { viewModel.dialogState = LauncherDialog.None },
                    theme = theme
                )
            }
            LauncherDialog.LauncherSettings -> {
                LauncherSettingsDialog(
                    onResetLayout = { viewModel.resetLayout() },
                    onDismiss = { viewModel.dialogState = LauncherDialog.None },
                    theme = theme
                )
            }
            LauncherDialog.Widgets -> {
                WidgetsDialog(
                    onSelectWidget = onSelectWidget,
                    onDismiss = { viewModel.dialogState = LauncherDialog.None },
                    theme = theme
                )
            }
            is LauncherDialog.AppList -> {
                AppListDialog(
                    pm = pm,
                    theme = theme,
                    restrictToDefaultApps = viewModel.restrictToDefaultApps,
                    onUnlockCode = { viewModel.unlockAllApps() },
                    onAppClick = { app ->
                        val newPages = viewModel.pages.toMutableList()
                        val currentPageItems = newPages[currentPage].toMutableList()
                        
                        val targetIndex = viewModel.pendingSlot?.let { if (it.first == currentPage) it.second else null } ?: 
                                         currentPageItems.indexOfFirst { it is LauncherItem.Empty && it.isOccupiedBy == null }

                        if (targetIndex != -1) {
                            currentPageItems[targetIndex] = app
                            newPages[currentPage] = currentPageItems
                            viewModel.pages = newPages
                            viewModel.savePages()
                            viewModel.pendingSlot = null
                        }
                        viewModel.dialogState = LauncherDialog.None
                    },
                    onDismiss = {
                        viewModel.dialogState = LauncherDialog.None
                        viewModel.pendingSlot = null
                    }
                )
            }
            is LauncherDialog.EmptySlotOptions -> {
                EmptySlotOptionsDialog(
                    theme = theme,
                    onAddApp = {
                        viewModel.pendingSlot = dialog.pageIndex to dialog.itemIndex
                        viewModel.dialogState = LauncherDialog.AppList
                    },
                    onAddFolder = { folderName ->
                        viewModel.addFolder(dialog.pageIndex, dialog.itemIndex, folderName)
                        viewModel.dialogState = LauncherDialog.None
                    },
                    onDismiss = { viewModel.dialogState = LauncherDialog.None }
                )
            }
            LauncherDialog.None -> {}
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
