package com.future.settings.ui

import com.future.sharednav.i18n.tr
import com.future.sharednav.systemui.StatusBarInset

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.onReadableAccentColor
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.components.LocalFutureCard
import com.future.sharednav.components.cardRowFocus
import com.future.sharednav.components.TopBarIconButton
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.components.InputDialog
import com.future.sharednav.components.FutureDialog
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureButtonVariant
import com.future.sharednav.components.FutureSpinner
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureAccents
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.scrimColor
import androidx.compose.ui.text.style.TextAlign
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.focus.bringIntoViewOnFocus

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.media.AudioManager
import android.media.RingtoneManager
import androidx.core.content.ContextCompat
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.future.sharednav.focus.focusMotion
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.future.settings.ui.components.*
import com.future.settings.ui.theme.ThemeConfig
import com.future.settings.viewmodel.SettingsViewModel
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.focus.animateFocusColor
import com.future.sharednav.focus.focusScale

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Connections : Screen("connections")
    object Bluetooth : Screen("bluetooth")
    object Sound : Screen("sound")
    object Display : Screen("display")
    object SystemUi : Screen("system_ui")
    object Battery : Screen("battery")
    object Storage : Screen("storage")
    object General : Screen("general")
    object About : Screen("about")
    object Feedback : Screen("feedback")
    object Updates : Screen("updates")
    object Ringtone : Screen("ringtone")
    object NotificationSound : Screen("notification_sound")
    object AlarmSound : Screen("alarm_sound")
    object Language : Screen("language")
    object KeyboardLanguages : Screen("keyboard_languages")
    object Security : Screen("security")
    object SecurityHistory : Screen("security_history")
    object Apps : Screen("apps")
    object AppDetail : Screen("app_detail/{pkg}") {
        fun route(pkg: String) = "app_detail/$pkg"
    }
    object Performance : Screen("performance")
    object ScreenTime : Screen("screen_time")
    object DefaultApps : Screen("default_apps")
    object Privacy : Screen("privacy")
    object AppPermissions : Screen("app_permissions/{pkg}") {
        fun route(pkg: String) = "app_permissions/$pkg"
    }
    object NotificationsFocus : Screen("notifications_focus")
    object EasterEggReveal : Screen("easter_egg_reveal")
    object T9Game : Screen("t9_game")
    object PartyMode : Screen("party_mode")
    object AsciiFlex : Screen("ascii_flex")
    object SimManager : Screen("sim_manager")
    object AppTimers : Screen("app_timers")
    object Diagnostics : Screen("diagnostics")
    object Sos : Screen("sos")
}

/** רשימת אפליקציות FutureOS האמיתיות המוצהרות ב-<queries> של המניפסט - אלו היחידות
 *  שה-PackageManager בכלל יכול לראות עליהן מידע מ-API 30 ומעלה. */
data class FutureApp(val packageName: String, val displayName: String, val icon: ImageVector)

val FUTURE_OS_APPS = listOf(
    FutureApp("com.future.futureui", "FutureUI (System UI)", FutureIcons.Widgets),
    FutureApp("com.future.futurelauncher", "FutureLauncher", FutureIcons.Home),
    FutureApp("com.future.dialer", tr("טלפון"), FutureIcons.Dialpad),
    FutureApp("com.future.messages", tr("הודעות"), FutureIcons.AutoMirrored.Chat),
    FutureApp("com.future.contact", tr("אנשי קשר"), FutureIcons.Contacts),
    FutureApp("com.future.files", tr("קבצים"), FutureIcons.Folder),
    FutureApp("com.future.gallery", tr("גלריה"), FutureIcons.Image),
    FutureApp("com.future.terminal", tr("טרמינל"), FutureIcons.Terminal),
    FutureApp("com.future.tools", tr("כלים"), FutureIcons.Handyman),
    FutureApp("com.future.music", tr("מוזיקה"), FutureIcons.MusicNote),
    FutureApp("com.future.notes", tr("פתקים"), FutureIcons.Description),
    FutureApp("com.future.gotitdone", "gotitdone", Icons.Rounded.CheckCircle),
    FutureApp("com.future.mikdash", tr("מקדש"), FutureIcons.Apps),
    FutureApp("com.future.sfarim", tr("ספרים"), FutureIcons.AutoMirrored.MenuBook)
)

/** חבילת האפליקציה של FutureUI (מרכז בקרה/שורת מצב) - להתאמה אישית שלהם. */
private const val FUTURE_UI_PACKAGE = "com.future.futureui"
private const val FUTURE_UI_LOCK_SETTINGS_ACTION = "com.future.futureui.ACTION_LOCK_SETTINGS"

/**
 * מפעיל Intent בבטחה: תופס ActivityNotFoundException/SecurityException (שאחרת
 * מקריסים את האפליקציה אם המסך היעד לא קיים על ה-ROM), ומציג הודעה אמיתית
 * למשתמש כשזה נכשל - במקום "לא לעשות כלום" בלי שום הסבר.
 */
private fun safeStartActivity(context: android.content.Context, intent: Intent, onFailMessage: String) {
    try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, onFailMessage, android.widget.Toast.LENGTH_SHORT).show()
    }
}

/** getParcelableExtra(String) הישן הוחלף ב-getParcelableExtra(String, Class) מ-API 33 -
 *  wrapper אחד שמשתמש בגרסה הנכונה לפי גרסת המכשיר, כדי לא לשכפל את הענף בכל שימוש. */
@Suppress("DEPRECATION")
private inline fun <reified T : android.os.Parcelable> Intent.getParcelableExtraCompat(key: String): T? =
    if (Build.VERSION.SDK_INT >= 33) getParcelableExtra(key, T::class.java) else getParcelableExtra(key)

/** מנסה לשלוח קובץ ישירות דרך אפליקציית ה-Bluetooth הסטנדרטית של אנדרואיד
 *  (com.android.bluetooth, זו ש-AOSP מספק); אם היא לא קיימת בפועל על המכשיר, נופלים
 *  חזרה לתפריט שיתוף רגיל כדי שהפעולה עדיין תצליח בדרך כלשהי. */
