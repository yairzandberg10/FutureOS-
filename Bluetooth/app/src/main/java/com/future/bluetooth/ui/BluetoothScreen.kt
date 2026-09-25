package com.future.bluetooth.ui
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching

import com.future.sharednav.icons.FutureIcons

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.future.bluetooth.data.BluetoothController
import com.future.bluetooth.data.BluetoothDeviceInfo
import com.future.bluetooth.data.Connection
import com.future.bluetooth.data.DeviceKind
import com.future.sharednav.components.AnimatedScreenHost
import com.future.sharednav.components.ConfirmDialog
import com.future.sharednav.components.EmptyState
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureButtonVariant
import com.future.sharednav.components.FutureCard
import com.future.sharednav.components.FutureDivider
import com.future.sharednav.components.FutureFocusCard
import com.future.sharednav.components.FutureIndeterminateProgressBar
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureSettingItem
import com.future.sharednav.components.FutureSnackbarHost
import com.future.sharednav.components.FutureSnackbarState
import com.future.sharednav.components.FutureSwitch
import com.future.sharednav.components.InputDialog
import com.future.sharednav.components.ScreenScaffold
import com.future.sharednav.components.rememberFutureSnackbarState
import com.future.sharednav.nav.onOptionsKeyPress
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureAccent
import com.future.sharednav.theme.avatarFillColor
import com.future.sharednav.theme.chevronColor
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.rememberFutureType
import com.future.sharednav.theme.sectionHeaderColor
import com.future.sharednav.theme.subtleTextColor
import com.future.sharednav.theme.textAlpha

/** מה שמשתנה מחוץ לממשק - הרשאות ומסכי מערכת. MainActivity מממש. */
class BluetoothActions(
    val requestPermission: () -> Unit,
    /** בקשת ההפעלה של המערכת - כש-enable() הישיר נדחה. */
    val requestEnable: () -> Unit,
    /** מסך הבלוטות' של המערכת - למה שהמכשיר לא מאפשר לעשות מכאן. */
    val openSystemSettings: () -> Unit,
    /** false כשאין קבצים שהתקבלו. */
    val openReceivedFiles: () -> Boolean,
)

private sealed interface Dialog {
    data class Pair(val device: BluetoothDeviceInfo) : Dialog
    data class Forget(val device: BluetoothDeviceInfo) : Dialog
    data object RenamePhone : Dialog
    data class RenameDevice(val device: BluetoothDeviceInfo) : Dialog
}

/**
 * אפליקציית הבלוטות' (ui_kits/bluetooth): מסך ראשי עם הרדיו, שם המכשיר,
 * מותאמים וזמינים, ומסך מכשיר. מקש התפריט פותח את תפריט האפשרויות.
 */
