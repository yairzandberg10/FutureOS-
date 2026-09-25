package com.future.tools.ui

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.idleFieldColor
import androidx.compose.foundation.shape.CircleShape
import com.future.sharednav.theme.subtleTextColor
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.FutureShapes
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.future.tools.data.ToolShortcuts
import com.future.sharednav.theme.FutureTheme

data class ToolEntry(val icon: ImageVector, val label: String, val subtitle: String, val route: ToolRoute)

val TOOL_ENTRIES = listOf(
    // כלים קיימים - המחשבון, השעון עצר/טיימר והפנס עברו לאפליקציות עצמאיות
    // משלהם (Calculator, Clock, Flashlight) ואינם חלק מ-Tools יותר.
    ToolEntry(FutureIcons.SwapHoriz, "ממיר יחידות", "אורך, משקל, טמפרטורה, נפח", ToolRoute.UnitConverter),
    ToolEntry(FutureIcons.Explore, "מצפן וגובה", "כיוון מגנטי וגובה ברומטרי", ToolRoute.Compass),
    ToolEntry(FutureIcons.Straighten, "פלס", "איזון אופקי לפי חיישן תאוצה", ToolRoute.Level),

    // מדידה וחיישנים
    ToolEntry(FutureIcons.GraphicEq, "מד רעש", "עוצמת קול בדציבלים מהמיקרופון", ToolRoute.NoiseMeter),
    ToolEntry(FutureIcons.WbSunny, "מד אור", "עוצמת תאורה בלוקס מהחיישן הקדמי", ToolRoute.LuxMeter),
    ToolEntry(FutureIcons.Architecture, "סרגל וזווית", "סרגל וירטואלי ומד זווית הטיה", ToolRoute.AngleRuler),

    // מחשבונים וממירים
    ToolEntry(FutureIcons.Receipt, "טיפים ופיצול חשבון", "תשר וחלוקה בין סועדים", ToolRoute.TipSplitCalculator),
    ToolEntry(FutureIcons.Percent, "מחשבון פיננסי", "הנחות, מע\"מ והחזרי הלוואה", ToolRoute.QuickFinanceCalculator),
    ToolEntry(FutureIcons.Public, "ממיר אזורי זמן", "השעה הנוכחית בכל העולם", ToolRoute.TimeZoneConverter),

    // פרודוקטיביות
    ToolEntry(FutureIcons.QrCodeScanner, "סורק קודים", "QR וברקוד ללא פרסומות", ToolRoute.QrScanner),
    ToolEntry(FutureIcons.LocalCafe, "פומודורו", "מחזורי מיקוד והפסקה", ToolRoute.Pomodoro),
    ToolEntry(FutureIcons.VpnKey, "מחולל סיסמאות", "סיסמאות חזקות ואקראיות", ToolRoute.PasswordGenerator),
    ToolEntry(FutureIcons.Checklist, "רשימה מהירה", "פתקים ורשימת מטלות", ToolRoute.QuickNotes),

    // כלי עזר אקראיים ופנאי
    ToolEntry(FutureIcons.Casino, "מטבע וקובייה", "הטלת מטבע או קוביות", ToolRoute.CoinDice),
    ToolEntry(FutureIcons.Shuffle, "בורר אקראי", "בחירת אפשרות אקראית מרשימה", ToolRoute.RandomPicker),
    ToolEntry(FutureIcons.Numbers, "מספר אקראי", "הגרלת מספר בטווח שתבחר", ToolRoute.RandomNumber),

    // שדרוגי AI
    ToolEntry(FutureIcons.DocumentScanner, "סורק טקסט", "צילום מסמך והפיכתו לטקסט", ToolRoute.TextScanner),
    ToolEntry(FutureIcons.RecordVoiceOver, "תמלול קולי", "הקלטה קצרה והפיכתה לטקסט", ToolRoute.VoiceTranscribe)
)

@Composable
fun ToolsHomeScreen(theme: FutureTheme, onOpen: (ToolRoute) -> Unit, lastOpenedRoute: ToolRoute? = null) {
    // אותה תקלת "אין פוקוס" שתועדה ותוקנה במחשבון/ממיר יחידות - ראו שם. כאן
    // מסך הבית של כל האפליקציה, כך שהתיקון קריטי במיוחד: בלעדיו נחיתה על המסך
    // הזה משאירה D-pad בלי שום פריט מודגש. כשחוזרים "אחורה" מכלי שנפתח, הפוקוס
    // חוזר בדיוק לשורה של אותו כלי, לא תמיד לשורה הראשונה.
    val rowFocusRequesters = remember { mutableMapOf<ToolRoute, FocusRequester>() }
    LaunchedEffect(Unit) {
        val target = TOOL_ENTRIES.firstOrNull { it.route == lastOpenedRoute } ?: TOOL_ENTRIES.firstOrNull()
        target?.let { rowFocusRequesters.getOrPut(it.route) { FocusRequester() }.requestFocus() }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(title = "כלים", theme = theme)
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingXs),
                    verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm)
                ) {
                    itemsIndexed(TOOL_ENTRIES) { _, entry ->
                        ToolRow(
                            entry.icon, entry.label, entry.subtitle, theme = theme,
                            onClick = { onOpen(entry.route) },
                            trailing = { PinToHomeButton(entry = entry, theme = theme) },
                            focusRequester = rowFocusRequesters.getOrPut(entry.route) { FocusRequester() }
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
    val accent = theme.readableAccentColor
    // כפתור אייקון (IconButton.jsx): 8% במנוחה, 30% הדגשה בפוקוס. נעוץ = נבחר,
    // ולכן האייקון בהדגשה; לא נעוץ = צבע הטקסט.
    val tint = if (isPinned) accent else theme.textColor
    val bgColor by animateColorAsState(
        if (isFocused) accent.copy(alpha = 0.30f) else theme.idleFieldColor,
        FutureMotion.focusColorSpec,
        label = "pinBg"
    )

    Box(
        modifier = Modifier
            .size(FutureDimens.rowHeightTopBarButton)
            .clip(CircleShape)
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
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Icon(FutureIcons.PushPin, contentDescription = "הוסף/הסר ממסך הבית", tint = tint, modifier = Modifier.size(FutureDimens.iconTopBar))
    }
}
