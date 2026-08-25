package com.future.fitness.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WorkoutHistoryEntry(
    val name: String,
    val minutes: Int,
    val calories: Int,
    val dateMillis: Long,
    val avgHr: Int? = null,
    val maxHr: Int? = null,
    val distanceKm: Double? = null,
)
data class DailyStats(val caloriesToday: Int, val activeMinutesToday: Int, val streakDays: Int)

/** פרופיל אישי (משקל/גיל) - משמש לחישוב קלוריות מדויק לפי met*משקל*שעות ולאזורי
 * דופק משוערים (220-גיל). null כשלא הוזן עדיין - מפעילים ברירת מחדל סבירה
 * (70 ק"ג) רק בחישוב עצמו, לא כאן, כדי שהמסך יידע להציג "לא הוגדר". */
data class UserProfile(val weightKg: Int?, val age: Int?)

/** מכשיר Bluetooth (שעון/רצועת דופק) שנשמר לחיבור אוטומטי בפעם הבאה. */
data class PairedDevice(val address: String, val name: String)

/** היסטוריית אימונים, סטטיסטיקת "היום"/רצף, אימונים מותאמים אישית, פרופיל
 * משתמש, ומכשיר Bluetooth משויך - הכל JSON ב-SharedPreferences, באותה שיטה
 * בדיוק כמו PlaylistStore ב-Music (אין DB מקומי בסוויטה לנתונים כאלה). */
class WorkoutStore(context: Context) {
    private val prefs = context.getSharedPreferences("fitness_store", Context.MODE_PRIVATE)
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun todayKey(): String = dayFormat.format(Date())

