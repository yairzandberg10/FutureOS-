package com.future.music.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import com.future.music.data.Song
import com.future.music.jam.JamItem
import com.future.music.jam.JamMember
import com.future.music.jam.JamPhase
import com.future.music.jam.JamSession
import com.future.music.jam.JamUiState
import com.future.music.ui.components.ConfirmDialog
import com.future.music.ui.components.ScreenTopBar
import com.future.music.ui.components.formatDuration
import com.future.sharednav.components.EmptyState
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.FutureCard
import com.future.sharednav.components.FutureCheckbox
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.components.FutureProgressBar
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureSpinner
import com.future.sharednav.components.FutureSwitch
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.components.InputDialog
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.nav.digitForKey
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.subtleTextColor
import kotlinx.coroutines.delay

/**
 * מסך הג'אם. שלושה מצבים: לפני (פתיחה / הצטרפות עם קוד), התחברות, ובתוך
 * ג'אם - הקוד לשיתוף, מה מתנגן, והתור המשותף. הכול במקשים: ספרות לפעולות
 * המהירות, OK על שיר בתור לאפשרויות שלו.
 */
@Composable
fun JamScreen(
    theme: FutureTheme,
    onBack: () -> Unit,
    onOpenAdd: () -> Unit,
    onOpenMembers: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    carry: () -> JamSession.Carry?,
) {
    val jam = JamSession.state
    when {
        !JamSession.configured -> Column(modifier = Modifier.fillMaxSize()) {
            ScreenTopBar(title = "ג'אם", theme = theme, onBack = onBack)
            EmptyState(
                icon = FutureIcons.Groups,
                title = "הג'אם עוד לא מחובר",
                subtitle = "צריך לחבר את המוזיקה ל-Firebase של Messages. ההוראות ב-Music/README.md",
                textColor = theme.textColor,
            )
        }
        jam.phase == JamPhase.IDLE -> JamStart(theme = theme, onBack = onBack, carry = carry)
        jam.phase == JamPhase.CONNECTING -> Column(modifier = Modifier.fillMaxSize()) {
            ScreenTopBar(title = "ג'אם", theme = theme, onBack = onBack)
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                FutureSpinner(theme = theme, label = "מתחבר לג'אם…")
            }
        }
        else -> JamActive(
            theme = theme,
            jam = jam,
            onBack = onBack,
            onOpenAdd = onOpenAdd,
            onOpenMembers = onOpenMembers,
            onOpenNowPlaying = onOpenNowPlaying,
        )
    }
}

private enum class AfterName { CREATE, JOIN }