private fun shareFileViaBluetooth(context: android.content.Context, uri: android.net.Uri) {
    val mimeType = context.contentResolver.getType(uri) ?: "*/*"
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        sendIntent.setPackage("com.android.bluetooth")
        context.startActivity(sendIntent)
    } catch (e: Exception) {
        try {
            sendIntent.setPackage(null)
            com.future.sharednav.share.FutureShare.open(context, sendIntent, tr("שיתוף קובץ"))
        } catch (e2: Exception) {
            android.widget.Toast.makeText(context, tr("לא ניתן לשתף את הקובץ"), android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun SettingsApp(
    viewModel: SettingsViewModel,
    startRoute: String = Screen.Main.route,
    feedbackPkg: String? = null,
    crashId: String? = null,
    openFeedbackHistory: Boolean = false,
) {
    val navController = rememberNavController()
    val theme by viewModel.themeConfig

    // מעברים ברירת מחדל של Navigation Compose לוקחים כ-300ms; במסך שמנווטים בו
    // הרבה עם מקלדת פיזית (בלי מגע) זה מצטבר ומרגיש איטי - קיצור ל-120ms הופך
    // את הניווט בין תפריטים למיידי בהרבה בלי לוותר על אנימציה לגמרי.
    // המעברים המשותפים של המערכת (FutureTransitions) - אותו עיקרון בדיוק
    // (קצר, 200ms), אבל עם כיוון: פנימה מחליק משמאל, חזרה מימין.
    CompositionLocalProvider(LocalLayoutDirection provides com.future.sharednav.i18n.Tr.layoutDirection) {
        NavHost(
            navController = navController,
            startDestination = startRoute,
            enterTransition = { com.future.sharednav.theme.FutureTransitions.navEnter },
            exitTransition = { com.future.sharednav.theme.FutureTransitions.navExit },
            popEnterTransition = { com.future.sharednav.theme.FutureTransitions.navPopEnter },
            popExitTransition = { com.future.sharednav.theme.FutureTransitions.navPopExit }
        ) {
            composable(Screen.Main.route) { MainMenu(navController, theme, viewModel) }
            composable(Screen.Connections.route) { ConnectionsScreen(navController, theme, viewModel) }
            composable(Screen.Bluetooth.route) { BluetoothScreen(navController, theme, viewModel) }
            composable(Screen.Sound.route) { SoundScreen(navController, theme, viewModel) }
            composable(Screen.Display.route) { DisplayScreen(navController, theme, viewModel) }
            composable(Screen.SystemUi.route) { SystemUiScreen(navController, theme, viewModel) }
            composable(Screen.Battery.route) { BatteryScreen(navController, theme, viewModel) }
            composable(Screen.Storage.route) { StorageScreen(navController, theme, viewModel) }
            composable(Screen.General.route) { GeneralScreen(navController, theme, viewModel) }
            composable(Screen.About.route) { AboutScreen(navController, theme, viewModel) }
            composable(Screen.Feedback.route) { FeedbackScreen(navController, theme, feedbackPkg, crashId, openFeedbackHistory) }
            composable(Screen.Updates.route) { UpdateScreen(navController, theme) }
            composable(Screen.Ringtone.route) {
                SoundPickerScreen(navController, theme, tr("רינגטון"), RingtoneManager.TYPE_RINGTONE, viewModel)
            }
            composable(Screen.NotificationSound.route) {
                SoundPickerScreen(navController, theme, tr("צליל התראות"), RingtoneManager.TYPE_NOTIFICATION, viewModel)
            }
            composable(Screen.AlarmSound.route) {
                SoundPickerScreen(navController, theme, tr("צליל אזעקה"), RingtoneManager.TYPE_ALARM, viewModel)
            }
            composable(Screen.Language.route) { LanguageScreen(navController, theme, viewModel) }
            composable(Screen.KeyboardLanguages.route) { KeyboardLanguagesScreen(navController, theme) }
            composable(Screen.Security.route) { SecurityScreen(navController, theme) }
            composable(Screen.SecurityHistory.route) { SecurityHistoryScreen(navController, theme) }
            composable(Screen.Apps.route) { AppsScreen(navController, theme) }
            composable(Screen.AppDetail.route) { backStackEntry ->
                val pkg = backStackEntry.arguments?.getString("pkg") ?: ""
                AppDetailScreen(navController, theme, viewModel, pkg)
            }
            composable(Screen.Performance.route) { PerformanceScreen(navController, theme, viewModel) }
            composable(Screen.ScreenTime.route) { ScreenTimeScreen(navController, theme, viewModel) }
            composable(Screen.DefaultApps.route) { DefaultAppsScreen(navController, theme, viewModel) }
            composable(Screen.Privacy.route) { PrivacyScreen(navController, theme) }
            composable(Screen.AppPermissions.route) { backStackEntry ->
                val pkg = backStackEntry.arguments?.getString("pkg") ?: ""
                AppPermissionsScreen(navController, theme, viewModel, pkg)
            }
            composable(Screen.NotificationsFocus.route) { NotificationsFocusScreen(navController, theme, viewModel) }
            composable(Screen.EasterEggReveal.route) { FutureOsRevealScreen(navController) }
            composable(Screen.T9Game.route) { T9GameScreen(navController, viewModel) }
            composable(Screen.PartyMode.route) { PartyModeScreen(navController, theme, viewModel) }
            composable(Screen.AsciiFlex.route) { AsciiFlexScreen(navController, viewModel) }
            composable(Screen.SimManager.route) { SimManagerScreen(navController, theme, viewModel) }
            composable(Screen.AppTimers.route) { AppTimersScreen(navController, theme, viewModel) }
            composable(Screen.Diagnostics.route) { DiagnosticsScreen(navController, theme, viewModel) }
            composable(Screen.Sos.route) { SosScreen(navController, theme, viewModel) }
            com.future.settings.utils.ExtraSystemSettings.ALL_SCREENS.forEach { extra ->
                composable(extra.route) { ExtraSettingsScreen(navController, theme, viewModel, extra) }
            }
        }
    }
}

/** קטלוג שטוח של כל הקטגוריות הראשיות ל"חיפוש בהגדרות" - מקור נתונים נפרד מהכרטיסים
 *  עצמם (לא נגזר מהם אוטומטית) כי לכל פריט יש גם מילות מפתח בעברית/אנגלית לחיפוש,
 *  לא רק את שם הכותרת המוצג. */
private data class SearchableSetting(val title: String, val keywords: String, val icon: ImageVector, val route: String)

private val SEARCHABLE_SETTINGS = listOf(
    SearchableSetting(tr("חיבורים"), tr("bluetooth בלוטות' מצב טיסה נתונים סלולריים sim מיקום location airplane"), FutureIcons.Hub, Screen.Connections.route),
    SearchableSetting(tr("צלילים"), tr("עוצמת שמע צליל רינגטון volume sound ringtone"), FutureIcons.AutoMirrored.VolumeUp, Screen.Sound.route),
    SearchableSetting(tr("תצוגה"), tr("בהירות מסך עיצוב brightness display"), FutureIcons.Brightness6, Screen.Display.route),
    SearchableSetting(tr("שורת מצב ורקע"), tr("שורת מצב סטטוס בר שעון status bar clock טפט wallpaper"), FutureIcons.Widgets, Screen.SystemUi.route),
    SearchableSetting(tr("סוללה"), tr("battery חיסכון טעינה אחוז"), FutureIcons.BatteryFull, Screen.Battery.route),
    SearchableSetting(tr("אחסון"), tr("storage זיכרון פנוי מקום"), FutureIcons.Storage, Screen.Storage.route),
    SearchableSetting(tr("ביצועים ותחזוקה"), tr("performance מעבד ram ניקוי אופטימיזציה"), FutureIcons.Speed, Screen.Performance.route),
    SearchableSetting(tr("התראות ומיקוד"), tr("notifications focus שקט מיקוד שינה"), FutureIcons.Notifications, Screen.NotificationsFocus.route),
    SearchableSetting(tr("זמן מסך"), tr("screen time שימוש טיימר אפליקציות"), FutureIcons.HourglassEmpty, Screen.ScreenTime.route),
    SearchableSetting(tr("מקלדת ושפות הקלדה"), tr("keyboard מקלדת ניבוי prediction t9 שפות הקלדה"), FutureIcons.Keyboard, Screen.KeyboardLanguages.route),
    SearchableSetting(tr("כללי"), tr("general תאריך שעה שפה נגישות accessibility date time language"), FutureIcons.Language, Screen.General.route),
    SearchableSetting(tr("אבטחה"), tr("security מסך נעילה קוד pin סיסמה זיהוי פנים face lock"), FutureIcons.Security, Screen.Security.route),
    SearchableSetting(tr("אפליקציות"), tr("apps אפליקציה"), FutureIcons.Apps, Screen.Apps.route),
    SearchableSetting(tr("ברירות מחדל"), "default ברירת מחדל חייגן הודעות sms מסך בית launcher", FutureIcons.Tune, Screen.DefaultApps.route),
    SearchableSetting(tr("אודות הטלפון"), tr("about imei גרסה מספר טלפון version"), FutureIcons.Info, Screen.About.route),
    SearchableSetting("שלח משוב", "feedback משוב באג דיווח הצעה bug report קריסה crash", FutureIcons.Chat, Screen.Feedback.route),
    SearchableSetting("עדכוני תוכנה", "update עדכון עדכונים גרסה תוכנה software ota", FutureIcons.Download, Screen.Updates.route)
) + com.future.settings.utils.ExtraSystemSettings.ALL_SCREENS.flatMap { screen ->
    // המסך עצמו, ואז כל הגדרה שבו בנפרד - חיפוש "תאורת לילה" מוביל ישר למסך שלה.
    listOf(SearchableSetting(screen.title, screen.keywords, FutureIcons.Tune, screen.route)) +
        screen.allSettings.map { SearchableSetting(it.title, screen.title, FutureIcons.Tune, screen.route) }
}

@Composable
fun MainMenu(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel) {
    var konamiBuffer by remember { mutableStateOf(listOf<Int>()) }
    var confettiTrigger by remember { mutableStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // מצב חגיגה זמני ל-5 דקות בלבד - הטיימר הזה מסמן מחדש כל שנייה כל עוד המצב
    // פעיל, כדי שהכרטיס "יתחבא" לבד ברגע שהזמן נגמר בלי צורך לצאת ולהיכנס שוב.
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(viewModel.partyModeExpiresAt.value) {
        while (System.currentTimeMillis() < viewModel.partyModeExpiresAt.value) {
            kotlinx.coroutines.delay(1000)
            now = System.currentTimeMillis()
        }
        now = System.currentTimeMillis()
    }
    val partyActive = now < viewModel.partyModeExpiresAt.value

    // הפוקוס הראשוני הוא על "חיבורים" ולא על שדה החיפוש: שדה טקסט שמקבל
    // פוקוס תופס את המקשים, כך שכל כניסה להגדרות התחילה בהקלדה במקום
    // בתפריט. בחזרה ממסך פנימי הפוקוס חוזר לפריט שממנו נכנסו.
    var lastFocusedRoute by rememberSaveable { mutableStateOf(Screen.Connections.route) }
    val menuFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val menuFocus: (String) -> Modifier = { route ->
        Modifier
            .focusRequester(menuFocusRequesters.getOrPut(route) { FocusRequester() })
            .onFocusChanged { if (it.isFocused) lastFocusedRoute = route }
    }
    LaunchedEffect(Unit) {
        runCatching { menuFocusRequesters[lastFocusedRoute]?.requestFocus() }
            .onFailure { runCatching { menuFocusRequesters[Screen.Connections.route]?.requestFocus() } }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            // מאזין פסיבי לרצף קונאמי במקלדת המספרים (8-8-2-2-4-6-4-6, כמו כיווני
            // תנועה בנוקיה סנייק) - בלי .focusable() כדי לא לשנות את יעד הפוקוס
            // הראשוני של המסך; מקשי ספרות ממילא לא נצרכים ע"י שום פריט כאן.
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val digit = digitForKey(event.key) ?: return@onKeyEvent false
                konamiBuffer = (konamiBuffer + digit).takeLast(KONAMI_NUMPAD_SEQUENCE.size)
                if (konamiBuffer == KONAMI_NUMPAD_SEQUENCE) {
                    viewModel.activatePartyMode()
                    confettiTrigger++
                    konamiBuffer = emptyList()
                }
                false
            }
    ) {
        // N-01/N-02: הרשימה מתחילה מתחת לשורת המצב, כך שגם פריט שנגלל למעלה (חיפוש, כותרת)
        // לא מצויר מתחתיה ולא נחתך.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = StatusBarInset.HEIGHT_DP.dp),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item { LargeHeader(tr("הגדרות"), theme) }

            item {
                FutureTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    theme = theme.futureTheme,
                    placeholder = tr("חיפוש בהגדרות"),
                    leading = {
                        Icon(FutureIcons.Search, contentDescription = null, tint = theme.textColor.copy(alpha = 0.6f), modifier = Modifier.size(FutureDimens.iconTopBar))
                    },
                    trailing = {
                        if (searchQuery.isNotEmpty()) {
                            TopBarIconButton(FutureIcons.Close, tr("נקה חיפוש"), theme.textColor, theme.primaryColor, { searchQuery = "" })
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingXs)
                        .escapeTextFieldFocusTrap()
                )
            }

            if (searchQuery.isBlank()) {
                // המסך מחולק לקטעים בעלי שם (ר' העיצוב): עד כה היו כאן שישה
                // כרטיסים אנונימיים שהחלוקה ביניהם לא נאמרה בשום מקום, כך
                // שהמשתמש היה צריך לקרוא את כל השורות כדי להבין למה דווקא הן
                // מקובצות יחד. תקצור כותרות המשנה נועד להשאיר כל שורה בשורת
                // טקסט אחת - שתי שורות הכפילו את גובה הפריט ופחות פריטים נכנסו למסך.
                item { SettingHeader(tr("עיקרי"), theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem(tr("חיבורים"), tr("Bluetooth, נתונים, SIM ומיקום"), FutureIcons.Hub, theme, modifier = menuFocus(Screen.Connections.route)) { navController.navigate(Screen.Connections.route) }
                        SettingDivider(theme)
                        SettingItem(tr("צלילים"), tr("עוצמה, רינגטון ורטט"), FutureIcons.AutoMirrored.VolumeUp, theme, modifier = menuFocus(Screen.Sound.route)) { navController.navigate(Screen.Sound.route) }
                        SettingDivider(theme)
                        SettingItem(tr("תצוגה"), tr("בהירות, מצב כהה וגודל טקסט"), FutureIcons.Brightness6, theme, modifier = menuFocus(Screen.Display.route)) { navController.navigate(Screen.Display.route) }
                    }
                }

                item { SettingHeader(tr("מכשיר"), theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem(tr("שורת מצב ורקע"), tr("מה מופיע למעלה ותמונת הרקע"), FutureIcons.Widgets, theme, modifier = menuFocus(Screen.SystemUi.route)) { navController.navigate(Screen.SystemUi.route) }
                        SettingDivider(theme)
                        SettingItem(tr("סוללה"), tr("מצב הסוללה וחיסכון בחשמל"), FutureIcons.BatteryFull, theme, modifier = menuFocus(Screen.Battery.route)) { navController.navigate(Screen.Battery.route) }
                        SettingDivider(theme)
                        SettingItem(tr("אחסון"), tr("כמה מקום תפוס ומה תופס אותו"), FutureIcons.Storage, theme, modifier = menuFocus(Screen.Storage.route)) { navController.navigate(Screen.Storage.route) }
                        SettingDivider(theme)
                        SettingItem(tr("ביצועים ותחזוקה"), tr("זיכרון, מעבד וניקוי"), FutureIcons.Speed, theme, modifier = menuFocus(Screen.Performance.route)) { navController.navigate(Screen.Performance.route) }
                    }
                }

                item { SettingHeader(tr("אבטחה"), theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem(tr("אבטחה"), "בדיקת אבטחה, מסך נעילה וקוד", FutureIcons.Security, theme, modifier = menuFocus(Screen.Security.route)) { navController.navigate(Screen.Security.route) }
                    }
                }

                item { SettingHeader(tr("שימוש"), theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem(tr("התראות ומיקוד"), tr("מצב מיקוד ושעות שינה"), FutureIcons.Notifications, theme, modifier = menuFocus(Screen.NotificationsFocus.route)) { navController.navigate(Screen.NotificationsFocus.route) }
                        SettingDivider(theme)
                        SettingItem(tr("זמן מסך"), tr("כמה זמן בכל אפליקציה, והגבלות"), FutureIcons.HourglassEmpty, theme, modifier = menuFocus(Screen.ScreenTime.route)) { navController.navigate(Screen.ScreenTime.route) }
                    }
                }

                item { SettingHeader(tr("עוד"), theme) }
                item {
                    val context = LocalContext.current
                    val accessibilityRoute = com.future.settings.utils.ExtraSystemSettings.ACCESSIBILITY.route
                    SettingsCard(theme) {
                        SettingItem(tr("נגישות"), tr("ניגודיות, תיקון צבעים, שמע והקראה"), FutureIcons.Accessibility, theme, modifier = menuFocus(accessibilityRoute)) { navController.navigate(accessibilityRoute) }
                        SettingDivider(theme)
                        SettingItem(tr("עוזרי"), tr("העוזר הקולי - שואלים אותו כל דבר"), FutureIcons.Mic, theme) {
                            val intent = context.packageManager.getLaunchIntentForPackage("com.future.assistant")
                            if (intent != null) safeStartActivity(context, intent, tr("לא ניתן לפתוח את עוזרי"))
                            else android.widget.Toast.makeText(context, tr("עוזרי לא מותקן על המכשיר"), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                item { SettingHeader(tr("מערכת"), theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem(tr("כללי"), tr("תאריך ושעה, שפה ונגישות"), FutureIcons.Language, theme, modifier = menuFocus(Screen.General.route)) { navController.navigate(Screen.General.route) }
                        SettingDivider(theme)
                        SettingItem(tr("אפליקציות"), tr("פתיחה, עצירה, הרשאות ונתונים"), FutureIcons.Apps, theme, modifier = menuFocus(Screen.Apps.route)) { navController.navigate(Screen.Apps.route) }
                        SettingDivider(theme)
                        SettingItem(tr("אודות הטלפון"), tr("גרסה, דגם ומספר טלפון"), FutureIcons.Info, theme, modifier = menuFocus(Screen.About.route)) { navController.navigate(Screen.About.route) }
                        SettingDivider(theme)
                        SettingItem("עדכוני תוכנה", "בדיקה והתקנה של עדכונים", FutureIcons.Download, theme, modifier = menuFocus(Screen.Updates.route)) { navController.navigate(Screen.Updates.route) }
                        SettingDivider(theme)
                        SettingItem("שלח משוב", "דיווח על באג או הצעה לשיפור", FutureIcons.Chat, theme, modifier = menuFocus(Screen.Feedback.route)) { navController.navigate(Screen.Feedback.route) }
                    }
                }

            } else {
                val query = searchQuery.trim()
                val matches = SEARCHABLE_SETTINGS.filter { it.title.contains(query, ignoreCase = true) || it.keywords.contains(query, ignoreCase = true) }
                if (matches.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(tr("לא נמצאו הגדרות תואמות"), color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.body)
                        }
                    }
                } else {
                    item {
                        SettingsCard(theme) {
                            matches.forEachIndexed { index, setting ->
                                SettingItem(setting.title, null, setting.icon, theme) { navController.navigate(setting.route) }
                                if (index < matches.lastIndex) SettingDivider(theme)
                            }
                        }
                    }
                }
            }

            if (partyActive) {
                item {
                    SettingsCard(theme) {
                        SettingItem(tr("מצב חגיגה"), "עוד ${((viewModel.partyModeExpiresAt.value - now) / 60000L).coerceAtLeast(0L)} דקות", FutureIcons.Celebration, theme) { navController.navigate(Screen.PartyMode.route) }
                    }
                }
            }
        }
        ConfettiBurst(trigger = confettiTrigger, modifier = Modifier.fillMaxSize())
    }
}

/** השורה העליונה של מסך פנימי - ScreenTopBar של הדיזיין סיסטם (היה IconButton של Material). */
@Composable
fun SmallHeader(title: String, theme: ThemeConfig, onBack: () -> Unit) {
    // ריווח קטן - הכותרת ישבה מתחת לשורת המצב (במסך הראשי הכותרת כבר רחוקה ממנה).
    Column(Modifier.padding(top = StatusBarInset.TITLE_GAP_DP.dp)) {
        ScreenTopBar(title = title, textColor = theme.textColor, accentColor = theme.primaryColor, onBack = onBack)
    }
}

/** Bluetooth נפתח באפליקציית ה-Bluetooth של FutureOS; המסך הפנימי נשאר רק
 *  כגיבוי כשהאפליקציה לא מותקנת. */
private fun openBluetoothApp(context: android.content.Context, navController: NavController) {
    val intent = context.packageManager.getLaunchIntentForPackage("com.future.bluetooth")
    if (intent != null) context.startActivity(intent) else navController.navigate(Screen.Bluetooth.route)
}

@Composable
fun ConnectionsScreen(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader(tr("חיבורים"), theme) { navController.popBackStack() }
            LazyColumn {
                item {
                    SettingsCard(theme) {
                        SettingItem("Bluetooth", if (viewModel.bluetoothEnabled.value) tr("מופעל") else tr("כבוי"), FutureIcons.Bluetooth, theme) { openBluetoothApp(context, navController) }
                        SettingDivider(theme)
                        SettingSwitch(tr("מצב טיסה"), tr("מנתק שיחות, נתונים ו-Bluetooth בבת אחת"), viewModel.airplaneMode.value, { viewModel.toggleAirplaneMode() }, theme)
                        SettingDivider(theme)
                        SettingItem(tr("רשת מתקדמת"), tr("Wi-Fi, DNS פרטי וחיסכון בנתונים"), FutureIcons.Wifi, theme) { navController.navigate(com.future.settings.utils.ExtraSystemSettings.NETWORK.route) }
                    }
                }
                item { SettingHeader(tr("רשת סלולרית"), theme) }
                item {
                    SettingsCard(theme) {
                        SettingSwitch(tr("נתונים סלולריים"), tr("אינטרנט דרך כרטיס ה-SIM"), viewModel.mobileData.value, { viewModel.toggleMobileData() }, theme)
                        SettingDivider(theme)
                        SettingSwitch(tr("נדידה (רומינג)"), tr("נתונים סלולריים גם מחוץ לארץ"), viewModel.dataRoaming.value, { viewModel.toggleDataRoaming() }, theme)
                        SettingDivider(theme)
                        SettingItem(tr("ניהול כרטיסי SIM"), tr("שם, צבע וברירת מחדל לכל קו"), FutureIcons.SimCard, theme) { navController.navigate(Screen.SimManager.route) }
                    }
                }
                item { SettingHeader(tr("מיקום"), theme) }
                item {
                    SettingsCard(theme) {
                        SettingSwitch(tr("שירותי מיקום"), tr("נדרש לניווט, לזמני היום ולמזג האוויר"), viewModel.locationEnabled.value, { viewModel.toggleLocationEnabled() }, theme)
                    }
                }
            }
        }
    }
}

/**
 * מסך Bluetooth כקטגוריה ראשית עצמאית: הפעלה/כיבוי, שם מכשיר גלוי עם עריכה, סריקת
 * מכשירים חדשים והתאמה (pairing) אליהם, רשימת מכשירים משויכים עם "שכח מכשיר", ושיתוף
 * קבצים דרך Bluetooth. הסריקה משתמשת ב-BroadcastReceiver מקומי למסך (ACTION_FOUND/
 * ACTION_BOND_STATE_CHANGED) שנרשם ומתבטל יחד עם חיי המסך, באותו דפוס בדיוק כמו
 * ה-DisposableEffect הקיים ב-SoundPickerScreen.
 */
@Composable
fun BluetoothScreen(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val interactor = remember { viewModel.getSystemInteractor() }

    LaunchedEffect(Unit) { viewModel.loadBluetoothInfo() }

    var isRenaming by remember { mutableStateOf(false) }
    var deviceNameText by remember { mutableStateOf("") }
    var forgetTarget by remember { mutableStateOf<com.future.settings.utils.SystemInteractor.BluetoothDeviceEntry?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    val discovered = remember { mutableStateListOf<android.bluetooth.BluetoothDevice>() }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) shareFileViaBluetooth(context, uri)
    }

    // מ-Android 12 BLUETOOTH_SCAN/BLUETOOTH_CONNECT הן הרשאות ריצה "מסוכנות" -
    // ה-pm grant דרך initSystemPermissions() (ר' SystemInteractor) עובד רק כשיש
    // root אמיתי (su); בלעדיו הוא נכשל בשקט, ואז startDiscovery/createBond
    // זורקים SecurityException שנבלע גם הוא בשקט - "סריקה לא עובדת" בלי שום
    // שגיאה גלויה. בקשת ההרשאה הרגילה של אנדרואיד היא רשת הביטחון שעובדת גם
    // בלי root.
    var pendingScan by remember { mutableStateOf(false) }
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (results.values.all { it } && pendingScan) {
            pendingScan = false
            discovered.clear()
            isScanning = interactor.startBluetoothDiscovery()
            if (!isScanning) android.widget.Toast.makeText(context, tr("לא ניתן להפעיל סריקת Bluetooth"), android.widget.Toast.LENGTH_SHORT).show()
        } else if (!results.values.all { it }) {
            pendingScan = false
            android.widget.Toast.makeText(context, tr("צריך הרשאת Bluetooth כדי לסרוק מכשירים"), android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    fun requestBluetoothPermissionsThenScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val needed = listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
                .filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
            if (needed.isNotEmpty()) {
                pendingScan = true
                bluetoothPermissionLauncher.launch(needed.toTypedArray())
                return
            }
        }
        discovered.clear()
        isScanning = interactor.startBluetoothDiscovery()
        if (!isScanning) android.widget.Toast.makeText(context, tr("לא ניתן להפעיל סריקת Bluetooth"), android.widget.Toast.LENGTH_SHORT).show()
    }

    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context?, intent: Intent?) {
                when (intent?.action) {
                    android.bluetooth.BluetoothDevice.ACTION_FOUND -> {
                        val device = intent.getParcelableExtraCompat<android.bluetooth.BluetoothDevice>(android.bluetooth.BluetoothDevice.EXTRA_DEVICE)
                        if (device != null && device.bondState != android.bluetooth.BluetoothDevice.BOND_BONDED && discovered.none { it.address == device.address }) {
                            discovered.add(device)
                        }
                    }
                    android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        isScanning = false
                    }
                    android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                        val device = intent.getParcelableExtraCompat<android.bluetooth.BluetoothDevice>(android.bluetooth.BluetoothDevice.EXTRA_DEVICE)
                        if (device != null) discovered.removeAll { it.address == device.address }
                        viewModel.loadBluetoothInfo()
                    }
                }
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction(android.bluetooth.BluetoothDevice.ACTION_FOUND)
            addAction(android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, android.content.Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            interactor.cancelBluetoothDiscovery()
            context.unregisterReceiver(receiver)
        }
    }

    if (isRenaming) {
        InputDialog(
            title = tr("שם המכשיר"),
            theme = theme.futureTheme,
            initialValue = deviceNameText,
            onDismiss = { isRenaming = false },
            onConfirm = { name ->
                viewModel.renameBluetoothDevice(name)
                isRenaming = false
            },
        )
    }

    forgetTarget?.let { device ->
        ConfirmDialog(
            theme = theme,
            title = tr("שכחת מכשיר"),
            message = tr("\"{0}\" יוסר מרשימת המכשירים המשויכים. תצטרך להתאים אותו מחדש כדי להתחבר שוב.", device.name),
            confirmLabel = tr("שכח מכשיר"),
            onConfirm = {
                viewModel.forgetBluetoothDevice(device.address, device.name)
                forgetTarget = null
            },
            onDismiss = { forgetTarget = null }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader("Bluetooth", theme) { navController.popBackStack() }
            LazyColumn {
                item {
                    SettingsCard(theme) {
                        SettingSwitch("Bluetooth", if (viewModel.bluetoothEnabled.value) tr("מופעל") else tr("כבוי"), viewModel.bluetoothEnabled.value, {
                            val wasEnabled = viewModel.bluetoothEnabled.value
                            viewModel.toggleBluetooth()
                            if (wasEnabled) { discovered.clear(); isScanning = false }
                        }, theme)
                    }
                }

                if (viewModel.bluetoothEnabled.value) {
                    item {
                        SettingsCard(theme) {
                            SettingItem(tr("שם המכשיר שלי"), viewModel.bluetoothDeviceName.value, FutureIcons.Edit, theme) {
                                deviceNameText = viewModel.bluetoothDeviceName.value
                                isRenaming = true
                            }
                        }
                    }

                    item { SettingHeader(tr("מכשירים משויכים"), theme) }
                    item {
                        val paired = viewModel.pairedBluetoothDevices.value
                        SettingsCard(theme) {
                            if (paired.isEmpty()) {
                                SettingItem(tr("אין עדיין מכשירים משויכים"), null, null, theme, showChevron = false)
                            } else {
                                paired.forEachIndexed { index, device ->
                                    SettingItem(
                                        device.name,
                                        (if (device.isConnected) tr("מחובר כעת") else tr("משויך")) + tr(" - לחצו OK כדי לשכוח"),
                                        if (device.isConnected) Icons.Rounded.BluetoothConnected else FutureIcons.Bluetooth,
                                        theme,
                                        showChevron = false
                                    ) { forgetTarget = device }
                                    if (index < paired.lastIndex) SettingDivider(theme)
                                }
                            }
                        }
                    }

                    item { SettingHeader(tr("מכשירים זמינים"), theme) }
                    item {
                        SettingsCard(theme) {
                            SettingItem(
                                if (isScanning) tr("סורק מכשירים בסביבה") else tr("סרוק מכשירים חדשים"),
                                if (discovered.isEmpty()) null else tr("{0} נמצאו", discovered.size),
                                FutureIcons.Search,
                                theme,
                                showChevron = false
                            ) {
                                if (!isScanning) requestBluetoothPermissionsThenScan()
                            }
                            if (discovered.isNotEmpty()) {
                                SettingDivider(theme)
                                discovered.forEachIndexed { index, device ->
                                    val name = try { device.name ?: device.address } catch (e: SecurityException) { device.address }
                                    SettingItem(name, tr("לחצו OK כדי להתאים"), FutureIcons.Bluetooth, theme, showChevron = false) {
                                        val started = interactor.pairBluetoothDevice(device)
                                        if (!started) android.widget.Toast.makeText(context, tr("לא ניתן היה להתחיל התאמה עם {0}", name), android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    if (index < discovered.lastIndex) SettingDivider(theme)
                                }
                            }
                        }
                    }

                    item { SettingHeader(tr("שיתוף קבצים"), theme) }
                    item {
                        SettingsCard(theme) {
                            SettingItem(tr("שלח קובץ דרך Bluetooth"), tr("בחירת קובץ מהמכשיר לשליחה"), FutureIcons.Send, theme) {
                                filePickerLauncher.launch("*/*")
                            }
                        }
                    }
                }
            }
        }
    }
}

/** "צלילים ותצוגה" פוצל לשתי קטגוריות ראשיות נפרדות (SoundScreen/DisplayScreen) -
 *  היו מעורבבים במסך אחד ענק שבו הגדרות שמע והגדרות מסך לא קשורות זו לזו. */
@Composable
fun SoundScreen(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel) {
    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader(tr("צלילים"), theme) { navController.popBackStack() }
            LazyColumn {
                item {
                    SettingsCard(theme) {
                        val currentMode = viewModel.ringerMode.value
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            SoundModeItem(tr("צליל"), FutureIcons.AutoMirrored.VolumeUp, currentMode == AudioManager.RINGER_MODE_NORMAL, theme) { viewModel.setRingerMode(AudioManager.RINGER_MODE_NORMAL) }
                            SoundModeItem(tr("רטט"), FutureIcons.Vibration, currentMode == AudioManager.RINGER_MODE_VIBRATE, theme) { viewModel.setRingerMode(AudioManager.RINGER_MODE_VIBRATE) }
                            SoundModeItem(tr("השתק"), FutureIcons.AutoMirrored.VolumeOff, currentMode == AudioManager.RINGER_MODE_SILENT, theme) { viewModel.setRingerMode(AudioManager.RINGER_MODE_SILENT) }
                        }
                    }
                }
                item { SettingHeader(tr("עוצמת שמע"), theme) }
                item {
                    SettingsCard(theme) {
                        VolumeSlider(tr("מדיה"), viewModel.mediaVolume.value, theme, tr("מוזיקה, סרטונים ומשחקים")) { viewModel.setVolume(AudioManager.STREAM_MUSIC, it) }
                        SettingDivider(theme)
                        VolumeSlider(tr("מערכת"), viewModel.systemVolume.value, theme, tr("צלילי מקשים ותפריטים")) { viewModel.setVolume(AudioManager.STREAM_SYSTEM, it) }
                        SettingDivider(theme)
                        VolumeSlider(tr("התראות"), viewModel.notificationVolume.value, theme, tr("הודעות והתראות מאפליקציות")) { viewModel.setNotificationVolume(it) }
                        SettingDivider(theme)
                        VolumeSlider(tr("אזעקה"), viewModel.alarmVolume.value, theme, tr("השעון המעורר והטיימרים")) { viewModel.setAlarmVolume(it) }
                        SettingDivider(theme)
                        VolumeSlider(tr("שיחות"), viewModel.callVolume.value, theme, tr("הקול של הצד השני בשיחה")) { viewModel.setCallVolume(it) }
                    }
                }
                item { SettingHeader(tr("קול ורטט"), theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem(tr("רינגטון"), tr("הצליל של שיחה נכנסת"), FutureIcons.MusicNote, theme) { navController.navigate(Screen.Ringtone.route) }
                        SettingDivider(theme)
                        SettingItem(tr("צליל התראות"), tr("הצליל של הודעה או התראה חדשה"), FutureIcons.Notifications, theme) { navController.navigate(Screen.NotificationSound.route) }
                        SettingDivider(theme)
                        SettingItem(tr("צליל אזעקה"), tr("הצליל של השעון המעורר"), FutureIcons.Alarm, theme) { navController.navigate(Screen.AlarmSound.route) }
                        SettingDivider(theme)
                        SettingSwitch(tr("רטט בצלצול"), tr("הטלפון רוטט גם כשהוא מצלצל"), viewModel.vibrateWhenRinging.value, { viewModel.toggleVibrateWhenRinging() }, theme)
                        SettingDivider(theme)
                        SettingSwitch(tr("צלילי מקשים"), tr("צליל קצר בכל לחיצה בתפריטים"), viewModel.soundEffects.value, { viewModel.toggleSoundEffects() }, theme)
                        SettingDivider(theme)
                        SettingSwitch(tr("צלילי חיוג"), tr("צליל בלחיצה על מקש בחייגן"), viewModel.dialPadTones.value, { viewModel.toggleDialPadTones() }, theme)
                        SettingDivider(theme)
                        SettingSwitch(tr("משוב רטט"), tr("רטט קל בלחיצה על מקש"), viewModel.hapticFeedback.value, { viewModel.toggleHapticFeedback() }, theme)
                        SettingDivider(theme)
                        SettingSwitch(tr("צליל נעילת מסך"), tr("צליל כשהמסך נכבה או נדלק"), viewModel.lockSound.value, { viewModel.toggleLockSound() }, theme)
                        SettingDivider(theme)
                        SettingSwitch(tr("צליל טעינה"), tr("צליל בחיבור/ניתוק מטען"), viewModel.chargingSound.value, { viewModel.toggleChargingSound() }, theme)
                        SettingDivider(theme)
                        SettingItem(tr("צלילים ורטט מתקדם"), tr("נא לא להפריע ועוצמת רטט"), FutureIcons.Tune, theme) { navController.navigate(com.future.settings.utils.ExtraSystemSettings.SOUND.route) }
                    }
                }
            }
        }
    }
}

