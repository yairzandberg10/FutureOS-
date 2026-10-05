package com.future.futureui.controlcenter.ui.components

import com.future.sharednav.theme.FutureShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun HeaderActionButton(icon: ImageVector, color: Color, onClick: () -> Unit, isPower: Boolean = false) {
    // עטיפה דקה סביב TopBarIconButton המשותף (מודול SharedKeypadNav) - חתימת
    // הקריאה נשארת זהה כדי שקריאות קיימות ב-FutureUI לא ישתנו.
    com.future.sharednav.components.TopBarIconButton(
        icon = icon,
        contentDescription = "",
        textColor = if (isPower) Color.Red else color,
        accentColor = Color.White,
        onClick = onClick,
    )
}

fun Modifier.focusEffect(isFocused: Boolean, shape: androidx.compose.ui.graphics.Shape = FutureShapes.lg): Modifier = this
    .zIndex(if (isFocused) 1f else 0f)
    .then(if (isFocused) Modifier.controlFocusRing(shape) else Modifier)

/**
 * טבעת הפוקוס של מרכז הבקרה: קו כהה עבה ובתוכו קו לבן דק. הטבעת הקודמת
 * (2dp אפור-בהיר) לא נראתה על הזכוכית הבהירה (UI1, ‏SY3), ואחת כהה בלבד הייתה
 * נעלמת על עיגולי האייקונים הכהים - שני הצבעים יחד נראים על שניהם.
 */
fun Modifier.controlFocusRing(shape: androidx.compose.ui.graphics.Shape): Modifier = this.drawWithContent {
    drawContent()
    val outline = shape.createOutline(size, layoutDirection, this)
    val outer = 3.dp.toPx()
    val inner = 1.5.dp.toPx()
    drawOutline(outline, Color(0xFF111114), style = androidx.compose.ui.graphics.drawscope.Stroke(width = outer * 2))
    drawOutline(outline, Color.White, style = androidx.compose.ui.graphics.drawscope.Stroke(width = inner * 2))
}
