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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * דיאלוג אישור אחיד לפעולות הרסניות (מחיקת קובץ/איש קשר/הודעה/התראה) -
 * מבוסס על ה-ConfirmDialog הפרטי שהיה קיים רק ב-Files/FilesScreen.kt
 * ומועתק/מפוזר מחדש ידנית בכל אפליקציה שצריכה דיאלוג כזה (Contact,
 * Messages, Clock). מנווט לגמרי במקלדת: כפתור ה-cancel/confirm שממוקד
 * מסומן גם בצבע וגם במסגרת (לא רק בצבע - כדי שאפשר יהיה להבחין בפוקוס גם
 * בלי תלות בניגודיות הצבע הספציפי).
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
    Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(surfaceColor).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(message, color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ConfirmDialogButton(cancelLabel, textColor.copy(alpha = 0.7f), onCancel)
                ConfirmDialogButton(confirmLabel, dangerColor, onConfirm)
            }
        }
    }
}

@Composable
private fun ConfirmDialogButton(text: String, color: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(if (isFocused) color else color.copy(alpha = 0.7f), label = "confirmDialogBtnBg")
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .then(
                if (isFocused) Modifier.border(width = 2.dp, color = Color.White, shape = RoundedCornerShape(20.dp))
                else Modifier
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Text(text, color = Color.Black, fontWeight = FontWeight.Bold)
    }
}
