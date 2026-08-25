package com.future.futureui.controlcenter.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.futureui.controlcenter.logic.ControlManager

@Composable
fun MusicPlayerCard(manager: ControlManager, labelColor: Color = Color.Black) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(35.dp)
    val legibilityShadow = androidx.compose.ui.graphics.Shadow(color = Color.Black.copy(alpha = 0.35f), blurRadius = 6f)

    if (!manager.isMediaServiceEnabled) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(shape)
                .focusEffect(isFocused, shape)
                .background(Color(0x80E0E0E0))
                .then(
                    if (isFocused) Modifier.border(2.dp, Color.LightGray, shape) else Modifier
                )
                .clickable(interactionSource = interactionSource, indication = null) { manager.openNotificationAccessSettings() }
                .focusable(interactionSource = interactionSource)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "לחץ כאן לאישור גישה למוזיקה",
                color = labelColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                style = androidx.compose.ui.text.TextStyle(shadow = legibilityShadow)
            )
        }
    } else if (!manager.hasActiveMedia) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(65.dp)
                .clip(shape)
                .focusEffect(isFocused, shape)
                .background(Color(0x80E0E0E0))
                .then(
                    if (isFocused) Modifier.border(2.dp, Color.LightGray, shape) else Modifier
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { /* Could open music player if desired */ }
                )
                .focusable(interactionSource = interactionSource)
                .padding(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF616161)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "אין מוזיקה מתנגנת",
                    fontSize = 13.sp,
                    color = labelColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    style = androidx.compose.ui.text.TextStyle(shadow = legibilityShadow)
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(shape)
                .background(Color.Black)
                .border(0.5.dp, Color.White.copy(alpha = 0.2f), shape)
        ) {
            manager.currentAlbumArt?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.6f),
                    contentScale = ContentScale.Crop
                )
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = manager.currentSongTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        text = manager.currentArtist,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    val progress = if (manager.mediaDuration > 0) manager.mediaPosition.toFloat() / manager.mediaDuration.toFloat() else 0f
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .background(Color.White)
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatMillis(manager.mediaPosition),
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = formatMillis(manager.mediaDuration),
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MediaControlButton(icon = Icons.Rounded.SkipPrevious, onClick = { manager.previousTrack() }, tint = Color.White)
                    Spacer(modifier = Modifier.width(24.dp))
                    MediaControlButton(
                        icon = if (manager.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        onClick = { manager.togglePlayPause() },
                        isLarge = true,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    MediaControlButton(icon = Icons.Rounded.SkipNext, onClick = { manager.nextTrack() }, tint = Color.White)
                }
            }
        }
    }
}

private fun formatMillis(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun MediaControlButton(icon: ImageVector, onClick: () -> Unit, isLarge: Boolean = false, tint: Color = Color.Black) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = CircleShape

    Box(
        modifier = Modifier
            .size(if (isLarge) 50.dp else 40.dp)
            .graphicsLayer {
                scaleX = if (isFocused) 1.2f else 1f
                scaleY = if (isFocused) 1.2f else 1f
            }
            .then(if (isFocused) Modifier.border(2.dp, Color.LightGray, shape) else Modifier)
            .clip(shape)
            .background(if (isFocused) Color.White.copy(alpha = 0.3f) else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused) Color.White else tint,
            modifier = Modifier.size(if (isLarge) 32.dp else 24.dp)
        )
    }
}
