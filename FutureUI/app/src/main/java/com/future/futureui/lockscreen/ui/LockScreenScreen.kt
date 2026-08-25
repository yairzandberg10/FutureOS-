package com.future.futureui.lockscreen.ui

// ייבוא ספריות נדרשות של אנדרואיד וקומפוז (Jetpack Compose)
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.animation.OvershootInterpolator
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import com.future.futureui.lockscreen.logic.LockScreenLayoutManager
import com.future.futureui.notificationcenter.logic.NotificationCenterManager
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * מסך הנעילה הראשי.
 * Composable היא פונקציה שמגדירה חלק מהממשק הויזואלי.
 */
@Composable
fun LockScreenScreen(
    modifier: Modifier = Modifier, // מאפשר לעדכן עיצוב מבחוץ
    wallpaper: ImageBitmap? = null, // תמונת הרקע (טפט)
    onUnlock: () -> Unit = {} // פעולה שתקרה כשמשחררים את הנעילה
) {
    // Context הוא אובייקט שנותן גישה למערכת ההפעלה אנדרואיד
    val context = LocalContext.current
    
    // ניהול הגדרות הפריסה של מסך הנעילה (שעון, קיצורי דרך וכו')
    val layoutManager = remember { LockScreenLayoutManager(context) }
    
    // ניהול התראות המערכת שיוצגו במסך הנעילה
    val notificationManager = remember { NotificationCenterManager(context) }

    // מצב (State): האם אנחנו במצב עריכה? (לחיצה ארוכה על Menu)
    var isEditMode by remember { mutableStateOf(false) }
    
    // איזה חלק נמצא בפוקוס בזמן עריכה? (שעון או קיצור דרך)
    var focusedSector by remember { mutableStateOf("clock") } 
    
    // הגדרות עיצוב שנשמרות בזיכרון המכשיר
    var clockStyle by remember { mutableStateOf(layoutManager.getClockStyle()) }
    var leftShortcut by remember { mutableStateOf(layoutManager.getLeftShortcut()) }
    var rightShortcut by remember { mutableStateOf(layoutManager.getRightShortcut()) }

    // משתנים שמחזיקים את השעה והתאריך הנוכחיים
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    // אובייקט שאחראי לבקש את הפוקוס (כדי שנוכל להקשיב למקשים בשלט/מקלדת)
    val focusRequester = remember { FocusRequester() }
    
    // האם המסך גלוי? (משמש לאנימציית כניסה)
    var isVisible by remember { mutableStateOf(false) }

    // האם המסך בתהליך פתיחת נעילה? (משמש לאנימציית יציאה - עולה למעלה ונעלם,
    // במקום שהחלון פשוט ייעלם באחת ומגלה את מסך הבית מתחתיו בפתאומיות)
    var isUnlocking by remember { mutableStateOf(false) }

    fun performUnlock() {
        isUnlocking = true
    }

    LaunchedEffect(isUnlocking) {
        if (isUnlocking) {
            delay(320)
            onUnlock()
        }
    }

    // מונע החלפת מצב עריכה שוב ושוב בזמן שהמקש עדיין לחוץ (Android שולח KeyDown חוזרים)
    var editModeLongPressHandled by remember { mutableStateOf(false) }

    // --- נעילת קוד PIN ---
    val hasPin = remember { layoutManager.hasPin() }
    var isPinMode by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    val pinLength = 4
    // הפעולה שתתבצע לאחר הזנת קוד PIN נכון (למשל פתיחת קיצור-דרך) - null פירושו פתיחת נעילה רגילה בלבד
    var pendingUnlockAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // אפקט שרץ פעם אחת כשהמסך עולה
    LaunchedEffect(Unit) {
        notificationManager.updateNotifications() // עדכון התראות
        focusRequester.requestFocus() // בקשת פוקוס למקשים
        isVisible = true // הפעלת אנימציית כניסה
        
        // לולאה שרצה כל שנייה ומעדכנת את השעה
        while (true) {
            val now = Calendar.getInstance().time
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            currentDate = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now)
            delay(1000)
        }
    }

    // הגדרה שהמסך יתמוך בכיוון מימין לשמאל (RTL) עבור עברית
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize() // מילוי כל המסך
                .focusRequester(focusRequester) // חיבור לפוקוס
                .focusable() // מאפשר למסך לקבל פוקוס
                .onKeyEvent { event -> // טיפול בלחיצות מקשים (שלט/מקלדת)
                    // מצב הזנת קוד PIN חוסם את כל שאר הטיפול במקשים - מטפלים בו
                    // כאן קודם, לפני הלוגיקה הרגילה של המסך.
                    if (isPinMode) {
                        if (event.type == KeyEventType.KeyUp) {
                            val digit = digitForKey(event.key)
                            when {
                                digit != null -> {
                                    pinError = false
                                    val next = (enteredPin + digit).take(pinLength)
                                    enteredPin = next
                                    if (next.length == pinLength) {
                                        if (layoutManager.verifyPin(next)) {
                                            isPinMode = false
                                            enteredPin = ""
                                            val action = pendingUnlockAction
                                            pendingUnlockAction = null
                                            if (action != null) action() else performUnlock()
                                        } else {
                                            pinError = true
                                            enteredPin = ""
                                        }
                                    }
                                }
                                event.key == Key.Back -> {
                                    if (enteredPin.isNotEmpty()) {
                                        enteredPin = enteredPin.dropLast(1)
                                    } else {
                                        isPinMode = false
                                        pendingUnlockAction = null
                                    }
                                }
                            }
                        }
                        return@onKeyEvent true
                    }
                    if (event.type == KeyEventType.KeyUp) { // כשהמשתמש משחרר את המקש
                        when (event.key) {
                            Key.DirectionCenter, Key.Enter -> { // לחיצה על OK או Enter
                                if (!isEditMode) {
                                    if (hasPin) {
                                        pendingUnlockAction = null
                                        isPinMode = true // יש קוד PIN מוגדר - קודם צריך להזין אותו
                                    } else {
                                        performUnlock() // אין קוד - פתח נעילה ישירות
                                    }
                                } else {
                                    // אם בעריכה - שנה את מה שבפוקוס
                                    when (focusedSector) {
                                        "clock" -> {
                                            clockStyle = (clockStyle + 1) % 4
                                            layoutManager.saveClockStyle(clockStyle)
                                        }
                                        "shortcuts_left" -> {
                                            leftShortcut = nextShortcut(leftShortcut)
                                            layoutManager.saveLeftShortcut(leftShortcut)
                                        }
                                        "shortcuts_right" -> {
                                            rightShortcut = nextShortcut(rightShortcut)
                                            layoutManager.saveRightShortcut(rightShortcut)
                                        }
                                    }
                                }
                                return@onKeyEvent true
                            }
                            Key.Back -> { // לחיצה על "חזור"
                                if (isEditMode) {
                                    isEditMode = false // צא ממצב עריכה
                                } else if (hasPin) {
                                    // יש קוד PIN מוגדר - יש להזין אותו קודם, גם לפני קיצור הדרך
                                    pendingUnlockAction = {
                                        launchShortcut(context, leftShortcut)
                                        performUnlock()
                                    }
                                    isPinMode = true
                                } else {
                                    launchShortcut(context, leftShortcut) // פתח קיצור דרך שמאלי
                                    performUnlock()
                                }
                                return@onKeyEvent true
                            }
                            Key.Settings, Key.Menu -> { // לחיצה קצרה על תפריט
                                // איפוס הדגל לקראת הלחיצה הבאה
                                val wasLongPress = editModeLongPressHandled
                                editModeLongPressHandled = false
                                if (!isEditMode && !wasLongPress) {
                                    if (hasPin) {
                                        // יש קוד PIN מוגדר - יש להזין אותו קודם, גם לפני קיצור הדרך
                                        pendingUnlockAction = {
                                            launchShortcut(context, rightShortcut)
                                            performUnlock()
                                        }
                                        isPinMode = true
                                    } else {
                                        launchShortcut(context, rightShortcut) // פתח קיצור דרך ימני
                                        performUnlock()
                                    }
                                }
                                return@onKeyEvent true
                            }
                        }
                    } else if (event.type == KeyEventType.KeyDown) { // כשהמקש לחוץ
                        if (event.key == Key.Settings || event.key == Key.Menu) {
                            // לחיצה ארוכה (יותר מ-2 חזרות) מכניסה/יוצאת ממצב עריכה - פעם אחת בלבד לכל לחיצה
                            if (event.nativeKeyEvent.repeatCount > 2 && !editModeLongPressHandled) {
                                editModeLongPressHandled = true
                                isEditMode = !isEditMode
                                return@onKeyEvent true
                            }
                            if (event.nativeKeyEvent.repeatCount > 2) {
                                return@onKeyEvent true
                            }
                        }
                        
                        if (isEditMode) { // ניווט עם החצים בתוך מצב עריכה
                            when (event.key) {
                                Key.DirectionUp -> {
                                    focusedSector = "clock"
                                    return@onKeyEvent true
                                }
                                Key.DirectionDown -> {
                                    if (focusedSector == "clock") focusedSector = "shortcuts_left"
                                    return@onKeyEvent true
                                }
                                Key.DirectionLeft -> {
                                    if (focusedSector == "shortcuts_right") focusedSector = "shortcuts_left"
                                    return@onKeyEvent true
                                }
                                Key.DirectionRight -> {
                                    if (focusedSector == "shortcuts_left") focusedSector = "shortcuts_right"
                                    return@onKeyEvent true
                                }
                            }
                        }
                    }
                    false
                }
        ) {
            // כל מסך הנעילה (רקע כולל) "עולה" מלמטה למעלה בכניסה - לא רק התוכן
            // הפנימי - כדי שהפתיחה תורגש כפתיחה אמיתית של המסך כולו. בפתיחת
            // הנעילה עצמה המסך ממשיך לעלות (הפוך מהכניסה) ודועך - כדי שהוא
            // באמת ייעלם במקום "לרחף" מעל מסך הבית שנחשף מתחתיו.
            val density = LocalDensity.current
            val entrySlidePx = with(density) { 140.dp.toPx() }
            val exitSlidePx = with(density) { (-600).dp.toPx() }
            val screenOffset by animateFloatAsState(
                targetValue = when {
                    isUnlocking -> exitSlidePx
                    isVisible -> 0f
                    else -> entrySlidePx
                },
                animationSpec = if (isUnlocking) tween(320, easing = FastOutLinearInEasing) else tween(450, easing = FastOutSlowInEasing),
                label = "lockScreenSlide"
            )
            val screenAlpha by animateFloatAsState(
                targetValue = if (isUnlocking) 0f else 1f,
                animationSpec = tween(280, easing = LinearEasing),
                label = "lockScreenFade"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = screenOffset; alpha = screenAlpha }
            ) {
                // תצוגת הטפט עם אנימציית טשטוש (Blur) כשנכנסים לעריכה
                val blurRadius by animateDpAsState(if (isEditMode) 20.dp else 0.dp, label = "blur")
                if (wallpaper != null) {
                    Image(
                        bitmap = wallpaper,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().blur(blurRadius),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black).blur(blurRadius))
                }

                // שכבת הצללה מעל הטפט כדי שהטקסט יהיה קריא
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = if (isEditMode) 0.5f else 0.2f))
                )

                // התוכן הראשי של המסך (שעון, התראות וקיצורים) עם אנימציית כניסה
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(600)) + expandVertically(tween(600)),
                    modifier = Modifier.fillMaxSize()
                ) {
                // עמודה יחידה שמחלקת את הגובה בצורה מכוונת: שעון למעלה (מתחת לשורת
                // המצב), מרווח גמיש שסופג את שאר החלל, ואשכול "התראות + קיצורים +
                // הנחיית פתיחה" צמוד לתחתית - כך שהמסך תמיד מאוזן ומתאים בדיוק
                // לגובה המסך בפועל (640x960), בלי רווחים מקריים בין החלקים.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = (com.future.futureui.statusbar.logic.StatusBarLayoutManager.HEIGHT_DP + 20).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedClock(
                        time = currentTime,
                        date = currentDate,
                        style = clockStyle,
                        isFocused = isEditMode && focusedSector == "clock"
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    NotificationSummary(
                        notifications = notificationManager.notifications.take(3).map {
                            it.notification.extras.getCharSequence("android.title")?.toString() ?: "התראה"
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 30.dp)
                    ) {
                        // קיצור דרך שמאלי
                        ShortcutCircle(
                            icon = getShortcutIcon(leftShortcut),
                            isFocused = isEditMode && focusedSector == "shortcuts_left",
                            modifier = Modifier.align(Alignment.CenterStart),
                            size = 60.dp
                        )

                        // קיצור דרך ימני
                        ShortcutCircle(
                            icon = getShortcutIcon(rightShortcut),
                            isFocused = isEditMode && focusedSector == "shortcuts_right",
                            modifier = Modifier.align(Alignment.CenterEnd),
                            size = 60.dp
                        )
                    }

                    UnlockPrompt(modifier = Modifier.padding(top = 8.dp, bottom = 28.dp))
                }
                }

                // שכבת הזנת קוד PIN - מכסה את כל המסך כשמוזן קוד
                PinEntryOverlay(visible = isPinMode, enteredLength = enteredPin.length, pinLength = pinLength, isError = pinError)
            }
        }
    }
}

