package com.future.fitness.ui

/** מצטבר כמחסנית אמיתית (כמו ב-Music) - כל push מוסיף מסך, וה-back הפיזי/
 * כפתור החזור פשוט מסיר את האחרון. זה נותן "חזור" נכון מכל נקודת כניסה
 * בלי לוגיקה נפרדת: WorkoutDetail שנפתח מכרטיס "האימון הבא" בבית יחזור
 * לבית, ואותו מסך שנפתח מרשימת האימונים יחזור לרשימה. */
sealed class Route {
    data object Home : Route()
    data object Workouts : Route()
    // מזוהה לפי Workout.id (לא אינדקס ברשימה) - ראו FitnessNavHost: אינדקס היה
    // שביר כי workouts = WorkoutRepository.workouts + store.getCustomWorkouts()
    // מחושב מחדש בכל שינוי (מחיקת אימון מותאם אישית), אז אינדקס ששמור על
    // מחסנית ה-back עלול להצביע פתאום על אימון אחר.
    data class WorkoutDetail(val workoutId: String) : Route()
    data class ActiveWorkout(val workoutId: String) : Route()
    data class Summary(
        val workoutName: String,
        val minutes: Int,
        val calories: Int,
        val totalSets: Int,
        val avgHr: Int? = null,
        val maxHr: Int? = null,
    ) : Route()
    data object Progress : Route()
    data object Settings : Route()
    data object History : Route()
    data object WorkoutBuilder : Route()
    data object HealthTips : Route()
    data object Run : Route()
    data class RunSummary(val minutes: Int, val distanceKm: Double, val calories: Int) : Route()

    // קטלוג כל סוגי האימון (ראו WorkoutActivityTypes) - "כל סוגי האימונים
    // שיש באפל וואטש". usesGps מנתב ל-GpsActivity (מסך RunScreen המוכלל),
    // אחרת ל-QuickStart (טיימר גנרי + דופק, בלי GPS).
    data object ActivityTypes : Route()
    data class GpsActivity(val activityTypeId: String) : Route()
    data class QuickStart(val activityTypeId: String) : Route()
    data class QuickStartSummary(
        val activityName: String,
        val minutes: Int,
        val calories: Int,
        val avgHr: Int? = null,
    ) : Route()
}
