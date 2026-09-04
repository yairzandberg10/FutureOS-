package com.future.fitness.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.fitness.data.UserProfile
import com.future.fitness.data.WorkoutStore
import com.future.fitness.ui.components.FocusableItem
import com.future.fitness.ui.components.ScreenTopBar
import com.future.sharednav.theme.FutureTheme

private data class HealthTip(val title: String, val body: String)

/** מידע בריאותי כללי, מבוסס על הנחיות פעילות גופנית מקובלות (ארגון הבריאות
 * העולמי / CDC) - לא ייעוץ רפואי אישי. כל כרטיס מנוסח בזהירות עם הסתייגות
 * במקומות שבהם ההערכה גסה (למשל דופק מקסימלי משוער). */
private val tips = listOf(
    HealthTip(
        "כמה פעילות מומלצת בשבוע?",
        "לפי הנחיות ארגון הבריאות העולמי למבוגרים: לפחות 150 דקות בשבוע של פעילות אירובית בעצימות בינונית (או 75 דקות בעצימות גבוהה), ועוד לפחות יומיים בשבוע של אימוני חיזוק לקבוצות שרירים עיקריות.",
    ),
    HealthTip(
        "מה זה אזורי דופק?",
        "דופק מקסימלי משוער (220 פחות גיל) הוא כלל אצבע גס, לא מדד רפואי מדויק. אזורים נפוצים: קל 50-60% מהמקסימום, בינוני 60-70%, אירובי 70-80%, סף 80-90%, מאמץ מרבי 90-100%. האפליקציה מציגה הערכה בהתאם לגיל שהזנת בהגדרות.",
    ),
    HealthTip(
        "מנוחה והתאוששות",
        "מקובל לתת לאותה קבוצת שרירים כ-48 שעות מנוחה בין אימוני כוח, כדי לאפשר התאוששות ובניית שריר. שינה מספקת והידרציה משפיעות ישירות על ביצועים והתאוששות.",
    ),
    HealthTip(
        "עומס הדרגתי (Progressive Overload)",
        "כדי להתקדם לאורך זמן, מומלץ להעלות בהדרגה משקל, חזרות או עצימות - לא הכל בבת אחת. קפיצות גדולות מדי מעלות סיכון לפציעה.",
    ),
    HealthTip(
        "חימום ושחרור",
        "כמה דקות חימום קל לפני אימון וכמה דקות שחרור/מתיחות אחריו מקובלים כהרגל טוב להפחתת סיכון לפציעה ולשיפור טווחי תנועה - לדוגמה, אימון הריצה באפליקציה כולל חימום ושחרור מובנים.",
    ),
    HealthTip(
        "הערכת הקלוריות באפליקציה",
        "מספר הקלוריות המוצג מחושב מנוסחת ההערכה הסטנדרטית met × משקל(ק״ג) × שעות, לפי סוג האימון והמשקל שהזנת בהגדרות. זו הערכה כללית ולא מדידה מדויקת - הערך הופך למדויק יותר ככל שהפרופיל שלך (משקל) מעודכן.",
    ),
)

private const val DISCLAIMER = "המידע כאן כללי-חינוכי בלבד ואינו ייעוץ רפואי אישי. לפני התחלת תוכנית אימונים חדשה, במיוחד עם מצב רפואי קיים, מומלץ להתייעץ עם רופא/ה."

@Composable
fun HealthTipsScreen(theme: FutureTheme, store: WorkoutStore, onBack: () -> Unit) {
    val profile: UserProfile = store.getProfile()

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(title = "בריאות", theme = theme, onBack = onBack)

        // כל כרטיס עטוף ב-FocusableItem (onClick={} - מידעי בלבד, לא פעולה) כדי
        // שיהיה focusable ואפשר יהיה לגלול אליו עם D-pad; בלי זה תוכן שגולש
        // מחוץ למסך היה בלתי-נגיש במכשיר בלי מסך מגע.
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            if (profile.age != null) {
                item {
                    val maxHr = WorkoutStore.estimateMaxHr(profile.age)
                    FocusableItem(onClick = {}, theme = theme, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(theme.accentColor.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                        ) {
                            Text("דופק מקסימלי משוער עבורך", color = theme.accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("$maxHr פעימות לדקה (לפי גיל ${profile.age}, הערכה גסה)", color = theme.textColor, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }

            items(tips) { tip ->
                FocusableItem(onClick = {}, theme = theme, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(theme.surfaceColor, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                    ) {
                        Text(tip.title, color = theme.textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(tip.body, color = theme.textColor.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }

            item {
                Text(
                    DISCLAIMER,
                    color = theme.textColor.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                )
            }
        }
    }
}
