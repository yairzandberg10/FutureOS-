package com.future.bluetooth.ui

import com.future.sharednav.i18n.tr

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.systemui.StatusBarInset

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.future.bluetooth.data.BluetoothController
import com.future.bluetooth.data.BluetoothDeviceInfo
import com.future.bluetooth.data.Connection
import com.future.bluetooth.data.DeviceKind
import com.future.bluetooth.data.ReceivedFile
import com.future.bluetooth.data.ReceivedFiles
import com.future.bluetooth.data.TransferManager
import com.future.bluetooth.data.TransferNav
import com.future.sharednav.components.AnimatedScreenHost
import com.future.sharednav.components.ConfirmDialog
import com.future.sharednav.components.EmptyState
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureButtonVariant
import com.future.sharednav.components.FutureCard
import com.future.sharednav.components.FutureDivider
import com.future.sharednav.components.FutureFocusCard
import com.future.sharednav.components.FutureIndeterminateProgressBar
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureSettingItem
import com.future.sharednav.components.FutureSnackbarHost
import com.future.sharednav.components.FutureSnackbarState
import com.future.sharednav.components.FutureSwitch
import com.future.sharednav.components.InputDialog
import com.future.sharednav.components.MarqueeText
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
import com.future.sharednav.theme.subtleTextColor
import com.future.sharednav.theme.textAlpha

/** מה שמשתנה מחוץ לממשק - הרשאות ומסכי מערכת. MainActivity מממש. */
class BluetoothActions(
    val requestPermission: () -> Unit,
    /** בקשת ההפעלה של המערכת - כש-enable() הישיר נדחה. */
    val requestEnable: () -> Unit,
    /** מסך הבלוטות' של המערכת - למה שהמכשיר לא מאפשר לעשות מכאן. false כשאין מסך כזה. */
    val openSystemSettings: () -> Boolean,
    /** בורר הקבצים (Files) - התוצאה מגיעה ל-TransferManager.onFilesChosen. */
    val pickFiles: () -> Boolean,
    /** דיאלוג הנראות של המערכת ("להפוך את הטלפון לגלוי"). */
    val requestDiscoverable: () -> Unit,
    /** פתיחת קובץ שהתקבל. null כשנפתח, אחרת הודעה למשתמש. */
    val openFile: (java.io.File) -> String?,
    val shareFile: (java.io.File) -> Unit,
    /** מסך "גישה לכל הקבצים" של האפליקציה - בלעדיו רשימת הקבצים שהתקבלו חלקית. */
    val grantFilesAccess: () -> Unit,
    /** מסך הגדרות האפליקציה - לאישור הרשאה שנדחתה לצמיתות (אחרי זה המערכת לא מציגה דיאלוג). */
    val openAppSettings: () -> Unit,
)

