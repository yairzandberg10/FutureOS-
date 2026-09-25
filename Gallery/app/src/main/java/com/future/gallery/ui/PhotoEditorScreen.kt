package com.future.gallery.ui
import com.future.sharednav.systemui.StatusBarInset

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.future.gallery.data.CropAspect
import com.future.gallery.data.EditState
import com.future.gallery.data.ImageEditor
import com.future.gallery.data.MediaItem
import com.future.gallery.data.PhotoFilter
import com.future.gallery.data.StickerKind
import com.future.sharednav.components.FutureChip
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.components.FutureSpinner
import com.future.sharednav.focus.bringIntoViewOnFocus
import com.future.sharednav.focus.focusMotion
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.readableAccentColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private enum class EditorTool(val label: String, val icon: ImageVector) {
    ROTATE("סיבוב", FutureIcons.RotateLeft),
    FLIP("היפוך", FutureIcons.Flip),
    CROP("חיתוך", FutureIcons.Crop),
    ADJUST("כוונון", FutureIcons.Tune),
    FILTER("מסנן", FutureIcons.FilterVintage),
    EFFECTS("אפקטים", FutureIcons.AutoAwesome),
}

private enum class AdjustParam(val label: String) {
    BRIGHTNESS("בהירות"), CONTRAST("ניגודיות"), SATURATION("רוויה"), WARMTH("חום"), VIGNETTE("וינייטה")
}

private fun EditState.valueOf(p: AdjustParam) = when (p) {
    AdjustParam.BRIGHTNESS -> brightness
    AdjustParam.CONTRAST -> contrast
    AdjustParam.SATURATION -> saturation
    AdjustParam.WARMTH -> warmth
    AdjustParam.VIGNETTE -> vignette
}

private fun EditState.with(p: AdjustParam, v: Float) = when (p) {
    AdjustParam.BRIGHTNESS -> copy(brightness = v)
    AdjustParam.CONTRAST -> copy(contrast = v)
    AdjustParam.SATURATION -> copy(saturation = v)
    AdjustParam.WARMTH -> copy(warmth = v)
    AdjustParam.VIGNETTE -> copy(vignette = v.coerceAtLeast(0f))
}

/**
 * עורך התמונות. בתחתית שורת כלים (הכלי נבחר כשהפוקוס עליו), ומעליה
 * האפשרויות של הכלי - שורה נגללת, כך שאף אפשרות לא נחתכת מחוץ למסך.
 *
 * מקשים: החצים לניווט בלבד. בחיתוך ובאפקטים המיקום זז במקשי 2/4/6/8,
 * הגודל ב-1/3 (ו-0 מוחק את המדבקה האחרונה). Options: שמירה, איפוס, ביטול.
 * העורך עוקב אחרי מצב בהיר/כהה של המערכת.
 */