@Composable
fun BluetoothApp(
    controller: BluetoothController,
    theme: FutureTheme,
    hasPermission: Boolean,
    actions: BluetoothActions,
) {
    var openAddress by rememberSaveable { mutableStateOf<String?>(null) }
    var lastOpened by rememberSaveable { mutableStateOf<String?>(null) }
    // "הצג עוד" - כל המותאמים. מכשיר שנפתח משם חוזר אליו ב-BACK.
    var showAllPaired by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<Dialog?>(null) }
    val snackbar = rememberFutureSnackbarState()

    val ready = controller.isSupported() && hasPermission
    onOptionsKeyPress { if (ready && dialog == null) menuOpen = !menuOpen }

    val openDevice = openAddress?.let { address -> controller.pairedDevices.firstOrNull { it.address == address } }
    // מכשיר שנשכח (או שהרדיו כבה) - חוזרים לרשימה.
    LaunchedEffect(openAddress, openDevice) {
        if (openAddress != null && openDevice == null) openAddress = null
    }
    BackHandler(enabled = openAddress != null || showAllPaired) {
        if (openAddress != null) openAddress = null else showAllPaired = false
    }

    fun toggleRadio() {
        val on = !controller.isEnabled
        if (!controller.setEnabled(on) && on) actions.requestEnable()
    }

    /** פעולה שהמכשיר לא מאפשר מכאן נפתחת במסך של המערכת, עם הסבר קצר. */
    fun orSystem(ok: Boolean) {
        if (!ok) {
            snackbar.show("פותח את הגדרות הבלוטות' של המערכת")
            actions.openSystemSettings()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedScreenHost(
            targetState = openDevice?.address ?: if (showAllPaired) ALL_PAIRED else null,
            depthOf = {
                when (it) {
                    null -> 0
                    ALL_PAIRED -> 1
                    else -> if (showAllPaired) 2 else 1
                }
            },
        ) { address ->
            val device = address?.let { a -> controller.pairedDevices.firstOrNull { it.address == a } }
            val openFromList: (BluetoothDeviceInfo) -> Unit = {
                lastOpened = it.address
                controller.markUsed(it.address)
                openAddress = it.address
            }
            if (address == ALL_PAIRED) {
                AllPairedScreen(
                    controller = controller,
                    theme = theme,
                    focusAddress = lastOpened,
                    onOpenDevice = openFromList,
                )
            } else if (device == null) {
                RootScreen(
                    controller = controller,
                    theme = theme,
                    hasPermission = hasPermission,
                    focusAddress = lastOpened,
                    onMenu = { menuOpen = true },
                    onRequestPermission = actions.requestPermission,
                    onToggleRadio = ::toggleRadio,
                    onRenamePhone = { dialog = Dialog.RenamePhone },
                    onOpenDevice = openFromList,
                    onShowAll = { lastOpened = SHOW_ALL_ROW; showAllPaired = true },
                    onScan = {
                        controller.startDiscovery()
                        snackbar.show("מחפש מכשירים")
                    },
                    onPair = { dialog = Dialog.Pair(it) },
                )
            } else {
                DeviceScreen(
                    device = device,
                    theme = theme,
                    onBack = { openAddress = null },
                    onMenu = { menuOpen = true },
                    onCalls = { orSystem(controller.setProfile(device.address, calls = true, enabled = !device.callsConnected)) },
                    onMedia = { orSystem(controller.setProfile(device.address, calls = false, enabled = !device.mediaConnected)) },
                    onRename = { dialog = Dialog.RenameDevice(device) },
                    onConnect = {
                        if (device.connection == Connection.Disconnected) {
                            orSystem(controller.connect(device.address))
                        } else {
                            val ok = controller.disconnect(device.address)
                            if (ok) snackbar.show("נותק · ${device.name}") else orSystem(false)
                        }
                    },
                    onForget = { dialog = Dialog.Forget(device) },
                )
            }
        }

        FutureSnackbarHost(snackbar, theme)
    }

    if (menuOpen) {
        BluetoothMenu(
            theme = theme,
            onDismiss = { menuOpen = false },
            onRefresh = {
                controller.refreshState()
                controller.startDiscovery()
                snackbar.show("מחפש מכשירים")
            },
            onRenamePhone = { dialog = Dialog.RenamePhone },
            onReceivedFiles = { if (!actions.openReceivedFiles()) snackbar.show("אין קבצים שהתקבלו") },
            onSettings = actions.openSystemSettings,
            onDisconnectAll = {
                val ok = controller.disconnectAll()
                if (ok) snackbar.show("כל המכשירים נותקו") else orSystem(false)
            },
        )
    }

    when (val d = dialog) {
        is Dialog.Pair -> ConfirmDialog(
            message = "להתאים את ${d.device.name}?",
            theme = theme,
            confirmLabel = "התאם",
            destructive = false,
            onCancel = { dialog = null },
            onConfirm = {
                dialog = null
                if (!controller.pair(d.device.address)) snackbar.show("ההתאמה לא התחילה")
            },
        )
        is Dialog.Forget -> ConfirmDialog(
            message = "לשכוח את ${d.device.name}?",
            theme = theme,
            confirmLabel = "שכח",
            onCancel = { dialog = null },
            onConfirm = {
                dialog = null
                if (controller.forget(d.device.address)) {
                    openAddress = null
                    snackbar.show("המכשיר נשכח")
                } else {
                    orSystem(false)
                }
            },
        )
        Dialog.RenamePhone -> InputDialog(
            title = "שם המכשיר",
            theme = theme,
            initialValue = controller.adapterName,
            onDismiss = { dialog = null },
            onConfirm = { name ->
                dialog = null
                if (controller.renameAdapter(name.trim())) snackbar.show("שם המכשיר עודכן")
                else snackbar.show("השם לא עודכן")
            },
        )
        is Dialog.RenameDevice -> InputDialog(
            title = "שנה שם",
            theme = theme,
            initialValue = d.device.name,
            onDismiss = { dialog = null },
            onConfirm = { name ->
                dialog = null
                controller.renameDevice(d.device.address, name.trim())
                snackbar.show("השם עודכן")
            },
        )
        null -> Unit
    }
}

