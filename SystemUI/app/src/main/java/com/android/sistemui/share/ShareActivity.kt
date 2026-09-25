package com.android.sistemui.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.android.sistemui.ui.theme.FutureUITheme
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTypography

/**
 * חלון השיתוף של המערכת. כל אפליקציה ב-FutureOS משתפת דרכו
 * (SharedKeypadNav: FutureShare.open -> com.future.futureui.ACTION_SHARE),
 * כך שיש חלון שיתוף אחד בכל המערכת במקום הבורר של אנדרואיד - ואם FutureUI
 * לא מותקן, FutureShare נופל לבורר של המערכת.
 *
 * מבנה: גיליון תחתון בשפת FutureUI (משטח כהה שקוף-למחצה, מסגרת פוקוס
 * אפורה-בהירה - אותה שפה של תפריט הכיבוי ומרכז הבקרה), כותרת ותקציר של מה
 * שמשותף, שורת פעולות מהירות (העתקה) ורשת של האפליקציות שמקבלות את התוכן -
 * האחרונות שנבחרו קודם.
 *
 * מקשים: חצים ברשת (RTL - שמאלה זה הבא), OK משתף, BACK סוגר.
 */
class ShareActivity : ComponentActivity() {
    // אין מגע במכשיר - חוסמים גם כדי שנגיעה מקרית בזכוכית לא תשתף.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    private class Target(
        val key: String,
        val label: String,
        val icon: ImageBitmap?,
        val glyph: ImageVector?,
        val run: () -> Unit,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        val send: Intent? = intent.getParcelableExtra(Intent.EXTRA_INTENT)
        if (send == null) {
            finish()
            return
        }
        val title = intent.getStringExtra(Intent.EXTRA_TITLE) ?: "שיתוף"
        val summary = describe(send)
        val quick = buildQuickActions(send)
        val apps = buildAppTargets(send)
        setContent {
            FutureUITheme { ShareSheet(title, summary, quick, apps, onClose = { finish() }) }
        }
    }

    /** שורה אחת שאומרת מה משותף: תחילת הטקסט, שם הקובץ, או מספר הפריטים. */
    private fun describe(send: Intent): String? {
        send.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.takeIf { it.isNotBlank() }?.let {
            return it.replace('\n', ' ').take(80)
        }
        @Suppress("DEPRECATION")
        val many: ArrayList<Uri>? = send.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
        if (!many.isNullOrEmpty()) return "${many.size} פריטים"
        @Suppress("DEPRECATION")
        val one: Uri? = send.getParcelableExtra(Intent.EXTRA_STREAM)
        if (one != null) {
            val name = runCatching {
                contentResolver.query(one, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
            }.getOrNull()
            return name ?: when {
                send.type?.startsWith("image/") == true -> "תמונה"
                send.type?.startsWith("audio/") == true -> "קובץ שמע"
                else -> "קובץ"
            }
        }
        return null
    }

    private fun buildQuickActions(send: Intent): List<Target> {
        val text = send.getCharSequenceExtra(Intent.EXTRA_TEXT)
        if (text.isNullOrBlank()) return emptyList()
        return listOf(
            Target("copy", "העתקה", null, FutureIcons.ContentCopy) {
                (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("text", text))
                Toast.makeText(this, "הועתק", Toast.LENGTH_SHORT).show()
                finish()
            }
        )
    }

    private fun buildAppTargets(send: Intent): List<Target> {
        val pm = packageManager
        val prefs = getSharedPreferences("share", Context.MODE_PRIVATE)
        val resolved = pm.queryIntentActivities(send, PackageManager.MATCH_DEFAULT_ONLY)
            .filter { it.activityInfo.packageName != packageName }
        return resolved.map { ri ->
            val component = ComponentName(ri.activityInfo.packageName, ri.activityInfo.name)
            val key = component.flattenToShortString()
            val label = ri.loadLabel(pm).toString()
            Target(
                key = key,
                label = label,
                icon = runCatching { ri.loadIcon(pm).toBitmap(96, 96).asImageBitmap() }.getOrNull(),
                glyph = null,
                run = {
                    prefs.edit().putLong(key, System.currentTimeMillis()).apply()
                    val out = Intent(send).setComponent(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    // הרשאת הקריאה לקובץ עוברת הלאה ליעד (ה-ClipData הגיע עם החלון).
                    intent.clipData?.let { out.clipData = it }
                    out.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    try {
                        startActivity(out)
                    } catch (e: Exception) {
                        android.util.Log.w("ShareActivity", "share to $key failed", e)
                        Toast.makeText(this, "לא ניתן לשתף אל $label", Toast.LENGTH_SHORT).show()
                    }
                    finish()
                },
            )
        }.sortedWith(compareByDescending<Target> { prefs.getLong(it.key, 0L) }.thenBy { it.label })
    }

    @Composable
    private fun ShareSheet(
        title: String,
        summary: String?,
        quick: List<Target>,
        apps: List<Target>,
        onClose: () -> Unit,
    ) {
        // מיקום אחד רציף: קודם הפעולות המהירות (שורה), אחר כך רשת האפליקציות.
        val all = quick + apps
        var focused by remember { mutableIntStateOf(0) }
        val focusRequester = remember { FocusRequester() }
        val shown = remember { MutableTransitionState(false).apply { targetState = true } }
        LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

        fun move(delta: Int) {
            if (all.isEmpty()) return
            focused = (focused + delta).coerceIn(0, all.lastIndex)
        }

        /** חץ למעלה/למטה: שורה שלמה ברשת; מהפעולות המהירות - אל/מן הרשת. */
        fun moveRow(down: Boolean) {
            if (all.isEmpty()) return
            val inQuick = focused < quick.size
            focused = if (down) {
                if (inQuick) quick.size.coerceAtMost(all.lastIndex)
                else (focused + COLUMNS).coerceAtMost(all.lastIndex)
            } else {
                val gridIndex = focused - quick.size
                when {
                    inQuick -> focused
                    gridIndex < COLUMNS -> if (quick.isNotEmpty()) 0 else focused
                    else -> focused - COLUMNS
                }
            }
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .focusRequester(focusRequester)
                    .focusable()
                    .onKeyEvent { e ->
                        if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                        when (e.key) {
                            // RTL: הפריט הבא נמצא משמאל.
                            Key.DirectionLeft -> { move(1); true }
                            Key.DirectionRight -> { move(-1); true }
                            Key.DirectionDown -> { moveRow(true); true }
                            Key.DirectionUp -> { moveRow(false); true }
                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { all.getOrNull(focused)?.run?.invoke(); true }
                            Key.Back -> { onClose(); true }
                            else -> false
                        }
                    },
                contentAlignment = Alignment.BottomCenter,
            ) {
                AnimatedVisibility(
                    visibleState = shown,
                    enter = slideInVertically(FutureMotion.enter()) { it / 3 } + fadeIn(FutureMotion.enter()),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clip(FutureShapes.xxl)
                            .background(SheetColor)
                            .border(0.5.dp, Color.White.copy(alpha = 0.15f), FutureShapes.xxl)
                            .padding(vertical = 12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(36.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f))
                        )
                        Text(
                            title,
                            color = Color.White,
                            fontSize = FutureTypography.screenTitle,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
                        )
                        if (summary != null) {
                            Text(
                                summary,
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = FutureTypography.summary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        if (quick.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                quick.forEachIndexed { i, t -> QuickChip(t, focused == i) }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                                    .height(0.5.dp)
                                    .background(Color.White.copy(alpha = 0.12f))
                            )
                        }

                        if (apps.isEmpty()) {
                            Text(
                                "אין אפליקציות שיכולות לקבל את התוכן הזה",
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = FutureTypography.body,
                                modifier = Modifier.padding(20.dp),
                            )
                        } else {
                            val scroll = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .heightIn(max = 520.dp)
                                    .verticalScroll(scroll)
                                    .padding(horizontal = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                apps.chunked(COLUMNS).forEachIndexed { rowIndex, row ->
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        row.forEachIndexed { colIndex, t ->
                                            val index = quick.size + rowIndex * COLUMNS + colIndex
                                            AppCell(
                                                t,
                                                isFocused = focused == index,
                                                scroll = scroll,
                                                modifier = Modifier.weight(1f),
                                            )
                                        }
                                        repeat(COLUMNS - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun QuickChip(t: Target, isFocused: Boolean) {
        val shape = FutureShapes.lg
        Row(
            modifier = Modifier
                .clip(shape)
                .background(if (isFocused) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f))
                .then(if (isFocused) Modifier.border(2.dp, Color.LightGray, shape) else Modifier)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            t.glyph?.let { Icon(it, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) }
            Spacer(modifier = Modifier.width(8.dp))
            Text(t.label, color = Color.White, fontSize = FutureTypography.body, fontWeight = FontWeight.Medium)
        }
    }

    @Composable
    private fun AppCell(
        t: Target,
        isFocused: Boolean,
        scroll: androidx.compose.foundation.ScrollState,
        modifier: Modifier = Modifier,
    ) {
        val shape = FutureShapes.lg
        var top by remember { mutableIntStateOf(0) }
        var height by remember { mutableIntStateOf(0) }
        // הפריט הממוקד תמיד בתוך החלון הנראה של הרשת.
        LaunchedEffect(isFocused, top, height) {
            if (!isFocused || height == 0) return@LaunchedEffect
            val viewport = scroll.viewportSize
            if (viewport == 0) return@LaunchedEffect
            if (top < scroll.value) scroll.animateScrollTo(top)
            else if (top + height > scroll.value + viewport) scroll.animateScrollTo(top + height - viewport)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .onGloballyPositioned { c ->
                    // מיקום השורה בתוך התוכן הנגלל (לא על המסך).
                    val parentY = c.parentLayoutCoordinates?.positionInParent()?.y ?: 0f
                    top = parentY.toInt()
                    height = c.size.height
                }
                .padding(2.dp)
                .clip(shape)
                .background(if (isFocused) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                .then(if (isFocused) Modifier.border(2.dp, Color.LightGray, shape) else Modifier)
                .padding(vertical = 10.dp, horizontal = 2.dp),
        ) {
            if (t.icon != null) {
                Image(t.icon, contentDescription = null, modifier = Modifier.size(46.dp).clip(RoundedCornerShape(percent = 28)))
            } else {
                Box(
                    modifier = Modifier.size(46.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    t.glyph?.let { Icon(it, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                }
            }
            Text(
                t.label,
                color = if (isFocused) Color.White else Color.White.copy(alpha = 0.75f),
                fontSize = FutureTypography.caption,
                fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }

    private companion object {
        const val COLUMNS = 4
        /** אותו משטח של תפריט הכיבוי (PowerMenuScreen). */
        val SheetColor = Color(0xEE1C1C1E)
    }
}
