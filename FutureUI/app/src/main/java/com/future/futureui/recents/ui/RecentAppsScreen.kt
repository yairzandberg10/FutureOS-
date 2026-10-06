package com.future.futureui.recents.ui

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import androidx.compose.ui.composed
import androidx.core.graphics.drawable.toBitmap
import com.future.futureui.recents.logic.RecentAppInfo
import com.future.sharednav.focus.animateFocusColor
import com.future.sharednav.focus.animateFocusDp

/**
 * "אפליקציות אחרונות" בפריסה של Redmi: רשת של שני חלונות בשורה - שם
 * האפליקציה ואייקון קטן מעל כל כרטיס, הכרטיס עצמו הוא צילום המסך האחרון של
 * האפליקציה (ר' RecentSnapshots; בלי צילום - צבע האפליקציה עם האייקון במרכז),
 * וכפתור X עגול שמרחף מעל הרשת בתחתית לסגירת הכל. בראש המסך - הזיכרון הפנוי.
 *
 * מקשים (RTL): חצים ברשת; OK פותח; Options/מחיקה סוגרים את החלון הממוקד;
 * 0 סוגר הכל; חץ למטה מהשורה האחרונה מגיע לכפתור X; BACK יוצא.
 */
@Composable
fun RecentAppsScreen(
    apps: List<RecentAppInfo>,
    snapshots: Map<String, ImageBitmap> = emptyMap(),
    onLaunch: (RecentAppInfo) -> Unit,
    onClose: (RecentAppInfo) -> Unit,
    onDismiss: () -> Unit,
    onCloseAll: () -> Unit = {},
    memorySummary: String? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    // -1 = כפתור "סגור הכל".
    var focusedIndex by remember { mutableIntStateOf(0) }
    val gridState = rememberLazyGridState()
    val focusRequester = remember { FocusRequester() }
    val clearFocused = focusedIndex == CLEAR_ALL && apps.isNotEmpty()

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // כניסה: הרקע מתכהה והכרטיסים עולים מלמטה בגל לפי שורה (riseIn).
    // יציאה ב-BACK: הכול נסוג מעט ונעלם לפני שהחלון מוסר; "סגור הכל": הרשת
    // עפה למעלה. קודם החלון הופיע ונעלם בפריים אחד.
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val enter = remember { androidx.compose.animation.core.Animatable(0f) }
    val leave = remember { androidx.compose.animation.core.Animatable(0f) }
    var flyUp by remember { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(Unit) { enter.animateTo(1f, com.future.sharednav.theme.FutureMotion.enter()) }
    fun dismissAnimated() {
        if (leave.isRunning) return
        scope.launch { leave.animateTo(1f, com.future.sharednav.theme.FutureMotion.exit()); onDismiss() }
    }
    fun closeAllAnimated() {
        if (leave.isRunning) return
        flyUp = true
        scope.launch { leave.animateTo(1f, androidx.compose.animation.core.tween(220, easing = com.future.sharednav.theme.FutureMotion.EasingAccelerate)); onCloseAll() }
    }
    LaunchedEffect(apps.size) {
        if (focusedIndex > apps.lastIndex) focusedIndex = apps.lastIndex.coerceAtLeast(0)
    }
    LaunchedEffect(focusedIndex) {
        if (focusedIndex >= 0 && apps.isNotEmpty()) gridState.animateScrollToItem(focusedIndex)
    }

    fun move(delta: Int) {
        if (apps.isEmpty()) return
        if (focusedIndex == CLEAR_ALL) {
            if (delta < 0) focusedIndex = apps.lastIndex
            return
        }
        val next = focusedIndex + delta
        focusedIndex = when {
            next > apps.lastIndex && delta >= COLUMNS -> CLEAR_ALL
            next > apps.lastIndex -> apps.lastIndex
            next < 0 -> focusedIndex
            else -> next
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = enter.value * (1f - leave.value) }
                .background(Color(0xF20E0E10))
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        // RTL: הכרטיס הבא משמאל.
                        Key.DirectionLeft -> { move(1); true }
                        Key.DirectionRight -> { move(-1); true }
                        Key.DirectionDown -> { move(COLUMNS); true }
                        Key.DirectionUp -> {
                            if (focusedIndex == CLEAR_ALL) move(-1) else move(-COLUMNS)
                            true
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            if (focusedIndex == CLEAR_ALL) closeAllAnimated() else apps.getOrNull(focusedIndex)?.let(onLaunch)
                            true
                        }
                        Key.Back -> {
                            dismissAnimated()
                            true
                        }
                        Key.Menu, Key.Settings, Key.Delete, Key.Backspace -> {
                            if (focusedIndex == CLEAR_ALL) closeAllAnimated() else apps.getOrNull(focusedIndex)?.let(onClose)
                            true
                        }
                        Key.Zero -> {
                            closeAllAnimated()
                            true
                        }
                        else -> false
                    }
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = com.future.futureui.statusbar.logic.StatusBarLayoutManager.HEIGHT_DP.dp + 10.dp, bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("אפליקציות אחרונות", color = Color.White, fontSize = FutureTypography.title, fontWeight = FontWeight.Bold)
                    if (memorySummary != null) {
                        Text(memorySummary, color = Color.White.copy(alpha = 0.5f), fontSize = FutureTypography.caption, modifier = Modifier.padding(top = 2.dp))
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (apps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("אין אפליקציות אחרונות", color = Color.White.copy(alpha = 0.5f), fontSize = FutureTypography.body)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(COLUMNS),
                        state = gridState,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            val l = leave.value
                            if (flyUp) translationY = -l * 160.dp.toPx()
                            else { val sc = 1f - 0.06f * l; scaleX = sc; scaleY = sc }
                        },
                        // ריווח תחתון כדי שהשורה האחרונה תוכל לעלות מעל כפתור ה-X המרחף
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 84.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        userScrollEnabled = false,
                    ) {
                        itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                            RecentAppCard(
                                app = app,
                                snapshot = snapshots[app.packageName],
                                isFocused = index == focusedIndex,
                                modifier = Modifier
                                    .animateItem(placementSpec = com.future.sharednav.theme.FutureMotion.Springs.dialogOffset)
                                    .riseIn(index),
                            )
                        }
                        item(span = { GridItemSpan(maxLineSpan) }) { Spacer(modifier = Modifier.height(4.dp)) }
                    }
                }

                // כפתור X עגול לסגירת הכל, כמו ב-Redmi - מרחף מעל הכרטיסים ולא תופס שורה משלו.
                if (apps.isNotEmpty()) {
                    val scale by animateFloatAsState(
                        if (clearFocused) 1.12f else 1f,
                        if (clearFocused) com.future.sharednav.theme.FutureMotion.Springs.press() else com.future.sharednav.theme.FutureMotion.focusScaleSpec,
                        label = "clearAllScale",
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 14.dp)
                            .size(56.dp)
                            .graphicsLayer {
                                scaleX = scale; scaleY = scale
                                shadowElevation = 12.dp.toPx(); shape = CircleShape; clip = true
                            }
                            .background(animateFocusColor(clearFocused, Color(0xFF4A4A50), Color(0xE62C2C30)).value)
                            .border(animateFocusDp(clearFocused, 2.dp, 1.dp).value, animateFocusColor(clearFocused, Color.LightGray, Color.White.copy(alpha = 0.18f)).value, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "סגור הכל", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
                }

                Text(
                    "OK פתיחה · אפשרויות סגירת חלון · 0 סגירת הכל",
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = FutureTypography.caption,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RecentAppCard(app: RecentAppInfo, snapshot: ImageBitmap?, isFocused: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(FutureShapes.radiusXl)
    val scale by animateFloatAsState(
        if (isFocused) 1.03f else 1f,
        if (isFocused) com.future.sharednav.theme.FutureMotion.Springs.press() else com.future.sharednav.theme.FutureMotion.focusScaleSpec,
        label = "recentCardScale",
    )
    // גודל קבוע: אייקון של אפליקציה זרה יכול להצהיר על גודל עצום, ו-toBitmap() בגודל
    // המקורי היה מפיל את FutureUI (ואיתו את מסך הנעילה) מחוסר זיכרון
    val icon = remember(app.packageName) { app.icon.toBitmap(96, 96).asImageBitmap() }
    val tint = Color(app.tint)

    Column(
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        // כותרת החלון: אייקון קטן ושם, מעל הכרטיס.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(icon, contentDescription = null, modifier = Modifier.size(20.dp).clip(RoundedCornerShape(percent = 28)))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                app.label,
                color = animateFocusColor(isFocused, Color.White, Color.White.copy(alpha = 0.8f)).value,
                fontSize = FutureTypography.summary,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CardHeight)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(tint, tint.copy(alpha = 0.75f).compositeOverBlack()))),
            contentAlignment = Alignment.Center,
        ) {
            if (snapshot != null) {
                // המסך האחרון של האפליקציה, מיושר לראש (שם בדרך כלל הכותרת והתוכן)
                Image(
                    snapshot,
                    contentDescription = app.label,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Image(icon, contentDescription = null, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(percent = 28)))
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(animateFocusDp(isFocused, 2.5.dp, 0.5.dp).value, animateFocusColor(isFocused, Color.White, Color.White.copy(alpha = 0.12f)).value, shape)
            )
        }
    }
}