// ---------------------------------------------------------------- מסך ראשי

@Composable
private fun RootScreen(
    controller: BluetoothController,
    theme: FutureTheme,
    hasPermission: Boolean,
    focusAddress: String?,
    onMenu: () -> Unit,
    onRequestPermission: () -> Unit,
    onToggleRadio: () -> Unit,
    onRenamePhone: () -> Unit,
    onOpenDevice: (BluetoothDeviceInfo) -> Unit,
    onShowAll: () -> Unit,
    onScan: () -> Unit,
    onPair: (BluetoothDeviceInfo) -> Unit,
) {
    ScreenScaffold(
        backgroundColor = theme.backgroundColor,
        title = "בלוטות'",
        textColor = theme.textColor,
        accentColor = theme.accentColor,
    ) {
        when {
            !controller.isSupported() -> EmptyState(
                icon = FutureIcons.BluetoothDisabled,
                title = "אין בלוטות' במכשיר הזה",
                textColor = theme.textColor,
            )
            !hasPermission -> PermissionPrompt(theme, onRequestPermission)
            else -> DeviceLists(controller, theme, focusAddress, onToggleRadio, onRenamePhone, onOpenDevice, onShowAll, onScan, onPair)
        }
    }
}

@Composable
private fun DeviceLists(
    controller: BluetoothController,
    theme: FutureTheme,
    focusAddress: String?,
    onToggleRadio: () -> Unit,
    onRenamePhone: () -> Unit,
    onOpenDevice: (BluetoothDeviceInfo) -> Unit,
    onShowAll: () -> Unit,
    onScan: () -> Unit,
    onPair: (BluetoothDeviceInfo) -> Unit,
) {
    val on = controller.isEnabled
    val paired = controller.pairedByRelevance()
    val shownPaired = paired.take(PAIRED_ON_ROOT)
    val nearby = controller.discoveredDevices

    // הפוקוס נוחת על הרדיו, או - בחזרה ממסך מכשיר / מ"הצג עוד" - על אותו כרטיס.
    val radioFocus = remember { FocusRequester() }
    val rowFocus = remember { mutableMapOf<String, FocusRequester>() }
    fun focusFor(address: String) = rowFocus.getOrPut(address) { FocusRequester() }
    LaunchedEffect(Unit) {
        val target = focusAddress?.takeIf { a ->
            (a == SHOW_ALL_ROW && paired.size > PAIRED_ON_ROOT) || shownPaired.any { it.address == a }
        }
        runCatching { if (target != null) focusFor(target).requestFocus() else radioFocus.requestFocus() }
    }
    // הסריקה מתחילה לבד כשנכנסים עם רדיו דולק, כמו בערכת העיצוב.
    LaunchedEffect(on) {
        if (on && !controller.isScanning && nearby.isEmpty()) controller.startDiscovery()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = FutureDimens.spacingXl),
    ) {
        FutureCard(theme = theme) {
            FutureSettingItem(
                title = "בלוטות'",
                summary = if (on) "מופעל" else "כבוי",
                icon = FutureIcons.Bluetooth,
                theme = theme,
                showChevron = false,
                focusRequester = radioFocus,
                onClick = onToggleRadio,
                trailing = { FutureSwitch(checked = on, theme = theme) },
            )
            FutureDivider(theme = theme)
            FutureSettingItem(
                title = "שם המכשיר",
                summary = controller.adapterName.ifEmpty { null },
                icon = FutureIcons.Badge,
                theme = theme,
                onClick = onRenamePhone,
            )
        }

        if (!on) {
            EmptyState(
                icon = FutureIcons.BluetoothDisabled,
                title = "בלוטות' כבוי",
                subtitle = "לחץ על אישור כדי להפעיל ולחפש מכשירים",
                textColor = theme.textColor,
            )
            return@Column
        }

        FutureSectionHeader("מכשירים מותאמים", theme)
        if (paired.isEmpty()) {
            EmptyState(
                icon = FutureIcons.Devices,
                title = "אין מכשירים מותאמים",
                subtitle = "בחר מכשיר מהרשימה שלמטה כדי להתאים",
                textColor = theme.textColor,
            )
        } else {
            CardStack {
                shownPaired.forEach { device ->
                    DeviceCard(
                        device = device,
                        theme = theme,
                        focusRequester = focusFor(device.address),
                        onClick = { onOpenDevice(device) },
                    )
                }
                if (paired.size > PAIRED_ON_ROOT) {
                    ShowMoreCard(
                        hidden = paired.size - PAIRED_ON_ROOT,
                        theme = theme,
                        focusRequester = focusFor(SHOW_ALL_ROW),
                        onClick = onShowAll,
                    )
                }
            }
        }

        FutureSectionHeader("מכשירים זמינים", theme)
        CardStack {
            // כרטיס החיפוש הוא תמיד יעד פוקוס. קודם, כשלא נמצאו מכשירים, לא היה
            // בחלק הזה אף פריט שאפשר להגיע אליו בחצים - והגלילה לא ירדה אליו בכלל.
            ScanCard(scanning = controller.isScanning, found = nearby.size, theme = theme, onClick = onScan)
            nearby.forEach { device ->
                DeviceCard(device = device, theme = theme, onClick = { onPair(device) })
            }
        }
        if (nearby.isEmpty() && !controller.isScanning) {
            EmptyState(
                icon = Icons.AutoMirrored.Rounded.BluetoothSearching,
                title = "לא נמצאו מכשירים",
                subtitle = "לחץ על חפש מכשירים כדי לחפש שוב",
                textColor = theme.textColor,
            )
        }
    }
}

