package com.future.settings.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.future.settings.ui.components.*
import com.future.settings.ui.theme.ThemeConfig
import com.future.settings.viewmodel.SettingsViewModel
import kotlin.random.Random

/** צבעי המותג של FutureOS (מהעמוד הנוחת) - סגול-כחול-ענבר, בשימוש בכל האסטר-אגס. */
private val EggViolet = Color(0xFFBF5AF2)
private val EggTeal = Color(0xFF64D2FF)
private val EggAmber = Color(0xFFFF9F0A)

/**
 * אזור שמזהה "לחיצה ארוכה" על מקש OK/Enter - מחזיק KeyDown שנשלח שוב ושוב באנדרואיד
 * בזמן החזקת מקש, אז מסתמכים על שינוי מצב isPressed (לא נדרס בכל KeyDown חוזר) כדי
 * שה-LaunchedEffect לא יתחיל את הטיימר מחדש בכל אירוע - אותו תיקון בדיוק כמו הבאג
 * שתועד ב-FutureUI (עריכת מסך נעילה) עם ריצוד matigo-toggle על כל KeyDown.
 */
@Composable
private fun LongPressOkArea(
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    holdMillis: Long = 800L,
    content: @Composable BoxScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(holdMillis)
            onLongPress()
        }
    }
    Box(
        modifier = modifier
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent {
                if (it.key == Key.DirectionCenter || it.key == Key.Enter || it.key == Key.NumPadEnter) {
                    when (it.type) {
                        KeyEventType.KeyDown -> { isPressed = true; true }
                        KeyEventType.KeyUp -> { isPressed = false; true }
                        else -> false
                    }
                } else false
            },
        content = content
    )
}

// ============================================================================
// Easter egg #1 + #2: לחיצה על "גרסת אנדרואיד" 7 פעמים -> מסך גילוי אנימציה,
// ולחיצה ארוכה על OK משם -> משחק T9 אמיתי.
// ============================================================================

@Composable
fun FutureOsRevealScreen(navController: NavController) {
    val infinite = rememberInfiniteTransition(label = "reveal")
    val hueShift by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "hue"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.9f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    LongPressOkArea(
        onLongPress = { navController.navigate(Screen.T9Game.route) },
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.key == Key.Back) {
                    navController.popBackStack(); true
                } else false
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(android.graphics.Color.HSVToColor(floatArrayOf(hueShift, 0.55f, 0.35f))),
                            Color.Black
                        ),
                        radius = 900f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size((140 * pulse).dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(listOf(EggViolet, EggTeal, EggAmber, EggViolet))
                        )
                )
                Spacer(modifier = Modifier.height(28.dp))
                Text("FutureOS", fontSize = 40.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text("תמיד קדימה", fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    "החזק OK למשחק חבוי",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }
    }
}

// ============================================================================
// Easter egg #2: משחק T9 אמיתי - התחמקות ממכשולים עם מקשי 4/6 (או חצים).
// ============================================================================

private data class T9Obstacle(val id: Int, val lane: Int, var y: Float)
private enum class T9GameState { Playing, GameOver }