@Composable
private fun JamStart(theme: FutureTheme, onBack: () -> Unit, carry: () -> JamSession.Carry?) {
    var askName by remember { mutableStateOf(false) }
    var afterName by remember { mutableStateOf<AfterName?>(null) }
    var askCode by remember { mutableStateOf(false) }

    // שם קודם: הוא מה שהחברים רואים ליד כל שיר שהוספת.
    fun start() {
        if (JamSession.name.isBlank()) {
            afterName = AfterName.CREATE
            askName = true
        } else {
            JamSession.create(carry())
        }
    }
    fun join() {
        if (JamSession.name.isBlank()) {
            afterName = AfterName.JOIN
            askName = true
        } else {
            askCode = true
        }
    }
    fun rename() {
        afterName = null
        askName = true
    }

    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (digitForKey(event.key)) {
                    "1" -> { start(); true }
                    "2" -> { join(); true }
                    "3" -> { rename(); true }
                    else -> false
                }
            }
    ) {
        ScreenTopBar(title = "ג'אם", theme = theme, onBack = onBack)
        Text(
            "האזנה משותפת עם חברים: כולם מוסיפים שירים מהטלפון שלהם לאותו תור, ושומעים אותם יחד.",
            color = theme.mutedTextColor,
            fontSize = FutureTypography.body,
            modifier = Modifier.padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(FutureDimens.itemSpacing),
        ) {
            FutureListItem(
                title = "פתיחת ג'אם",
                summary = "מקבלים קוד ומשתפים אותו",
                theme = theme,
                onClick = ::start,
                focusRequester = firstFocus,
                leading = { FutureAvatar(theme = theme, icon = FutureIcons.Groups) },
                trailing = { DigitHint("1", theme) },
            )
            FutureListItem(
                title = "הצטרפות לג'אם",
                summary = "מקלידים את הקוד של מי שפתח",
                theme = theme,
                onClick = ::join,
                leading = { FutureAvatar(theme = theme, icon = FutureIcons.PersonAdd) },
                trailing = { DigitHint("2", theme) },
            )
            FutureListItem(
                title = "השם שלי בג'אם",
                summary = JamSession.name.ifBlank { "עוד לא נבחר" },
                theme = theme,
                onClick = ::rename,
                leading = { AvatarFor(JamSession.name, FutureIcons.Person, theme) },
                trailing = { DigitHint("3", theme) },
            )
        }
    }

    if (askName) {
        InputDialog(
            title = "השם שלך בג'אם",
            theme = theme,
            initialValue = JamSession.name,
            placeholder = "השם שהחברים יראו",
            confirmLabel = if (afterName == null) "שמור" else "המשך",
            onDismiss = {
                askName = false
                afterName = null
            },
            onConfirm = { value ->
                JamSession.updateName(value)
                askName = false
                when (afterName) {
                    AfterName.CREATE -> JamSession.create(carry())
                    AfterName.JOIN -> askCode = true
                    null -> Unit
                }
                afterName = null
            },
        )
    }
    if (askCode) {
        InputDialog(
            title = "קוד הג'אם",
            theme = theme,
            placeholder = "6 ספרות",
            confirmLabel = "הצטרפות",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            onDismiss = { askCode = false },
            onConfirm = { code ->
                askCode = false
                JamSession.join(code)
            },
        )
    }
}

