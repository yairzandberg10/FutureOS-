package com.future.frixa.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.future.sharednav.components.FutureCard
import com.future.sharednav.components.FutureDivider
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureSettingItem
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.nav.digitForKey
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.readableAccentColor
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.roundToInt

/** כמות אחת של מרכיב לכל 12 פריקסה (המתכון הבסיסי), והיחידה שלה. */
private data class Amount(val name: String, val per12: Double, val unit: String, val whole: Boolean = false)

private val DOUGH = listOf(
    Amount("קמח", 500.0, "גרם"),
    Amount("שמרים יבשים", 10.0, "גרם"),
    Amount("סוכר", 12.0, "גרם"),
    Amount("מלח", 6.0, "גרם"),
    Amount("ביצים", 1.0, "", whole = true),
    Amount("שמן", 45.0, "מ\"ל"),
    Amount("מים פושרים", 250.0, "מ\"ל"),
)

private val FILLING = listOf(
    Amount("טונה", 2.0, "קופסאות", whole = true),
    Amount("ביצים קשות", 4.0, "", whole = true),
    Amount("תפוחי אדמה", 3.0, "", whole = true),
    Amount("חריסה", 6.0, "כפות"),
    Amount("זיתים", 24.0, "", whole = true),
    Amount("לימון כבוש", 1.0, "צנצנת קטנה"),
)

/** שלבי ההכנה שיש להם זמן - כל אחד טיימר שמתחיל ב-OK. */
private data class Stage(val title: String, val minutes: Int)

private val STAGES = listOf(
    Stage("התפחה ראשונה", 60),
    Stage("התפחה שנייה", 20),
    Stage("טיגון (לכל מחבת)", 3),
    Stage("בישול ביצים קשות", 10),
    Stage("בישול תפוחי אדמה", 20),
)

private fun format(value: Double, whole: Boolean): String =
    if (whole) ceil(value - 1e-9).toInt().toString()
    else if (value >= 20) (value / 5).roundToInt().times(5).toString()
    else "%.1f".format(value).removeSuffix(".0")

/**
 * כלי ההכנה: כמה פריקסה מכינים (ספרות מקלידות את המספר, ימינה/שמאלה על
 * השורה - פחות/יותר), כל הכמויות לבצק ולמילוי מחושבות לפיו, וטיימר לכל
 * שלב (התפחות, טיגון, בישול) - OK מתחיל ועוצר.
 */
@Composable
fun ToolScreen(theme: FutureTheme) {
    var count by rememberSaveable { mutableIntStateOf(12) }
    var typed by remember { mutableStateOf("") }
    var runningStage by remember { mutableStateOf<Stage?>(null) }
    var remaining by remember { mutableIntStateOf(0) }
    val countFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { countFocus.requestFocus() } }

    LaunchedEffect(runningStage) {
        val stage = runningStage ?: return@LaunchedEffect
        remaining = stage.minutes * 60
        while (remaining > 0) {
            delay(1000)
            remaining--
        }
        runningStage = null
    }
    // ספרות שהוקלדו מתחלפות במספר חדש אחרי הפסקה קצרה.
    LaunchedEffect(typed) {
        if (typed.isEmpty()) return@LaunchedEffect
        delay(1500)
        typed = ""
    }
    val factor = count / 12.0

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
        FutureSectionHeader("כמות", theme)
        FutureCard(theme = theme) {
            FutureSettingItem(
                title = "$count פריקסה",
                summary = "ספרות - מספר · ימינה/שמאלה - פחות/יותר",
                icon = FutureIcons.Add,
                theme = theme,
                showChevron = false,
                focusRequester = countFocus,
                onClick = { count += 1 },
                modifier = Modifier.onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    digitForKey(event.key)?.let { digit ->
                        typed = (typed + digit).takeLast(3)
                        count = typed.toInt().coerceIn(1, 200)
                        return@onKeyEvent true
                    }
                    when (event.key) {
                        // RTL: שמאלה = יותר, ימינה = פחות.
                        Key.DirectionLeft -> { count = (count + 1).coerceAtMost(200); true }
                        Key.DirectionRight -> { count = (count - 1).coerceAtLeast(1); true }
                        else -> false
                    }
                },
            )
        }

        FutureSectionHeader("בצק", theme)
        AmountsCard(DOUGH, factor, theme)
        FutureSectionHeader("מילוי", theme)
        AmountsCard(FILLING, factor, theme)

        FutureSectionHeader("טיימרים", theme)
        FutureCard(theme = theme) {
            STAGES.forEachIndexed { index, stage ->
                if (index > 0) FutureDivider(theme = theme)
                val active = runningStage == stage
                FutureSettingItem(
                    title = stage.title,
                    summary = if (active) "%d:%02d נותרו".format(remaining / 60, remaining % 60) else "${stage.minutes} דקות",
                    icon = FutureIcons.Timer,
                    iconTint = if (active) theme.readableAccentColor else null,
                    theme = theme,
                    showChevron = false,
                    onClick = { runningStage = if (active) null else stage },
                    trailing = {
                        Text(if (active) "עצור" else "התחל", color = if (active) theme.readableAccentColor else theme.mutedTextColor, fontSize = FutureTypography.summary)
                    },
                )
            }
        }
    }
}

@Composable
private fun AmountsCard(items: List<Amount>, factor: Double, theme: FutureTheme) {
    FutureCard(theme = theme) {
        items.forEachIndexed { index, item ->
            if (index > 0) FutureDivider(theme = theme)
            FutureSettingItem(
                title = item.name,
                theme = theme,
                onClick = null,
                showChevron = false,
                trailing = {
                    Text(
                        listOf(format(item.per12 * factor, item.whole), item.unit).filter { it.isNotBlank() }.joinToString(" "),
                        color = theme.textColor,
                        fontSize = FutureTypography.bodyLarge,
                        fontWeight = FutureTypography.weightSemibold,
                    )
                },
            )
        }
    }
}