@Composable
fun T9GameScreen(navController: NavController, viewModel: SettingsViewModel) {
    var playerLane by remember { mutableStateOf(1) }
    val obstacles = remember { mutableStateListOf<T9Obstacle>() }
    var nextId by remember { mutableStateOf(0) }
    var survivedMs by remember { mutableStateOf(0L) }
    var msSinceSpawn by remember { mutableStateOf(0f) }
    var gameState by remember { mutableStateOf(T9GameState.Playing) }
    var restartTrigger by remember { mutableStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val highScore = viewModel.t9HighScore.value
    val score = (survivedMs / 100L).toInt()

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    LaunchedEffect(restartTrigger) {
        playerLane = 1
        obstacles.clear()
        nextId = 0
        survivedMs = 0L
        msSinceSpawn = 0f
        gameState = T9GameState.Playing

        var lastTime = withFrameMillis { it }
        while (gameState == T9GameState.Playing) {
            val frameTime = withFrameMillis { it }
            val delta = (frameTime - lastTime).coerceIn(0L, 50L)
            lastTime = frameTime
            survivedMs += delta

            val speed = 0.00028f * delta * (1f + score / 40f)
            obstacles.forEach { it.y += speed }

            msSinceSpawn += delta
            val spawnInterval = (1100 - score.coerceAtMost(55) * 10).coerceAtLeast(420)
            if (msSinceSpawn >= spawnInterval) {
                msSinceSpawn = 0f
                obstacles.add(T9Obstacle(nextId++, Random.nextInt(3), -0.05f))
            }

            val playerY = 0.85f
            var collided = false
            obstacles.forEach { o ->
                if (!collided && o.lane == playerLane && o.y in (playerY - 0.06f)..(playerY + 0.06f)) {
                    collided = true
                }
            }
            obstacles.removeAll { it.y > 1.1f }

            if (collided) {
                gameState = T9GameState.GameOver
                viewModel.submitT9Score(score)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A12))
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when {
                    event.key == Key.Back -> { navController.popBackStack(); true }
                    gameState != T9GameState.Playing -> false
                    event.key == Key.Four || event.key == Key.NumPad4 || event.key == Key.DirectionLeft ->
                        { playerLane = (playerLane - 1).coerceAtLeast(0); true }
                    event.key == Key.Six || event.key == Key.NumPad6 || event.key == Key.DirectionRight ->
                        { playerLane = (playerLane + 1).coerceAtMost(2); true }
                    else -> false
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val laneWidth = size.width / 3f
            // מסלולים
            for (i in 1..2) {
                drawRect(
                    color = Color.White.copy(alpha = 0.06f),
                    topLeft = Offset(laneWidth * i - 1f, 0f),
                    size = Size(2f, size.height)
                )
            }
            // מכשולים
            obstacles.forEach { o ->
                val cx = laneWidth * (o.lane + 0.5f)
                val cy = o.y * size.height
                drawRect(
                    color = EggAmber,
                    topLeft = Offset(cx - laneWidth * 0.28f, cy - 18f),
                    size = Size(laneWidth * 0.56f, 36f)
                )
            }
            // שחקן
            val px = laneWidth * (playerLane + 0.5f)
            val py = 0.85f * size.height
            drawRect(
                color = if (gameState == T9GameState.GameOver) Color.Red else EggTeal,
                topLeft = Offset(px - laneWidth * 0.24f, py - 16f),
                size = Size(laneWidth * 0.48f, 32f)
            )
        }

        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text("ניקוד: $score", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("שיא: $highScore", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
        }

        if (gameState == T9GameState.Playing) {
            Text(
                "4 / 6 או ←→ לתזוזה",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
            )
        }

        if (gameState == T9GameState.GameOver) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("המשחק נגמר", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("ניקוד: $score", color = Color.White, fontSize = 16.sp)
                    Text(
                        if (score >= highScore && score > 0) "שיא חדש! 🎉" else "שיא: ${viewModel.t9HighScore.value}",
                        color = EggAmber, fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { restartTrigger++ },
                        colors = ButtonDefaults.buttonColors(containerColor = EggTeal)
                    ) { Text("שחק שוב", color = Color.Black, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// ============================================================================
// Easter egg #3: קוד קונאמי במקלדת המספרים (8-8-2-2-4-6-4-6, כמו בנוקיה סנייק) ->
// "מצב חגיגה" - קונפטי אמיתי + צבע הדגשה מתחלף.
// ============================================================================

val KONAMI_NUMPAD_SEQUENCE = listOf(8, 8, 2, 2, 4, 6, 4, 6)

fun digitForKey(key: Key): Int? = when (key) {
    Key.Zero, Key.NumPad0 -> 0
    Key.One, Key.NumPad1 -> 1
    Key.Two, Key.NumPad2 -> 2
    Key.Three, Key.NumPad3 -> 3
    Key.Four, Key.NumPad4 -> 4
    Key.Five, Key.NumPad5 -> 5
    Key.Six, Key.NumPad6 -> 6
    Key.Seven, Key.NumPad7 -> 7
    Key.Eight, Key.NumPad8 -> 8
    Key.Nine, Key.NumPad9 -> 9
    else -> null
}

private data class ConfettiParticle(
    val xFrac: Float, val color: Color, val angle: Float, val speed: Float, val size: Float, val spin: Float
)

/** תוקע קונפטי אמיתי (לא תמונה סטטית) - מבוסס פיזיקה פשוטה (כבידה + דעיכה),
 *  מונע ע"י withFrameMillis כדי לא לחסום שום thread. trigger>0 חדש = פיצוץ חדש. */
@Composable
fun ConfettiBurst(trigger: Int, modifier: Modifier = Modifier) {
    var particles by remember { mutableStateOf(emptyList<ConfettiParticle>()) }
    var elapsedMs by remember { mutableStateOf(0L) }
    LaunchedEffect(trigger) {
        if (trigger <= 0) return@LaunchedEffect
        particles = List(36) {
            ConfettiParticle(
                xFrac = Random.nextFloat(),
                color = listOf(EggViolet, EggTeal, EggAmber, Color(0xFF30D158), Color.White).random(),
                angle = Random.nextFloat() * 360f,
                speed = 0.4f + Random.nextFloat() * 0.6f,
                size = 5f + Random.nextFloat() * 7f,
                spin = (Random.nextFloat() - 0.5f) * 8f
            )
        }
        val start = withFrameMillis { it }
        var now = start
        while (now - start < 2200L) {
            now = withFrameMillis { it }
            elapsedMs = now - start
        }
        particles = emptyList()
    }
    if (particles.isNotEmpty()) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val t = elapsedMs / 1000f
            particles.forEach { p ->
                val x = p.xFrac * size.width
                val vy = 260f + p.speed * 280f
                val y = -30f + vy * t + 0.5f * 480f * t * t
                if (y < size.height + 40f) {
                    val alpha = (1f - t / 2.2f).coerceIn(0f, 1f)
                    rotate(p.angle + p.spin * t * 90f, pivot = Offset(x, y)) {
                        drawRect(
                            color = p.color.copy(alpha = alpha),
                            topLeft = Offset(x - p.size / 2, y - p.size / 2),
                            size = Size(p.size, p.size)
                        )
                    }
                }
            }
        }
    }
}

/** מצב חגיגה זמני בכוונה - 5 דקות מהזנת הקוד, עם טיימר גלוי; כשהזמן נגמר
 *  המסך יוצא לבד (וגם הצבע המתחלף עוצר את עצמו, ב-toggleCyclingAccent). */
@Composable
fun PartyModeScreen(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel) {
    var burstTrigger by remember { mutableStateOf(1) }
    var remainingMs by remember {
        mutableStateOf((viewModel.partyModeExpiresAt.value - System.currentTimeMillis()).coerceAtLeast(0L))
    }

    LaunchedEffect(viewModel.partyModeExpiresAt.value) {
        while (true) {
            remainingMs = (viewModel.partyModeExpiresAt.value - System.currentTimeMillis()).coerceAtLeast(0L)
            if (remainingMs <= 0L) {
                navController.popBackStack()
                break
            }
            kotlinx.coroutines.delay(1000)
        }
    }

    val minutes = (remainingMs / 60000L).toInt()
    val seconds = ((remainingMs / 1000L) % 60L).toInt()

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader("🎉 מצב חגיגה", theme) { navController.popBackStack() }
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    "מצאת קוד סודי! זו תודה קטנה על שאתה חוקר את המכשיר שלך לעומק.",
                    color = theme.textColor.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "נגמר בעוד %d:%02d".format(minutes, seconds),
                    color = theme.primaryColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { burstTrigger++ },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("עוד קונפטי!", color = theme.backgroundColor, fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.height(16.dp))
                SettingsCard(theme) {
                    SettingSwitch(
                        "צבע הדגשה מתחלף",
                        "צבע הדגשה של FutureOS ירקוד לבד עד שהזמן נגמר",
                        viewModel.cyclingAccentEnabled.value,
                        { viewModel.toggleCyclingAccent() },
                        theme
                    )
                }
            }
        }
        ConfettiBurst(trigger = burstTrigger, modifier = Modifier.fillMaxSize())
    }
}

// ============================================================================
// Easter egg #4: לחיצה ארוכה על אייקון הטלפון ב"אודות" -> מסך "פלקס" בסגנון
// neofetch, עם נתוני מכשיר אמיתיים (לא בדויים - אותם נתונים ממש כמו "אודות").
// ============================================================================

@Composable
fun AsciiFlexScreen(navController: NavController, viewModel: SettingsViewModel) {
    val interactor = remember { viewModel.getSystemInteractor() }
    val ram = remember { interactor.getRamInfo() }
    val usedGb = (ram.totalBytes - ram.availBytes) / (1024.0 * 1024.0 * 1024.0)
    val totalGb = ram.totalBytes / (1024.0 * 1024.0 * 1024.0)
    val cpu = remember { interactor.getCpuInfo() }
    val uptime = remember { interactor.getUptimeString() }
    val kernel = remember { interactor.getKernelVersion() }
    val rooted = remember { interactor.isRooted() }

    val ascii = """
        █▀▀ █░█ ▀█▀ █░█ █▀█ █▀▀
        █▀░ █▄█ ░█░ █▄█ █▀▄ ██▄
    """.trimIndent()

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.key == Key.Back) {
                    navController.popBackStack(); true
                } else false
            }
            .padding(20.dp)
    ) {
        Column {
            Text(ascii, color = Color(0xFF39FF6A), fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 15.sp)
            Spacer(modifier = Modifier.height(16.dp))
            val lines = listOf(
                "דגם" to android.os.Build.MODEL,
                "מערכת" to "FutureOS (Android ${android.os.Build.VERSION.RELEASE})",
                "קרנל" to kernel,
                "זמן פעולה" to uptime,
                "זיכרון" to "%.1fGB / %.1fGB".format(usedGb, totalGb),
                "מעבד" to cpu,
                "Root" to if (rooted) "כן" else "לא"
            )
            lines.forEach { (label, value) ->
                Text(
                    "$label: $value",
                    color = Color(0xFF39FF6A),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "← Back לחזרה",
                color = Color(0xFF39FF6A).copy(alpha = 0.5f),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }
    }
}

// ============================================================================
// Easter egg #5: הקשה מהירה 5 פעמים על # ב"אודות" -> פתק מזל גיקי אקראי.
// ============================================================================

private val GEEKY_FORTUNES = listOf(
    "יש רק 10 סוגי אנשים בעולם: אלה שמבינים בינארי, ואלה שלא.",
    "המכשיר שלך רץ על Android, אבל הלב שלו רץ על T9.",
    "root אמיתי פירושו כוח אמיתי - השתמש בו בחוכמה.",
    "כל הגדרה כאן היא אמיתית. אף מתג לא מתחזה.",
    "יום אחד גם למחשבון יהיה זיכרון היסטוריה. היום הוא רק מחשב.",
    "FutureOS: כי גם בלי מסך מגע, אפשר להרגיש מתקדם.",
    "יש לך 8 ליבות מעבד ושתי ידיים. תשתמש בכולן.",
    "מפתחים אמיתיים קוראים לוגים באור נר.",
    "אם זה עובד, אל תיגע בו. אם זה לא עובד, בטח יש כאן Easter Egg."
)

/**
 * חלון הודעה גנרי לכל האסטר-אגס שרק "מגלים משהו כתוב" (פתק מזל, בדיחת מפתחים,
 * מסר אישיות אמיתי לפי מצב סוללה/אחסון) - כדי לא לשכפל את אותו UI שוב ושוב.
 * דורש focusRequester מפורש: בלעדיו החלון הזה לא באמת מקבל את לחיצת המקש הבאה
 * כי שום דבר לא ביקש עבורו פוקוס - הוא לא צאצא של מה שכבר ממוקד במסך.
 */
@Composable
fun EasterEggMessage(icon: String, title: String, message: String, onDismiss: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1C1C1E))
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent {
                    if (it.type == KeyEventType.KeyDown) { onDismiss(); true } else false
                }
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$icon $title", color = EggAmber, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Text(message, color = Color.White, fontSize = 15.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun FortuneToast(fortune: String, onDismiss: () -> Unit) =
    EasterEggMessage("🥠", "פתק מזל", fortune, onDismiss)

fun randomFortune(): String = GEEKY_FORTUNES.random()
