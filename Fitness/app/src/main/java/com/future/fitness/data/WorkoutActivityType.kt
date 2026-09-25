package com.future.fitness.data

import com.future.sharednav.icons.FutureIcons

import androidx.compose.ui.graphics.vector.ImageVector

/** קטגוריות סוגי הפעילות, באותו סדר שבו הן מוצגות במסך "כל סוגי האימונים" -
 * תואם לקיבוץ שאפל שומרת בין ריצה/הליכה/רכיבה/שחייה לשאר סוגי הפעילות
 * במסך "אימון" בשעון החכם. */
enum class ActivityCategory(val label: String) {
    RUNNING("ריצה"),
    WALKING("הליכה"),
    CYCLING("רכיבה על אופניים"),
    CARDIO("קרדיו וחדר כושר"),
    STRENGTH("כוח"),
    MIND_BODY("גוף ונפש"),
    DANCE("ריקוד"),
    TEAM_SPORTS("ספורט קבוצתי וכדורים"),
    RACQUET_SPORTS("ספורט מחבטים"),
    INDIVIDUAL_SPORTS("קרב וספורט אישי"),
    WATER_SPORTS("ספורט ימי"),
    WINTER_SPORTS("ספורט חורף"),
    OUTDOOR("שטח וטבע"),
    OTHER("אחר"),
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
        WorkoutActivityType("outdoor_run", "ריצה בחוץ", ActivityCategory.RUNNING, 9.8, FutureIcons.AutoMirrored.DirectionsRun, usesGps = true),
        WorkoutActivityType("indoor_run", "ריצה במכונה", ActivityCategory.RUNNING, 9.0, FutureIcons.AutoMirrored.DirectionsRun),

        // הליכה
        WorkoutActivityType("outdoor_walk", "הליכה בחוץ", ActivityCategory.WALKING, 3.5, FutureIcons.DirectionsWalk, usesGps = true),
        WorkoutActivityType("indoor_walk", "הליכה במכונה", ActivityCategory.WALKING, 3.0, FutureIcons.DirectionsWalk),

        // רכיבה על אופניים
        WorkoutActivityType("outdoor_cycle", "רכיבה בחוץ", ActivityCategory.CYCLING, 7.5, FutureIcons.DirectionsBike, usesGps = true),
        WorkoutActivityType("indoor_cycle", "רכיבה בחדר כושר", ActivityCategory.CYCLING, 6.8, FutureIcons.DirectionsBike),
        WorkoutActivityType("hand_cycling", "רכיבת יד", ActivityCategory.CYCLING, 5.0, FutureIcons.DirectionsBike),

        // קרדיו וחדר כושר
        WorkoutActivityType("elliptical", "אליפטי", ActivityCategory.CARDIO, 5.0, FutureIcons.Whatshot),
        WorkoutActivityType("rower_indoor", "חתירה בחדר כושר", ActivityCategory.CARDIO, 7.0, FutureIcons.Whatshot),
        WorkoutActivityType("stair_stepper", "מדרגות (מכונה)", ActivityCategory.CARDIO, 8.0, FutureIcons.Whatshot),
        WorkoutActivityType("hiit", "HIIT - אימון אינטרוולים", ActivityCategory.CARDIO, 8.0, FutureIcons.Whatshot),
        WorkoutActivityType("mixed_cardio", "קרדיו משולב", ActivityCategory.CARDIO, 6.0, FutureIcons.Whatshot),
        WorkoutActivityType("cooldown", "שחרור / קירור", ActivityCategory.CARDIO, 2.0, FutureIcons.Whatshot),

        // כוח
        WorkoutActivityType("core_training", "אימון ליבה", ActivityCategory.STRENGTH, 3.5, FutureIcons.FitnessCenter),
        WorkoutActivityType("functional_strength", "כוח פונקציונלי", ActivityCategory.STRENGTH, 6.0, FutureIcons.FitnessCenter),
        WorkoutActivityType("traditional_strength", "כוח מסורתי (משקולות)", ActivityCategory.STRENGTH, 5.0, FutureIcons.FitnessCenter),

        // גוף ונפש
        WorkoutActivityType("yoga", "יוגה", ActivityCategory.MIND_BODY, 2.5, FutureIcons.Spa),
        WorkoutActivityType("pilates", "פילאטיס", ActivityCategory.MIND_BODY, 3.0, FutureIcons.Spa),
        WorkoutActivityType("barre", "בארה", ActivityCategory.MIND_BODY, 3.5, FutureIcons.Spa),
        WorkoutActivityType("flexibility", "גמישות ומתיחות", ActivityCategory.MIND_BODY, 2.5, FutureIcons.Spa),
        WorkoutActivityType("mind_and_body", "גוף ונפש כללי", ActivityCategory.MIND_BODY, 2.0, FutureIcons.Spa),