/**
 * שכבת הזנת קוד PIN - מכסה את המסך, מציגה נקודות למספר הספרות שהוזנו (בלי
 * לחשוף את הקוד עצמו), ומטלטלת קלות אם הוזן קוד שגוי.
 */
@Composable
fun PinEntryOverlay(visible: Boolean, enteredLength: Int, pinLength: Int, isError: Boolean) {
    val shake by animateFloatAsState(
        targetValue = if (isError) 1f else 0f,
        animationSpec = if (isError) repeatable(iterations = 3, animation = tween(60), repeatMode = RepeatMode.Reverse) else tween(0),
        label = "pinShake"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(150)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.graphicsLayer { translationX = shake * 12f },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isError) "קוד שגוי, נסה שוב" else "הזן קוד נעילה",
                    color = if (isError) Color(0xFFFF6B6B) else Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    repeat(pinLength) { index ->
                        val filled = index < enteredLength
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (filled) Color.White else Color.White.copy(alpha = 0.25f))
                                .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                        )
                    }
                }
            }
        }
    }
}

/** ממיר מקש מספרי (0-9, מקלדת רגילה או NumPad) לספרה כמחרוזת - לצורך הזנת PIN. */
private fun digitForKey(key: Key): String? = when (key) {
    Key.Zero, Key.NumPad0 -> "0"
    Key.One, Key.NumPad1 -> "1"
    Key.Two, Key.NumPad2 -> "2"
    Key.Three, Key.NumPad3 -> "3"
    Key.Four, Key.NumPad4 -> "4"
    Key.Five, Key.NumPad5 -> "5"
    Key.Six, Key.NumPad6 -> "6"
    Key.Seven, Key.NumPad7 -> "7"
    Key.Eight, Key.NumPad8 -> "8"
    Key.Nine, Key.NumPad9 -> "9"
    else -> null
}

