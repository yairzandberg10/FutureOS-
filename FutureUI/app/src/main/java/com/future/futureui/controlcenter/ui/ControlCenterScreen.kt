package com.future.futureui.controlcenter.ui

import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.futureui.controlcenter.logic.ControlLayoutManager
import com.future.futureui.controlcenter.logic.ControlManager
import com.future.futureui.controlcenter.logic.PillCatalog
import com.future.futureui.controlcenter.logic.PillControlManager
import com.future.futureui.controlcenter.ui.components.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ControlCenterScreen(
    modifier: Modifier = Modifier,
    isVisibleByDefault: Boolean = false,
    isEditModeByDefault: Boolean = false,
    wallpaper: ImageBitmap? = null,
    controlManager: ControlManager? = null,
    onPowerClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSwitchToNotificationCenter: () -> Unit = {},
    onRequestClose: () -> Unit = {}
) {
    val context = LocalContext.current
    val manager = controlManager ?: remember { ControlManager(context) }
    val layoutManager = remember { ControlLayoutManager(context) }

    var isVisible by remember { mutableStateOf(isVisibleByDefault) }
    var isExpanded by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(isEditModeByDefault) }
    var isGridEditing by remember { mutableStateOf(isEditModeByDefault) }
    var isCenterKeyPressed by remember { mutableStateOf(false) }

    val pillManager = remember { PillControlManager(context) }
    DisposableEffect(pillManager) { onDispose { pillManager.dispose() } }
    var pillIds by remember { mutableStateOf(layoutManager.getPillIds()) }
    var activeControlIds by remember { mutableStateOf(layoutManager.getActiveLayout()) }
    var sectionOrder by remember { mutableStateOf(layoutManager.getSectionOrder()) }
    var movingSectionId by remember { mutableStateOf<String?>(null) }

    // בלי בקשת פוקוס ראשונית מפורש, אף אלמנט לא ממוקד כשהמסך נפתח - ואז מקשי
    // הכיוונים/OK של השלט לא מגיעים לשום מקום, וכל הכפתורים נראים "לא עובדים".
    val initialFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        isVisible = true
        while(true) {
            manager.updateStates()
            manager.updateMediaController()
            manager.updateProgress()
            // Faster update when visible for responsive toggles
            delay(500)
        }
    }

    // מצב הגלולות נקרא בקצב איטי יותר מהלולאה למעלה - חלק מהקריאות עוברות דרך root
    LaunchedEffect(pillIds) {
        while (true) {
            pillManager.refresh(pillIds)
            delay(2000)
        }
    }

    // null = גלולת "הוספת פקד", שמופיעה רק במצב עריכה וכל עוד לא הגענו למקסימום
    val pillCells: List<String?> =
        if (isEditMode && pillIds.size < PillCatalog.MAX_PILLS) pillIds + null else pillIds

    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    val hebrewDateFormat = remember {
        android.icu.text.DateFormat.getDateInstance(
            android.icu.text.DateFormat.FULL,
            android.icu.util.ULocale.forLanguageTag("he-IL-u-ca-hebrew")
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance().time
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            currentDate = hebrewDateFormat.format(now)
            delay(1000 * 60)
        }
    }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(150)
            try { initialFocusRequester.requestFocus() } catch (t: Throwable) {
                android.util.Log.w("ControlCenterScreen", "ControlCenterScreen failed", t)
            }
        }
    }

    val scrollState = rememberScrollState()
    val isDarkBackground = wallpaper != null
    val clockColor = if (isDarkBackground) Color.White else Color.Black
    val dateColor = if (isDarkBackground) Color.LightGray else Color.Gray

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(FutureMotion.DurationSlow, easing = FutureMotion.EasingDecelerate)
            ) + fadeIn(animationSpec = tween(FutureMotion.DurationSlow)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(FutureMotion.DurationSlow, easing = FutureMotion.EasingAccelerate)
            ) + fadeOut(animationSpec = tween(FutureMotion.DurationSlow))
        ) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .onKeyEvent { event ->
                        if (event.key == Key.DirectionRight) {
                            if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount > 8) {
                                onSwitchToNotificationCenter()
                                return@onKeyEvent true
                            }
                        }
                        false
                    }
            ) {
                if (wallpaper != null) {
                    Image(
                        bitmap = wallpaper,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(40.dp),
                        contentScale = ContentScale.Crop
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (wallpaper != null) Color.White.copy(alpha = 0.2f)
                            else Color(0xCCFFFFFF)
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(top = com.future.futureui.statusbar.logic.StatusBarLayoutManager.HEIGHT_DP.dp)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = currentTime,
                                fontSize = FutureTypography.display,
                                fontWeight = FontWeight.Bold,
                                color = clockColor,
                                style = androidx.compose.ui.text.TextStyle(
                                    shadow = androidx.compose.ui.graphics.Shadow(color = Color.Black.copy(alpha = 0.3f), blurRadius = 8f)
                                )
                            )
                            Text(
                                text = currentDate,
                                fontSize = FutureTypography.caption,
                                color = dateColor,
                                style = androidx.compose.ui.text.TextStyle(
                                    shadow = androidx.compose.ui.graphics.Shadow(color = Color.Black.copy(alpha = 0.3f), blurRadius = 8f)
                                )
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HeaderActionButton(
                                icon = if (isEditMode) Icons.Rounded.Check else Icons.Rounded.Edit,
                                color = clockColor,
                                onClick = { 
                                    if (isEditMode) {
                                        layoutManager.saveLayout(activeControlIds)
                                        layoutManager.saveSectionOrder(sectionOrder)
                                        layoutManager.savePillIds(pillIds)
                                        movingSectionId = null
                                        isGridEditing = false
                                    }
                                    isEditMode = !isEditMode 
                                }
                            )
                            HeaderActionButton(icon = Icons.Rounded.Settings, color = clockColor, onClick = onSettingsClick)
                            HeaderActionButton(icon = Icons.Rounded.PowerSettingsNew, color = clockColor, onClick = onPowerClick, isPower = true)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    sectionOrder.forEach { sectionId ->
                        if (sectionId == "available") return@forEach
                        
                        val isMoving = movingSectionId == sectionId
                        
                        FocusableSection(
                            id = sectionId,
                            isEditMode = isEditMode,
                            isMoving = isMoving,
                            isGridEditing = if (sectionId == "grid") isGridEditing else false,
                            onLongClick = {
                                movingSectionId = if (isMoving) null else sectionId
                            },
                            onMove = { direction ->
                                val currentIndex = sectionOrder.indexOf(sectionId)
                                val newIndex = (currentIndex + direction).coerceIn(0, sectionOrder.size - 1)
                                if (currentIndex != newIndex) {
                                    val newList = sectionOrder.toMutableList()
                                    Collections.swap(newList, currentIndex, newIndex)
                                    sectionOrder = newList
                                }
                            },
                            onMenuClick = {
                                if (sectionId == "grid") {
                                    isGridEditing = !isGridEditing
                                }
                            }
                        ) {
                            when (sectionId) {
                                "pills" -> {
                                    if (pillCells.isNotEmpty()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            if (isEditMode) {
                                                Text(
                                                    text = "פקדים ${pillIds.size}/${PillCatalog.MAX_PILLS} · OK מסיר · Options מחליף",
                                                    color = clockColor.copy(alpha = 0.7f),
                                                    fontSize = FutureTypography.label,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            pillCells.chunked(2).forEachIndexed { rowIndex, row ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    row.forEachIndexed { colIndex, id ->
                                                        val index = rowIndex * 2 + colIndex
                                                        val requester = if (index == 0) initialFocusRequester else null
                                                        val info = id?.let { PillCatalog.get(it) }
                                                        if (id == null || info == null) {
                                                            TogglePill(
                                                                label = "הוספת פקד",
                                                                icon = Icons.Rounded.Add,
                                                                isOn = false,
                                                                onToggle = {
                                                                    val next = PillCatalog.all.firstOrNull { it.id !in pillIds }
                                                                    if (next != null && pillIds.size < PillCatalog.MAX_PILLS) {
                                                                        pillIds = pillIds + next.id
                                                                    }
                                                                },
                                                                modifier = Modifier.weight(1f),
                                                                focusRequester = requester,
                                                                labelColor = clockColor
                                                            )
                                                        } else {
                                                            TogglePill(
                                                                label = info.label,
                                                                icon = info.icon,
                                                                isOn = info.isToggle && pillManager.isOn(id),
                                                                onToggle = {
                                                                    if (isEditMode) {
                                                                        pillIds = pillIds.filterIndexed { i, _ -> i != index }
                                                                    } else {
                                                                        pillManager.activate(id)
                                                                        if (info.closesPanel) onRequestClose()
                                                                    }
                                                                },
                                                                modifier = Modifier.weight(1f),
                                                                isEditMode = isEditMode,
                                                                onOptionPressed = {
                                                                    // מחליף לפקד הבא בקטלוג שעוד לא מוצג
                                                                    val all = PillCatalog.all
                                                                    val start = all.indexOfFirst { it.id == id }
                                                                    val replacement = (1 until all.size)
                                                                        .map { all[(start + it) % all.size] }
                                                                        .firstOrNull { it.id !in pillIds }
                                                                    if (replacement != null) {
                                                                        pillIds = pillIds.toMutableList().also { it[index] = replacement.id }
                                                                    }
                                                                },
                                                                focusRequester = requester,
                                                                labelColor = clockColor
                                                            )
                                                        }
                                                    }
                                                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                                "grid" -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateContentSize(animationSpec = tween(FutureMotion.DurationSlow))
                                            .clip(FutureShapes.xxl)
                                            .background(Color(0x80E0E0E0))
                                            .border(
                                                width = if (isGridEditing) 2.dp else 0.5.dp, 
                                                color = if (isGridEditing) Color.Red else Color.White.copy(alpha = 0.5f), 
                                                shape = FutureShapes.xxl
                                            )
                                            .onKeyEvent { event ->
                                                if (isEditMode && (event.key == Key.DirectionCenter || event.key == Key.Enter)) {
                                                    if (event.type == KeyEventType.KeyDown) {
                                                        isCenterKeyPressed = true
                                                    } else if (event.type == KeyEventType.KeyUp) {
                                                        isCenterKeyPressed = false
                                                    }
                                                    true
                                                } else false
                                            }
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        if (isGridEditing) {
                                            val availableControls = layoutManager.allAvailableControls.filter { 
                                                it.id !in activeControlIds 
                                            }
                                            if (availableControls.isNotEmpty()) {
                                                Text(
                                                    text = "הוספת כפתורים",
                                                    color = clockColor.copy(alpha = 0.7f),
                                                    fontSize = FutureTypography.label,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(bottom = 8.dp)
                                                )
                                                val rows = (availableControls.size + 4) / 5
                                                repeat(rows) { rowIndex ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                                        horizontalArrangement = Arrangement.SpaceEvenly
                                                    ) {
                                                        repeat(5) { iconIndex ->
                                                            val index = rowIndex * 5 + iconIndex
                                                            if (index < availableControls.size) {
                                                                val control = availableControls[index]
                                                                FocusableIcon(
                                                                    icon = control.icon,
                                                                    label = control.label,
                                                                    isOn = false,
                                                                    onToggle = {
                                                                        activeControlIds = activeControlIds + control.id
                                                                    },
                                                                    showLabel = true,
                                                                    isEditMode = true,
                                                                    isRemove = false,
                                                                    onMenuClick = { isGridEditing = false },
                                                                    labelColor = clockColor
                                                                )
                                                            } else {
                                                                Spacer(modifier = Modifier.width(54.dp))
                                                            }
                                                        }
                                                    }
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .padding(vertical = 12.dp)
                                                        .fillMaxWidth(0.95f)
                                                        .height(1.5.dp)
                                                        .background(Color.White.copy(alpha = 0.6f))
                                                )
                                            }
                                        }

                                        val gridControls = activeControlIds.mapNotNull { id ->
                                            val info = layoutManager.getControlById(id) ?: return@mapNotNull null
                                            object {
                                                val id = id
                                                val label = info.label
                                                val icon = info.icon
                                                val isOn = manager.getControlState(id)
                                                val onToggle = { manager.handleControlToggle(id) }
                                            }
                                        }

                                        val maxRows = if (isExpanded || isEditMode) (gridControls.size + 4) / 5 else 2
                                        repeat(maxRows) { rowIndex ->
                                            AnimatedVisibility(
                                                visible = isExpanded || isEditMode || rowIndex < 2,
                                                enter = expandVertically() + fadeIn(),
                                                exit = shrinkVertically() + fadeOut()
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                                    horizontalArrangement = Arrangement.SpaceEvenly
                                                ) {
                                                    repeat(5) { iconIndex ->
                                                        val index = rowIndex * 5 + iconIndex
                                                        if (index < gridControls.size) {
                                                            val item = gridControls[index]
                                                            FocusableIcon(
                                                                icon = item.icon,
                                                                label = item.label,
                                                                isOn = item.isOn,
                                                                onToggle = {
                                                                    if (isGridEditing) {
                                                                        activeControlIds = activeControlIds.filter { it != item.id }
                                                                    } else {
                                                                        item.onToggle()
                                                                    }
                                                                },
                                                                showLabel = isExpanded || isEditMode,
                                                                isEditMode = isGridEditing,
                                                                isRemove = true,
                                                                onMenuClick = { if (isGridEditing) isGridEditing = false },
                                                                labelColor = clockColor,
                                                                // בלי גלולות, הפוקוס הראשוני נוחת על האייקון הראשון ברשת
                                                                focusRequester = if (pillCells.isEmpty() && index == 0) initialFocusRequester else null
                                                            )
                                                        } else {
                                                            Spacer(modifier = Modifier.width(54.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (!isEditMode) {
                                            val indicatorInteractionSource = remember { MutableInteractionSource() }
                                            val isIndicatorFocused by indicatorInteractionSource.collectIsFocusedAsState()
                                            val indicatorShape = FutureShapes.md
                                            Box(
                                                modifier = Modifier.width(80.dp).height(20.dp).focusEffect(isIndicatorFocused, indicatorShape)
                                                    .clip(indicatorShape)
                                                    .clickable(interactionSource = indicatorInteractionSource, indication = null, onClick = { isExpanded = !isExpanded })
                                                    .focusable(interactionSource = indicatorInteractionSource).padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(modifier = Modifier.width(36.dp).height(4.dp).clip(CircleShape)
                                                    .background(if (isIndicatorFocused) Color(0xFF525252) else Color(0xFFBDBDBD)))
                                            }
                                        }
                                    }
                                }
                                "media" -> {
                                    Box(modifier = Modifier.animateContentSize()) {
                                        MusicPlayerCard(manager = manager, labelColor = clockColor)
                                    }
                                }
                                "sliders" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SliderBar(icon = Icons.Rounded.Brightness6, value = manager.brightnessLevel, onValueChange = { manager.setBrightness(it) }, isDarkBackground = isDarkBackground)
                                        SliderBar(icon = Icons.AutoMirrored.Rounded.VolumeUp, value = manager.volumeLevel, onValueChange = { manager.setVolume(it) }, isDarkBackground = isDarkBackground)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Preview(widthDp = 640, heightDp = 960)
@Composable
fun ControlCenterPreview() {
    ControlCenterScreen(isVisibleByDefault = true)
}