        // ריקוד
        WorkoutActivityType("dance", "ריקוד", ActivityCategory.DANCE, 4.8, FutureIcons.MusicNote),
        WorkoutActivityType("cardio_dance", "ריקוד קרדיו", ActivityCategory.DANCE, 6.5, FutureIcons.MusicNote),
        WorkoutActivityType("social_dance", "ריקוד חברתי", ActivityCategory.DANCE, 4.5, FutureIcons.MusicNote),

        // ספורט קבוצתי וכדורים
        WorkoutActivityType("basketball", "כדורסל", ActivityCategory.TEAM_SPORTS, 6.5, FutureIcons.EmojiEvents),
        WorkoutActivityType("soccer", "כדורגל", ActivityCategory.TEAM_SPORTS, 7.0, FutureIcons.EmojiEvents),
        WorkoutActivityType("american_football", "פוטבול אמריקאי", ActivityCategory.TEAM_SPORTS, 8.0, FutureIcons.EmojiEvents),
        WorkoutActivityType("baseball", "בייסבול", ActivityCategory.TEAM_SPORTS, 5.0, FutureIcons.EmojiEvents),
        WorkoutActivityType("softball", "סופטבול", ActivityCategory.TEAM_SPORTS, 5.0, FutureIcons.EmojiEvents),
        WorkoutActivityType("ice_hockey", "הוקי קרח", ActivityCategory.TEAM_SPORTS, 8.0, FutureIcons.EmojiEvents),
        WorkoutActivityType("rugby", "רוגבי", ActivityCategory.TEAM_SPORTS, 8.3, FutureIcons.EmojiEvents),
        WorkoutActivityType("cricket", "קריקט", ActivityCategory.TEAM_SPORTS, 5.0, FutureIcons.EmojiEvents),
        WorkoutActivityType("handball", "כדוריד", ActivityCategory.TEAM_SPORTS, 8.0, FutureIcons.EmojiEvents),
        WorkoutActivityType("volleyball", "כדורעף", ActivityCategory.TEAM_SPORTS, 4.0, FutureIcons.EmojiEvents),

        // ספורט מחבטים
        WorkoutActivityType("tennis", "טניס", ActivityCategory.RACQUET_SPORTS, 7.3, FutureIcons.EmojiEvents),
        WorkoutActivityType("table_tennis", "טניס שולחן", ActivityCategory.RACQUET_SPORTS, 4.0, FutureIcons.EmojiEvents),
        WorkoutActivityType("badminton", "בדמינטון", ActivityCategory.RACQUET_SPORTS, 5.5, FutureIcons.EmojiEvents),
        WorkoutActivityType("racquetball", "רקטבול", ActivityCategory.RACQUET_SPORTS, 7.0, FutureIcons.EmojiEvents),
        WorkoutActivityType("squash", "סקווש", ActivityCategory.RACQUET_SPORTS, 7.3, FutureIcons.EmojiEvents),
        WorkoutActivityType("pickleball", "פיקלבול", ActivityCategory.RACQUET_SPORTS, 5.0, FutureIcons.EmojiEvents),

        // קרב וספורט אישי
        WorkoutActivityType("golf", "גולף", ActivityCategory.INDIVIDUAL_SPORTS, 4.3, FutureIcons.EmojiEvents),
        WorkoutActivityType("bowling", "באולינג", ActivityCategory.INDIVIDUAL_SPORTS, 3.0, FutureIcons.EmojiEvents),
        WorkoutActivityType("boxing", "אגרוף", ActivityCategory.INDIVIDUAL_SPORTS, 7.8, FutureIcons.EmojiEvents),
        WorkoutActivityType("kickboxing", "קיקבוקס", ActivityCategory.INDIVIDUAL_SPORTS, 7.0, FutureIcons.EmojiEvents),
        WorkoutActivityType("martial_arts", "אומנויות לחימה", ActivityCategory.INDIVIDUAL_SPORTS, 10.3, FutureIcons.EmojiEvents),
        WorkoutActivityType("wrestling", "היאבקות", ActivityCategory.INDIVIDUAL_SPORTS, 6.0, FutureIcons.EmojiEvents),
        WorkoutActivityType("fencing", "סיף", ActivityCategory.INDIVIDUAL_SPORTS, 6.0, FutureIcons.EmojiEvents),
        WorkoutActivityType("track_and_field", "אתלטיקה קלה", ActivityCategory.INDIVIDUAL_SPORTS, 8.0, FutureIcons.EmojiEvents, usesGps = true),
        WorkoutActivityType("climbing", "טיפוס", ActivityCategory.INDIVIDUAL_SPORTS, 8.0, FutureIcons.Terrain),
        WorkoutActivityType("gymnastics", "התעמלות", ActivityCategory.INDIVIDUAL_SPORTS, 4.0, FutureIcons.EmojiEvents),

