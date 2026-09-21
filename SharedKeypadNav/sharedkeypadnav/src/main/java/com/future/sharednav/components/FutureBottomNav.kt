package com.future.sharednav.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.onReadableAccentColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.rememberFutureType

/**
 * הסרגל התחתון של המערכת. שלוש אפליקציות בלבד מחזיקות אחד (שעון, חייגן,
 * כושר), וכל אחת מהן בנתה אותו בנפרד מ-NavigationBar של Material3 - כלומר
 * סרגל ברוחב מלא, צמוד לתחתית המסך, עם תווית מתחת לכל אייקון ובגיאומטריה
 * של Material ולא של FutureOS.
 *
 * העיצוב (components/navigation/BottomNav.jsx) הוא סרגל אחר לגמרי: פס
 * מרחף בצורת גלולה, מוסט מהשוליים, שבו רק הפריט הנבחר מציג תווית - והיא
 * יושבת *בתוך* גלולת ההדגשה לצד האייקון, לא מתחתיו.
 *
 * הפריטים אינם פוקוסביליים בכוונה: מעבר בין טאבים נעשה בחצי ימין/שמאל
 * ברמת המסך. לו היו פוקוסביליים, הפוקוס ההתחלתי היה עלול לנחות עליהם
 * ולחיצת אישור הייתה מפעילה טאב במקום את הפריט הממוקד במסך.
 */
data class FutureNavItem(
    val label: String,
    val icon: ImageVector,
    /** האייקון במצב נבחר. בעיצוב הוא מלא, והלא-נבחר מתאר בלבד. */
    val selectedIcon: ImageVector = icon,
)

@Composable
fun FutureBottomNav(
    items: List<FutureNavItem>,
    selectedIndex: Int,
    theme: FutureTheme,
    modifier: Modifier = Modifier,
) {
    val type = rememberFutureType()

    // הגלולה נצבעת בהדגשה *המתוקנת* ולא בגולמית. ברירת המחדל של ההדגשה
    // היא לבן, ובמצב בהיר גלולה לבנה על כרטיס לבן נעלמת - וזו בדיוק
    // התקלה שהעיצוב עצמו מסמן כפגם שאסור לשחזר.
    val pill = theme.readableAccentColor
    val onPill = theme.onReadableAccentColor

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FutureDimens.spacingLg, vertical = FutureDimens.spacingLg)
            .clip(FutureShapes.pill)
            .background(theme.surfaceColor)
            .height(NavBarHeight)
            .padding(horizontal = FutureDimens.spacingSm)
            .focusProperties { canFocus = false },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            NavItem(
                item = item,
                isSelected = index == selectedIndex,
                textColor = theme.textColor,
                pillColor = pill,
                onPillColor = onPill,
                labelSize = type.body,
            )
        }
    }
}

/** גובה הסרגל - 66dp, מתוך tokens/spacing.css דרך העיצוב של הרכיב. */
private val NavBarHeight = 66.dp

/** גובה גלולת ההדגשה בתוך הסרגל. */
private val NavPillHeight = 46.dp

/** האייקון בסרגל - אותו גודל של שורת תפריט. */
private val NavIconSize = FutureDimens.iconMenuRow

@Composable
private fun RowScope.NavItem(
    item: FutureNavItem,
    isSelected: Boolean,
    textColor: Color,
    pillColor: Color,
    onPillColor: Color,
    labelSize: androidx.compose.ui.unit.TextUnit,
) {
    // הפריט הנבחר רחב יותר כי הוא היחיד שנושא תווית.
    val background by animateColorAsState(
        if (isSelected) pillColor else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "bottomNavPill",
    )
    Box(
        modifier = Modifier
            .weight(if (isSelected) 1.4f else 1f)
            .focusProperties { canFocus = false },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .height(NavPillHeight)
                .clip(FutureShapes.pill)
                .background(background)
                .padding(horizontal = if (isSelected) FutureDimens.spacingMd else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
        ) {
            Icon(
                if (isSelected) item.selectedIcon else item.icon,
                contentDescription = item.label,
                tint = if (isSelected) onPillColor else textColor.copy(alpha = 0.5f),
                modifier = Modifier.size(NavIconSize),
            )
            if (isSelected) {
                Text(
                    item.label,
                    color = onPillColor,
                    fontSize = labelSize,
                    fontWeight = com.future.sharednav.theme.FutureTypography.weightMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
