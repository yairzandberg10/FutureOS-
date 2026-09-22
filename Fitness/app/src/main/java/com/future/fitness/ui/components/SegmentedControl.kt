package com.future.fitness.ui.components
import com.future.sharednav.components.FutureTabItem
import com.future.sharednav.theme.FutureDimens
import androidx.compose.foundation.layout.Arrangement

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
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

/**
 * בורר פלחים (רמת קושי, יחידות משקל) - שורת הלשוניות של הדיזיין סיסטם
 * (components/navigation/TabRow.jsx). segmentWidth null = כל הפלחים
 * מתחלקים באותו רוחב; אחרת רוחב קבוע לכל פלח.
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selected: String,
    theme: FutureTheme,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    segmentWidth: Dp? = null,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm)) {
        options.forEach { option ->
            FutureTabItem(
                label = option,
                selected = option == selected,
                theme = theme,
                onClick = { onSelect(option) },
                modifier = if (segmentWidth != null) Modifier.width(segmentWidth) else Modifier.weight(1f),
            )
        }
    }
}
