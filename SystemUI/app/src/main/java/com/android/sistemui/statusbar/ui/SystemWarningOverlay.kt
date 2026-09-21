package com.android.sistemui.statusbar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * חלונית אזהרת מערכת חולפת (בסגנון הכרטיס הכהה של PowerMenuScreen) - כרגע
 * משמשת רק לאזהרת סוללה חלשה מאוד (ACTION_BATTERY_LOW), אבל בנויה כללית
 * מספיק לאזהרות מערכת עתידיות דומות.
 */
@Composable
fun SystemWarningOverlay(message: String, percent: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Row(
            modifier = Modifier
                .padding(top = 44.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xEE1C1C1E))
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.BatteryAlert, contentDescription = null, tint = Color(0xFFFF453A), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = message, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(text = "$percent% סוללה", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            }
        }
    }
}
