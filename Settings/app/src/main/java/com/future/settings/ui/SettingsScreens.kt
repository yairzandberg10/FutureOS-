package com.future.settings.ui

import android.Manifest
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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
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
    object Ringtone : Screen("ringtone")
    object NotificationSound : Screen("notification_sound")
    object AlarmSound : Screen("alarm_sound")
    object Language : Screen("language")
    object Security : Screen("security")
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
    FutureApp("com.future.futureui", "FutureUI (System UI)", Icons.Rounded.Widgets),
    FutureApp("com.future.futurelauncher", "FutureLauncher", Icons.Rounded.Home),
    FutureApp("com.future.dialer", "טלפון", Icons.Rounded.Dialpad),
    FutureApp("com.future.messages", "הודעות", Icons.AutoMirrored.Rounded.Message),
    FutureApp("com.future.contact", "אנשי קשר", Icons.Rounded.Contacts),
    FutureApp("com.future.files", "קבצים", Icons.Rounded.Folder),
    FutureApp("com.future.gallery", "גלריה", Icons.Rounded.Image),
    FutureApp("com.future.terminal", "טרמינל", Icons.Rounded.Terminal),
    FutureApp("com.future.tools", "כלים", Icons.Rounded.Handyman),
    FutureApp("com.future.music", "מוזיקה", Icons.Rounded.MusicNote),
    FutureApp("com.future.notes", "פתקים", Icons.AutoMirrored.Rounded.Notes),
    FutureApp("com.future.gotitdone", "gotitdone", Icons.Rounded.CheckCircle),
    FutureApp("com.future.mikdash", "מקדש", Icons.Rounded.Apps),
    FutureApp("com.future.sfarim", "ספרים", Icons.AutoMirrored.Rounded.MenuBook)
)

/** חבילת האפליקציה של FutureUI (מרכז בקרה/מסך נעילה/שורת מצב) - להתאמה אישית שלהם. */
private const val FUTURE_UI_PACKAGE = "com.future.futureui"
private const val FUTURE_UI_SETTINGS_ACTIVITY = "com.future.futureui.SettingsActivity"

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
            context.startActivity(Intent.createChooser(sendIntent, "שתף קובץ").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e2: Exception) {
            android.widget.Toast.makeText(context, "לא ניתן לשתף את הקובץ", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun SettingsApp(viewModel: SettingsViewModel) {
    val navController = rememberNavController()
    val theme by viewModel.themeConfig

    // מעברים ברירת מחדל של Navigation Compose לוקחים כ-300ms; במסך שמנווטים בו
    // הרבה עם מקלדת פיזית (בלי מגע) זה מצטבר ומרגיש איטי - קיצור ל-120ms הופך
    // את הניווט בין תפריטים למיידי בהרבה בלי לוותר על אנימציה לגמרי.
    val fastFade = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(120))
    val fastFadeOut = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(100))

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        NavHost(
            navController = navController,
            startDestination = Screen.Main.route,
            enterTransition = { fastFade },
            exitTransition = { fastFadeOut },
            popEnterTransition = { fastFade },
            popExitTransition = { fastFadeOut }
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
            composable(Screen.Ringtone.route) {
                SoundPickerScreen(navController, theme, "רינגטון", RingtoneManager.TYPE_RINGTONE, viewModel)
            }
            composable(Screen.NotificationSound.route) {
                SoundPickerScreen(navController, theme, "צליל התראות", RingtoneManager.TYPE_NOTIFICATION, viewModel)
            }
            composable(Screen.AlarmSound.route) {
                SoundPickerScreen(navController, theme, "צליל אזעקה", RingtoneManager.TYPE_ALARM, viewModel)
            }
            composable(Screen.Language.route) { LanguageScreen(navController, theme, viewModel) }
            composable(Screen.Security.route) { SecurityScreen(navController, theme) }
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
        }
    }
}

/** קטלוג שטוח של כל הקטגוריות הראשיות ל"חיפוש בהגדרות" - מקור נתונים נפרד מהכרטיסים
 *  עצמם (לא נגזר מהם אוטומטית) כי לכל פריט יש גם מילות מפתח בעברית/אנגלית לחיפוש,
 *  לא רק את שם הכותרת המוצג. */
private data class SearchableSetting(val title: String, val keywords: String, val icon: ImageVector, val route: String)

private val SEARCHABLE_SETTINGS = listOf(
    SearchableSetting("חיבורים", "bluetooth בלוטות' מצב טיסה נתונים סלולריים sim מיקום location airplane", Icons.Rounded.SettingsInputAntenna, Screen.Connections.route),
    SearchableSetting("צלילים", "עוצמת שמע צליל רינגטון volume sound ringtone", Icons.AutoMirrored.Rounded.VolumeUp, Screen.Sound.route),
    SearchableSetting("תצוגה", "בהירות מסך עיצוב brightness display", Icons.Rounded.Brightness6, Screen.Display.route),
    SearchableSetting("שורת מצב, מסך נעילה ורקע", "סטטוס בר שעון lockscreen status bar clock טפט wallpaper", Icons.Rounded.Widgets, Screen.SystemUi.route),
    SearchableSetting("סוללה", "battery חיסכון טעינה אחוז", Icons.Rounded.BatteryFull, Screen.Battery.route),
    SearchableSetting("אחסון", "storage זיכרון פנוי מקום", Icons.Rounded.Storage, Screen.Storage.route),
    SearchableSetting("ביצועים ותחזוקה", "performance מעבד ram ניקוי אופטימיזציה", Icons.Rounded.Speed, Screen.Performance.route),
    SearchableSetting("התראות ומיקוד", "notifications focus שקט מיקוד שינה", Icons.Rounded.NotificationsActive, Screen.NotificationsFocus.route),
    SearchableSetting("זמן מסך", "screen time שימוש טיימר אפליקציות", Icons.Rounded.HourglassBottom, Screen.ScreenTime.route),
    SearchableSetting("כללי", "general תאריך שעה שפה נגישות accessibility date time language", Icons.Rounded.Language, Screen.General.route),
    SearchableSetting("אפליקציות", "apps אפליקציה", Icons.Rounded.Apps, Screen.Apps.route),
    SearchableSetting("אודות הטלפון", "about imei גרסה מספר טלפון version", Icons.Rounded.Info, Screen.About.route)
)

@Composable
fun MainMenu(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel) {
    var konamiBuffer by remember { mutableStateOf(listOf<Int>()) }
    var confettiTrigger by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item { LargeHeader("הגדרות", theme) }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    placeholder = { Text("חיפוש בהגדרות") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = theme.textColor.copy(alpha = 0.6f)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "נקה חיפוש", tint = theme.textColor.copy(alpha = 0.6f))
                            }
                        }
                    },
                    shape = RoundedCornerShape(theme.borderRadius),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = theme.textColor,
                        unfocusedTextColor = theme.textColor,
                        focusedBorderColor = theme.primaryColor,
                        unfocusedBorderColor = theme.textColor.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        // בשדה טקסט, Compose "בולע" את מקש למטה פנימית ולא מזיז פוקוס -
                        // מכשיר עם מקלדת בלבד (בלי מגע) היה נשאר תקוע בשדה החיפוש בלי
                        // דרך לרדת לרשימת התוצאות. יורטים את המקש כאן ומזיזים פוקוס ידנית.
                        .onPreviewKeyEvent {
                            if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionDown) {
                                focusManager.moveFocus(FocusDirection.Down)
                                true
                            } else false
                        }
                )
            }

            if (searchQuery.isBlank()) {
                item {
                    SettingsCard(theme) {
                        SettingItem("חיבורים", "Bluetooth, מצב טיסה, נתונים סלולריים, מיקום", Icons.Rounded.SettingsInputAntenna, theme) { navController.navigate(Screen.Connections.route) }
                    }
                }

                item {
                    SettingsCard(theme) {
                        SettingItem("צלילים", "עוצמת שמע, רינגטון, רטט", Icons.AutoMirrored.Rounded.VolumeUp, theme) { navController.navigate(Screen.Sound.route) }
                        SettingDivider(theme)
                        SettingItem("תצוגה", "בהירות, מצב כהה, גודל טקסט", Icons.Rounded.Brightness6, theme) { navController.navigate(Screen.Display.route) }
                    }
                }

                item {
                    SettingsCard(theme) {
                        SettingItem("שורת מצב, מסך נעילה ורקע", "סוללה, Bluetooth, שעון, טפט", Icons.Rounded.Widgets, theme) { navController.navigate(Screen.SystemUi.route) }
                    }
                }

                item {
                    SettingsCard(theme) {
                        SettingItem("סוללה", "חיסכון בחשמל, אחוז טעינה", Icons.Rounded.BatteryFull, theme) { navController.navigate(Screen.Battery.route) }
                        SettingDivider(theme)
                        SettingItem("אחסון", "מקום פנוי בזיכרון", Icons.Rounded.Storage, theme) { navController.navigate(Screen.Storage.route) }
                        SettingDivider(theme)
                        SettingItem("ביצועים ותחזוקה", "זיכרון, מעבד, אופטימיזציה", Icons.Rounded.Speed, theme) { navController.navigate(Screen.Performance.route) }
                    }
                }

                item {
                    SettingsCard(theme) {
                        SettingItem("התראות ומיקוד", "מצב מיקוד, שעות שינה", Icons.Rounded.NotificationsActive, theme) { navController.navigate(Screen.NotificationsFocus.route) }
                        SettingDivider(theme)
                        SettingItem("זמן מסך", "שימוש יומי באפליקציות", Icons.Rounded.HourglassBottom, theme) { navController.navigate(Screen.ScreenTime.route) }
                    }
                }

                item {
                    SettingsCard(theme) {
                        SettingItem("כללי", "תאריך, שעה, שפה, נגישות", Icons.Rounded.Language, theme) { navController.navigate(Screen.General.route) }
                        SettingDivider(theme)
                        SettingItem("אפליקציות", "כל אפליקציות FutureOS", Icons.Rounded.Apps, theme) { navController.navigate(Screen.Apps.route) }
                        SettingDivider(theme)
                        SettingItem("אודות הטלפון", "גרסה, דגם, סטטוס", Icons.Rounded.Info, theme) { navController.navigate(Screen.About.route) }
                    }
                }
            } else {
                val query = searchQuery.trim()
                val matches = SEARCHABLE_SETTINGS.filter { it.title.contains(query, ignoreCase = true) || it.keywords.contains(query, ignoreCase = true) }
                if (matches.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("לא נמצאו הגדרות תואמות", color = theme.textColor.copy(alpha = 0.5f), fontSize = 14.sp)
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
                        SettingItem("🎉 מצב חגיגה", "עוד ${((viewModel.partyModeExpiresAt.value - now) / 60000L).coerceAtLeast(0L)} דקות", Icons.Rounded.Celebration, theme) { navController.navigate(Screen.PartyMode.route) }
                    }
                }
            }
        }
        ConfettiBurst(trigger = confettiTrigger, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun SmallHeader(title: String, theme: ThemeConfig, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "חזור", tint = theme.textColor)
        }
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = theme.textColor)
    }
}

