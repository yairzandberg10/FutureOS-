package com.future.futureui.controlcenter.ui.components

import androidx.compose.animation.*
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Subtle drop shadow applied to label text so it stays legible over any wallpaper/blur behind the glass chips. */
private val LegibilityShadow = Shadow(color = Color.Black.copy(alpha = 0.35f), blurRadius = 6f)

@Composable
fun TogglePill(
    label: String,
    icon: ImageVector,
    isOn: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false,
    onOptionPressed: () -> Unit = {},
    focusRequester: FocusRequester? = null,
    labelColor: Color = Color.Black
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(35.dp)

    Box(
        modifier = modifier
            .height(55.dp)
            .clip(shape)
            .focusEffect(isFocused, shape)
            .background(Color(0x80E0E0E0))
            .then(
                if (isFocused) Modifier.border(2.dp, Color.LightGray, shape) else Modifier
            )
            .padding(6.dp)
            .onKeyEvent { event ->
                if (isEditMode && event.type == KeyEventType.KeyDown && event.key == Key.Menu) {
                    onOptionPressed()
                    true
                } else false
            }
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggle
            )
            .focusable(interactionSource = interactionSource)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isOn) Color(0xFF6E6969) else Color(0xFF616161)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = labelColor,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                style = TextStyle(shadow = LegibilityShadow)
            )
        }
    }
}

@Composable
fun FocusableIcon(
    icon: ImageVector,
    label: String,
    isOn: Boolean,
    onToggle: () -> Unit,
    showLabel: Boolean,
    isEditMode: Boolean = false,
    isRemove: Boolean = false,
    onMenuClick: () -> Unit = {},
    labelColor: Color = Color.Black
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(54.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .focusEffect(isFocused, CircleShape)
                .clip(CircleShape)
                .background(if (isOn) Color(0xFF969494).copy(alpha = 0.9f) else Color(0xFF616161).copy(alpha = 0.8f))
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        if (event.key == Key.Menu || event.key == Key.Settings || event.key == Key.F1 || event.key == Key.Back) {
                            onMenuClick()
                            true
                        } else false
                    } else false
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onToggle
                )
                .focusable(interactionSource = interactionSource),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )

            if (isEditMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (isRemove) Color.Red else Color(0xFF4CAF50))
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRemove) Icons.Rounded.Remove else Icons.Rounded.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = showLabel,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    fontSize = 8.sp,
                    color = labelColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    style = TextStyle(shadow = LegibilityShadow)
                )
            }
        }
    }
}

@Composable
fun FocusableSection(
    id: String,
    isEditMode: Boolean,
    isMoving: Boolean,
    isGridEditing: Boolean = false,
    onLongClick: () -> Unit,
    onMove: (Int) -> Unit,
    onMenuClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(24.dp)
    
    var isKeyPressed by remember { mutableStateOf(false) }
    
    LaunchedEffect(isFocused, isKeyPressed, isEditMode, isMoving) {
        if (isFocused && isKeyPressed && isEditMode && !isMoving) {
            delay(800)
            if (isKeyPressed) {
                onLongClick()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isEditMode) {
                    Modifier
                        .border(
                            width = 2.dp,
                            color = when {
                                isMoving -> Color.Red
                                isFocused -> Color.LightGray
                                else -> Color.Transparent
                            },
                            shape = shape
                        )
                        .onKeyEvent { event ->
                            if (event.key == Key.DirectionCenter || event.key == Key.Enter) {
                                if (event.type == KeyEventType.KeyDown) {
                                    if (!isKeyPressed) {
                                        if (isMoving) {
                                            onLongClick()
                                        }
                                        isKeyPressed = true
                                    }
                                } else if (event.type == KeyEventType.KeyUp) {
                                    isKeyPressed = false
                                }
                                true
                            } else if (event.key == Key.Menu || event.key == Key.Settings || event.key == Key.F1) {
                                if (event.type == KeyEventType.KeyDown) {
                                    onMenuClick()
                                }
                                true
                            } else if (isMoving && event.type == KeyEventType.KeyDown) {
                                when (event.key) {
                                    Key.DirectionUp -> {
                                        onMove(-1)
                                        true
                                    }
                                    Key.DirectionDown -> {
                                        onMove(1)
                                        true
                                    }
                                    else -> false
                                }
                            } else false
                        }
                        .focusable(enabled = !isGridEditing, interactionSource = interactionSource)
                } else Modifier
            )
            .padding(if (isEditMode) 4.dp else 0.dp)
    ) {
        content()
    }
}
