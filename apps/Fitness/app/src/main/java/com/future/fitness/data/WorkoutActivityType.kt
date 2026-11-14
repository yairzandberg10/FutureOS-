package com.future.fitness.data

import com.future.sharednav.i18n.tr

import com.future.sharednav.icons.FutureIcons

import androidx.compose.ui.graphics.vector.ImageVector

/** קטגוריות סוגי הפעילות, באותו סדר שבו הן מוצגות במסך "כל סוגי האימונים" -
 * תואם לקיבוץ שאפל שומרת בין ריצה/הליכה/רכיבה/שחייה לשאר סוגי הפעילות
 * במסך "אימון" בשעון החכם. */
enum class ActivityCategory(val label: String) {
    RUNNING(tr("ריצה")),
    WALKING(tr("הליכה")),
    CYCLING(tr("רכיבה על אופניים")),
    CARDIO(tr("קרדיו וחדר כושר")),
    STRENGTH(tr("כוח")),
    MIND_BODY(tr("גוף ונפש")),
    DANCE(tr("ריקוד")),
    TEAM_SPORTS(tr("ספורט קבוצתי וכדורים")),
    RACQUET_SPORTS(tr("ספורט מחבטים")),
    INDIVIDUAL_SPORTS(tr("קרב וספורט אישי")),
    WATER_SPORTS(tr("ספורט ימי")),
    WINTER_SPORTS(tr("ספורט חורף")),
    OUTDOOR(tr("שטח וטבע")),
    OTHER(tr("אחר")),
}

/** met - Metabolic Equivalent of Task, ראו Workout.kt - כאן משמש להערכת קלוריות
 * לכל סוג פעילות במסך ה-Quick Start הגנרי. usesGps מסמן סוגי פעילות של תנועה
 * בחוץ (ריצה/הליכה/רכיבה/טיולי שטח בחוץ) שמנותבים למסך המעקב GPS הקיים
 * (RunScreen המוכלל) במקום למסך ה-Quick Start הגנרי בלי GPS. */
data class WorkoutActivityType(
    val id: String,
    val displayName: String,
    val category: ActivityCategory,
    val met: Double,
    val icon: ImageVector,
    val usesGps: Boolean = false,
)

/**
 * תבנית מסך האימון החי - איזה מדדים ומסכי-משנה מוצגים בזמן אימון (ניתן לדפדף
 * ביניהם עם חצי שמאל/ימין), במקביל לארבע התבניות של אפליקציית "אימון" ב-
 * Apple Watch: https://support.apple.com/guide/watch/workout-views-and-running-metrics-apd1f24d4d35/watchos
 * אין כאן תבנית שחייה - מכשיר טלפון רגיל (בלי אטימות מים, בלי מסך מגע שאפשר
 * לנעול נגד מים) לא מתאים למעקב אימון תוך כדי שחייה בפועל.
 */
enum class WorkoutTemplate {
    RUN_WALK,
    CYCLING,
    GENERAL,
}

/** ריצה/הליכה/טיולי שטח/אתלטיקה מקבלים את תבנית הריצה (דינמיקת ריצה, גובה) -
 * בדיוק כמו קיבוץ "ריצה והליכה" באפל ווטש. שאר הפעילויות מקבלות את התבנית
 * הגנרית, פרט לרכיבה על אופניים שמקבלת תבנית ייעודית משלה. */
fun WorkoutActivityType.template(): WorkoutTemplate = when {
    category == ActivityCategory.CYCLING -> WorkoutTemplate.CYCLING
    category == ActivityCategory.RUNNING || category == ActivityCategory.WALKING -> WorkoutTemplate.RUN_WALK
    id == "hiking" || id == "track_and_field" -> WorkoutTemplate.RUN_WALK
    else -> WorkoutTemplate.GENERAL
}

/** קטלוג סוגי האימון המלא - כל סוגי הפעילות שאפשר להתחיל דרך אפליקציית
 * "אימון" בשעון חכם (Apple Watch), מתורגם לעברית. כל סוג שנבחר במסך
 * ActivityTypesScreen מתחיל מעקב חי - GPS לסוגי חוץ (usesGps), או טיימר
 * גנרי + קלוריות + דופק חי לכל השאר (ראו QuickStartScreen). */
