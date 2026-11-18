package com.future.bluetooth.ui

import com.future.sharednav.i18n.tr

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.future.bluetooth.data.BluetoothController
import com.future.bluetooth.data.BluetoothDeviceInfo
import com.future.bluetooth.data.OutgoingFile
import com.future.bluetooth.data.ReceivedFile
import com.future.bluetooth.data.Transfer
import com.future.bluetooth.data.TransferRecord
import com.future.bluetooth.data.TransferStatus
import com.future.sharednav.components.EmptyState
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureButtonVariant
import com.future.sharednav.components.FutureCard
import com.future.sharednav.components.FutureDivider
import com.future.sharednav.components.FutureFocusCard
import com.future.sharednav.components.FutureIndeterminateProgressBar
import com.future.sharednav.components.FutureProgressBar
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureSettingItem
import com.future.sharednav.components.MarqueeText
import com.future.sharednav.components.ScreenScaffold
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.systemui.StatusBarInset
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.avatarFillColor
import com.future.sharednav.theme.chevronColor
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.rememberFutureType
import com.future.sharednav.theme.subtleTextColor
import com.future.sharednav.theme.textAlpha
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------- העברת קבצים (מרכז)

/**
 * "העברת קבצים": השליחה הפעילה (אם יש), שלח קובץ, קבצים שהתקבלו, נראות
 * (כדי שמכשיר אחר ימצא את הטלפון וישלח אליו), והשליחות האחרונות.
 */
