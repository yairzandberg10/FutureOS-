package com.future.clock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

data class CityClock(val name: String, val timezone: String)

val DEFAULT_CITIES = listOf(
    CityClock("ירושלים", "Asia/Jerusalem"),
    CityClock("לונדון", "Europe/London"),
    CityClock("ניו יורק", "America/New_York"),
    CityClock("טוקיו", "Asia/Tokyo"),
    CityClock("פריז", "Europe/Paris")
)

@Composable
fun WorldClockScreen(theme: FutureTheme, onBack: () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(title = "שעון עולמי", theme = theme, onBack = onBack)
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(DEFAULT_CITIES) { city ->
                        CityClockRow(city, theme)
                    }
                }
            }
        }
    }
}

@Composable
fun CityClockRow(city: CityClock, theme: FutureTheme) {
    var timeText by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone(city.timezone)
        while (true) {
            timeText = sdf.format(Date())
            delay(1000)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.textColor.copy(alpha = 0.05f), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(city.name, color = theme.textColor, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Text(timeText, color = theme.accentColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}
