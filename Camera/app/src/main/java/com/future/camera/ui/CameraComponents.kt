package com.future.camera.ui
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureContrast
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.scrimColor

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** כפתור עגול צף מעל תצוגת המצלמה - אותה שפת עיצוב כמו ToolsIconButton
 * (טבעת מיקוד בצבע ההדגשה, רקע כהה למראה שקוף מעל התצוגה החיה). */
/**
 * כפתור עגול צף מעל תצוגת המצלמה. במנוחה - עיגול בהכהיה של המערכת (60%
 * שחור, קבוע ולכן קריא מעל כל תמונה חיה); בפוקוס - מילוי מלא בהדגשה, והאייקון
 * בדיו שמתאים לה (קודם Color.Black קבוע - שחור על שחור עם הדגשה כהה).
 */
@Composable
fun CameraIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color = Color.White,
    size: Dp = 52.dp,
    iconSize: Dp = 24.dp,
    accentColor: Color,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(
        if (isFocused) accentColor else ScrimOverPreview,
        FutureMotion.focusColorSpec,
        label = "cameraIconBtnBg"
    )
    Box(
        modifier = Modifier
            .size(size)
            .background(bgColor, CircleShape)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = if (isFocused) FutureContrast.onColor(accentColor) else tint, modifier = Modifier.size(iconSize))
    }
}

/** ההכהיה של המערכת (--fos-scrim, 60% שחור) - מה שנותן לפקד קריאות מעל התצוגה החיה. */
val ScrimOverPreview: Color = FutureTheme().scrimColor

/** המצלמה תמיד מעל תמונה חיה, ולכן לוקחת את צבעי הסטטוס של הערכה הכהה. */
val CameraDanger: Color = FutureTheme(isDarkMode = true).dangerColor