/** כל המכשירים המותאמים - המסך של "הצג עוד". */
@Composable
private fun AllPairedScreen(
    controller: BluetoothController,
    theme: FutureTheme,
    focusAddress: String?,
    onOpenDevice: (BluetoothDeviceInfo) -> Unit,
) {
    val paired = controller.pairedByRelevance()
    val rowFocus = remember { mutableMapOf<String, FocusRequester>() }
    fun focusFor(address: String) = rowFocus.getOrPut(address) { FocusRequester() }
    LaunchedEffect(Unit) {
        val target = paired.firstOrNull { it.address == focusAddress } ?: paired.firstOrNull()
        runCatching { target?.let { focusFor(it.address).requestFocus() } }
    }
    ScreenScaffold(
        backgroundColor = theme.backgroundColor,
        title = "מכשירים מותאמים",
        textColor = theme.textColor,
        accentColor = theme.accentColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = FutureDimens.spacingXl),
        ) {
            CardStack {
                paired.forEach { device ->
                    DeviceCard(
                        device = device,
                        theme = theme,
                        focusRequester = focusFor(device.address),
                        onClick = { onOpenDevice(device) },
                    )
                }
            }
        }
    }
}

/** עמודת כרטיסים: 16dp מדפנות המסך ו-8dp ביניהם. */
@Composable
private fun CardStack(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FutureDimens.spacingLg, vertical = FutureDimens.spacingXs),
        verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
    ) { content() }
}

