package com.future.tools.ui

import com.future.sharednav.i18n.tr
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
    ToolEntry(FutureIcons.SwapHoriz, tr("ממיר יחידות"), tr("אורך, משקל, טמפרטורה, נפח"), ToolRoute.UnitConverter),
    ToolEntry(FutureIcons.Explore, tr("מצפן"), tr("צפון אמיתי לפי המיקום, קואורדינטות וגובה"), ToolRoute.Compass),
    ToolEntry(FutureIcons.Straighten, tr("פלס"), tr("איזון אופקי לפי חיישן תאוצה"), ToolRoute.Level),

    // מדידה וחיישנים
    ToolEntry(FutureIcons.GraphicEq, tr("מד רעש"), tr("עוצמת קול בדציבלים מהמיקרופון"), ToolRoute.NoiseMeter),
    ToolEntry(FutureIcons.WbSunny, tr("מד אור"), tr("עוצמת תאורה בלוקס מהחיישן הקדמי"), ToolRoute.LuxMeter),
    ToolEntry(FutureIcons.Architecture, tr("סרגל וזווית"), tr("סרגל וירטואלי ומד זווית הטיה"), ToolRoute.AngleRuler),
    ToolEntry(FutureIcons.MusicNote, tr("מכוון גיטרה"), tr("כיוון מיתרים לפי המיקרופון"), ToolRoute.GuitarTuner),

    // מחשבונים וממירים
    ToolEntry(FutureIcons.Receipt, tr("טיפים ופיצול חשבון"), tr("תשר וחלוקה בין סועדים"), ToolRoute.TipSplitCalculator),
    ToolEntry(FutureIcons.Percent, tr("מחשבון פיננסי"), tr("הנחות, מע\"מ והחזרי הלוואה"), ToolRoute.QuickFinanceCalculator),
    ToolEntry(FutureIcons.Public, tr("ממיר אזורי זמן"), tr("השעה הנוכחית בכל העולם"), ToolRoute.TimeZoneConverter),

    // פרודוקטיביות
    ToolEntry(FutureIcons.QrCodeScanner, tr("סורק קודים"), tr("QR וברקוד ללא פרסומות"), ToolRoute.QrScanner),
    ToolEntry(FutureIcons.LocalCafe, tr("פומודורו"), tr("מחזורי מיקוד והפסקה"), ToolRoute.Pomodoro),
    ToolEntry(FutureIcons.VpnKey, tr("מחולל סיסמאות"), tr("סיסמאות חזקות ואקראיות"), ToolRoute.PasswordGenerator),

    // כלי עזר אקראיים ופנאי
    ToolEntry(FutureIcons.Casino, tr("מטבע וקובייה"), tr("הטלת מטבע או קוביות"), ToolRoute.CoinDice),
    ToolEntry(FutureIcons.Shuffle, tr("בורר אקראי"), tr("בחירת אפשרות אקראית מרשימה"), ToolRoute.RandomPicker),
    ToolEntry(FutureIcons.Numbers, tr("מספר אקראי"), tr("הגרלת מספר בטווח שתבחר"), ToolRoute.RandomNumber),

    // שדרוגי AI
    ToolEntry(FutureIcons.DocumentScanner, tr("סורק טקסט"), tr("צילום מסמך והפיכתו לטקסט"), ToolRoute.TextScanner)
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

    CompositionLocalProvider(LocalLayoutDirection provides com.future.sharednav.i18n.Tr.layoutDirection) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(title = tr("כלים"), theme = theme)
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
            FutureMenuRow(tr("פתח"), entry.icon, theme, { menuFor = null; onOpen(entry.route) })
            FutureMenuRow(
                if (isPinned) tr("הסר ממסך הבית") else tr("הצמד למסך הבית"),
                FutureIcons.PushPin,
                theme,
                {
                    menuFor = null
                    ToolShortcuts.setPinnedToHome(context, entry.route, !isPinned)
                    Toast.makeText(
                        context,
                        if (!isPinned) tr("{0} נוסף כאפליקציה עצמאית - אפשר להוסיף אותו למסך הבית דרך \"הוספת אפליקציה\" בלאנצ'ר", entry.label)
                        else tr("{0} הוסר מרשימת האפליקציות העצמאיות", entry.label),
                        Toast.LENGTH_LONG
                    ).show()
                },
            )
        }
    }
}
