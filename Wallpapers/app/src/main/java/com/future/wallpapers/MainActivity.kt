package com.future.wallpapers
import com.future.sharednav.components.ConfirmDialog
import com.future.sharednav.systemui.StatusBarInset

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.future.sharednav.components.AnimatedScreenHost
import com.future.sharednav.components.FutureChip
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.components.FutureSnackbarHost
import com.future.sharednav.components.FutureSpinner
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.components.rememberFutureSnackbarState
import com.future.sharednav.focus.FocusableItem
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.nav.onOptionsKeyPress
import com.future.sharednav.theme.FutureAppTheme
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.idleChipColor
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.rememberFutureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "Wallpapers"
/** גודל הממוזערת ברשת, וגודל התצוגה המלאה - לפי המסך 640x960. */
private const val ThumbPx = 300
private const val FullPx = 960

/**
 * רקעים: רשת של תמונות ממוזערות (שלוש בשורה), סינון לפי קטגוריה, OK פותח
 * תצוגה מלאה ומשם מקש Options - הגדר למסך הבית / לנעילה / לשניהם. מקש Options
 * ברשת פותח את התפריט של התמונה הממוקדת. אין לה אייקון בלאנצ'ר; נפתחת
 * מעריכת מסך הבית ומהגדרות הלאנצ'ר.
 */
class MainActivity : ComponentActivity() {
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val theme = rememberFutureTheme()
            FutureAppTheme(theme) { WallpapersApp(theme) }
        }
    }
}