object WorkoutActivityTypes {
    val all: List<WorkoutActivityType> = listOf(
        // ריצה
        WorkoutActivityType("outdoor_run", tr("ריצה בחוץ"), ActivityCategory.RUNNING, 9.8, FutureIcons.AutoMirrored.DirectionsRun, usesGps = true),
        WorkoutActivityType("indoor_run", tr("ריצה במכונה"), ActivityCategory.RUNNING, 9.0, FutureIcons.AutoMirrored.DirectionsRun),

        // הליכה
        WorkoutActivityType("outdoor_walk", tr("הליכה בחוץ"), ActivityCategory.WALKING, 3.5, FutureIcons.DirectionsWalk, usesGps = true),
        WorkoutActivityType("indoor_walk", tr("הליכה במכונה"), ActivityCategory.WALKING, 3.0, FutureIcons.DirectionsWalk),

        // רכיבה על אופניים
        WorkoutActivityType("outdoor_cycle", tr("רכיבה בחוץ"), ActivityCategory.CYCLING, 7.5, FutureIcons.DirectionsBike, usesGps = true),
        WorkoutActivityType("indoor_cycle", tr("רכיבה בחדר כושר"), ActivityCategory.CYCLING, 6.8, FutureIcons.DirectionsBike),
        WorkoutActivityType("hand_cycling", tr("רכיבת יד"), ActivityCategory.CYCLING, 5.0, FutureIcons.DirectionsBike),

        // קרדיו וחדר כושר
        WorkoutActivityType("elliptical", tr("אליפטי"), ActivityCategory.CARDIO, 5.0, FutureIcons.Whatshot),
        WorkoutActivityType("rower_indoor", tr("חתירה בחדר כושר"), ActivityCategory.CARDIO, 7.0, FutureIcons.Whatshot),
        WorkoutActivityType("stair_stepper", tr("מדרגות (מכונה)"), ActivityCategory.CARDIO, 8.0, FutureIcons.Whatshot),
        WorkoutActivityType("hiit", tr("HIIT - אימון אינטרוולים"), ActivityCategory.CARDIO, 8.0, FutureIcons.Whatshot),
        WorkoutActivityType("mixed_cardio", tr("קרדיו משולב"), ActivityCategory.CARDIO, 6.0, FutureIcons.Whatshot),
        WorkoutActivityType("cooldown", tr("שחרור / קירור"), ActivityCategory.CARDIO, 2.0, FutureIcons.Whatshot),

        // כוח
        WorkoutActivityType("core_training", tr("אימון ליבה"), ActivityCategory.STRENGTH, 3.5, FutureIcons.FitnessCenter),
        WorkoutActivityType("functional_strength", tr("כוח פונקציונלי"), ActivityCategory.STRENGTH, 6.0, FutureIcons.FitnessCenter),
        WorkoutActivityType("traditional_strength", tr("כוח מסורתי (משקולות)"), ActivityCategory.STRENGTH, 5.0, FutureIcons.FitnessCenter),

        // גוף ונפש
        WorkoutActivityType("yoga", tr("יוגה"), ActivityCategory.MIND_BODY, 2.5, FutureIcons.Spa),
        WorkoutActivityType("pilates", tr("פילאטיס"), ActivityCategory.MIND_BODY, 3.0, FutureIcons.Spa),
        WorkoutActivityType("barre", tr("בארה"), ActivityCategory.MIND_BODY, 3.5, FutureIcons.Spa),
        WorkoutActivityType("flexibility", tr("גמישות ומתיחות"), ActivityCategory.MIND_BODY, 2.5, FutureIcons.Spa),
        WorkoutActivityType("mind_and_body", tr("גוף ונפש כללי"), ActivityCategory.MIND_BODY, 2.0, FutureIcons.Spa),

        // ריקוד
        WorkoutActivityType("dance", tr("ריקוד"), ActivityCategory.DANCE, 4.8, FutureIcons.MusicNote),
        WorkoutActivityType("cardio_dance", tr("ריקוד קרדיו"), ActivityCategory.DANCE, 6.5, FutureIcons.MusicNote),
        WorkoutActivityType("social_dance", tr("ריקוד חברתי"), ActivityCategory.DANCE, 4.5, FutureIcons.MusicNote),

        // ספורט קבוצתי וכדורים
        WorkoutActivityType("basketball", tr("כדורסל"), ActivityCategory.TEAM_SPORTS, 6.5, FutureIcons.EmojiEvents),
        WorkoutActivityType("soccer", tr("כדורגל"), ActivityCategory.TEAM_SPORTS, 7.0, FutureIcons.EmojiEvents),
        WorkoutActivityType("american_football", tr("פוטבול אמריקאי"), ActivityCategory.TEAM_SPORTS, 8.0, FutureIcons.EmojiEvents),
        WorkoutActivityType("baseball", tr("בייסבול"), ActivityCategory.TEAM_SPORTS, 5.0, FutureIcons.EmojiEvents),
        WorkoutActivityType("softball", tr("סופטבול"), ActivityCategory.TEAM_SPORTS, 5.0, FutureIcons.EmojiEvents),
        WorkoutActivityType("ice_hockey", tr("הוקי קרח"), ActivityCategory.TEAM_SPORTS, 8.0, FutureIcons.EmojiEvents),
        WorkoutActivityType("rugby", tr("רוגבי"), ActivityCategory.TEAM_SPORTS, 8.3, FutureIcons.EmojiEvents),
        WorkoutActivityType("cricket", tr("קריקט"), ActivityCategory.TEAM_SPORTS, 5.0, FutureIcons.EmojiEvents),
        WorkoutActivityType("handball", tr("כדוריד"), ActivityCategory.TEAM_SPORTS, 8.0, FutureIcons.EmojiEvents),
        WorkoutActivityType("volleyball", tr("כדורעף"), ActivityCategory.TEAM_SPORTS, 4.0, FutureIcons.EmojiEvents),

        // ספורט מחבטים
        WorkoutActivityType("tennis", tr("טניס"), ActivityCategory.RACQUET_SPORTS, 7.3, FutureIcons.EmojiEvents),
        WorkoutActivityType("table_tennis", tr("טניס שולחן"), ActivityCategory.RACQUET_SPORTS, 4.0, FutureIcons.EmojiEvents),
        WorkoutActivityType("badminton", tr("בדמינטון"), ActivityCategory.RACQUET_SPORTS, 5.5, FutureIcons.EmojiEvents),
        WorkoutActivityType("racquetball", tr("רקטבול"), ActivityCategory.RACQUET_SPORTS, 7.0, FutureIcons.EmojiEvents),
        WorkoutActivityType("squash", tr("סקווש"), ActivityCategory.RACQUET_SPORTS, 7.3, FutureIcons.EmojiEvents),
        WorkoutActivityType("pickleball", tr("פיקלבול"), ActivityCategory.RACQUET_SPORTS, 5.0, FutureIcons.EmojiEvents),

        // קרב וספורט אישי
        WorkoutActivityType("golf", tr("גולף"), ActivityCategory.INDIVIDUAL_SPORTS, 4.3, FutureIcons.EmojiEvents),
        WorkoutActivityType("bowling", tr("באולינג"), ActivityCategory.INDIVIDUAL_SPORTS, 3.0, FutureIcons.EmojiEvents),
        WorkoutActivityType("boxing", tr("אגרוף"), ActivityCategory.INDIVIDUAL_SPORTS, 7.8, FutureIcons.EmojiEvents),
        WorkoutActivityType("kickboxing", tr("קיקבוקס"), ActivityCategory.INDIVIDUAL_SPORTS, 7.0, FutureIcons.EmojiEvents),
        WorkoutActivityType("martial_arts", tr("אומנויות לחימה"), ActivityCategory.INDIVIDUAL_SPORTS, 10.3, FutureIcons.EmojiEvents),
        WorkoutActivityType("wrestling", tr("היאבקות"), ActivityCategory.INDIVIDUAL_SPORTS, 6.0, FutureIcons.EmojiEvents),
        WorkoutActivityType("fencing", tr("סיף"), ActivityCategory.INDIVIDUAL_SPORTS, 6.0, FutureIcons.EmojiEvents),
        WorkoutActivityType("track_and_field", tr("אתלטיקה קלה"), ActivityCategory.INDIVIDUAL_SPORTS, 8.0, FutureIcons.EmojiEvents, usesGps = true),
        WorkoutActivityType("climbing", tr("טיפוס"), ActivityCategory.INDIVIDUAL_SPORTS, 8.0, FutureIcons.Terrain),
        WorkoutActivityType("gymnastics", tr("התעמלות"), ActivityCategory.INDIVIDUAL_SPORTS, 4.0, FutureIcons.EmojiEvents),

        // ספורט ימי
        WorkoutActivityType("water_fitness", tr("כושר מים"), ActivityCategory.WATER_SPORTS, 4.5, FutureIcons.Pool),
        WorkoutActivityType("water_polo", tr("כדורמים"), ActivityCategory.WATER_SPORTS, 10.0, FutureIcons.Pool),
        WorkoutActivityType("water_sports", tr("ספורט מים כללי"), ActivityCategory.WATER_SPORTS, 5.0, FutureIcons.DirectionsBoat),
        WorkoutActivityType("paddle_sports", tr("חתירה / פאדל"), ActivityCategory.WATER_SPORTS, 5.0, FutureIcons.DirectionsBoat),
        WorkoutActivityType("surfing", tr("גלישה"), ActivityCategory.WATER_SPORTS, 3.0, FutureIcons.DirectionsBoat),
        WorkoutActivityType("sailing", tr("שייט"), ActivityCategory.WATER_SPORTS, 3.0, FutureIcons.DirectionsBoat),
        WorkoutActivityType("rowing_outdoor", tr("חתירה בחוץ"), ActivityCategory.WATER_SPORTS, 7.0, FutureIcons.DirectionsBoat),

        // ספורט חורף
        WorkoutActivityType("downhill_skiing", tr("סקי מדרון"), ActivityCategory.WINTER_SPORTS, 6.0, FutureIcons.AcUnit),
        WorkoutActivityType("cross_country_skiing", tr("סקי רוחב"), ActivityCategory.WINTER_SPORTS, 9.0, FutureIcons.AcUnit),
        WorkoutActivityType("snowboarding", tr("סנובורד"), ActivityCategory.WINTER_SPORTS, 5.3, FutureIcons.AcUnit),
        WorkoutActivityType("ice_skating", tr("החלקה על קרח"), ActivityCategory.WINTER_SPORTS, 7.0, FutureIcons.AcUnit),
        WorkoutActivityType("snow_sports", tr("ספורט שלג כללי"), ActivityCategory.WINTER_SPORTS, 6.0, FutureIcons.AcUnit),

        // שטח וטבע
        WorkoutActivityType("hiking", tr("טיולי שטח"), ActivityCategory.OUTDOOR, 6.0, FutureIcons.Terrain, usesGps = true),
        WorkoutActivityType("equestrian", tr("רכיבה על סוסים"), ActivityCategory.OUTDOOR, 3.5, FutureIcons.Terrain),
        WorkoutActivityType("hunting", tr("ציד"), ActivityCategory.OUTDOOR, 3.5, FutureIcons.Terrain),
        WorkoutActivityType("fishing", tr("דיג"), ActivityCategory.OUTDOOR, 3.5, FutureIcons.Terrain),
        WorkoutActivityType("disc_sports", tr("פריזבי"), ActivityCategory.OUTDOOR, 4.0, FutureIcons.Terrain),
        WorkoutActivityType("play", tr("משחק חופשי"), ActivityCategory.OUTDOOR, 4.0, FutureIcons.Terrain),

        // אחר
        WorkoutActivityType("wheelchair_walk_pace", tr("כיסא גלגלים - קצב הליכה"), ActivityCategory.OTHER, 3.0, FutureIcons.Accessibility),
        WorkoutActivityType("wheelchair_run_pace", tr("כיסא גלגלים - קצב ריצה"), ActivityCategory.OTHER, 8.0, FutureIcons.Accessibility),
        WorkoutActivityType("underwater_diving", tr("צלילה"), ActivityCategory.OTHER, 7.0, FutureIcons.Pool),
        WorkoutActivityType("fitness_gaming", tr("גיימינג כושר"), ActivityCategory.OTHER, 4.0, FutureIcons.VideogameAsset),
        WorkoutActivityType("other", tr("אחר"), ActivityCategory.OTHER, 4.0, FutureIcons.FitnessCenter),
    )

    fun byId(id: String): WorkoutActivityType? = all.find { it.id == id }

    val byCategory: Map<ActivityCategory, List<WorkoutActivityType>> =
        all.groupBy { it.category }
}
