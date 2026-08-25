package com.future.futureui.controlcenter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.futureui.controlcenter.ui.components.focusEffect
import kotlinx.coroutines.delay

/**
 * תפריט כיבוי בעיצוב FutureUI, מוצג מעל Control Center. קיים כי הדיאלוג
 * המקורי של אנדרואיד (GLOBAL_ACTION_POWER_DIALOG) לא בנוי לניווט בשלט/מקלדת
 * T9 בלבד ולא תואם ויזואלית לשאר המערכת. כיבוי/הפעלה-מחדש בפועל דורשים
 * הרשאת root (כמו שאר הפעולות ב-ControlManager) כי לאפליקציה רגילה אין
 * גישה ציבורית לפעולות האלה.
 */
@Composable
fun PowerMenuScreen(
    onPowerOff: () -> Unit,
    onRestart: () -> Unit,
    onCancel: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(100)
        try { focusRequester.requestFocus() } catch (t: Throwable) {}
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 230.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xEE1C1C1E))
                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    text = "אפשרויות כיבוי",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp)
                )
                PowerMenuRow(
                    icon = Icons.Rounded.PowerSettingsNew,
                    label = "כיבוי",
                    tint = Color(0xFFFF453A),
                    onClick = onPowerOff,
                    modifier = Modifier.focusRequester(focusRequester)
                )
                PowerMenuRow(
                    icon = Icons.Rounded.RestartAlt,
                    label = "הפעלה מחדש",
                    tint = Color.White,
                    onClick = onRestart
                )
                PowerMenuRow(
                    icon = Icons.Rounded.Close,
                    label = "ביטול",
                    tint = Color.White.copy(alpha = 0.7f),
                    onClick = onCancel
                )
            }
        }
    }
}

@Composable
private fun PowerMenuRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .focusEffect(isFocused, shape)
            .clip(shape)
            .background(if (isFocused) Color.White.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}
