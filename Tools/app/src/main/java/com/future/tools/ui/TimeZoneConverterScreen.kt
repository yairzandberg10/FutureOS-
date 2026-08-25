package com.future.tools.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.tools.ui.theme.FutureTheme
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private data class WorldCity(val label: String, val zoneId: String)

private val WORLD_CITIES = listOf(
    WorldCity("ירושלים", "Asia/Jerusalem"),
    WorldCity("ניו יורק", "America/New_York"),
    WorldCity("לוס אנג'לס", "America/Los_Angeles"),
    WorldCity("לונדון", "Europe/London"),
    WorldCity("פריז", "Europe/Paris"),
    WorldCity("מוסקבה", "Europe/Moscow"),
    WorldCity("דובאי", "Asia/Dubai"),
    WorldCity("מומבאי", "Asia/Kolkata"),
    WorldCity("בנגקוק", "Asia/Bangkok"),
    WorldCity("טוקיו", "Asia/Tokyo"),
    WorldCity("סידני", "Australia/Sydney")
)

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM")

@Composable
fun TimeZoneConverterScreen(theme: FutureTheme, onBack: () -> Unit) {
    var now by remember { mutableStateOf(ZonedDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = ZonedDateTime.now()
            delay(1000)
        }
    }

    val localZone = ZoneId.systemDefault()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(title = "ממיר אזורי זמן", theme = theme, onBack = onBack)

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(WORLD_CITIES) { city ->
                        val zoned = now.withZoneSameInstant(ZoneId.of(city.zoneId))
                        val dayDiff = zoned.toLocalDate().toEpochDay() - now.withZoneSameInstant(localZone).toLocalDate().toEpochDay()
                        CityTimeRow(city = city, zoned = zoned, dayDiff = dayDiff, theme = theme)
                    }
                }
            }
        }
    }
}

@Composable
private fun CityTimeRow(city: WorldCity, zoned: ZonedDateTime, dayDiff: Long, theme: FutureTheme) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(theme.textColor.copy(alpha = 0.055f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(city.label, color = theme.textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(
                zoned.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("he")) + " · " + zoned.format(dateFormatter) +
                    when {
                        dayDiff > 0 -> " (מחר)"
                        dayDiff < 0 -> " (אתמול)"
                        else -> ""
                    },
                color = theme.textColor.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
        Text(zoned.format(timeFormatter), color = theme.accentColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}
