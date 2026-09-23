package com.future.translate.ui

import com.future.sharednav.icons.FutureIcons

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.future.sharednav.components.AnimatedBackStackHost
import com.future.sharednav.components.ConfirmDialog
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.components.FutureSnackbarHost
import com.future.sharednav.components.rememberFutureSnackbarState
import com.future.sharednav.nav.onOptionsKeyPress
import com.future.sharednav.theme.FutureTheme

/** המסכים של האפליקציה, כמחסנית: התרגום בתחתית, והשאר נכנסים מעליו. */
sealed interface Route {
    data object Root : Route
    data class Picker(val source: Boolean) : Route
    data object History : Route
    data object Talk : Route
    data object Downloads : Route
    data object Settings : Route
}

/** פעולות שיוצאות מהאפליקציה - מתבצעות ב-Activity. */
class TranslateActions(
    val copy: (String) -> Unit,
    val share: (String) -> Unit,
    /** מבקש הרשאת מיקרופון; [onGranted] רץ כשהיא אושרה. */
    val requestMic: (onGranted: () -> Unit) -> Unit,
)

/**
 * אפליקציית התרגום (ui_kits/translate): מסך תרגום עם בורר שפות, קלט ותוצאה;
 * בורר שפה; היסטוריה עם שמורים; מצב שיחה; הורדת שפות והגדרות. מקש התפריט
 * פותח את תפריט האפשרויות.
 */
@Composable
fun TranslateApp(viewModel: TranslateViewModel, theme: FutureTheme, actions: TranslateActions) {
    val stack = remember { mutableStateListOf<Route>(Route.Root) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val snackbar = rememberFutureSnackbarState()

    fun push(route: Route) {
        viewModel.commit()
        stack.add(route)
    }
    fun pop() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    BackHandler(enabled = stack.size > 1) { pop() }

    val top = stack.last()
    val menuAllowed = top == Route.Root || top == Route.History || top == Route.Talk
    onOptionsKeyPress { if (menuAllowed && !confirmClear) menuOpen = !menuOpen }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackStackHost(backStack = stack.toList()) { route ->
            when (route) {
                Route.Root -> TranslateScreen(
                    viewModel = viewModel,
                    theme = theme,
                    snackbar = snackbar,
                    actions = actions,
                    onPick = { source -> push(Route.Picker(source)) },
                    onOpenHistory = { push(Route.History) },
                    onMenu = { menuOpen = true },
                )
                is Route.Picker -> LanguagePickerScreen(
                    viewModel = viewModel,
                    theme = theme,
                    source = route.source,
                    onBack = ::pop,
                    onSelect = { code ->
                        if (route.source) viewModel.setFrom(code) else viewModel.setTo(code)
                        pop()
                    },
                )
                Route.History -> HistoryScreen(
                    viewModel = viewModel,
                    theme = theme,
                    onBack = ::pop,
                    onMenu = { menuOpen = true },
                    onOpen = { entry ->
                        viewModel.load(entry)
                        pop()
                    },
                )
                Route.Talk -> TalkScreen(
                    viewModel = viewModel,
                    theme = theme,
                    actions = actions,
                    onBack = ::pop,
                    onMenu = { menuOpen = true },
                )
                Route.Downloads -> DownloadsScreen(
                    viewModel = viewModel,
                    theme = theme,
                    snackbar = snackbar,
                    onBack = ::pop,
                )
                Route.Settings -> SettingsScreen(viewModel = viewModel, theme = theme, onBack = ::pop)
            }
        }
        FutureSnackbarHost(snackbar, theme)
    }

    if (menuOpen) {
        FutureOptionsMenu(theme = theme, onDismissRequest = { menuOpen = false }, header = "תרגום") {
            fun pick(action: () -> Unit): () -> Unit = { menuOpen = false; action() }
            if (top != Route.Talk) FutureMenuRow("שיחה", FutureIcons.Forum, theme, pick { push(Route.Talk) })
            if (top != Route.History) FutureMenuRow("היסטוריה", FutureIcons.History, theme, pick { push(Route.History) })
            FutureMenuRow("הורדת שפה", FutureIcons.Download, theme, pick { push(Route.Downloads) })
            FutureMenuRow("הגדרות", FutureIcons.Settings, theme, pick { push(Route.Settings) })
            FutureMenuRow("נקה היסטוריה", FutureIcons.Delete, theme, pick { confirmClear = true }, destructive = true)
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            message = "לנקות את היסטוריית התרגומים?",
            theme = theme,
            confirmLabel = "נקה",
            onCancel = { confirmClear = false },
            onConfirm = {
                confirmClear = false
                viewModel.clearHistory()
                snackbar.show("ההיסטוריה נוקתה")
            },
        )
    }
}