@Composable
fun DisplayScreen(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel) {
    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader(tr("תצוגה"), theme) { navController.popBackStack() }
            LazyColumn {
                item { SettingHeader(tr("תצוגה"), theme) }
                item {
                    SettingsCard(theme) {
                        VolumeSlider(tr("בהירות מסך"), viewModel.brightness.value, theme, tr("חץ שמאלה מבהיר, ימינה מעמעם")) { viewModel.setBrightness(it) }
                        SettingDivider(theme)
                        SettingSwitch(tr("בהירות אוטומטית"), tr("המסך מתאים את עצמו לאור בחדר"), viewModel.adaptiveBrightness.value, { viewModel.toggleAdaptiveBrightness() }, theme)
                        SettingDivider(theme)
                        VolumeSlider(tr("גודל טקסט"), (theme.fontSizeMultiplier - 0.8f) / 0.6f, theme, tr("גודל האותיות באפליקציות FutureOS")) {
                            viewModel.updateFontSize(0.8f + (it.coerceIn(0f, 1f) * 0.6f))
                        }
                        SettingDivider(theme)
                        SettingSwitch(tr("מצב כהה"), tr("רקע כהה בכל אפליקציות FutureOS"), theme.isDarkMode, { viewModel.toggleDarkMode() }, theme)
                        SettingDivider(theme)
                        SettingSwitch(tr("הפחתת אנימציות"), tr("מעברים מיידיים בין מסכים, בלי תנועה"), viewModel.animationsReduced.value, { viewModel.toggleAnimationsReduced() }, theme)
                        SettingDivider(theme)
                        val zoomStep = remember(viewModel.densityDpi.value) {
                            // עיגול (לא חיתוך): ה-dpi נשמר כ-toInt, ולכן החיתוך הפוך היה מחזיר צעד קודם.
                            kotlin.math.round((viewModel.densityDpi.value.toFloat() / viewModel.defaultDensityDpi - 0.88f) / 0.06f).toInt().coerceIn(0, viewModel.densityPresetsCount - 1)
                        }
                        VolumeSlider(tr("זום מסך"), zoomStep.toFloat() / (viewModel.densityPresetsCount - 1), theme, tr("מגדיל או מקטין את כל מה שעל המסך")) {
                            viewModel.setScreenZoomStep((it * (viewModel.densityPresetsCount - 1)).toInt())
                        }
                        if (viewModel.densityDpi.value != viewModel.defaultDensityDpi) {
                            SettingDivider(theme)
                            SettingItem(tr("איפוס זום מסך"), tr("חזרה לגודל המקורי"), FutureIcons.RestartAlt, theme, showChevron = false) { viewModel.resetScreenZoom() }
                        }
                        SettingDivider(theme)
                        SettingItem(tr("כיבוי מסך אוטומטי"), tr("נכבה אחרי ") + viewModel.screenTimeoutLabel(viewModel.screenTimeout.value) + tr(" בלי שימוש"), FutureIcons.Timer, theme) {
                            viewModel.cycleScreenTimeout()
                        }
                        SettingDivider(theme)
                        SettingItem(tr("תצוגה מתקדמת"), tr("תאורת לילה, צבעים ושומר מסך"), FutureIcons.Nightlight, theme) { navController.navigate(com.future.settings.utils.ExtraSystemSettings.DISPLAY.route) }
                    }
                }
                item { SettingHeader(tr("צבע הדגשה"), theme) }
                item {
                    SettingsCard(theme) {
                        ColorPicker(currentColor = theme.primaryColor, theme = theme) { viewModel.updatePrimaryColor(it) }
                    }
                }
            }
        }
    }
}

/** מסך בחירת צליל גנרי - משרת רינגטון/התראה/אזעקה לפי type, כדי לא לשכפל את
 *  אותו מסך שלוש פעמים. */
@Composable
fun SoundPickerScreen(navController: NavController, theme: ThemeConfig, title: String, type: Int, viewModel: SettingsViewModel) {
    val interactor = remember { viewModel.getSystemInteractor() }
    val ringtones = remember(type) { interactor.getAvailableRingtones(type) }
    var selectedUri by remember(type) { mutableStateOf(interactor.getCurrentRingtoneUri(type)) }

    DisposableEffect(Unit) {
        onDispose { interactor.stopRingtonePreview() }
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader(title, theme) { navController.popBackStack() }
            if (ringtones.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(tr("לא נמצאו צלילים במכשיר"), color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.body)
                }
            } else {
                LazyColumn {
                    item {
                        SettingsCard(theme) {
                            ringtones.forEachIndexed { index, ringtone ->
                                SettingItem(
                                    ringtone.title,
                                    null,
                                    if (ringtone.uri == selectedUri) Icons.Rounded.CheckCircle else FutureIcons.MusicNote,
                                    theme,
                                    showChevron = false
                                ) {
                                    selectedUri = ringtone.uri
                                    interactor.previewRingtone(ringtone.uri)
                                    interactor.setDefaultRingtone(ringtone.uri, type)
                                }
                                if (index < ringtones.lastIndex) SettingDivider(theme)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * בורר צבעים אמיתי (HSV) בשלושה מחוונים - גוון, רוויה, בהירות - פלוס תצוגה
 * מקדימה חיה ומספר Hex. שלושה מחוונים חד-ממדיים, לא משטח דו-ממדי לגרירה,
 * כדי שיהיה נוח לכוונן עם חצים במקלדת/שלט.
 */
@Composable
fun ColorPicker(currentColor: Color, theme: ThemeConfig, onColorChange: (Color) -> Unit) {
    val hsv = remember(currentColor) {
        val arr = FloatArray(3)
        android.graphics.Color.colorToHSV(currentColor.toArgb(), arr)
        arr
    }
    var hue by remember(currentColor) { mutableFloatStateOf(hsv[0]) }
    var saturation by remember(currentColor) { mutableFloatStateOf(hsv[1]) }
    var value by remember(currentColor) { mutableFloatStateOf(hsv[2]) }

    fun apply(h: Float = hue, s: Float = saturation, v: Float = value) {
        onColorChange(Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))))
    }

    // המחוונים הם שורות מלאות בכרטיס (כמו בשאר המסכים), ולכן רק התצוגה
    // המקדימה ושורת הצבעים המוכנים מקבלות ריפוד משלהן.
    Column {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(currentColor)
                    .border(1.dp, theme.textColor.copy(alpha = 0.3f), CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "#%06X".format(0xFFFFFF and currentColor.toArgb()),
                color = theme.textColor,
                fontWeight = FontWeight.Bold
            )
        }
        SettingDivider(theme)
        VolumeSlider(tr("גוון"), hue / 360f, theme, tr("איזה צבע: אדום, ירוק, כחול וכו'")) { hue = it * 360f; apply(h = hue) }
        SettingDivider(theme)
        VolumeSlider(tr("רוויה"), saturation, theme, tr("כמה הצבע חי - מאפור ועד צבע מלא")) { saturation = it; apply(s = it) }
        SettingDivider(theme)
        VolumeSlider(tr("בהירות"), value, theme, tr("כמה הצבע בהיר - משחור ועד בוהק")) { value = it; apply(v = it) }
        SettingDivider(theme)

        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            val presets = FutureAccents.presets
            presets.forEach { color ->
                var isFocused by remember { mutableStateOf(false) }
                val selected = currentColor == color
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .focusScale(isFocused, focusedScale = 1.2f)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            if (selected) 3.dp else 2.dp,
                            animateFocusColor(isFocused && !selected, theme.primaryColor, if (selected) theme.textColor else Color.Transparent).value,
                            CircleShape,
                        )
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusable().bringIntoViewOnFocus()
                        .clickable { onColorChange(color) }
                )
            }
        }
    }
}

/** "שורת מצב ומסך נעילה" ו"רקע ומסך הבית" אוחדו לקטגוריה ראשית אחת - שתיהן
 *  היו בעצם על אותו נושא ("איך מסך הבית/הנעילה נראים") ומוצגות גם בכרטיס אחד
 *  בתפריט הראשי, אז לא היה הגיוני שיהיו שני מסכים נפרדים לגמרי. */
@Composable
fun SystemUiScreen(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(com.future.settings.theme.SystemUiSettingsClient.get(context)) }
    // בלי קישור מאומת ל-FutureUI המתגים לא נשמרים בכלל - מסומנים כ"לא זמין" ולא מתנהגים כרגיל.
    val systemUiLinked = com.future.settings.theme.SystemUiSettingsClient.isLinked(context)
    val unavailable = tr("לא זמין")

    fun reload() { settings = com.future.settings.theme.SystemUiSettingsClient.get(context) }

    val wallpaperRows = remember { com.future.settings.utils.WallpaperPhotos.all.chunked(3) }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.applyWallpaperFromUri(uri)
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader(tr("שורת מצב ורקע"), theme) { navController.popBackStack() }
            LazyColumn {
                item { SettingHeader(tr("סגנון שורת המצב"), theme) }
                item {
                    SettingsCard(theme) {
                        StatusBarStyles.forEachIndexed { index, style ->
                            if (index > 0) SettingDivider(theme)
                            StatusBarStyleOption(style, selected = settings.barStyle == style.id, theme = theme) {
                                com.future.settings.theme.SystemUiSettingsClient.setBarStyle(context, style.id); reload()
                            }
                        }
                    }
                }
                item { SettingHeader("רקע שורת המצב", theme) }
                item {
                    SettingsCard(theme) {
                        StatusBarTones.forEachIndexed { index, tone ->
                            if (index > 0) SettingDivider(theme)
                            StatusBarStyleOption(tone, selected = settings.barTone == tone.id, theme = theme) {
                                com.future.settings.theme.SystemUiSettingsClient.setBarTone(context, tone.id); reload()
                            }
                        }
                    }
                }
                item { SettingHeader(tr("שורת מצב"), theme) }
                item {
                    SettingsCard(theme) {
                        SettingSwitch(tr("הצג אחוז סוללה"), if (systemUiLinked) tr("מספר לצד סמל הסוללה בשורת המצב") else unavailable, settings.showBattery, {
                            if (systemUiLinked) { com.future.settings.theme.SystemUiSettingsClient.setShowBattery(context, it); reload() }
                        }, theme)
                        SettingDivider(theme)
                        SettingSwitch(tr("הצג סמל Bluetooth"), if (systemUiLinked) tr("סמל בשורת המצב כש-Bluetooth דולק") else unavailable, settings.showBluetooth, {
                            if (systemUiLinked) { com.future.settings.theme.SystemUiSettingsClient.setShowBluetooth(context, it); reload() }
                        }, theme)
                        SettingDivider(theme)
                        SettingSwitch(tr("שעון 24 שעות"), if (systemUiLinked) tr("17:00 במקום 5:00") else unavailable, settings.use24HourClock, {
                            if (systemUiLinked) { com.future.settings.theme.SystemUiSettingsClient.setUse24HourClock(context, it); reload() }
                        }, theme)
                        SettingDivider(theme)
                        // עבר לכאן ממסך "התאמה אישית" של FutureUI - כל ההגדרות באפליקציה אחת.
                        SettingSwitch(tr("הסתר שורת מצב מקורית של אנדרואיד (דורש root)"), if (systemUiLinked) null else unavailable, settings.suppressSystemBars, {
                            if (systemUiLinked) { com.future.settings.theme.SystemUiSettingsClient.setSuppressSystemBars(context, it); reload() }
                        }, theme)
                    }
                }
                item { SettingHeader(tr("רקע מסך"), theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem(tr("בחר תמונה מהגלריה"), tr("טפט מותאם אישית מהתמונות במכשיר"), FutureIcons.Image, theme) {
                            galleryLauncher.launch("image/*")
                        }
                    }
                }
                item { SettingHeader(tr("טפטים מובנים"), theme) }
                // שורות של 3 (ולא LazyVerticalGrid בגובה קבוע) - כך 100 הטפטים נטענים
                // בעצלות תוך כדי גלילה, והמיקוד בחצים זורם בתוך הרשימה הרגילה.
                items(wallpaperRows, key = { row -> "wp-" + row.first().id }) { row ->
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        row.forEach { photo ->
                            Box(modifier = Modifier.weight(1f)) {
                                WallpaperPhotoTile(photo, theme) { viewModel.applyWallpaperPhoto(photo) }
                            }
                        }
                        repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
                item {
                    Text(
                        text = tr("כדי לסדר, להוסיף או להסיר סמלים ממסך הבית - לחצו פעמיים על מקש האפשרויות במסך הבית עצמו כדי להיכנס למצב עריכה."),
                        color = theme.textColor.copy(alpha = 0.6f),
                        fontSize = FutureTypography.label,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }
        }
        if (viewModel.isApplyingWallpaper.value) {
            Box(modifier = Modifier.fillMaxSize().background(theme.futureTheme.scrimColor), contentAlignment = Alignment.Center) {
                FutureSpinner(theme = theme.futureTheme)
            }
        }
    }
}

/** ארבעת סגנונות שורת המצב, עם תמונה של כל אחד מתוך העיצוב (drawable-nodpi, 640×64). */
private data class StatusBarStyle(val id: String, val title: String, val summary: String, val preview: Int)

private val StatusBarTones = listOf(
    StatusBarStyle("auto", "אוטומטי", "בצבע של האפליקציה שמתחת - לבן, שחור או צבע הכותרת שלה", 0),
    StatusBarStyle("transparent", "שקוף", "תמיד שקוף, הטקסט כהה או בהיר לפי הרקע", 0),
    StatusBarStyle("dark", "כהה", "תמיד שחור עם טקסט לבן", 0),
)