@Composable
private fun JamActive(
    theme: FutureTheme,
    jam: JamUiState,
    onBack: () -> Unit,
    onOpenAdd: () -> Unit,
    onOpenMembers: () -> Unit,
    onOpenNowPlaying: () -> Unit,
) {
    var menuItem by remember { mutableStateOf<JamItem?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }

    // מיקום חי בשיר - מהנגן כשהמכשיר משמיע, אחרת לפי השעון המשותף.
    var position by remember { mutableLongStateOf(JamSession.positionNow()) }
    LaunchedEffect(jam.playback, jam.current?.id) {
        while (true) {
            position = JamSession.positionNow()
            delay(500)
        }
    }

    val nowFocus = remember { FocusRequester() }
    val addFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { nowFocus.requestFocus() } }

    val current = jam.current
    val upcoming = jam.upcoming

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (digitForKey(event.key)) {
                    "1" -> { onOpenAdd(); true }
                    "2" -> { onOpenMembers(); true }
                    "3" -> { showSettings = true; true }
                    "4" -> { JamSession.previous(); true }
                    "5" -> { JamSession.togglePlay(); true }
                    "6" -> { JamSession.next(); true }
                    "0" -> { confirmLeave = true; true }
                    else -> false
                }
            }
    ) {
        ScreenTopBar(
            title = if (jam.isHost) "הג'אם שלי" else "הג'אם של ${jam.hostName}",
            theme = theme,
            onBack = onBack,
            trailingIcon = FutureIcons.Groups,
            trailingContentDescription = "משתתפים (2)",
            onTrailingClick = onOpenMembers,
        )

        FutureCard(theme = theme) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.spacingLg, vertical = FutureDimens.spacingSm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("קוד הצטרפות", color = theme.mutedTextColor, fontSize = FutureTypography.label)
                    Text(
                        formatJamCode(jam.code),
                        color = theme.textColor,
                        fontSize = FutureTypography.headline,
                        fontWeight = FutureTypography.weightBold,
                        letterSpacing = 2.sp,
                        // "482 915" בתוך פסקה RTL היה מתהפך ל-"915 482".
                        style = TextStyle(textDirection = TextDirection.Ltr),
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(membersLabel(jam.members.size), color = theme.textColor, fontSize = FutureTypography.body)
                    Text(
                        if (jam.open) "פתוח להצטרפות" else "סגור להצטרפות",
                        color = theme.mutedTextColor,
                        fontSize = FutureTypography.summary,
                    )
                }
            }
        }

        FutureListItem(
            title = current?.title ?: "עוד לא מתנגן כלום",
            summary = current?.let { item -> listOf(item.artist, "הוסיף/ה: ${item.addedByName}").filter { it.isNotBlank() }.joinToString(" · ") }
                ?: if (jam.queue.isEmpty()) "הוסיפו שירים לתור (1)" else "השיר הראשון עוד עולה",
            theme = theme,
            onClick = onOpenNowPlaying,
            focusRequester = nowFocus,
            modifier = Modifier.padding(horizontal = FutureDimens.screenPadding),
            leading = {
                FutureAvatar(
                    theme = theme,
                    icon = if (jam.playback.playing) FutureIcons.Equalizer else FutureIcons.Pause,
                )
            },
            trailing = {
                if (current != null) {
                    Text(formatDuration(position), color = theme.subtleTextColor, fontSize = FutureTypography.summary)
                }
            },
        )
        if (current != null && current.durationMs > 0) {
            FutureProgressBar(
                progress = (position.toFloat() / current.durationMs).coerceIn(0f, 1f),
                theme = theme,
                mini = true,
                modifier = Modifier.padding(horizontal = FutureDimens.screenPadding + FutureDimens.spacingLg, vertical = FutureDimens.spacingXs),
            )
        }
        if (!jam.listening) {
            Text(
                "המכשיר הזה לא משמיע את הג'אם - רק שלט ותור (3 להגדרות)",
                color = theme.mutedTextColor,
                fontSize = FutureTypography.summary,
                modifier = Modifier.padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingXs),
            )
        }

        FutureSectionHeader("הבא בתור", theme)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = FutureDimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(FutureDimens.itemSpacing),
        ) {
            item(key = "add") {
                FutureListItem(
                    title = "הוספת שירים",
                    summary = "מהשירים שבטלפון שלך",
                    theme = theme,
                    onClick = onOpenAdd,
                    focusRequester = addFocus,
                    leading = { FutureAvatar(theme = theme, icon = FutureIcons.Add) },
                    trailing = { DigitHint("1", theme) },
                )
            }
            if (upcoming.isEmpty()) {
                item(key = "empty") {
                    Text(
                        "אין עוד שירים בתור",
                        color = theme.subtleTextColor,
                        fontSize = FutureTypography.body,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = FutureDimens.spacingLg),
                    )
                }
            }
            items(upcoming, key = { it.id }) { item ->
                val upload = jam.uploads[item.id]
                FutureListItem(
                    title = item.title,
                    summary = listOf(item.artist, item.addedByName).filter { it.isNotBlank() }.joinToString(" · "),
                    theme = theme,
                    onClick = {
                        if (jam.canEdit(item)) menuItem = item
                        else JamSession.notify("אפשר לשנות רק שירים שהוספת")
                    },
                    leading = { AvatarFor(item.addedByName, FutureIcons.MusicNote, theme) },
                    trailing = {
                        when {
                            upload != null -> Text("$upload%", color = theme.readableAccentColor, fontSize = FutureTypography.summary)
                            !item.ready -> Text("עולה…", color = theme.subtleTextColor, fontSize = FutureTypography.summary)
                            else -> Text(formatDuration(item.durationMs), color = theme.subtleTextColor, fontSize = FutureTypography.summary)
                        }
                    },
                )
            }
        }
        Text(
            "1 הוספה · 2 משתתפים · 3 הגדרות · 4/5/6 ניגון · 0 יציאה",
            color = theme.subtleTextColor,
            fontSize = FutureTypography.caption,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = FutureDimens.spacingSm),
        )
    }

    menuItem?.let { item ->
        val index = upcoming.indexOfFirst { it.id == item.id }
        val close = { menuItem = null }
        FutureOptionsMenu(theme = theme, onDismissRequest = close, header = item.title) {
            if (jam.canControl) {
                FutureMenuRow("נגן עכשיו", FutureIcons.PlayArrow, theme, onClick = {
                    JamSession.playNow(item)
                    close()
                })
                if (index > 0) {
                    FutureMenuRow("הזזה למעלה", FutureIcons.ArrowUpward, theme, onClick = {
                        JamSession.move(item, up = true)
                        close()
                    })
                }
                if (index in 0 until upcoming.lastIndex) {
                    FutureMenuRow("הזזה למטה", FutureIcons.ArrowDownward, theme, onClick = {
                        JamSession.move(item, up = false)
                        close()
                    })
                }
            }
            FutureMenuRow("הסרה מהתור", FutureIcons.Delete, theme, destructive = true, onClick = {
                JamSession.remove(item)
                close()
                // השורה שהייתה בפוקוס נעלמת - הפוקוס עובר לשורת ההוספה.
                runCatching { addFocus.requestFocus() }
            })
        }
    }

    if (showSettings) {
        FutureOptionsMenu(theme = theme, onDismissRequest = { showSettings = false }, header = "הגדרות הג'אם") {
            FutureMenuRow(
                "האזנה במכשיר הזה",
                FutureIcons.Headphones,
                theme,
                onClick = { JamSession.setListening(!jam.listening) },
                trailing = { FutureSwitch(jam.listening, theme) },
            )
            if (jam.isHost) {
                FutureMenuRow(
                    "כולם שולטים בניגון",
                    FutureIcons.SettingsRemote,
                    theme,
                    onClick = { JamSession.setGuestsControl(!jam.guestsControl) },
                    trailing = { FutureSwitch(jam.guestsControl, theme) },
                )
                FutureMenuRow(
                    "פתוח להצטרפות",
                    FutureIcons.PersonAdd,
                    theme,
                    onClick = { JamSession.setOpen(!jam.open) },
                    trailing = { FutureSwitch(jam.open, theme) },
                )
            }
            FutureMenuRow(
                if (jam.isHost) "סיום הג'אם" else "יציאה מהג'אם",
                FutureIcons.Close,
                theme,
                destructive = true,
                onClick = {
                    showSettings = false
                    confirmLeave = true
                },
            )
        }
    }

    if (confirmLeave) {
        ConfirmDialog(
            title = if (jam.isHost) "סיום הג'אם" else "יציאה מהג'אם",
            message = if (jam.isHost) "הג'אם ייסגר לכל המשתתפים, והשירים שהועלו יימחקו."
            else "אפשר לחזור עם אותו קוד כל עוד הג'אם פתוח.",
            theme = theme,
            confirmLabel = if (jam.isHost) "סיום" else "יציאה",
            onDismiss = { confirmLeave = false },
            onConfirm = {
                confirmLeave = false
                JamSession.leave()
            },
        )
    }
}

