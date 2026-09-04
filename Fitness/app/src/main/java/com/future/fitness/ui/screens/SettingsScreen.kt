package com.future.fitness.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.future.fitness.bluetooth.HeartRateMonitor
import com.future.fitness.bluetooth.HrConnectionState
import com.future.fitness.data.UserProfile
import com.future.fitness.ui.components.FocusableItem
import com.future.fitness.ui.components.SegmentedControl
import com.future.fitness.ui.components.escapeTextFieldFocusTrap
import com.future.fitness.ui.components.fitnessTextFieldColors
import com.future.fitness.ui.components.ScreenTopBar
import com.future.sharednav.theme.FutureTheme

/** מצב כהה/בהיר וצבע הדגשה משותפים לכל אפליקציות FutureOS ונשלטים ממסך
 * ההגדרות המרכזי של המערכת (כמו בכל שאר אפליקציות הסוויטה - אף אחת מהן לא
 * כופלת בורר עיצוב משלה); כאן ההעדפות הספציפיות לאפליקציית הכושר: יחידות,
 * פרופיל אישי (משקל/גיל - לחישוב קלוריות ואזורי דופק מדויקים), וחיבור שעון
 * חכם/רצועת דופק ב-Bluetooth. */
@Composable
fun SettingsScreen(
    theme: FutureTheme,
    units: String,
    profile: UserProfile,
    heartRateMonitor: HeartRateMonitor,
    onBack: () -> Unit,
    onSetUnits: (String) -> Unit,
    onSetProfile: (weightKg: Int?, age: Int?) -> Unit,
    onDeviceConnected: (address: String, name: String) -> Unit,
    onDeviceDisconnected: () -> Unit,
) {
    val context = LocalContext.current
    var weightText by remember { mutableStateOf(profile.weightKg?.toString() ?: "") }
    var ageText by remember { mutableStateOf(profile.age?.toString() ?: "") }
    val weightFieldFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { weightFieldFocusRequester.requestFocus() }

    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= 31) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        emptyArray()
    }
    var hasBluetoothPermission by remember {
        mutableStateOf(bluetoothPermissions.isEmpty() || bluetoothPermissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED })
    }
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        hasBluetoothPermission = results.values.all { it }
        if (hasBluetoothPermission) heartRateMonitor.startScan()
    }

    val fieldColors = fitnessTextFieldColors(theme)

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(title = "הגדרות", theme = theme, onBack = onBack)

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().escapeTextFieldFocusTrap()) {
            item {
                SectionLabel("עיצוב", theme)
                Text(
                    "מצב כהה/בהיר וצבע הדגשה משותפים לכל האפליקציות ונקבעים במסך ההגדרות המרכזי של המערכת.",
                    color = theme.textColor.copy(alpha = 0.45f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
                )
            }

            item {
                SectionLabel("יחידות", theme)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(theme.textColor.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("יחידות משקל", color = theme.textColor, fontSize = 15.sp)
                    SegmentedControl(
                        options = listOf("ק״ג", "lb"),
                        selected = if (units == "kg") "ק״ג" else "lb",
                        theme = theme,
                        onSelect = { onSetUnits(if (it == "ק״ג") "kg" else "lb") },
                        segmentWidth = 48.dp,
                    )
                }
            }

            item {
                SectionLabel("פרופיל אישי", theme)
                Text(
                    "משמש לחישוב קלוריות מדויק (met × משקל × זמן) ולהערכת אזורי דופק.",
                    color = theme.textColor.copy(alpha = 0.45f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
                )
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { v ->
                            if (v.length <= 3 && v.all { it.isDigit() }) {
                                weightText = v
                                onSetProfile(v.toIntOrNull(), ageText.toIntOrNull())
                            }
                        },
                        label = { Text("משקל (ק״ג)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).focusRequester(weightFieldFocusRequester),
                        singleLine = true,
                        colors = fieldColors,
                    )
                    Spacer(Modifier.width(10.dp))
                    OutlinedTextField(
                        value = ageText,
                        onValueChange = { v ->
                            if (v.length <= 3 && v.all { it.isDigit() }) {
                                ageText = v
                                onSetProfile(weightText.toIntOrNull(), v.toIntOrNull())
                            }
                        },
                        label = { Text("גיל") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = fieldColors,
                    )
                }
                Spacer(Modifier.padding(bottom = 8.dp))
            }

            item {
                SectionLabel("שעון חכם / רצועת דופק", theme)
                Text(
                    "חיבור Bluetooth סטנדרטי (Heart Rate Service) - תואם לרוב השעונים החכמים ורצועות הדופק. הדופק החי יוצג באימונים ובריצות ויישמר בהיסטוריה.",
                    color = theme.textColor.copy(alpha = 0.45f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
                )
            }

            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    when (heartRateMonitor.state) {
                        HrConnectionState.CONNECTED -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(theme.accentColor.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Rounded.Watch, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(heartRateMonitor.connectedDeviceName ?: "מכשיר מחובר", color = theme.textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("מחובר", color = theme.accentColor, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { heartRateMonitor.disconnect(); onDeviceDisconnected() },
                                    colors = ButtonDefaults.buttonColors(containerColor = theme.textColor.copy(alpha = 0.1f), contentColor = theme.textColor),
                                ) { Text("נתק", fontSize = 13.sp) }
                            }
                        }
                        HrConnectionState.CONNECTING -> {
                            Text("מתחבר...", color = theme.textColor.copy(alpha = 0.6f), fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
                        }
                        HrConnectionState.SCANNING -> {
                            Text("סורק מכשירים בקרבת מקום...", color = theme.textColor.copy(alpha = 0.6f), fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
                        }
                        HrConnectionState.DISCONNECTED -> {
                            Button(
                                onClick = {
                                    if (hasBluetoothPermission) heartRateMonitor.startScan() else bluetoothPermissionLauncher.launch(bluetoothPermissions)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor, contentColor = theme.backgroundColor),
                            ) {
                                Icon(Icons.Rounded.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("סרוק מכשירים", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (heartRateMonitor.state == HrConnectionState.SCANNING || heartRateMonitor.foundDevices.isNotEmpty()) {
                items(heartRateMonitor.foundDevices) { device ->
                    FocusableItem(
                        onClick = {
                            heartRateMonitor.connect(device.address)
                            onDeviceConnected(device.address, device.name)
                        },
                        theme = theme,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    ) { isFocused ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(theme.textColor.copy(alpha = if (isFocused) 0.1f else 0.04f), RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier.size(32.dp).background(theme.textColor.copy(alpha = 0.08f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.Watch, contentDescription = null, tint = theme.textColor.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(device.name, color = theme.textColor, fontSize = 14.sp)
                        }
                    }
                }
            }

            item { Spacer(Modifier.padding(bottom = 24.dp)) }
        }
    }
}

@Composable
private fun SectionLabel(text: String, theme: FutureTheme) {
    Text(
        text,
        color = theme.textColor.copy(alpha = 0.6f),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp),
    )
}
