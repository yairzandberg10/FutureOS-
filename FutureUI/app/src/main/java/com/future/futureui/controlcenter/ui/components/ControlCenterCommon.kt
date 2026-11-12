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
import com.future.sharednav.focus.animateFocusFloat
import com.future.sharednav.focus.focusScale

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

/**
 * פוקוס של פקד במרכז הבקרה ובשאר חלונות המערכת: הטבעת נצבעת פנימה (ולא
 * קופצת בבת אחת), והפקד גדל בקפיץ press - אותה תנועה כמו בשאר המערכת
 * (ר' focusMotion ב-SharedKeypadNav). zIndex נשאר מורם עד שהטבעת דועכת,
 * כדי שהפקד שיוצא מפוקוס לא ייחתך מתחת לשכן בזמן שהוא מתכווץ.
 */
@Composable
fun Modifier.focusEffect(
    isFocused: Boolean,
    shape: androidx.compose.ui.graphics.Shape = FutureShapes.lg,
    focusedScale: Float = 1.04f,
): Modifier {
    val ring = animateFocusFloat(isFocused, 1f, 0f)
    return this
        .zIndex(if (isFocused || ring.value > 0f) 1f else 0f)
        .focusScale(isFocused, focusedScale)
        .controlFocusRing(shape) { ring.value }
}

/**
 * טבעת הפוקוס של מרכז הבקרה: קו כהה עבה ובתוכו קו לבן דק. הטבעת הקודמת
 * (2dp אפור-בהיר) לא נראתה על הזכוכית הבהירה (UI1, ‏SY3), ואחת כהה בלבד הייתה
 * נעלמת על עיגולי האייקונים הכהים - שני הצבעים יחד נראים על שניהם.
 */
fun Modifier.controlFocusRing(shape: androidx.compose.ui.graphics.Shape, alpha: () -> Float = { 1f }): Modifier = this.drawWithContent {
    drawContent()
    val a = alpha()
    if (a <= 0f) return@drawWithContent
    val outline = shape.createOutline(size, layoutDirection, this)
    val outer = 3.dp.toPx()
    val inner = 1.5.dp.toPx()
    drawOutline(outline, Color(0xFF111114), alpha = a, style = androidx.compose.ui.graphics.drawscope.Stroke(width = outer * 2))
    drawOutline(outline, Color.White, alpha = a, style = androidx.compose.ui.graphics.drawscope.Stroke(width = inner * 2))
}
