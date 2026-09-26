package com.future.futureui.settings.ui

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.futureui.controlcenter.ui.components.focusEffect
import com.future.futureui.statusbar.logic.StatusBarLayoutManager

/**
 * מסך ההתאמה האישית המרכזי של FutureUI - מרכז במקום אחד את כל ההגדרות
 * שאפשר לכוונן בכל חלקי המערכת (שורת מצב וכו'), כדי שלא יהיה
 * צריך לחפש כל הגדרה בנפרד. מרכז הבקרה עצמו כבר תומך בעריכה ישירה
 * (לחיצה ארוכה בתוכו) ולכן לא מופיע כאן.
 */
@Composable
fun FutureUISettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val statusBarLayout = remember { StatusBarLayoutManager(context) }
    var showBattery by remember { mutableStateOf(statusBarLayout.getShowBattery()) }
    var showBluetooth by remember { mutableStateOf(statusBarLayout.getShowBluetooth()) }
    var use24Hour by remember { mutableStateOf(statusBarLayout.getUse24HourClock()) }
    var suppressSystemBars by remember { mutableStateOf(statusBarLayout.getSuppressSystemBars()) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                text = "התאמה אישית",
                color = Color.White,
                fontSize = FutureTypography.headline,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            SettingsSection(title = "שורת מצב") {
                SettingsToggleRow("הצג סוללה", showBattery) {
                    showBattery = it; statusBarLayout.saveShowBattery(it)
                }
                SettingsToggleRow("הצג Bluetooth", showBluetooth) {
                    showBluetooth = it; statusBarLayout.saveShowBluetooth(it)
                }
                SettingsToggleRow("שעון 24 שעות", use24Hour) {
                    use24Hour = it; statusBarLayout.saveUse24HourClock(it)
                }
                SettingsToggleRow("הסתר שורת מצב מקורית של אנדרואיד (דורש root)", suppressSystemBars) {
                    suppressSystemBars = it; statusBarLayout.saveSuppressSystemBars(it)
                }
            }


            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "מרכז הבקרה ניתן לעריכה ישירות בתוכו - לחיצה ארוכה על כפתור העריכה מאפשרת להוסיף, להסיר ולסדר מחדש כפתורים.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = FutureTypography.label
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        text = title,
        color = Color.White.copy(alpha = 0.6f),
        fontSize = FutureTypography.summary,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FutureShapes.xl)
            .background(Color.White.copy(alpha = 0.08f))
    ) {
        content()
    }
}

@Composable
private fun SettingsToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = FutureShapes.lg

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusEffect(isFocused, shape)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onChange(!value) }
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White, fontSize = FutureTypography.body, modifier = Modifier.weight(1f))
        Switch(
            checked = value,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Color.White, checkedThumbColor = Color.Black)
        )
    }
}
