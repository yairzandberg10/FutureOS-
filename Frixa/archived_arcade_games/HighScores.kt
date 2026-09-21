package com.future.frixa.data

import android.content.Context

/** שיאים מקומיים לכל משחק - SharedPreferences פשוט, בלי צורך ב-JSON כי זה
 * רק שני מספרים שלמים. */
object HighScores {
    private const val PREFS_NAME = "frixa_scores"
    private const val KEY_SNAKE = "high_score_snake"
    private const val KEY_2048 = "high_score_2048"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSnakeHighScore(context: Context): Int = prefs(context).getInt(KEY_SNAKE, 0)

    fun submitSnakeScore(context: Context, score: Int) {
        if (score > getSnakeHighScore(context)) {
            prefs(context).edit().putInt(KEY_SNAKE, score).apply()
        }
    }

    fun get2048HighScore(context: Context): Int = prefs(context).getInt(KEY_2048, 0)

    fun submit2048Score(context: Context, score: Int) {
        if (score > get2048HighScore(context)) {
            prefs(context).edit().putInt(KEY_2048, score).apply()
        }
    }
}