@Composable
private fun WallpapersApp(theme: FutureTheme) {
    val context = LocalContext.current
    // הקטלוג המובנה מוצג מיד; המטמון וה-Firebase נטענים ברקע (W17) ומחליפים אותו.
    var wallpapers by remember { mutableStateOf(WallpaperCatalog.builtIn) }
    // W01: הקטגוריה, התצוגה המקדימה והפריט האחרון שורדים סיבוב מסך ושחזור תהליך.
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var previewId by rememberSaveable { mutableStateOf<String?>(null) }
    // Options ברשת פותח את התצוגה המלאה עם התפריט פתוח; OK פותח בלי תפריט.
    var previewMenu by rememberSaveable { mutableStateOf(false) }
    // OK על צ'יפ: הפוקוס עובר לתמונה הראשונה בקטגוריה החדשה (ולא נשאר על הצ'יפ).
    var chipPicked by rememberSaveable { mutableStateOf(false) }
    var lastFocused by rememberSaveable { mutableStateOf<String?>(null) }
    val preview = previewId?.let { id -> wallpapers.firstOrNull { it.id == id } }

    LaunchedEffect(Unit) {
        val cached = withContext(Dispatchers.IO) { WallpaperCatalog.cached(context) }
        if (cached.isNotEmpty()) wallpapers = cached
        if (withContext(Dispatchers.IO) { WallpaperCatalog.isFirebaseConfigured(context) }) {
            withContext(Dispatchers.IO) { runCatching { WallpaperCatalog.fetch(context) } }
                .getOrNull()?.takeIf { it.isNotEmpty() }?.let { wallpapers = it }
        }
    }
    BackHandler(enabled = preview != null) { previewId = null }

    val categories = remember(wallpapers) { wallpapers.map { it.category }.distinct() }
    val shown = remember(wallpapers, category) { if (category == null) wallpapers else wallpapers.filter { it.category == category } }

    // W19: Options ברשת הראשית פותח את התפריט (הגדר למסך הבית/הנעילה/שניהם) לתמונה הממוקדת
    onOptionsKeyPress {
        val id = lastFocused
        if (preview == null && id != null && shown.any { it.id == id }) {
            previewMenu = true
            previewId = id
        }
    }

    AnimatedScreenHost(targetState = preview, depthOf = { if (it == null) 0 else 1 }, contentKey = { it?.id }) { open ->
        if (open != null) {
            PreviewScreen(open, theme, openMenu = previewMenu)
        } else {
            Column(modifier = Modifier.fillMaxSize().background(theme.backgroundColor).padding(top = StatusBarInset.TITLE_GAP_DP.dp)) {
                ScreenTopBar(title = "רקעים", textColor = theme.textColor, accentColor = theme.accentColor)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = FutureDimens.spacingLg),
                    horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
                ) {
                    item { FutureChip("הכל", theme = theme, selected = category == null, onClick = { category = null; chipPicked = true }) }
                    items(categories) { c -> FutureChip(c, theme = theme, selected = category == c, onClick = { category = c; chipPicked = true }) }
                }
                val first = remember { FocusRequester() }
                val gridState = rememberLazyGridState()
                // כניסה למסך - הפוקוס לתמונה האחרונה שהייתה ממוקדת. OK על צ'יפ - לראש הרשת ולתמונה הראשונה.
                LaunchedEffect(category, shown.firstOrNull()?.id) {
                    runCatching {
                        if (chipPicked) gridState.scrollToItem(0)
                        first.requestFocus()
                    }
                    chipPicked = false
                }
                // W18: קטגוריה ריקה מקבלת הודעה, ולא רשת ריקה.
                if (shown.isEmpty()) {
                    com.future.sharednav.components.EmptyState(
                        icon = com.future.sharednav.icons.FutureIcons.Wallpaper,
                        title = "אין רקעים בקטגוריה הזו",
                        textColor = theme.textColor,
                    )
                    return@Column
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // אחרי מעבר קטגוריה הרקע האחרון שנבחר כבר לא ברשימה - אז אף תמונה לא
                    // קיבלה את ה-FocusRequester והפוקוס נשאר תקוע על הצ'יפים.
                    val focusId = if (chipPicked) {
                        shown.firstOrNull()?.id
                    } else {
                        lastFocused?.takeIf { id -> shown.any { it.id == id } } ?: shown.firstOrNull()?.id
                    }
                    itemsIndexed(shown, key = { _, w -> w.id }) { _, wallpaper ->
                        Thumbnail(
                            wallpaper = wallpaper,
                            theme = theme,
                            focusRequester = if (wallpaper.id == focusId) first else null,
                            onFocus = { lastFocused = wallpaper.id },
                            onClick = {
                                lastFocused = wallpaper.id
                                chipPicked = false
                                previewMenu = false
                                previewId = wallpaper.id
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Thumbnail(
    wallpaper: Wallpaper,
    theme: FutureTheme,
    focusRequester: FocusRequester?,
    onFocus: () -> Unit,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    // W03: טעינה שנכשלה מסומנת, ולא ספינר לנצח.
    var loadFailed by remember(wallpaper.thumbUrl) { mutableStateOf(false) }
    val bitmap by produceState(ImageLoader.peek(wallpaper.thumbUrl, ThumbPx), wallpaper.thumbUrl) {
        value = ImageLoader.load(context, wallpaper.thumbUrl, ThumbPx)
        loadFailed = value == null
    }
    FocusableItem(
        onClick = onClick,
        accentColor = theme.accentColor,
        idleBackgroundColor = theme.idleChipColor,
        borderWidth = FutureDimens.focusBorderControl,
        cornerRadius = FutureShapes.radiusLg,
        contentPadding = 3.dp,
        focusRequester = focusRequester,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .onFocusChanged { if (it.hasFocus) onFocus() },
    ) {
        val b = bitmap
        if (b != null) {
            Image(b.asImageBitmap(), contentDescription = wallpaper.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(FutureShapes.sm))
        } else if (loadFailed) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Text("לא נטען", color = theme.textColor.copy(alpha = 0.6f), fontSize = com.future.sharednav.theme.FutureTypography.caption)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { FutureSpinner(theme = theme) }
        }
    }
}

private enum class Target(val label: String, val flags: Int) {
    HOME("מסך הבית", WallpaperManager.FLAG_SYSTEM),
    LOCK("מסך הנעילה", WallpaperManager.FLAG_LOCK),
    BOTH("שניהם", WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK),
}

/**
 * תצוגה מלאה: OK - הגדר למסך הבית (אחרי אישור); Options - בחירה בין בית,
 * נעילה ושניהם. קודם OK החליף את הרקע מיד, והרקע הקודם אבד בלחיצה מקרית.
 * אחרי כשל טעינה OK מנסה שוב (N01).
 */
@Composable
private fun PreviewScreen(wallpaper: Wallpaper, theme: FutureTheme, openMenu: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = rememberFutureSnackbarState()
    var bitmap by remember { mutableStateOf<Bitmap?>(ImageLoader.peek(wallpaper.url, FullPx)) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var menuOpen by remember { mutableStateOf(openMenu) }
    var applying by remember { mutableStateOf(false) }
    var confirmHome by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val darkTheme = remember(theme.accentColor) { FutureTheme(isDarkMode = true, accentColor = theme.accentColor) }

    LaunchedEffect(wallpaper.url, retry) {
        if (bitmap == null) {
            failed = false
            bitmap = ImageLoader.load(context, wallpaper.url, FullPx)
            failed = bitmap == null
        }
        runCatching { focus.requestFocus() }
    }
    onOptionsKeyPress { if (bitmap != null) menuOpen = !menuOpen }

    fun apply(target: Target) {
        // גם מהתפריט - לא רק מ-OK - לא מתחילים הגדרה שנייה באמצע הראשונה
        if (applying) return
        val b = bitmap ?: return
        applying = true
        scope.launch {
            // W20: הכשל נרשם ביומן, והמשתמש מקבל הודעה שמתאימה לסיבה
            val result = withContext(Dispatchers.IO) {
                runCatching { WallpaperManager.getInstance(context).setBitmap(b, null, true, target.flags) }
                    .onFailure { Log.w(TAG, "הגדרת הרקע נכשלה", it) }
            }
            applying = false
            snackbar.show(
                when {
                    result.isSuccess -> "הרקע הוגדר ל${target.label}"
                    result.exceptionOrNull() is SecurityException -> "אין הרשאה להגדיר רקע"
                    else -> "לא ניתן להגדיר את הרקע"
                },
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { event ->
                val isOk = event.key == Key.DirectionCenter || event.key == Key.Enter
                if (event.type == KeyEventType.KeyUp && isOk && !applying && (bitmap != null || failed)) {
                    if (bitmap != null) confirmHome = true else {
                        failed = false
                        retry++
                    }
                    true
                } else false
            },
        contentAlignment = Alignment.Center,
    ) {
        val b = bitmap
        when {
            b != null -> Image(b.asImageBitmap(), contentDescription = wallpaper.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            failed -> Text("לא ניתן לטעון את התמונה", color = darkTheme.mutedTextColor, fontSize = FutureTypography.body)
            else -> FutureSpinner(theme = darkTheme)
        }
        if (applying) FutureSpinner(theme = darkTheme, label = "מגדיר רקע")
        // W04: הכתובית מוצגת רק כשיש לה תוכן (בלי פיל שקוף ריק)
        val hint = when {
            b != null -> "OK - מסך הבית · Options - עוד"
            failed -> "OK - נסה שוב"
            else -> null
        }
        if (hint != null) {
            Text(
                hint,
                color = Color.White,
                fontSize = FutureTypography.summary,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .background(darkTheme.idleChipColor.copy(alpha = 0.6f), FutureShapes.pill)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
        FutureSnackbarHost(snackbar, darkTheme)
    }

    if (confirmHome) {
        ConfirmDialog(
            message = "להגדיר את התמונה כרקע של מסך הבית?",
            theme = theme,
            confirmLabel = "הגדר",
            destructive = false,
            onCancel = { confirmHome = false },
            onConfirm = {
                confirmHome = false
                apply(Target.HOME)
            },
        )
    }

    if (menuOpen) {
        FutureOptionsMenu(theme = theme, onDismissRequest = { menuOpen = false }, header = wallpaper.title) {
            Target.entries.forEach { target ->
                FutureMenuRow("הגדר ל${target.label}", FutureIcons.Image, theme, { menuOpen = false; apply(target) })
            }
        }
    }
}
