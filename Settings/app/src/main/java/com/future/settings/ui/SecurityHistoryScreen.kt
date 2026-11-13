package com.future.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.future.settings.theme.SecurityEvent
import com.future.settings.ui.components.SettingDivider
import com.future.settings.ui.components.SettingHeader
import com.future.settings.ui.components.SettingItem
import com.future.settings.ui.components.SettingsCard
import com.future.settings.ui.theme.ThemeConfig
import com.future.sharednav.icons.FutureIcons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * היסטוריית האבטחה: כל מה שההגנה עשתה ומצאה, החדש קודם. היומן נשמר מוצפן בממשק המערכת
 * (SecurityAudit) ונקרא כאן לתצוגה בלבד - אי אפשר למחוק או לערוך אותו מכאן.
 */
@Composable
fun SecurityHistoryScreen(navController: NavController, theme: ThemeConfig) {
    val context = LocalContext.current
    var events by remember { mutableStateOf<List<SecurityEvent>?>(null) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        events = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.future.settings.theme.ThreatClient.log(context)
        }
        loaded = true
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader("היסטוריית אבטחה", theme) { navController.popBackStack() }
            LazyColumn {
                val list = events
                when {
                    !loaded -> item {
                        SettingsCard(theme) {
                            SettingItem("טוען…", null, FutureIcons.History, theme, showChevron = false, infoFocusable = true)
                        }
                    }
                    list == null -> item {
                        SettingsCard(theme) {
                            SettingItem(
                                "היומן לא זמין",
                                "צריך את הגרסה החדשה של ממשק המערכת (FutureUI)",
                                FutureIcons.Error, theme, showChevron = false, infoFocusable = true
                            )
                        }
                    }
                    list.isEmpty() -> item {
                        SettingsCard(theme) {
                            SettingItem(
                                "אין אירועים עדיין",
                                "כאן יופיע כל מה שההגנה חוסמת או מוצאת",
                                FutureIcons.Check, theme, showChevron = false, infoFocusable = true
                            )
                        }
                    }
                    else -> {
                        list.groupBy { dayOf(it.time) }.forEach { (day, dayEvents) ->
                            item { SettingHeader(day, theme) }
                            item {
                                SettingsCard(theme) {
                                    dayEvents.forEachIndexed { i, e ->
                                        val (title, summary) = describeEvent(e)
                                        SettingItem(
                                            title,
                                            listOf(timeOf(e.time), summary).filter { it.isNotBlank() }.joinToString(" · "),
                                            iconFor(e.type),
                                            theme,
                                            showChevron = false,
                                            infoFocusable = true
                                        )
                                        if (i < dayEvents.lastIndex) SettingDivider(theme)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun dayOf(t: Long): String {
    val fmt = SimpleDateFormat("yyyyMMdd", Locale.US)
    val now = System.currentTimeMillis()
    return when (fmt.format(Date(t))) {
        fmt.format(Date(now)) -> "היום"
        fmt.format(Date(now - 24L * 60 * 60 * 1000)) -> "אתמול"
        else -> SimpleDateFormat("d.M.yyyy", Locale.getDefault()).format(Date(t))
    }
}

private fun timeOf(t: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(t))

/** מה קרה, ובמשפט השני - מה היה לפני כן (כשידוע). */
private fun describeEvent(e: SecurityEvent): Pair<String, String> {
    val name = e.label.ifBlank { e.target }
    val what = when (e.kind) {
        "ACCESSIBILITY" -> "שליטה במסך"
        "NOTIFICATION_LISTENER" -> "קריאת התראות"
        "SMS_READER" -> "קריאת SMS"
        "OVERLAY" -> "חלון מעל אפליקציות"
        "INSTALLER" -> "התקנת אפליקציות"
        "PROXY" -> "שרת מתווך"
        "DEVICE_ADMIN" -> "ניהול המכשיר"
        "USER_CA" -> "תעודת אבטחה"
        "VPN" -> "VPN"
        "ADB_WIFI" -> "ניפוי באגים ב-Wi-Fi"
        "KNOWN_MALWARE" -> "נוזקת ריגול ידועה"
        "SUSPICIOUS" -> "שילוב יכולות חשוד"
        "SIDELOADED" -> "התקנה ממקור לא מוכר"
        else -> e.kind
    }
    val before = if (e.detail.isNotBlank()) "לפני כן: ${e.detail}" else ""
    return when (e.type) {
        "AUTO_BLOCK" -> "נחסם אוטומטית: $what של $name" to before
        "BLOCK" -> "חסמת: $what של $name" to before
        "HOLD" -> "הושהתה: $name ($what)" to "אי אפשר היה לפתוח אותה עד שתחליט"
        "RESTORE" -> "הוחזר: $what של $name" to "סומן כמהימן"
        "ALLOW" -> "אישרת: $what של $name" to ""
        "NEW" -> "ממצא חדש: $what של $name" to e.detail
        "SIDELOAD" -> "$name הותקנה ממקור לא מוכר" to (if (e.detail.isNotBlank() && e.detail != "unknown") "דרך ${e.detail}" else "")
        "BASELINE" -> "ההגנה הופעלה" to "${e.detail} ממצאים שהיו קיימים חיכו להחלטה שלך"
        "TAMPER" -> "זוהה שינוי בהגדרות ההגנה" to "רשימת המהימנים שונתה מחוץ למערכת ואופסה"
        "DB_UPDATE" -> "מאגר הנוזקות עודכן" to e.detail
        "DB_REJECT" -> "עדכון מאגר נדחה" to when (e.detail) {
            "signature" -> "החתימה לא תקינה"
            "invalid" -> "הקובץ פגום"
            "too_big" -> "הקובץ גדול מדי"
            else -> e.detail
        }
        "LOG_RESET" -> "היומן הקודם לא נפתח" to "הקובץ שונה או נפגם, אז התחיל יומן חדש"
        else -> e.type to e.detail
    }
}

private fun iconFor(type: String) = when (type) {
    "AUTO_BLOCK", "BLOCK", "HOLD" -> FutureIcons.Security
    "RESTORE", "ALLOW" -> FutureIcons.Check
    "TAMPER", "LOG_RESET", "DB_REJECT" -> FutureIcons.Error
    "DB_UPDATE" -> FutureIcons.Download
    else -> FutureIcons.PrivacyTip
}
