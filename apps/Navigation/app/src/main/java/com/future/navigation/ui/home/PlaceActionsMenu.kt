package com.future.navigation.ui.home

import androidx.compose.runtime.Composable
import com.future.navigation.data.common.LatLng
import com.future.navigation.data.geocoding.GeocodeResult
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.i18n.tr
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureTheme

/**
 * מקום שאפשר לפעול עליו מתפריט ה-* : תוצאת חיפוש, יעד אחרון, מועדף או מקום
 * בקרבת מקום. [fromRecent] קובע אם מופיעה האפשרות "הסר מהאחרונים".
 */
data class PlaceTarget(
    val title: String,
    val address: String,
    val location: LatLng,
    val fromRecent: Boolean,
)

/** היעד כמו שמסלול הניווט מקבל אותו (כמו תוצאת חיפוש). */
fun PlaceTarget.asGeocodeResult(): GeocodeResult = GeocodeResult(label = address, location = location)

/**
 * תפריט הפעולות על מקום - נפתח בלחיצה על * כשהפוקוס על שורה. האפשרות הראשונה
 * (נווט) היא ברירת המחדל. "הסר מהאחרונים" מופיעה רק ליעדים מהרשימה הזו.
 */
@Composable
fun PlaceActionsMenu(
    target: PlaceTarget,
    isFavorite: Boolean,
    theme: FutureTheme,
    onDismiss: () -> Unit,
    onNavigate: () -> Unit,
    onSaveHome: () -> Unit,
    onSaveWork: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemoveRecent: () -> Unit,
) {
    FutureOptionsMenu(theme = theme, onDismissRequest = onDismiss, header = target.title) {
        FutureMenuRow(tr("נווט לכאן"), FutureIcons.DirectionsCar, theme, {
            onDismiss()
            onNavigate()
        })
        FutureMenuRow(tr("שמור כבית"), FutureIcons.Home, theme, {
            onDismiss()
            onSaveHome()
        })
        FutureMenuRow(tr("שמור כעבודה"), FutureIcons.Work, theme, {
            onDismiss()
            onSaveWork()
        })
        FutureMenuRow(
            if (isFavorite) tr("הסר ממועדפים") else tr("הוסף למועדפים"),
            if (isFavorite) FutureIcons.Star else FutureIcons.StarBorder,
            theme,
            {
                onDismiss()
                onToggleFavorite()
            },
        )
        if (target.fromRecent) {
            FutureMenuRow(tr("הסר מהאחרונים"), FutureIcons.Delete, theme, {
                onDismiss()
                onRemoveRecent()
            }, destructive = true)
        }
    }
}