/**
 * מכשיר ככרטיס: אייקון הסוג בעיגול, שם ומצב, וחץ כניסה. הכרטיס כולו הוא
 * יעד הפוקוס (FutureFocusCard) ולא שורה בתוך כרטיס משותף.
 */
@Composable
private fun DeviceCard(
    device: BluetoothDeviceInfo,
    theme: FutureTheme,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    val type = rememberFutureType()
    val connected = device.connection == Connection.Connected
    FutureFocusCard(theme = theme, onClick = onClick, focusRequester = focusRequester) {
        Box(
            modifier = Modifier
                .size(DeviceCircle)
                .clip(FutureShapes.pill)
                .background(if (connected) accentOf(theme).copy(alpha = 0.18f) else theme.avatarFillColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                iconOf(device.kind),
                contentDescription = null,
                tint = if (connected) accentOf(theme) else theme.textColor,
                modifier = Modifier.size(FutureDimens.iconSettingRow),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                device.name,
                color = theme.textColor,
                fontSize = type.title,
                fontWeight = FutureTypography.weightMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (device.isBonded || device.connection != Connection.Disconnected) statusOf(device) else "זמין להתאמה",
                color = if (connected) accentOf(theme) else theme.mutedTextColor,
                fontSize = type.summary,
                maxLines = 1,
            )
            if (device.connection == Connection.Connecting) {
                FutureIndeterminateProgressBar(theme = theme, modifier = Modifier.padding(top = 6.dp))
            }
        }
        Icon(
            FutureIcons.KeyboardArrowLeft,
            contentDescription = null,
            tint = theme.chevronColor,
            modifier = Modifier.size(FutureDimens.iconTopBar),
        )
    }
}

/** "הצג עוד" - שאר המותאמים שלא נכנסו לשלושת הראשונים. */
@Composable
private fun ShowMoreCard(hidden: Int, theme: FutureTheme, focusRequester: FocusRequester, onClick: () -> Unit) {
    val type = rememberFutureType()
    FutureFocusCard(theme = theme, onClick = onClick, focusRequester = focusRequester, minHeight = FutureDimens.rowHeightSetting) {
        Text(
            "הצג עוד",
            color = theme.textColor,
            fontSize = type.title,
            fontWeight = FutureTypography.weightMedium,
            modifier = Modifier.weight(1f),
        )
        Text("$hidden", color = theme.mutedTextColor, fontSize = type.summary)
        Icon(
            FutureIcons.KeyboardArrowLeft,
            contentDescription = null,
            tint = theme.chevronColor,
            modifier = Modifier.size(FutureDimens.iconTopBar),
        )
    }
}

/** חיפוש מכשירים: "מחפש" עם פס התקדמות בזמן סריקה, אחרת "חפש מכשירים". */
@Composable
private fun ScanCard(scanning: Boolean, found: Int, theme: FutureTheme, onClick: () -> Unit) {
    val type = rememberFutureType()
    FutureFocusCard(theme = theme, onClick = onClick, minHeight = FutureDimens.rowHeightSetting) {
        Icon(
            if (scanning) Icons.AutoMirrored.Rounded.BluetoothSearching else FutureIcons.Refresh,
            contentDescription = null,
            tint = if (scanning) accentOf(theme) else theme.textColor,
            modifier = Modifier.size(FutureDimens.iconSettingRow),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (scanning) "מחפש" else "חפש מכשירים",
                color = theme.textColor,
                fontSize = type.title,
                fontWeight = FutureTypography.weightMedium,
            )
            if (scanning) {
                FutureIndeterminateProgressBar(theme = theme, modifier = Modifier.padding(top = 6.dp))
            } else if (found > 0) {
                Text("נמצאו $found", color = theme.mutedTextColor, fontSize = type.summary)
            }
        }
    }
}

