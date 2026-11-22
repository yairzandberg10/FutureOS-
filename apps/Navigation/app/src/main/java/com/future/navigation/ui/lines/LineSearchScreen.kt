package com.future.navigation.ui.lines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.future.navigation.data.common.LatLng
import com.future.navigation.data.lines.LineDeparture
import com.future.navigation.data.lines.LineHistory
import com.future.navigation.data.lines.LineLogic
import com.future.navigation.data.lines.LineTimes
import com.future.navigation.data.lines.SavedLine
import com.future.navigation.data.lines.toSaved
import com.future.navigation.data.location.LocationHelper
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.nav.onStarKeyPress
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.rememberFutureType
import kotlinx.coroutines.launch

/** שורה ברשימה: קו שנשמר/נפתח, ומזהה נסיעה אם כבר ידוע (מתוצאות החיפוש). */
private data class LineRow(
    val saved: SavedLine,
    val tripId: String?,
    val section: String? = null,
)

/**
 * "קווים" (בסגנון אגד): חיפוש קו לפי מספר בין היציאות הקרובות למיקום, וכן "מועדפים"
 * ו"לאחרונה". הכתיבה לשדה החיפוש היא ספרות רגילות. חץ למטה מעביר מהשדה לרשימה,
 * וחץ למעלה מהשורה הראשונה חוזר לשדה. OK פותח את הקו. * על שורה פותח תפריט:
 * מועדף, או הסרה מ"לאחרונה".
 */