@Composable
fun PhotoEditorScreen(item: MediaItem, theme: FutureTheme, onBack: () -> Unit, onSaved: (android.net.Uri) -> Unit) {
    val context = LocalContext.current
    var sourceBitmap by remember(item.id) { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var state by remember { mutableStateOf(EditState()) }
    var activeTool by remember { mutableStateOf(EditorTool.FILTER) }
    var adjustParam by remember { mutableStateOf(AdjustParam.BRIGHTNESS) }
    var isSaving by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }
    val firstTabFocus = remember { FocusRequester() }

    com.future.sharednav.nav.onOptionsKeyPress { showMenu = !showMenu }

    val rotatedBitmap by produceState<Bitmap?>(initialValue = null, sourceBitmap, state.rotationDegrees, state.flipHorizontal, state.flipVertical) {
        val src = sourceBitmap
        value = if (src == null) null else withContext(Dispatchers.Default) { ImageEditor.applyRotationAndFlip(src, state) }
    }

    LaunchedEffect(item.id) {
        sourceBitmap = withContext(Dispatchers.IO) {
            try {
                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, item.uri)
                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                    val maxDim = 1280
                    val w = info.size.width
                    val h = info.size.height
                    val scale = maxDim.toFloat() / maxOf(w, h)
                    if (scale < 1f) decoder.setTargetSize((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
                }
            } catch (e: Exception) {
                null
            }
        }
        runCatching { firstTabFocus.requestFocus() }
    }

    LaunchedEffect(sourceBitmap, state) {
        val src = sourceBitmap ?: return@LaunchedEffect
        // דיבאונס: מקש מוחזק לא מרנדר כל צעד בנפרד.
        delay(100)
        previewBitmap = withContext(Dispatchers.Default) { ImageEditor.renderFinal(src, state) }
    }
    LaunchedEffect(hint) { if (hint != null) { delay(2200); hint = null } }

    LaunchedEffect(isSaving) {
        if (!isSaving) return@LaunchedEffect
        val src = sourceBitmap
        if (src == null) {
            isSaving = false
            return@LaunchedEffect
        }
        val uri = withContext(Dispatchers.IO) {
            val fullSource = try {
                val full = android.graphics.ImageDecoder.createSource(context.contentResolver, item.uri)
                android.graphics.ImageDecoder.decodeBitmap(full) { decoder, _, _ ->
                    decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } catch (e: Exception) {
                src
            }
            val rendered = ImageEditor.renderFinal(fullSource, state)
            ImageEditor.saveAsNewImage(context, rendered, item.displayName.substringBeforeLast('.').ifBlank { "IMG" })
        }
        isSaving = false
        if (uri != null) {
            android.widget.Toast.makeText(context, "נשמר כעותק חדש", android.widget.Toast.LENGTH_SHORT).show()
            onSaved(uri)
        } else {
            android.widget.Toast.makeText(context, "לא ניתן לשמור את התמונה", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun addStickers(kind: StickerKind) {
        val src = sourceBitmap ?: return
        val base = ImageEditor.renderFinal(src, state.copy(stickers = emptyList(), vignette = 0f))
        val (placed, found) = ImageEditor.placeStickers(base, kind)
        state = state.copy(stickers = state.stickers + placed)
        hint = if (found) "זוהו ${placed.size} פנים" else "לא זוהו פנים - 2/4/6/8 להזזה, 1/3 לגודל"
    }

    // מקשי המספרים: הזזה/גודל בחיתוך ובאפקטים. החצים נשארים לניווט בלבד.
    fun onDigit(key: Key): Boolean {
        val step = 0.03f
        when (activeTool) {
            EditorTool.CROP -> {
                state = when (key) {
                    Key.Two -> state.copy(cropCenterY = (state.cropCenterY - step).coerceIn(0f, 1f))
                    Key.Eight -> state.copy(cropCenterY = (state.cropCenterY + step).coerceIn(0f, 1f))
                    Key.Four -> state.copy(cropCenterX = (state.cropCenterX - step).coerceIn(0f, 1f))
                    Key.Six -> state.copy(cropCenterX = (state.cropCenterX + step).coerceIn(0f, 1f))
                    Key.One -> state.copy(cropZoom = (state.cropZoom - 0.1f).coerceAtLeast(1f))
                    Key.Three -> state.copy(cropZoom = (state.cropZoom + 0.1f).coerceAtMost(4f))
                    else -> return false
                }
                return true
            }
            EditorTool.EFFECTS -> {
                val last = state.stickers.lastOrNull() ?: return false
                val moved = when (key) {
                    Key.Two -> last.copy(cy = (last.cy - step).coerceIn(0f, 1f))
                    Key.Eight -> last.copy(cy = (last.cy + step).coerceIn(0f, 1f))
                    Key.Four -> last.copy(cx = (last.cx - step).coerceIn(0f, 1f))
                    Key.Six -> last.copy(cx = (last.cx + step).coerceIn(0f, 1f))
                    Key.One -> last.copy(size = (last.size * 0.9f).coerceAtLeast(0.03f))
                    Key.Three -> last.copy(size = (last.size * 1.1f).coerceAtMost(0.6f))
                    Key.Zero -> null
                    else -> return false
                }
                state = state.copy(stickers = state.stickers.dropLast(1) + listOfNotNull(moved))
                return true
            }
            else -> return false
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.backgroundColor)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    onDigit(event.key)
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = StatusBarInset.TITLE_GAP_DP.dp).padding(horizontal = FutureDimens.spacingLg, vertical = FutureDimens.spacingSm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("עריכת תמונה", color = theme.textColor, fontWeight = FontWeight.Bold, fontSize = FutureTypography.screenTitle, modifier = Modifier.weight(1f))
                    if (isSaving) {
                        FutureSpinner(theme = theme, size = 24.dp)
                    } else if (state.hasEdits) {
                        Text("Options לשמירה", color = theme.mutedTextColor, fontSize = FutureTypography.caption)
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (activeTool == EditorTool.CROP && rotatedBitmap != null) {
                        CropFocusOverlay(
                            rotatedBitmap = rotatedBitmap!!,
                            state = state,
                            frameColor = theme.readableAccentColor,
                            modifier = Modifier.fillMaxSize().padding(10.dp)
                        )
                    } else {
                        previewBitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().padding(10.dp).clip(FutureShapes.sm),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    if (sourceBitmap == null) {
                        FutureSpinner(theme = theme, label = "טוען תמונה")
                    }
                    hint?.let {
                        Text(
                            it,
                            color = theme.textColor,
                            fontSize = FutureTypography.caption,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 14.dp)
                                .clip(FutureShapes.pill)
                                .background(theme.surfaceColor.copy(alpha = 0.92f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                EditorToolPanel(
                    tool = activeTool,
                    state = state,
                    adjustParam = adjustParam,
                    theme = theme,
                    onAdjustParam = { adjustParam = it },
                    onStateChange = { state = it },
                    onAddSticker = { addStickers(it) },
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(EditorTool.entries) { tool ->
                        EditorToolTab(
                            tool,
                            isSelected = tool == activeTool,
                            theme = theme,
                            focusRequester = if (tool == EditorTool.entries.first()) firstTabFocus else null,
                            onFocus = { activeTool = tool },
                        )
                    }
                }
            }

            if (showMenu) {
                FutureOptionsMenu(theme = theme, onDismissRequest = { showMenu = false }, header = "עריכה") {
                    FutureMenuRow("שמור כעותק חדש", FutureIcons.Check, theme, onClick = {
                        showMenu = false
                        if (state.hasEdits && !isSaving) isSaving = true
                    })
                    FutureMenuRow("איפוס כל השינויים", FutureIcons.RestartAlt, theme, onClick = {
                        showMenu = false
                        state = EditState()
                    })
                    if (state.stickers.isNotEmpty()) {
                        FutureMenuRow("הסר את כל המדבקות", FutureIcons.Delete, theme, onClick = {
                            showMenu = false
                            state = state.copy(stickers = emptyList())
                        })
                    }
                    FutureMenuRow("יציאה בלי לשמור", FutureIcons.Close, theme, destructive = true, onClick = {
                        showMenu = false
                        onBack()
                    })
                }
            }
        }
    }
}

@Composable
private fun EditorToolPanel(
    tool: EditorTool,
    state: EditState,
    adjustParam: AdjustParam,
    theme: FutureTheme,
    onAdjustParam: (AdjustParam) -> Unit,
    onStateChange: (EditState) -> Unit,
    onAddSticker: (StickerKind) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp), verticalArrangement = Arrangement.Center) {
        when (tool) {
            EditorTool.ROTATE -> ChipRow {
                item { FutureChip("סובב שמאלה", theme, onClick = { onStateChange(state.copy(rotationDegrees = ((state.rotationDegrees - 90) % 360 + 360) % 360)) }) }
                item { FutureChip("סובב ימינה", theme, onClick = { onStateChange(state.copy(rotationDegrees = (state.rotationDegrees + 90) % 360)) }) }
                item { FutureChip("180°", theme, onClick = { onStateChange(state.copy(rotationDegrees = (state.rotationDegrees + 180) % 360)) }) }
            }
            EditorTool.FLIP -> ChipRow {
                item { FutureChip("הפוך אופקית", theme, selected = state.flipHorizontal, onClick = { onStateChange(state.copy(flipHorizontal = !state.flipHorizontal)) }) }
                item { FutureChip("הפוך אנכית", theme, selected = state.flipVertical, onClick = { onStateChange(state.copy(flipVertical = !state.flipVertical)) }) }
            }
            EditorTool.CROP -> {
                ChipRow {
                    items(CropAspect.entries) { aspect ->
                        FutureChip(aspect.label, theme, selected = state.cropAspect == aspect, onClick = { onStateChange(state.copy(cropAspect = aspect)) })
                    }
                }
                PanelHint("זום ${"%.1f".format(state.cropZoom)}x · 2/4/6/8 הזזה · 1/3 זום", theme)
            }
            EditorTool.ADJUST -> {
                ChipRow {
                    items(AdjustParam.entries) { p ->
                        FutureChip(p.label, theme, selected = p == adjustParam, onClick = { onAdjustParam(p) })
                    }
                }
                AdjustSlider(adjustParam, state.valueOf(adjustParam), theme) { onStateChange(state.with(adjustParam, it)) }
            }
            EditorTool.FILTER -> ChipRow {
                items(PhotoFilter.entries) { filter ->
                    FutureChip(filter.label, theme, selected = state.filter == filter, onClick = { onStateChange(state.copy(filter = filter)) })
                }
            }
            EditorTool.EFFECTS -> {
                ChipRow {
                    items(StickerKind.entries) { kind ->
                        FutureChip(kind.label, theme, onClick = { onAddSticker(kind) })
                    }
                }
                PanelHint(
                    if (state.stickers.isEmpty()) "OK מוסיף על הפנים שבתמונה"
                    else "2/4/6/8 הזזה · 1/3 גודל · 0 מחיקה",
                    theme
                )
            }
        }
    }
}

@Composable
private fun ChipRow(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun PanelHint(text: String, theme: FutureTheme) {
    Text(
        text,
        color = theme.mutedTextColor,
        fontSize = FutureTypography.caption,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

/**
 * מחוון כוונון. ב-RTL הפס מתמלא מימין לשמאל, ולכן חץ שמאל מגביר וחץ ימין
 * מחליש - כיוון החץ הוא כיוון התנועה של המילוי (קודם זה היה הפוך).
 */
@Composable
private fun AdjustSlider(param: AdjustParam, value: Float, theme: FutureTheme, onChange: (Float) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val min = if (param == AdjustParam.VIGNETTE) 0f else -100f
    val fraction by animateFloatAsState(((value - min) / (100f - min)).coerceIn(0f, 1f), FutureMotion.fast(), label = "adjust")
    val shape = FutureShapes.md
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(shape)
            .background(if (isFocused) theme.surfaceColor else Color.Transparent)
            .then(if (isFocused) Modifier.border(FutureDimens.focusBorderItem, theme.readableAccentColor, shape) else Modifier)
            .onKeyEvent { event ->
                if (!isFocused || event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> { onChange((value + 5f).coerceIn(min, 100f)); true }
                    Key.DirectionRight -> { onChange((value - 5f).coerceIn(min, 100f)); true }
                    Key.Zero -> { onChange(0f); true }
                    else -> false
                }
            }
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(param.label, color = theme.textColor, fontSize = FutureTypography.summary)
            Text(value.toInt().toString(), color = theme.mutedTextColor, fontSize = FutureTypography.label)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(FutureShapes.xs).background(theme.textColor.copy(alpha = 0.15f))) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(theme.readableAccentColor, FutureShapes.xs)
            )
        }
    }
}

/** לשונית כלי: הכלי נבחר כשהפוקוס מגיע אליו; נבחר = בהדגשה, ממוקד = מסגרת. */
@Composable
private fun EditorToolTab(
    tool: EditorTool,
    isSelected: Boolean,
    theme: FutureTheme,
    focusRequester: FocusRequester?,
    onFocus: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    LaunchedEffect(isFocused) { if (isFocused) onFocus() }
    val tint = if (isSelected) theme.readableAccentColor else theme.mutedTextColor
    val shape = FutureShapes.md
    val bgColor by animateColorAsState(if (isFocused) theme.surfaceColor else Color.Transparent, FutureMotion.focusColorSpec, label = "toolTabBg")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .focusMotion(interactionSource)
            .clip(shape)
            .background(bgColor)
            .then(if (isFocused) Modifier.border(FutureDimens.focusBorderItem, theme.readableAccentColor, shape) else Modifier)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onFocus)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
            .padding(vertical = FutureDimens.spacingSm)
    ) {
        Icon(tool.icon, contentDescription = tool.label, tint = tint, modifier = Modifier.size(FutureDimens.iconSettingRow))
        Spacer(modifier = Modifier.height(FutureDimens.spacingXxs))
        Text(tool.label, color = tint, fontSize = FutureTypography.caption, maxLines = 1)
    }
}

/**
 * "חלון" חיתוך חי: התמונה השלמה מוכהית, ורק החלק שייחתך נשאר חד וממוסגר.
 * המיפוי נעשה ב-DrawScope (פיקסלים מוחלטים, לא מושפע מ-RTL).
 */
@Composable
private fun CropFocusOverlay(rotatedBitmap: Bitmap, state: EditState, frameColor: Color, modifier: Modifier = Modifier) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val bitmapImage = remember(rotatedBitmap) { rotatedBitmap.asImageBitmap() }
    val cropRectPx = remember(rotatedBitmap, state.cropAspect, state.cropCenterX, state.cropCenterY, state.cropZoom) {
        ImageEditor.cropRect(rotatedBitmap, state)
    }

    val screenCropRect = remember(boxSize, rotatedBitmap, cropRectPx) {
        if (boxSize.width == 0 || boxSize.height == 0) {
            Rect.Zero
        } else {
            val bitmapAspect = rotatedBitmap.width.toFloat() / rotatedBitmap.height.toFloat()
            val boxAspect = boxSize.width.toFloat() / boxSize.height.toFloat()
            val renderedW: Float
            val renderedH: Float
            if (bitmapAspect > boxAspect) {
                renderedW = boxSize.width.toFloat()
                renderedH = boxSize.width.toFloat() / bitmapAspect
            } else {
                renderedW = boxSize.height.toFloat() * bitmapAspect
                renderedH = boxSize.height.toFloat()
            }
            val offsetX = (boxSize.width - renderedW) / 2f
            val offsetY = (boxSize.height - renderedH) / 2f
            val scaleToScreen = renderedW / rotatedBitmap.width.toFloat()
            Rect(
                left = offsetX + cropRectPx.left * scaleToScreen,
                top = offsetY + cropRectPx.top * scaleToScreen,
                right = offsetX + cropRectPx.right * scaleToScreen,
                bottom = offsetY + cropRectPx.bottom * scaleToScreen
            )
        }
    }

    Box(modifier = modifier.onSizeChanged { boxSize = it }, contentAlignment = Alignment.Center) {
        Image(bitmap = bitmapImage, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        Canvas(modifier = Modifier.fillMaxSize()) { drawRect(Color.Black.copy(alpha = 0.60f)) }
        if (screenCropRect != Rect.Zero) {
            Image(
                bitmap = bitmapImage,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        clipRect(screenCropRect.left, screenCropRect.top, screenCropRect.right, screenCropRect.bottom) {
                            this@drawWithContent.drawContent()
                        }
                    },
                contentScale = ContentScale.Fit
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    color = frameColor,
                    topLeft = Offset(screenCropRect.left, screenCropRect.top),
                    size = Size(screenCropRect.width, screenCropRect.height),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}
