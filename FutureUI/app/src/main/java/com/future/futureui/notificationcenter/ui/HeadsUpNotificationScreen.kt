package com.future.futureui.notificationcenter.ui

import com.future.futureui.utils.safeText
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureTypography
import android.app.Notification
import android.service.notification.StatusBarNotification
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.future.futureui.utils.FrostedBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * באנר קופץ (heads-up) שמופיע לזמן קצר כשמגיעה התראה חדשה, באותו סגנון עיצוב
 * (זכוכית, פינות מעוגלות) כמו מרכז ההתראות - כדי שהמערכת תיראה עקבית.
 * המסך הזה בלבד אינו אינטראקטיבי (המכשיר ללא מסך מגע ובלי לגזול פוקוס מקשים
 * מהאפליקציה שבחזית) - האינטראקציה (מענה/דחייה/פתיחה) קיימת במרכז ההתראות.
 *
 * תנועה - "האי": ההתראה לא נשלפת מלמעלה כמו popup, אלא גדלה מגלולה קטנה
 * במרכז שורת המצב - קודם לרוחב, אחר כך לגובה, בקפיץ island עם overshoot
 * קטן - והתוכן מופיע רק כשיש לו מקום. ביציאה היא נאספת חזרה לאותה נקודה.
 * התראה נוספת בזמן שזו מוצגת לא בונה באנר חדש: רק התוכן מתחלף (הישן עולה
 * החוצה, החדש נכנס מלמטה) והבאנר "נושם" ([generation]).
 *
 * רקע: זכוכית מטושטשת אמיתית של מה שמתחת ([frost], ר' FrostedBackdrop),
 * עם אותו גוון כהה מעליה. בלי צילום (שירות שורת המצב כבוי, הגבלת קצב) -
 * הרקע האטום של קודם. הכול נקרא בשלב הציור: הקפיץ מצייר מחדש בלי
 * recomposition.
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
        reveal.animateTo(1f, FutureMotion.Springs.island())
        delay(dismissAfter)
        reveal.animateTo(0f, spring(dampingRatio = 1f, stiffness = 700f))
        dismissed()
    }

    // מיקום הכרטיס בחלון - כדי שהזכוכית תצויר בדיוק מעל מה שמתחתיה.
    val cardPos = remember { FloatArray(2) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        // בלי Modifier.fillMaxSize() כאן בכוונה: החלון של השירות מוגדר WRAP_CONTENT
        // בגובה (HeadsUpNotificationService). fillMaxSize() בתוך שורש שכזה גורם ל-
        // Compose "למלא" את מלוא גובה המסך בפועל, ואז הבאנר "חוסם" חלק מהמסך.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp, start = 12.dp, end = 12.dp)
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
                        val s = breath.value
                        scaleX = s
                        scaleY = s
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        translationY = (1f - reveal.value.coerceIn(0f, 1f)) * -10.dp.toPx()
                    }
                    .islandSurface(reveal = { reveal.value }, frost = frost, cardPos = cardPos, radius = CardRadius)
            ) {
                AnimatedContent(
                    targetState = sbn,
                    contentKey = { it.key + "#" + it.postTime },
                    transitionSpec = {
                        (slideInVertically(FutureMotion.Springs.dialogOffset) { it / 2 } + fadeIn(FutureMotion.enter())) togetherWith
                            (slideOutVertically(FutureMotion.exit()) { -it / 2 } + fadeOut(FutureMotion.exit()))
                    },
                    modifier = Modifier.graphicsLayer {
                        // התוכן מופיע רק כשיש לו מקום - לא נמעך בתוך הגלולה
                        alpha = ((reveal.value - 0.5f) / 0.4f).coerceIn(0f, 1f)
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

    var appIcon by remember(sbn.packageName) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(sbn.packageName) {
        try {
            val icon = context.packageManager.getApplicationIcon(sbn.packageName)
            appIcon = icon.toBitmap(width = 64, height = 64).asImageBitmap()
        } catch (e: Exception) {
            android.util.Log.w("HeadsUpNotificationScre", "HeadsUpNotificationScreen failed", e)
        }
    }

    val textColor = Color.White
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // מעט "שמנה" יותר מבעבר (10dp -> 15dp, אייקון 34 -> 40, טקסט בשתי שורות).
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            val icon = appIcon
            if (icon != null) {
                Image(bitmap = icon, contentDescription = null, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Rounded.Notifications, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        // שיחה נכנסת (CATEGORY_CALL) היא ההתראה הכי דחופה שיכולה להופיע - במקום
        // כותרת/טקסט קטנים כמו כל התראה אחרת, שם/מספר המתקשר גדול וברור, עם רמז
        // מקשים מפורש (טלפון = מענה, ניתוק = דחייה), כי אין כאן זמן לקרוא פרטים.
        if (n.category == Notification.CATEGORY_CALL) {
            Column {
                Text(
                    text = title.ifBlank { "שיחה נכנסת" },
                    fontSize = FutureTypography.label,
                    color = textColor.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = text.ifBlank { appName },
                    fontSize = FutureTypography.screenTitle,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "טלפון = מענה · ניתוק = דחייה",
                    fontSize = FutureTypography.caption,
                    color = textColor.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Column {
                Text(
                    text = if (title.isNotBlank()) "$appName: $title" else appName,
                    fontSize = FutureTypography.body,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (text.isNotBlank()) {
                    Text(
                        text = text,
                        fontSize = FutureTypography.summary,
                        color = textColor.copy(alpha = 0.72f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * המשטח של האי: גלולה שגדלה לכרטיס לפי [reveal] (0 = גלולה במרכז העליון,
 * 1 = הכרטיס המלא), עם הזכוכית המטושטשת והגוון בפנים ומסגרת דקה סביב.
 * ה-Path אחד ונבנה מחדש בכל פריים בלי הקצאה חדשה (rewind).
 */
private fun Modifier.islandSurface(
    reveal: () -> Float,
    frost: FrostedBackdrop.Frost?,
    cardPos: FloatArray,
    radius: Dp,
): Modifier = drawWithCache {
    val path = Path()
    val r = radius.toPx()
    val seedW = 84.dp.toPx()
    val seedH = 24.dp.toPx()
    val stroke = Stroke(width = 0.5.dp.toPx())
    // הזכוכית: התמונה הקטנה מוגדלת לגודל הרצועה שצולמה, ומוזזת כך שהנקודה
    // שמתחת לכרטיס תצויר בתוכו.
    val frostDstSize = frost?.let { IntSize(it.sourceWidth, it.sourceHeight) }
    val frostSrcSize = frost?.let { IntSize(it.image.width, it.image.height) }
    onDrawWithContent {
        val p = reveal()
        if (p <= 0.001f) return@onDrawWithContent
        val pw = (p / 0.55f).coerceIn(0f, 1.06f)
        val ph = ((p - 0.22f) / 0.78f).coerceIn(0f, 1.06f)
        val w = seedW + (size.width - seedW) * pw
        val h = seedH + (size.height - seedH) * ph
        val left = (size.width - w) / 2f
        val cr = minOf(r, h / 2f)
        path.rewind()
        path.addRoundRect(RoundRect(left, 0f, left + w, h, CornerRadius(cr, cr)))
        val a = (p * 8f).coerceIn(0f, 1f)
        clipPath(path) {
            if (frost != null && frostDstSize != null && frostSrcSize != null) {
                drawImage(
                    image = frost.image,
                    srcOffset = IntOffset.Zero,
                    srcSize = frostSrcSize,
                    dstOffset = IntOffset(-cardPos[0].toInt(), -cardPos[1].toInt()),
                    dstSize = frostDstSize,
                    alpha = a,
                    filterQuality = FilterQuality.Low,
                )
                drawRect(FrostTint, alpha = a)
            } else {
                drawRect(OpaqueTint, alpha = a)
            }
            this@onDrawWithContent.drawContent()
        }
        drawPath(path, Color.White.copy(alpha = 0.15f * a), style = stroke)
    }
}

/** הרדיוס של המשטח (FutureShapes.xxl, 28dp). */
private val CardRadius = 28.dp

/** הגוון מעל הזכוכית - שקוף מספיק כדי לראות את הטשטוש, כהה מספיק לטקסט לבן. */
private val FrostTint = Color(0x9E1C1C1E)

/** בלי זכוכית - הרקע של קודם. */
private val OpaqueTint = Color(0xE61C1C1E)
