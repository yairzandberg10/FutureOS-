package com.future.futureui.notificationcenter.ui

import com.future.sharednav.i18n.tr

import com.future.futureui.utils.safeText
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureTypography
import android.app.Notification
import android.service.notification.StatusBarNotification
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import com.future.sharednav.theme.FutureShapes
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.future.futureui.utils.FrostedBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * באנר קופץ (heads-up) שמופיע לזמן קצר כשמגיעה התראה חדשה - רכיב
 * HeadsUpNotification של הדיזיין סיסטם (components/feedback/HeadsUpNotification.jsx):
 * 12dp מהצדדים ו-8dp מתחת לשורת המצב, פינות 28dp, #1C1C1E ב-90% עם קו שיער
 * לבן 15%, וטקסט לבן תמיד - גם כשהמערכת במצב בהיר. שלוש שורות לכל היותר:
 * שם האפליקציה (12sp, 65%), הכותרת (15sp מודגש) ושורת תוכן אחת (13sp, 55%).
 *
 * קודם הבאנר הלך לפי פלטת One UI (לבן במצב בהיר), חיבר את שם האפליקציה
 * והכותרת לשורה אחת ופתח את התוכן לשתי שורות - ונראה שונה מכל שאר
 * ההתראות במערכת.
 *
 * המסך אינו אינטראקטיבי (אין מגע, והוא לא גוזל פוקוס מהאפליקציה שבחזית) -
 * מענה/דחייה/פתיחה במרכז ההתראות.
 *
 * תנועה: הכרטיס נופל משורת המצב בקפיץ dialog (גדל מ-90% ונכנס מלמעלה),
 * ויוצא באותה דרך למעלה. התראה נוספת בזמן שזו מוצגת לא בונה באנר חדש: רק
 * התוכן מתחלף (הישן עולה החוצה, החדש נכנס מלמטה) והבאנר "נושם" ([generation]).
 *
 * רקע: זכוכית מטושטשת אמיתית של מה שמתחת ([frost], ר' FrostedBackdrop) מתחת
 * לגוון של 90%. בלי צילום - הגוון לבדו. הכול נקרא בשלב הציור.
 */
@Composable
fun HeadsUpNotificationScreen(
    sbn: StatusBarNotification,
    generation: Int = 0,
    frost: FrostedBackdrop.Frost? = null,
    autoDismissMillis: Long = 4500L,
    onDismissed: () -> Unit
) {
    val reveal = remember { Animatable(0f) }
    val breath = remember { Animatable(1f) }
    val dismissed by rememberUpdatedState(onDismissed)
    val dismissAfter by rememberUpdatedState(autoDismissMillis)

    // כל התראה חדשה (generation) מאפסת את הטיימר. אם הבאנר כבר באמצע
    // היציאה, הוא חוזר ונפתח מהנקודה שבה הוא נמצא.
    LaunchedEffect(generation) {
        if (generation > 0) launch {
            breath.snapTo(1.05f)
            breath.animateTo(1f, FutureMotion.Springs.press())
        }
        reveal.animateTo(1f, FutureMotion.Springs.dialog())
        delay(dismissAfter)
        reveal.animateTo(0f, FutureMotion.Springs.settle())
        dismissed()
    }

    // מיקום הכרטיס בחלון - כדי שהזכוכית תצויר בדיוק מעל מה שמתחתיה.
    val cardPos = remember { FloatArray(2) }
    val shape = FutureShapes.xxl

    CompositionLocalProvider(LocalLayoutDirection provides com.future.sharednav.i18n.Tr.layoutDirection) {
        // בלי Modifier.fillMaxSize() כאן בכוונה: החלון של השירות מוגדר WRAP_CONTENT
        // בגובה (HeadsUpNotificationService). fillMaxSize() בתוך שורש שכזה גורם ל-
        // Compose "למלא" את מלוא גובה המסך בפועל, ואז הבאנר "חוסם" חלק מהמסך.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // שורת המצב שלנו מצוירת מעל החלון הזה - הכרטיס מתחיל 8dp מתחתיה.
                // הריווח התחתון משאיר מקום לצל, שאחרת היה נחתך בגבול החלון.
                .padding(
                    top = (com.future.futureui.statusbar.logic.StatusBarLayoutManager.HEIGHT_DP + 8).dp,
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 16.dp,
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned {
                        val p = it.positionInWindow()
                        cardPos[0] = p.x
                        cardPos[1] = p.y
                    }
                    .graphicsLayer {
                        val p = reveal.value
                        val s = breath.value * (0.9f + 0.1f * p)
                        scaleX = s
                        scaleY = s
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        alpha = p.coerceIn(0f, 1f)
                        translationY = (1f - p) * -(size.height + 12.dp.toPx())
                    }
                    // --fos-shadow-headsup: 0 8px 32px rgba(0,0,0,.45)
                    .shadow(16.dp, shape, ambientColor = Color.Black.copy(alpha = 0.45f), spotColor = Color.Black.copy(alpha = 0.45f))
                    .clip(shape)
                    .headsUpGlass(frost, cardPos)
                    .border(0.5.dp, HeadsUpBorder, shape)
            ) {
                AnimatedContent(
                    targetState = sbn,
                    contentKey = { it.key + "#" + it.postTime },
                    transitionSpec = {
                        (slideInVertically(FutureMotion.Springs.dialogOffset) { it / 2 } + fadeIn(FutureMotion.enter())) togetherWith
                            (slideOutVertically(FutureMotion.exit()) { -it / 2 } + fadeOut(FutureMotion.exit()))
                    },
                    label = "headsUpContent",
                ) { current ->
                    HeadsUpContent(current)
                }
            }
        }
    }
}

