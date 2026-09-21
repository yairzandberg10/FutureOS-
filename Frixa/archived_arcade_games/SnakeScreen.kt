package com.future.frixa.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.frixa.data.HighScores
import com.future.frixa.ui.theme.FrixaColors
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val COLS = 14
private const val ROWS = 20
private data class Cell(val x: Int, val y: Int)
private enum class Direction(val dx: Int, val dy: Int) { UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0) }

/** נחש קלאסי - בדיוק כמו במשחק הפולחני על טלפוני פיצ'ר ישנים, שולט לגמרי
 * בחיצי D-pad. הגוף גדל בכל פעם שאוכלים "מזון", המשחק נגמר בפגיעה בקיר
 * או בגוף עצמו. */
@Composable
fun SnakeScreen() {
    val context = LocalContext.current
    var highScore by remember { mutableIntStateOf(HighScores.getSnakeHighScore(context)) }

    var snake by remember { mutableStateOf(listOf(Cell(COLS / 2, ROWS / 2))) }
    var direction by remember { mutableStateOf(Direction.RIGHT) }
    var pendingDirection by remember { mutableStateOf(Direction.RIGHT) }
    var food by remember { mutableStateOf(Cell(Random.nextInt(COLS), Random.nextInt(ROWS))) }
    var isGameOver by remember { mutableStateOf(false) }
    var tickMillis by remember { mutableIntStateOf(180) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    fun reset() {
        snake = listOf(Cell(COLS / 2, ROWS / 2))
        direction = Direction.RIGHT
        pendingDirection = Direction.RIGHT
        food = Cell(Random.nextInt(COLS), Random.nextInt(ROWS))
        isGameOver = false
        tickMillis = 180
    }

    LaunchedEffect(isGameOver) {
        while (!isGameOver) {
            delay(tickMillis.toLong())
            direction = pendingDirection
            val head = snake.first()
            val newHead = Cell(head.x + direction.dx, head.y + direction.dy)
            val hitsWall = newHead.x !in 0 until COLS || newHead.y !in 0 until ROWS
            val hitsSelf = newHead in snake
            if (hitsWall || hitsSelf) {
                isGameOver = true
                val score = snake.size - 1
                HighScores.submitSnakeScore(context, score)
                highScore = HighScores.getSnakeHighScore(context)
            } else {
                val ateFood = newHead == food
                snake = listOf(newHead) + if (ateFood) snake else snake.dropLast(1)
                if (ateFood) {
                    tickMillis = (tickMillis - 4).coerceAtLeast(80)
                    var newFood: Cell
                    do { newFood = Cell(Random.nextInt(COLS), Random.nextInt(ROWS)) } while (newFood in snake)
                    food = newFood
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FrixaColors.background)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                if (isGameOver) {
                    if (event.key == Key.DirectionCenter || event.key == Key.Enter) { reset(); return@onKeyEvent true }
                    return@onKeyEvent false
                }
                val requested = when (event.key) {
                    Key.DirectionUp -> Direction.UP
                    Key.DirectionDown -> Direction.DOWN
                    Key.DirectionLeft -> Direction.LEFT
                    Key.DirectionRight -> Direction.RIGHT
                    else -> return@onKeyEvent false
                }
                // לא מאפשרים היפוך ישיר לתוך הגוף (למשל ימינה כשזזים שמאלה).
                val isOpposite = requested.dx == -direction.dx && requested.dy == -direction.dy
                if (!isOpposite) pendingDirection = requested
                true
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        ) {
            Text("נחש", color = FrixaColors.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Column(horizontalAlignment = Alignment.End) {
                Text("ניקוד: ${snake.size - 1}", color = FrixaColors.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("שיא: $highScore", color = FrixaColors.tertiary, fontSize = 11.sp)
            }
        }
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .aspectRatio(COLS.toFloat() / ROWS.toFloat())
                .clip(RoundedCornerShape(12.dp))
                .background(FrixaColors.surface),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellW = size.width / COLS
                val cellH = size.height / ROWS
                drawRect(FrixaColors.secondary, topLeft = Offset(food.x * cellW, food.y * cellH), size = Size(cellW, cellH))
                snake.forEachIndexed { index, cell ->
                    drawRect(
                        color = if (index == 0) FrixaColors.primary else FrixaColors.primary.copy(alpha = 0.7f),
                        topLeft = Offset(cell.x * cellW + 1, cell.y * cellH + 1),
                        size = Size(cellW - 2, cellH - 2),
                    )
                }
            }
            if (isGameOver) {
                Box(modifier = Modifier.fillMaxSize().background(FrixaColors.background.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("המשחק נגמר", color = FrixaColors.secondary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("ניקוד: ${snake.size - 1}", color = FrixaColors.onSurface, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("לחץ OK כדי לשחק שוב", color = FrixaColors.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("נווט בחיצים · OK להתחיל מחדש בסיום", color = FrixaColors.onSurfaceVariant, fontSize = 12.sp)
    }
}
