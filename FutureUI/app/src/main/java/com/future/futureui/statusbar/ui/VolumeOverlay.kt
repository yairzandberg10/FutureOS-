package com.future.futureui.statusbar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * חלון עצמאי, זמני, שמופיע כשלוחצים על מקשי הווליום ונעלם אחרי כמה שניות -
 * מחליף את חלונית הווליום המקורית של אנדרואיד (שמוסתרת ע"י צריכת האירוע בשירות).
 * העיצוב תואם בכוונה למראה "זכוכית כהה בהירה" של SliderBar במרכז הבקרה,
 * כדי שההדגשות הזמניות ירגישו כמו חלק מאותה שפת עיצוב - לא רכיב זר.
 */
@Composable
fun VolumeOverlay(level: Float, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(30.dp)

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .padding(top = 44.dp)
                .width(200.dp)
                .height(47.dp)
                .clip(shape)
                .background(Color(0xE6E0E0E0))
                .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.6f), shape = shape),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(level.coerceIn(0f, 1f).coerceAtLeast(0.01f))
                    .clip(shape)
                    .background(Color(0xFFBDBDBD))
            )
            Icon(
                imageVector = if (level <= 0f) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = null,
                tint = Color(0xFF616161),
                modifier = Modifier.padding(start = 14.dp).size(22.dp)
            )
        }
    }
}