@Composable
internal fun TransfersScreen(
    controller: BluetoothController,
    theme: FutureTheme,
    transfer: Transfer?,
    history: List<TransferRecord>,
    receivedCount: Int?,
    onOpenProgress: () -> Unit,
    onSendFile: () -> Unit,
    onReceived: () -> Unit,
    onVisibility: () -> Unit,
    onResend: (TransferRecord) -> Unit,
) {
    val type = rememberFutureType()
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    ScreenScaffold(
        modifier = Modifier.padding(top = StatusBarInset.TITLE_GAP_DP.dp),
        backgroundColor = theme.backgroundColor,
        title = tr("העברת קבצים"),
        textColor = theme.textColor,
        accentColor = theme.accentColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = FutureDimens.spacingXl),
        ) {
            if (transfer != null) {
                FutureSectionHeader(if (transfer.running) tr("שליחה פעילה") else tr("שליחה אחרונה"), theme)
                CardStack {
                    ActiveTransferCard(transfer, theme, onOpenProgress, focusRequester = first)
                }
            }

            FutureSectionHeader(tr("פעולות"), theme)
            FutureCard(theme = theme) {
                FutureSettingItem(
                    title = tr("שלח קובץ"),
                    summary = tr("בחר קובץ ומכשיר"),
                    icon = FutureIcons.Send,
                    theme = theme,
                    focusRequester = if (transfer == null) first else null,
                    onClick = onSendFile,
                )
                FutureDivider(theme = theme)
                FutureSettingItem(
                    title = tr("קבצים שהתקבלו"),
                    summary = receivedCount?.let { if (it == 0) tr("אין קבצים") else tr("{0} קבצים", it) },
                    icon = FutureIcons.Download,
                    theme = theme,
                    onClick = onReceived,
                )
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
            Text(
                tr("כדי לקבל קובץ: הפעל נראות, ושלח מהמכשיר השני אל {0}. אישור הקבלה מופיע בהתראה.", controller.adapterName.ifEmpty { tr("הטלפון") }),
                color = theme.mutedTextColor,
                fontSize = type.summary,
                modifier = Modifier.padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
            )

            if (history.isNotEmpty()) {
                FutureSectionHeader(tr("נשלחו לאחרונה"), theme)
                FutureCard(theme = theme) {
                    history.forEachIndexed { i, record ->
                        if (i > 0) FutureDivider(theme = theme)
                        FutureSettingItem(
                            title = record.title,
                            summary = tr("אל {0}", record.deviceName) + " · " + recordLabel(record.status) + " · " + shortTime(record.time),
                            icon = statusIcon(record.status),
                            theme = theme,
                            onClick = { onResend(record) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * השליחה הפעילה (או האחרונה, עד שסוגרים אותה) ככרטיס: שם הקובץ, היעד, המצב
 * ופס ההתקדמות. אישור פותח את מסך השליחה - כך חוזרים אליו אחרי "הסתר".
 */
@Composable
internal fun ActiveTransferCard(
    transfer: Transfer,
    theme: FutureTheme,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    val type = rememberFutureType()
    FutureFocusCard(theme = theme, onClick = onClick, focusRequester = focusRequester) { focused ->
        FileGlyph(statusIcon(transfer.status), theme, highlighted = transfer.running)
        Column(modifier = Modifier.weight(1f)) {
            MarqueeText(
                text = transfer.current.name,
                color = theme.textColor,
                isFocused = focused,
                style = TextStyle(fontSize = type.title, fontWeight = FutureTypography.weightMedium),
            )
            Text(
                tr("אל {0}", transfer.deviceName) + " · " + statusLabel(transfer),
                color = theme.mutedTextColor,
                fontSize = type.summary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TransferBar(transfer, theme, Modifier.padding(top = 6.dp))
        }
        Chevron(theme)
    }
}

// ---------------------------------------------------------------- בחירת מכשיר

/** "שלח אל" - המכשיר שיקבל את הקבצים שממתינים. */
@Composable
internal fun ChooseDeviceScreen(
    controller: BluetoothController,
    theme: FutureTheme,
    files: List<OutgoingFile>,
    onToggleRadio: () -> Unit,
    onScan: () -> Unit,
    onPick: (BluetoothDeviceInfo) -> Unit,
) {
    val type = rememberFutureType()
    val first = remember { FocusRequester() }
    val paired = controller.pairedByRelevance()
    val nearby = controller.discoveredDevices
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    ScreenScaffold(
        modifier = Modifier.padding(top = StatusBarInset.TITLE_GAP_DP.dp),
        backgroundColor = theme.backgroundColor,
        title = tr("שלח אל"),
        textColor = theme.textColor,
        accentColor = theme.accentColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = FutureDimens.spacingXl),
        ) {
            // מה נשלח - שם הקובץ (או הראשון ומספר הנוספים) והגודל הכולל.
            if (files.isNotEmpty()) {
                val context = LocalContext.current
                val total = files.sumOf { it.size.coerceAtLeast(0) }
                Text(
                    (if (files.size == 1) files.first().name else tr("{0} ועוד {1}", files.first().name, files.size - 1)) +
                        " · " + Formatter.formatShortFileSize(context, total),
                    color = theme.mutedTextColor,
                    fontSize = type.summary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
                )
            }
            if (!controller.isEnabled) {
                EmptyState(
                    icon = FutureIcons.BluetoothDisabled,
                    title = tr("בלוטות' כבוי"),
                    subtitle = tr("הפעל כדי לבחור מכשיר"),
                    textColor = theme.textColor,
                )
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    FutureButton(tr("הפעל"), theme, onToggleRadio, focusRequester = first)
                }
                return@Column
            }
            FutureSectionHeader(tr("מכשירים מותאמים"), theme)
            if (paired.isEmpty()) {
                Text(
                    tr("אין מכשירים מותאמים"),
                    color = theme.mutedTextColor,
                    fontSize = type.summary,
                    modifier = Modifier.padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
                )
            } else {
                CardStack {
                    paired.forEachIndexed { i, device ->
                        DeviceCard(
                            device = device,
                            theme = theme,
                            focusRequester = if (i == 0) first else null,
                            onClick = { onPick(device) },
                        )
                    }
                }
            }
            FutureSectionHeader(tr("מכשירים זמינים"), theme)
            CardStack {
                ScanCard(
                    scanning = controller.isScanning,
                    found = nearby.size,
                    theme = theme,
                    focusRequester = if (paired.isEmpty()) first else null,
                    onClick = onScan,
                )
                nearby.forEach { device ->
                    DeviceCard(device = device, theme = theme, onClick = { onPick(device) })
                }
            }
        }
    }
}

// ---------------------------------------------------------------- התקדמות

/** מסך השליחה: הקובץ, היעד, פס ההתקדמות והמצב, וביטול / סגירה / ניסיון חוזר. */
@Composable
internal fun TransferProgressScreen(
    theme: FutureTheme,
    transfer: Transfer?,
    onBack: () -> Unit,
    /** יציאה מהמסך בלי לעצור את השליחה - חוזרים מכרטיס "שליחה פעילה". */
    onHide: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onSendMore: () -> Unit,
) {
    val type = rememberFutureType()
    val context = LocalContext.current
    val primary = remember { FocusRequester() }
    val running = transfer?.running == true
    // הפוקוס עובר לכפתור הראשי כשהמצב משתנה (בטל -> סגור).
    LaunchedEffect(running, transfer == null) { runCatching { primary.requestFocus() } }

    ScreenScaffold(
        modifier = Modifier.padding(top = StatusBarInset.TITLE_GAP_DP.dp),
        backgroundColor = theme.backgroundColor,
        title = tr("שליחת קובץ"),
        textColor = theme.textColor,
        accentColor = theme.accentColor,
        onBack = onBack,
    ) {
        if (transfer == null) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                EmptyState(
                    icon = FutureIcons.Send,
                    title = tr("אין שליחה פעילה"),
                    textColor = theme.textColor,
                    modifier = Modifier.weight(1f),
                )
                FutureButton(tr("סגור"), theme, onBack, focusRequester = primary)
            }
            return@ScreenScaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = FutureDimens.screenPadding, end = FutureDimens.screenPadding, top = FutureDimens.spacingMd, bottom = FutureDimens.spacingLg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
            ) {
                Box(
                    modifier = Modifier
                        .size(HeroSize)
                        .clip(FutureShapes.pill)
                        .background(theme.textAlpha(8)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        statusIcon(transfer.status),
                        contentDescription = null,
                        tint = when (transfer.status) {
                            TransferStatus.Failed, TransferStatus.Rejected -> theme.dangerColor
                            TransferStatus.Cancelled -> theme.subtleTextColor
                            else -> accentOf(theme)
                        },
                        modifier = Modifier.size(HeroSize / 2),
                    )
                }
                Text(
                    transfer.current.name,
                    color = theme.textColor,
                    fontSize = type.screenTitle,
                    fontWeight = FutureTypography.weightBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(tr("אל {0}", transfer.deviceName), color = theme.mutedTextColor, fontSize = type.body, maxLines = 1)
                Text(
                    statusLabel(transfer),
                    color = when (transfer.status) {
                        TransferStatus.Failed, TransferStatus.Rejected -> theme.dangerColor
                        TransferStatus.Done -> accentOf(theme)
                        else -> theme.textColor
                    },
                    fontSize = type.title,
                    fontWeight = FutureTypography.weightMedium,
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FutureDimens.screenPadding),
                verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
            ) {
                TransferBar(transfer, theme, Modifier.fillMaxWidth())
                val sizes = if (transfer.totalBytes > 0) {
                    tr(
                        "{0} מתוך {1} · {2}%",
                        Formatter.formatShortFileSize(context, transfer.sentBytes),
                        Formatter.formatShortFileSize(context, transfer.totalBytes),
                        (transfer.fraction * 100).toInt(),
                    )
                } else ""
                val count = if (transfer.files.size > 1) {
                    tr("קובץ {0} מתוך {1}", (transfer.index + 1).coerceAtMost(transfer.files.size), transfer.files.size)
                } else ""
                Text(
                    listOf(count, sizes).filter { it.isNotEmpty() }.joinToString(" · "),
                    color = theme.mutedTextColor,
                    fontSize = type.summary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = FutureDimens.screenPadding, end = FutureDimens.screenPadding, top = FutureDimens.spacingXl, bottom = FutureDimens.spacingLg),
                verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
            ) {
                when {
                    running -> {
                        // "הסתר" ראשון וממוקד: לחיצה בטעות על אישור לא מבטלת את השליחה.
                        FutureButton(tr("הסתר"), theme, onHide, fillMaxWidth = true, focusRequester = primary)
                        FutureButton(
                            text = tr("בטל שליחה"),
                            theme = theme,
                            onClick = onCancel,
                            variant = FutureButtonVariant.Destructive,
                            fillMaxWidth = true,
                        )
                        Text(
                            tr("השליחה תמשיך גם אחרי הסתרה. חוזרים אליה מהמסך הראשי של הבלוטות'."),
                            color = theme.mutedTextColor,
                            fontSize = type.summary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    transfer.status == TransferStatus.Done -> {
                        FutureButton(tr("סגור"), theme, onBack, fillMaxWidth = true, focusRequester = primary)
                        FutureButton(tr("שלח קובץ נוסף"), theme, onSendMore, variant = FutureButtonVariant.Secondary, fillMaxWidth = true)
                    }
                    else -> {
                        FutureButton(tr("נסה שוב"), theme, onRetry, fillMaxWidth = true, focusRequester = primary)
                        FutureButton(tr("סגור"), theme, onBack, variant = FutureButtonVariant.Secondary, fillMaxWidth = true)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- קבצים שהתקבלו

/**
 * הקבצים שהתקבלו בבלוטות', החדשים קודם. אישור פותח; מקש התפריט - פתח / שתף /
 * מחק לקובץ הממוקד. בלי "גישה לכל הקבצים" הרשימה חלקית, ומוצגת בקשה לאשר.
 */
@Composable
internal fun ReceivedFilesScreen(
    theme: FutureTheme,
    files: List<ReceivedFile>?,
    hasAccess: Boolean,
    focusPath: String?,
    onGrantAccess: () -> Unit,
    onOpen: (ReceivedFile) -> Unit,
    onFocused: (ReceivedFile) -> Unit,
) {
    val type = rememberFutureType()
    val context = LocalContext.current
    val rowFocus = remember { mutableMapOf<String, FocusRequester>() }
    fun focusFor(path: String) = rowFocus.getOrPut(path) { FocusRequester() }
    val grantFocus = remember { FocusRequester() }
    LaunchedEffect(files == null) {
        if (files == null) return@LaunchedEffect
        val target = files.firstOrNull { it.file.absolutePath == focusPath } ?: files.firstOrNull()
        runCatching {
            if (target != null) focusFor(target.file.absolutePath).requestFocus()
            else if (!hasAccess) grantFocus.requestFocus()
        }
    }
    ScreenScaffold(
        modifier = Modifier.padding(top = StatusBarInset.TITLE_GAP_DP.dp),
        backgroundColor = theme.backgroundColor,
        title = tr("קבצים שהתקבלו"),
        textColor = theme.textColor,
        accentColor = theme.accentColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = FutureDimens.spacingXl),
        ) {
            if (!hasAccess) {
                FutureCard(theme = theme) {
                    FutureSettingItem(
                        title = tr("אפשר גישה לקבצים"),
                        summary = tr("כדי להציג את כל הקבצים שהתקבלו"),
                        icon = FutureIcons.Folder,
                        theme = theme,
                        focusRequester = grantFocus,
                        onClick = onGrantAccess,
                    )
                }
            }
            when {
                files == null -> Box(Modifier.fillMaxWidth().padding(FutureDimens.spacingXl)) {
                    FutureIndeterminateProgressBar(theme = theme, modifier = Modifier.fillMaxWidth())
                }
                files.isEmpty() -> EmptyState(
                    icon = FutureIcons.Download,
                    title = tr("אין קבצים שהתקבלו"),
                    subtitle = tr("קבצים שיישלחו אליך בבלוטות' יופיעו כאן"),
                    textColor = theme.textColor,
                )
                else -> {
                    FutureSectionHeader(tr("{0} קבצים", files.size), theme)
                    CardStack {
                        files.forEach { file ->
                            FutureFocusCard(
                                theme = theme,
                                onClick = { onOpen(file) },
                                focusRequester = focusFor(file.file.absolutePath),
                                modifier = Modifier.onFocusChanged { if (it.isFocused) onFocused(file) },
                            ) { focused ->
                                FileGlyph(iconForName(file.name), theme)
                                Column(modifier = Modifier.weight(1f)) {
                                    MarqueeText(
                                        text = file.name,
                                        color = theme.textColor,
                                        isFocused = focused,
                                        style = TextStyle(fontSize = type.title, fontWeight = FutureTypography.weightMedium),
                                    )
                                    Text(
                                        Formatter.formatShortFileSize(context, file.size) + " · " + shortTime(file.modified),
                                        color = theme.mutedTextColor,
                                        fontSize = type.summary,
                                        maxLines = 1,
                                    )
                                }
                                Chevron(theme)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- עזרים

private val HeroSize = 80.dp

@Composable
private fun FileGlyph(icon: ImageVector, theme: FutureTheme, highlighted: Boolean = false) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(FutureShapes.pill)
            .background(if (highlighted) accentOf(theme).copy(alpha = 0.18f) else theme.avatarFillColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (highlighted) accentOf(theme) else theme.textColor,
            modifier = Modifier.size(FutureDimens.iconSettingRow),
        )
    }
}

@Composable
private fun Chevron(theme: FutureTheme) {
    Icon(
        FutureIcons.KeyboardArrowLeft,
        contentDescription = null,
        tint = theme.chevronColor,
        modifier = Modifier.size(FutureDimens.iconTopBar),
    )
}

/** פס ההתקדמות: לא מוגדר בזמן חיבור/המתנה לאישור, ואחוזים בזמן שליחה. */
@Composable
private fun TransferBar(transfer: Transfer, theme: FutureTheme, modifier: Modifier) {
    when (transfer.status) {
        TransferStatus.Connecting, TransferStatus.WaitingForAccept -> FutureIndeterminateProgressBar(theme = theme, modifier = modifier)
        TransferStatus.Done -> FutureProgressBar(progress = 1f, theme = theme, modifier = modifier)
        else -> FutureProgressBar(progress = transfer.fraction, theme = theme, modifier = modifier)
    }
}

/** "גלוי · 1:45", "גלוי", "מוסתר", או "בלוטות' כבוי". מתעדכן כל שנייה בזמן הנראות. */
@Composable
internal fun visibilitySummary(controller: BluetoothController): String {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val until = controller.discoverableUntil
    LaunchedEffect(until, controller.isDiscoverable) {
        while (controller.isDiscoverable && until > System.currentTimeMillis()) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
        now = System.currentTimeMillis()
    }
    return when {
        !controller.isEnabled -> tr("בלוטות' כבוי")
        !controller.isDiscoverable -> tr("מוסתר")
        until > now -> {
            val left = ((until - now) / 1000).toInt()
            tr("גלוי · {0}", "%d:%02d".format(left / 60, left % 60))
        }
        else -> tr("גלוי")
    }
}

internal fun statusLabel(t: Transfer): String = when (t.status) {
    TransferStatus.Connecting -> tr("מתחבר...")
    TransferStatus.WaitingForAccept -> tr("ממתין לאישור במכשיר השני")
    TransferStatus.Sending -> tr("שולח")
    TransferStatus.Done -> if (t.files.size == 1) tr("נשלח") else tr("נשלחו {0} קבצים", t.files.size)
    TransferStatus.Rejected -> tr("המכשיר השני דחה את הקובץ")
    TransferStatus.Failed -> if (t.doneCount > 0) tr("נשלחו {0} מתוך {1}, השאר נכשלו", t.doneCount, t.files.size) else tr("השליחה נכשלה")
    TransferStatus.Cancelled -> tr("השליחה בוטלה")
}

private fun recordLabel(status: TransferStatus): String = when (status) {
    TransferStatus.Done -> tr("נשלח")
    TransferStatus.Rejected -> tr("נדחה")
    TransferStatus.Cancelled -> tr("בוטל")
    else -> tr("נכשל")
}

private fun statusIcon(status: TransferStatus): ImageVector = when (status) {
    TransferStatus.Done -> FutureIcons.DownloadDone
    TransferStatus.Failed, TransferStatus.Rejected -> FutureIcons.Error
    TransferStatus.Cancelled -> FutureIcons.Cancel
    else -> FutureIcons.Send
}

private fun iconForName(name: String): ImageVector = when (name.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
    "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic" -> FutureIcons.Image
    "mp3", "m4a", "aac", "wav", "ogg", "amr", "flac" -> FutureIcons.MusicNote
    "mp4", "3gp", "mkv", "webm", "avi", "mov" -> FutureIcons.Movie
    "vcf" -> FutureIcons.Contacts
    "apk" -> FutureIcons.Android
    else -> FutureIcons.Description
}

private fun shortTime(millis: Long): String =
    SimpleDateFormat("d.M · HH:mm", Locale.getDefault()).format(Date(millis))
