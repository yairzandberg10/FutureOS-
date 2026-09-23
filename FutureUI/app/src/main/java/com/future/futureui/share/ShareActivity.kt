package com.future.futureui.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.future.futureui.statusbar.logic.StatusBarLayoutManager
import com.future.futureui.ui.theme.FutureUITheme
import com.future.futureui.ui.theme.ShellGlass
import com.future.futureui.ui.theme.shellFocusRing
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureTheme

/**
 * חלון השיתוף של המערכת - כל אפליקציה משתפת דרכו (FutureShare.open), כך
 * שיש חלון שיתוף אחד בשפת המעטפת (זכוכית, פוקוס במסגרת) במקום הבורר של
 * אנדרואיד. מציג את האפליקציות שמקבלות את ה-Intent, האחרונות שנבחרו למעלה,
 * ו"העתקה" כשמשתפים טקסט.
 *
 * מקשים: למעלה/למטה בין היעדים, OK משתף, BACK סוגר.
 */
class ShareActivity : ComponentActivity() {
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
        val targets = buildTargets(send)
        setContent {
            FutureUITheme { ShareSheet(title = title, targets = targets, onClose = { finish() }) }
        }
    }

    private fun buildTargets(send: Intent): List<Target> {
        val pm = packageManager
        val prefs = getSharedPreferences("share", Context.MODE_PRIVATE)
        val resolved = pm.queryIntentActivities(send, PackageManager.MATCH_DEFAULT_ONLY)
            .filter { it.activityInfo.packageName != packageName }
        val apps = resolved.map { ri ->
            val component = ComponentName(ri.activityInfo.packageName, ri.activityInfo.name)
            val key = component.flattenToShortString()
            Target(
                key = key,
                label = ri.loadLabel(pm).toString(),
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
                        Toast.makeText(this, "לא ניתן לשתף אל ${ri.loadLabel(pm)}", Toast.LENGTH_SHORT).show()
                    }
                    finish()
                },
            )
        }.sortedWith(compareByDescending<Target> { prefs.getLong(it.key, 0L) }.thenBy { it.label })

        val text = send.getCharSequenceExtra(Intent.EXTRA_TEXT)
        val copy = if (!text.isNullOrBlank()) listOf(
            Target("copy", "העתקה", null, FutureIcons.ContentCopy) {
                (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("text", text))
                Toast.makeText(this, "הועתק", Toast.LENGTH_SHORT).show()
                finish()
            }
        ) else emptyList()
        return copy + apps
    }

    @Composable
    private fun ShareSheet(title: String, targets: List<Target>, onClose: () -> Unit) {
        val theme = LocalFutureTheme.current
        var focused by remember { mutableIntStateOf(0) }
        val listState = rememberLazyListState()
        val focusRequester = remember { FocusRequester() }
        val shown = remember { MutableTransitionState(false).apply { targetState = true } }
        LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
        LaunchedEffect(focused) { if (targets.isNotEmpty()) listState.animateScrollToItem(focused) }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ShellGlass.scrim(theme))
                    .focusRequester(focusRequester)
                    .focusable()
                    .onKeyEvent { e ->
                        if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                        when (e.key) {
                            Key.DirectionDown -> { focused = (focused + 1).coerceAtMost(targets.lastIndex.coerceAtLeast(0)); true }
                            Key.DirectionUp -> { focused = (focused - 1).coerceAtLeast(0); true }
                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { targets.getOrNull(focused)?.run?.invoke(); true }
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
                            .padding(top = StatusBarLayoutManager.HEIGHT_DP.dp + 24.dp)
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                            .clip(FutureShapes.xxl)
                            .background(ShellGlass.panel(theme))
                            .padding(vertical = 12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(36.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(ShellGlass.inkMuted(theme).copy(alpha = 0.4f))
                        )
                        Text(
                            title,
                            color = ShellGlass.ink(theme),
                            fontSize = FutureTypography.screenTitle,
                            fontWeight = FutureTypography.weightBold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                        if (targets.isEmpty()) {
                            Text(
                                "אין אפליקציות שיכולות לקבל את התוכן הזה",
                                color = ShellGlass.inkMuted(theme),
                                fontSize = FutureTypography.body,
                                modifier = Modifier.padding(20.dp),
                            )
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.heightIn(max = 560.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                itemsIndexed(targets, key = { _, t -> t.key }) { index, t ->
                                    TargetRow(t, index == focused)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TargetRow(t: Target, isFocused: Boolean) {
        val theme = LocalFutureTheme.current
        val shape = FutureShapes.lg
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(shape)
                .background(if (isFocused) ShellGlass.tileFocused(theme) else ShellGlass.tile(theme).copy(alpha = 0f))
                .shellFocusRing(isFocused, shape)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (t.icon != null) {
                Image(t.icon, contentDescription = null, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(percent = 28)))
            } else {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(ShellGlass.tile(theme)),
                    contentAlignment = Alignment.Center,
                ) {
                    t.glyph?.let { Icon(it, contentDescription = null, tint = ShellGlass.ink(theme), modifier = Modifier.size(18.dp)) }
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                t.label,
                color = ShellGlass.ink(theme),
                fontSize = FutureTypography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
