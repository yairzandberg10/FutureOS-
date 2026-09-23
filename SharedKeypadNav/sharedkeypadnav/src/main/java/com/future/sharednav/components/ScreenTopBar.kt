package com.future.sharednav.components
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.future.sharednav.focus.focusMotion
import com.future.sharednav.theme.FutureContrast
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureTransitions
import com.future.sharednav.theme.rememberFutureType

/**
 * שורת כותרת משותפת למסכי רשימה - כפתור חזור (RTL: חץ ימינה) + כותרת +
 * כפתור פעולה אופציונלי בצד ההפוך.
 *
 * כשהכותרת משתנה בתוך אותו מסך (מעבר תיקייה בקבצים, החלפת פרק בספרים)
 * היא מתחלפת ב-crossfade ולא קופצת.
 */
@Composable
fun ScreenTopBar(
    title: String,
    textColor: Color,
    accentColor: Color,
    onBack: (() -> Unit)? = null,
    trailingIcon: ImageVector? = null,
    trailingContentDescription: String? = null,
    onTrailingClick: (() -> Unit)? = null,
    trailingFocusRequester: FocusRequester? = null,
) {
    val type = rememberFutureType()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FutureDimens.spacingLg, vertical = FutureDimens.spacingMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            TopBarIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "חזור", textColor, accentColor, onBack)
            Spacer(modifier = Modifier.width(FutureDimens.spacingSm))
        }
        AnimatedContent(
            targetState = title,
            transitionSpec = { FutureTransitions.appear() },
            modifier = Modifier.weight(1f),
            label = "topBarTitle",
        ) { shownTitle ->
            Text(
                shownTitle,
                fontSize = type.screenTitle,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailingIcon != null && onTrailingClick != null) {
            TopBarIconButton(trailingIcon, trailingContentDescription ?: "", textColor, accentColor, onTrailingClick, trailingFocusRequester)
        }
    }
}

/**
 * כפתור אייקון עגול. הפוקוס מסומן במילוי *ובטבעת* - קודם הוא סומן רק
 * בשינוי צבע רקע של 30% מצבע ההדגשה, שבמצב בהיר עם ההדגשה הלבנה של ברירת
 * המחדל פשוט לא נראה.
 */
@Composable
fun TopBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    textColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
) {
    // החתימה המקורית נשמרת כעומס נפרד: קריאות עם lambda נגרר
    // (`TopBarIconButton(icon, "", c, a) { ... }`) נקשרות תמיד לפרמטר האחרון,
    // ולכן הפרמטרים החדשים לא יכולים לבוא אחרי onClick באותה פונקציה.
    TopBarIconButton(icon, contentDescription, textColor, accentColor, onClick, focusRequester = null)
}

@Composable
fun TopBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    textColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
    enabled: Boolean = true,
    // צבע האייקון כשהוא שונה מצבע הטקסט - החצים של בורר השעה צבועים בהדגשה
    // (TimePicker.jsx), אבל הרקע והטבעת נשארים נגזרים מהטקסט.
    iconColor: Color? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val ring = FutureContrast.accentForText(accentColor, textColor)

    // כפתור אייקון הוא החריג השני בפירוט הפוקוס (guidelines/focus-spec.html):
    // הסימון שלו הוא רקע ב-30% מההדגשה ובלי מסגרת כלל. ב-22% שהיה כאן
    // הרקע לא נשא את הסימון לבדו, ולכן נוספה לו מסגרת שאינה בעיצוב.
    val bgColor by animateColorAsState(
        if (isFocused) ring.copy(alpha = 0.30f) else textColor.copy(alpha = 0.08f),
        FutureMotion.focusColorSpec,
        label = "topBarIconBtnBg",
    )
    Box(
        modifier = Modifier
            .size(FutureDimens.rowHeightTopBarButton)
            .focusMotion(interactionSource, focusedScale = 1.08f, pressedScale = 0.92f)
            .clip(CircleShape)
            .background(bgColor)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            // כפתור שאי אפשר להפעיל (שליחה בלי טקסט) לא מקבל פוקוס בכלל, והאייקון
            // שלו יורד ל-40% - אותו כלל של FutureButton.
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val tint = iconColor ?: textColor
        Icon(icon, contentDescription = contentDescription, tint = if (enabled) tint else tint.copy(alpha = 0.4f), modifier = Modifier.size(FutureDimens.iconTopBar))
    }
}