private sealed interface Dialog {
    data class Pair(val device: BluetoothDeviceInfo) : Dialog
    data class Forget(val device: BluetoothDeviceInfo) : Dialog
    data object RenamePhone : Dialog
    data class RenameDevice(val device: BluetoothDeviceInfo) : Dialog
    data object DisconnectAll : Dialog
    data class DeleteReceived(val file: ReceivedFile) : Dialog
    data object ClearHistory : Dialog
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
    // BT-01: התפריט נשמר גם אחרי יצירה מחדש של הפעילות.
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<Dialog?>(null) }
    val snackbar = rememberFutureSnackbarState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // מסכי העברת הקבצים - מחסנית מעל המסך הראשי / מסך המכשיר ("|" מפריד).
    var routeStack by rememberSaveable { mutableStateOf("") }
    val routes = if (routeStack.isEmpty()) emptyList() else routeStack.split('|')
    val topRoute = routes.lastOrNull()
    // קוראים את המחסנית העדכנית בכל קריאה, ולא את זו של הקומפוזיציה שבה נוצרה הפונקציה.
    fun current() = if (routeStack.isEmpty()) emptyList() else routeStack.split('|')
    fun push(route: String) { routeStack = (current() + route).joinToString("|") }
    fun pop() { routeStack = current().dropLast(1).joinToString("|") }
    fun replaceTop(route: String) { routeStack = (current().dropLast(1) + route).joinToString("|") }

    // קבצים שהתקבלו: נטענים מחדש בכל כניסה למסכי ההעברה ובכל חזרה לאפליקציה
    // (ייתכן שבינתיים התקבל קובץ, או שאושרה גישה לקבצים).
    var received by remember { mutableStateOf<List<ReceivedFile>?>(null) }
    var filesAccess by remember { mutableStateOf(ReceivedFiles.hasAccess()) }
    var receivedVersion by remember { mutableStateOf(0) }
    var focusedReceived by remember { mutableStateOf<ReceivedFile?>(null) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) receivedVersion++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val inTransfers = topRoute == ROUTE_TRANSFERS || topRoute == ROUTE_RECEIVED
    LaunchedEffect(receivedVersion, inTransfers) {
        if (!inTransfers) return@LaunchedEffect
        filesAccess = ReceivedFiles.hasAccess()
        received = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { ReceivedFiles.load(context) }
    }
    LaunchedEffect(Unit) { TransferManager.loadHistory(context) }
    // תוצאות שמגיעות מאוחר בשידור של המערכת (הותאם, מחובר, נכשל...).
    DisposableEffect(controller) {
        controller.onEvent = { snackbar.show(it) }
        onDispose { controller.onEvent = null }
    }

    val ready = controller.isSupported() && hasPermission
    onOptionsKeyPress { if (ready && dialog == null) menuOpen = !menuOpen }

    // שיתוף מאפליקציה אחרת / תוצאה מבורר הקבצים - פותחים את המסך המתאים.
    val nav = TransferManager.nav
    LaunchedEffect(nav, ready) {
        if (nav == null || !ready) return@LaunchedEffect
        TransferManager.consumeNav()
        when (nav) {
            TransferNav.PickDevice -> if (topRoute != ROUTE_PICK) push(ROUTE_PICK)
            TransferNav.Progress -> if (topRoute == ROUTE_PICK || topRoute == ROUTE_PROGRESS) replaceTop(ROUTE_PROGRESS) else push(ROUTE_PROGRESS)
        }
    }

    /** "שלח קובץ": בורר הקבצים, ואחריו מסך בחירת המכשיר (או ישר ליעד שנקבע). */
    fun sendFile(address: String? = null, name: String? = null) {
        if (TransferManager.transfer?.running == true) {
            snackbar.show(tr("כבר יש שליחה פעילה"))
            return
        }
        TransferManager.setPickTarget(address, name)
        if (!actions.pickFiles()) {
            TransferManager.setPickTarget(null, null)
            snackbar.show(tr("אין אפליקציה לבחירת קבצים"))
        }
    }

    fun toggleVisibility() {
        when {
            !controller.isEnabled -> snackbar.show(tr("הבלוטות' כבוי"))
            controller.isDiscoverable -> snackbar.show(tr("הטלפון כבר גלוי למכשירים אחרים"))
            else -> actions.requestDiscoverable()
        }
    }

    fun openReceived(file: ReceivedFile) {
        actions.openFile(file.file)?.let { snackbar.show(it) }
    }

    val openDevice = openAddress?.let { address -> controller.pairedDevices.firstOrNull { it.address == address } }
    // מכשיר שנשכח (או שהרדיו כבה) - חוזרים לרשימה. BT-01: רק אחרי שהרשימה נטענה,
    // אחרת מכשיר פתוח לפני יצירה מחדש של הפעילות היה נסגר על רשימה ריקה.
    LaunchedEffect(openAddress, openDevice, controller.devicesLoaded) {
        if (controller.devicesLoaded && openAddress != null && openDevice == null) openAddress = null
    }
    BackHandler(enabled = routes.isNotEmpty() || openAddress != null || showAllPaired) {
        when {
            routes.isNotEmpty() -> {
                if (topRoute == ROUTE_PROGRESS) TransferManager.dismissFinished()
                if (topRoute == ROUTE_PICK) TransferManager.pending.clear()
                pop()
            }
            openAddress != null -> openAddress = null
            else -> showAllPaired = false
        }
    }

    fun toggleRadio() {
        val on = !controller.isEnabled
        when {
            controller.setEnabled(on) -> snackbar.show(if (on) tr("מפעיל בלוטות'") else tr("מכבה בלוטות'"))
            on -> actions.requestEnable()
            else -> snackbar.show(tr("לא ניתן לכבות את הבלוטות'"))
        }
    }

    /** סריקה - כשהרדיו כבוי startDiscovery לא עושה כלום, וההודעה "מחפש" הטעתה. */
    fun scan() {
        if (!controller.isEnabled) {
            snackbar.show(tr("הבלוטות' כבוי"))
            return
        }
        snackbar.show(if (controller.startDiscovery()) tr("מחפש מכשירים") else tr("הסריקה לא התחילה"))
    }

    /** פעולה שהמכשיר לא מאפשר מכאן נפתחת במסך של המערכת, עם הסבר קצר. */
    fun orSystem(ok: Boolean) {
        if (!ok) {
            // BT-11: ההודעה מתאימה למה שקרה בפועל - לא "פותח" כשהפתיחה נכשלה.
            if (actions.openSystemSettings()) snackbar.show(tr("פותח את הגדרות הבלוטות' של המערכת"))
            else snackbar.show(tr("אין מסך הגדרות בלוטות' במכשיר"))
        }
    }

    /**
     * NEW-01: הפרוקסי של הפרופיל עוד לא מוכן (למשל אחרי הפעלה מחדש) - מבקשים שוב
     * ומבקשים לנסות שוב, בלי לפתוח את מסך המערכת. true כשעצרנו כאן.
     */
    fun waitForProfiles(ready: Boolean): Boolean {
        if (ready) return false
        controller.refreshState()
        snackbar.show(tr("מתחבר לשירותי הבלוטות', נסה שוב"))
        return true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val baseDepth = when {
            openDevice != null -> if (showAllPaired) 2 else 1
            showAllPaired -> 1
            else -> 0
        }
        AnimatedScreenHost(
            targetState = topRoute ?: openDevice?.address ?: if (showAllPaired) ALL_PAIRED else null,
            depthOf = {
                val routeIndex = routes.indexOf(it)
                when {
                    routeIndex >= 0 -> baseDepth + routeIndex + 1
                    it == null -> 0
                    it == ALL_PAIRED -> 1
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
            if (address == ROUTE_TRANSFERS) {
                TransfersScreen(
                    controller = controller,
                    theme = theme,
                    transfer = TransferManager.transfer,
                    history = TransferManager.history,
                    receivedCount = received?.size,
                    onOpenProgress = { push(ROUTE_PROGRESS) },
                    onSendFile = { sendFile() },
                    onReceived = { push(ROUTE_RECEIVED) },
                    onVisibility = ::toggleVisibility,
                    onResend = { sendFile(it.address, it.deviceName) },
                )
            } else if (address == ROUTE_RECEIVED) {
                ReceivedFilesScreen(
                    theme = theme,
                    files = received,
                    hasAccess = filesAccess,
                    focusPath = focusedReceived?.file?.absolutePath,
                    onGrantAccess = actions.grantFilesAccess,
                    onOpen = ::openReceived,
                    onFocused = { focusedReceived = it },
                )
            } else if (address == ROUTE_PICK) {
                ChooseDeviceScreen(
                    controller = controller,
                    theme = theme,
                    files = TransferManager.pending,
                    onToggleRadio = ::toggleRadio,
                    onScan = ::scan,
                    onPick = { target ->
                        if (TransferManager.pending.isEmpty()) {
                            pop()
                        } else if (TransferManager.start(context, target.address, target.name, TransferManager.pending.toList())) {
                            controller.markUsed(target.address)
                            TransferManager.pending.clear()
                            replaceTop(ROUTE_PROGRESS)
                        } else {
                            snackbar.show(tr("כבר יש שליחה פעילה"))
                        }
                    },
                )
            } else if (address == ROUTE_PROGRESS) {
                TransferProgressScreen(
                    theme = theme,
                    transfer = TransferManager.transfer,
                    onBack = {
                        TransferManager.dismissFinished()
                        pop()
                    },
                    onCancel = { TransferManager.cancel() },
                    onRetry = { if (!TransferManager.retry(context)) snackbar.show(tr("לא ניתן לנסות שוב")) },
                    onSendMore = {
                        val t = TransferManager.transfer
                        TransferManager.dismissFinished()
                        pop()
                        sendFile(t?.address, t?.deviceName)
                    },
                )
            } else if (address == ALL_PAIRED) {
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
                    onOpenAppSettings = actions.openAppSettings,
                    onToggleRadio = ::toggleRadio,
                    onRenamePhone = { dialog = Dialog.RenamePhone },
                    onVisibility = ::toggleVisibility,
                    onTransfers = { push(ROUTE_TRANSFERS) },
                    onOpenDevice = openFromList,
                    onShowAll = { lastOpened = SHOW_ALL_ROW; showAllPaired = true },
                    onScan = ::scan,
                    onPair = { dialog = Dialog.Pair(it) },
                )
            } else {
                DeviceScreen(
                    device = device,
                    theme = theme,
                    onBack = { openAddress = null },
                    onMenu = { menuOpen = true },
                    onCalls = {
                        val on = !device.callsConnected
                        if (!waitForProfiles(controller.profileReady(calls = true))) {
                            val ok = controller.setProfile(device.address, calls = true, enabled = on)
                            if (ok) snackbar.show(if (on) tr("מחבר שיחות · {0}", device.name) else tr("שיחות נותקו · {0}", device.name))
                            orSystem(ok)
                        }
                    },
                    onMedia = {
                        val on = !device.mediaConnected
                        if (!waitForProfiles(controller.profileReady(calls = false))) {
                            val ok = controller.setProfile(device.address, calls = false, enabled = on)
                            if (ok) snackbar.show(if (on) tr("מחבר מדיה · {0}", device.name) else tr("מדיה נותקה · {0}", device.name))
                            orSystem(ok)
                        }
                    },
                    onRename = { dialog = Dialog.RenameDevice(device) },
                    onConnect = {
                        if (device.connection == Connection.Disconnected) {
                            val profilesReady = controller.profileReady(calls = true) || controller.profileReady(calls = false)
                            if (!waitForProfiles(profilesReady)) {
                                val ok = controller.connect(device.address)
                                if (ok) snackbar.show(tr("מתחבר · {0}", device.name))
                                orSystem(ok)
                            }
                        } else if (controller.isPairing(device.address)) {
                            // BT-07: התאמה בתהליך היא לא חיבור - אין מה לנתק.
                            snackbar.show(tr("ההתאמה עדיין בתהליך"))
                        } else {
                            val ok = controller.disconnect(device.address)
                            if (ok) snackbar.show(tr("נותק · {0}", device.name)) else orSystem(false)
                        }
                    },
                    onForget = { dialog = Dialog.Forget(device) },
                    onSendFile = { sendFile(device.address, device.name) },
                )
            }
        }

        FutureSnackbarHost(snackbar, theme)
    }

    if (menuOpen) {
        val items = when (topRoute) {
            ROUTE_TRANSFERS -> listOf(
                MenuItem(tr("שלח קובץ"), FutureIcons.Send) { sendFile() },
                MenuItem(tr("קבצים שהתקבלו"), FutureIcons.Download) { push(ROUTE_RECEIVED) },
                MenuItem(tr("גלוי למכשירים אחרים"), FutureIcons.Visibility) { toggleVisibility() },
            ) + if (TransferManager.history.isNotEmpty()) {
                listOf(MenuItem(tr("נקה היסטוריה"), FutureIcons.Delete, destructive = true) { dialog = Dialog.ClearHistory })
            } else emptyList()
            ROUTE_RECEIVED -> {
                val file = focusedReceived?.takeIf { f -> received?.any { it.file == f.file } == true }
                (if (file != null) listOf(
                    MenuItem(tr("פתח"), FutureIcons.Description) { openReceived(file) },
                    MenuItem(tr("שתף"), FutureIcons.Share) { actions.shareFile(file.file) },
                ) else emptyList()) + listOf(
                    MenuItem(tr("רענן"), FutureIcons.Refresh) { receivedVersion++ },
                ) + (if (file != null) listOf(
                    MenuItem(tr("מחק"), FutureIcons.Delete, destructive = true) { dialog = Dialog.DeleteReceived(file) },
                ) else emptyList())
            }
            ROUTE_PICK -> listOf(
                MenuItem(tr("חפש מכשירים"), FutureIcons.Refresh) { scan() },
            )
            ROUTE_PROGRESS -> if (TransferManager.transfer?.running == true) {
                listOf(MenuItem(tr("בטל שליחה"), FutureIcons.Close, destructive = true) { TransferManager.cancel() })
            } else listOf(MenuItem(tr("שלח קובץ"), FutureIcons.Send) { sendFile() })
            else -> listOf(
                MenuItem(tr("רענן"), FutureIcons.Refresh) {
                    controller.refreshState()
                    scan()
                },
                MenuItem(tr("שלח קובץ"), FutureIcons.Send) { sendFile() },
                MenuItem(tr("העברת קבצים"), FutureIcons.SwapVert) { push(ROUTE_TRANSFERS) },
                MenuItem(tr("קבצים שהתקבלו"), FutureIcons.Folder) { push(ROUTE_RECEIVED) },
                MenuItem(tr("גלוי למכשירים אחרים"), FutureIcons.Visibility) { toggleVisibility() },
                MenuItem(tr("שם המכשיר"), FutureIcons.Edit) { dialog = Dialog.RenamePhone },
                MenuItem(tr("הגדרות"), FutureIcons.Settings) { orSystem(false) },
                // BT-08: ניתוק כל המכשירים מבקש אישור קודם.
                MenuItem(tr("נתק הכל"), FutureIcons.LinkOff, destructive = true) { dialog = Dialog.DisconnectAll },
            )
        }
        BluetoothMenu(
            theme = theme,
            header = when (topRoute) {
                ROUTE_TRANSFERS -> tr("העברת קבצים")
                ROUTE_RECEIVED -> focusedReceived?.name ?: tr("קבצים שהתקבלו")
                ROUTE_PICK -> tr("שלח אל")
                ROUTE_PROGRESS -> tr("שליחת קובץ")
                else -> tr("בלוטות'")
            },
            items = items,
            onDismiss = { menuOpen = false },
        )
    }

    when (val d = dialog) {
        is Dialog.DeleteReceived -> ConfirmDialog(
            message = tr("למחוק את {0}?", d.file.name),
            theme = theme,
            confirmLabel = tr("מחק"),
            onCancel = { dialog = null },
            onConfirm = {
                dialog = null
                if (ReceivedFiles.delete(d.file, context)) {
                    snackbar.show(tr("הקובץ נמחק"))
                    focusedReceived = null
                    receivedVersion++
                } else {
                    snackbar.show(tr("לא ניתן למחוק"))
                }
            },
        )
        Dialog.ClearHistory -> ConfirmDialog(
            message = tr("לנקות את היסטוריית השליחה?"),
            theme = theme,
            confirmLabel = tr("נקה"),
            onCancel = { dialog = null },
            onConfirm = {
                dialog = null
                TransferManager.clearHistory(context)
            },
        )
        Dialog.DisconnectAll -> ConfirmDialog(
            message = tr("לנתק את כל המכשירים המחוברים?"),
            theme = theme,
            confirmLabel = tr("נתק הכל"),
            onCancel = { dialog = null },
            onConfirm = {
                dialog = null
                when (controller.disconnectAll()) {
                    null -> snackbar.show(tr("אין מכשירים מחוברים"))
                    0 -> orSystem(false)
                    else -> snackbar.show(tr("כל המכשירים נותקו"))
                }
            },
        )
        is Dialog.Pair -> ConfirmDialog(
            message = tr("להתאים את {0}?", d.device.name),
            theme = theme,
            confirmLabel = tr("התאם"),
            destructive = false,
            onCancel = { dialog = null },
            onConfirm = {
                dialog = null
                if (controller.pair(d.device.address)) snackbar.show(tr("מתאים · {0}", d.device.name))
                else snackbar.show(tr("ההתאמה לא התחילה"))
            },
        )
        is Dialog.Forget -> ConfirmDialog(
            message = tr("לשכוח את {0}?", d.device.name),
            theme = theme,
            confirmLabel = tr("שכח"),
            onCancel = { dialog = null },
            onConfirm = {
                dialog = null
                if (controller.forget(d.device.address)) {
                    openAddress = null
                    snackbar.show(tr("המכשיר נשכח"))
                } else {
                    orSystem(false)
                }
            },
        )
        Dialog.RenamePhone -> InputDialog(
            title = tr("שם המכשיר"),
            theme = theme,
            initialValue = controller.adapterName,
            onDismiss = { dialog = null },
            onConfirm = { name ->
                dialog = null
                // שם ריק היה נשלח כמו שהוא, והטלפון הופיע בלי שם אצל מכשירים אחרים
                if (name.isBlank()) snackbar.show(tr("השם לא יכול להיות ריק"))
                else if (controller.renameAdapter(name.trim())) snackbar.show(tr("שם המכשיר עודכן"))
                else snackbar.show(tr("השם לא עודכן"))
            },
        )
        is Dialog.RenameDevice -> InputDialog(
            title = tr("שנה שם"),
            theme = theme,
            // בלי שם מהמערכת לא מציעים "מכשיר לא מזוהה" כשם לעריכה
            initialValue = if (d.device.nameKnown) d.device.name else "",
            onDismiss = { dialog = null },
            onConfirm = { name ->
                dialog = null
                if (name.isBlank()) {
                    snackbar.show(tr("השם לא יכול להיות ריק"))
                } else {
                    controller.renameDevice(d.device.address, name.trim())
                    snackbar.show(tr("השם עודכן"))
                }
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
    onOpenAppSettings: () -> Unit,
    onToggleRadio: () -> Unit,
    onRenamePhone: () -> Unit,
    onVisibility: () -> Unit,
    onTransfers: () -> Unit,
    onOpenDevice: (BluetoothDeviceInfo) -> Unit,
    onShowAll: () -> Unit,
    onScan: () -> Unit,
    onPair: (BluetoothDeviceInfo) -> Unit,
) {
    ScreenScaffold(
        modifier = Modifier.padding(top = StatusBarInset.TITLE_GAP_DP.dp),
        backgroundColor = theme.backgroundColor,
        title = tr("בלוטות'"),
        textColor = theme.textColor,
        accentColor = theme.accentColor,
    ) {
        when {
            !controller.isSupported() -> EmptyState(
                icon = FutureIcons.BluetoothDisabled,
                title = tr("אין בלוטות' במכשיר הזה"),
                textColor = theme.textColor,
            )
            !hasPermission -> PermissionPrompt(theme, onRequestPermission, onOpenAppSettings)
            else -> DeviceLists(controller, theme, focusAddress, onToggleRadio, onRenamePhone, onVisibility, onTransfers, onOpenDevice, onShowAll, onScan, onPair)
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
    onVisibility: () -> Unit,
    onTransfers: () -> Unit,
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
        if (on && !controller.autoScanDone && !controller.isScanning && nearby.isEmpty()) {
            controller.autoScanDone = true
            controller.startDiscovery()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = FutureDimens.spacingXl),
    ) {
        FutureCard(theme = theme) {
            FutureSettingItem(
                title = tr("בלוטות'"),
                // BT-25: בזמן ההדלקה (שנייה-שתיים) מראים שהבלוטות' בדרך.
                summary = when {
                    controller.isTurningOn -> tr("מופעל...")
                    on -> tr("מופעל")
                    else -> tr("כבוי")
                },
                icon = FutureIcons.Bluetooth,
                theme = theme,
                showChevron = false,
                focusRequester = radioFocus,
                onClick = onToggleRadio,
                trailing = { FutureSwitch(checked = on, theme = theme) },
            )
            FutureDivider(theme = theme)
            FutureSettingItem(
                title = tr("שם המכשיר"),
                summary = controller.adapterName.ifEmpty { null },
                icon = FutureIcons.Badge,
                theme = theme,
                onClick = onRenamePhone,
            )
            if (on) {
                FutureDivider(theme = theme)
                FutureSettingItem(
                    title = tr("גלוי למכשירים אחרים"),
                    summary = visibilitySummary(controller),
                    icon = if (controller.isDiscoverable) FutureIcons.Visibility else FutureIcons.VisibilityOff,
                    theme = theme,
                    showChevron = false,
                    onClick = onVisibility,
                )
            }
            FutureDivider(theme = theme)
            val transfer = TransferManager.transfer
            FutureSettingItem(
                title = tr("העברת קבצים"),
                summary = if (transfer?.running == true) {
                    statusLabel(transfer) + " · " + (transfer.fraction * 100).toInt() + "%"
                } else {
                    tr("שליחה וקבלה של קבצים")
                },
                icon = FutureIcons.SwapVert,
                theme = theme,
                onClick = onTransfers,
            )
        }

        if (!on) {
            EmptyState(
                icon = FutureIcons.BluetoothDisabled,
                title = tr("בלוטות' כבוי"),
                subtitle = tr("לחץ על אישור כדי להפעיל ולחפש מכשירים"),
                textColor = theme.textColor,
            )
            return@Column
        }

        FutureSectionHeader(tr("מכשירים מותאמים"), theme)
        if (paired.isEmpty()) {
            EmptyState(
                icon = FutureIcons.Devices,
                title = tr("אין מכשירים מותאמים"),
                subtitle = tr("בחר מכשיר מהרשימה שלמטה כדי להתאים"),
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

        FutureSectionHeader(tr("מכשירים זמינים"), theme)
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
                icon = FutureIcons.Bluetooth,
                title = tr("לא נמצאו מכשירים"),
                subtitle = tr("לחץ על חפש מכשירים כדי לחפש שוב"),
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
        modifier = Modifier.padding(top = StatusBarInset.TITLE_GAP_DP.dp),
        backgroundColor = theme.backgroundColor,
        title = tr("מכשירים מותאמים"),
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
internal fun CardStack(content: @Composable () -> Unit) {
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
internal fun DeviceCard(
    device: BluetoothDeviceInfo,
    theme: FutureTheme,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    val type = rememberFutureType()
    val connected = device.connection == Connection.Connected
    FutureFocusCard(theme = theme, onClick = onClick, focusRequester = focusRequester) { focused ->
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
            // BT-22: שם ארוך בכרטיס הממוקד גולל (MarqueeText), ובשאר נחתך בשלוש נקודות.
            MarqueeText(
                text = device.name,
                color = theme.textColor,
                isFocused = focused,
                style = TextStyle(fontSize = type.title, fontWeight = FutureTypography.weightMedium),
            )
            // BT-23: בלי שם מהמערכת שורת המשנה היא הכתובת, כי השם הראשי הוא "מכשיר לא מזוהה".
            Text(
                when {
                    !device.nameKnown -> device.address
                    device.isBonded || device.connection != Connection.Disconnected -> statusOf(device)
                    else -> tr("זמין להתאמה")
                },
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
            tr("הצג עוד"),
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
internal fun ScanCard(
    scanning: Boolean,
    found: Int,
    theme: FutureTheme,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    val type = rememberFutureType()
    FutureFocusCard(theme = theme, onClick = onClick, focusRequester = focusRequester, minHeight = FutureDimens.rowHeightSetting) {
        Icon(
            if (scanning) FutureIcons.Bluetooth else FutureIcons.Refresh,
            contentDescription = null,
            tint = if (scanning) accentOf(theme) else theme.textColor,
            modifier = Modifier.size(FutureDimens.iconSettingRow),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (scanning) tr("מחפש") else tr("חפש מכשירים"),
                color = theme.textColor,
                fontSize = type.title,
                fontWeight = FutureTypography.weightMedium,
            )
            if (scanning) {
                FutureIndeterminateProgressBar(theme = theme, modifier = Modifier.padding(top = 6.dp))
            } else if (found > 0) {
                Text(tr("נמצאו {0}", found), color = theme.mutedTextColor, fontSize = type.summary)
            }
        }
    }
}

@Composable
private fun PermissionPrompt(theme: FutureTheme, onRequest: () -> Unit, onOpenAppSettings: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = FutureDimens.spacingXxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyState(
            icon = FutureIcons.Bluetooth,
            title = tr("אין הרשאה לבלוטות'"),
            subtitle = tr("לחץ על אישור כדי לאשר. אם ההרשאה נדחתה לצמיתות - פתח את הגדרות האפליקציה"),
            textColor = theme.textColor,
            modifier = Modifier.weight(1f),
        )
        // "הגדרות" תמיד זמין כאן: אחרי דחייה לצמיתות אישור לא מציג דיאלוג, והמשתמש צריך דרך לתקן.
        FutureButton(tr("אשר"), theme, onRequest, focusRequester = focus)
        Spacer(modifier = Modifier.height(FutureDimens.spacingSm))
        FutureButton(tr("הגדרות האפליקציה"), theme, onOpenAppSettings, variant = FutureButtonVariant.Secondary)
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
    onSendFile: () -> Unit,
) {
    val type = rememberFutureType()
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    val connected = device.connection == Connection.Connected

    ScreenScaffold(
        modifier = Modifier.padding(top = StatusBarInset.TITLE_GAP_DP.dp),
        backgroundColor = theme.backgroundColor,
        title = tr("מכשיר"),
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
                // BT-21: שם ארוך לא דוחף את הכפתורים למטה.
                Text(
                    device.name,
                    color = theme.textColor,
                    fontSize = type.screenTitle,
                    fontWeight = FutureTypography.weightBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(statusOf(device), color = theme.mutedTextColor, fontSize = type.body)
                if (device.connection == Connection.Connecting) {
                    FutureIndeterminateProgressBar(
                        theme = theme,
                        modifier = Modifier.fillMaxWidth(0.6f).padding(top = 6.dp),
                    )
                }
            }

            if (device.supportsCalls || device.supportsMedia || device.supportsFiles) {
                FutureSectionHeader(tr("שימוש"), theme)
            }
            FutureCard(theme = theme) {
                var rows = 0
                if (device.supportsCalls) {
                    FutureSettingItem(
                        title = tr("שיחות ואודיו"),
                        summary = if (device.callsConnected) tr("מופעל") else tr("כבוי"),
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
                        title = tr("מדיה"),
                        summary = if (device.mediaConnected) tr("מופעל") else tr("כבוי"),
                        icon = FutureIcons.MusicNote,
                        theme = theme,
                        showChevron = false,
                        focusRequester = if (rows == 0) first else null,
                        onClick = onMedia,
                        trailing = { FutureSwitch(checked = device.mediaConnected, theme = theme) },
                    )
                    rows++
                }
                if (device.supportsFiles) {
                    if (rows > 0) FutureDivider(theme = theme)
                    FutureSettingItem(
                        title = tr("שלח קובץ"),
                        summary = tr("שליחת קובץ אל {0}", device.name),
                        icon = FutureIcons.Send,
                        theme = theme,
                        focusRequester = if (rows == 0) first else null,
                        onClick = onSendFile,
                    )
                    rows++
                }
                if (rows > 0) FutureDivider(theme = theme)
                FutureSettingItem(
                    title = tr("שנה שם"),
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
                    text = if (device.connection == Connection.Disconnected) tr("התחבר") else tr("נתק"),
                    theme = theme,
                    onClick = onConnect,
                    variant = FutureButtonVariant.Secondary,
                    fillMaxWidth = true,
                )
                // BT-39: פעולה הרסנית באותו סגנון כמו "נתק הכל" בתפריט.
                FutureButton(
                    text = tr("שכח מכשיר"),
                    theme = theme,
                    onClick = onForget,
                    variant = FutureButtonVariant.Destructive,
                    fillMaxWidth = true,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- תפריט

private class MenuItem(
    val label: String,
    val icon: ImageVector,
    val destructive: Boolean = false,
    val action: () -> Unit,
)

/** תפריט האפשרויות - השורות תלויות במסך הפתוח. */
@Composable
private fun BluetoothMenu(
    theme: FutureTheme,
    header: String,
    items: List<MenuItem>,
    onDismiss: () -> Unit,
) {
    FutureOptionsMenu(theme = theme, onDismissRequest = onDismiss, header = header) {
        items.forEach { item ->
            FutureMenuRow(item.label, item.icon, theme, { onDismiss(); item.action() }, destructive = item.destructive)
        }
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

/** מסכי העברת הקבצים במחסנית - מחרוזות שאינן כתובת MAC. */
private const val ROUTE_TRANSFERS = "#transfers"
private const val ROUTE_RECEIVED = "#received"
private const val ROUTE_PICK = "#pick"
private const val ROUTE_PROGRESS = "#progress"

/** מפתח הפוקוס של כרטיס "הצג עוד", כדי לחזור אליו מהמסך המלא. */
private const val SHOW_ALL_ROW = "#show-all"

@Composable
internal fun accentOf(theme: FutureTheme): Color = LocalFutureAccent.current ?: theme.readableAccentColor

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
    Connection.Connecting -> tr("מתחבר")
    Connection.Connected -> device.battery?.let { tr("מחובר · {0}%", it) } ?: tr("מחובר")
    Connection.Disconnected -> tr("מותאם")
}
