package com.future.tools.ui

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Architecture
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.future.tools.data.ToolShortcuts
import com.future.tools.ui.theme.FutureTheme

data class ToolEntry(val icon: ImageVector, val label: String, val subtitle: String, val route: ToolRoute)

val TOOL_ENTRIES = listOf(
    // כלים קיימים
    ToolEntry(Icons.Rounded.Calculate, "מחשבון", "חישובים והיסטוריה", ToolRoute.Calculator),
    ToolEntry(Icons.Rounded.FlashlightOn, "פנס", "הדלקה/כיבוי מהירים", ToolRoute.Flashlight),
    ToolEntry(Icons.Rounded.Watch, "שעון עצר", "מדידת זמן עם הקפות", ToolRoute.Stopwatch),
    ToolEntry(Icons.Rounded.Timer, "טיימר", "ספירה לאחור עם התראה", ToolRoute.Timer),
    ToolEntry(Icons.Rounded.SwapHoriz, "ממיר יחידות", "אורך, משקל, טמפרטורה, נפח", ToolRoute.UnitConverter),
    ToolEntry(Icons.Rounded.Explore, "מצפן וגובה", "כיוון מגנטי וגובה ברומטרי", ToolRoute.Compass),
    ToolEntry(Icons.Rounded.Straighten, "פלס", "איזון אופקי לפי חיישן תאוצה", ToolRoute.Level),

    // מדידה וחיישנים
    ToolEntry(Icons.Rounded.GraphicEq, "מד רעש", "עוצמת קול בדציבלים מהמיקרופון", ToolRoute.NoiseMeter),
    ToolEntry(Icons.Rounded.WbSunny, "מד אור", "עוצמת תאורה בלוקס מהחיישן הקדמי", ToolRoute.LuxMeter),
    ToolEntry(Icons.Rounded.Architecture, "סרגל וזווית", "סרגל וירטואלי ומד זווית הטיה", ToolRoute.AngleRuler),

    // מחשבונים וממירים
    ToolEntry(Icons.Rounded.Receipt, "טיפים ופיצול חשבון", "תשר וחלוקה בין סועדים", ToolRoute.TipSplitCalculator),
    ToolEntry(Icons.Rounded.Percent, "מחשבון פיננסי", "הנחות, מע\"מ והחזרי הלוואה", ToolRoute.QuickFinanceCalculator),
    ToolEntry(Icons.Rounded.Public, "ממיר אזורי זמן", "השעה הנוכחית בכל העולם", ToolRoute.TimeZoneConverter),

    // פרודוקטיביות
    ToolEntry(Icons.Rounded.QrCodeScanner, "סורק קודים", "QR וברקוד ללא פרסומות", ToolRoute.QrScanner),
    ToolEntry(Icons.Rounded.LocalCafe, "פומודורו", "מחזורי מיקוד והפסקה", ToolRoute.Pomodoro),
    ToolEntry(Icons.Rounded.VpnKey, "מחולל סיסמאות", "סיסמאות חזקות ואקראיות", ToolRoute.PasswordGenerator),
    ToolEntry(Icons.Rounded.Checklist, "רשימה מהירה", "פתקים ורשימת מטלות", ToolRoute.QuickNotes),

    // כלי עזר אקראיים ופנאי
    ToolEntry(Icons.Rounded.Casino, "מטבע וקובייה", "הטלת מטבע או קוביות", ToolRoute.CoinDice),
    ToolEntry(Icons.Rounded.Shuffle, "בורר אקראי", "בחירת אפשרות אקראית מרשימה", ToolRoute.RandomPicker),
    ToolEntry(Icons.Rounded.Numbers, "מספר אקראי", "הגרלת מספר בטווח שתבחר", ToolRoute.RandomNumber),

    // שדרוגי AI
    ToolEntry(Icons.Rounded.DocumentScanner, "סורק טקסט", "צילום מסמך והפיכתו לטקסט", ToolRoute.TextScanner),
    ToolEntry(Icons.Rounded.RecordVoiceOver, "תמלול קולי", "הקלטה קצרה והפיכתה לטקסט", ToolRoute.VoiceTranscribe)
)

@Composable
fun ToolsHomeScreen(theme: FutureTheme, onOpen: (ToolRoute) -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(title = "כלים", theme = theme)
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(TOOL_ENTRIES) { entry ->
                        ToolRow(
                            entry.icon, entry.label, entry.subtitle, theme = theme,
                            onClick = { onOpen(entry.route) },
                            trailing = { PinToHomeButton(entry = entry, theme = theme) }
                        )
                    }
                }
            }
        }
    }
}

/** מוסיף/מסיר את הכלי כאייקון עצמאי במסך הבית (activity-alias נפרד, ראו ToolShortcuts). */
@Composable
private fun PinToHomeButton(entry: ToolEntry, theme: FutureTheme) {
    val context = LocalContext.current
    var isPinned by remember(entry.route) { mutableStateOf(ToolShortcuts.isPinnedToHome(context, entry.route)) }
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
                ToolShortcuts.setPinnedToHome(context, entry.route, next)
                isPinned = next
                Toast.makeText(
                    context,
                    if (next) "${entry.label} נוסף כאפליקציה עצמאית - אפשר להוסיף אותו למסך הבית דרך \"הוספת אפליקציה\" בלאנצ'ר"
                    else "${entry.label} הוסר מרשימת האפליקציות העצמאיות",
                    Toast.LENGTH_LONG
                ).show()
            }
            .focusable(interactionSource = interactionSource),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Icon(Icons.Rounded.PushPin, contentDescription = "הוסף/הסר ממסך הבית", tint = tint, modifier = Modifier.size(18.dp))
    }
}