        // ספורט ימי
        WorkoutActivityType("water_fitness", "כושר מים", ActivityCategory.WATER_SPORTS, 4.5, FutureIcons.Pool),
        WorkoutActivityType("water_polo", "כדורמים", ActivityCategory.WATER_SPORTS, 10.0, FutureIcons.Pool),
        WorkoutActivityType("water_sports", "ספורט מים כללי", ActivityCategory.WATER_SPORTS, 5.0, FutureIcons.DirectionsBoat),
        WorkoutActivityType("paddle_sports", "חתירה / פאדל", ActivityCategory.WATER_SPORTS, 5.0, FutureIcons.DirectionsBoat),
        WorkoutActivityType("surfing", "גלישה", ActivityCategory.WATER_SPORTS, 3.0, FutureIcons.DirectionsBoat),
        WorkoutActivityType("sailing", "שייט", ActivityCategory.WATER_SPORTS, 3.0, FutureIcons.DirectionsBoat),
        WorkoutActivityType("rowing_outdoor", "חתירה בחוץ", ActivityCategory.WATER_SPORTS, 7.0, FutureIcons.DirectionsBoat),

        // ספורט חורף
        WorkoutActivityType("downhill_skiing", "סקי מדרון", ActivityCategory.WINTER_SPORTS, 6.0, FutureIcons.AcUnit),
        WorkoutActivityType("cross_country_skiing", "סקי רוחב", ActivityCategory.WINTER_SPORTS, 9.0, FutureIcons.AcUnit),
        WorkoutActivityType("snowboarding", "סנובורד", ActivityCategory.WINTER_SPORTS, 5.3, FutureIcons.AcUnit),
        WorkoutActivityType("ice_skating", "החלקה על קרח", ActivityCategory.WINTER_SPORTS, 7.0, FutureIcons.AcUnit),
        WorkoutActivityType("snow_sports", "ספורט שלג כללי", ActivityCategory.WINTER_SPORTS, 6.0, FutureIcons.AcUnit),

        // שטח וטבע
        WorkoutActivityType("hiking", "טיולי שטח", ActivityCategory.OUTDOOR, 6.0, FutureIcons.Terrain, usesGps = true),
        WorkoutActivityType("equestrian", "רכיבה על סוסים", ActivityCategory.OUTDOOR, 3.5, FutureIcons.Terrain),
        WorkoutActivityType("hunting", "ציד", ActivityCategory.OUTDOOR, 3.5, FutureIcons.Terrain),
        WorkoutActivityType("fishing", "דיג", ActivityCategory.OUTDOOR, 3.5, FutureIcons.Terrain),
        WorkoutActivityType("disc_sports", "פריזבי", ActivityCategory.OUTDOOR, 4.0, FutureIcons.Terrain),
        WorkoutActivityType("play", "משחק חופשי", ActivityCategory.OUTDOOR, 4.0, FutureIcons.Terrain),

        // אחר
        WorkoutActivityType("wheelchair_walk_pace", "כיסא גלגלים - קצב הליכה", ActivityCategory.OTHER, 3.0, FutureIcons.Accessibility),
        WorkoutActivityType("wheelchair_run_pace", "כיסא גלגלים - קצב ריצה", ActivityCategory.OTHER, 8.0, FutureIcons.Accessibility),
        WorkoutActivityType("underwater_diving", "צלילה", ActivityCategory.OTHER, 7.0, FutureIcons.Pool),
        WorkoutActivityType("fitness_gaming", "גיימינג כושר", ActivityCategory.OTHER, 4.0, FutureIcons.VideogameAsset),
        WorkoutActivityType("other", "אחר", ActivityCategory.OTHER, 4.0, FutureIcons.FitnessCenter),
    )

    fun byId(id: String): WorkoutActivityType? = all.find { it.id == id }

    val byCategory: Map<ActivityCategory, List<WorkoutActivityType>> =
        all.groupBy { it.category }
}