@Composable
private fun HeadsUpContent(sbn: StatusBarNotification) {
    val context = LocalContext.current
    val n = sbn.notification
    val title = n.safeText(Notification.EXTRA_TITLE)?.toString() ?: ""
    val text = n.safeText(Notification.EXTRA_TEXT)?.toString() ?: ""

    val appName = remember(sbn.packageName) {
        try {
            val ai = context.packageManager.getApplicationInfo(sbn.packageName, 0)
            context.packageManager.getApplicationLabel(ai).toString()
        } catch (e: Exception) {
            sbn.packageName.substringAfterLast(".")
        }
    }

    // הסמל: אייקון קו לבן של המערכת לאפליקציה מוכרת (כמו בשורת המצב), אחרת
    // האייקון של האפליקציה עצמה.
    val glyph = remember(sbn.packageName) { com.future.futureui.statusbar.ui.appGlyph(sbn.packageName) }
    var appIcon by remember(sbn.packageName) { mutableStateOf<ImageBitmap?>(null) }
    if (glyph == null) {
        LaunchedEffect(sbn.packageName) {
            try {
                val icon = context.packageManager.getApplicationIcon(sbn.packageName)
                appIcon = icon.toBitmap(width = 68, height = 68).asImageBitmap()
            } catch (e: Exception) {
                android.util.Log.w("HeadsUpNotificationScre", "app icon failed", e)
            }
        }
    }

    val isCall = n.category == Notification.CATEGORY_CALL
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // --fos-space-4 / --fos-space-6, ורווח --fos-space-5 בין הסמל לטקסט
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            val icon = appIcon
            when {
                isCall -> Icon(com.future.sharednav.icons.FutureIcons.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                glyph != null -> Icon(glyph, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                icon != null -> Image(bitmap = icon, contentDescription = null, modifier = Modifier.fillMaxSize())
                else -> Icon(Icons.Rounded.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appName,
                fontSize = FutureTypography.label,
                color = Color.White.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // שיחה נכנסת: שם/מספר המתקשר הוא הכותרת, ושורת התוכן היא רמז המקשים -
            // אין כאן זמן לקרוא פרטים (טלפון = מענה, ניתוק = דחייה).
            val headline = if (isCall) text.ifBlank { title.ifBlank { tr("שיחה נכנסת") } } else title.ifBlank { text }
            val bodyLine = if (isCall) tr("טלפון = מענה · ניתוק = דחייה") else if (title.isNotBlank()) text else ""
            if (headline.isNotBlank()) {
                Text(
                    text = headline,
                    fontSize = FutureTypography.dialog,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            if (bodyLine.isNotBlank()) {
                Text(
                    text = bodyLine,
                    fontSize = FutureTypography.summary,
                    color = Color.White.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

/**
 * המשטח: הזכוכית המטושטשת (אם יש) ומעליה הגוון של 90%. התמונה הקטנה מוגדלת
 * לגודל הרצועה שצולמה ומוזזת כך שהנקודה שמתחת לכרטיס תצויר בתוכו.
 */
private fun Modifier.headsUpGlass(frost: FrostedBackdrop.Frost?, cardPos: FloatArray): Modifier = drawWithCache {
    val frostDstSize = frost?.let { IntSize(it.sourceWidth, it.sourceHeight) }
    val frostSrcSize = frost?.let { IntSize(it.image.width, it.image.height) }
    onDrawWithContent {
        if (frost != null && frostDstSize != null && frostSrcSize != null) {
            drawImage(
                image = frost.image,
                srcOffset = IntOffset.Zero,
                srcSize = frostSrcSize,
                dstOffset = IntOffset(-cardPos[0].toInt(), -cardPos[1].toInt()),
                dstSize = frostDstSize,
                filterQuality = FilterQuality.Low,
            )
        }
        drawRect(HeadsUpSurface)
        drawContent()
    }
}

/** --fos-headsup-bg: #1C1C1E ב-90% (headsUpSurfaceColor). */
private val HeadsUpSurface = Color(0xE61C1C1E)

/** --fos-border-headsup: 0.5dp, לבן 15% (headsUpBorderColor). */
private val HeadsUpBorder = Color.White.copy(alpha = 0.15f)