@Composable
private fun PermissionPrompt(theme: FutureTheme, onRequest: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = FutureDimens.spacingXxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyState(
            icon = FutureIcons.Bluetooth,
            title = "אין הרשאה לבלוטות'",
            subtitle = "לחץ על אישור כדי לאשר",
            textColor = theme.textColor,
            modifier = Modifier.weight(1f),
        )
        FutureButton("אשר", theme, onRequest, focusRequester = focus)
    }
}

// ---------------------------------------------------------------- מסך מכשיר

@Composable
private fun DeviceScreen(
    device: BluetoothDeviceInfo,
    theme: FutureTheme,
    onBack: () -> Unit,
    onMenu: () -> Unit,
    onCalls: () -> Unit,
    onMedia: () -> Unit,
    onRename: () -> Unit,
    onConnect: () -> Unit,
    onForget: () -> Unit,
) {
    val type = rememberFutureType()
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    val connected = device.connection == Connection.Connected

    ScreenScaffold(
        backgroundColor = theme.backgroundColor,
        title = "מכשיר",
        textColor = theme.textColor,
        accentColor = theme.accentColor,
        onBack = onBack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // הגיבור - אייקון גדול בעיגול, שם ומצב. אין לו רכיב במערכת (README של הערכה).
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = FutureDimens.screenPadding, end = FutureDimens.screenPadding, top = FutureDimens.spacingMd, bottom = FutureDimens.spacingXl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
            ) {
                Box(
                    modifier = Modifier
                        .size(HeroCircle)
                        .clip(FutureShapes.pill)
                        .background(theme.textAlpha(8)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        iconOf(device.kind),
                        contentDescription = null,
                        tint = if (connected) accentOf(theme) else theme.subtleTextColor,
                        modifier = Modifier.size(HeroCircle / 2),
                    )
                }
                Text(
                    device.name,
                    color = theme.textColor,
                    fontSize = type.screenTitle,
                    fontWeight = FutureTypography.weightBold,
                    textAlign = TextAlign.Center,
                )
                Text(statusOf(device), color = theme.mutedTextColor, fontSize = type.body)
                if (device.connection == Connection.Connecting) {
                    FutureIndeterminateProgressBar(
                        theme = theme,
                        modifier = Modifier.fillMaxWidth(0.6f).padding(top = 6.dp),
                    )
                }
            }

            if (device.supportsCalls || device.supportsMedia) {
                FutureSectionHeader("שימוש", theme)
            }
            FutureCard(theme = theme) {
                var rows = 0
                if (device.supportsCalls) {
                    FutureSettingItem(
                        title = "שיחות ואודיו",
                        summary = if (device.callsConnected) "מופעל" else "כבוי",
                        icon = FutureIcons.Call,
                        theme = theme,
                        showChevron = false,
                        focusRequester = first,
                        onClick = onCalls,
                        trailing = { FutureSwitch(checked = device.callsConnected, theme = theme) },
                    )
                    rows++
                }
                if (device.supportsMedia) {
                    if (rows > 0) FutureDivider(theme = theme)
                    FutureSettingItem(
                        title = "מדיה",
                        summary = if (device.mediaConnected) "מופעל" else "כבוי",
                        icon = FutureIcons.MusicNote,
                        theme = theme,
                        showChevron = false,
                        focusRequester = if (rows == 0) first else null,
                        onClick = onMedia,
                        trailing = { FutureSwitch(checked = device.mediaConnected, theme = theme) },
                    )
                    rows++
                }
                if (rows > 0) FutureDivider(theme = theme)
                FutureSettingItem(
                    title = "שנה שם",
                    summary = device.name,
                    icon = FutureIcons.Edit,
                    theme = theme,
                    focusRequester = if (rows == 0) first else null,
                    onClick = onRename,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = FutureDimens.screenPadding, end = FutureDimens.screenPadding, top = FutureDimens.spacingXl, bottom = FutureDimens.spacingLg),
                verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
            ) {
                FutureButton(
                    text = if (device.connection == Connection.Disconnected) "התחבר" else "נתק",
                    theme = theme,
                    onClick = onConnect,
                    variant = FutureButtonVariant.Secondary,
                    fillMaxWidth = true,
                )
                FutureButton(
                    text = "שכח מכשיר",
                    theme = theme,
                    onClick = onForget,
                    variant = FutureButtonVariant.Quiet,
                    fillMaxWidth = true,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- תפריט

@Composable
private fun BluetoothMenu(
    theme: FutureTheme,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onRenamePhone: () -> Unit,
    onReceivedFiles: () -> Unit,
    onSettings: () -> Unit,
    onDisconnectAll: () -> Unit,
) {
    fun pick(action: () -> Unit): () -> Unit = { onDismiss(); action() }
    FutureOptionsMenu(theme = theme, onDismissRequest = onDismiss, header = "בלוטות'") {
        FutureMenuRow("רענן", FutureIcons.Refresh, theme, pick(onRefresh))
        FutureMenuRow("שם המכשיר", FutureIcons.Edit, theme, pick(onRenamePhone))
        FutureMenuRow("קבצים שהתקבלו", FutureIcons.Folder, theme, pick(onReceivedFiles))
        FutureMenuRow("הגדרות", FutureIcons.Settings, theme, pick(onSettings))
        FutureMenuRow("נתק הכל", FutureIcons.LinkOff, theme, pick(onDisconnectAll), destructive = true)
    }
}

// ---------------------------------------------------------------- עזרים

/** 80dp - העיגול של אייקון המכשיר במסך המכשיר (160px בערכה). */
private val HeroCircle = 80.dp

/** 40dp - העיגול של אייקון המכשיר בכרטיס. */
private val DeviceCircle = 40.dp

/** כמה מותאמים מוצגים במסך הראשי לפני "הצג עוד". */
private const val PAIRED_ON_ROOT = 3

/** מסלול "כל המותאמים" - לא כתובת MAC חוקית, ולכן לא מתנגש בכתובת של מכשיר. */
private const val ALL_PAIRED = "#all-paired"

/** מפתח הפוקוס של כרטיס "הצג עוד", כדי לחזור אליו מהמסך המלא. */
private const val SHOW_ALL_ROW = "#show-all"

@Composable
private fun accentOf(theme: FutureTheme): Color = LocalFutureAccent.current ?: theme.readableAccentColor

/** אייקון סוג המכשיר - בהדגשה כשהוא מחובר, ב-40% כשלא. */
@Composable
private fun DeviceGlyph(kind: DeviceKind, theme: FutureTheme, lit: Boolean) {
    Icon(
        iconOf(kind),
        contentDescription = null,
        tint = if (lit) accentOf(theme) else theme.subtleTextColor,
        modifier = Modifier.size(FutureDimens.iconSettingRow),
    )
}

private fun iconOf(kind: DeviceKind): ImageVector = when (kind) {
    DeviceKind.Headphones -> FutureIcons.Headphones
    DeviceKind.Speaker -> FutureIcons.Speaker
    DeviceKind.Car -> FutureIcons.DirectionsCar
    DeviceKind.Phone -> FutureIcons.Smartphone
    DeviceKind.Computer -> FutureIcons.LaptopMac
    DeviceKind.Keyboard -> FutureIcons.Keyboard
    DeviceKind.Mouse -> FutureIcons.Mouse
    DeviceKind.Watch -> FutureIcons.Watch
    DeviceKind.Other -> FutureIcons.Bluetooth
}

/** שורת המצב מתחת לשם: "מחובר · 80%", "מחובר", "מתחבר", "מותאם". */
private fun statusOf(device: BluetoothDeviceInfo): String = when (device.connection) {
    Connection.Connecting -> "מתחבר"
    Connection.Connected -> device.battery?.let { "מחובר · $it%" } ?: "מחובר"
    Connection.Disconnected -> "מותאם"
}