    fun getHistory(): List<WorkoutHistoryEntry> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        val arr = JSONArray(raw)
        val result = mutableListOf<WorkoutHistoryEntry>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            result.add(
                WorkoutHistoryEntry(
                    name = o.getString("name"),
                    minutes = o.getInt("minutes"),
                    calories = o.getInt("calories"),
                    dateMillis = o.getLong("date"),
                    avgHr = if (o.has("avgHr") && !o.isNull("avgHr")) o.getInt("avgHr") else null,
                    maxHr = if (o.has("maxHr") && !o.isNull("maxHr")) o.getInt("maxHr") else null,
                    distanceKm = if (o.has("distanceKm") && !o.isNull("distanceKm")) o.getDouble("distanceKm") else null,
                )
            )
        }
        return result
    }

    private fun saveHistory(entries: List<WorkoutHistoryEntry>) {
        val arr = JSONArray()
        entries.forEach { e ->
            val o = JSONObject()
            o.put("name", e.name)
            o.put("minutes", e.minutes)
            o.put("calories", e.calories)
            o.put("date", e.dateMillis)
            if (e.avgHr != null) o.put("avgHr", e.avgHr)
            if (e.maxHr != null) o.put("maxHr", e.maxHr)
            if (e.distanceKm != null) o.put("distanceKm", e.distanceKm)
            arr.put(o)
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    fun getStats(): DailyStats {
        rolloverIfNeeded()
        return DailyStats(
            caloriesToday = prefs.getInt(KEY_CALORIES_TODAY, 0),
            activeMinutesToday = prefs.getInt(KEY_MINUTES_TODAY, 0),
            streakDays = prefs.getInt(KEY_STREAK, 0),
        )
    }

    /** מאפס את סטטיסטיקת "היום" כשמתחיל יום חדש, ושובר את הרצף אם עבר יותר
     * מיום אחד בלי אימון שהושלם. */
    private fun rolloverIfNeeded() {
        val lastDay = prefs.getString(KEY_LAST_DAY, null)
        val today = todayKey()
        if (lastDay == today) return
        val editor = prefs.edit()
        editor.putInt(KEY_CALORIES_TODAY, 0)
        editor.putInt(KEY_MINUTES_TODAY, 0)
        if (lastDay != null && !isYesterday(lastDay, today)) {
            editor.putInt(KEY_STREAK, 0)
        }
        editor.putString(KEY_LAST_DAY, today)
        editor.apply()
    }

    private fun isYesterday(lastDay: String, today: String): Boolean {
        return try {
            val last = dayFormat.parse(lastDay) ?: return false
            val cur = dayFormat.parse(today) ?: return false
            val diffDays = (cur.time - last.time) / (24 * 60 * 60 * 1000)
            diffDays == 1L
        } catch (e: Exception) {
            false
        }
    }

    fun recordCompletedWorkout(
        name: String,
        minutes: Int,
        calories: Int,
        avgHr: Int? = null,
        maxHr: Int? = null,
        distanceKm: Double? = null,
    ): DailyStats {
        rolloverIfNeeded()
        saveHistory(listOf(WorkoutHistoryEntry(name, minutes, calories, System.currentTimeMillis(), avgHr, maxHr, distanceKm)) + getHistory())

        val today = todayKey()
        val lastWorkoutDay = prefs.getString(KEY_LAST_WORKOUT_DAY, null)
        var streak = prefs.getInt(KEY_STREAK, 0)
        if (lastWorkoutDay != today) {
            streak = if (lastWorkoutDay != null && isYesterday(lastWorkoutDay, today)) streak + 1 else 1
        }

        val calToday = prefs.getInt(KEY_CALORIES_TODAY, 0) + calories
        val minToday = prefs.getInt(KEY_MINUTES_TODAY, 0) + minutes

        prefs.edit()
            .putInt(KEY_CALORIES_TODAY, calToday)
            .putInt(KEY_MINUTES_TODAY, minToday)
            .putInt(KEY_STREAK, streak)
            .putString(KEY_LAST_DAY, today)
            .putString(KEY_LAST_WORKOUT_DAY, today)
            .apply()

        return DailyStats(calToday, minToday, streak)
    }

    fun getUnits(): String = prefs.getString(KEY_UNITS, "kg") ?: "kg"
    fun setUnits(units: String) {
        prefs.edit().putString(KEY_UNITS, units).apply()
    }

    // --- פרופיל אישי (למשקל מדויק לחישוב קלוריות, ולגיל לאזורי דופק) ---

    fun getProfile(): UserProfile {
        val w = prefs.getInt(KEY_WEIGHT, -1)
        val a = prefs.getInt(KEY_AGE, -1)
        return UserProfile(weightKg = if (w > 0) w else null, age = if (a > 0) a else null)
    }

    fun setProfile(weightKg: Int?, age: Int?) {
        val editor = prefs.edit()
        if (weightKg != null) editor.putInt(KEY_WEIGHT, weightKg) else editor.remove(KEY_WEIGHT)
        if (age != null) editor.putInt(KEY_AGE, age) else editor.remove(KEY_AGE)
        editor.apply()
    }

    // --- אימונים מותאמים אישית שהמשתמש בנה בעצמו ---

    fun getCustomWorkouts(): List<Workout> {
        val raw = prefs.getString(KEY_CUSTOM_WORKOUTS, null) ?: return emptyList()
        val arr = JSONArray(raw)
        val result = mutableListOf<Workout>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val exercisesJson = o.getJSONArray("exercises")
            val exercises = mutableListOf<Exercise>()
            for (j in 0 until exercisesJson.length()) {
                val eo = exercisesJson.getJSONObject(j)
                exercises.add(Exercise(eo.getString("name"), eo.getInt("sets"), eo.getString("repsLabel")))
            }
            result.add(
                Workout(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    difficulty = o.getString("difficulty"),
                    durationMin = o.getInt("durationMin"),
                    met = o.getDouble("met"),
                    exercises = exercises,
                    isCustom = true,
                )
            )
        }
        return result
    }

    fun addCustomWorkout(workout: Workout) {
        val all = getCustomWorkouts() + workout
        saveCustomWorkouts(all)
    }

    fun deleteCustomWorkout(id: String) {
        saveCustomWorkouts(getCustomWorkouts().filter { it.id != id })
    }

    private fun saveCustomWorkouts(workouts: List<Workout>) {
        val arr = JSONArray()
        workouts.forEach { w ->
            val o = JSONObject()
            o.put("id", w.id)
            o.put("name", w.name)
            o.put("difficulty", w.difficulty)
            o.put("durationMin", w.durationMin)
            o.put("met", w.met)
            val exercisesJson = JSONArray()
            w.exercises.forEach { e ->
                val eo = JSONObject()
                eo.put("name", e.name)
                eo.put("sets", e.sets)
                eo.put("repsLabel", e.repsLabel)
                exercisesJson.put(eo)
            }
            o.put("exercises", exercisesJson)
            arr.put(o)
        }
        prefs.edit().putString(KEY_CUSTOM_WORKOUTS, arr.toString()).apply()
    }

    // --- מכשיר Bluetooth משויך (שעון/רצועת דופק) - נשמר לחיבור אוטומטי ---

    fun getPairedDevice(): PairedDevice? {
        val address = prefs.getString(KEY_DEVICE_ADDRESS, null) ?: return null
        val name = prefs.getString(KEY_DEVICE_NAME, null) ?: address
        return PairedDevice(address, name)
    }

    fun setPairedDevice(device: PairedDevice?) {
        val editor = prefs.edit()
        if (device != null) {
            editor.putString(KEY_DEVICE_ADDRESS, device.address)
            editor.putString(KEY_DEVICE_NAME, device.name)
        } else {
            editor.remove(KEY_DEVICE_ADDRESS)
            editor.remove(KEY_DEVICE_NAME)
        }
        editor.apply()
    }

    companion object {
        private const val KEY_HISTORY = "history"
        private const val KEY_CALORIES_TODAY = "calories_today"
        private const val KEY_MINUTES_TODAY = "minutes_today"
        private const val KEY_STREAK = "streak"
        private const val KEY_LAST_DAY = "last_day"
        private const val KEY_LAST_WORKOUT_DAY = "last_workout_day"
        private const val KEY_UNITS = "units"
        private const val KEY_WEIGHT = "weight_kg"
        private const val KEY_AGE = "age"
        private const val KEY_CUSTOM_WORKOUTS = "custom_workouts"
        private const val KEY_DEVICE_ADDRESS = "device_address"
        private const val KEY_DEVICE_NAME = "device_name"

        /** ברירת מחדל סבירה כשהמשתמש עדיין לא הזין משקל בפרופיל - ממוצע גס,
         * רק כדי שהערכת הקלוריות לא תישבר; ברגע שיוגדר משקל אמיתי היא תוחלף. */
        const val DEFAULT_WEIGHT_KG = 70

        /** קק"ל = met * משקל(ק"ג) * שעות - נוסחת ההערכה הסטנדרטית למאמץ גופני. */
        fun estimateCalories(met: Double, weightKg: Int, minutes: Int): Int {
            val hours = minutes / 60.0
            return Math.round(met * weightKg * hours).toInt()
        }

        /** דופק מקסימלי משוער לפי הכלל הנפוץ 220-גיל (הערכה גסה, לא מדד רפואי
         * מדויק) - משמש רק לחישוב אזורי דופק אינפורמטיביים באפליקציה. */
        fun estimateMaxHr(age: Int): Int = 220 - age
    }
}
