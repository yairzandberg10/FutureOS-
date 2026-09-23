package com.future.fitness.ui
import androidx.compose.material.icons.rounded.Dashboard

import com.future.sharednav.icons.FutureIcons

import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.future.sharednav.components.FutureBottomNav
import com.future.sharednav.components.FutureNavItem

/** שלושת הטאבים של הסרגל התחתון (ראשי/אימונים/התקדמות). המעבר ביניהם
 * קורה עם חיצי ימינה/שמאלה ברמת ה-Scaffold (ר' FitnessNavHost), ולכן
 * הפריטים עצמם *לא* פוקוסביליים - הכלל הזה נאכף עכשיו בתוך
 * [FutureBottomNav] ולא בכל אפליקציה בנפרד. */
enum class FitnessTab(val label: String, val icon: ImageVector) {
    HOME("ראשי", Icons.Rounded.Dashboard),
    WORKOUTS("אימונים", FutureIcons.FitnessCenter),
    PROGRESS("התקדמות", FutureIcons.AutoMirrored.TrendingUp),
}

@Composable
fun FitnessBottomNav(selectedTab: FitnessTab?, theme: com.future.sharednav.theme.FutureTheme) {
    FutureBottomNav(
        items = FitnessTab.entries.map { FutureNavItem(label = it.label, icon = it.icon) },
        selectedIndex = FitnessTab.entries.indexOf(selectedTab).coerceAtLeast(0),
        theme = theme,
    )
}
