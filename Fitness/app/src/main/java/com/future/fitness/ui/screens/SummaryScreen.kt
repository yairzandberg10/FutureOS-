package com.future.fitness.ui.screens
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.theme.elevatedSurfaceColor
import com.future.sharednav.theme.idleFieldColor
import com.future.sharednav.theme.idleChipColor
import com.future.sharednav.theme.focusFillChipColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.subtleTextColor
import com.future.sharednav.theme.sectionHeaderColor
import com.future.sharednav.theme.textAlpha
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureButtonVariant
import com.future.fitness.ui.components.FitnessTextField

import com.future.sharednav.theme.onReadableAccentColor
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.sharednav.theme.FutureTheme

/** מסך סיכום גנרי לכל סוג פעילות שהסתיימה (אימון כוח או ריצה) - כותרת/כותרת
 * משנה, ורשימת סטטיסטיקות (ערך + תווית) שהמסך הקורא מרכיב לפי מה שרלוונטי
 * (למשל אימון: דקות/קלוריות/סטים/דופק ממוצע; ריצה: דקות/ק"מ/קלוריות). */
@Composable
fun SummaryScreen(
    title: String,
    subtitle: String,
    stats: List<Pair<String, String>>,
    theme: FutureTheme,
    onDone: () -> Unit,
) {
    val doneButtonFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { doneButtonFocusRequester.requestFocus() }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        FutureAvatar(theme = theme, icon = Icons.Rounded.Check, size = 76.dp)
        Spacer(Modifier.height(16.dp))
        Text(title, color = theme.textColor, fontSize = FutureTypography.headline, fontWeight = FontWeight.Bold)
        Text(subtitle, color = theme.textColor.copy(alpha = 0.6f), fontSize = FutureTypography.body, modifier = Modifier.padding(top = 4.dp, bottom = 28.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(28.dp), modifier = Modifier.padding(bottom = 34.dp)) {
            stats.forEach { (value, label) -> SummaryStat(value, label, theme) }
        }

        FutureButton("חזרה לבית", theme, onDone, focusRequester = doneButtonFocusRequester)
    }
}

@Composable
private fun SummaryStat(value: String, label: String, theme: FutureTheme) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = theme.textColor, fontSize = FutureTypography.headline, fontWeight = FontWeight.Bold)
        Text(label, color = theme.textColor.copy(alpha = 0.6f), fontSize = FutureTypography.caption)
    }
}
