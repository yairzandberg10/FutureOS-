package com.future.fitness.data

/** ספריית תוכניות האימון המובנות באפליקציה - נתונים סטטיים, בלי שרת.
 * ערכי met (Metabolic Equivalent of Task) מבוססים על טווחים מקובלים
 * (Compendium of Physical Activities) לכל סוג פעילות - כוח מתון ~5, כוח
 * עצים ~6, ליבה/יציבה קלה ~3.5, אינטרוולים/HIIT ~7.5-8, יוגה/גמישות ~2.5. */
object WorkoutRepository {
    val workouts: List<Workout> = listOf(
        Workout(
            id = "upper_body",
            name = "פלג גוף עליון",
            difficulty = "בינוני",
            durationMin = 45,
            met = 5.0,
            exercises = listOf(
                Exercise("לחיצת חזה", 4, "10 חזרות"),
                Exercise("חתירה בישיבה", 4, "12 חזרות"),
                Exercise("לחיצת כתפיים", 3, "10 חזרות"),
                Exercise("כפיפת מרפקים", 3, "12 חזרות"),
                Exercise("פשיטת מרפקים", 3, "12 חזרות"),
                Exercise("פלאנק", 3, "45 שניות"),
            ),
        ),
        Workout(
            id = "legs",
            name = "רגליים",
            difficulty = "קשה",
            durationMin = 40,
            met = 6.0,
            exercises = listOf(
                Exercise("סקוואט", 4, "10 חזרות"),
                Exercise("לאנג׳", 3, "12 חזרות לרגל"),
                Exercise("הרמת אגן", 3, "15 חזרות"),
                Exercise("עלייה על קופסה", 3, "10 חזרות"),
                Exercise("שוקיים בעמידה", 4, "15 חזרות"),
            ),
        ),
        Workout(
            id = "core",
            name = "ליבה ויציבה",
            difficulty = "קל",
            durationMin = 20,
            met = 3.5,
            exercises = listOf(
                Exercise("פלאנק", 3, "45 שניות"),
                Exercise("כפיפות בטן", 3, "15 חזרות"),
                Exercise("הרמת רגליים", 3, "12 חזרות"),
                Exercise("רוסי טוויסט", 3, "20 חזרות"),
            ),
        ),
        Workout(
            id = "interval_run",
            name = "ריצת אינטרוולים",
            difficulty = "בינוני",
            durationMin = 30,
            met = 8.0,
            exercises = listOf(
                Exercise("חימום קל", 1, "5 דקות"),
                Exercise("אינטרוול ריצה-הליכה", 8, "דקה ריצה + דקה הליכה"),
                Exercise("שחרור", 1, "5 דקות"),
            ),
        ),
        Workout(
            id = "full_body",
            name = "גוף מלא",
            difficulty = "בינוני",
            durationMin = 40,
            met = 5.5,
            exercises = listOf(
                Exercise("סקוואט", 3, "12 חזרות"),
                Exercise("לחיצת חזה", 3, "10 חזרות"),
                Exercise("חתירה בישיבה", 3, "12 חזרות"),
                Exercise("לחיצת כתפיים", 3, "10 חזרות"),
                Exercise("כפיפות בטן", 3, "15 חזרות"),
            ),
        ),
        Workout(
            id = "push_pull",
            name = "דחיפה ומשיכה",
            difficulty = "קשה",
            durationMin = 45,
            met = 5.5,
            exercises = listOf(
                Exercise("לחיצת חזה", 4, "10 חזרות"),
                Exercise("לחיצת כתפיים", 3, "10 חזרות"),
                Exercise("פשיטת מרפקים", 3, "12 חזרות"),
                Exercise("חתירה בישיבה", 4, "10 חזרות"),
                Exercise("משיכת פולי עליון", 3, "8 חזרות"),
                Exercise("כפיפת מרפקים", 3, "12 חזרות"),
            ),
        ),
        Workout(
            id = "hiit_fast",
            name = "HIIT מהיר",
            difficulty = "קשה",
            durationMin = 25,
            met = 7.5,
            exercises = listOf(
                Exercise("קפיצות פיסוק", 4, "30 שניות"),
                Exercise("ברפיז", 4, "10 חזרות"),
                Exercise("הרמות ברכיים", 4, "30 שניות"),
                Exercise("פלאנק עם מגע כתף", 4, "20 שניות"),
            ),
        ),
        Workout(
            id = "yoga_mobility",
            name = "יוגה וגמישות",
            difficulty = "קל",
            durationMin = 25,
            met = 2.5,
            exercises = listOf(
                Exercise("כלב מביט מטה", 3, "30 שניות"),
                Exercise("יציבת לוחם", 3, "30 שניות לכל צד"),
                Exercise("ישיבת פרפר", 3, "40 שניות"),
                Exercise("פיתול שכיבה", 3, "30 שניות לכל צד"),
            ),
        ),
    )

    fun byId(id: String): Workout? = workouts.find { it.id == id }
}
