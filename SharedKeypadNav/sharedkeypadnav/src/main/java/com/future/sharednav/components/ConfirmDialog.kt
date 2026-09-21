package com.future.sharednav.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.future.sharednav.focus.focusMotion
import com.future.sharednav.theme.FutureContrast
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.rememberFutureType

/**
 * דיאלוג אישור אחיד לפעולות הרסניות (מחיקת קובץ/איש קשר/הודעה/התראה).
 * מנווט לגמרי במקלדת: הכפתור הממוקד מסומן גם בצבע וגם בטבעת.
 *
 * שני באגים של מצב בהיר תוקנו כאן: הטקסט על הכפתורים היה Color.Black
 * קבוע, ובמצב בהיר כפתור הביטול הוא שחור-70% - כלומר "ביטול" בשחור על
 * שחור. וטבעת הפוקוס הייתה Color.White קבוע על דיאלוג לבן. עכשיו שניהם
 * נגזרים מהניגודיות ([FutureContrast]).
 */
@Composable
fun ConfirmDialog(
    message: String,
    surfaceColor: Color,
    textColor: Color,
    dangerColor: Color,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    cancelLabel: String = "ביטול",
    confirmLabel: String = "מחק",
) {
    val type = rememberFutureType()
    AppDialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .clip(FutureShapes.dialog)
                .background(surfaceColor)
                .padding(FutureDimens.spacingXl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(message, color = textColor, fontWeight = FontWeight.Bold, fontSize = type.dialog)
            Spacer(modifier = Modifier.height(FutureDimens.spacingLg))
            Row(horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingMd)) {
                ConfirmDialogButton(cancelLabel, textColor.copy(alpha = 0.7f), textColor, onCancel)
                ConfirmDialogButton(confirmLabel, dangerColor, textColor, onConfirm)
            }
        }
    }
}

@Composable
private fun ConfirmDialogButton(text: String, color: Color, ringColor: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(
        if (isFocused) color else color.copy(alpha = color.alpha * 0.7f),
        FutureMotion.focusColorSpec,
        label = "confirmDialogBtnBg",
    )
    val ring by animateColorAsState(
        if (isFocused) ringColor else ringColor.copy(alpha = 0f),
        FutureMotion.focusColorSpec,
        label = "confirmDialogBtnRing",
    )
    Box(
        modifier = Modifier
            .focusMotion(interactionSource, focusedScale = 1.04f)
            .clip(FutureShapes.pill)
            .background(bgColor)
            .border(width = FutureDimens.focusBorderWidth, color = ring, shape = FutureShapes.pill)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = FutureDimens.spacingXl, vertical = FutureDimens.spacingMd),
    ) {
        // הצבע מחושב מהכפתור כפי שהוא נראה בפוקוס (אטום), לא מהגרסה השקופה.
        Text(text, color = FutureContrast.onColor(color.copy(alpha = 1f)), fontWeight = FontWeight.Bold)
    }
}
