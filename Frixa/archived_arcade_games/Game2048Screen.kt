package com.future.frixa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import kotlin.random.Random

private const val SIZE = 4

private fun emptyGrid(): Array<IntArray> = Array(SIZE) { IntArray(SIZE) }

private fun cloneGrid(grid: Array<IntArray>): Array<IntArray> = Array(SIZE) { grid[it].copyOf() }

private fun spawnTile(grid: Array<IntArray>) {
    val emptyCells = mutableListOf<Pair<Int, Int>>()
    for (r in 0 until SIZE) for (c in 0 until SIZE) if (grid[r][c] == 0) emptyCells.add(r to c)
    if (emptyCells.isEmpty()) return
    val (r, c) = emptyCells.random()
    grid[r][c] = if (Random.nextFloat() < 0.9f) 2 else 4
}

/** דוחס+ממזג שורה אחת שמאלה (כל שאר הכיוונים מסתובבים לזה ואז חזרה). מחזיר
 * את השורה החדשה + כמה נקודות נוספו מהמיזוגים. */
private fun compressRowLeft(row: IntArray): Pair<IntArray, Int> {
    val nonZero = row.filter { it != 0 }.toMutableList()
    var gained = 0
    var i = 0
    while (i < nonZero.size - 1) {
        if (nonZero[i] == nonZero[i + 1]) {
            nonZero[i] *= 2
            gained += nonZero[i]
            nonZero.removeAt(i + 1)
        }
        i++
    }
    while (nonZero.size < SIZE) nonZero.add(0)
    return nonZero.toIntArray() to gained
}

private fun rotateClockwise(grid: Array<IntArray>): Array<IntArray> {
    val result = emptyGrid()
    for (r in 0 until SIZE) for (c in 0 until SIZE) result[c][SIZE - 1 - r] = grid[r][c]
    return result
}

private enum class Move { LEFT, RIGHT, UP, DOWN }

/** מפעיל תזוזה בכיוון נתון - כל הכיוונים ממומשים דרך סיבוב הרשת כך ש"שמאלה"
 * תמיד יהיה כיוון הדחיסה הבסיסי, ואז מסובבים בחזרה. מחזיר רשת חדשה, נקודות
 * שנוספו, והאם משהו בכלל זז (כדי לא להוסיף אריח חדש על "מהלך" שלא שינה כלום). */
private fun applyMove(grid: Array<IntArray>, move: Move): Triple<Array<IntArray>, Int, Boolean> {
    val rotations = when (move) {
        Move.LEFT -> 0
        Move.UP -> 1
        Move.RIGHT -> 2
        Move.DOWN -> 3
    }
    var working = cloneGrid(grid)
    repeat(rotations) { working = rotateClockwise(working) }

    var totalGained = 0
    val newRows = Array(SIZE) { r ->
        val (compressed, gained) = compressRowLeft(working[r])
        totalGained += gained
        compressed
    }

    var result = newRows
    repeat((4 - rotations) % 4) { result = rotateClockwise(result) }

    val changed = (0 until SIZE).any { r -> (0 until SIZE).any { c -> result[r][c] != grid[r][c] } }
    return Triple(result, totalGained, changed)
}

private fun hasAnyMove(grid: Array<IntArray>): Boolean {
    for (r in 0 until SIZE) for (c in 0 until SIZE) {
        if (grid[r][c] == 0) return true
        if (c < SIZE - 1 && grid[r][c] == grid[r][c + 1]) return true
        if (r < SIZE - 1 && grid[r][c] == grid[r + 1][c]) return true
    }
    return false
}

private fun tileColor(value: Int): androidx.compose.ui.graphics.Color = when (value) {
    0 -> FrixaColors.surfaceContainer
    2 -> FrixaColors.surfaceContainerHigh
    4 -> FrixaColors.surfaceContainerHighest
    8 -> FrixaColors.tertiary.copy(alpha = 0.4f)
    16 -> FrixaColors.tertiary.copy(alpha = 0.6f)
    32 -> FrixaColors.secondary.copy(alpha = 0.5f)
    64 -> FrixaColors.secondary.copy(alpha = 0.75f)
    128 -> FrixaColors.primary.copy(alpha = 0.5f)
    256 -> FrixaColors.primary.copy(alpha = 0.65f)
    512 -> FrixaColors.primary.copy(alpha = 0.8f)
    1024 -> FrixaColors.primary
    else -> FrixaColors.tertiary
}

/** 2048 קלאסי על רשת 4x4 - חצי D-pad מזיזים את כל האריחים בכיוון אחד, אריחים
 * זהים שנפגשים מתמזגים. משחק חדש נפתח עם שני אריחים אקראיים. */
@Composable
fun Game2048Screen() {
    val context = LocalContext.current
    var highScore by remember { mutableIntStateOf(HighScores.get2048HighScore(context)) }
    var grid by remember { mutableStateOf(emptyGrid().also { spawnTile(it); spawnTile(it) }) }
    var score by remember { mutableIntStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    fun reset() {
        grid = emptyGrid().also { spawnTile(it); spawnTile(it) }
        score = 0
        isGameOver = false
    }

    fun handleMove(move: Move) {
        if (isGameOver) return
        val (result, gained, changed) = applyMove(grid, move)
        if (changed) {
            spawnTile(result)
            grid = result
            score += gained
            if (!hasAnyMove(result)) {
                isGameOver = true
                HighScores.submit2048Score(context, score)
                highScore = HighScores.get2048HighScore(context)
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
                when (event.key) {
                    Key.DirectionUp -> handleMove(Move.UP)
                    Key.DirectionDown -> handleMove(Move.DOWN)
                    Key.DirectionLeft -> handleMove(Move.LEFT)
                    Key.DirectionRight -> handleMove(Move.RIGHT)
                    else -> return@onKeyEvent false
                }
                true
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("2048", color = FrixaColors.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Column(horizontalAlignment = Alignment.End) {
                Text("ניקוד: $score", color = FrixaColors.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("שיא: $highScore", color = FrixaColors.tertiary, fontSize = 11.sp)
            }
        }
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(FrixaColors.surface)
                .padding(6.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (r in 0 until SIZE) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (c in 0 until SIZE) {
                            val value = grid[r][c]
                            Box(
                                modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(8.dp)).background(tileColor(value)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (value != 0) {
                                    Text(
                                        "$value",
                                        color = if (value <= 4) FrixaColors.onSurface else FrixaColors.background,
                                        fontSize = if (value >= 1024) 16.sp else 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (isGameOver) {
                Box(modifier = Modifier.fillMaxSize().background(FrixaColors.background.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("המשחק נגמר", color = FrixaColors.secondary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("ניקוד: $score", color = FrixaColors.onSurface, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("לחץ OK כדי לשחק שוב", color = FrixaColors.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("החליקו אריחים בחיצים · מזג אריחים זהים", color = FrixaColors.onSurfaceVariant, fontSize = 12.sp)
    }
}
