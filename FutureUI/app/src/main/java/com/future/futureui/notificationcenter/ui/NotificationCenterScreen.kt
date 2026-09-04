package com.future.futureui.notificationcenter.ui
import com.future.sharednav.focus.bringIntoViewOnFocus

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.drawable.Drawable
import android.service.notification.StatusBarNotification
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.future.futureui.controlcenter.ui.components.focusEffect
import com.future.futureui.notificationcenter.logic.NotificationCenterManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * מסך מרכז ההתראות הראשי - מציג רשימת התראות, שעון, תאריך וכפתורי פעולה בתחתית.
 */
@Composable
fun NotificationCenterScreen(
    modifier: Modifier = Modifier,
    wallpaper: ImageBitmap? = null,
    manager: NotificationCenterManager? = null,
    onSwitchToControlCenter: () -> Unit = {}
) {
    val context = LocalContext.current
    val ncManager = manager ?: remember { NotificationCenterManager(context) }

    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    val hebrewDateFormat = remember {
        android.icu.text.DateFormat.getDateInstance(
            android.icu.text.DateFormat.FULL,
            android.icu.util.ULocale.forLanguageTag("he-IL-u-ca-hebrew")
        )
    }

    // עדכון זמן ותאריך ורענון התראות כל 30 שניות
    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance().time
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            currentDate = hebrewDateFormat.format(now)
            ncManager.updateNotifications()
            delay(1000 * 30) // רענון כל 30 שניות
        }
    }

    val isDarkBackground = wallpaper != null
    val textColor = if (isDarkBackground) Color.White else Color.Black
    val subTextColor = if (isDarkBackground) Color.LightGray else Color.Gray

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    // בלי בקשת פוקוס ראשונית, אף התראה לא ממוקדת כשהמסך נפתח - ואז מקשי השלט
    // (מחיקה, פתיחה, מעבר ל-Control Center) לא מגיעים לשום מקום.
    val initialFocusRequester = remember { FocusRequester() }
    // כשאין התראות בכלל, אין פריט ראשון לממקד - וכתוצאה מכך גם כפתורי הפעולה
    // התחתונים ("נקה הכל"/"השתק") נשארים בלתי-נגישים לחלוטין. ממקדים את הראשון
    // מביניהם באופן מפורש במקרה הזה.
    val clearAllFocusRequester = remember { FocusRequester() }
    LaunchedEffect(isVisible, ncManager.notifications.size) {
        if (isVisible && ncManager.notifications.isNotEmpty()) {
            delay(150)
            try { initialFocusRequester.requestFocus() } catch (e: Exception) {}
        }
    }
    LaunchedEffect(isVisible, ncManager.notifications.size) {
        if (isVisible && ncManager.notifications.isEmpty()) {
            delay(150)
            try { clearAllFocusRequester.requestFocus() } catch (e: Exception) {}
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AnimatedVisibility(
            visible = isVisible,
            // כניסה עם spring קליל (overshoot עדין) במקום tween ליניארי - מרגישה
            // "חיה" יותר, כמו וילון התראות אמיתי, ולא כמו View שקופץ למקומו.
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
            ) + fadeIn(animationSpec = tween(durationMillis = 220)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(durationMillis = 220, easing = FastOutLinearInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = 180))
        ) {
            Box(modifier = modifier.fillMaxSize()) {
                // רקע עם טשטוש (Blur) אם קיימת תמונת רקע
                if (wallpaper != null) {
                    Image(
                        bitmap = wallpaper,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().blur(40.dp),
                        contentScale = ContentScale.Crop
                    )
                }

                // שכבת כיסוי חצי שקופה
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (wallpaper != null) Color.White.copy(alpha = 0.2f)
                            else Color(0xCCFFFFFF)
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(top = com.future.futureui.statusbar.logic.StatusBarLayoutManager.HEIGHT_DP.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // כותרת עליונה: שעון ותאריך
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = currentTime,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                style = androidx.compose.ui.text.TextStyle(
                                    shadow = androidx.compose.ui.graphics.Shadow(color = Color.Black.copy(alpha = 0.3f), blurRadius = 8f)
                                )
                            )
                            Text(
                                text = currentDate,
                                fontSize = 12.sp,
                                color = subTextColor,
                                style = androidx.compose.ui.text.TextStyle(
                                    shadow = androidx.compose.ui.graphics.Shadow(color = Color.Black.copy(alpha = 0.3f), blurRadius = 8f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // רשימת התראות נגללת
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        if (ncManager.notifications.isEmpty()) {
                            // תצוגה כשאין התראות
                            item {
                                Box(
                                    modifier = Modifier.fillParentMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "אין התראות חדשות",
                                        color = subTextColor,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        } else {
                            // הצגת כל התראה ברשימה - כניסה מדורגת (stagger לפי אינדקס) כדי
                            // שההתראות "יזרמו" פנימה אחת אחרי השנייה במקום לקפוץ כולן ביחד.
                        itemsIndexed(ncManager.notifications, key = { _, sbn -> sbn.key }) { index, sbn ->
                            val visibleState = remember {
                                MutableTransitionState(false)
                            }
                            var isDismissing by remember { mutableStateOf(false) }

                            LaunchedEffect(Unit) {
                                delay((index * 45L).coerceAtMost(360L))
                                visibleState.targetState = true
                            }
                            LaunchedEffect(isDismissing) {
                                if (isDismissing) {
                                    visibleState.targetState = false
                                    delay(220)
                                    ncManager.dismissNotification(sbn)
                                }
                            }

                            AnimatedVisibility(
                                visibleState = visibleState,
                                enter = fadeIn(animationSpec = tween(220)) +
                                    slideInVertically(
                                        initialOffsetY = { -it / 3 },
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                                    ) +
                                    expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)),
                                exit = fadeOut(animationSpec = tween(180)) +
                                    slideOutHorizontally(targetOffsetX = { it / 4 }, animationSpec = tween(200)) +
                                    shrinkVertically(animationSpec = tween(200))
                            ) {
                                NotificationItem(
                                    sbn = sbn,
                                    onSwitchToControlCenter = onSwitchToControlCenter,
                                    onDismiss = { isDismissing = true },
                                    onOpen = { ncManager.launchApp(sbn.packageName) },
                                    onMuteApp = { ncManager.openNotificationSettings(sbn.packageName) },
                                    textColor = textColor,
                                    subTextColor = subTextColor,
                                    isDarkBackground = isDarkBackground,
                                    focusRequester = if (index == 0) initialFocusRequester else null
                                )
                            }
                        }
                        }
                    }

                    // כפתורי פעולה בתחתית (נקה הכל והשתק)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NotificationCenterButton(
                            text = "נקה הכל",
                            icon = Icons.Rounded.DeleteSweep,
                            onClick = { ncManager.clearAll() },
                            modifier = Modifier.weight(1f),
                            color = Color.Black.copy(alpha = 0.3f),
                            focusRequester = clearAllFocusRequester
                        )
                        NotificationCenterButton(
                            text = "השתק",
                            icon = if (ncManager.controlManager.isDndOn) Icons.Rounded.NotificationsOff else Icons.Rounded.NotificationsActive,
                            onClick = { ncManager.toggleDnd() },
                            modifier = Modifier.weight(1f),
                            color = Color.Black.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * פריט התראה בודד ברשימה
 */
@Composable
fun NotificationItem(
    sbn: StatusBarNotification,
    onSwitchToControlCenter: () -> Unit,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onMuteApp: () -> Unit,
    textColor: Color,
    subTextColor: Color,
    isDarkBackground: Boolean = false,
    focusRequester: FocusRequester? = null
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var isExpanded by remember { mutableStateOf(false) }
    var showOptions by remember { mutableStateOf(false) }
    var selectedOptionIndex by remember { mutableStateOf(0) }

    val n = sbn.notification
    val title = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
    val text = n.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

    // הפעולות האמיתיות שההתראה הביאה איתה מהאפליקציה המקורית (למשל "ענה"/"דחה"
    // בהתראת שיחה, או "סמן כנקרא") - בלי RemoteInput כי אין מקלדת מגע להקליד תשובה.
    data class NotificationOption(val label: String, val isDestructive: Boolean = false, val action: () -> Unit)
    val notificationActions = remember(sbn.key) {
        n.actions?.filter { it.remoteInputs.isNullOrEmpty() }?.take(2) ?: emptyList()
    }
    val options = remember(notificationActions) {
        buildList {
            notificationActions.forEach { act ->
                add(NotificationOption(label = act.title?.toString() ?: "") {
                    try { act.actionIntent?.send() } catch (e: PendingIntent.CanceledException) {}
                })
            }
            add(NotificationOption(label = "כבה התראות", isDestructive = true) { onMuteApp() })
            add(NotificationOption(label = "חזור") { })
        }
    }
    LaunchedEffect(showOptions) { if (showOptions) selectedOptionIndex = 0 }
    
    // שליפת שם האפליקציה לפי Package Name
    val appName = remember(sbn.packageName) {
        try {
            val ai = context.packageManager.getApplicationInfo(sbn.packageName, 0)
            context.packageManager.getApplicationLabel(ai).toString()
        } catch (e: Exception) {
            sbn.packageName.substringAfterLast(".")
        }
    }

    // טעינת אייקון האפליקציה
    var appIcon by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(sbn.packageName) {
        try {
            val icon = context.packageManager.getApplicationIcon(sbn.packageName)
            appIcon = icon.toBitmap(width = 64, height = 64).asImageBitmap()
        } catch (e: Exception) {}
    }

    val shape = RoundedCornerShape(35.dp)
    // הרקע של הכרטיס נגזר מכיוון הטקסט (isDarkBackground) ולא מהטפט עצמו, כדי
    // שהניגודיות טקסט-מול-כרטיס תישמר גם כשהטפט שמתחת בהיר או כהה באופן בלתי צפוי.
    val cardBackground = if (isDarkBackground) {
        if (isFocused) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.38f)
    } else {
        if (isFocused) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.42f)
    }
    val legibilityShadow = androidx.compose.ui.graphics.Shadow(color = Color.Black.copy(alpha = 0.35f), blurRadius = 6f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(durationMillis = 200))
            .focusEffect(isFocused, shape)
            .clip(shape)
            .background(cardBackground)
            .then(
                if (isFocused) Modifier.border(2.dp, Color.LightGray, shape) else Modifier
            )
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onKeyEvent { event ->
                if (!isFocused) return@onKeyEvent false
                if (showOptions) {
                    when (event.key) {
                        // ניווט בין הפעולות הזמינות בתפריט
                        Key.DirectionLeft -> {
                            if (event.type == KeyEventType.KeyDown) {
                                selectedOptionIndex = (selectedOptionIndex - 1 + options.size) % options.size
                            }
                            true
                        }
                        Key.DirectionRight -> {
                            if (event.type == KeyEventType.KeyDown) {
                                selectedOptionIndex = (selectedOptionIndex + 1) % options.size
                            }
                            true
                        }
                        // OK קצר - הפעלת הפעולה המסומנת (למשל "ענה" בהתראת שיחה)
                        Key.DirectionCenter, Key.Enter -> {
                            if (event.type == KeyEventType.KeyUp && event.nativeKeyEvent.repeatCount == 0) {
                                options.getOrNull(selectedOptionIndex)?.action?.invoke()
                                showOptions = false
                            }
                            true
                        }
                        Key.Menu -> {
                            if (event.type == KeyEventType.KeyUp) showOptions = false
                            true
                        }
                        else -> false
                    }
                } else {
                    when (event.key) {
                        // כפתור OK ארוך - פתיחת האפליקציה שמקורה בהתראה
                        Key.DirectionCenter, Key.Enter -> {
                            if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount > 8) {
                                onOpen()
                                return@onKeyEvent true
                            }
                            false
                        }
                        // חץ שמאל ארוך - מעבר למרכז הבקרה
                        Key.DirectionLeft -> {
                            if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount > 8) {
                                onSwitchToControlCenter()
                                return@onKeyEvent true
                            }
                            false
                        }
                        // חץ ימין ארוך - מחיקת ההתראה הזו. בכוונה לא מגיבים ללחיצה קצרה/בודדת:
                        // חץ ימין הוא גם מקש ניווט רגיל, ולחיצה קצרה בטעות לא אמורה למחוק
                        // התראה בלי אפשרות ביטול. דורשים החזקה (כמו יתר הפעולות הארוכות כאן).
                        Key.DirectionRight -> {
                            if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount > 8) {
                                onDismiss()
                                return@onKeyEvent true
                            }
                            false
                        }
                        // מקש Options (Menu) - פתיחת תפריט אפשרויות/פעולות
                        Key.Menu -> {
                            if (event.type == KeyEventType.KeyUp) {
                                showOptions = true
                            }
                            true
                        }
                        else -> false
                    }
                }
            }
            // לחיצה רגילה - הרחבה/צמצום של פרטי ההתראה
            .clickable(interactionSource = interactionSource, indication = null, onClick = {
                if (showOptions) showOptions = false else isExpanded = !isExpanded
            })
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // מעבר חלק (fade צולב) בין תוכן ההתראה הרגיל לתפריט האפשרויות, במקום
        // חיתוך קשה - כך שפתיחת/סגירת התפריט מרגישה כמו טרנזישן מכוון ולא כמו
        // "קפיצה" של הממשק.
        Crossfade(targetState = showOptions, animationSpec = tween(180), label = "notificationItemContent") { optionsVisible ->
            if (optionsVisible) {
                // תפריט אפשרויות (Overlay) - פעולות אמיתיות מההתראה + השתקה/חזרה,
                // ניתנות לניווט בין הכפתורים עם חצי כיוון ובחירה עם OK.
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    options.forEachIndexed { index, option ->
                        val isSelected = index == selectedOptionIndex
                        val baseColor = if (option.isDestructive) Color.Red.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.2f)
                        Text(
                            text = option.label,
                            color = textColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            style = androidx.compose.ui.text.TextStyle(shadow = legibilityShadow),
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(baseColor)
                                .then(
                                    if (isSelected) Modifier.border(1.5.dp, Color.White, CircleShape) else Modifier
                                )
                                .clickable {
                                    option.action()
                                    showOptions = false
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // אייקון האפליקציה (בצד ימין ב-RTL)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (appIcon != null) {
                            Image(
                                bitmap = appIcon!!,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Rounded.Notifications, contentDescription = null, tint = subTextColor, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // תוכן ההתראה: שם אפליקציה ותקציר הודעה
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (title.isNotBlank()) "$appName: $title" else appName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = androidx.compose.ui.text.TextStyle(shadow = legibilityShadow)
                        )
                        Text(
                            text = if (isExpanded) text else if (text.length > 40) text.take(40) + "..." else text,
                            fontSize = 11.sp,
                            color = subTextColor,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis,
                            style = androidx.compose.ui.text.TextStyle(shadow = legibilityShadow)
                        )
                    }
                }
            }
        }
    }
}

/**
 * כפתור מעוצב עבור מרכז ההתראות (בסגנון Pill שקוף חצי-כהה)
 */
@Composable
fun NotificationCenterButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color,
    focusRequester: FocusRequester? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = CircleShape

    Box(
        modifier = modifier
            .height(38.dp)
            .focusEffect(isFocused, shape)
            .clip(shape)
            .background(if (isFocused) Color.White.copy(alpha = 0.25f) else color)
            .then(
                if (isFocused) Modifier.border(2.dp, Color.White, shape) else Modifier
            )
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
    }
}