/**
 * פונקציה להפעלת קיצור דרך (Intent) לפי מזהה.
 * היא "צועקת" למערכת לפתוח את האפליקציה המתאימה.
 */
private fun launchShortcut(context: Context, id: String) {
    try {
        val intent = when (id) {
            "phone" -> Intent(Intent.ACTION_DIAL) // חיוג
            "camera" -> Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA) // מצלמה
            "flashlight" -> Intent(Settings.ACTION_DISPLAY_SETTINGS) // פנס (כאן הגדרות תצוגה כדוגמה)
            "settings" -> Intent(Settings.ACTION_SETTINGS) // הגדרות
            else -> context.packageManager.getLaunchIntentForPackage(id) // אפליקציה כללית
        }
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        android.util.Log.e("LockScreen", "Failed to launch shortcut: $id", e)
    }
}

/**
 * טקסט קטן בתחתית המנחה את המשתמש איך לפתוח את הנעילה.
 */
@Composable
fun UnlockPrompt(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "לחץ OK",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * רכיב השעון המונפש.
 * משתנה ויזואלית לפי ה-Style שנבחר ומגיב לפוקוס בזמן עריכה.
 */
@Composable
fun AnimatedClock(
    time: String,
    date: String,
    style: Int, // סגנון השעון (0, 1 או 2)
    isFocused: Boolean, // האם השעון כרגע נבחר לעריכה?
    modifier: Modifier = Modifier
) {
    // אנימציית הגדלה (Scale) כשהשעון בפוקוס
    val scale by animateFloatAsState(if (isFocused) 1.05f else 1f, label = "scale")

    Column(
        modifier = modifier
            .scale(scale)
            .padding(16.dp)
            .then(
                // אם בפוקוס - הוסף מסגרת לבנה מעוגלת
                if (isFocused) Modifier.border(2.dp, Color.White, RoundedCornerShape(20.dp)).padding(10.dp)
                else Modifier
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // אנימציית החלפה חלקה בין סגנונות השעון
        AnimatedContent(
            targetState = style,
            transitionSpec = { fadeIn().togetherWith(fadeOut()) },
            label = "clock"
        ) { targetStyle ->
            when (targetStyle) {
                0 -> { // סגנון עבה ובולט
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = time, fontSize = 70.sp, fontWeight = FontWeight.Black, color = Color.White, lineHeight = 82.sp)
                        Text(text = date, fontSize = 24.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
                1 -> { // סגנון דק ואלגנטי
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = time, fontSize = 80.sp, fontWeight = FontWeight.ExtraLight, color = Color.White)
                        Text(text = date, fontSize = 20.sp, fontWeight = FontWeight.Light, color = Color.White.copy(alpha = 0.9f))
                    }
                }
                2 -> { // סגנון "קוביה" (שעות מעל דקות)
                    val parts = time.split(":")
                    if (parts.size == 2) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = parts[0], fontSize = 80.sp, fontWeight = FontWeight.Bold, color = Color.White, lineHeight = 0.7.em)
                            Text(text = parts[1], fontSize = 80.sp, fontWeight = FontWeight.Bold, color = Color.White, lineHeight = 0.7.em)
                            Text(text = date, fontSize = 18.sp, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }
                3 -> { // סגנון עבה ובולט

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = time, fontSize = 70.sp, fontWeight = FontWeight.SemiBold, color = Color.White, lineHeight = 82.sp)
                        Text(text = date, fontSize = 15.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

/**
 * מציג רשימה קטנה של התראות אחרונות כ"בועות" זכוכית.
 */
@Composable
fun NotificationSummary(
    notifications: List<String>,
    modifier: Modifier = Modifier
) {
    if (notifications.isEmpty()) return // אם אין התראות - אל תציג כלום

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        notifications.forEach { title ->
            Box(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.White.copy(alpha = 0.1f)) // רקע שקוף למחצה
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(text = title, color = Color.White, fontSize = 14.sp, maxLines = 1)
            }
        }
    }
}

/**
 * עיגול קיצור דרך (Shortcut).
 * מציג אייקון ומגיב לפוקוס עריכה.
 */
@Composable
fun ShortcutCircle(
    icon: ImageVector, // האייקון להצגה
    isFocused: Boolean, // האם הקיצור נבחר כרגע לעריכה?
    modifier: Modifier = Modifier,
    size: Dp = 60.dp // גודל העיגול
) {
    // אנימציית הגדלה בפוקוס
    val scale by animateFloatAsState(if (isFocused) 1.2f else 1f, label = "scale")
    
    Box(
        modifier = modifier
            .scale(scale)
            .size(size) // שימוש בפרמטר הגודל
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (isFocused) 0.3f else 0.15f))
            .then(
                if (isFocused) Modifier.border(2.dp, Color.White, CircleShape) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
    }
}

/**
 * פונקציית עזר להחזרת האייקון המתאים לפי מזהה הקיצור.
 */
fun getShortcutIcon(id: String): ImageVector = when(id) {
    "phone" -> Icons.Rounded.Phone
    "camera" -> Icons.Rounded.CameraAlt
    "flashlight" -> Icons.Rounded.FlashlightOn
    "settings" -> Icons.Rounded.Settings
    else -> Icons.Rounded.Apps
}

/**
 * פונקציית עזר שעוברת לקיצור הבא ברשימה (בזמן עריכה).
 */
fun nextShortcut(current: String): String {
    val all = listOf("phone", "camera", "flashlight", "settings")
    val idx = all.indexOf(current)
    return all[(idx + 1) % all.size]
}
