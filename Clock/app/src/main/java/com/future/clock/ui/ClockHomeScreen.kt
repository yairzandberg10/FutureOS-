package com.future.clock.ui
import com.future.sharednav.focus.bringIntoViewOnFocus

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.clock.data.ClockShortcuts
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ClockEntry(val icon: ImageVector, val label: String, val subtitle: String, val route: ClockRoute)

val CLOCK_ENTRIES = listOf(
    ClockEntry(Icons.Rounded.AccessTime, "שעונים מעוררים", "ניהול התראות וקימה", ClockRoute.Alarms),
    ClockEntry(Icons.Rounded.Public, "שעון עולמי", "זמן בערים שונות בעולם", ClockRoute.WorldClock),
    ClockEntry(Icons.Rounded.Watch, "שעון עצר", "מדידת זמן עם הקפות", ClockRoute.Stopwatch),
    ClockEntry(Icons.Rounded.Timer, "טיימר", "ספירה לאחור עם התראה", ClockRoute.Timer)
)

@Composable
fun ClockHomeScreen(theme: FutureTheme, onOpen: (ClockRoute) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(title = "שעון", theme = theme)
                
                CurrentTimeDisplay(theme)
                
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(CLOCK_ENTRIES) { index, entry ->
                        ToolRow(
                            entry.icon, entry.label, entry.subtitle, theme = theme,
                            onClick = { onOpen(entry.route) },
                            trailing = { PinToHomeButton(entry = entry, theme = theme) },
                            focusRequester = if (index == 0) focusRequester else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentTimeDisplay(theme: FutureTheme) {
    var timeText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, d בMMMM", Locale("he"))
        while (true) {
            val now = Date()
            timeText = timeFormat.format(now)
            dateText = dateFormat.format(now)
            delay(1000)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            timeText,
            color = theme.textColor,
            fontSize = 64.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center
        )
        Text(
            dateText,
            color = theme.textColor.copy(alpha = 0.6f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

/** מוסיף/מסיר את המסך כאייקון עצמאי במסך הבית (activity-alias נפרד, ראו ClockShortcuts). */
@Composable
private fun PinToHomeButton(entry: ClockEntry, theme: FutureTheme) {
    val context = LocalContext.current
    var isPinned by remember(entry.route) { mutableStateOf(ClockShortcuts.isPinnedToHome(context, entry.route)) }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val tint by animateColorAsState(
        if (isPinned) theme.accentColor else theme.textColor.copy(alpha = if (isFocused) 0.6f else 0.3f),
        label = "pinTint"
    )
    val bgColor by animateColorAsState(
        if (isFocused) theme.textColor.copy(alpha = 0.14f) else theme.textColor.copy(alpha = 0f),
        label = "pinBg"
    )

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null) {
                val next = !isPinned
                ClockShortcuts.setPinnedToHome(context, entry.route, next)
                isPinned = next
                Toast.makeText(
                    context,
                    if (next) "${entry.label} נוסף כאפליקציה עצמאית - אפשר להוסיף אותו למסך הבית דרך \"הוספת אפליקציה\" בלאנצ'ר"
                    else "${entry.label} הוסר מרשימת האפליקציות העצמאיות",
                    Toast.LENGTH_LONG
                ).show()
            }
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Icon(Icons.Rounded.PushPin, contentDescription = "הוסף/הסר ממסך הבית", tint = tint, modifier = Modifier.size(18.dp))
    }
}
