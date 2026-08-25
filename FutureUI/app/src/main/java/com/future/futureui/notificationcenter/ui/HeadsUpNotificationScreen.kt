package com.future.futureui.notificationcenter.ui

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay

/**
 * באנר קופץ (heads-up) שמופיע לזמן קצר כשמגיעה התראה חדשה, באותו סגנון עיצוב
 * (זכוכית שקופה, פינות מעוגלות) כמו מרכז ההתראות - כדי שהמערכת תיראה עקבית.
 * המסך הזה בלבד אינו אינטראקטיבי (המכשיר ללא מסך מגע ובלי לגזול פוקוס מקשים
 * מהאפליקציה שבחזית) - האינטראקציה (מענה/דחייה/פתיחה) קיימת במרכז ההתראות.
 */
@Composable
fun HeadsUpNotificationScreen(
    sbn: StatusBarNotification,
    autoDismissMillis: Long = 4500L,
    onDismissed: () -> Unit
) {
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(sbn.key) {
        isVisible = true
        delay(autoDismissMillis)
        isVisible = false
        delay(300)
        onDismissed()
    }

    val n = sbn.notification
    val title = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
    val text = n.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

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
        } catch (e: Exception) {}
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        // בלי Modifier.fillMaxSize() כאן בכוונה: החלון של השירות מוגדר WRAP_CONTENT
        // בגובה (HeadsUpNotificationService). fillMaxSize() בתוך שורש שכזה גורם ל-
        // Compose "למלא" את מלוא גובה המסך בפועל (Box שקוף בגודל מסך שלם) - זה בדיוק
        // מה שגרם לבאנר "לחסום" חלק מהמסך ולהיראות מוזר, למרות ש-FLAG_NOT_TOUCHABLE
        // כן מונע ממנו ליירט מגע/מקשים בפועל. fillMaxWidth() מספיק ונותן לחלון לעטוף
        // בדיוק את גובה התוכן האמיתי (הבאנר עצמו).
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(durationMillis = 250, easing = LinearOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(250)),
            exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(200)) + fadeOut(animationSpec = tween(200)),
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp, start = 12.dp, end = 12.dp)
        ) {
            val shape = RoundedCornerShape(28.dp)
            // אותה שפת עיצוב "זכוכית כהה" כמו שאר חלקי FutureUI (Control Center,
            // Notification Center, תפריט הכיבוי) - לא MaterialTheme.colorScheme, כדי
            // שהבאנר לא יבלוט ככתם בהיר/לא עקבי מעל שאר המערכת.
            val textColor = Color.White
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Color(0xE61C1C1E))
                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), shape)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = appIcon
                    if (icon != null) {
                        Image(bitmap = icon, contentDescription = null, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(Icons.Rounded.Notifications, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (title.isNotBlank()) "$appName: $title" else appName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (text.isNotBlank()) {
                        Text(
                            text = text,
                            fontSize = 12.sp,
                            color = textColor.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