@Composable
fun LineSearchScreen(
    onBack: () -> Unit,
    onOpenLine: (tripId: String, line: String, stopName: String) -> Unit,
) {
    val context = LocalContext.current
    val theme = LocalFutureTheme.current
    val type = rememberFutureType()
    val scope = rememberCoroutineScope()
    val history = remember { LineHistory.get(context) }
    val recent by history.recent.collectAsState()
    val favorites by history.favorites.collectAsState()

    var query by remember { mutableStateOf("") }
    var here by remember { mutableStateOf<LatLng?>(null) }
    var nearby by remember { mutableStateOf<List<LineDeparture>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var opening by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var menuFor by remember { mutableStateOf<SavedLine?>(null) }
    // השורה שבפוקוס כרגע (ל-*). מתאפסת כשהפוקוס יוצא ממנה
    var focusedRow by remember { mutableStateOf<LineRow?>(null) }
    val fieldFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { fieldFocus.requestFocus() }
        val location = LocationHelper.lastKnownLatLon(context)?.let { LatLng(it.first, it.second) }
        here = location
        nearby = location?.let { LineTimes.nearby(it) }
        loading = false
    }

    val rows = remember(query, nearby, favorites, recent) {
        val now = System.currentTimeMillis()
        if (query.isBlank()) {
            section("מועדפים", favorites.map { LineRow(it, null) }) +
                section("לאחרונה", recent.map { LineRow(it, null) })
        } else {
            val hits = LineLogic.distinctLines(nearby.orEmpty(), query, here, now)
            section("תוצאות", hits.map { LineRow(it.toSaved(), it.tripId) })
        }
    }

    // * מגיע כשידור (לא כ-KeyEvent). תפריט רק כשיש שורה בפוקוס, לא מתוך שדה החיפוש.
    com.future.navigation.ui.common.onStarKey {
        if (menuFor == null) focusedRow?.let { menuFor = it.saved }
    }

    fun refocusField() {
        runCatching { fieldFocus.requestFocus() }
    }

    fun open(row: LineRow) {
        if (row.tripId != null) {
            onOpenLine(row.tripId, row.saved.line, row.saved.stopName)
            return
        }
        if (opening) return
        scope.launch {
            opening = true
            notice = "מחפש את הנסיעה הנוכחית…"
            val tripId = LineTimes.resolveTrip(row.saved, System.currentTimeMillis())
            opening = false
            if (tripId == null) {
                notice = "הקו הזה לא נמצא עכשיו ליד התחנה שלו"
            } else {
                notice = null
                onOpenLine(tripId, row.saved.line, row.saved.stopName)
            }
        }
    }

    val status = when {
        notice != null -> notice.orEmpty()
        query.isBlank() && rows.isEmpty() -> "אין קווים אחרונים או מועדפים. הקלידו מספר קו לחיפוש"
        query.isBlank() -> "* על קו לאפשרויות · OK לפתיחה"
        here == null -> "אין מיקום - חיפוש קו לפי מספר דורש את המיקום שלך"
        loading -> "מחפש קווים ליד מיקומך…"
        nearby == null -> "אין חיבור לשרת - לא ניתן לחפש קו עכשיו"
        rows.isEmpty() -> "אין קו $query ביציאות הקרובות ליד מיקומך"
        else -> "* על קו לאפשרויות · OK לפתיחה"
    }

    Column(
        modifier = Modifier.fillMaxSize().escapeTextFieldFocusTrap(),
    ) {
        ScreenTopBar(
            title = "קווים",
            textColor = MaterialTheme.colorScheme.onBackground,
            accentColor = MaterialTheme.colorScheme.primary,
            onBack = onBack,
        )
        FutureTextField(
            value = query,
            onValueChange = { query = it.take(MAX_QUERY_LENGTH) },
            theme = theme,
            placeholder = "חיפוש קו לפי מספר",
            focusRequester = fieldFocus,
            leading = {
                Icon(
                    FutureIcons.Search,
                    contentDescription = null,
                    tint = theme.mutedTextColor,
                    modifier = Modifier.size(FutureDimens.iconTopBar),
                )
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.screenPadding),
        )
        Text(
            status,
            color = theme.mutedTextColor,
            fontSize = type.summary,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingXs),
        )

        if (rows.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
                verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingXs),
            ) {
                itemsIndexed(rows) { _, row ->
                    Column {
                        row.section?.let { FutureSectionHeader(it, theme, inset = false) }
                        FutureListItem(
                            title = "קו ${row.saved.line}" + (row.saved.headsign.takeIf { it.isNotBlank() }?.let { " · לכיוון $it" } ?: ""),
                            summary = listOfNotNull(
                                row.saved.agency,
                                row.saved.stopName.takeIf { it.isNotBlank() }?.let { "תחנה: $it" },
                            ).joinToString(" · ").ifBlank { null },
                            theme = theme,
                            onClick = { open(row) },
                            modifier = Modifier.onFocusChanged { state ->
                                if (state.hasFocus) {
                                    focusedRow = row
                                } else if (focusedRow == row) {
                                    focusedRow = null
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    // תפריט *: מועדף, או הסרה מ"לאחרונה" (רק לקו שנמצא שם)
    menuFor?.let { target ->
        val isFavorite = favorites.any { it.key == target.key }
        val inRecent = recent.any { it.key == target.key }
        FutureOptionsMenu(
            theme = theme,
            onDismissRequest = { menuFor = null },
            header = "קו ${target.line}" + (target.headsign.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
        ) {
            FutureMenuRow(
                if (isFavorite) "הסר ממועדפים" else "הוסף למועדפים",
                if (isFavorite) FutureIcons.Star else FutureIcons.StarBorder,
                theme,
                {
                    history.toggleFavorite(target)
                    menuFor = null
                    refocusField()
                },
            )
            if (inRecent) {
                FutureMenuRow("הסר מהאחרונים", FutureIcons.Delete, theme, {
                    history.removeRecent(target)
                    menuFor = null
                    refocusField()
                }, destructive = true)
            }
        }
    }
}

/** מוסיף כותרת קטע לשורה הראשונה של הקטע. */
private fun section(title: String, items: List<LineRow>): List<LineRow> =
    items.mapIndexed { index, row -> if (index == 0) row.copy(section = title) else row }

private const val MAX_QUERY_LENGTH = 6
