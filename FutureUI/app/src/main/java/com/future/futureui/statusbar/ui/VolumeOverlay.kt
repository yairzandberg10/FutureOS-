package com.future.futureui.statusbar.ui
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.elevatedSurfaceColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.onReadableAccentColor

import com.future.sharednav.theme.FutureShapes
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
    val shape = FutureShapes.xxl
    val theme = LocalFutureTheme.current

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
                // "זכוכית" של המעטפת (elevatedSurfaceColor) - לא אפור בהיר קבוע,
                // שנראה אותו דבר במצב כהה ובהיר.
                .background(theme.elevatedSurfaceColor),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(level.coerceIn(0f, 1f).coerceAtLeast(0.01f))
                    .clip(shape)
                    .background(theme.readableAccentColor)
            )
            Icon(
                imageVector = if (level <= 0f) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = null,
                tint = if (level >= 0.12f) theme.onReadableAccentColor else theme.textColor,
                modifier = Modifier.padding(start = 14.dp).size(22.dp)
            )
        }
    }
}