@Composable
fun ConnectionsScreen(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel) {
    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader("חיבורים", theme) { navController.popBackStack() }
            LazyColumn {
                item {
                    SettingsCard(theme) {
                        SettingItem("Bluetooth", if (viewModel.bluetoothEnabled.value) "מופעל" else "כבוי", Icons.Rounded.Bluetooth, theme) { navController.navigate(Screen.Bluetooth.route) }
                        SettingDivider(theme)
                        SettingSwitch("מצב טיסה", null, viewModel.airplaneMode.value, { viewModel.toggleAirplaneMode() }, theme)
                    }
                }
                item { SettingHeader("רשת סלולרית", theme) }
                item {
                    SettingsCard(theme) {
                        SettingSwitch("נתונים סלולריים", null, viewModel.mobileData.value, { viewModel.toggleMobileData() }, theme)
                        SettingDivider(theme)
                        SettingSwitch("נדידה (רומינג)", "נתונים סלולריים גם מחוץ לארץ", viewModel.dataRoaming.value, { viewModel.toggleDataRoaming() }, theme)
                        SettingDivider(theme)
                        SettingItem("ניהול כרטיסי SIM", "שם, צבע וברירת מחדל לכל קו", Icons.Rounded.SimCard, theme) { navController.navigate(Screen.SimManager.route) }
                    }
                }
                item { SettingHeader("מיקום", theme) }
                item {
                    SettingsCard(theme) {
                        SettingSwitch("שירותי מיקום", null, viewModel.locationEnabled.value, { viewModel.toggleLocationEnabled() }, theme)
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
            if (!isScanning) android.widget.Toast.makeText(context, "לא ניתן להפעיל סריקת Bluetooth", android.widget.Toast.LENGTH_SHORT).show()
        } else if (!results.values.all { it }) {
            pendingScan = false
            android.widget.Toast.makeText(context, "צריך הרשאת Bluetooth כדי לסרוק מכשירים", android.widget.Toast.LENGTH_SHORT).show()
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
        if (!isScanning) android.widget.Toast.makeText(context, "לא ניתן להפעיל סריקת Bluetooth", android.widget.Toast.LENGTH_SHORT).show()
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
        AlertDialog(
            onDismissRequest = { isRenaming = false },
            containerColor = theme.surfaceColor,
            titleContentColor = theme.textColor,
            title = { Text("שם המכשיר", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = deviceNameText,
                    onValueChange = { deviceNameText = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = theme.textColor,
                        unfocusedTextColor = theme.textColor,
                        focusedBorderColor = theme.primaryColor
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (deviceNameText.isNotBlank()) viewModel.renameBluetoothDevice(deviceNameText.trim())
                    isRenaming = false
                }) { Text("שמור", color = theme.primaryColor, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { isRenaming = false }) { Text("ביטול", color = theme.textColor.copy(alpha = 0.6f)) }
            }
        )
    }

    forgetTarget?.let { device ->
        ConfirmDialog(
            theme = theme,
            title = "שכחת מכשיר",
            message = "\"${device.name}\" יוסר מרשימת המכשירים המשויכים. תצטרך להתאים אותו מחדש כדי להתחבר שוב.",
            confirmLabel = "שכח מכשיר",
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
                        SettingSwitch("Bluetooth", if (viewModel.bluetoothEnabled.value) "מופעל" else "כבוי", viewModel.bluetoothEnabled.value, {
                            val wasEnabled = viewModel.bluetoothEnabled.value
                            viewModel.toggleBluetooth()
                            if (wasEnabled) { discovered.clear(); isScanning = false }
                        }, theme)
                    }
                }

                if (viewModel.bluetoothEnabled.value) {
                    item {
                        SettingsCard(theme) {
                            SettingItem("שם המכשיר שלי", viewModel.bluetoothDeviceName.value, Icons.Rounded.Edit, theme) {
                                deviceNameText = viewModel.bluetoothDeviceName.value
                                isRenaming = true
                            }
                        }
                    }

                    item { SettingHeader("מכשירים משויכים", theme) }
                    item {
                        val paired = viewModel.pairedBluetoothDevices.value
                        SettingsCard(theme) {
                            if (paired.isEmpty()) {
                                SettingItem("אין עדיין מכשירים משויכים", null, null, theme, showChevron = false) { }
                            } else {
                                paired.forEachIndexed { index, device ->
                                    SettingItem(
                                        device.name,
                                        (if (device.isConnected) "מחובר כעת" else "משויך") + " - הקש כדי לשכוח",
                                        if (device.isConnected) Icons.Rounded.BluetoothConnected else Icons.Rounded.Bluetooth,
                                        theme,
                                        showChevron = false
                                    ) { forgetTarget = device }
                                    if (index < paired.lastIndex) SettingDivider(theme)
                                }
                            }
                        }
                    }

                    item { SettingHeader("מכשירים זמינים", theme) }
                    item {
                        SettingsCard(theme) {
                            SettingItem(
                                if (isScanning) "סורק מכשירים בסביבה..." else "סרוק מכשירים חדשים",
                                if (discovered.isEmpty()) null else "${discovered.size} נמצאו",
                                Icons.Rounded.Search,
                                theme,
                                showChevron = false
                            ) {
                                if (!isScanning) requestBluetoothPermissionsThenScan()
                            }
                            if (discovered.isNotEmpty()) {
                                SettingDivider(theme)
                                discovered.forEachIndexed { index, device ->
                                    val name = try { device.name ?: device.address } catch (e: SecurityException) { device.address }
                                    SettingItem(name, "הקש כדי להתאים", Icons.Rounded.BluetoothSearching, theme, showChevron = false) {
                                        val started = interactor.pairBluetoothDevice(device)
                                        if (!started) android.widget.Toast.makeText(context, "לא ניתן היה להתחיל התאמה עם $name", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    if (index < discovered.lastIndex) SettingDivider(theme)
                                }
                            }
                        }
                    }

                    item { SettingHeader("שיתוף קבצים", theme) }
                    item {
                        SettingsCard(theme) {
                            SettingItem("שלח קובץ דרך Bluetooth", "בחירת קובץ מהמכשיר לשליחה", Icons.Rounded.Send, theme) {
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
            SmallHeader("צלילים", theme) { navController.popBackStack() }
            LazyColumn {
                item {
                    SettingsCard(theme) {
                        val currentMode = viewModel.ringerMode.value
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            SoundModeItem("צליל", Icons.AutoMirrored.Rounded.VolumeUp, currentMode == AudioManager.RINGER_MODE_NORMAL, theme) { viewModel.setRingerMode(AudioManager.RINGER_MODE_NORMAL) }
                            SoundModeItem("רטט", Icons.Rounded.Vibration, currentMode == AudioManager.RINGER_MODE_VIBRATE, theme) { viewModel.setRingerMode(AudioManager.RINGER_MODE_VIBRATE) }
                            SoundModeItem("השתק", Icons.AutoMirrored.Rounded.VolumeOff, currentMode == AudioManager.RINGER_MODE_SILENT, theme) { viewModel.setRingerMode(AudioManager.RINGER_MODE_SILENT) }
                        }
                    }
                }
                item { SettingHeader("עוצמת שמע", theme) }
                item {
                    SettingsCard(theme) {
                        VolumeSlider("מדיה", viewModel.mediaVolume.value, theme) { viewModel.setVolume(AudioManager.STREAM_MUSIC, it) }
                        SettingDivider(theme)
                        VolumeSlider("מערכת", viewModel.systemVolume.value, theme) { viewModel.setVolume(AudioManager.STREAM_SYSTEM, it) }
                        SettingDivider(theme)
                        VolumeSlider("התראות", viewModel.notificationVolume.value, theme) { viewModel.setNotificationVolume(it) }
                        SettingDivider(theme)
                        VolumeSlider("אזעקה", viewModel.alarmVolume.value, theme) { viewModel.setAlarmVolume(it) }
                        SettingDivider(theme)
                        VolumeSlider("שיחות", viewModel.callVolume.value, theme) { viewModel.setCallVolume(it) }
                    }
                }
                item { SettingHeader("קול ורטט", theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem("רינגטון", null, Icons.Rounded.MusicNote, theme) { navController.navigate(Screen.Ringtone.route) }
                        SettingDivider(theme)
                        SettingItem("צליל התראות", null, Icons.Rounded.NotificationsActive, theme) { navController.navigate(Screen.NotificationSound.route) }
                        SettingDivider(theme)
                        SettingItem("צליל אזעקה", null, Icons.Rounded.Alarm, theme) { navController.navigate(Screen.AlarmSound.route) }
                        SettingDivider(theme)
                        SettingSwitch("רטט בצלצול", null, viewModel.vibrateWhenRinging.value, { viewModel.toggleVibrateWhenRinging() }, theme)
                        SettingDivider(theme)
                        SettingSwitch("צלילי מקשים", null, viewModel.soundEffects.value, { viewModel.toggleSoundEffects() }, theme)
                        SettingDivider(theme)
                        SettingSwitch("צלילי חיוג", "צליל בלחיצה על מקש בחייגן", viewModel.dialPadTones.value, { viewModel.toggleDialPadTones() }, theme)
                        SettingDivider(theme)
                        SettingSwitch("משוב רטט", null, viewModel.hapticFeedback.value, { viewModel.toggleHapticFeedback() }, theme)
                        SettingDivider(theme)
                        SettingSwitch("צליל נעילת מסך", null, viewModel.lockSound.value, { viewModel.toggleLockSound() }, theme)
                        SettingDivider(theme)
                        SettingSwitch("צליל טעינה", "צליל בחיבור/ניתוק מטען", viewModel.chargingSound.value, { viewModel.toggleChargingSound() }, theme)
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
            SmallHeader("תצוגה", theme) { navController.popBackStack() }
            LazyColumn {
                item { SettingHeader("תצוגה", theme) }
                item {
                    SettingsCard(theme) {
                        VolumeSlider("בהירות מסך", viewModel.brightness.value, theme) { viewModel.setBrightness(it) }
                        SettingDivider(theme)
                        SettingSwitch("בהירות אדפטיבית", "התאמה אוטומטית לתאורת הסביבה", viewModel.adaptiveBrightness.value, { viewModel.toggleAdaptiveBrightness() }, theme)
                        SettingDivider(theme)
                        VolumeSlider("גודל טקסט", (theme.fontSizeMultiplier - 0.8f) / 0.6f, theme) {
                            viewModel.updateFontSize(0.8f + (it.coerceIn(0f, 1f) * 0.6f))
                        }
                        SettingDivider(theme)
                        SettingSwitch("מצב כהה", if (theme.isDarkMode) "מופעל" else "כבוי", theme.isDarkMode, { viewModel.toggleDarkMode() }, theme)
                        SettingDivider(theme)
                        SettingSwitch("הפחתת אנימציות", "מסכים יעברו בלי הנפשה", viewModel.animationsReduced.value, { viewModel.toggleAnimationsReduced() }, theme)
                        SettingDivider(theme)
                        val zoomStep = remember(viewModel.densityDpi.value) {
                            ((viewModel.densityDpi.value.toFloat() / viewModel.defaultDensityDpi - 0.88f) / 0.06f).toInt().coerceIn(0, viewModel.densityPresetsCount - 1)
                        }
                        VolumeSlider("זום מסך", zoomStep.toFloat() / (viewModel.densityPresetsCount - 1), theme) {
                            viewModel.setScreenZoomStep((it * (viewModel.densityPresetsCount - 1)).toInt())
                        }
                        if (viewModel.densityDpi.value != viewModel.defaultDensityDpi) {
                            SettingDivider(theme)
                            SettingItem("איפוס זום מסך", null, Icons.Rounded.RestartAlt, theme, showChevron = false) { viewModel.resetScreenZoom() }
                        }
                        SettingDivider(theme)
                        SettingItem("כיבוי מסך אוטומטי", viewModel.screenTimeoutLabel(viewModel.screenTimeout.value), Icons.Rounded.Timelapse, theme) {
                            viewModel.cycleScreenTimeout()
                        }
                    }
                }
                item { SettingHeader("צבע הדגשה", theme) }
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
                    Text("לא נמצאו צלילים במכשיר", color = theme.textColor.copy(alpha = 0.5f), fontSize = 14.sp)
                }
            } else {
                LazyColumn {
                    item {
                        SettingsCard(theme) {
                            ringtones.forEachIndexed { index, ringtone ->
                                SettingItem(
                                    ringtone.title,
                                    null,
                                    if (ringtone.uri == selectedUri) Icons.Rounded.CheckCircle else Icons.Rounded.MusicNote,
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

    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
        Spacer(modifier = Modifier.height(8.dp))
        VolumeSlider("גוון", hue / 360f, theme) { hue = it * 360f; apply(h = hue) }
        VolumeSlider("רוויה", saturation, theme) { saturation = it; apply(s = it) }
        VolumeSlider("בהירות", value, theme) { value = it; apply(v = it) }

        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            val presets = listOf(Color.White, Color(0xFF64D2FF), Color(0xFFFF9F0A), Color(0xFF30D158), Color(0xFFBF5AF2))
            presets.forEach { color ->
                var isFocused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            when {
                                currentColor == color -> Modifier.border(3.dp, theme.textColor, CircleShape)
                                isFocused -> Modifier.border(2.dp, theme.primaryColor, CircleShape)
                                else -> Modifier
                            }
                        )
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusable()
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
    val clockStyleNames = listOf("קלאסי", "דיגיטלי", "מינימלי", "גדול")

    fun reload() { settings = com.future.settings.theme.SystemUiSettingsClient.get(context) }

    val presets = remember(theme.primaryColor) { wallpaperPresets(theme.primaryColor) }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.applyWallpaperFromUri(uri)
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader("שורת מצב, מסך נעילה ורקע", theme) { navController.popBackStack() }
            LazyColumn {
                item { SettingHeader("שורת מצב", theme) }
                item {
                    SettingsCard(theme) {
                        SettingSwitch("הצג אחוז סוללה", null, settings.showBattery, {
                            com.future.settings.theme.SystemUiSettingsClient.setShowBattery(context, it); reload()
                        }, theme)
                        SettingDivider(theme)
                        SettingSwitch("הצג סמל Bluetooth", null, settings.showBluetooth, {
                            com.future.settings.theme.SystemUiSettingsClient.setShowBluetooth(context, it); reload()
                        }, theme)
                        SettingDivider(theme)
                        SettingSwitch("שעון 24 שעות", null, settings.use24HourClock, {
                            com.future.settings.theme.SystemUiSettingsClient.setUse24HourClock(context, it); reload()
                        }, theme)
                        SettingDivider(theme)
                        SettingSwitch("הסתר סרגלי מערכת", "מסתיר את סרגל הניווט המקורי של אנדרואיד", settings.suppressSystemBars, {
                            com.future.settings.theme.SystemUiSettingsClient.setSuppressSystemBars(context, it); reload()
                        }, theme)
                    }
                }
                item { SettingHeader("מסך נעילה", theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem("סגנון שעון", clockStyleNames.getOrElse(settings.clockStyle) { "קלאסי" }, Icons.Rounded.Schedule, theme) {
                            val next = (settings.clockStyle + 1) % clockStyleNames.size
                            com.future.settings.theme.SystemUiSettingsClient.setClockStyle(context, next)
                            reload()
                        }
                    }
                }
                item { SettingHeader("רקע מסך", theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem("בחר תמונה מהגלריה", "טפט מותאם אישית מהתמונות במכשיר", Icons.Rounded.Image, theme) {
                            galleryLauncher.launch("image/*")
                        }
                    }
                }
                item { SettingHeader("טפטים מובנים", theme) }
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxWidth().height(420.dp).padding(horizontal = 12.dp),
                        userScrollEnabled = false
                    ) {
                        items(presets) { preset ->
                            WallpaperPresetTile(preset, theme) {
                                viewModel.applyWallpaperBitmap(renderGradientWallpaperBitmap(context, preset.colors))
                            }
                        }
                    }
                }
                item {
                    Text(
                        text = "כדי לסדר, להוסיף או להסיר סמלים ממסך הבית - לחצו פעמיים על מקש האפשרויות במסך הבית עצמו כדי להיכנס למצב עריכה.",
                        color = theme.textColor.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }
        }
        if (viewModel.isApplyingWallpaper.value) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = theme.primaryColor)
            }
        }
    }
}

/** רשימת טפטי הגרדיאנט המובנים - מוגדרים כצבעים בקוד (לא כקבצי תמונה בינאריים),
 *  ומוצגים כתצוגה מקדימה חיה עם Brush; בבחירה הם מצוירים ל-Bitmap אמיתי בגודל המסך
 *  ומוחלים דרך WallpaperManager. הפריסט האחרון עוקב אחרי צבע ההדגשה הנוכחי של המשתמש
 *  כדי שתמיד יהיה טפט אחד שמתאים אוטומטית לעיצוב שהוא בחר. */
private data class WallpaperPreset(val name: String, val colors: List<Color>)

private fun wallpaperPresets(accentColor: Color): List<WallpaperPreset> = listOf(
    WallpaperPreset("כחול עמוק", listOf(Color(0xFF123A5E), Color(0xFF040B14), Color.Black)),
    WallpaperPreset("סגול לילה", listOf(Color(0xFF4A1D6E), Color(0xFF12051C), Color.Black)),
    WallpaperPreset("שקיעה כתומה", listOf(Color(0xFF8C3A10), Color(0xFF230D05), Color.Black)),
    WallpaperPreset("ירוק ניאון", listOf(Color(0xFF0F5C34), Color(0xFF03130A), Color.Black)),
    WallpaperPreset("מונוכרום", listOf(Color(0xFF3A3A40), Color(0xFF0E0E10), Color.Black)),
    WallpaperPreset("צבע ההדגשה שלי", listOf(accentColor, Color(0xFF0A0A0C), Color.Black))
)

/** מצייר את הגרדיאנט לביטמאפ אמיתי בגודל המסך הפיזי - כך שהטפט המוחל לא מפוקסל
 *  ומכסה את כל המסך, לא רק את שטח תצוגת המקדימה הקטנה בתוך הרשימה. */
private fun renderGradientWallpaperBitmap(context: android.content.Context, colors: List<Color>): android.graphics.Bitmap {
    val metrics = context.resources.displayMetrics
    val width = metrics.widthPixels.coerceAtLeast(1)
    val height = metrics.heightPixels.coerceAtLeast(1)
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint().apply {
        shader = android.graphics.LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            colors.map { it.toArgb() }.toIntArray(), null, android.graphics.Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    return bitmap
}

@Composable
private fun WallpaperPresetTile(preset: WallpaperPreset, theme: ThemeConfig, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .padding(6.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent {
                if (it.type == KeyEventType.KeyDown && (it.key == Key.DirectionCenter || it.key == Key.Enter || it.key == Key.NumPadEnter)) {
                    onClick(); true
                } else false
            }
            .focusable()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.62f)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(preset.colors))
                .then(
                    if (isFocused) Modifier.border(width = 2.dp, color = theme.primaryColor, shape = RoundedCornerShape(16.dp))
                    else Modifier
                )
        )
        Text(
            text = preset.name,
            fontSize = 12.sp,
            color = theme.textColor.copy(alpha = 0.8f),
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
            SmallHeader("סוללה", theme) { navController.popBackStack() }
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
                                        viewModel.isCharging.value -> "טוען כוח... ⚡ עוד קצת ונהיה במיטבנו"
                                        percent >= 80 -> "מרגיש חזק היום! 💪 $percent% זה כוח אמיתי"
                                        percent >= 30 -> "עוד יש לאן להתקדם, אבל בסך הכל מסתדרים"
                                        else -> "המצב קצת דחוק... אולי כדאי לחפש שקע 🔌"
                                    }
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("${viewModel.batteryPercent.value}%", fontSize = 52.sp, fontWeight = FontWeight.Bold, color = theme.textColor)
                        Text(
                            if (viewModel.isCharging.value) "בטעינה" else "לא בטעינה",
                            fontSize = 14.sp,
                            color = theme.textColor.copy(alpha = 0.6f)
                        )
                    }
                }
                item {
                    SettingsCard(theme) {
                        SettingSwitch("חיסכון בחשמל", "הגבל ביצועים כדי לחסוך סוללה", viewModel.batterySaver.value, { viewModel.toggleBatterySaver() }, theme)
                        SettingDivider(theme)
                        VolumeSlider(
                            "סף חיסכון אוטומטי (${viewModel.lowBatteryWarningLevel.value}%)",
                            viewModel.lowBatteryWarningLevel.value / 100f,
                            theme
                        ) { viewModel.setLowBatteryWarningLevel((it * 100).toInt().coerceIn(0, 100)) }
                    }
                }
                item { SettingHeader("מצב הסוללה", theme) }
                item {
                    SettingsCard(theme) {
                        val details = viewModel.batteryDetails.value
                        SettingItem("טמפרטורה", "%.1f°C".format(details.tempCelsius), Icons.Rounded.Thermostat, theme, showChevron = false) { }
                        SettingDivider(theme)
                        SettingItem("מתח", "%.2fV".format(details.voltageVolts), Icons.Rounded.Bolt, theme, showChevron = false) { }
                        SettingDivider(theme)
                        SettingItem("תקינות", details.healthLabel, Icons.Rounded.HealthAndSafety, theme, showChevron = false) { }
                    }
                }
                item { SettingHeader("מידע סוללה מפורט", theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem("מחזורי טעינה", extended.cycleCount?.toString() ?: "לא זמין במכשיר זה", Icons.Rounded.Autorenew, theme, showChevron = false) { }
                        SettingDivider(theme)
                        SettingItem("קיבולת נטענת", extended.chargeCounterMah?.let { "$it mAh" } ?: "לא זמין", Icons.Rounded.BatteryChargingFull, theme, showChevron = false) { }
                        SettingDivider(theme)
                        SettingItem("זרם נוכחי", extended.currentNowMa?.let { "$it mA" } ?: "לא זמין", Icons.Rounded.Bolt, theme, showChevron = false) { }
                    }
                }
            }
        }
        battMessage?.let { msg -> EasterEggMessage("🔋", "מצב הסוללה", msg) { battMessage = null } }
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
            SmallHeader("אחסון", theme) { navController.popBackStack() }
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
                                        fraction > 0.9f -> "כמעט מלא לגמרי! אולי כדאי לפנות קצת מקום 😅"
                                        fraction > 0.6f -> "יש שימוש נאה באחסון - עדיין יש לאן לגדול"
                                        else -> "יש המון מקום פנוי - תרגיש חופשי לצבור עוד קבצים 🎉"
                                    }
                                }
                            }
                    ) {
                        Text(
                            text = "%.1f GB מתוך %.1f GB בשימוש".format(usedGb, totalGb),
                            color = theme.textColor,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(theme.surfaceColor)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(theme.primaryColor)
                            )
                        }
                    }
                }
                item {
                    SettingsCard(theme) {
                        val freeing = viewModel.isFreeingSpace.value
                        SettingItem(
                            if (freeing) "מפנה מקום..." else "פינוי מקום עכשיו",
                            "ניקוי קבצי מטמון של המערכת",
                            Icons.Rounded.CleaningServices,
                            theme,
                            showChevron = false
                        ) {
                            if (!freeing) viewModel.freeUpSpace()
                        }
                    }
                }
            }
        }
        storageMessage?.let { msg -> EasterEggMessage("💾", "סטטוס אחסון", msg) { storageMessage = null } }
    }
}

