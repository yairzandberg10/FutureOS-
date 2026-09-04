package com.future.gallery.ui
import com.future.sharednav.focus.bringIntoViewOnFocus

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.RotateLeft
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.FilterVintage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
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
import androidx.compose.ui.unit.sp
import com.future.gallery.data.CropAspect
import com.future.gallery.data.EditState
import com.future.gallery.data.ImageEditor
import com.future.gallery.data.MediaItem
import com.future.gallery.data.PhotoFilter
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private enum class EditorTool(val label: String, val icon: ImageVector) {
    ROTATE("סיבוב", Icons.Rounded.RotateLeft),
    FLIP("היפוך", Icons.Rounded.Flip),
    CROP("חיתוך", Icons.Rounded.Crop),
    ADJUST("כוונון", Icons.Rounded.Tune),
    FILTER("מסנן", Icons.Rounded.FilterVintage)
}

@Composable
fun PhotoEditorScreen(item: MediaItem, theme: FutureTheme, onBack: () -> Unit, onSaved: (android.net.Uri) -> Unit) {
    val context = LocalContext.current
    var sourceBitmap by remember(item.id) { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var state by remember { mutableStateOf(EditState()) }
    var activeTool by remember { mutableStateOf(EditorTool.ROTATE) }
    var isSaving by remember { mutableStateOf(false) }

    // תמונה מסובבת/הפוכה בלבד (בלי חיתוך/פילטר) - זול לחשב וצריך רק להתעדכן
    // כששינוי גיאומטריה אמיתי קורה, לא בכל נדנוד של מרכז החיתוך. זו הבסיס
    // לתצוגת ה"מסגרת חיתוך חיה" בכלי CROP במקום לרנדר את כל הצינור המלא.
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
    }

    LaunchedEffect(sourceBitmap, state) {
        val src = sourceBitmap ?: return@LaunchedEffect
        // דיבאונס: כל שינוי ב-state (למשל נדנוד חיצי D-pad של מרכז החיתוך תוך
        // החזקת מקש) מבטל ומפעיל מחדש את ה-LaunchedEffect הזה - בלי ה-delay כאן,
        // כל צעד בודד גרר רינדור מלא (חיתוך+ColorMatrix) על התמונה, מה שגרם
        // לתצוגה המקדימה "להיתקע"/לפגר מאחורי הקלט בזמן כוונון מהיר.
        delay(120)
        previewBitmap = withContext(Dispatchers.Default) { ImageEditor.renderFinal(src, state) }
    }

    fun save() {
        val src = sourceBitmap ?: return
        isSaving = true
    }

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
            onSaved(uri)
        } else {
            android.widget.Toast.makeText(context, "לא ניתן לשמור את התמונה", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onKeyEvent { event ->
                    if (activeTool != EditorTool.CROP || event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    val step = 0.05f
                    when (event.key) {
                        Key.DirectionLeft -> { state = state.copy(cropCenterX = (state.cropCenterX - step).coerceIn(0f, 1f)); true }
                        Key.DirectionRight -> { state = state.copy(cropCenterX = (state.cropCenterX + step).coerceIn(0f, 1f)); true }
                        Key.DirectionUp -> { state = state.copy(cropCenterY = (state.cropCenterY - step).coerceIn(0f, 1f)); true }
                        Key.DirectionDown -> { state = state.copy(cropCenterY = (state.cropCenterY + step).coerceIn(0f, 1f)); true }
                        else -> false
                    }
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditorIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "ביטול", theme) { onBack() }
                    Text("עריכת תמונה", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(start = 10.dp))
                    EditorSaveButton(theme = theme, enabled = state.hasEdits && !isSaving, isSaving = isSaving) { save() }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (activeTool == EditorTool.CROP && rotatedBitmap != null) {
                        // בכלי חיתוך: תמונה מסובבת שלמה (חדה, בלי לחתוך אותה בפועל) +
                        // מסגרת חיה שמראה מה בתוך/מחוץ לחיתוך - במקום לרנדר מחדש ולחתוך
                        // את הביטמאפ בפועל על כל תזוזת חץ (שהיה גם איטי וגם לא הראה
                        // שום אינדיקציה של גבול החיתוך, רק את התוצאה הסופית קופצת).
                        CropFocusOverlay(
                            rotatedBitmap = rotatedBitmap!!,
                            state = state,
                            accentColor = theme.accentColor,
                            modifier = Modifier.fillMaxSize().padding(12.dp)
                        )
                    } else {
                        previewBitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    if (sourceBitmap == null) {
                        Text("טוען תמונה...", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                    }
                }

                EditorToolPanel(activeTool, state, theme, onStateChange = { state = it })

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    EditorTool.entries.forEach { tool ->
                        EditorToolTab(tool, isSelected = tool == activeTool, theme = theme) { activeTool = tool }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorToolPanel(tool: EditorTool, state: EditState, theme: FutureTheme, onStateChange: (EditState) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 76.dp)) {
        when (tool) {
            EditorTool.ROTATE -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EditorActionChip("סובב שמאלה", theme) { onStateChange(state.copy(rotationDegrees = ((state.rotationDegrees - 90) % 360 + 360) % 360)) }
                EditorActionChip("סובב ימינה", theme) { onStateChange(state.copy(rotationDegrees = (state.rotationDegrees + 90) % 360)) }
            }
            EditorTool.FLIP -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EditorActionChip("הפוך אופקית", theme) { onStateChange(state.copy(flipHorizontal = !state.flipHorizontal)) }
                EditorActionChip("הפוך אנכית", theme) { onStateChange(state.copy(flipVertical = !state.flipVertical)) }
            }
            EditorTool.CROP -> Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CropAspect.entries.forEach { aspect ->
                        EditorActionChip(aspect.label, theme, isSelected = state.cropAspect == aspect) {
                            onStateChange(state.copy(cropAspect = aspect))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    EditorActionChip("−", theme) { onStateChange(state.copy(cropZoom = (state.cropZoom - 0.1f).coerceAtLeast(1f))) }
                    Text("זום ${"%.1f".format(state.cropZoom)}x · חצים למיקום", color = theme.textColor.copy(alpha = 0.6f), fontSize = 11.sp)
                    EditorActionChip("+", theme) { onStateChange(state.copy(cropZoom = (state.cropZoom + 0.1f).coerceAtMost(3f))) }
                }
            }
            EditorTool.ADJUST -> Column {
                AdjustRow("בהירות", state.brightness, theme) { onStateChange(state.copy(brightness = it)) }
                AdjustRow("ניגודיות", state.contrast, theme) { onStateChange(state.copy(contrast = it)) }
                AdjustRow("רוויה", state.saturation, theme) { onStateChange(state.copy(saturation = it)) }
            }
            EditorTool.FILTER -> LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(PhotoFilter.entries) { filter ->
                    EditorActionChip(filter.label, theme, isSelected = state.filter == filter) {
                        onStateChange(state.copy(filter = filter))
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustRow(label: String, value: Float, theme: FutureTheme, onChange: (Float) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) theme.accentColor.copy(alpha = 0.15f) else Color.Transparent)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
            .onKeyEvent { event ->
                if (!isFocused || event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> { onChange((value - 5f).coerceIn(-100f, 100f)); true }
                    Key.DirectionRight -> { onChange((value + 5f).coerceIn(-100f, 100f)); true }
                    else -> false
                }
            }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, color = theme.textColor, fontSize = 13.sp)
            Text(value.toInt().toString(), color = theme.textColor.copy(alpha = 0.6f), fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(theme.textColor.copy(alpha = 0.15f))) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(((value + 100f) / 200f).coerceIn(0f, 1f))
                    .background(theme.accentColor, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun EditorActionChip(label: String, theme: FutureTheme, isSelected: Boolean = false, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(
        when {
            isFocused -> theme.accentColor
            isSelected -> theme.accentColor.copy(alpha = 0.35f)
            else -> Color.White.copy(alpha = 0.1f)
        },
        label = "chipBg"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(label, color = if (isFocused) Color.Black else theme.textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EditorToolTab(tool: EditorTool, isSelected: Boolean, theme: FutureTheme, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val tint = if (isSelected || isFocused) theme.accentColor else Color.White.copy(alpha = 0.6f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
            .padding(8.dp)
    ) {
        Icon(tool.icon, contentDescription = tool.label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(tool.label, color = tint, fontSize = 10.sp)
    }
}

@Composable
private fun EditorIconButton(icon: ImageVector, contentDescription: String, theme: FutureTheme, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(if (isFocused) theme.accentColor.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f), label = "iconBtnBg")
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus(),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun EditorSaveButton(theme: FutureTheme, enabled: Boolean, isSaving: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(
        when {
            !enabled -> Color.White.copy(alpha = 0.1f)
            isFocused -> theme.accentColor
            else -> theme.accentColor.copy(alpha = 0.6f)
        },
        label = "saveBtnBg"
    )
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick)
            .focusable(interactionSource = interactionSource, enabled = enabled).bringIntoViewOnFocus(),
        contentAlignment = Alignment.Center
    ) {
        if (isSaving) {
            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Black)
        } else {
            Icon(Icons.Rounded.Check, contentDescription = "שמור", tint = if (enabled) Color.Black else Color.White.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * "חלון פוקוס" חי לכלי החיתוך: מציג את התמונה השלמה (לא חתוכה) מטושטשת+מוכהת,
 * עם חלון חד בדיוק במקום/גודל שייחתך בפועל - בלי לרנדר/לחתוך ביטמאפ אמיתי
 * על כל תזוזת חץ (זה מה שגרם לתצוגה הקודמת להיות איטית ובלי שום אינדיקציה
 * ויזואלית של גבול החיתוך). כל מיפוי הקואורדינטות קורה ב-DrawScope (Canvas/
 * drawWithContent) ולא דרך Modifier.offset, כי DrawScope תמיד עובד בקואורדינטות
 * פיקסל מוחלטות ולא מושפע מ-LocalLayoutDirection.Rtl שעוטף את כל המסך.
 */
@Composable
private fun CropFocusOverlay(rotatedBitmap: Bitmap, state: EditState, accentColor: Color, modifier: Modifier = Modifier) {
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
        Image(
            bitmap = bitmapImage,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().blur(16.dp),
            contentScale = ContentScale.Fit
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Color.Black.copy(alpha = 0.4f))
        }
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
                    color = accentColor,
                    topLeft = Offset(screenCropRect.left, screenCropRect.top),
                    size = Size(screenCropRect.width, screenCropRect.height),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}
