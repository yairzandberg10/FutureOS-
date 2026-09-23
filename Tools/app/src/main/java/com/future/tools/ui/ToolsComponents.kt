package com.future.tools.ui
import androidx.compose.material.icons.rounded.Remove

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.AvatarListSize

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.ScreenTopBar as SharedScreenTopBar
import com.future.sharednav.components.TopBarIconButton as SharedTopBarIconButton
import com.future.sharednav.focus.bringIntoViewOnFocus
import com.future.sharednav.theme.FutureContrast
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.idleFieldColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.rememberFutureType

/**
 * כפתור אייקון - עטיפה דקה סביב TopBarIconButton המשותף. האייקון בצבע
 * הטקסט (components/core/IconButton.jsx: "18dp glyph in the text color");
 * קודם הוא נצבע כברירת מחדל בצבע ההדגשה.
 */
@Composable
fun ToolsIconButton(icon: ImageVector, contentDescription: String, theme: FutureTheme, tint: Color = theme.textColor, onClick: () -> Unit) {
    SharedTopBarIconButton(icon, contentDescription, tint, theme.accentColor, onClick)
}

/**
 * השורה העליונה. בלי תוכן בצד השני היא ScreenTopBar עצמו; עם תוכן חופשי
 * (כמה כפתורים) היא אותה שורה בדיוק - אותו ריפוד 16/12dp, אותה כותרת 20sp.
 * קודם הגרסה עם התוכן הייתה בכותרת 17sp ובריפוד 14dp, ולכן כותרת המסך
 * "קפצה" בגודל בין מסך למסך באותה אפליקציה.
 */
@Composable
fun ToolsHeader(title: String, theme: FutureTheme, onBack: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    if (trailing == null) {
        SharedScreenTopBar(title = title, textColor = theme.textColor, accentColor = theme.accentColor, onBack = onBack)
    } else {
        val type = rememberFutureType()
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.spacingLg, vertical = FutureDimens.spacingMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // אין כפתור חזור על המסך - מקש BACK הפיזי עושה את זה
            if (onBack != null) androidx.activity.compose.BackHandler(onBack = onBack)
            Text(
                title,
                color = theme.textColor,
                fontSize = type.screenTitle,
                fontWeight = FutureTypography.weightBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = true),
            )
            trailing.invoke()
        }
    }
}

/**
 * שורת כלי במסך הבית - שורת רשימה רגילה של הדיזיין סיסטם. קודם היא הייתה
 * אריח ב-5.5% (דרגה שלא קיימת בסולם), פוקוס ב-14% מהטקסט במקום מההדגשה,
 * ועיגול אייקון ב-18% מההדגשה - כלומר ההדגשה כקישוט.
 */
@Composable
fun ToolRow(
    icon: ImageVector,
    label: String,
    subtitle: String,
    theme: FutureTheme,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    focusRequester: FocusRequester? = null
) {
    FutureListItem(
        title = label,
        summary = subtitle,
        theme = theme,
        onClick = onClick,
        focusRequester = focusRequester,
        leading = { ToolIcon(icon, theme) },
        trailing = trailing?.let { content -> { content() } },
    )
}

/** אייקון בתחילת שורה - האווטאר של הדיזיין סיסטם (Avatar.jsx), עם אייקון. */
@Composable
fun ToolIcon(icon: ImageVector, theme: FutureTheme, size: Dp = AvatarListSize) {
    FutureAvatar(theme = theme, icon = icon, size = size)
}

/** כפתור +/- ליד ערך מספרי - כפתור אייקון רגיל (8% במנוחה, 30% הדגשה בפוקוס). */
@Composable
fun ToolsStepperButton(label: String, theme: FutureTheme, onClick: () -> Unit) {
    val icon = if (label.trim() == "+") FutureIcons.Add else Icons.Rounded.Remove
    SharedTopBarIconButton(icon, label, theme.textColor, theme.accentColor, onClick)
}

/**
 * כפתור פעולה עגול וגדול (מיקרופון, התחל/השהה). ההתנהגות היא של כפתור
 * הדיזיין סיסטם: במנוחה 70% אטימות, בפוקוס מלא עם מסגרת 2dp בצבע הטקסט.
 * התוכן בצבע הדיו שמתאים למילוי - לא Color.Black קבוע, שנעלם על מילוי כהה.
 */
@Composable
fun ToolsRoundActionButton(
    theme: FutureTheme,
    size: Dp,
    fill: Color,
    onClick: () -> Unit,
    content: @Composable (contentColor: Color) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val opacity by animateFloatAsState(if (isFocused) 1f else 0.7f, FutureMotion.fast(), label = "roundActionOpacity")
    val ring by animateColorAsState(
        if (isFocused) theme.textColor else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "roundActionRing",
    )
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .alpha(opacity)
            .background(fill)
            .border(FutureDimens.focusBorderControl, ring, CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .bringIntoViewOnFocus(),
        contentAlignment = Alignment.Center
    ) {
        content(FutureContrast.onColor(fill))
    }
}

/** מילוי "ראשי" לכפתור עגול - ההדגשה המתוקנת, כמו FutureButton.Primary. */
val FutureTheme.toolsPrimaryFill: Color get() = readableAccentColor
