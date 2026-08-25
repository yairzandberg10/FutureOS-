package com.future.fitness.data

data class Exercise(val name: String, val sets: Int, val repsLabel: String)

/** met - Metabolic Equivalent of Task של האימון (למשל 5.0 לאימון כוח בינוני,
 * 9.0 לריצה) - משמש לחישוב קלוריות מדויק יותר: קק"ל = met * משקל(ק"ג) * שעות,
 * במקום נוסחה גסה שלא מתחשבת במשקל המשתמש/עצימות האימון. */
data class Workout(
    val id: String,
    val name: String,
    val difficulty: String,
    val durationMin: Int,
    val met: Double,
    val exercises: List<Exercise>,
    val isCustom: Boolean = false,
)
