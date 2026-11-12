package com.future.settings.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.future.settings.ui.components.SettingDivider
import com.future.settings.ui.components.SettingHeader
import com.future.settings.ui.components.SettingItem
import com.future.settings.ui.components.SettingSwitch
import com.future.settings.ui.components.SettingsCard
import com.future.settings.ui.theme.ThemeConfig
import com.future.settings.update.UpdatePolicy
import com.future.settings.update.UpdateRepository
import com.future.settings.update.UpdateRepository.CheckResult
import com.future.settings.update.UpdateRepository.Progress
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureSpinner
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTypography
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * "עדכוני תוכנה": בדיקה מול שרת FutureOS, רשימת עדכונים לאפליקציות, והתקנה בלחיצה.
 * כל עדכון עובר אימות (חתימת שרת, hash, חבילה, גרסה, חתימת APK) לפני שהוא מגיע למסך האישור של אנדרואיד.
 */
@Composable
fun UpdateScreen(navController: NavController, theme: ThemeConfig) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val progress by UpdateRepository.progress.collectAsState()
    var offers by remember { mutableStateOf(UpdateRepository.offers(context)) }
    var checking by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<CheckResult?>(null) }
    var canInstall by remember { mutableStateOf(UpdateRepository.canInstall(context)) }
    var autoCheck by remember { mutableStateOf(UpdateRepository.autoCheckEnabled(context)) }
    var lastCheck by remember { mutableStateOf(UpdateRepository.lastCheck(context)) }
    var refresh by remember { mutableIntStateOf(0) }
    val configured = remember { UpdateRepository.isConfigured() }
    val checkFocus = remember { FocusRequester() }

    fun runCheck() {
        if (checking) return
        checking = true
        scope.launch {
            lastResult = UpdateRepository.check(context)
            offers = UpdateRepository.offers(context)
            lastCheck = UpdateRepository.lastCheck(context)
            checking = false
        }
    }

    // חזרה מהגדרת "התקנת אפליקציות לא מוכרות" או ממסך האישור של אנדרואיד: מרעננים
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            canInstall = UpdateRepository.canInstall(context)
            UpdateRepository.reconcilePending(context)
            offers = UpdateRepository.offers(context)
            refresh++
        }
    }
    // כניסה למסך = בדיקה, אם הבדיקה האחרונה ישנה מ-10 דקות
    LaunchedEffect(Unit) {
        runCatching { checkFocus.requestFocus() }
        if (configured && System.currentTimeMillis() - lastCheck > 10 * 60 * 1000L) runCheck()
    }
    LaunchedEffect(progress) { offers = UpdateRepository.offers(context) }

    val installed = remember(refresh) {
        UpdateRepository.installedFuturePackages(context).keys
            .map { pkg ->
                val info = runCatching { context.packageManager.getPackageInfo(pkg, 0) }.getOrNull()
                Triple(UpdateRepository.appLabel(context, pkg), info?.versionName ?: "", pkg)
            }
            .sortedBy { it.first }
    }
    val fmt = remember { SimpleDateFormat("d.M HH:mm", Locale.ROOT) }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader("עדכוני תוכנה", theme) { navController.popBackStack() }
            LazyColumn {
                item {
                    SettingsCard(theme) {
                        SettingItem("גרסת FutureOS", Build.DISPLAY, FutureIcons.Info, theme, showChevron = false, infoFocusable = true)
                        SettingDivider(theme)
                        val status = when {
                            !configured -> "שרת העדכונים לא מוגדר בגרסה הזו"
                            checking -> "בודק..."
                            lastResult == CheckResult.NotRegistered -> "הטלפון עוד לא נרשם לשרת. נסה שוב בעוד כמה דקות"
                            lastResult == CheckResult.Failed || (lastResult == null && UpdateRepository.lastCheckFailed(context)) ->
                                "הבדיקה נכשלה. בדוק חיבור לאינטרנט"
                            lastCheck == 0L -> "עוד לא נבדק"
                            offers.isEmpty() -> "הכול מעודכן · נבדק ${fmt.format(Date(lastCheck))}"
                            else -> "${offers.size} עדכונים זמינים · נבדק ${fmt.format(Date(lastCheck))}"
                        }
                        SettingItem("מצב", status, FutureIcons.Refresh, theme, showChevron = false, infoFocusable = true)
                    }
                }
                if (configured) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.spacingMd, vertical = FutureDimens.spacingSm),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            if (checking) FutureSpinner(theme = theme.futureTheme)
                            else FutureButton("בדוק עכשיו", theme.futureTheme, { runCheck() }, fillMaxWidth = true, focusRequester = checkFocus)
                        }
                    }
                }
                if (offers.isNotEmpty() && !canInstall) {
                    item {
                        SettingsCard(theme) {
                            SettingItem(
                                "צריך לאשר התקנת עדכונים",
                                "פעם אחת: לאפשר ל-Settings להתקין אפליקציות. לחץ כדי לפתוח",
                                FutureIcons.Security,
                                theme,
                            ) {
                                runCatching {
                                    context.startActivity(
                                        Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                }
                            }
                        }
                    }
                }
                if (offers.isNotEmpty()) {
                    item { SettingHeader("עדכונים זמינים", theme) }
                    item {
                        SettingsCard(theme) {
                            offers.forEachIndexed { index, offer ->
                                OfferRow(theme, offer, progress[offer.releaseId], enabled = canInstall) {
                                    scope.launch { UpdateRepository.install(context, offer) }
                                }
                                if (index < offers.lastIndex) SettingDivider(theme)
                            }
                        }
                    }
                }
                val done = progress.filterValues { it is Progress.Installed || it is Progress.Failed }
                    .filterKeys { id -> offers.none { it.releaseId == id } }
                if (done.isNotEmpty()) {
                    item {
                        SettingsCard(theme) {
                            done.values.forEachIndexed { index, p ->
                                val (title, icon) = if (p is Progress.Installed) "העדכון הותקן" to FutureIcons.Check
                                else "העדכון נכשל: ${UpdatePolicy.failureLabel((p as Progress.Failed).code)}" to FutureIcons.Error
                                SettingItem(title, null, icon, theme, showChevron = false, infoFocusable = true)
                                if (index < done.size - 1) SettingDivider(theme)
                            }
                        }
                    }
                }
                if (configured) {
                    item {
                        SettingsCard(theme) {
                            SettingSwitch(
                                "בדיקה אוטומטית",
                                "פעם ב-6 שעות, עם התראה כשיש עדכון. ההתקנה תמיד באישורך",
                                autoCheck,
                                { on -> autoCheck = on; UpdateRepository.setAutoCheck(context, on) },
                                theme,
                            )
                        }
                    }
                }
                item { SettingHeader("גרסאות מותקנות", theme) }
                item {
                    SettingsCard(theme) {
                        installed.forEachIndexed { index, (label, version, _) ->
                            SettingItem(label, version.ifBlank { null }, null, theme, showChevron = false, infoFocusable = true)
                            if (index < installed.lastIndex) SettingDivider(theme)
                        }
                    }
                }
                item {
                    Text(
                        "עדכונים מגיעים רק משרת FutureOS, ומותקנים רק אם הם חתומים באותו מפתח כמו האפליקציה שבטלפון.",
                        color = theme.textColor.copy(alpha = 0.5f),
                        fontSize = FutureTypography.label,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun OfferRow(theme: ThemeConfig, offer: UpdatePolicy.Offer, progress: Progress?, enabled: Boolean, onInstall: () -> Unit) {
    val context = LocalContext.current
    val label = remember(offer.pkg) { UpdateRepository.appLabel(context, offer.pkg) }
    val sizeMb = "%.1f MB".format(Locale.ROOT, offer.size / 1_048_576.0)
    val busy = progress is Progress.Downloading || progress is Progress.Verifying
    val summary = when (progress) {
        is Progress.Downloading -> "מוריד ${progress.percent}%"
        Progress.Verifying -> "מאמת חתימה..."
        Progress.WaitingForUser -> "ממתין לאישור התקנה"
        Progress.Installed -> "הותקן"
        is Progress.Failed -> UpdatePolicy.failureLabel(progress.code) + ". לחץ לנסות שוב"
        null -> listOf(offer.notesHe.take(120), sizeMb).filter { it.isNotBlank() }.joinToString(" · ")
    }
    SettingItem(
        "$label ${offer.versionName}",
        summary,
        if (progress is Progress.Failed) FutureIcons.Error else FutureIcons.Download,
        theme,
        showChevron = false,
        infoFocusable = !enabled || busy,
        onClick = if (enabled && !busy) onInstall else null,
    )
}
