package com.future.fitness.data

import com.future.sharednav.i18n.tr

/** ספריית תוכניות האימון המובנות באפליקציה - נתונים סטטיים, בלי שרת.
 * ערכי met (Metabolic Equivalent of Task) מבוססים על טווחים מקובלים
 * (Compendium of Physical Activities) לכל סוג פעילות - כוח מתון ~5, כוח
 * עצים ~6, ליבה/יציבה קלה ~3.5, אינטרוולים/HIIT ~7.5-8, יוגה/גמישות ~2.5. */
object WorkoutRepository {
    val workouts: List<Workout> = listOf(
        Workout(
            id = "upper_body",
            name = tr("פלג גוף עליון"),
            difficulty = tr("בינוני"),
            durationMin = 45,
            met = 5.0,
            exercises = listOf(
                Exercise(tr("לחיצת חזה"), 4, tr("10 חזרות")),
                Exercise(tr("חתירה בישיבה"), 4, tr("12 חזרות")),
                Exercise(tr("לחיצת כתפיים"), 3, tr("10 חזרות")),
                Exercise(tr("כפיפת מרפקים"), 3, tr("12 חזרות")),
                Exercise(tr("פשיטת מרפקים"), 3, tr("12 חזרות")),
                Exercise(tr("פלאנק"), 3, tr("45 שניות")),
            ),
        ),
        Workout(
            id = "legs",
            name = tr("רגליים"),
            difficulty = tr("קשה"),
            durationMin = 40,
            met = 6.0,
            exercises = listOf(
                Exercise(tr("סקוואט"), 4, tr("10 חזרות")),
                Exercise(tr("לאנג׳"), 3, tr("12 חזרות לרגל")),
                Exercise(tr("הרמת אגן"), 3, tr("15 חזרות")),
                Exercise(tr("עלייה על קופסה"), 3, tr("10 חזרות")),
                Exercise(tr("שוקיים בעמידה"), 4, tr("15 חזרות")),
            ),
        ),
        Workout(
            id = "core",
            name = tr("ליבה ויציבה"),
            difficulty = tr("קל"),
            durationMin = 20,
            met = 3.5,
            exercises = listOf(
                Exercise(tr("פלאנק"), 3, tr("45 שניות")),
                Exercise(tr("כפיפות בטן"), 3, tr("15 חזרות")),
                Exercise(tr("הרמת רגליים"), 3, tr("12 חזרות")),
                Exercise(tr("רוסי טוויסט"), 3, tr("20 חזרות")),
            ),
        ),
        Workout(
            id = "interval_run",
            name = tr("ריצת אינטרוולים"),
            difficulty = tr("בינוני"),
            durationMin = 30,
            met = 8.0,
            exercises = listOf(
                Exercise(tr("חימום קל"), 1, tr("5 דקות")),
                Exercise(tr("אינטרוול ריצה-הליכה"), 8, tr("דקה ריצה + דקה הליכה")),
                Exercise(tr("שחרור"), 1, tr("5 דקות")),
            ),
        ),
        Workout(
            id = "full_body",
            name = tr("גוף מלא"),
            difficulty = tr("בינוני"),
            durationMin = 40,
            met = 5.5,
            exercises = listOf(
                Exercise(tr("סקוואט"), 3, tr("12 חזרות")),
                Exercise(tr("לחיצת חזה"), 3, tr("10 חזרות")),
                Exercise(tr("חתירה בישיבה"), 3, tr("12 חזרות")),
                Exercise(tr("לחיצת כתפיים"), 3, tr("10 חזרות")),
                Exercise(tr("כפיפות בטן"), 3, tr("15 חזרות")),
            ),
        ),
        Workout(
            id = "push_pull",
            name = tr("דחיפה ומשיכה"),
            difficulty = tr("קשה"),
            durationMin = 45,
            met = 5.5,
            exercises = listOf(
                Exercise(tr("לחיצת חזה"), 4, tr("10 חזרות")),
                Exercise(tr("לחיצת כתפיים"), 3, tr("10 חזרות")),
                Exercise(tr("פשיטת מרפקים"), 3, tr("12 חזרות")),
                Exercise(tr("חתירה בישיבה"), 4, tr("10 חזרות")),
                Exercise(tr("משיכת פולי עליון"), 3, tr("8 חזרות")),
                Exercise(tr("כפיפת מרפקים"), 3, tr("12 חזרות")),
            ),
        ),
        Workout(
            id = "hiit_fast",
            name = tr("HIIT מהיר"),
            difficulty = tr("קשה"),
            durationMin = 25,
            met = 7.5,
            exercises = listOf(
                Exercise(tr("קפיצות פיסוק"), 4, tr("30 שניות")),
                Exercise(tr("ברפיז"), 4, tr("10 חזרות")),
                Exercise(tr("הרמות ברכיים"), 4, tr("30 שניות")),
                Exercise(tr("פלאנק עם מגע כתף"), 4, tr("20 שניות")),
            ),
        ),
        Workout(
            id = "yoga_mobility",
            name = tr("יוגה וגמישות"),
            difficulty = tr("קל"),
            durationMin = 25,
            met = 2.5,
            exercises = listOf(
                Exercise(tr("כלב מביט מטה"), 3, tr("30 שניות")),
                Exercise(tr("יציבת לוחם"), 3, tr("30 שניות לכל צד")),
                Exercise(tr("ישיבת פרפר"), 3, tr("40 שניות")),
                Exercise(tr("פיתול שכיבה"), 3, tr("30 שניות לכל צד")),
            ),
        ),
    )

    fun byId(id: String): Workout? = workouts.find { it.id == id }
}
