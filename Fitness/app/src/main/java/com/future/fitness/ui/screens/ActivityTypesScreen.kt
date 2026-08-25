package com.future.fitness.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.fitness.data.WorkoutActivityType
import com.future.fitness.data.WorkoutActivityTypes
import com.future.fitness.data.WorkoutStore
import com.future.fitness.ui.components.IconListRow
import com.future.fitness.ui.components.ScreenTopBar
import com.future.fitness.ui.theme.FutureTheme

/** קטלוג כל סוגי הפעילות הניתנים להתחלה - כמו מסך "אימון" בשעון חכם
 * (Apple Watch): מקובצים לפי קטגוריה, כל שורה מציגה הערכת קלוריות לשעה
 * (met * משקל ברירת מחדל, כי אין עדיין נתוני משך אמיתיים) ומתחילה מעקב חי -
 * GPS לסוגי חוץ (usesGps), טיימר גנרי + דופק לכל השאר. */
@Composable
fun ActivityTypesScreen(
    theme: FutureTheme,
    onBack: () -> Unit,
    onSelect: (WorkoutActivityType) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(title = "כל סוגי האימון", theme = theme, onBack = onBack)

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            WorkoutActivityTypes.byCategory.forEach { (category, types) ->
                item {
                    Text(
                        category.label,
                        color = theme.textColor.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
                    )
                }
                items(types) { type ->
                    val caloriesPerHour = WorkoutStore.estimateCalories(type.met, WorkoutStore.DEFAULT_WEIGHT_KG, 60)
                    IconListRow(
                        icon = type.icon,
                        title = type.displayName,
                        subtitle = "~$caloriesPerHour קלוריות לשעה",
                        theme = theme,
                        onClick = { onSelect(type) },
                        showChevron = true,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
            item { androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 8.dp)) }
        }
    }
}