/** הוספת שירים מהספרייה לתור המשותף. OK מוסיף; שדה הסינון למעלה. */
@Composable
fun JamAddSongsScreen(allSongs: List<Song>, theme: FutureTheme, onBack: () -> Unit) {
    var filter by remember { mutableStateOf("") }
    val added = remember { mutableStateListOf<Long>() }
    val shown = remember(filter, allSongs) {
        val q = filter.trim()
        if (q.isEmpty()) allSongs else allSongs.filter { it.title.contains(q, true) || it.artist.contains(q, true) }
    }

    Column(modifier = Modifier.fillMaxSize().escapeTextFieldFocusTrap()) {
        ScreenTopBar(title = "הוספה לג'אם", theme = theme, onBack = onBack)
        FutureTextField(
            value = filter,
            onValueChange = { filter = it },
            theme = theme,
            placeholder = "סינון שירים",
            autoFocus = true,
            leading = {
                Icon(FutureIcons.Search, contentDescription = null, tint = theme.mutedTextColor, modifier = Modifier.size(FutureDimens.iconTopBar))
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
        )
        if (shown.isEmpty()) {
            EmptyState(
                icon = FutureIcons.MusicNote,
                title = if (allSongs.isEmpty()) "לא נמצאה מוזיקה בטלפון" else "אין שירים שמתאימים לסינון",
                textColor = theme.textColor,
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = FutureDimens.screenPadding),
                verticalArrangement = Arrangement.spacedBy(FutureDimens.itemSpacing),
            ) {
                items(shown, key = { it.id }) { song ->
                    FutureListItem(
                        title = song.title,
                        summary = song.artist,
                        theme = theme,
                        onClick = {
                            if (song.id in added) {
                                JamSession.notify("השיר כבר בתור")
                            } else {
                                added += song.id
                                JamSession.addSongs(listOf(song))
                            }
                        },
                        trailing = { FutureCheckbox(song.id in added, theme) },
                    )
                }
            }
        }
    }
}

