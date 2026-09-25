package com.future.wallpapers
import com.future.sharednav.systemui.StatusBarInset

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.os.Bundle
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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

/**
 * רקעים: רשת של תמונות ממוזערות (שלוש בשורה), סינון לפי קטגוריה, OK פותח
 * תצוגה מלאה ומשם מקש Options - הגדר למסך הבית / לנעילה / לשניהם. אין
 * לה אייקון בלאנצ'ר; נפתחת מעריכת מסך הבית ומהגדרות הלאנצ'ר.
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
    var wallpapers by remember {
        mutableStateOf(WallpaperCatalog.cached(context).ifEmpty { WallpaperCatalog.builtIn })
    }
    var category by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<Wallpaper?>(null) }
    var lastFocused by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (WallpaperCatalog.isFirebaseConfigured(context)) {
            withContext(Dispatchers.IO) { runCatching { WallpaperCatalog.fetch(context) } }
                .getOrNull()?.takeIf { it.isNotEmpty() }?.let { wallpapers = it }
        }
    }
    BackHandler(enabled = preview != null) { preview = null }

    val categories = remember(wallpapers) { wallpapers.map { it.category }.distinct() }
    val shown = remember(wallpapers, category) { if (category == null) wallpapers else wallpapers.filter { it.category == category } }

    AnimatedScreenHost(targetState = preview, depthOf = { if (it == null) 0 else 1 }, contentKey = { it?.id }) { open ->
        if (open != null) {
            PreviewScreen(open, theme)
        } else {
            Column(modifier = Modifier.fillMaxSize().background(theme.backgroundColor).padding(top = StatusBarInset.TITLE_GAP_DP.dp)) {
                ScreenTopBar(title = "רקעים", textColor = theme.textColor, accentColor = theme.accentColor)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = FutureDimens.spacingLg),
                    horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
                ) {
                    item { FutureChip("הכל", theme = theme, selected = category == null, onClick = { category = null }) }
                    items(categories) { c -> FutureChip(c, theme = theme, selected = category == c, onClick = { category = c }) }
                }
                val first = remember { FocusRequester() }
                LaunchedEffect(shown.firstOrNull()?.id) { runCatching { first.requestFocus() } }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(shown, key = { _, w -> w.id }) { index, wallpaper ->
                        Thumbnail(
                            wallpaper = wallpaper,
                            theme = theme,
                            focusRequester = if (wallpaper.id == lastFocused || (lastFocused == null && index == 0)) first else null,
                            onClick = { lastFocused = wallpaper.id; preview = wallpaper },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Thumbnail(wallpaper: Wallpaper, theme: FutureTheme, focusRequester: FocusRequester?, onClick: () -> Unit) {
    val context = LocalContext.current
    val bitmap by produceState(ImageLoader.peek(wallpaper.thumbUrl), wallpaper.thumbUrl) {
        value = withContext(Dispatchers.IO) { ImageLoader.load(context, wallpaper.thumbUrl, 300) }
    }
    FocusableItem(
        onClick = onClick,
        accentColor = theme.accentColor,
        idleBackgroundColor = theme.idleChipColor,
        borderWidth = FutureDimens.focusBorderControl,
        cornerRadius = FutureShapes.radiusLg,
        contentPadding = 3.dp,
        focusRequester = focusRequester,
        modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
    ) {
        val b = bitmap
        if (b != null) {
            Image(b.asImageBitmap(), contentDescription = wallpaper.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(FutureShapes.sm))
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

/** תצוגה מלאה: OK - הגדר למסך הבית; Options - בחירה בין בית, נעילה ושניהם. */
@Composable
private fun PreviewScreen(wallpaper: Wallpaper, theme: FutureTheme) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = rememberFutureSnackbarState()
    var bitmap by remember { mutableStateOf<Bitmap?>(ImageLoader.peek(wallpaper.url)) }
    var failed by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var applying by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val darkTheme = remember(theme.accentColor) { FutureTheme(isDarkMode = true, accentColor = theme.accentColor) }

    LaunchedEffect(wallpaper.url) {
        if (bitmap == null) {
            bitmap = withContext(Dispatchers.IO) { ImageLoader.load(context, wallpaper.url, 1400) }
            failed = bitmap == null
        }
        runCatching { focus.requestFocus() }
    }
    onOptionsKeyPress { if (bitmap != null) menuOpen = !menuOpen }

    fun apply(target: Target) {
        val b = bitmap ?: return
        applying = true
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { WallpaperManager.getInstance(context).setBitmap(b, null, true, target.flags) }.isSuccess
            }
            applying = false
            snackbar.show(if (ok) "הרקע הוגדר ל${target.label}" else "לא ניתן להגדיר את הרקע")
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
                if (event.type == KeyEventType.KeyUp && isOk && !applying) {
                    apply(Target.HOME)
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
        Text(
            if (b != null) "OK - מסך הבית · Options - עוד" else "",
            color = Color.White,
            fontSize = FutureTypography.summary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .background(darkTheme.idleChipColor.copy(alpha = 0.6f), FutureShapes.pill)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
        FutureSnackbarHost(snackbar, darkTheme)
    }

    if (menuOpen) {
        FutureOptionsMenu(theme = theme, onDismissRequest = { menuOpen = false }, header = wallpaper.title) {
            Target.entries.forEach { target ->
                FutureMenuRow("הגדר ל${target.label}", FutureIcons.Image, theme, { menuOpen = false; apply(target) })
            }
        }
    }
}