private fun Color.compositeOverBlack(): Color = Color(red * alpha, green * alpha, blue * alpha, 1f)

private const val COLUMNS = 2
private const val CLEAR_ALL = -1
// מסך 640x960 ב-320dpi = 320x480dp: כרטיס ברוחב ~140dp; 156dp מראה את רוב
// המסך של האפליקציה (החלק התחתון נחתך) ועדיין שתי שורות כמעט שלמות נכנסות
private val CardHeight = 156.dp


/**
 * הכרטיס עולה מלמטה (70dp), מתנפח מ-0.8 ומופיע, בקפיץ lift - שורה אחרי
 * שורה (55ms) ובתוך השורה עמודה אחרי עמודה (25ms). רק 6 הכרטיסים הראשונים;
 * כל השאר מתחת לקצה המסך ממילא.
 */
private fun Modifier.riseIn(index: Int): Modifier = composed {
    if (index >= 6) return@composed this
    val p = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((40 + (index / COLUMNS) * 55 + (index % COLUMNS) * 25).toLong())
        p.animateTo(1f, com.future.sharednav.theme.FutureMotion.Springs.lift())
    }
    graphicsLayer {
        val v = p.value
        alpha = v.coerceIn(0f, 1f)
        translationY = (1f - v) * 70.dp.toPx()
        val sc = 0.8f + 0.2f * v
        scaleX = sc
        scaleY = sc
    }
}
