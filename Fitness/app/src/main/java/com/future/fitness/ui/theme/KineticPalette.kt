package com.future.fitness.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.future.sharednav.theme.FutureTheme

/** מיפוי השפה החזותית "Kinetic Obsidian" (מקובץ העיצוב שסופק ע"י המשתמש)
 * על גבי FutureTheme המשותף - בכוונה בלי פלטה קבועה משלה. ה-accentColor
 * (הצבע שהמשתמש בוחר בהגדרות המערכת) הוא ה"primary" של קינטיק אובסידיאן,
 * לא ירוק ננעל בקוד - כך שהעיצוב הזה עדיין מכבד את ההתאמה האישית ומצב
 * כהה/בהיר, בניגוד לפלטת נאון קבועה שמתעלמת מהתמה המשותפת.
 *
 * secondary/tertiary של קינטיק אובסידיאן (ציאן לביו-מטריקה, אדום-בורדו
 * לדופק/אזהרה) ממופים ל-successColor/dangerColor הקיימים כבר ב-FutureTheme
 * במקום לייבא צבעים חדשים - כל app שכבר צורך FutureTheme מקבל את שני אלה
 * "בחינם" מ-ThemeClient. */
val FutureTheme.kineticSecondary: Color
    get() = successColor

val FutureTheme.kineticTertiary: Color
    get() = dangerColor

/** רמות "משטח" (surface containers) - קינטיק אובסידיאן משתמש בכמה גוונים
 * של המשטח הבסיסי בעומק הולך וגדל. גוזרים אותם מ-surfaceColor הקיים ע"י
 * ערבוב קל עם טקסט/רקע, כדי שיישארו נאמנים למצב כהה/בהיר הנוכחי. */
val FutureTheme.surfaceContainerHigh: Color
    get() = lerp(surfaceColor, textColor, if (isDarkMode) 0.08f else 0.05f)

val FutureTheme.surfaceContainerHighest: Color
    get() = lerp(surfaceColor, textColor, if (isDarkMode) 0.14f else 0.09f)

val FutureTheme.surfaceContainerLowest: Color
    get() = lerp(surfaceColor, backgroundColor, 0.6f)

/** טוקני רדיוס/ריפוד עקביים לרכיבי הבנטו (כרטיסי מדד, טבעות פעילות,
 * כרטיסי אימון) - תואמים לסקאלה במסמך העיצוב (rounded-2xl/xl/full). */
object KineticDimens {
    val cardCorner: Dp = 24.dp
    val subCardCorner: Dp = 16.dp
    val chipCorner: Dp = 999.dp
}
