package com.future.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.future.settings.ui.components.SettingDivider
import com.future.settings.ui.components.SettingHeader
import com.future.settings.ui.components.SettingItem
import com.future.settings.ui.components.SettingSwitch
import com.future.settings.ui.components.SettingsCard
import com.future.settings.ui.theme.ThemeConfig
import com.future.settings.utils.MoreSystemSettings
import com.future.settings.viewmodel.SettingsViewModel
import com.future.sharednav.components.InputDialog
import com.future.sharednav.i18n.tr
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Wi-Fi: הפעלה, החיבור הנוכחי, סריקה וחיבור לרשת (עם סיסמה), ורשתות שמורות.
 * הכל דרך `cmd wifi` ב-root - בלי לצאת למסך המגע של אנדרואיד.
 */
@Composable
fun WifiScreen(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel) {
    val interactor = remember { viewModel.getSystemInteractor() }
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(MoreSystemSettings.WifiStatus(false, null)) }
    var networks by remember { mutableStateOf(emptyList<MoreSystemSettings.WifiNetwork>()) }
    var saved by remember { mutableStateOf(emptyList<MoreSystemSettings.SavedWifi>()) }
    var scanning by remember { mutableStateOf(false) }
    var connectingTo by remember { mutableStateOf<String?>(null) }
    var passwordFor by remember { mutableStateOf<MoreSystemSettings.WifiNetwork?>(null) }
    var forgetting by remember { mutableStateOf<MoreSystemSettings.SavedWifi?>(null) }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        val s = MoreSystemSettings.wifiStatus(interactor)
        val n = if (s.enabled) MoreSystemSettings.wifiScanResults(interactor) else emptyList()
        val sv = MoreSystemSettings.savedWifiNetworks(interactor)
        withContext(Dispatchers.Main) { status = s; networks = n; saved = sv }
    }

    fun scan() {
        if (scanning) return
        scope.launch {
            scanning = true
            withContext(Dispatchers.IO) { MoreSystemSettings.startWifiScan(interactor) }
            delay(3500)
            refresh()
            scanning = false
        }
    }

    fun connect(network: MoreSystemSettings.WifiNetwork, password: String?) {
        scope.launch {
            connectingTo = network.ssid
            val ok = withContext(Dispatchers.IO) { MoreSystemSettings.connectWifi(interactor, network, password) }
            if (ok) delay(5000)
            refresh()
            connectingTo = null
            viewModel.showToast(
                when {
                    status.connectedSsid == network.ssid -> tr("מחובר ל-{0}", network.ssid)
                    ok -> tr("החיבור ל-{0} עדיין לא הצליח - אולי הסיסמה שגויה", network.ssid)
                    else -> tr("לא ניתן להתחבר - נדרשת הרשאת root")
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        refresh()
        if (status.enabled) scan()
    }

    passwordFor?.let { network ->
        InputDialog(
            title = tr("סיסמה ל-{0}", network.ssid),
            theme = theme.futureTheme,
            placeholder = tr("סיסמת הרשת"),
            confirmLabel = tr("התחבר"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            onDismiss = { passwordFor = null },
            onConfirm = { pass ->
                passwordFor = null
                if (pass.length < 8) viewModel.showToast(tr("סיסמת Wi-Fi צריכה לפחות 8 תווים"))
                else connect(network, pass)
            },
        )
    }
    forgetting?.let { net ->
        ConfirmDialog(
            theme = theme,
            title = tr("שכח רשת"),
            message = tr("למחוק את {0} מהרשתות השמורות? כדי להתחבר שוב תצטרך את הסיסמה.", net.ssid),
            confirmLabel = tr("שכח"),
            onConfirm = {
                forgetting = null
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { MoreSystemSettings.forgetWifi(interactor, net.id) }
                    viewModel.showToast(if (ok) tr("הרשת נמחקה") else tr("לא ניתן למחוק את הרשת"))
                    refresh()
                }
            },
            onDismiss = { forgetting = null },
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader("Wi-Fi", theme) { navController.popBackStack() }
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    SettingsCard(theme) {
                        SettingSwitch("Wi-Fi", tr("חיבור לרשתות אלחוטיות"), status.enabled, { on ->
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) { MoreSystemSettings.setWifiEnabled(interactor, on) }
                                if (!ok) viewModel.showToast(tr("לא ניתן לשנות הגדרה זו - נדרשת הרשאת root"))
                                delay(1500)
                                refresh()
                                if (on && ok) scan()
                            }
                        }, theme)
                        if (status.enabled) {
                            SettingDivider(theme)
                            SettingItem(
                                tr("מחובר כעת"),
                                status.connectedSsid ?: tr("לא מחובר לרשת"),
                                FutureIcons.Wifi,
                                theme,
                                showChevron = false,
                                infoFocusable = true,
                            )
                        }
                    }
                }
                if (status.enabled) {
                    item { SettingHeader(tr("רשתות זמינות"), theme) }
                    item {
                        SettingsCard(theme) {
                            SettingItem(
                                if (scanning) tr("מחפש רשתות...") else tr("חיפוש רשתות"),
                                tr("סריקה מחדש של הרשתות בסביבה"),
                                FutureIcons.Refresh,
                                theme,
                                showChevron = false,
                            ) { scan() }
                            networks.forEach { net ->
                                SettingDivider(theme)
                                val isConnected = net.ssid == status.connectedSsid
                                val summary = when {
                                    isConnected -> tr("מחובר")
                                    connectingTo == net.ssid -> tr("מתחבר...")
                                    net.security == "eap" -> tr("רשת ארגונית - לא נתמכת")
                                    else -> (if (net.secured) tr("מאובטחת") else tr("פתוחה")) + " · " + MoreSystemSettings.signalLabel(net.rssi)
                                }
                                SettingItem(net.ssid, summary, if (net.secured) FutureIcons.Lock else FutureIcons.Wifi, theme, showChevron = false) {
                                    when {
                                        isConnected || connectingTo != null -> Unit
                                        net.security == "eap" -> viewModel.showToast(tr("רשתות ארגוניות (EAP) לא נתמכות"))
                                        net.secured && saved.none { it.ssid == net.ssid } -> passwordFor = net
                                        else -> connect(net, null)
                                    }
                                }
                            }
                        }
                    }
                    if (networks.isEmpty() && !scanning) {
                        item {
                            Text(
                                tr("לא נמצאו רשתות. נסה לחפש שוב."),
                                color = theme.textColor.copy(alpha = 0.6f),
                                fontSize = FutureTypography.label,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
                if (saved.isNotEmpty()) {
                    item { SettingHeader(tr("רשתות שמורות"), theme) }
                    item {
                        SettingsCard(theme) {
                            saved.forEachIndexed { index, net ->
                                SettingItem(net.ssid, tr("לחץ OK כדי לשכוח את הרשת"), FutureIcons.Delete, theme, showChevron = false) { forgetting = net }
                                if (index < saved.lastIndex) SettingDivider(theme)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** בחירת אזור זמן ידנית (מכבה אזור זמן אוטומטי). */
@Composable
fun TimeZoneScreen(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel) {
    val interactor = remember { viewModel.getSystemInteractor() }
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { current = withContext(Dispatchers.IO) { MoreSystemSettings.currentTimeZone(interactor) } }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader(tr("אזור זמן"), theme) { navController.popBackStack() }
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    Text(
                        tr("בחירת אזור זמן מכבה את אזור הזמן האוטומטי."),
                        color = theme.textColor.copy(alpha = 0.6f),
                        fontSize = FutureTypography.label,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
                item {
                    SettingsCard(theme) {
                        MoreSystemSettings.TIME_ZONES.forEachIndexed { index, (id, label) ->
                            SettingItem(
                                label,
                                MoreSystemSettings.gmtOffsetLabel(id),
                                if (id == current) FutureIcons.RadioButtonChecked else FutureIcons.RadioButtonUnchecked,
                                theme,
                                showChevron = false,
                            ) {
                                scope.launch {
                                    val ok = withContext(Dispatchers.IO) { MoreSystemSettings.setTimeZone(interactor, id) }
                                    if (ok) {
                                        current = id
                                        viewModel.refreshAutoTimeZone()
                                        viewModel.showToast(tr("אזור הזמן שונה ל{0}", label))
                                    } else {
                                        viewModel.showToast(tr("לא ניתן לשנות אזור זמן - נדרשת הרשאת root"))
                                    }
                                }
                            }
                            if (index < MoreSystemSettings.TIME_ZONES.lastIndex) SettingDivider(theme)
                        }
                    }
                }
            }
        }
    }
}

/** שורות "הגדרת שעה" ו"הגדרת תאריך" למסך כללי - פעילות רק כשהשעה האוטומטית כבויה. */
@Composable
fun ManualDateTimeRows(theme: ThemeConfig, viewModel: SettingsViewModel) {
    val interactor = remember { viewModel.getSystemInteractor() }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<String?>(null) } // "time" / "date"
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { now = System.currentTimeMillis(); delay(15_000) }
    }
    val autoTime = viewModel.autoTime.value
    val timeText = java.text.SimpleDateFormat("HH:mm", java.util.Locale.ROOT).format(java.util.Date(now))
    val dateText = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.ROOT).format(java.util.Date(now))

    fun apply(millis: Long) {
        scope.launch {
            val ok = withContext(Dispatchers.IO) { MoreSystemSettings.setSystemTime(interactor, millis) }
            viewModel.refreshAutoTime()
            now = System.currentTimeMillis()
            viewModel.showToast(if (ok) tr("השעון עודכן") else tr("לא ניתן לשנות את השעה - נדרשת הרשאת root"))
        }
    }

    editing?.let { kind ->
        InputDialog(
            title = if (kind == "time") tr("הגדרת שעה") else tr("הגדרת תאריך"),
            theme = theme.futureTheme,
            initialValue = if (kind == "time") timeText.replace(":", "") else dateText.replace(".", ""),
            placeholder = if (kind == "time") tr("שעה: 4 ספרות, למשל 1430") else tr("תאריך: 8 ספרות, למשל 24112026"),
            confirmLabel = tr("הגדר"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            onDismiss = { editing = null },
            onConfirm = { value ->
                editing = null
                val cal = java.util.Calendar.getInstance()
                if (kind == "time") {
                    val t = MoreSystemSettings.parseTime(value)
                    if (t == null) { viewModel.showToast(tr("שעה לא תקינה")); return@InputDialog }
                    cal.set(java.util.Calendar.HOUR_OF_DAY, t.first)
                    cal.set(java.util.Calendar.MINUTE, t.second)
                    cal.set(java.util.Calendar.SECOND, 0)
                } else {
                    val d = MoreSystemSettings.parseDate(value)
                    if (d == null) { viewModel.showToast(tr("תאריך לא תקין")); return@InputDialog }
                    cal.set(d.third, d.second - 1, d.first)
                }
                apply(cal.timeInMillis)
            },
        )
    }

    val disabledHint = tr("כבה קודם שעה ותאריך אוטומטיים")
    SettingItem(tr("הגדרת שעה"), if (autoTime) "$timeText · $disabledHint" else timeText, FutureIcons.Schedule, theme, showChevron = false) {
        if (autoTime) viewModel.showToast(disabledHint) else editing = "time"
    }
    SettingDivider(theme)
    SettingItem(tr("הגדרת תאריך"), if (autoTime) "$dateText · $disabledHint" else dateText, FutureIcons.CalendarToday, theme, showChevron = false) {
        if (autoTime) viewModel.showToast(disabledHint) else editing = "date"
    }
}

/** פירוט האחסון לפי סוג תוכן - נטען ברקע (du יכול לקחת כמה שניות). */
@Composable
fun StorageBreakdownCard(theme: ThemeConfig, viewModel: SettingsViewModel, usedBytes: Long) {
    val interactor = remember { viewModel.getSystemInteractor() }
    var categories by remember { mutableStateOf<List<MoreSystemSettings.StorageCategory>?>(null) }
    LaunchedEffect(usedBytes) {
        categories = withContext(Dispatchers.IO) { MoreSystemSettings.storageBreakdown(interactor, usedBytes) }
    }
    SettingHeader(tr("מה תופס מקום"), theme)
    SettingsCard(theme) {
        val list = categories
        when {
            list == null -> SettingItem(tr("מחשב..."), tr("בודק כמה מקום תופס כל סוג קבצים"), FutureIcons.HourglassEmpty, theme, showChevron = false, infoFocusable = true)
            list.isEmpty() -> SettingItem(tr("פירוט לא זמין"), tr("נדרשת הרשאת root כדי למדוד תיקיות"), FutureIcons.Info, theme, showChevron = false, infoFocusable = true)
            else -> list.forEachIndexed { index, c ->
                val icon = when (c.label) {
                    tr("אפליקציות ונתונים שלהן") -> FutureIcons.Apps
                    tr("תמונות וצילומים") -> FutureIcons.Image
                    tr("סרטונים") -> FutureIcons.Movie
                    tr("שמע והקלטות") -> FutureIcons.MusicNote
                    tr("הורדות") -> FutureIcons.Download
                    tr("מסמכים") -> FutureIcons.Description
                    else -> FutureIcons.Folder
                }
                SettingItem(c.label, MoreSystemSettings.formatBytes(c.bytes), icon, theme, showChevron = false, infoFocusable = true)
                if (index < list.lastIndex) SettingDivider(theme)
            }
        }
    }
}
