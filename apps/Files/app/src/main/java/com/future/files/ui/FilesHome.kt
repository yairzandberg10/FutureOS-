package com.future.files.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import com.future.files.data.FileCategory
import com.future.files.data.FileRepository
import com.future.files.data.HomeCategory
import com.future.files.data.ScannedFile
import com.future.files.data.categorize
import com.future.sharednav.components.FutureActionCell
import com.future.sharednav.components.FutureCard
import com.future.sharednav.components.FutureDivider
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureSettingItem
import com.future.sharednav.components.FutureSpinner
import com.future.sharednav.components.ScreenScaffold
import com.future.sharednav.i18n.tr
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme

/**
 * מסך הבית של הקבצים: אחרונים, קטגוריות ואחסון - כמו "הקבצים שלי", אבל בנוי
 * רק מרכיבי המערכת: כותרת המסך (ScreenScaffold), כותרות קטע, כרטיס עם
 * שורות הגדרה (FutureCard + FutureSettingItem) ותאי פעולה (FutureActionCell)
 * לקטגוריות. כל מה שכאן נבחר במקשים: חצים ו-OK.
 */
@Composable
fun FilesHomeScreen(
    theme: FutureTheme,
    repository: FileRepository,
    /** null - הסריקה עוד רצה. */
    recents: List<ScannedFile>?,
    storage: Pair<Long, Long>?,
    pickMode: Boolean,
    /** המפתח של הפריט שממנו יצאנו (קטגוריה / "storage" / נתיב קובץ) - הפוקוס חוזר אליו. */
    lastFocusKey: String?,
    onOpenRecent: (ScannedFile) -> Unit,
    onOpenCategory: (HomeCategory) -> Unit,
    onOpenStorage: () -> Unit,
) {
    val requesters = remember { mutableMapOf<String, FocusRequester>() }
    fun requester(key: String) = requesters.getOrPut(key) { FocusRequester() }
    val firstKey = recents?.firstOrNull()?.entry?.file?.absolutePath ?: "cat:${HomeCategory.entries.first().name}"
    // מישהו במסך כבר ממוקד - לא לוקחים ממנו את הפוקוס כשהסריקה מסתיימת.
    var homeHasFocus by remember { mutableStateOf(false) }
    LaunchedEffect(recents == null) {
        if (homeHasFocus) return@LaunchedEffect
        val wanted = lastFocusKey?.takeIf { it in requesters } ?: firstKey
        repeat(10) {
            if (runCatching { requester(wanted).requestFocus() }.isSuccess) return@LaunchedEffect
            kotlinx.coroutines.delay(30)
        }
    }

    ScreenScaffold(
        backgroundColor = theme.backgroundColor,
        title = if (pickMode) tr("בחירת קובץ") else tr("הקבצים שלי"),
        textColor = theme.textColor,
        accentColor = theme.accentColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .onFocusChanged { homeHasFocus = it.hasFocus }
                .verticalScroll(rememberScrollState())
                .padding(bottom = FutureDimens.spacingLg),
        ) {
            // אחרונים
            FutureSectionHeader(tr("אחרונים"), theme)
            when {
                recents == null -> FutureCard(theme) {
                    FutureSpinner(theme = theme, label = tr("טוען"), modifier = Modifier.fillMaxWidth().padding(FutureDimens.spacingLg))
                }
                recents.isEmpty() -> FutureCard(theme) {
                    FutureSettingItem(title = tr("אין קבצים אחרונים"), theme = theme, onClick = null, icon = FutureIcons.History, showChevron = false)
                }
                else -> FutureCard(theme) {
                    recents.forEachIndexed { i, item ->
                        if (i > 0) FutureDivider(theme)
                        val file = item.entry.file
                        val summary = remember(item) {
                            ltr(android.text.format.DateFormat.format("dd/MM/yy HH:mm", item.modified)) + "  ·  " +
                                ltr(repository.formatSize(item.entry.sizeBytes))
                        }
                        FutureSettingItem(
                            title = file.name,
                            theme = theme,
                            onClick = { onOpenRecent(item) },
                            summary = summary,
                            icon = categoryIcon(categorize(file)),
                            showChevron = false,
                            focusRequester = requester(file.absolutePath),
                        )
                    }
                }
            }

            // קטגוריות - שתי שורות של שלושה תאים
            FutureSectionHeader(tr("קטגוריות"), theme)
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.spacingLg),
                verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
            ) {
                HomeCategory.entries.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm)) {
                        row.forEach { cat ->
                            FutureActionCell(
                                icon = homeCategoryIcon(cat),
                                label = cat.label,
                                theme = theme,
                                onClick = { onOpenCategory(cat) },
                                modifier = Modifier.weight(1f),
                                focusRequester = requester("cat:${cat.name}"),
                            )
                        }
                    }
                }
            }

            // אחסון
            FutureSectionHeader(tr("אחסון"), theme)
            FutureCard(theme) {
                FutureSettingItem(
                    title = tr("אחסון פנימי"),
                    theme = theme,
                    onClick = onOpenStorage,
                    summary = storage?.let { (used, total) ->
                        val percent = if (total > 0) (used * 100 / total).toInt() else 0
                        tr("{0} בשימוש מתוך {1}", ltr(repository.formatSize(used)), ltr(repository.formatSize(total))) +
                            "  ·  " + ltr("$percent%")
                    },
                    icon = FutureIcons.Storage,
                    focusRequester = requester("storage"),
                )
            }
            Spacer(modifier = Modifier.height(FutureDimens.spacingSm))
        }
    }
}

/** מבודד קטע משמאל לימין (LRI…PDI) - תאריכים, גדלים ואחוזים בתוך שורה בעברית. */
internal fun ltr(text: CharSequence): String = Char(0x2066).toString() + text + Char(0x2069)

private fun homeCategoryIcon(cat: HomeCategory): ImageVector = when (cat) {
    HomeCategory.IMAGES -> FutureIcons.Image
    HomeCategory.VIDEOS -> FutureIcons.Movie
    HomeCategory.AUDIO -> FutureIcons.MusicNote
    HomeCategory.DOCUMENTS -> FutureIcons.Description
    HomeCategory.DOWNLOADS -> FutureIcons.Download
    HomeCategory.SCREENSHOTS -> FutureIcons.Image
}

internal fun categoryIcon(category: FileCategory): ImageVector = when (category) {
    FileCategory.IMAGE -> FutureIcons.Image
    FileCategory.AUDIO -> FutureIcons.MusicNote
    FileCategory.VIDEO -> FutureIcons.Movie
    FileCategory.PDF -> FutureIcons.PictureAsPdf
    FileCategory.APK -> FutureIcons.Android
    else -> FutureIcons.Description
}