/** המשתתפים. המארח יכול להסיר משתתף (OK על השורה). */
@Composable
fun JamMembersScreen(theme: FutureTheme, onBack: () -> Unit) {
    val jam = JamSession.state
    var removing by remember { mutableStateOf<JamMember?>(null) }
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(title = "משתתפים", theme = theme, onBack = onBack)
        if (jam.open && jam.code != null) {
            Text(
                "חברים מצטרפים עם הקוד ${formatJamCode(jam.code)}",
                color = theme.mutedTextColor,
                fontSize = FutureTypography.body,
                modifier = Modifier.padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = FutureDimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(FutureDimens.itemSpacing),
        ) {
            items(jam.members, key = { it.uid }) { member ->
                val isMe = member.uid == jam.myUid
                FutureListItem(
                    title = if (isMe) "${member.name} (אני)" else member.name,
                    summary = if (member.uid == jam.hostUid) "מארח/ת" else "משתתף/ת",
                    theme = theme,
                    onClick = { if (jam.isHost && !isMe) removing = member },
                    focusRequester = if (member == jam.members.firstOrNull()) firstFocus else null,
                    leading = { AvatarFor(member.name, FutureIcons.Person, theme) },
                )
            }
        }
    }

    removing?.let { member ->
        ConfirmDialog(
            title = "הסרה מהג'אם",
            message = "להסיר את ${member.name} מהג'אם? אפשר להצטרף שוב עם הקוד כל עוד הג'אם פתוח.",
            theme = theme,
            confirmLabel = "הסרה",
            onDismiss = { removing = null },
            onConfirm = {
                JamSession.kick(member)
                removing = null
            },
        )
    }
}

/** ראשי התיבות של השם (כמו ב-Spotify, מי הוסיף), ואייקון רק כשאין שם. */
@Composable
private fun AvatarFor(name: String, fallback: androidx.compose.ui.graphics.vector.ImageVector, theme: FutureTheme) {
    if (name.isBlank()) FutureAvatar(theme = theme, icon = fallback) else FutureAvatar(theme = theme, name = name)
}

@Composable
private fun DigitHint(digit: String, theme: FutureTheme) {
    Text(digit, color = theme.subtleTextColor, fontSize = FutureTypography.body, fontWeight = FutureTypography.weightBold)
}

private fun formatJamCode(code: String?): String =
    if (code == null || code.length != 6) code.orEmpty() else "${code.take(3)} ${code.drop(3)}"

private fun membersLabel(count: Int): String = if (count == 1) "משתתף אחד" else "$count משתתפים"