@Composable
fun GeneralScreen(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    var showRestartConfirm by remember { mutableStateOf(false) }
    var showShutdownConfirm by remember { mutableStateOf(false) }
    var showMultiWindowConfirm by remember { mutableStateOf(false) }

    if (showMultiWindowConfirm) {
        ConfirmDialog(
            theme = theme,
            title = "חלון מרובה",
            message = "הפעלת האפשרות הזו עלולה לגרום למסכים מסוימים להיפתח במצב מפוצל שקשה לצאת ממנו במכשיר הזה, שכן אין תמיכה במגע. להמשיך?",
            confirmLabel = "הפעל",
            onConfirm = { showMultiWindowConfirm = false; viewModel.toggleMultiWindowForced() },
            onDismiss = { showMultiWindowConfirm = false }
        )
    }

    if (showRestartConfirm) {
        ConfirmDialog(
            theme = theme,
            title = "הפעלה מחדש",
            message = "המכשיר יופעל מחדש כעת.",
            confirmLabel = "הפעל מחדש",
            onConfirm = { showRestartConfirm = false; viewModel.restartDevice() },
            onDismiss = { showRestartConfirm = false }
        )
    }
    if (showShutdownConfirm) {
        ConfirmDialog(
            theme = theme,
            title = "כיבוי המכשיר",
            message = "המכשיר יכבה כעת. תצטרך ללחוץ על מקש ההפעלה כדי להדליק אותו שוב.",
            confirmLabel = "כבה",
            onConfirm = { showShutdownConfirm = false; viewModel.shutdownDevice() },
            onDismiss = { showShutdownConfirm = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader("כללי", theme) { navController.popBackStack() }
            LazyColumn {
                item { SettingHeader("תאריך ושעה", theme) }
                item {
                    SettingsCard(theme) {
                        SettingSwitch("תצוגת 24 שעות", if (viewModel.use24Hour.value) "14:30" else "2:30 PM", viewModel.use24Hour.value, { viewModel.toggle24Hour() }, theme)
                        SettingDivider(theme)
                        SettingSwitch("עדכון אוטומטי", "לפי הרשת הסלולרית", viewModel.autoTime.value, { viewModel.toggleAutoTime() }, theme)
                        SettingDivider(theme)
                        SettingSwitch("אזור זמן אוטומטי", "לפי הרשת הסלולרית", viewModel.autoTimeZone.value, { viewModel.toggleAutoTimeZone() }, theme)
                    }
                }
                item { SettingHeader("שפה ונגישות", theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem("שפת מערכת", null, Icons.Rounded.Translate, theme) { navController.navigate(Screen.Language.route) }
                        SettingDivider(theme)
                        SettingItem("גודל טקסט", "מתוך תצוגה", Icons.Rounded.Accessibility, theme) { navController.navigate(Screen.Display.route) }
                        SettingDivider(theme)
                        SettingSwitch("היפוך צבעים", "לניגודיות גבוהה יותר", viewModel.colorInversion.value, { viewModel.toggleColorInversion() }, theme)
                        SettingDivider(theme)
                        SettingItem("הגדרות נגישות נוספות", null, Icons.Rounded.Accessibility, theme) {
                            safeStartActivity(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), "לא ניתן לפתוח הגדרות נגישות")
                        }
                    }
                }
                item { SettingHeader("אבטחה", theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem("אבטחה", "נעילת מסך FutureOS", Icons.Rounded.Security, theme) { navController.navigate(Screen.Security.route) }
                        SettingDivider(theme)
                        SettingItem("התראות ומיקוד", "גישת מדיניות התראות, מצב מיקוד", Icons.Rounded.NotificationsActive, theme) {
                            navController.navigate(Screen.NotificationsFocus.route)
                        }
                    }
                }
                item { SettingHeader("בטיחות וחירום", theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem("SOS ואיש קשר לחירום", "שליחת מיקום והודעת חירום", Icons.Rounded.Sos, theme) {
                            navController.navigate(Screen.Sos.route)
                        }
                    }
                }
                item { SettingHeader("FutureOS", theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem("איפוס הגדרות FutureOS", "מצב כהה, צבע הדגשה, גודל טקסט", Icons.Rounded.RestartAlt, theme) {
                            viewModel.resetFutureOsSettings()
                        }
                    }
                }
                item { SettingHeader("מתקדם", theme) }
                item {
                    SettingsCard(theme) {
                        val nfcAvailable = remember { viewModel.getSystemInteractor().isNfcAvailable() }
                        if (nfcAvailable) {
                            SettingSwitch("NFC", null, viewModel.nfcEnabled.value, { viewModel.toggleNfc() }, theme)
                            SettingDivider(theme)
                        }
                        SettingSwitch("השאר פעיל בזמן טעינה", "המסך לא יכבה כשהמכשיר מחובר למטען", viewModel.stayAwakeWhileCharging.value, { viewModel.toggleStayAwakeWhileCharging() }, theme)
                        SettingDivider(theme)
                        SettingSwitch("חלון מרובה (Multi Window)", "מכריח כל אפליקציה להיפתח גם בחלון מפוצל", viewModel.multiWindowForced.value, {
                            if (viewModel.multiWindowForced.value) {
                                // כיבוי הוא "דלת היציאה" - לא דורש אישור.
                                viewModel.toggleMultiWindowForced()
                            } else {
                                showMultiWindowConfirm = true
                            }
                        }, theme)
                        SettingDivider(theme)
                        SettingItem("הפעלה מחדש", null, Icons.Rounded.RestartAlt, theme, showChevron = false) { showRestartConfirm = true }
                        SettingDivider(theme)
                        SettingItem("כיבוי המכשיר", null, Icons.Rounded.PowerSettingsNew, theme, showChevron = false) { showShutdownConfirm = true }
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
            SmallHeader("שפת מערכת", theme) { navController.popBackStack() }
            LazyColumn {
                item {
                    Text(
                        text = "שינוי שפת המערכת דורש הרשאת root ועשוי לדרוש הפעלה מחדש של אפליקציות פתוחות.",
                        color = theme.textColor.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                item {
                    SettingsCard(theme) {
                        languages.forEachIndexed { index, (label, tag) ->
                            SettingItem(label, null, if (current.startsWith(tag.take(2))) Icons.Rounded.CheckCircle else Icons.Rounded.Language, theme, showChevron = false) {
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
    val interactor = remember { com.future.settings.utils.SystemInteractor(context) }
    val isSecure = remember { interactor.isDeviceSecure() }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader("אבטחה", theme) { navController.popBackStack() }
            LazyColumn {
                item {
                    SettingsCard(theme) {
                        SettingItem(
                            if (isSecure) "המכשיר מאובטח" else "המכשיר לא מאובטח",
                            if (isSecure) "נעילת מסך פעילה" else "לא הוגדר קוד נעילה",
                            if (isSecure) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                            theme,
                            showChevron = false
                        ) { }
                    }
                }
                item { SettingHeader("נעילת מסך", theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem("קוד PIN של FutureUI", "הגדרת/שינוי קוד הנעילה", Icons.Rounded.Pin, theme) {
                            safeStartActivity(
                                context,
                                Intent().setClassName(FUTURE_UI_PACKAGE, FUTURE_UI_SETTINGS_ACTIVITY),
                                "FutureUI לא מותקן על המכשיר"
                            )
                        }
                        SettingDivider(theme)
                        SettingItem("זיהוי פנים וביומטריה", "פותח את הרשמת הביומטריה של המערכת", Icons.Rounded.Face, theme) {
                            safeStartActivity(
                                context,
                                Intent(Settings.ACTION_BIOMETRIC_ENROLL),
                                "המכשיר הזה לא תומך בהרשמה ביומטרית"
                            )
                        }
                    }
                }
                item { SettingHeader("איתור ופרטיות מתקדמת", theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem("איתור המכשיר (Find My Device)", "מיקום, נעילה ומחיקה מרחוק דרך Google", Icons.Rounded.MyLocation, theme) {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.adm")
                            if (launchIntent != null) {
                                safeStartActivity(context, launchIntent, "לא ניתן לפתוח את Find My Device")
                            } else {
                                android.widget.Toast.makeText(context, "אפליקציית Find My Device לא מותקנת על המכשיר", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                        SettingDivider(theme)
                        SettingItem("אזור פרטי (Private Space)", "אזור מוצפן ומבודד לאפליקציות ונתונים", Icons.Rounded.Lock, theme) {
                            safeStartActivity(
                                context,
                                Intent("android.settings.PRIVATE_SPACE_SETTINGS"),
                                "אזור פרטי לא נתמך בגרסת האנדרואיד של המכשיר הזה"
                            )
                        }
                    }
                }
            }
        }
    }
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
                    leetMessage = "לא נמצאו באגים. כלומר... אולי כן, אבל הם רשומים כפיצ'רים. 🐛"
                }
                false
            }
    ) {
        Column {
            SmallHeader("אפליקציות", theme) { navController.popBackStack() }
            LazyColumn {
                item {
                    SettingsCard(theme) {
                        FUTURE_OS_APPS.forEachIndexed { index, app ->
                            val installed = installedMap[app.packageName] == true
                            SettingItem(
                                app.displayName,
                                if (installed) "מותקנת" else "לא מותקנת",
                                app.icon,
                                theme
                            ) { navController.navigate(Screen.AppDetail.route(app.packageName)) }
                            if (index < FUTURE_OS_APPS.lastIndex) SettingDivider(theme)
                        }
                    }
                }
            }
        }
        leetMessage?.let { msg -> EasterEggMessage("🐛", "מצב מפתח סמוי", msg) { leetMessage = null } }
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
    var audioMuted by remember(packageName) { mutableStateOf(if (installed) interactor.isAppAudioMuted(packageName) else false) }

    if (showClearDataConfirm) {
        ConfirmDialog(
            theme = theme,
            title = "מחיקת נתונים",
            message = "כל הנתונים המקומיים של ${app?.displayName ?: packageName} יימחקו לצמיתות. לא ניתן לבטל פעולה זו.",
            confirmLabel = "מחק נתונים",
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
                        Text(packageName, fontSize = 12.sp, color = theme.textColor.copy(alpha = 0.5f))
                    }
                }
                item {
                    SettingsCard(theme) {
                        SettingItem("סטטוס", if (installed) "מותקנת" else "לא מותקנת", if (installed) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel, theme, showChevron = false) { }
                        if (versionName != null) {
                            SettingDivider(theme)
                            SettingItem("גרסה", versionName, Icons.Rounded.Info, theme, showChevron = false) { }
                        }
                    }
                }
                if (installed) {
                    item { SettingHeader("סאונד", theme) }
                    item {
                        SettingsCard(theme) {
                            SettingSwitch(
                                "השתקת סאונד לאפליקציה זו",
                                "משתיק את פלט האודיו של האפליקציה בלבד",
                                audioMuted,
                                { newValue ->
                                    audioMuted = newValue
                                    viewModel.setAppAudioMuted(packageName, newValue) { }
                                },
                                theme
                            )
                        }
                    }
                    item { SettingHeader("פעולות", theme) }
                    item {
                        SettingsCard(theme) {
                            SettingItem("פתח אפליקציה", null, Icons.AutoMirrored.Rounded.Launch, theme, showChevron = false) {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                                if (launchIntent != null) {
                                    safeStartActivity(context, launchIntent, "לא ניתן לפתוח את האפליקציה")
                                } else {
                                    android.widget.Toast.makeText(context, "לא ניתן לפתוח את האפליקציה", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                            SettingDivider(theme)
                            SettingItem("עצירה בכפייה", "סוגר את האפליקציה אם היא תקועה", Icons.Rounded.StopCircle, theme, showChevron = false) {
                                viewModel.forceStopApp(packageName, app?.displayName ?: packageName)
                            }
                            SettingDivider(theme)
                            SettingItem("מחיקת נתונים", "מוחק לצמיתות את כל המידע השמור", Icons.Rounded.DeleteForever, theme, showChevron = false) {
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
@Composable
fun ConfirmDialog(
    theme: ThemeConfig,
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.surfaceColor,
        titleContentColor = theme.textColor,
        textContentColor = theme.textColor.copy(alpha = 0.8f),
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = Color(0xFFFF453A), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ביטול", color = theme.primaryColor)
            }
        }
    )
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
            SmallHeader("ביצועים ותחזוקה", theme) { navController.popBackStack() }
            LazyColumn {
                item { SettingHeader("זיכרון (RAM)", theme) }
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                        Text(
                            text = "%.1f GB מתוך %.1f GB בשימוש".format(usedGb, totalGb),
                            color = theme.textColor,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(theme.surfaceColor)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (ram.lowMemory) Color(0xFFFF453A) else theme.primaryColor)
                            )
                        }
                        if (ram.lowMemory) {
                            Text("זיכרון נמוך - מומלץ לבצע אופטימיזציה", fontSize = 12.sp, color = Color(0xFFFF453A), modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
                item {
                    SettingsCard(theme) {
                        SettingItem("מעבד", cpuInfo, Icons.Rounded.Memory, theme, showChevron = false) { }
                    }
                }
                item { SettingHeader("תחזוקה", theme) }
                item {
                    SettingsCard(theme) {
                        val optimizing = viewModel.isOptimizing.value
                        SettingItem(
                            if (optimizing) "מבצע אופטימיזציה..." else "אופטימיזציה מהירה",
                            "ניקוי זיכרון וקבצי מטמון",
                            Icons.Rounded.Speed,
                            theme,
                            showChevron = false
                        ) {
                            if (!optimizing) viewModel.optimizeNow()
                        }
                        SettingDivider(theme)
                        SettingItem("אבחון מכשיר", "בדיקת מגע, רטט, רמקול וחיישנים", Icons.Rounded.Troubleshoot, theme) {
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
            SettingItem("טיימרים לאפליקציות", "הגבלת זמן שימוש יומי", Icons.Rounded.HourglassTop, theme) {
                navController.navigate(Screen.AppTimers.route)
            }
        }
    }

    fun formatDuration(millis: Long): String {
        val totalMinutes = millis / 60000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "$hours שעות ו-$minutes דקות" else "$minutes דקות"
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader("זמן מסך", theme) { navController.popBackStack() }
            LazyColumn {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.HourglassBottom, contentDescription = null, modifier = Modifier.size(48.dp), tint = theme.primaryColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(formatDuration(summary.totalMillis), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = theme.textColor)
                        Text("שימוש היום", fontSize = 13.sp, color = theme.textColor.copy(alpha = 0.6f))
                    }
                }
                if (summary.topApps.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("עדיין אין מספיק נתוני שימוש היום", color = theme.textColor.copy(alpha = 0.5f), fontSize = 13.sp)
                        }
                    }
                } else {
                    item { SettingHeader("הכי בשימוש היום", theme) }
                    item {
                        SettingsCard(theme) {
                            summary.topApps.forEachIndexed { index, entry ->
                                val app = FUTURE_OS_APPS.find { it.packageName == entry.packageName }
                                SettingItem(
                                    app?.displayName ?: entry.packageName,
                                    formatDuration(entry.millis),
                                    app?.icon ?: Icons.Rounded.Apps,
                                    theme,
                                    showChevron = false
                                ) { }
                                if (index < summary.topApps.lastIndex) SettingDivider(theme)
                            }
                        }
                    }
                }
                item { SettingHeader("מגבלות", theme) }
                item { itemsTail() }
            }
        }
    }
}

@Composable
fun DefaultAppsScreen(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.loadDefaultApps() }
    val smsPackage = viewModel.defaultSmsPackage.value
    val dialerPackage = viewModel.defaultDialerPackage.value

    fun openApp(pkg: String?, notInstalledMessage: String) {
        val launchIntent = pkg?.let { context.packageManager.getLaunchIntentForPackage(it) }
        if (launchIntent != null) {
            safeStartActivity(context, launchIntent, notInstalledMessage)
        } else {
            android.widget.Toast.makeText(context, notInstalledMessage, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader("ברירות מחדל", theme) { navController.popBackStack() }
            LazyColumn {
                item {
                    Text(
                        text = "כדי לשנות אפליקציית ברירת מחדל, פתח אותה - היא תבקש את זה בעצמה.",
                        color = theme.textColor.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                item { SettingHeader("הודעות", theme) }
                item {
                    SettingsCard(theme) {
                        val smsApp = FUTURE_OS_APPS.find { it.packageName == smsPackage }
                        SettingItem(
                            smsApp?.displayName ?: smsPackage ?: "לא הוגדר",
                            if (smsPackage == "com.future.messages") "אפליקציית FutureOS" else "אפליקציה אחרת",
                            Icons.AutoMirrored.Rounded.Message,
                            theme,
                            showChevron = false
                        ) { openApp("com.future.messages", "הודעות לא מותקנת על המכשיר") }
                    }
                }
                item { SettingHeader("חיוג", theme) }
                item {
                    SettingsCard(theme) {
                        val dialerApp = FUTURE_OS_APPS.find { it.packageName == dialerPackage }
                        SettingItem(
                            dialerApp?.displayName ?: dialerPackage ?: "לא הוגדר",
                            if (dialerPackage == "com.future.dialer") "אפליקציית FutureOS" else "אפליקציה אחרת",
                            Icons.Rounded.Dialpad,
                            theme,
                            showChevron = false
                        ) { openApp("com.future.dialer", "טלפון לא מותקנת על המכשיר") }
                    }
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
            SmallHeader("פרטיות", theme) { navController.popBackStack() }
            LazyColumn {
                item {
                    SettingsCard(theme) {
                        SettingItem("לוח בקרת פרטיות", "שימוש אחרון בהרשאות רגישות", Icons.Rounded.PrivacyTip, theme) {
                            safeStartActivity(context, Intent("android.settings.PRIVACY_SETTINGS"), "לא ניתן לפתוח לוח בקרת פרטיות")
                        }
                    }
                }
                item { SettingHeader("הרשאות לפי אפליקציה", theme) }
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

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader(app?.displayName ?: packageName, theme) { navController.popBackStack() }
            if (permissions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("לא נמצאו הרשאות רגישות לאפליקציה זו", color = theme.textColor.copy(alpha = 0.5f), fontSize = 14.sp)
                }
            } else {
                LazyColumn {
                    item {
                        SettingsCard(theme) {
                            permissions.forEachIndexed { index, perm ->
                                SettingItem(
                                    perm.shortLabel,
                                    if (perm.granted) "מאושרת" else "לא מאושרת",
                                    if (perm.granted) Icons.Rounded.CheckCircle else Icons.Rounded.Block,
                                    theme,
                                    showChevron = false
                                ) {
                                    if (perm.granted) {
                                        viewModel.revokePermission(packageName, perm.name) { updated -> permissions = updated }
                                    }
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
            SmallHeader("התראות ומיקוד", theme) { navController.popBackStack() }
            LazyColumn {
                item {
                    SettingsCard(theme) {
                        SettingItem("גישת מדיניות התראות", "נדרש כדי לשלוט על מצב שקט/רטט", Icons.Rounded.Notifications, theme) {
                            safeStartActivity(context, Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS), "לא ניתן לפתוח הגדרות התראות")
                        }
                    }
                }
                item { SettingHeader("מיקוד עכשיו", theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem("שקט לשעה הקרובה", "השתקת התראות עם חזרה אוטומטית", Icons.Rounded.DoNotDisturbOn, theme, showChevron = false) {
                            viewModel.focusNow(60)
                        }
                        SettingDivider(theme)
                        SettingItem("ביטול מיקוד", null, Icons.Rounded.NotificationsActive, theme, showChevron = false) {
                            viewModel.cancelFocusNow()
                        }
                    }
                }
                item { SettingHeader("מצב שינה", theme) }
                item {
                    SettingsCard(theme) {
                        SettingSwitch("שקט אוטומטי (23:00–07:00)", "השתקה אוטומטית בשעות השינה", viewModel.sleepModeEnabled.value, { viewModel.toggleSleepMode() }, theme)
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
            SmallHeader("אודות הטלפון", theme) { navController.popBackStack() }
            LazyColumn(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 16.dp)) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.PhoneAndroid, contentDescription = null, modifier = Modifier.size(80.dp), tint = theme.primaryColor)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Future Phone", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = theme.textColor)
                        Text("FutureOS", fontSize = 14.sp, color = theme.textColor.copy(alpha = 0.6f))
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item {
                    SettingsCard(theme) {
                        // 7 הקשות על "דגם" - כמו הקשות על גרסת אנדרואיד באנדרואיד המקורי,
                        // חושף אנימציית לוגו FutureOS (ומשם, החזקת OK פותחת משחק חבוי).
                        SettingItem("דגם", "Future 1", null, theme, showChevron = false) {
                            modelTaps++
                            if (modelTaps >= 7) {
                                modelTaps = 0
                                navController.navigate(Screen.EasterEggReveal.route)
                            }
                        }
                        SettingDivider(theme)
                        // "הקש 7 פעמים כדי להפוך למפתח" - כמו באנדרואיד המקורי, לצורך אמיתי:
                        // חושף גישה למסך "אפשרויות למפתחים" האמיתי של המערכת.
                        SettingItem("מספר בנייה", Build.DISPLAY, null, theme, showChevron = false) {
                            if (viewModel.developerModeEnabled.value) {
                                viewModel.showToast("אתה כבר מפתח")
                            } else {
                                buildTaps++
                                when {
                                    buildTaps >= 7 -> viewModel.enableDeveloperMode()
                                    buildTaps >= 4 -> viewModel.showToast("עוד ${7 - buildTaps} לחיצות ותהיה מפתח")
                                }
                            }
                        }
                        SettingDivider(theme)
                        SettingItem("גרסת קרנל", kernel, null, theme, showChevron = false) { }
                    }
                }
                item { SettingHeader("סטטוס", theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem("זמן פעולה", uptime, Icons.Rounded.Schedule, theme, showChevron = false) { }
                        SettingDivider(theme)
                        SettingItem("זיכרון RAM", ram, Icons.Rounded.Memory, theme, showChevron = false) { }
                    }
                }
                item { SettingHeader("זיהוי המכשיר", theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem("IMEI", viewModel.imei.value ?: "לא זמין במכשיר זה", Icons.Rounded.Pin, theme, showChevron = false) { }
                        SettingDivider(theme)
                        SettingItem("מספר טלפון", viewModel.phoneNumber.value ?: "לא זמין", Icons.Rounded.Phone, theme, showChevron = false) { }
                        SettingDivider(theme)
                        SettingItem("מידע רגולטורי ומשפטי", null, Icons.Rounded.Gavel, theme) { viewModel.openRegulatoryInfo() }
                    }
                }
                if (viewModel.developerModeEnabled.value) {
                    item { SettingHeader("מפתחים", theme) }
                    item {
                        SettingsCard(theme) {
                            SettingItem("אפשרויות למפתחים", "USB, ניפוי באגים, ביצועים", Icons.Rounded.Code, theme) {
                                safeStartActivity(context, Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS), "לא ניתן לפתוח אפשרויות למפתחים")
                            }
                        }
                    }
                }
            }
        }
        fortune?.let { text -> FortuneToast(text) { fortune = null } }
    }
}

@Composable
fun SoundModeItem(label: String, icon: ImageVector, selected: Boolean, theme: ThemeConfig, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }.padding(8.dp)) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (selected) theme.primaryColor else theme.surfaceColor)
                .then(if (isFocused) Modifier.border(2.dp, theme.primaryColor.copy(alpha = 0.8f), CircleShape) else Modifier)
                .onFocusChanged { isFocused = it.isFocused }
                .focusable(),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) theme.backgroundColor else theme.textColor)
        }
        Text(label, fontSize = 12.sp, color = theme.textColor, modifier = Modifier.padding(top = 4.dp))
    }
}

/** מחוון בנוי-בעצמנו במקום Slider של Material3 - ל-Slider יש נקודות סימון
 * שלב מובנות שאי אפשר לכבות (מציירות "נקודות" לאורך כל המסלול, לא רק בערך
 * הנוכחי) וללא אינדיקציית פוקוס אמיתית. גרסה זו שולטת בשני הדברים ותואמת
 * לשפת העיצוב של שאר ההגדרות (מסגרת הדגשה על פוקוס, כמו ב-SettingItem). */
@Composable
fun VolumeSlider(label: String, value: Float, theme: ThemeConfig, onValueChange: (Float) -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    val bgColor by animateColorAsState(
        if (isFocused) theme.primaryColor.copy(alpha = 0.18f) else theme.textColor.copy(alpha = 0.06f),
        label = "volumeSliderBg"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> { onValueChange((value - 0.05f).coerceIn(0f, 1f)); true }
                    Key.DirectionRight -> { onValueChange((value + 0.05f).coerceIn(0f, 1f)); true }
                    else -> false
                }
            }
            .focusable()
            .clip(shape)
            .background(bgColor)
            .then(if (isFocused) Modifier.border(width = 2.dp, color = theme.primaryColor, shape = shape) else Modifier)
            .padding(16.dp)
    ) {
        Text(label, fontSize = 14.sp, color = theme.textColor.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(theme.textColor.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(value.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(3.dp))
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

    val simColors = listOf(Color(0xFF64D2FF), Color(0xFFFF9F0A), Color(0xFF30D158), Color(0xFFBF5AF2), Color(0xFFFF453A))

    if (renamingSubId != null) {
        AlertDialog(
            onDismissRequest = { renamingSubId = null },
            containerColor = theme.surfaceColor,
            titleContentColor = theme.textColor,
            title = { Text("שם תצוגה לקו", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = theme.textColor,
                        unfocusedTextColor = theme.textColor,
                        focusedBorderColor = theme.primaryColor
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val subId = renamingSubId
                    if (subId != null && renameText.isNotBlank()) viewModel.renameSim(subId, renameText.trim())
                    renamingSubId = null
                }) { Text("שמור", color = theme.primaryColor, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { renamingSubId = null }) { Text("ביטול", color = theme.textColor.copy(alpha = 0.6f)) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader("ניהול כרטיסי SIM", theme) { navController.popBackStack() }
            if (sims.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("לא נמצאו כרטיסי SIM פעילים", color = theme.textColor.copy(alpha = 0.5f), fontSize = 14.sp)
                }
            } else {
                LazyColumn {
                    items(sims) { sim ->
                        SettingHeader(sim.displayName, theme)
                        SettingsCard(theme) {
                            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(sim.colorArgb.let { if (it == 0) simColors[0].toArgb() else it })))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sim.carrierName, color = theme.textColor, fontWeight = FontWeight.SemiBold)
                                    Text(sim.phoneNumber ?: "מספר לא זמין", color = theme.textColor.copy(alpha = 0.6f), fontSize = 12.sp)
                                }
                            }
                            SettingDivider(theme)
                            SettingItem("שנה שם תצוגה", null, Icons.Rounded.Edit, theme) {
                                renamingSubId = sim.subscriptionId
                                renameText = sim.displayName
                            }
                            SettingDivider(theme)
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                simColors.forEach { color ->
                                    var isFocused by remember { mutableStateOf(false) }
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .then(
                                                when {
                                                    sim.colorArgb == color.toArgb() -> Modifier.border(2.dp, theme.textColor, CircleShape)
                                                    isFocused -> Modifier.border(2.dp, theme.primaryColor, CircleShape)
                                                    else -> Modifier
                                                }
                                            )
                                            .onFocusChanged { isFocused = it.isFocused }
                                            .focusable()
                                            .clickable { viewModel.recolorSim(sim.subscriptionId, color.toArgb()) }
                                    )
                                }
                            }
                            SettingDivider(theme)
                            SettingSwitch("ברירת מחדל לנתונים", null, viewModel.defaultDataSubId.value == sim.subscriptionId, { if (it) viewModel.setDefaultDataSim(sim.subscriptionId) }, theme)
                            SettingDivider(theme)
                            SettingSwitch("ברירת מחדל לשיחות", null, viewModel.defaultVoiceSubId.value == sim.subscriptionId, { if (it) viewModel.setDefaultVoiceSim(sim.subscriptionId) }, theme)
                            SettingDivider(theme)
                            SettingSwitch("ברירת מחדל ל-SMS", null, viewModel.defaultSmsSubId.value == sim.subscriptionId, { if (it) viewModel.setDefaultSmsSim(sim.subscriptionId) }, theme)
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
        AlertDialog(
            onDismissRequest = { pickingApp = null },
            containerColor = theme.surfaceColor,
            titleContentColor = theme.textColor,
            title = { Text("מכסה יומית ל-${pickingApp?.displayName}", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { new -> if (new.all { it.isDigit() }) minutesText = new },
                    singleLine = true,
                    label = { Text("דקות ליום") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = theme.textColor,
                        unfocusedTextColor = theme.textColor,
                        focusedBorderColor = theme.primaryColor
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val minutes = minutesText.toIntOrNull()
                    val pkg = pickingApp?.packageName
                    if (minutes != null && minutes > 0 && pkg != null) viewModel.setAppTimer(pkg, minutes)
                    pickingApp = null
                }) { Text("הגדר", color = theme.primaryColor, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pickingApp = null }) { Text("ביטול", color = theme.textColor.copy(alpha = 0.6f)) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader("טיימרים לאפליקציות", theme) { navController.popBackStack() }
            LazyColumn {
                if (timers.isNotEmpty()) {
                    item { SettingHeader("מכסות פעילות", theme) }
                    item {
                        SettingsCard(theme) {
                            timers.forEachIndexed { index, timer ->
                                val app = FUTURE_OS_APPS.find { it.packageName == timer.packageName }
                                SettingItem(
                                    app?.displayName ?: timer.packageName,
                                    "${timer.dailyLimitMinutes} דקות ליום - הקש כדי להסיר",
                                    app?.icon ?: Icons.Rounded.Apps,
                                    theme,
                                    showChevron = false
                                ) { viewModel.removeAppTimer(timer.packageName) }
                                if (index < timers.lastIndex) SettingDivider(theme)
                            }
                        }
                    }
                }
                item { SettingHeader("הוספת מכסה", theme) }
                item {
                    SettingsCard(theme) {
                        val available = installedApps.filter { app -> timers.none { it.packageName == app.packageName } }
                        if (available.isEmpty()) {
                            SettingItem("לכל האפליקציות המותקנות כבר יש מכסה", null, null, theme, showChevron = false) { }
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
    var accelText by remember { mutableStateOf("ממתין לנתונים...") }
    var lightText by remember { mutableStateOf("ממתין לנתונים...") }
    var proximityText by remember { mutableStateOf("ממתין לנתונים...") }
    var touchPoints by remember { mutableStateOf(listOf<Offset>()) }

    DisposableEffect(Unit) {
        val accel = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
        val light = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_LIGHT)
        val proximity = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_PROXIMITY)
        val listener = object : android.hardware.SensorEventListener {
            override fun onSensorChanged(event: android.hardware.SensorEvent) {
                when (event.sensor.type) {
                    android.hardware.Sensor.TYPE_ACCELEROMETER ->
                        accelText = "X: %.2f  Y: %.2f  Z: %.2f".format(event.values[0], event.values[1], event.values[2])
                    android.hardware.Sensor.TYPE_LIGHT -> lightText = "%.0f לוקס".format(event.values[0])
                    android.hardware.Sensor.TYPE_PROXIMITY -> proximityText = "%.1f ס\"מ".format(event.values[0])
                }
            }
            override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
        }
        accel?.let { sensorManager.registerListener(listener, it, android.hardware.SensorManager.SENSOR_DELAY_UI) }
        light?.let { sensorManager.registerListener(listener, it, android.hardware.SensorManager.SENSOR_DELAY_UI) }
        proximity?.let { sensorManager.registerListener(listener, it, android.hardware.SensorManager.SENSOR_DELAY_UI) }
        if (accel == null) accelText = "לא זמין במכשיר זה"
        if (light == null) lightText = "לא זמין במכשיר זה"
        if (proximity == null) proximityText = "לא זמין במכשיר זה"
        onDispose { sensorManager.unregisterListener(listener) }
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader("אבחון מכשיר", theme) { navController.popBackStack() }
            LazyColumn {
                item { SettingHeader("רמקול ורטט", theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem("השמע צליל בדיקה", null, Icons.AutoMirrored.Rounded.VolumeUp, theme, showChevron = false) {
                            viewModel.testSpeakerTone()
                        }
                        SettingDivider(theme)
                        SettingItem("בדוק רטט", null, Icons.Rounded.Vibration, theme, showChevron = false) {
                            viewModel.testVibration()
                        }
                    }
                }
                item { SettingHeader("חיישנים חיים", theme) }
                item {
                    SettingsCard(theme) {
                        SettingItem("תאוצה (Accelerometer)", accelText, Icons.Rounded.Speed, theme, showChevron = false) { }
                        SettingDivider(theme)
                        SettingItem("חיישן אור", lightText, Icons.Rounded.LightMode, theme, showChevron = false) { }
                        SettingDivider(theme)
                        SettingItem("חיישן קרבה", proximityText, Icons.Rounded.Sensors, theme, showChevron = false) { }
                    }
                }
                item { SettingHeader("מסך מגע", theme) }
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(theme.borderRadius))
                            .background(theme.surfaceColor)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset -> touchPoints = touchPoints + offset },
                                    onDrag = { change, _ -> touchPoints = touchPoints + change.position }
                                )
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            touchPoints.takeLast(400).forEach { point ->
                                drawCircle(color = theme.primaryColor, radius = 4f, center = point)
                            }
                        }
                        Text(
                            "גררו כדי לבדוק תגובת מגע",
                            color = theme.textColor.copy(alpha = 0.35f),
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
                if (touchPoints.isNotEmpty()) {
                    item {
                        SettingsCard(theme) {
                            SettingItem("נקה בדיקת מגע", null, Icons.Rounded.ClearAll, theme, showChevron = false) { touchPoints = emptyList() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SosScreen(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel) {
    LaunchedEffect(Unit) { viewModel.loadEmergencyContact() }
    val contact = viewModel.emergencyContact.value
    var showEditDialog by remember { mutableStateOf(false) }
    var nameField by remember { mutableStateOf(contact?.first ?: "") }
    var phoneField by remember { mutableStateOf(contact?.second ?: "") }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = theme.surfaceColor,
            titleContentColor = theme.textColor,
            title = { Text("איש קשר לחירום", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = nameField,
                        onValueChange = { nameField = it },
                        singleLine = true,
                        label = { Text("שם") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = theme.textColor, unfocusedTextColor = theme.textColor, focusedBorderColor = theme.primaryColor)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phoneField,
                        onValueChange = { phoneField = it },
                        singleLine = true,
                        label = { Text("מספר טלפון") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = theme.textColor, unfocusedTextColor = theme.textColor, focusedBorderColor = theme.primaryColor)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (nameField.isNotBlank() && phoneField.isNotBlank()) {
                        viewModel.saveEmergencyContact(nameField.trim(), phoneField.trim())
                        showEditDialog = false
                    }
                }) { Text("שמור", color = theme.primaryColor, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("ביטול", color = theme.textColor.copy(alpha = 0.6f)) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader("בטיחות וחירום", theme) { navController.popBackStack() }
            LazyColumn {
                item { SettingHeader("איש קשר לחירום", theme) }
                item {
                    SettingsCard(theme) {
                        if (contact != null) {
                            SettingItem(contact.first, contact.second, Icons.Rounded.ContactPhone, theme) {
                                nameField = contact.first
                                phoneField = contact.second
                                showEditDialog = true
                            }
                            SettingDivider(theme)
                            SettingItem("הסר איש קשר", null, Icons.Rounded.PersonRemove, theme, showChevron = false) { viewModel.clearEmergencyContact() }
                        } else {
                            SettingItem("הוסף איש קשר לחירום", "יקבל SMS עם מיקום בעת שליחת SOS", Icons.Rounded.PersonAddAlt, theme) {
                                nameField = ""
                                phoneField = ""
                                showEditDialog = true
                            }
                        }
                    }
                }
                item { SettingHeader("שליחה", theme) }
                item {
                    SettingsCard(theme) {
                        val sending = viewModel.sosSending.value
                        SettingItem(
                            if (sending) "שולח..." else "שלח הודעת SOS עכשיו",
                            "שולח מיקום נוכחי ל-SMS לאיש הקשר",
                            Icons.Rounded.Sos,
                            theme,
                            showChevron = false
                        ) {
                            if (!sending) viewModel.sendSosNow()
                        }
                    }
                }
                item {
                    Text(
                        text = "הפעלה כאן היא ידנית מתוך המסך - טריגר פיזי (למשל 3 לחיצות על כפתור הצד) דורש הרשאת נגישות ברמת FutureUI ולא קיים כרגע.",
                        color = theme.textColor.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}
