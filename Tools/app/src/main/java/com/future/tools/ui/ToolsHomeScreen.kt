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
import androidx.compose.ui.focus.onFocusChanged
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.nav.onOptionsKeyPress
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
    ToolEntry(FutureIcons.Explore, "מצפן", "צפון אמיתי לפי המיקום, קואורדינטות וגובה", ToolRoute.Compass),
    ToolEntry(FutureIcons.Straighten, "פלס", "איזון אופקי לפי חיישן תאוצה", ToolRoute.Level),

    // מדידה וחיישנים
    ToolEntry(FutureIcons.GraphicEq, "מד רעש", "עוצמת קול בדציבלים מהמיקרופון", ToolRoute.NoiseMeter),
    ToolEntry(FutureIcons.WbSunny, "מד אור", "עוצמת תאורה בלוקס מהחיישן הקדמי", ToolRoute.LuxMeter),
    ToolEntry(FutureIcons.Architecture, "סרגל וזווית", "סרגל וירטואלי ומד זווית הטיה", ToolRoute.AngleRuler),
    ToolEntry(FutureIcons.MusicNote, "מכוון גיטרה", "כיוון מיתרים לפי המיקרופון", ToolRoute.GuitarTuner),

    // מחשבונים וממירים
    ToolEntry(FutureIcons.Receipt, "טיפים ופיצול חשבון", "תשר וחלוקה בין סועדים", ToolRoute.TipSplitCalculator),
    ToolEntry(FutureIcons.Percent, "מחשבון פיננסי", "הנחות, מע\"מ והחזרי הלוואה", ToolRoute.QuickFinanceCalculator),
    ToolEntry(FutureIcons.Public, "ממיר אזורי זמן", "השעה הנוכחית בכל העולם", ToolRoute.TimeZoneConverter),

    // פרודוקטיביות
    ToolEntry(FutureIcons.QrCodeScanner, "סורק קודים", "QR וברקוד ללא פרסומות", ToolRoute.QrScanner),
    ToolEntry(FutureIcons.LocalCafe, "פומודורו", "מחזורי מיקוד והפסקה", ToolRoute.Pomodoro),
    ToolEntry(FutureIcons.VpnKey, "מחולל סיסמאות", "סיסמאות חזקות ואקראיות", ToolRoute.PasswordGenerator),

    // כלי עזר אקראיים ופנאי
    ToolEntry(FutureIcons.Casino, "מטבע וקובייה", "הטלת מטבע או קוביות", ToolRoute.CoinDice),
    ToolEntry(FutureIcons.Shuffle, "בורר אקראי", "בחירת אפשרות אקראית מרשימה", ToolRoute.RandomPicker),
    ToolEntry(FutureIcons.Numbers, "מספר אקראי", "הגרלת מספר בטווח שתבחר", ToolRoute.RandomNumber),

    // שדרוגי AI
    ToolEntry(FutureIcons.DocumentScanner, "סורק טקסט", "צילום מסמך והפיכתו לטקסט", ToolRoute.TextScanner)
)

@Composable
fun ToolsHomeScreen(theme: FutureTheme, onOpen: (ToolRoute) -> Unit, lastOpenedRoute: ToolRoute? = null) {
    // אותה תקלת "אין פוקוס" שתועדה ותוקנה במחשבון/ממיר יחידות - ראו שם. כאן
    // מסך הבית של כל האפליקציה, כך שהתיקון קריטי במיוחד: בלעדיו נחיתה על המסך
    // הזה משאירה D-pad בלי שום פריט מודגש. כשחוזרים "אחורה" מכלי שנפתח, הפוקוס
    // חוזר בדיוק לשורה של אותו כלי, לא תמיד לשורה הראשונה.
    val context = LocalContext.current
    val rowFocusRequesters = remember { mutableMapOf<ToolRoute, FocusRequester>() }
    LaunchedEffect(Unit) {
        val target = TOOL_ENTRIES.firstOrNull { it.route == lastOpenedRoute } ?: TOOL_ENTRIES.firstOrNull()
        target?.let { rowFocusRequesters.getOrPut(it.route) { FocusRequester() }.requestFocus() }
    }

    // ההצמדה למסך הבית עברה מכפתור בכל שורה למקש Options - שורה עם כפתור
    // נוסף דרשה שתי לחיצות חץ לכל כלי, והכפתור בלבל עם פתיחת הכלי עצמו.
    var focusedEntry by remember { mutableStateOf<ToolEntry?>(null) }
    var menuFor by remember { mutableStateOf<ToolEntry?>(null) }
    onOptionsKeyPress { menuFor = if (menuFor == null) focusedEntry else null }

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
                            focusRequester = rowFocusRequesters.getOrPut(entry.route) { FocusRequester() },
                            modifier = Modifier.onFocusChanged { if (it.isFocused) focusedEntry = entry },
                        )
                    }
                }
            }
        }
    }

    menuFor?.let { entry ->
        val isPinned = ToolShortcuts.isPinnedToHome(context, entry.route)
        FutureOptionsMenu(theme = theme, onDismissRequest = { menuFor = null }, header = entry.label) {
            FutureMenuRow("פתח", entry.icon, theme, { menuFor = null; onOpen(entry.route) })
            FutureMenuRow(
                if (isPinned) "הסר ממסך הבית" else "הצמד למסך הבית",
                FutureIcons.PushPin,
                theme,
                {
                    menuFor = null
                    ToolShortcuts.setPinnedToHome(context, entry.route, !isPinned)
                    Toast.makeText(
                        context,
                        if (!isPinned) "${entry.label} נוסף כאפליקציה עצמאית - אפשר להוסיף אותו למסך הבית דרך \"הוספת אפליקציה\" בלאנצ'ר"
                        else "${entry.label} הוסר מרשימת האפליקציות העצמאיות",
                        Toast.LENGTH_LONG
                    ).show()
                },
            )
        }
    }
}
