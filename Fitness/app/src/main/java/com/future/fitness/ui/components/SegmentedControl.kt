package com.future.fitness.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.sharednav.theme.FutureTheme

/** בורר פלחים (רמת קושי, יחידות משקל וכו') - היה מוכפל בנפרד (עם עיצוב זהה)
 * ב-WorkoutBuilderScreen וב-SettingsScreen, ובלי שום סימון פוקוס D-pad
 * (Box.clickable רגיל) - כאן עטוף ב-FocusableItem כדי שיהיה ברור אילו פלח
 * ממוקד עכשיו, קריטי במכשיר בלי מסך מגע. segmentWidth null = כל הפלחים
 * מתחלקים באותו רוחב (weight); אחרת רוחב קבוע לכל פלח. */
@Composable
fun SegmentedControl(
    options: List<String>,
    selected: String,
    theme: FutureTheme,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    segmentWidth: Dp? = null,
) {
    Row(
        modifier = modifier
            .background(theme.textColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(3.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            FocusableItem(
                onClick = { onSelect(option) },
                theme = theme,
                modifier = if (segmentWidth != null) Modifier.width(segmentWidth) else Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSelected) theme.surfaceColor else Color.Transparent, RoundedCornerShape(9.dp))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        option,
                        color = if (isSelected) theme.textColor else theme.textColor.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