private val StatusBarStyles = listOf(
    StatusBarStyle("classic", tr("קלאסי"), tr("השורה המוכרת, עם נקודה כשיש התראות"), com.future.settings.R.drawable.statusbar_style_classic),
    StatusBarStyle("quiet", tr("שקט"), tr("שעה ואייקוני האפליקציות עם התראות, בלי קישוטים"), com.future.settings.R.drawable.statusbar_style_quiet),
    StatusBarStyle("capsules", tr("כמוסות"), tr("השעה וההתראות בכמוסה אחת, מצב המכשיר בשנייה"), com.future.settings.R.drawable.statusbar_style_capsules),
    StatusBarStyle("centered", tr("שעון במרכז"), tr("תאריך והתראות בצד, השעה באמצע השורה"), com.future.settings.R.drawable.statusbar_style_centered),
)

/** שורת בחירה בכרטיס: תמונה של הסגנון, שם, הסבר וסימון בחירה - אישור בוחר. */
@Composable
private fun StatusBarStyleOption(style: StatusBarStyle, selected: Boolean, theme: ThemeConfig, onSelect: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor = animateColorAsState(
        if (isFocused) theme.textColor.copy(alpha = 0.06f) else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "barStyleBg"
    )
    val borderColor = animateColorAsState(
        if (isFocused) theme.futureTheme.readableAccentColor else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "barStyleBorder"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent {
                if (it.type == KeyEventType.KeyDown && (it.key == Key.DirectionCenter || it.key == Key.Enter || it.key == Key.NumPadEnter)) {
                    onSelect(); true
                } else false
            }
            .focusable().bringIntoViewOnFocus()
            .cardRowFocus({ bgColor.value }, { borderColor.value }, fallbackShape = FutureShapes.lg)
            .clickable { onSelect() }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // התמונה מצוירת על שחור כמו שורת המצב האמיתית, גם בערכת נושא בהירה
        if (style.preview != 0) androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(style.preview),
            contentDescription = style.title,
            contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .clip(FutureShapes.md)
                .background(Color.Black)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = style.title, fontSize = theme.titleFontSize, color = theme.textColor, fontWeight = FontWeight.SemiBold)
                Text(text = style.summary, fontSize = theme.summaryFontSize, color = theme.textColor.copy(alpha = 0.6f))
            }
            Icon(
                imageVector = if (selected) FutureIcons.RadioButtonChecked else FutureIcons.RadioButtonUnchecked,
                contentDescription = if (selected) tr("נבחר") else null,
                tint = if (selected) theme.textColor else theme.textColor.copy(alpha = 0.3f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun WallpaperPhotoTile(photo: com.future.settings.utils.WallpaperPhoto, theme: ThemeConfig, onClick: () -> Unit) {
    val context = LocalContext.current
    var isFocused by remember { mutableStateOf(false) }
    // תמונה ממוזערת מהרשת (200x300), עם מטמון בזיכרון ובדיסק - כמו באפליקציית הטפטים.
    val thumb by produceState(com.future.settings.utils.WallpaperPhotos.peek(photo.thumbUrl), photo.thumbUrl) {
        if (value == null) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.future.settings.utils.WallpaperPhotos.load(context, photo.thumbUrl, 300)
            }
        }
    }
    Column(
        modifier = Modifier
            .padding(6.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent {
                if (it.type == KeyEventType.KeyDown && (it.key == Key.DirectionCenter || it.key == Key.Enter || it.key == Key.NumPadEnter)) {
                    onClick(); true
                } else false
            }
            .focusable().bringIntoViewOnFocus()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.62f)
                .clip(FutureShapes.lg)
                .background(theme.textColor.copy(alpha = 0.08f))
                .then(
                    if (isFocused) Modifier.border(width = FutureDimens.focusBorderControl, color = theme.futureTheme.readableAccentColor, shape = FutureShapes.lg)
                    else Modifier
                )
        ) {
            thumb?.let {
                androidx.compose.foundation.Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = photo.title,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.matchParentSize().clip(FutureShapes.lg)
                )
            }
        }
        Text(
            text = photo.title,
            fontSize = FutureTypography.label,
            color = theme.textColor.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun BatteryScreen(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel) {
    var battTaps by remember { mutableStateOf(0) }
    var battMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { viewModel.loadBatteryExtended() }
    val extended = viewModel.batteryExtended.value

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader(tr("סוללה"), theme) { navController.popBackStack() }
            LazyColumn {
                item {
                    // הקשה 5 פעמים על האחוז חושפת מסר "אישיות" - אבל תמיד לפי המצב
                    // האמיתי של הסוללה כרגע, לא טקסט קבוע.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                            .clickable {
                                battTaps++
                                if (battTaps >= 5) {
                                    battTaps = 0
                                    val percent = viewModel.batteryPercent.value
                                    battMessage = when {
                                        viewModel.isCharging.value -> tr("טוען כוח... ⚡ עוד קצת ונהיה במיטבנו")
                                        percent >= 80 -> tr("מרגיש חזק היום! 💪 {0}% זה כוח אמיתי", percent)
                                        percent >= 30 -> tr("עוד יש לאן להתקדם, אבל בסך הכל מסתדרים")
                                        else -> tr("המצב קצת דחוק... אולי כדאי לחפש שקע 🔌")
                                    }
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("${viewModel.batteryPercent.value}%", fontSize = FutureTypography.hero, fontWeight = FontWeight.Bold, color = theme.textColor)
                        Text(
                            if (viewModel.isCharging.value) tr("בטעינה") else tr("לא בטעינה"),
                            fontSize = FutureTypography.body,
                            color = theme.textColor.copy(alpha = 0.6f)
                        )
                    }
                }
                item {
                    SettingsCard(theme) {
                        SettingSwitch(tr("חיסכון בחשמל"), tr("מאט רקע ואנימציות כדי שהסוללה תחזיק יותר"), viewModel.batterySaver.value, { viewModel.toggleBatterySaver() }, theme)
                        SettingDivider(theme)
                        VolumeSlider(
                            tr("חיסכון אוטומטי ב-{0}%", viewModel.lowBatteryWarningLevel.value),
                            viewModel.lowBatteryWarningLevel.value / 100f,
                            theme,
                            tr("חיסכון בחשמל יידלק לבד כשהסוללה יורדת לרמה הזו")
                        ) { viewModel.setLowBatteryWarningLevel((it * 100).toInt().coerceIn(0, 100)) }
                        SettingDivider(theme)
                        SettingItem(tr("חשמל מתקדם"), tr("סוללה מותאמת והמתנה לאפליקציות"), FutureIcons.BatterySaver, theme) { navController.navigate(com.future.settings.utils.ExtraSystemSettings.POWER.route) }
                    }
                }
                item { SettingHeader(tr("מצב הסוללה"), theme) }
                item {
                    SettingsCard(theme) {
                        val details = viewModel.batteryDetails.value
                        SettingItem(tr("טמפרטורה"), "%.1f°C".format(details.tempCelsius), FutureIcons.Thermostat, theme, showChevron = false)
                        SettingDivider(theme)
                        SettingItem(tr("מתח"), "%.2fV".format(details.voltageVolts), FutureIcons.Bolt, theme, showChevron = false)
                        SettingDivider(theme)
                        SettingItem(tr("תקינות"), details.healthLabel, FutureIcons.HealthAndSafety, theme, showChevron = false)
                    }
                }
                item { SettingHeader(tr("מידע סוללה מפורט"), theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem(tr("מחזורי טעינה"), extended.cycleCount?.toString() ?: tr("לא זמין במכשיר זה"), FutureIcons.Autorenew, theme, showChevron = false)
                        SettingDivider(theme)
                        SettingItem(tr("קיבולת נטענת"), extended.chargeCounterMah?.let { "$it mAh" } ?: tr("לא זמין"), FutureIcons.BatteryChargingFull, theme, showChevron = false)
                        SettingDivider(theme)
                        SettingItem(tr("זרם נוכחי"), extended.currentNowMa?.let { "$it mA" } ?: tr("לא זמין"), FutureIcons.Bolt, theme, showChevron = false)
                    }
                }
            }
        }
        battMessage?.let { msg -> EasterEggMessage("🔋", tr("מצב הסוללה"), msg) { battMessage = null } }
    }
}

@Composable
fun StorageScreen(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val info = remember { com.future.settings.utils.SystemInteractor(context).getStorageInfo() }
    val usedGb = info.usedBytes / (1024.0 * 1024.0 * 1024.0)
    val totalGb = info.totalBytes / (1024.0 * 1024.0 * 1024.0)
    val fraction = if (info.totalBytes > 0) (info.usedBytes.toFloat() / info.totalBytes.toFloat()) else 0f
    var storageTaps by remember { mutableStateOf(0) }
    var storageMessage by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader(tr("אחסון"), theme) { navController.popBackStack() }
            LazyColumn {
                item {
                    // הקשה 5 פעמים על פס האחסון חושפת מסר לפי האחוז האמיתי בשימוש.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .clickable {
                                storageTaps++
                                if (storageTaps >= 5) {
                                    storageTaps = 0
                                    storageMessage = when {
                                        fraction > 0.9f -> tr("כמעט מלא לגמרי! אולי כדאי לפנות קצת מקום 😅")
                                        fraction > 0.6f -> tr("יש שימוש נאה באחסון - עדיין יש לאן לגדול")
                                        else -> tr("יש המון מקום פנוי - תרגיש חופשי לצבור עוד קבצים 🎉")
                                    }
                                }
                            }
                    ) {
                        Text(
                            text = tr("%.1f GB מתוך %.1f GB בשימוש").format(usedGb, totalGb),
                            color = theme.textColor,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(FutureShapes.xs)
                                .background(theme.surfaceColor)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                                    .clip(FutureShapes.xs)
                                    .background(theme.primaryColor)
                            )
                        }
                    }
                }
                item {
                    SettingsCard(theme) {
                        val freeing = viewModel.isFreeingSpace.value
                        SettingItem(
                            if (freeing) tr("מפנה מקום") else tr("פינוי מקום עכשיו"),
                            tr("ניקוי קבצי מטמון של המערכת"),
                            FutureIcons.CleaningServices,
                            theme,
                            showChevron = false
                        ) {
                            if (!freeing) viewModel.freeUpSpace()
                        }
                    }
                }
            }
        }
        storageMessage?.let { msg -> EasterEggMessage("💾", tr("סטטוס אחסון"), msg) { storageMessage = null } }
    }
}

