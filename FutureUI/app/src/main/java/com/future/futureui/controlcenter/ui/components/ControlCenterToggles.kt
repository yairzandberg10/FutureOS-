package com.future.futureui.controlcenter.ui.components
import androidx.compose.material.icons.rounded.Remove

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.elevatedSurfaceColor
import com.future.sharednav.theme.raisedSurfaceColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.onReadableAccentColor
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.textAlpha

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
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
    labelColor: Color = Color.Unspecified
) {
    val theme = LocalFutureTheme.current
    val ink = if (labelColor == Color.Unspecified) theme.textColor else labelColor
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = FutureShapes.xxl

    // מקש Options הפיזי נחסם ברמת המערכת ולעולם לא מגיע כ-Key.Menu לכאן - זו הדרך
    // האמיתית שהוא מפעיל את "החלפת הקיצור" הזה כשהוא ממוקד במצב עריכה.
    com.future.sharednav.nav.onOptionsKeyPress { if (isFocused && isEditMode) onOptionPressed() }

    Box(
        modifier = modifier
            .height(55.dp)
            .clip(shape)
            .focusEffect(isFocused, shape)
            .background(theme.elevatedSurfaceColor)
            .then(
                if (isFocused) Modifier.border(FutureDimens.focusBorderControl, theme.readableAccentColor, shape) else Modifier
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
                    // דלוק = נבחר, ולכן מילוי מלא בהדגשה (states.html); כבוי = דרגה אחת
                    // מעל הזכוכית. קודם שני המצבים היו #6E6969 מול #616161 - כמעט זהים.
                    .background(if (isOn) theme.readableAccentColor else theme.raisedSurfaceColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isOn) theme.onReadableAccentColor else theme.textColor,
                    modifier = Modifier.size(FutureDimens.iconTopBar)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                fontSize = FutureTypography.label,
                color = ink,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                
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
    labelColor: Color = Color.Unspecified
) {
    val theme = LocalFutureTheme.current
    val ink = if (labelColor == Color.Unspecified) theme.textColor else labelColor
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // מקש Options הפיזי נחסם ברמת המערכת ולעולם לא מגיע כ-Key.Menu לכאן - זו הדרך
    // האמיתית שהוא מפעיל את onMenuClick כשהאייקון הזה ממוקד.
    com.future.sharednav.nav.onOptionsKeyPress { if (isFocused) onMenuClick() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(54.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .focusEffect(isFocused, CircleShape)
                .clip(CircleShape)
                .background(if (isOn) theme.readableAccentColor else theme.raisedSurfaceColor)
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
                tint = if (isOn) theme.onReadableAccentColor else theme.textColor,
                modifier = Modifier.size(FutureDimens.iconMenuRow)
            )

            if (isEditMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (isRemove) theme.dangerColor else theme.successColor)
                        .border(1.5.dp, theme.surfaceColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRemove) Icons.Rounded.Remove else FutureIcons.Add,
                        contentDescription = null,
                        tint = com.future.sharednav.theme.FutureContrast.onColor(if (isRemove) theme.dangerColor else theme.successColor),
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
                    fontSize = FutureTypography.caption,
                    color = ink,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    
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
    val theme = LocalFutureTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = FutureShapes.xl

    // מקש Options הפיזי נחסם ברמת המערכת ולעולם לא מגיע כ-Key.Menu לכאן - זו הדרך
    // האמיתית שהוא מפעיל את onMenuClick (עריכת אריחי הרשת) כשהסקשן ממוקד במצב עריכה.
    com.future.sharednav.nav.onOptionsKeyPress { if (isFocused && isEditMode) onMenuClick() }

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
                                isMoving -> theme.dangerColor
                                isFocused -> theme.readableAccentColor
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