@Composable
fun GeneralScreen(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    var showRestartConfirm by remember { mutableStateOf(false) }
    // איפוס FutureOS מחזיר את ההגדרות לברירת מחדל - לפני כן מבקשים אישור (SET-21).
    var showResetFutureOsConfirm by remember { mutableStateOf(false) }
    if (showResetFutureOsConfirm) {
        ConfirmDialog(
            theme = theme,
            title = tr("איפוס הגדרות FutureOS"),
            message = "ההגדרות יחזרו לברירת המחדל: מצב כהה, צבע הדגשה וגודל טקסט. להמשיך?",
            confirmLabel = "אפס",
            onConfirm = { showResetFutureOsConfirm = false; viewModel.resetFutureOsSettings() },
            onDismiss = { showResetFutureOsConfirm = false }
        )
    }
    var showShutdownConfirm by remember { mutableStateOf(false) }
    var showMultiWindowConfirm by remember { mutableStateOf(false) }

    if (showMultiWindowConfirm) {
        ConfirmDialog(
            theme = theme,
            title = tr("חלון מרובה"),
            message = tr("הפעלת האפשרות הזו עלולה לגרום למסכים מסוימים להיפתח במצב מפוצל שקשה לצאת ממנו במכשיר הזה, שכן אין תמיכה במגע. להמשיך?"),
            confirmLabel = tr("הפעל"),
            onConfirm = { showMultiWindowConfirm = false; viewModel.toggleMultiWindowForced() },
            onDismiss = { showMultiWindowConfirm = false }
        )
    }

    if (showRestartConfirm) {
        ConfirmDialog(
            theme = theme,
            title = tr("הפעלה מחדש"),
            message = tr("המכשיר יופעל מחדש כעת."),
            confirmLabel = tr("הפעל מחדש"),
            onConfirm = { showRestartConfirm = false; viewModel.restartDevice() },
            onDismiss = { showRestartConfirm = false }
        )
    }
    if (showShutdownConfirm) {
        ConfirmDialog(
            theme = theme,
            title = tr("כיבוי המכשיר"),
            message = tr("המכשיר יכבה כעת. תצטרך ללחוץ על מקש ההפעלה כדי להדליק אותו שוב."),
            confirmLabel = tr("כבה"),
            onConfirm = { showShutdownConfirm = false; viewModel.shutdownDevice() },
            onDismiss = { showShutdownConfirm = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader(tr("כללי"), theme) { navController.popBackStack() }
            LazyColumn {
                item { SettingHeader(tr("תאריך ושעה"), theme) }
                item {
                    SettingsCard(theme) {
                        SettingSwitch(tr("תצוגת 24 שעות"), if (viewModel.use24Hour.value) "14:30" else "\u20662:30 PM\u2069" /* LRI…PDI: היה "PM 2:30" */, viewModel.use24Hour.value, { viewModel.toggle24Hour() }, theme)
                        SettingDivider(theme)
                        SettingSwitch(tr("שעה ותאריך אוטומטיים"), tr("נלקחים מהרשת הסלולרית"), viewModel.autoTime.value, { viewModel.toggleAutoTime() }, theme)
                        SettingDivider(theme)
                        SettingSwitch(tr("אזור זמן אוטומטי"), tr("מתעדכן לבד כשעוברים מדינה"), viewModel.autoTimeZone.value, { viewModel.toggleAutoTimeZone() }, theme)
                    }
                }
                item { SettingHeader(tr("שפה ונגישות"), theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem(tr("שפת מערכת"), tr("השפה של התפריטים וההודעות"), FutureIcons.Translate, theme) { navController.navigate(Screen.Language.route) }
                        SettingDivider(theme)
                        SettingItem(tr("מקלדת ושפות הקלדה"), tr("באילו שפות מקלידים ובאילו יש ניבוי"), FutureIcons.Keyboard, theme) {
                            navController.navigate(Screen.KeyboardLanguages.route)
                        }
                        SettingDivider(theme)
                        SettingItem(tr("גודל טקסט"), tr("נמצא במסך תצוגה"), FutureIcons.Accessibility, theme) { navController.navigate(Screen.Display.route) }
                        SettingDivider(theme)
                        SettingSwitch(tr("היפוך צבעים"), tr("לבן הופך לשחור ולהפך, לניגודיות חזקה"), viewModel.colorInversion.value, { viewModel.toggleColorInversion() }, theme)
                        SettingDivider(theme)
                        SettingItem(tr("נגישות"), tr("ניגודיות, תיקון צבעים, שמע והקראה"), FutureIcons.Accessibility, theme) { navController.navigate(com.future.settings.utils.ExtraSystemSettings.ACCESSIBILITY.route) }
                    }
                }
                item { SettingHeader(tr("אבטחה"), theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem(tr("התראות ומיקוד"), tr("השתקה בזמן מיקוד ובשעות שינה"), FutureIcons.Notifications, theme) {
                            navController.navigate(Screen.NotificationsFocus.route)
                        }
                    }
                }
                item { SettingHeader(tr("בטיחות וחירום"), theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem(tr("SOS ואיש קשר לחירום"), tr("שליחת מיקום והודעת חירום"), FutureIcons.Sos, theme) {
                            navController.navigate(Screen.Sos.route)
                        }
                    }
                }
                item { SettingHeader("FutureOS", theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem(tr("איפוס הגדרות FutureOS"), tr("מחזיר מצב כהה, צבע הדגשה וגודל טקסט לברירת המחדל"), FutureIcons.RestartAlt, theme) {
                            showResetFutureOsConfirm = true
                        }
                    }
                }
                item { SettingHeader(tr("מתקדם"), theme) }
                item {
                    SettingsCard(theme) {
                        val nfcAvailable = remember { viewModel.getSystemInteractor().isNfcAvailable() }
                        if (nfcAvailable) {
                            SettingSwitch("NFC", tr("תקשורת מגע קצרה, למשל לתשלום"), viewModel.nfcEnabled.value, { viewModel.toggleNfc() }, theme)
                            SettingDivider(theme)
                        }
                        SettingSwitch(tr("השאר פעיל בזמן טעינה"), tr("המסך לא יכבה כשהמכשיר מחובר למטען"), viewModel.stayAwakeWhileCharging.value, { viewModel.toggleStayAwakeWhileCharging() }, theme)
                        SettingDivider(theme)
                        SettingSwitch(tr("חלון מרובה"), "מכריח כל אפליקציה להיפתח גם בחלון מפוצל (${Char(0x2068)}Multi Window${Char(0x2069)})", viewModel.multiWindowForced.value, {
                            if (viewModel.multiWindowForced.value) {
                                // כיבוי הוא "דלת היציאה" - לא דורש אישור.
                                viewModel.toggleMultiWindowForced()
                            } else {
                                showMultiWindowConfirm = true
                            }
                        }, theme)
                        SettingDivider(theme)
                        SettingItem(tr("הפעלה מחדש"), tr("כיבוי והדלקה של המכשיר"), FutureIcons.RestartAlt, theme, showChevron = false) { showRestartConfirm = true }
                        SettingDivider(theme)
                        SettingItem(tr("כיבוי המכשיר"), tr("יבקש אישור לפני הכיבוי"), FutureIcons.PowerSettingsNew, theme, showChevron = false) { showShutdownConfirm = true }
                    }
                }
            }
        }
    }
}

/**
 * שפות המקלדת: לכל שפה OK מחליף כבויה → הקלדה בלבד → הקלדה וניבוי. # במקלדת
 * עובר רק על השפות הפעילות. אותו מידע בדיוק כמו "שפות מקלדת" במרכז הבקרה.
 */
@Composable
fun KeyboardLanguagesScreen(navController: NavController, theme: ThemeConfig) {
    val context = LocalContext.current
    var languages by remember { mutableStateOf(emptyList<com.future.sharednav.keyboard.KeyboardLanguage>()) }
    var predictive by remember { mutableStateOf(true) }
    var loaded by remember { mutableStateOf(false) }
    var loadFailed by remember { mutableStateOf(false) }
    // ה-ContentProvider של המקלדת בתהליך אחר, שלפעמים צריך לעלות מאפס - לא על
    // ה-main thread בזמן ה-composition (המסך קפא עד שהמקלדת עלתה).
    LaunchedEffect(Unit) {
        // כשל בקריאה לא מפיל את המסך: מוצגת הודעה במקום מסך ריק.
        runCatching {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.future.sharednav.keyboard.KeyboardSettingsClient.languages(context) to
                    com.future.sharednav.keyboard.KeyboardSettingsClient.isPredictiveEnabled(context)
            }
        }.onSuccess { (langs, pred) ->
            languages = langs
            predictive = pred
        }.onFailure {
            loadFailed = true
        }
        loaded = true
    }
    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader(tr("מקלדת ושפות הקלדה"), theme) { navController.popBackStack() }
            LazyColumn {
                item {
                    SettingsCard(theme) {
                        SettingSwitch(tr("ניבוי טקסט"), tr("מתג כללי - גם במרכז הבקרה"), predictive, {
                            predictive = !predictive
                            com.future.sharednav.keyboard.KeyboardSettingsClient.setPredictiveEnabled(context, predictive)
                        }, theme)
                    }
                }
                item { SettingHeader(tr("שפות"), theme) }
                item {
                    Text(
                        text = tr("OK מחליף מצב: כבויה · הקלדה בלבד · הקלדה וניבוי. # במקלדת עובר רק על השפות הפעילות."),
                        color = theme.textColor.copy(alpha = 0.6f),
                        fontSize = FutureTypography.label,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
                if (!loaded) {
                    // עד שהמקלדת עונה - בלי "המקלדת לא מותקנת" מהבהב.
                } else if (languages.isEmpty()) {
                    item {
                        Text(
                            if (loadFailed) "לא ניתן לטעון את הגדרות המקלדת" else tr("המקלדת לא מותקנת"),
                            color = theme.textColor.copy(alpha = 0.6f),
                            fontSize = FutureTypography.body,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                } else {
                    item {
                        SettingsCard(theme) {
                            languages.forEachIndexed { index, lang ->
                                SettingItem(
                                    lang.name,
                                    com.future.sharednav.keyboard.KeyboardSettingsClient.stateLabel(lang),
                                    if (lang.enabled) Icons.Rounded.CheckCircle else FutureIcons.Language,
                                    theme,
                                    showChevron = false
                                ) {
                                    val updated = com.future.sharednav.keyboard.KeyboardSettingsClient.cycleLanguage(context, lang)
                                    languages = languages.map { if (it.code == lang.code) updated else it }
                                }
                                if (index < languages.lastIndex) SettingDivider(theme)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageScreen(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel) {
    val languages = listOf("עברית" to "he-IL", "English" to "en-US", "العربية" to "ar-SA", "Русский" to "ru-RU")
    val current = remember { viewModel.getSystemInteractor().getCurrentLocaleTag() }
    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader(tr("שפת מערכת"), theme) { navController.popBackStack() }
            LazyColumn {
                item {
                    Text(
                        text = tr("שינוי שפת המערכת דורש הרשאת root ועשוי לדרוש הפעלה מחדש של אפליקציות פתוחות."),
                        color = theme.textColor.copy(alpha = 0.6f),
                        fontSize = FutureTypography.label,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                item {
                    SettingsCard(theme) {
                        languages.forEachIndexed { index, (label, tag) ->
                            SettingItem(label, null, if (current.startsWith(tag.take(2))) Icons.Rounded.CheckCircle else FutureIcons.Language, theme, showChevron = false) {
                                viewModel.setSystemLanguage(tag)
                            }
                            if (index < languages.lastIndex) SettingDivider(theme)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityScreen(navController: NavController, theme: ThemeConfig) {
    val context = LocalContext.current
    // מרכז האבטחה: המצב נקרא מחדש בכל חזרה למסך - למשל אחרי הגדרת קוד ב-FutureUI
    var status by remember { mutableStateOf(com.future.settings.theme.SecurityStatusClient.get(context)) }
    var threats by remember { mutableStateOf<com.future.settings.theme.ThreatReport?>(null) }
    var scanning by remember { mutableStateOf(true) }
    var scanTick by remember { mutableIntStateOf(0) }
    // פעולת חסימה/שחרור רצה (root דרך FutureUI) - לא מתחילים עוד אחת במקביל
    var acting by remember { mutableStateOf(false) }
    var dbUpdating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                status = com.future.settings.theme.SecurityStatusClient.get(context)
                scanTick++
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    // סריקת האיומים רצה ב-FutureUI ועלולה לפתוח סשן root - תמיד ברקע, ושוב בכל חזרה למסך
    LaunchedEffect(scanTick) {
        if (scanTick == 0) return@LaunchedEffect
        scanning = true
        threats = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.future.settings.theme.ThreatClient.scan(context)
        }
        scanning = false
    }
    val debugSigned = remember { isDebugSigned(context) }
    val openLockSettings: () -> Unit = {
        safeStartActivity(
            context,
            Intent(FUTURE_UI_LOCK_SETTINGS_ACTION).setPackage(FUTURE_UI_PACKAGE),
            tr("לא ניתן לפתוח את הגדרות מסך הנעילה")
        )
    }
    val openDeveloperOptions: () -> Unit = {
        safeStartActivity(
            context,
            Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
            "לא ניתן לפתוח את אפשרויות המפתחים"
        )
    }
    val checks = securityChecks(status)
    val passed = checks.count { it.ok }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader(tr("אבטחה"), theme) { navController.popBackStack() }
            LazyColumn {
                item {
                    SettingsCard(theme) {
                        // ממצא חמור (שליטה במסך, מנהל מכשיר, תעודה...) מוריד ל"נמוכה" בלי קשר לבדיקות;
                        // ממצא "לבדיקה" מונע "גבוהה". הממצאים לא נספרים בתוך הבדיקות.
                        val worst = threats?.worst ?: 0
                        val level = when {
                            !status.linked -> "לא ידועה"
                            worst >= 2 -> "נמוכה"
                            // "גבוהה" רק אחרי סריקה שהסתיימה - בלי תוצאה אין על מה להסתמך
                            passed == checks.size && threats != null && worst == 0 -> "גבוהה"
                            checks.first().ok && passed >= checks.size - 2 -> "בינונית"
                            else -> "נמוכה"
                        }
                        val findings = threats?.open?.size ?: 0
                        SettingItem(
                            "רמת האבטחה: $level",
                            if (!status.linked) "אין חיבור לממשק המערכת (FutureUI)"
                            else "$passed מתוך ${checks.size} בדיקות עברו" + when (findings) {
                                0 -> ""
                                1 -> ", ממצא אחד בסריקה"
                                else -> ", $findings ממצאים בסריקה"
                            },
                            if (level == "גבוהה") FutureIcons.Security else FutureIcons.PrivacyTip,
                            theme,
                            showChevron = false,
                            infoFocusable = true
                        )
                    }
                }

                item { SettingHeader("בדיקות", theme) }
                item {
                    SettingsCard(theme) {
                        checks.forEachIndexed { index, check ->
                            SettingItem(
                                check.title,
                                check.summary,
                                if (check.ok) FutureIcons.Check else FutureIcons.Error,
                                theme,
                                showChevron = !check.ok,
                                infoFocusable = true,
                                onClick = if (check.ok) null else when (check.fix) {
                                    SecurityFix.LOCK_SETTINGS -> openLockSettings
                                    SecurityFix.DEVELOPER -> openDeveloperOptions
                                }
                            )
                            if (index < checks.lastIndex) SettingDivider(theme)
                        }
                    }
                }

                // הגנה מפני תקיפות: סריקה, מאגר נוזקות וחסימה (ThreatScanner / ThreatGuard ב-FutureUI)
                item { SettingHeader("הגנה מפני תקיפות", theme) }
                item {
                    SettingsCard(theme) {
                        val report = threats
                        when {
                            report == null && scanning -> SettingItem(
                                "סורק את הטלפון…",
                                "בודק אפליקציות, הרשאות, נוזקות ידועות והגדרות רשת",
                                FutureIcons.Search,
                                theme,
                                showChevron = false,
                                infoFocusable = true
                            )
                            report == null -> SettingItem(
                                "סריקת איומים לא זמינה",
                                "צריך את הגרסה החדשה של ממשק המערכת (FutureUI)",
                                FutureIcons.Error,
                                theme,
                                showChevron = false,
                                infoFocusable = true
                            )
                            else -> {
                                val openCount = report.open.size
                                val blockedCount = report.findings.count { it.status == com.future.settings.theme.Threat.BLOCKED }
                                val title = when {
                                    openCount == 0 && report.unchecked.isNotEmpty() -> "הסריקה לא הושלמה"
                                    openCount == 0 && blockedCount == 0 -> "לא נמצאו איומים"
                                    openCount == 0 -> if (blockedCount == 1) "איום אחד נחסם" else "$blockedCount איומים נחסמו"
                                    openCount == 1 -> "ממצא אחד דורש החלטה"
                                    else -> "$openCount ממצאים דורשים החלטה"
                                }
                                val parts = buildList {
                                    add("נסרקו ${report.scannedApps} אפליקציות")
                                    if (report.dbSize > 0) add(
                                        "מאגר נוזקות מ-${formatPatch(report.dbDate)} (${report.dbSize} סימנים" +
                                            (if (report.dbFromUpdate) ", עודכן מהרשת)" else ")")
                                    )
                                    if (report.unchecked.isNotEmpty()) add("לא נבדק: " + report.unchecked.joinToString(", ") { uncheckedName(it) })
                                    if (!report.appOpsChecked) add("הרשאות חלון צף והתקנה לא נבדקו (אין root)")
                                }
                                SettingItem(
                                    title,
                                    parts.joinToString(". "),
                                    if (openCount == 0 && report.unchecked.isEmpty()) FutureIcons.Check else FutureIcons.Error,
                                    theme,
                                    showChevron = false,
                                    infoFocusable = true
                                )
                                report.findings.forEach { threat ->
                                    SettingDivider(theme)
                                    val (baseTitle, baseSummary, icon) = describeThreat(threat)
                                    val (rowTitle, rowSummary) = when {
                                        threat.status == com.future.settings.theme.Threat.BLOCKED && (threat.kind == "KNOWN_MALWARE" || threat.kind == "SUSPICIOUS") ->
                                            "הושהתה: $baseTitle" to "אי אפשר לפתוח אותה עד שתחליט. אם אתה מכיר אותה ובטוח בה, לחץ כדי לבטל את ההשהיה ולסמן כמהימנה"
                                        threat.status == com.future.settings.theme.Threat.BLOCKED ->
                                            "נחסם: $baseTitle" to "היכולת בוטלה, האפליקציה עצמה לא נמחקה. אם זה היית אתה, לחץ כדי להחזיר ולסמן כמהימן"
                                        threat.status == com.future.settings.theme.Threat.ALLOWED ->
                                            "אושר: $baseTitle" to "סימנת את זה כמהימן, ולכן זה לא מוריד את רמת האבטחה. לחץ כדי לחסום"
                                        threat.kind == "SUSPICIOUS" -> baseTitle to "$baseSummary. לחץ כדי להשהות אותה"
                                        threat.blockable -> baseTitle to "$baseSummary. לחץ כדי לחסום"
                                        else -> baseTitle to baseSummary
                                    }
                                    SettingItem(
                                        rowTitle,
                                        rowSummary,
                                        if (threat.status == com.future.settings.theme.Threat.BLOCKED) FutureIcons.Security else icon,
                                        theme,
                                        onClick = {
                                            if (acting) return@SettingItem
                                            // מה שנחסם או הושהה תמיד אפשר להחזיר; השאר פותח את המסך המתאים
                                            if (!threat.blockable && threat.status != com.future.settings.theme.Threat.BLOCKED) {
                                                fixThreat(context, threat)
                                                return@SettingItem
                                            }
                                            acting = true
                                            scope.launch {
                                                val restore = threat.status == com.future.settings.theme.Threat.BLOCKED
                                                val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                    if (restore) com.future.settings.theme.ThreatClient.allow(context, threat.key)
                                                    else com.future.settings.theme.ThreatClient.block(context, threat.key)
                                                }
                                                android.widget.Toast.makeText(
                                                    context,
                                                    when {
                                                        ok && restore -> "הוחזר וסומן כמהימן"
                                                        ok -> "נחסם"
                                                        restore -> "לא ניתן להחזיר (אין root, או שהאפליקציה הוסרה)"
                                                        else -> "לא ניתן לחסום (אין root, או שזה כבר השתנה)"
                                                    },
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                                acting = false
                                                scanTick++
                                            }
                                        }
                                    )
                                }
                                SettingDivider(theme)
                                SettingSwitch(
                                    "חסימה אוטומטית",
                                    if (report.autoBlock) "יכולת מסוכנת שאפליקציה זרה מקבלת נחסמת מיד, ואתה מקבל התראה. מה שהיה פעיל לפני ההפעלה מחכה להחלטה שלך"
                                    else "כבויה: ממשק המערכת רק מתריע, ולא חוסם",
                                    report.autoBlock,
                                    { on ->
                                        if (!acting) {
                                            acting = true
                                            scope.launch {
                                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                    com.future.settings.theme.ThreatClient.setAutoBlock(context, on)
                                                }
                                                acting = false
                                                scanTick++
                                            }
                                        }
                                    },
                                    theme
                                )
                                SettingDivider(theme)
                                SettingItem(
                                    "הגנה בזמן אמת: פועלת",
                                    "ממשק המערכת בודק כל אפליקציה שמותקנת וכל שינוי בשליטה במסך, בהתראות, ב-SMS וברשת, ומתריע על כל ממצא חדש",
                                    FutureIcons.Security,
                                    theme,
                                    showChevron = false,
                                    infoFocusable = true
                                )
                            }
                        }
                        SettingDivider(theme)
                        SettingItem(
                            if (scanning && report != null) "סורק…" else "סרוק שוב",
                            "סריקה מלאה של האפליקציות והרשת",
                            FutureIcons.Refresh,
                            theme,
                            showChevron = false
                        ) { if (!scanning) scanTick++ }
                        SettingDivider(theme)
                        SettingItem(
                            if (dbUpdating) "מעדכן את מאגר הנוזקות…" else "עדכן את מאגר הנוזקות",
                            "מוריד את הגרסה החתומה של FutureOS. ממשק המערכת בודק את החתימה לפני שהוא טוען אותה",
                            FutureIcons.Download,
                            theme,
                            showChevron = false
                        ) {
                            if (!dbUpdating) {
                                dbUpdating = true
                                scope.launch {
                                    val r = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        com.future.settings.theme.MalwareDbUpdateJob.update(context)
                                    }
                                    android.widget.Toast.makeText(
                                        context,
                                        when {
                                            r.ok -> "המאגר עודכן (${formatPatch(r.date)})"
                                            r.reason == "not_newer" -> "המאגר כבר מעודכן"
                                            r.reason == "download" -> "אין חיבור לשרת העדכונים"
                                            r.reason == "signature" -> "העדכון נדחה: החתימה לא תקינה"
                                            else -> "לא ניתן לעדכן את המאגר כרגע"
                                        },
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                    dbUpdating = false
                                    if (r.ok) scanTick++
                                }
                            }
                        }
                        SettingDivider(theme)
                        SettingItem(
                            "היסטוריית אבטחה",
                            "מה נחסם, מתי, ומה היה המצב לפני כן",
                            FutureIcons.History,
                            theme
                        ) { navController.navigate(Screen.SecurityHistory.route) }
                    }
                }

                if (status.linked && status.hasPin) {
                    item { SettingHeader("ניסיונות פתיחה", theme) }
                    item {
                        SettingsCard(theme) {
                            val summary = when {
                                status.lockoutMs > 0 -> "הטלפון חסום כרגע לניסיונות נוספים (${(status.lockoutMs + 59_999) / 60_000} דק')"
                                status.failedLastWeek == 0 -> "לא היו ניסיונות שגויים בשבוע האחרון"
                                else -> "האחרון: ${formatSecurityTime(status.lastFailedAt)}"
                            }
                            SettingItem(
                                when (status.failedLastWeek) {
                                    0 -> "אין ניסיונות שגויים"
                                    1 -> "ניסיון שגוי אחד השבוע"
                                    else -> "${status.failedLastWeek} ניסיונות שגויים השבוע"
                                },
                                summary,
                                FutureIcons.History,
                                theme,
                                showChevron = false,
                                infoFocusable = true
                            )
                        }
                    }
                }

                // מסך הנעילה של FutureOS. ההגדרות עצמן רצות בתוך FutureUI, כי הקוד
                // ונתוני הפנים מוצפנים במפתח Keystore של FutureUI בלבד; המסך שם
                // דורש את הקוד הנוכחי לפני כל שינוי.
                item { SettingHeader(tr("מסך נעילה"), theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem(tr("מסך נעילה, קוד וזיהוי פנים"), tr("קוד, פתיחה בפנים, שעון, ווידג'טים והתראות"), FutureIcons.Lock, theme) {
                            openLockSettings()
                        }
                    }
                }
                item { SettingHeader(tr("ביומטריה"), theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem(tr("זיהוי פנים וביומטריה"), tr("רישום טביעת אצבע או פנים באנדרואיד"), FutureIcons.Face, theme) {
                            safeStartActivity(
                                context,
                                Intent(Settings.ACTION_BIOMETRIC_ENROLL),
                                tr("המכשיר הזה לא תומך בהרשמה ביומטרית")
                            )
                        }
                    }
                }
                item { SettingHeader(tr("איתור ופרטיות מתקדמת"), theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem(tr("איתור המכשיר (Find My Device)"), "מיקום, נעילה ומחיקה מרחוק דרך שירות איתור המכשיר", FutureIcons.MyLocation, theme) {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.adm")
                            if (launchIntent != null) {
                                safeStartActivity(context, launchIntent, tr("לא ניתן לפתוח את Find My Device"))
                            } else {
                                android.widget.Toast.makeText(context, tr("אפליקציית Find My Device לא מותקנת על המכשיר"), android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                        SettingDivider(theme)
                        SettingItem(tr("אזור פרטי (Private Space)"), tr("אזור מוצפן ומבודד לאפליקציות ונתונים"), FutureIcons.Lock, theme) {
                            safeStartActivity(
                                context,
                                Intent("android.settings.PRIVATE_SPACE_SETTINGS"),
                                tr("אזור פרטי לא נתמך בגרסת האנדרואיד של המכשיר הזה")
                            )
                        }
                    }
                }
                // מה מסך הנעילה לא מגן מפניו - בכנות, כדי שהבדיקות למעלה לא ייתנו תחושת ביטחון כוזבת
                item { SettingHeader("גבולות ההגנה", theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem(
                            if (status.androidLockSet) "קוד נעילה של אנדרואיד: מוגדר" else "קוד נעילה של אנדרואיד: לא מוגדר",
                            "מסך הנעילה של FutureOS רץ כשכבת ממשק. מחשב עם ניפוי באגים ב-USB, או אתחול למצב בטוח, עוקפים אותו. קוד של אנדרואיד מצפין את הנתונים עצמם.",
                            FutureIcons.Info,
                            theme,
                            showChevron = false,
                            infoFocusable = true
                        )
                        SettingDivider(theme)
                        val patch = android.os.Build.VERSION.SECURITY_PATCH
                        val patchOld = patchAgeDays(patch)?.let { it > 365 } ?: false
                        SettingItem(
                            "עדכון האבטחה של אנדרואיד: ${formatPatch(patch)}",
                            if (patchOld) "עברה יותר משנה. פרצות שהתגלו מאז לא תוקנו במכשיר, אז כדאי להתקין רק אפליקציות שאתה מכיר"
                            else "מתעדכן דרך יצרן המכשיר, לא דרך FutureOS",
                            FutureIcons.Security,
                            theme,
                            showChevron = false,
                            infoFocusable = true
                        )
                        if (debugSigned) {
                            SettingDivider(theme)
                            SettingItem(
                                "מפתח החתימה: מפתח פיתוח",
                                "אפליקציות FutureOS חתומות במפתח ה-debug של המחשב שבנה אותן. ההגנות שמבוססות על חתימה בטוחות כל עוד הקובץ הזה לא דולף מהמחשב",
                                FutureIcons.Code,
                                theme,
                                showChevron = false,
                                infoFocusable = true
                            )
                        }
                    }
                }
            }
        }
    }
}

/** כותרת, תיאור ואייקון לממצא של סורק האיומים. */
private fun describeThreat(t: com.future.settings.theme.Threat): Triple<String, String, androidx.compose.ui.graphics.vector.ImageVector> {
    val name = t.label.ifBlank { t.target }
    val prefix = if (t.severity >= 2) "חמור: " else "לבדיקה: "
    return when (t.kind) {
        "KNOWN_MALWARE" -> Triple(
            "$name היא נוזקת ריגול ידועה",
            "חמור: מופיעה במאגר הנוזקות" + (if (t.detail.isNotBlank()) " (${t.detail})" else "") +
                ". לחץ כדי לפתוח את האפליקציה ולהסיר אותה",
            FutureIcons.Error
        )
        "SUSPICIOUS" -> Triple(
            "$name משלבת יכולות של נוזקה",
            prefix + t.detail.split(',').filter { it.isNotBlank() }.joinToString(", ") { reasonName(it) },
            FutureIcons.PrivacyTip
        )
        "SIDELOADED" -> Triple(
            "$name הותקנה ממקור לא מוכר",
            prefix + "לא מחנות אפליקציות מוכרת" + (if (t.detail.isNotBlank() && t.detail != "unknown") " (הותקנה דרך ${t.detail})" else "") +
                ". אם לא אתה התקנת אותה, הסר אותה",
            FutureIcons.Download
        )
        "ACCESSIBILITY" -> Triple(
            "$name שולטת במסך",
            prefix + "שירות נגישות פעיל. הוא קורא כל מה שעל המסך וכל מקש, כולל הקוד. אם לא הפעלת אותו, חסום אותו והסר את האפליקציה",
            FutureIcons.Accessibility
        )
        "NOTIFICATION_LISTENER" -> Triple(
            "$name קוראת את ההתראות",
            prefix + "רואה תוכן הודעות וקודי אימות שמגיעים ב-SMS. אם לא אישרת את זה, חסום",
            FutureIcons.Notifications
        )
        "DEVICE_ADMIN" -> Triple(
            "$name היא מנהלת המכשיר",
            prefix + "יכולה לנעול ולמחוק את הטלפון ולמנוע את הסרתה. אם אתה לא מכיר אותה, בטל את ההרשאה ואז הסר אותה",
            FutureIcons.Lock
        )
        "USER_CA" -> Triple(
            "תעודת אבטחה שהותקנה ידנית",
            prefix + "\"$name\" מאפשרת למי שהתקין אותה לקרוא תעבורה מוצפנת. אם לא התקנת אותה, הסר אותה",
            FutureIcons.Badge
        )
        "PROXY" -> Triple(
            "כל התעבורה עוברת דרך שרת מתווך",
            prefix + "$name. הוגדר מחוץ להגדרות הרשת",
            FutureIcons.Public
        )
        "ADB_WIFI" -> Triple(
            "ניפוי באגים דרך Wi-Fi פועל",
            prefix + "מחשב באותה רשת יכול לנסות להתחבר לטלפון. כבה אותו באפשרויות המפתחים",
            FutureIcons.Wifi
        )
        "SMS_READER" -> Triple(
            "$name קוראת הודעות SMS",
            prefix + "יש לה הרשאה לקרוא הודעות, כולל קודי אימות. אם היא לא צריכה את זה, חסום",
            FutureIcons.Chat
        )
        "OVERLAY" -> Triple(
            "$name מציגה חלונות מעל אפליקציות",
            prefix + "כך מזייפים מסכי כניסה. אם היא לא צריכה את זה, חסום",
            FutureIcons.Visibility
        )
        "INSTALLER" -> Triple(
            "$name יכולה להתקין אפליקציות",
            prefix + "אפליקציה כזו יכולה להוריד ולהתקין אפליקציות נוספות. אם לא ביקשת את זה, חסום",
            FutureIcons.Download
        )
        "VPN" -> Triple(
            "VPN פעיל",
            prefix + "כל התעבורה עוברת דרך שירות ה-VPN. ודא שאתה מכיר אותו",
            FutureIcons.VpnKey
        )
        else -> Triple(name, prefix + "ממצא לא מוכר", FutureIcons.Error)
    }
}

private val THREAT_PKG_RE = Regex("""^[A-Za-z0-9_.]{1,255}$""")

/**
 * לממצאים שאי אפשר לחסום מכאן (נוזקה ידועה, מנהל מכשיר, תעודה, VPN, ניפוי באגים): פותח את
 * המסך שבו מתקנים. שום דבר לא משתנה בלי שהמשתמש מאשר שם.
 */
private fun fixThreat(context: android.content.Context, t: com.future.settings.theme.Threat) {
    val pkgUri = if (THREAT_PKG_RE.matches(t.target)) android.net.Uri.fromParts("package", t.target, null) else null
    val intents = when (t.kind) {
        "ACCESSIBILITY" -> listOf(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        "NOTIFICATION_LISTENER" -> listOf(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        "DEVICE_ADMIN" -> listOf(
            Intent().setClassName("com.android.settings", "com.android.settings.Settings\$DeviceAdminSettingsActivity"),
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        )
        "USER_CA" -> listOf(Intent("com.android.settings.TRUSTED_CREDENTIALS_USER"), Intent(Settings.ACTION_SECURITY_SETTINGS))
        "ADB_WIFI" -> listOf(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
        "VPN" -> listOf(Intent(Settings.ACTION_VPN_SETTINGS))
        "OVERLAY" -> listOfNotNull(pkgUri?.let { Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, it) })
        "INSTALLER" -> listOfNotNull(pkgUri?.let { Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, it) })
        else -> emptyList()
    } + listOfNotNull(pkgUri?.let { Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, it) })
    for (intent in intents) {
        val started = runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
        if (started) return
    }
    android.widget.Toast.makeText(context, "לא ניתן לפתוח את המסך המתאים", android.widget.Toast.LENGTH_SHORT).show()
}

/** האם האפליקציה חתומה במפתח ה-debug של Android Studio (אין keystore.properties). */
private fun reasonName(reason: String): String = when (reason) {
    "accessibility" -> "שולטת במסך"
    "notifications" -> "קוראת התראות"
    "sms" -> "קוראת SMS"
    "admin" -> "מנהלת מכשיר"
    "overlay" -> "חלון מעל אפליקציות"
    "installer" -> "מתקינה אפליקציות"
    "hidden" -> "בלי אייקון במסך הבית"
    "sideloaded" -> "ממקור לא מוכר"
    else -> reason
}

private fun uncheckedName(source: String): String = when (source) {
    "accessibility" -> "שירותי נגישות"
    "notifications" -> "גישה להתראות"
    "device_admin" -> "מנהלי מכשיר"
    "apps" -> "רשימת האפליקציות"
    "signatures" -> "חתימות"
    "certificates" -> "תעודות"
    "malware_db" -> "מאגר הנוזקות"
    else -> source
}

private fun isDebugSigned(context: android.content.Context): Boolean = runCatching {
    val info = context.packageManager.getPackageInfo(
        context.packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
    )
    val signer = info.signingInfo?.apkContentsSigners?.firstOrNull() ?: return@runCatching false
    val cert = java.security.cert.CertificateFactory.getInstance("X.509")
        .generateCertificate(signer.toByteArray().inputStream()) as java.security.cert.X509Certificate
    cert.subjectX500Principal.name.contains("CN=Android Debug")
}.getOrDefault(false)

private fun patchAgeDays(patch: String?): Long? = runCatching {
    val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(patch ?: return null) ?: return null
    (System.currentTimeMillis() - date.time) / (24L * 60 * 60 * 1000)
}.getOrNull()

private fun formatPatch(patch: String?): String {
    val parts = patch?.split('-') ?: return "לא ידוע"
    return if (parts.size >= 2) "${parts[1].trimStart('0')}.${parts[0]}" else patch
}

private enum class SecurityFix { LOCK_SETTINGS, DEVELOPER }

private data class SecurityCheck(val ok: Boolean, val title: String, val summary: String, val fix: SecurityFix)

/** הבדיקות של מרכז האבטחה. הראשונה (קוד נעילה) היא תנאי לכל רמה מעל "נמוכה". */
private fun securityChecks(s: com.future.settings.theme.SecurityStatus): List<SecurityCheck> {
    val adbCheck = SecurityCheck(
        !s.adbEnabled,
        "ניפוי באגים ב-USB",
        if (s.adbEnabled) "פועל: מחשב שמחובר בכבל יכול לעקוף את מסך הנעילה. כבה כשלא מתקינים מהמחשב" else tr("כבוי"),
        SecurityFix.DEVELOPER
    )
    // בלי קישור ל-FutureUI (גרסה ישנה, או ממשק מערכת אחר) מצב מסך הנעילה לא ידוע -
    // לא אומרים "אין קוד" כשאולי יש
    if (!s.linked) return listOf(
        SecurityCheck(false, "מצב מסך הנעילה לא ידוע", "עדכן את ממשק המערכת (FutureUI) כדי לראות את הבדיקות", SecurityFix.LOCK_SETTINGS),
        adbCheck,
    )
    val pinOn = s.lockEnabled && s.hasPin
    return listOf(
        SecurityCheck(
            pinOn,
            if (pinOn) "קוד נעילה פעיל" else "אין קוד נעילה",
            if (pinOn) "צריך קוד כדי לפתוח את הטלפון" else "כל מי שמחזיק בטלפון יכול לפתוח אותו",
            SecurityFix.LOCK_SETTINGS
        ),
        SecurityCheck(
            pinOn && s.autoLockMs <= 30_000L,
            "נעילה אוטומטית",
            when {
                !pinOn -> "פועלת רק כשמוגדר קוד"
                s.autoLockMs == 0L -> "הטלפון ננעל מיד כשהמסך כבה"
                s.autoLockMs <= 30_000L -> "הטלפון ננעל תוך ${s.autoLockMs / 1000} שניות"
                else -> "הטלפון נשאר פתוח ${s.autoLockMs / 60_000} דק' אחרי כיבוי המסך. מומלץ עד 30 שניות"
            },
            SecurityFix.LOCK_SETTINGS
        ),
        SecurityCheck(
            pinOn && s.notificationPrivacy != 0,
            "תוכן התראות במסך הנעילה",
            when {
                !pinOn -> "פועלת רק כשמוגדר קוד"
                s.notificationPrivacy != 0 -> "תוכן ההודעות מוסתר עד שהטלפון מזהה אותך"
                else -> "כל אחד יכול לקרוא הודעות מהמסך הנעול"
            },
            SecurityFix.LOCK_SETTINGS
        ),
        adbCheck,
    )
}

private fun formatSecurityTime(at: Long): String {
    if (at <= 0L) return "לא ידוע"
    val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(at))
    val dayFmt = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
    val today = dayFmt.format(java.util.Date()) == dayFmt.format(java.util.Date(at))
    return if (today) "היום ב-$time" else "${java.text.SimpleDateFormat("d.M", java.util.Locale.getDefault()).format(java.util.Date(at))} ב-$time"
}

private val LEET_SEQUENCE = listOf(1, 3, 3, 7)

@Composable
fun AppsScreen(navController: NavController, theme: ThemeConfig) {
    val context = LocalContext.current
    val installedMap = remember {
        val interactor = com.future.settings.utils.SystemInteractor(context)
        FUTURE_OS_APPS.associate { it.packageName to interactor.isAppInstalled(it.packageName) }
    }
    var leetBuffer by remember { mutableStateOf(listOf<Int>()) }
    var leetMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            // "1337" במקלדת המספרים - עוד קוד סודי, הפעם עם רמז דק ("מפתחים") שהוא
            // שם, בלי לחשוף איך בדיוק מגיעים אליו.
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val digit = digitForKey(event.key) ?: return@onKeyEvent false
                leetBuffer = (leetBuffer + digit).takeLast(LEET_SEQUENCE.size)
                if (leetBuffer == LEET_SEQUENCE) {
                    leetBuffer = emptyList()
                    leetMessage = tr("לא נמצאו באגים. כלומר... אולי כן, אבל הם רשומים כפיצ'רים. 🐛")
                }
                false
            }
    ) {
        Column {
            SmallHeader(tr("אפליקציות"), theme) { navController.popBackStack() }
            LazyColumn {
                // N-03: מסך "ברירות מחדל" (טלפון, הודעות, בית) לא היה נגיש מאף מקום.
                item {
                    SettingsCard(theme) {
                        SettingItem(tr("ברירות מחדל"), "טלפון, הודעות ומסך הבית", FutureIcons.Tune, theme) {
                            navController.navigate(Screen.DefaultApps.route)
                        }
                    }
                }
                item {
                    SettingsCard(theme) {
                        FUTURE_OS_APPS.forEachIndexed { index, app ->
                            val installed = installedMap[app.packageName] == true
                            SettingItem(
                                app.displayName,
                                if (installed) tr("מותקנת") else tr("לא מותקנת"),
                                app.icon,
                                theme
                            ) { navController.navigate(Screen.AppDetail.route(app.packageName)) }
                            if (index < FUTURE_OS_APPS.lastIndex) SettingDivider(theme)
                        }
                    }
                }
            }
        }
        leetMessage?.let { msg -> EasterEggMessage("🐛", tr("מצב מפתח סמוי"), msg) { leetMessage = null } }
    }
}

@Composable
fun AppDetailScreen(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel, packageName: String) {
    val context = LocalContext.current
    val interactor = remember { com.future.settings.utils.SystemInteractor(context) }
    val app = remember(packageName) { FUTURE_OS_APPS.find { it.packageName == packageName } }
    val installed = remember(packageName) { interactor.isAppInstalled(packageName) }
    val versionName = remember(packageName) { if (installed) interactor.getAppVersionName(packageName) else null }
    var showClearDataConfirm by remember { mutableStateOf(false) }
    // הקריאה ל-root (appops) רצה ברקע - לא בתוך ההרכבה, כדי שהמסך לא ייתקע בפתיחה.
    var audioMuted by remember(packageName) { mutableStateOf(false) }
    LaunchedEffect(packageName, installed) {
        if (installed) audioMuted = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { interactor.isAppAudioMuted(packageName) }
    }

    if (showClearDataConfirm) {
        ConfirmDialog(
            theme = theme,
            title = tr("מחיקת נתונים"),
            message = tr("כל הנתונים המקומיים של {0} יימחקו לצמיתות. לא ניתן לבטל פעולה זו.", app?.displayName ?: packageName),
            confirmLabel = tr("מחק נתונים"),
            onConfirm = {
                showClearDataConfirm = false
                viewModel.clearAppData(packageName, app?.displayName ?: packageName)
            },
            onDismiss = { showClearDataConfirm = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader(app?.displayName ?: packageName, theme) { navController.popBackStack() }
            LazyColumn {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (app != null) {
                            Icon(app.icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = theme.primaryColor)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        Text(packageName, fontSize = FutureTypography.label, color = theme.textColor.copy(alpha = 0.5f))
                    }
                }
                item {
                    SettingsCard(theme) {
                        SettingItem(tr("סטטוס"), if (installed) tr("מותקנת") else tr("לא מותקנת"), if (installed) Icons.Rounded.CheckCircle else FutureIcons.Cancel, theme, showChevron = false)
                        if (versionName != null) {
                            SettingDivider(theme)
                            SettingItem(tr("גרסה"), versionName, FutureIcons.Info, theme, showChevron = false)
                        }
                    }
                }
                if (installed) {
                    item { SettingHeader(tr("סאונד"), theme) }
                    item {
                        SettingsCard(theme) {
                            SettingSwitch(
                                tr("השתקת סאונד לאפליקציה זו"),
                                tr("משתיק את פלט האודיו של האפליקציה בלבד"),
                                audioMuted,
                                { newValue ->
                                    // המתג משתנה רק אם הכתיבה למערכת הצליחה.
                                    viewModel.setAppAudioMuted(packageName, newValue) { success -> if (success) audioMuted = newValue }
                                },
                                theme
                            )
                        }
                    }
                    item { SettingHeader(tr("פעולות"), theme) }
                    item {
                        SettingsCard(theme) {
                            SettingItem(tr("פתח אפליקציה"), null, Icons.AutoMirrored.Rounded.Launch, theme, showChevron = false) {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                                if (launchIntent != null) {
                                    safeStartActivity(context, launchIntent, tr("לא ניתן לפתוח את האפליקציה"))
                                } else {
                                    android.widget.Toast.makeText(context, tr("לא ניתן לפתוח את האפליקציה"), android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                            SettingDivider(theme)
                            SettingItem(tr("עצירה בכפייה"), tr("סוגר את האפליקציה אם היא תקועה"), FutureIcons.StopCircle, theme, showChevron = false) {
                                viewModel.forceStopApp(packageName, app?.displayName ?: packageName)
                            }
                            SettingDivider(theme)
                            SettingItem(tr("מחיקת נתונים"), tr("מוחק לצמיתות את כל המידע השמור"), FutureIcons.Delete, theme, showChevron = false) {
                                showClearDataConfirm = true
                            }
                        }
                    }
                }
            }
        }
    }
}

/** דיאלוג אישור גנרי לפעולות הרסניות/בלתי-הפיכות (מחיקת נתונים, אתחול, כיבוי) -
 *  צעד ביניים חובה לפני שקוראים לפעולה בפועל. */
/** דיאלוג אישור לפעולות הרסניות/בלתי-הפיכות (מחיקת נתונים, אתחול, כיבוי) - צעד ביניים
 *  חובה לפני שקוראים לפעולה בפועל. המשטח והכפתורים של הדיזיין סיסטם (היה AlertDialog
 *  של Material עם אדום #FF453A שאינו בפלטה); כאן יש גם שורת הסבר מתחת לכותרת. */
@Composable
fun ConfirmDialog(
    theme: ThemeConfig,
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    FutureDialog(
        theme = theme.futureTheme,
        onDismissRequest = onDismiss,
        title = title,
        buttons = {
            FutureButton(tr("ביטול"), theme.futureTheme, onDismiss, variant = FutureButtonVariant.Secondary)
            FutureButton(confirmLabel, theme.futureTheme, onConfirm, variant = FutureButtonVariant.Destructive)
        },
    ) {
        Text(
            message,
            color = theme.futureTheme.mutedTextColor,
            fontSize = FutureTypography.body,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun PerformanceScreen(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel) {
    LaunchedEffect(Unit) { viewModel.refreshRamInfo() }
    val ram = viewModel.ramInfo.value
    val usedGb = (ram.totalBytes - ram.availBytes) / (1024.0 * 1024.0 * 1024.0)
    val totalGb = ram.totalBytes / (1024.0 * 1024.0 * 1024.0)
    val fraction = if (ram.totalBytes > 0) ((ram.totalBytes - ram.availBytes).toFloat() / ram.totalBytes.toFloat()) else 0f
    val cpuInfo = remember { viewModel.getSystemInteractor().getCpuInfo() }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader(tr("ביצועים ותחזוקה"), theme) { navController.popBackStack() }
            LazyColumn {
                item { SettingHeader(tr("זיכרון (RAM)"), theme) }
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                        Text(
                            text = tr("%.1f GB מתוך %.1f GB בשימוש").format(usedGb, totalGb),
                            color = theme.textColor,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(FutureShapes.xs).background(theme.surfaceColor)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                                    .clip(FutureShapes.xs)
                                    .background(if (ram.lowMemory) theme.dangerColor else theme.primaryColor)
                            )
                        }
                        if (ram.lowMemory) {
                            Text(tr("זיכרון נמוך - מומלץ לבצע אופטימיזציה"), fontSize = FutureTypography.label, color = theme.dangerColor, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
                item {
                    SettingsCard(theme) {
                        SettingItem(tr("מעבד"), cpuInfo, FutureIcons.Memory, theme, showChevron = false)
                    }
                }
                item { SettingHeader(tr("תחזוקה"), theme) }
                item {
                    SettingsCard(theme) {
                        val optimizing = viewModel.isOptimizing.value
                        SettingItem(
                            if (optimizing) tr("מבצע אופטימיזציה") else tr("אופטימיזציה מהירה"),
                            tr("ניקוי זיכרון וקבצי מטמון"),
                            FutureIcons.Speed,
                            theme,
                            showChevron = false
                        ) {
                            if (!optimizing) viewModel.optimizeNow()
                        }
                        SettingDivider(theme)
                        SettingItem(tr("אבחון מכשיר"), tr("בדיקת מגע, רטט, רמקול וחיישנים"), FutureIcons.Troubleshoot, theme) {
                            navController.navigate(Screen.Diagnostics.route)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScreenTimeScreen(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel) {
    LaunchedEffect(Unit) { viewModel.loadScreenTime() }
    val summary = viewModel.screenTimeSummary.value

    val itemsTail: @Composable () -> Unit = {
        SettingsCard(theme) {
            SettingItem(tr("טיימרים לאפליקציות"), tr("הגבלת זמן שימוש יומי"), FutureIcons.HourglassEmpty, theme) {
                navController.navigate(Screen.AppTimers.route)
            }
        }
    }

    fun formatDuration(millis: Long): String {
        val totalMinutes = millis / 60000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) tr("{0} שעות ו-{1} דקות", hours, minutes) else tr("{0} דקות", minutes)
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader(tr("זמן מסך"), theme) { navController.popBackStack() }
            LazyColumn {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(FutureIcons.HourglassEmpty, contentDescription = null, modifier = Modifier.size(48.dp), tint = theme.primaryColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(formatDuration(summary.totalMillis), fontSize = FutureTypography.display, fontWeight = FontWeight.Bold, color = theme.textColor)
                        Text(tr("שימוש היום"), fontSize = FutureTypography.summary, color = theme.textColor.copy(alpha = 0.6f))
                    }
                }
                if (summary.topApps.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(tr("עדיין אין מספיק נתוני שימוש היום"), color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.summary)
                        }
                    }
                } else {
                    item { SettingHeader(tr("הכי בשימוש היום"), theme) }
                    item {
                        SettingsCard(theme) {
                            summary.topApps.forEachIndexed { index, entry ->
                                val app = FUTURE_OS_APPS.find { it.packageName == entry.packageName }
                                SettingItem(
                                    app?.displayName ?: entry.packageName,
                                    formatDuration(entry.millis),
                                    app?.icon ?: FutureIcons.Apps,
                                    theme,
                                    showChevron = false,
                                    // שורת מידע בלי פעולה: לא נראית כלחיצה, אבל נשארת נגישה לגלילה במקשים.
                                    infoFocusable = true,
                                )
                                if (index < summary.topApps.lastIndex) SettingDivider(theme)
                            }
                        }
                    }
                }
                item { SettingHeader(tr("מגבלות"), theme) }
                item { itemsTail() }
            }
        }
    }
}

@Composable
fun DefaultAppsScreen(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val roleManager = remember { context.getSystemService(android.content.Context.ROLE_SERVICE) as? RoleManager }
    LaunchedEffect(Unit) { viewModel.loadDefaultApps() }
    val smsPackage = viewModel.defaultSmsPackage.value
    val dialerPackage = viewModel.defaultDialerPackage.value
    val homePackage = viewModel.defaultHomePackage.value

    // RoleManager.createRequestRoleIntent פותח דיאלוג מערכת אינטראקטיבי (אישור המשתמש
    // חובה - אי אפשר להעניק תפקיד ברירת מחדל מקוד בלי זה). מרעננים את המצב בחזרה
    // מהדיאלוג כדי שהמסך יראה מיד את הבחירה בפועל, לא רק מה שהיה לפני הבקשה.
    val roleRequestLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.loadDefaultApps()
    }

    fun requestRole(roleName: String) {
        // SET-39: כל מקרה שבו הבורר לא נפתח מקבל הודעה, ולא חוזר בשקט.
        val rm = roleManager
        if (rm == null || !rm.isRoleAvailable(roleName)) {
            android.widget.Toast.makeText(context, "המכשיר לא תומך בבחירה הזו", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        if (rm.isRoleHeld(roleName)) {
            android.widget.Toast.makeText(context, "ההגדרות כבר ברירת המחדל", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        try {
            roleRequestLauncher.launch(rm.createRequestRoleIntent(roleName))
        } catch (e: Exception) {
            android.util.Log.w("SettingsScreens", "requestRole failed", e)
            android.widget.Toast.makeText(context, "לא ניתן לפתוח את בחירת ברירת המחדל", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun openApp(pkg: String?, notInstalledMessage: String) {
        val launchIntent = pkg?.let { context.packageManager.getLaunchIntentForPackage(it) }
        if (launchIntent != null) {
            safeStartActivity(context, launchIntent, notInstalledMessage)
        } else {
            android.widget.Toast.makeText(context, notInstalledMessage, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    @Composable
    fun DefaultAppSection(title: String, icon: ImageVector, currentPackage: String?, ourPackage: String, ourLabel: String, roleName: String, notInstalledMessage: String) {
        SettingHeader(title, theme)
        SettingsCard(theme) {
            val app = FUTURE_OS_APPS.find { it.packageName == currentPackage }
            SettingItem(
                app?.displayName ?: currentPackage ?: tr("לא הוגדר"),
                if (currentPackage == ourPackage) tr("אפליקציית FutureOS") else tr("אפליקציה אחרת"),
                icon,
                theme,
                showChevron = false
            ) { openApp(ourPackage, notInstalledMessage) }
            if (currentPackage != ourPackage) {
                SettingDivider(theme)
                SettingItem("הפוך את $ourLabel לברירת מחדל", null, Icons.Rounded.CheckCircle, theme, showChevron = false) {
                    requestRole(roleName)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader(tr("ברירות מחדל"), theme) { navController.popBackStack() }
            LazyColumn {
                item {
                    Text(
                        text = tr("לחיצה על \"הפוך לברירת מחדל\" פותחת אישור מערכת - האישור עצמו חייב להינתן על ידך."),
                        color = theme.textColor.copy(alpha = 0.6f),
                        fontSize = FutureTypography.label,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                item {
                    DefaultAppSection(tr("הודעות"), FutureIcons.AutoMirrored.Chat, smsPackage, "com.future.messages", tr("הודעות"), RoleManager.ROLE_SMS, tr("הודעות לא מותקנת על המכשיר"))
                }
                item {
                    DefaultAppSection(tr("חיוג"), FutureIcons.Dialpad, dialerPackage, "com.future.dialer", tr("טלפון"), RoleManager.ROLE_DIALER, tr("טלפון לא מותקנת על המכשיר"))
                }
                item {
                    DefaultAppSection(tr("בית"), FutureIcons.Home, homePackage, "com.future.futurelauncher", "FutureLauncher", RoleManager.ROLE_HOME, tr("FutureLauncher לא מותקן על המכשיר"))
                }
            }
        }
    }
}

@Composable
fun PrivacyScreen(navController: NavController, theme: ThemeConfig) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader(tr("פרטיות"), theme) { navController.popBackStack() }
            LazyColumn {
                item { SettingHeader(tr("הרשאות לפי אפליקציה"), theme) }
                item {
                    SettingsCard(theme) {
                        FUTURE_OS_APPS.forEachIndexed { index, app ->
                            SettingItem(app.displayName, null, app.icon, theme) {
                                navController.navigate(Screen.AppPermissions.route(app.packageName))
                            }
                            if (index < FUTURE_OS_APPS.lastIndex) SettingDivider(theme)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppPermissionsScreen(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel, packageName: String) {
    val app = remember(packageName) { FUTURE_OS_APPS.find { it.packageName == packageName } }
    var permissions by remember(packageName) { mutableStateOf(viewModel.getSystemInteractor().getAppPermissions(packageName)) }
    // SET-36: ביטול הרשאה מבקש אישור - האפליקציה עלולה להפסיק לעבוד כראוי.
    var pendingRevoke by remember { mutableStateOf<com.future.settings.utils.SystemInteractor.PermissionEntry?>(null) }
    pendingRevoke?.let { perm ->
        ConfirmDialog(
            theme = theme,
            title = "ביטול הרשאה",
            message = "לבטל את ההרשאה \"${perm.shortLabel}\"? חלק מהפעולות באפליקציה עלולות להפסיק לעבוד.",
            confirmLabel = "בטל הרשאה",
            onConfirm = {
                pendingRevoke = null
                viewModel.revokePermission(packageName, perm.name) { updated -> permissions = updated }
            },
            onDismiss = { pendingRevoke = null }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader(app?.displayName ?: packageName, theme) { navController.popBackStack() }
            if (permissions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(tr("לא נמצאו הרשאות רגישות לאפליקציה זו"), color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.body)
                }
            } else {
                LazyColumn {
                    item {
                        SettingsCard(theme) {
                            permissions.forEachIndexed { index, perm ->
                                SettingItem(
                                    perm.shortLabel,
                                    if (perm.granted) tr("מאושרת") else tr("לא מאושרת"),
                                    if (perm.granted) Icons.Rounded.CheckCircle else FutureIcons.Block,
                                    theme,
                                    showChevron = false
                                ) {
                                    if (perm.granted) pendingRevoke = perm
                                }
                                if (index < permissions.lastIndex) SettingDivider(theme)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsFocusScreen(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader(tr("התראות ומיקוד"), theme) { navController.popBackStack() }
            LazyColumn {
                item {
                    SettingsCard(theme) {
                        SettingItem(tr("גישת מדיניות התראות"), tr("נדרש כדי לשלוט על מצב שקט/רטט"), FutureIcons.Notifications, theme) {
                            viewModel.requestNotificationPolicyAccess()
                        }
                        SettingDivider(theme)
                        SettingItem(tr("התראות מתקדמות"), tr("התראות קופצות, היסטוריה ונורית"), FutureIcons.Tune, theme) { navController.navigate(com.future.settings.utils.ExtraSystemSettings.NOTIFICATIONS.route) }
                    }
                }
                item { SettingHeader(tr("מיקוד עכשיו"), theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem(tr("שקט לשעה הקרובה"), tr("השתקת התראות עם חזרה אוטומטית"), FutureIcons.DoNotDisturbOn, theme, showChevron = false) {
                            viewModel.focusNow(60)
                        }
                        SettingDivider(theme)
                        SettingItem(tr("ביטול מיקוד"), null, FutureIcons.Notifications, theme, showChevron = false) {
                            viewModel.cancelFocusNow()
                        }
                    }
                }
                item { SettingHeader(tr("מצב שינה"), theme) }
                item {
                    SettingsCard(theme) {
                        SettingSwitch(tr("שקט אוטומטי (23:00–07:00)"), tr("השתקה אוטומטית בשעות השינה"), viewModel.sleepModeEnabled.value, { viewModel.toggleSleepMode() }, theme)
                    }
                }
            }
        }
    }
}

@Composable
fun AboutScreen(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val interactor = remember { viewModel.getSystemInteractor() }
    val uptime = remember { interactor.getUptimeString() }
    val ram = remember { interactor.getTotalRamString() }
    val kernel = remember { interactor.getKernelVersion() }
    var buildTaps by remember { mutableStateOf(0) }
    var modelTaps by remember { mutableStateOf(0) }
    var fiveCount by remember { mutableStateOf(0) }
    var lastFiveTime by remember { mutableStateOf(0L) }
    var zeroHeld by remember { mutableStateOf(false) }
    var fortune by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { viewModel.loadPhoneIdentity() }

    // "החזק 0 למסך פלקס נסתר" - 800ms החזקה. במקור זה היה על *, אבל * ו-# תפוסים
    // גלובלית ע"י FutureUI (פותחים ישר את Control Center/Notification Center לפני
    // שהאירוע בכלל מגיע לאפליקציה) - עברנו למקשי ספרות שלא שמורים לשום דבר אחר.
    LaunchedEffect(zeroHeld) {
        if (zeroHeld) {
            kotlinx.coroutines.delay(800)
            navController.navigate(Screen.AsciiFlex.route)
            zeroHeld = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .onKeyEvent { event ->
                val digit = digitForKey(event.key)
                when {
                    digit == 5 && event.type == KeyEventType.KeyDown -> {
                        val now = System.currentTimeMillis()
                        fiveCount = if (now - lastFiveTime > 1500) 1 else fiveCount + 1
                        lastFiveTime = now
                        if (fiveCount >= 5) {
                            fiveCount = 0
                            fortune = randomFortune()
                        }
                        true
                    }
                    digit == 0 -> {
                        when (event.type) {
                            KeyEventType.KeyDown -> { zeroHeld = true; true }
                            KeyEventType.KeyUp -> { zeroHeld = false; true }
                            else -> false
                        }
                    }
                    else -> false
                }
            }
    ) {
        Column {
            SmallHeader(tr("אודות הטלפון"), theme) { navController.popBackStack() }
            LazyColumn(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 16.dp)) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(FutureIcons.Smartphone, contentDescription = null, modifier = Modifier.size(80.dp), tint = theme.primaryColor)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Future Phone", fontSize = FutureTypography.display, fontWeight = FontWeight.Bold, color = theme.textColor)
                        Text("FutureOS", fontSize = FutureTypography.body, color = theme.textColor.copy(alpha = 0.6f))
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item {
                    SettingsCard(theme) {
                        // 7 הקשות על "דגם" - כמו הקשות על גרסת אנדרואיד באנדרואיד המקורי,
                        // חושף אנימציית לוגו FutureOS (ומשם, החזקת OK פותחת משחק חבוי).
                        SettingItem(tr("דגם"), "Future 1", null, theme, showChevron = false) {
                            modelTaps++
                            if (modelTaps >= 7) {
                                modelTaps = 0
                                navController.navigate(Screen.EasterEggReveal.route)
                            }
                        }
                        SettingDivider(theme)
                        // "הקש 7 פעמים כדי להפוך למפתח" - כמו באנדרואיד המקורי, לצורך אמיתי:
                        // חושף גישה למסך "אפשרויות למפתחים" האמיתי של המערכת.
                        SettingItem(tr("מספר בנייה"), Build.DISPLAY, null, theme, showChevron = false) {
                            if (viewModel.developerModeEnabled.value) {
                                viewModel.showToast(tr("אתה כבר מפתח"))
                            } else {
                                buildTaps++
                                when {
                                    buildTaps >= 7 -> viewModel.enableDeveloperMode()
                                    buildTaps >= 4 -> viewModel.showToast("עוד ${7 - buildTaps} לחיצות ותהיה מפתח")
                                }
                            }
                        }
                        SettingDivider(theme)
                        SettingItem(tr("גרסת קרנל"), kernel, null, theme, showChevron = false, infoFocusable = true)
                    }
                }
                item { SettingHeader(tr("סטטוס"), theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem(tr("זמן פעולה"), uptime, FutureIcons.Schedule, theme, showChevron = false, infoFocusable = true)
                        SettingDivider(theme)
                        SettingItem(tr("זיכרון RAM"), ram, FutureIcons.Memory, theme, showChevron = false, infoFocusable = true)
                    }
                }
                item { SettingHeader(tr("זיהוי המכשיר"), theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem("IMEI", viewModel.imei.value ?: tr("לא זמין במכשיר זה"), FutureIcons.Pin, theme, showChevron = false, infoFocusable = true)
                        SettingDivider(theme)
                        SettingItem(tr("מספר טלפון"), viewModel.phoneNumber.value ?: tr("לא זמין"), FutureIcons.Call, theme, showChevron = false, infoFocusable = true)
                        SettingDivider(theme)
                        SettingItem(tr("יצרן וחומרה"), "${Build.MANUFACTURER} ${Build.MODEL} · ${Build.HARDWARE}", FutureIcons.Gavel, theme, showChevron = false, infoFocusable = true)
                    }
                }
                if (viewModel.developerModeEnabled.value) {
                    item { SettingHeader(tr("מפתחים"), theme) }
                    item {
                        SettingsCard(theme) {
                            SettingItem(tr("אפשרויות למפתחים"), tr("USB, ניפוי באגים, אנימציות ורינדור"), FutureIcons.Code, theme) {
                                navController.navigate(com.future.settings.utils.ExtraSystemSettings.DEVELOPER.route)
                            }
                        }
                    }
                }
            }
        }
        fortune?.let { text -> FortuneToast(text) { fortune = null } }
    }
}

/** כפתור מצב שמע (צליל/רטט/השתק). קודם היו כאן שתי מטרות פוקוס: העמודה
 * עם clickable (מקבלת פוקוס בלי שום סימון) והעיגול עם focusable (מסומן אבל
 * OK עליו לא עושה כלום) - הפוקוס נחת פעם על זה ופעם על זה. עכשיו מטרה אחת:
 * העמודה כולה ממוקדת ולוחצת, והעיגול רק מצייר את הטבעת. */
@Composable
fun SoundModeItem(label: String, icon: ImageVector, selected: Boolean, theme: ThemeConfig, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .focusMotion(interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .bringIntoViewOnFocus()
            .padding(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                // נבחר = ההדגשה המתוקנת (ההדגשה הגולמית הלבנה נעלמת על משטח לבן במצב בהיר);
                // טבעת הפוקוס בצבע הטקסט כשהעיגול כבר מלא בהדגשה, אחרת בהדגשה.
                .background(
                    animateFocusColor(
                        isFocused,
                        if (selected) theme.futureTheme.readableAccentColor else theme.futureTheme.readableAccentColor.copy(alpha = 0.14f),
                        if (selected) theme.futureTheme.readableAccentColor else theme.textColor.copy(alpha = 0.06f),
                    ).value
                )
                .border(
                    FutureDimens.focusBorderControl,
                    animateFocusColor(isFocused, if (selected) theme.textColor else theme.futureTheme.readableAccentColor, Color.Transparent).value,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) theme.futureTheme.onReadableAccentColor else theme.textColor)
        }
        Text(
            label,
            fontSize = FutureTypography.label,
            color = animateFocusColor(isFocused, theme.futureTheme.readableAccentColor, theme.textColor).value,
            fontWeight = if (isFocused || selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** מחוון בנוי-בעצמנו במקום Slider של Material3 - ל-Slider יש נקודות סימון
 * שלב מובנות שאי אפשר לכבות וללא אינדיקציית פוקוס אמיתית.
 *
 * המחוון הוא שורה בכרטיס בדיוק כמו SettingItem/SettingSwitch: שקוף על
 * הכרטיס, כותרת באותו גודל ומשקל, קו מפריד בינו לבין השורה הבאה, והפוקוס
 * הוא אותו מילוי ומסגרת שלוקחים את פינות הכרטיס (cardRowFocus). קודם היה
 * לכל מחוון מילוי אפור ופינות משלו, כך שבתוך הכרטיס הוא נראה כמו גלולה
 * נפרדת שצפה בו ולא כמו עוד פריט ברשימה. המסילה עצמה לא השתנתה (Slider.jsx):
 * 12dp, 15% מצבע הטקסט, מילוי בהדגשה שגדל מימין, 5% לכל לחיצת חץ. */
@Composable
fun VolumeSlider(label: String, value: Float, theme: ThemeConfig, summary: String? = null, onValueChange: (Float) -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val inCard = LocalFutureCard.current != null
    val bgColor = animateColorAsState(
        if (isFocused) theme.textColor.copy(alpha = 0.06f) else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "volumeSliderBg"
    )
    val borderColor = animateColorAsState(
        if (isFocused) theme.futureTheme.readableAccentColor else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "volumeSliderBorder"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    // הממשק RTL והמילוי גדל מימין (לפי ה-DS) - שמאלה זה "קדימה".
                    Key.DirectionLeft -> { onValueChange((value + 0.05f).coerceIn(0f, 1f)); true }
                    Key.DirectionRight -> { onValueChange((value - 0.05f).coerceIn(0f, 1f)); true }
                    else -> false
                }
            }
            .focusable().bringIntoViewOnFocus()
            .then(if (inCard) Modifier else Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
            .cardRowFocus({ bgColor.value }, { borderColor.value }, fallbackShape = FutureShapes.lg)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(label, fontSize = theme.titleFontSize, color = theme.textColor, fontWeight = FontWeight.SemiBold)
        if (summary != null) {
            Text(summary, fontSize = theme.summaryFontSize, color = theme.textColor.copy(alpha = 0.6f))
        }
        Spacer(modifier = Modifier.height(10.dp))
        val fill by androidx.compose.animation.core.animateFloatAsState(
            value.coerceIn(0f, 1f), FutureMotion.fast(), label = "volumeSliderFill"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(theme.textColor.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fill)
                    .clip(RoundedCornerShape(6.dp))
                    .background(theme.primaryColor)
            )
        }
    }
}

@Composable
fun SimManagerScreen(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel) {
    LaunchedEffect(Unit) { viewModel.loadSimCards() }
    val sims = viewModel.simCards.value
    var renamingSubId by remember { mutableStateOf<Int?>(null) }
    var renameText by remember { mutableStateOf("") }

    val simColors = FutureAccents.presets.drop(1) + theme.dangerColor

    if (renamingSubId != null) {
        InputDialog(
            title = tr("שם תצוגה לקו"),
            theme = theme.futureTheme,
            initialValue = renameText,
            onDismiss = { renamingSubId = null },
            onConfirm = { name ->
                val subId = renamingSubId
                if (subId != null) viewModel.renameSim(subId, name)
                renamingSubId = null
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader(tr("ניהול כרטיסי SIM"), theme) { navController.popBackStack() }
            if (sims.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(tr("לא נמצאו כרטיסי SIM פעילים"), color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.body)
                }
            } else {
                LazyColumn {
                    items(sims, key = { it.subscriptionId }) { sim ->
                        SettingHeader(sim.displayName, theme)
                        SettingsCard(theme) {
                            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(sim.colorArgb.let { if (it == 0) simColors[0].toArgb() else it })))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sim.carrierName, color = theme.textColor, fontWeight = FontWeight.SemiBold)
                                    Text(sim.phoneNumber ?: tr("מספר לא זמין"), color = theme.textColor.copy(alpha = 0.6f), fontSize = FutureTypography.label)
                                }
                            }
                            SettingDivider(theme)
                            SettingItem(tr("שנה שם תצוגה"), null, FutureIcons.Edit, theme) {
                                renamingSubId = sim.subscriptionId
                                renameText = sim.displayName
                            }
                            SettingDivider(theme)
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                simColors.forEach { color ->
                                    var isFocused by remember { mutableStateOf(false) }
                                    val selected = sim.colorArgb == color.toArgb()
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .focusScale(isFocused, focusedScale = 1.2f)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                2.dp,
                                                animateFocusColor(isFocused && !selected, theme.primaryColor, if (selected) theme.textColor else Color.Transparent).value,
                                                CircleShape,
                                            )
                                            .onFocusChanged { isFocused = it.isFocused }
                                            .focusable().bringIntoViewOnFocus()
                                            .clickable { viewModel.recolorSim(sim.subscriptionId, color.toArgb()) }
                                    )
                                }
                            }
                            SettingDivider(theme)
                            SettingSwitch(tr("ברירת מחדל לנתונים"), null, viewModel.defaultDataSubId.value == sim.subscriptionId, { if (it) viewModel.setDefaultDataSim(sim.subscriptionId) }, theme)
                            SettingDivider(theme)
                            SettingSwitch(tr("ברירת מחדל לשיחות"), null, viewModel.defaultVoiceSubId.value == sim.subscriptionId, { if (it) viewModel.setDefaultVoiceSim(sim.subscriptionId) }, theme)
                            SettingDivider(theme)
                            SettingSwitch(tr("ברירת מחדל ל-SMS"), null, viewModel.defaultSmsSubId.value == sim.subscriptionId, { if (it) viewModel.setDefaultSmsSim(sim.subscriptionId) }, theme)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppTimersScreen(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel) {
    LaunchedEffect(Unit) { viewModel.loadAppTimers() }
    val timers = viewModel.appTimers.value
    var pickingApp by remember { mutableStateOf<FutureApp?>(null) }
    var minutesText by remember { mutableStateOf("60") }

    val installedApps = remember {
        val interactor = viewModel.getSystemInteractor()
        FUTURE_OS_APPS.filter { interactor.isAppInstalled(it.packageName) }
    }

    if (pickingApp != null) {
        InputDialog(
            title = tr("מכסה יומית ל-{0}", pickingApp?.displayName),
            theme = theme.futureTheme,
            initialValue = minutesText,
            placeholder = tr("דקות ליום"),
            confirmLabel = tr("הגדר"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            onDismiss = { pickingApp = null },
            onConfirm = { value ->
                val minutes = value.filter { it.isDigit() }.toIntOrNull()
                val pkg = pickingApp?.packageName
                if (minutes != null && minutes > 0 && pkg != null) viewModel.setAppTimer(pkg, minutes)
                pickingApp = null
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader(tr("טיימרים לאפליקציות"), theme) { navController.popBackStack() }
            LazyColumn {
                if (timers.isNotEmpty()) {
                    item { SettingHeader(tr("מכסות פעילות"), theme) }
                    item {
                        SettingsCard(theme) {
                            timers.forEachIndexed { index, timer ->
                                val app = FUTURE_OS_APPS.find { it.packageName == timer.packageName }
                                SettingItem(
                                    app?.displayName ?: timer.packageName,
                                    tr("{0} דקות ליום - לחצו OK כדי להסיר", timer.dailyLimitMinutes),
                                    app?.icon ?: FutureIcons.Apps,
                                    theme,
                                    showChevron = false
                                ) { viewModel.removeAppTimer(timer.packageName) }
                                if (index < timers.lastIndex) SettingDivider(theme)
                            }
                        }
                    }
                }
                item { SettingHeader(tr("הוספת מכסה"), theme) }
                item {
                    SettingsCard(theme) {
                        val available = installedApps.filter { app -> timers.none { it.packageName == app.packageName } }
                        if (available.isEmpty()) {
                            SettingItem(tr("לכל האפליקציות המותקנות כבר יש מכסה"), null, null, theme, showChevron = false)
                        } else {
                            available.forEachIndexed { index, app ->
                                SettingItem(app.displayName, null, app.icon, theme) {
                                    pickingApp = app
                                    minutesText = "60"
                                }
                                if (index < available.lastIndex) SettingDivider(theme)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticsScreen(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val sensorManager = remember { viewModel.getSystemInteractor().getSensorManager() }
    var accelText by remember { mutableStateOf(tr("ממתין לנתונים")) }
    var lightText by remember { mutableStateOf(tr("ממתין לנתונים")) }
    var proximityText by remember { mutableStateOf(tr("ממתין לנתונים")) }

    DisposableEffect(Unit) {
        val accel = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
        val light = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_LIGHT)
        val proximity = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_PROXIMITY)
        val listener = object : android.hardware.SensorEventListener {
            override fun onSensorChanged(event: android.hardware.SensorEvent) {
                when (event.sensor.type) {
                    android.hardware.Sensor.TYPE_ACCELEROMETER ->
                        accelText = "X: %.2f  Y: %.2f  Z: %.2f".format(event.values[0], event.values[1], event.values[2])
                    android.hardware.Sensor.TYPE_LIGHT -> lightText = tr("%.0f לוקס").format(event.values[0])
                    android.hardware.Sensor.TYPE_PROXIMITY -> proximityText = tr("%.1f ס\"מ").format(event.values[0])
                }
            }
            override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
        }
        accel?.let { sensorManager.registerListener(listener, it, android.hardware.SensorManager.SENSOR_DELAY_UI) }
        light?.let { sensorManager.registerListener(listener, it, android.hardware.SensorManager.SENSOR_DELAY_UI) }
        proximity?.let { sensorManager.registerListener(listener, it, android.hardware.SensorManager.SENSOR_DELAY_UI) }
        if (accel == null) accelText = tr("לא זמין במכשיר זה")
        if (light == null) lightText = tr("לא זמין במכשיר זה")
        if (proximity == null) proximityText = tr("לא זמין במכשיר זה")
        onDispose { sensorManager.unregisterListener(listener) }
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader(tr("אבחון מכשיר"), theme) { navController.popBackStack() }
            LazyColumn {
                item { SettingHeader(tr("רמקול ורטט"), theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem(tr("השמע צליל בדיקה"), null, FutureIcons.AutoMirrored.VolumeUp, theme, showChevron = false) {
                            viewModel.testSpeakerTone()
                        }
                        SettingDivider(theme)
                        SettingItem(tr("בדוק רטט"), null, FutureIcons.Vibration, theme, showChevron = false) {
                            viewModel.testVibration()
                        }
                    }
                }
                item { SettingHeader(tr("חיישנים חיים"), theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem(tr("תאוצה (Accelerometer)"), accelText, FutureIcons.Speed, theme, showChevron = false, infoFocusable = true)
                        SettingDivider(theme)
                        SettingItem(tr("חיישן אור"), lightText, FutureIcons.Brightness6, theme, showChevron = false, infoFocusable = true)
                        SettingDivider(theme)
                        SettingItem(tr("חיישן קרבה"), proximityText, FutureIcons.Sensors, theme, showChevron = false, infoFocusable = true)
                    }
                }
                // "מסך מגע" (בדיקת גרירה חיה) הוסרה - dispatchTouchEvent ב-MainActivity
                // מבטל קלט מגע לחלוטין ברמת המערכת, כך שהבדיקה הזו הייתה מתה מלידה:
                // שום MotionEvent לא היה מגיע אליה אף פעם על המכשיר האמיתי.
            }
        }
    }
}

@Composable
fun SosScreen(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel) {
    LaunchedEffect(Unit) { viewModel.loadEmergencyContact() }
    val contact = viewModel.emergencyContact.value
    var showEditDialog by remember { mutableStateOf(false) }
    var showSosConfirm by remember { mutableStateOf(false) }
    var nameField by rememberSaveable { mutableStateOf(contact?.first ?: "") }
    var phoneField by rememberSaveable { mutableStateOf(contact?.second ?: "") }
    // SET-36: הסרת איש הקשר לחירום מבקשת אישור.
    var showRemoveContactConfirm by remember { mutableStateOf(false) }
    if (showRemoveContactConfirm) {
        ConfirmDialog(
            theme = theme,
            title = "הסרת איש קשר לחירום",
            message = "אחרי ההסרה הודעת SOS לא תישלח לאף אחד. להסיר?",
            confirmLabel = "הסר",
            onConfirm = { showRemoveContactConfirm = false; viewModel.clearEmergencyContact() },
            onDismiss = { showRemoveContactConfirm = false }
        )
    }

    if (showEditDialog) {
        FutureDialog(
            theme = theme.futureTheme,
            onDismissRequest = { showEditDialog = false },
            title = tr("איש קשר לחירום"),
            buttons = {
                FutureButton(tr("ביטול"), theme.futureTheme, { showEditDialog = false }, variant = FutureButtonVariant.Secondary)
                FutureButton(tr("שמור"), theme.futureTheme, {
                    if (nameField.isNotBlank() && phoneField.isNotBlank()) {
                        viewModel.saveEmergencyContact(nameField.trim(), phoneField.trim())
                        showEditDialog = false
                    }
                })
            },
        ) {
            Column(
                modifier = Modifier.escapeTextFieldFocusTrap(),
                verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
            ) {
                FutureTextField(
                    value = nameField,
                    onValueChange = { nameField = it },
                    theme = theme.futureTheme,
                    placeholder = tr("שם"),
                    autoFocus = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FutureTextField(
                    value = phoneField,
                    onValueChange = { phoneField = it },
                    theme = theme.futureTheme,
                    placeholder = tr("מספר טלפון"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader(tr("בטיחות וחירום"), theme) { navController.popBackStack() }
            LazyColumn {
                item { SettingHeader(tr("איש קשר לחירום"), theme) }
                item {
                    SettingsCard(theme) {
                        if (contact != null) {
                            SettingItem(contact.first, contact.second, FutureIcons.ContactPhone, theme) {
                                nameField = contact.first
                                phoneField = contact.second
                                showEditDialog = true
                            }
                            SettingDivider(theme)
                            SettingItem(tr("הסר איש קשר"), null, Icons.Rounded.PersonRemove, theme, showChevron = false) { showRemoveContactConfirm = true }
                        } else {
                            SettingItem(tr("הוסף איש קשר לחירום"), tr("יקבל SMS עם מיקום בעת שליחת SOS"), FutureIcons.PersonAdd, theme) {
                                nameField = ""
                                phoneField = ""
                                showEditDialog = true
                            }
                        }
                    }
                }
                item { SettingHeader(tr("שליחה"), theme) }
                item {
                    SettingsCard(theme) {
                        val sending = viewModel.sosSending.value
                        SettingItem(
                            if (sending) tr("שולח") else tr("שלח הודעת SOS עכשיו"),
                            tr("שולח מיקום נוכחי ל-SMS לאיש הקשר"),
                            FutureIcons.Sos,
                            theme,
                            showChevron = false
                        ) {
                            if (!sending) showSosConfirm = true
                        }
                        if (showSosConfirm) {
                            ConfirmDialog(
                                theme = theme,
                                title = "שליחת הודעת SOS",
                                message = "תישלח הודעת SMS עם המיקום האחרון לאיש הקשר לחירום. להמשיך?",
                                confirmLabel = "שלח",
                                onConfirm = { showSosConfirm = false; viewModel.sendSosNow() },
                                onDismiss = { showSosConfirm = false }
                            )
                        }
                    }
                }
                item {
                    Text(
                        text = tr("הפעלה כאן היא ידנית מתוך המסך - טריגר פיזי (למשל 3 לחיצות על כפתור הצד) דורש הרשאת נגישות ברמת FutureUI ולא קיים כרגע."),
                        color = theme.textColor.copy(alpha = 0.5f),
                        fontSize = FutureTypography.label,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}
