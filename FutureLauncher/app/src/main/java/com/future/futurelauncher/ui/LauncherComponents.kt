package com.future.futurelauncher.ui

import android.appwidget.AppWidgetHost
import android.content.pm.PackageManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import com.future.futurelauncher.ui.theme.FutureTheme

/**
 * מטמון גלובלי לאייקוני אפליקציות - לא תלוי ב-composition של דף ספציפי ב-
 * HorizontalPager. בלי זה, כל פעם שדף יוצא מתחום התצוגה של ה-Pager ונכנס
 * שוב, כל 16 האייקונים שבו נטענים מהדיסק מחדש (loadIcon+toBitmap, פעולה
 * יקרה יחסית), מה שגרם לתחושת "תקיעה" בדיוק ברגע המעבר בין דפים.
 */
private object AppIconCache {
    private val cache = mutableMapOf<String, ImageBitmap>()

    fun get(packageName: String, load: () -> ImageBitmap): ImageBitmap {
        return cache.getOrPut(packageName, load)
    }

    fun evict(packageName: String) {
        cache.remove(packageName)
    }
}

/**
 * מפנה מהמטמון את האייקון של חבילה שהוסרה/הוחלפה, כדי שאייקון "ישן" לא
 * ימשיך להיראות מותקן אחרי שהאפליקציה כבר לא קיימת (ראו fix מס' 4).
 */
fun evictAppIconCache(packageName: String) {
    AppIconCache.evict(packageName)
}

/**
 * מסך הבית תמיד יושב מעל טפט - תמונה שרירותית, לא רקע אחיד של האפליקציה -
 * אז הטקסט והאייקונים שלו חייבים להישאר לבנים קבועים (עם צל לקריאות) בלי
 * קשר למצב כהה/בהיר שנבחר בהגדרות. אחרת בחירת מצב בהיר הופכת את כל הכיתוב
 * למסך הבית לשחור על גבי תמונה - בלתי קריא לגמרי. צבע ההדגשה (theme.accentColor)
 * כן ממשיך לעקוב אחרי הבחירה בהגדרות, כי הוא בחירה מכוונת ולא טקסט רקע.
 */
internal val OnWallpaperColor = Color.White
private val OnWallpaperShadow = Shadow(color = Color.Black.copy(alpha = 0.7f), offset = Offset(1f, 1f), blurRadius = 2f)

@Composable
fun FixedLauncherGrid(
    items: List<LauncherItem>,
    modifier: Modifier = Modifier,
    columns: Int = 4,
    rows: Int = 4,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(0.dp),
    itemContent: @Composable (index: Int, item: LauncherItem) -> Unit
) {
    Layout(
        content = {
            items.forEachIndexed { index, item ->
                if (item is LauncherItem.Empty && item.isOccupiedBy != null) return@forEachIndexed
                itemContent(index, item)
            }
        },
        modifier = modifier
    ) { measurables, constraints ->
        val spacingX = horizontalArrangement.spacing.roundToPx()
        val spacingY = verticalArrangement.spacing.roundToPx()
        
        val paddingTop = contentPadding.calculateTopPadding().roundToPx()
        val paddingStart = contentPadding.calculateStartPadding(layoutDirection).roundToPx()

        val totalWidth = constraints.maxWidth
        val cellWidth = (totalWidth - paddingStart - contentPadding.calculateEndPadding(layoutDirection).roundToPx() - spacingX * (columns - 1)) / columns
        
        // Dynamic cell height based on the items' natural size in the original layout
        val isEditMode = spacingY < 10.dp.roundToPx() // Heuristic to detect edit mode parameters
        val cellHeight = (if (isEditMode) 70.dp else 78.dp).roundToPx()

        var measurableIndex = 0
        val placeables = items.mapIndexedNotNull { index, item ->
            if (item is LauncherItem.Empty && item.isOccupiedBy != null) return@mapIndexedNotNull null

            val spanX = if (item is LauncherItem.Widget) item.spanX else 1
            val spanY = if (item is LauncherItem.Widget) item.spanY else 1

            val itemWidth = cellWidth * spanX + spacingX * (spanX - 1)
            val itemHeight = cellHeight * spanY + spacingY * (spanY - 1)

            val placeable = measurables[measurableIndex++].measure(
                Constraints(maxWidth = itemWidth, maxHeight = itemHeight)
            )

            val row = index / columns
            val col = index % columns
            val x = paddingStart + col * (cellWidth + spacingX)
            val y = paddingTop + row * (cellHeight + spacingY)

            Triple(placeable, x, y)
        }

        layout(totalWidth, constraints.maxHeight) {
            placeables.forEach { (placeable, x, y) ->
                placeable.placeRelative(x, y)
            }
        }
    }
}

@Composable
fun ItemPanel(
    item: LauncherItem,
    pm: PackageManager,
    isEditMode: Boolean,
    isFocused: Boolean,
    isMoving: Boolean = false,
    appWidgetHost: AppWidgetHost? = null,
    theme: FutureTheme = FutureTheme()
) {
    val iconSize = if (isEditMode) 40.dp else 48.dp
    val scale by animateFloatAsState(if (isFocused || isMoving) 1.15f else 1f)
    val borderColor = when {
        isMoving -> Color.Red
        isFocused -> theme.accentColor
        else -> if (item is LauncherItem.Empty && isEditMode) OnWallpaperColor.copy(alpha = 0.05f) else Color.Transparent
    }

    Column(
        modifier = Modifier
            .then(if (item is LauncherItem.Widget) Modifier.fillMaxSize() else Modifier.fillMaxWidth().wrapContentHeight())
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                // תא ריק שמקבל פוקוס חייב להיראות (הנקודה הקטנה), אחרת אין שום
                // אינדיקציה חזותית שהפוקוס בכלל נמצא שם.
                alpha = if (isMoving) 0.7f else if (item is LauncherItem.Empty && !isEditMode && !isFocused) 0f else 1f
            }
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (item is LauncherItem.Empty && isEditMode) 1.dp else 2.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (item is LauncherItem.Widget) {
                        Modifier.weight(1f).fillMaxWidth()
                    } else {
                        Modifier.size(iconSize)
                    }
                )
                .clip(RoundedCornerShape(percent = 28))
                .background(if (item is LauncherItem.Empty) Color.Transparent else OnWallpaperColor.copy(alpha = 0.05f))
        ) {
            when (item) {
                is LauncherItem.App -> {
                    val icon = AppIconCache.get(item.resolveInfo.activityInfo.packageName) {
                        item.resolveInfo.loadIcon(pm).toBitmap().asImageBitmap()
                    }
                    Image(bitmap = icon, contentDescription = null, modifier = Modifier.fillMaxSize())
                }
                is LauncherItem.Folder -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.padding(4.dp).fillMaxSize(),
                        userScrollEnabled = false
                    ) {
                        items(minOf(item.apps.size, 4)) { index ->
                            val app = item.apps[index]
                            val icon = AppIconCache.get(app.resolveInfo.activityInfo.packageName) {
                                app.resolveInfo.loadIcon(pm).toBitmap().asImageBitmap()
                            }
                            Image(
                                bitmap = icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp).clip(RoundedCornerShape(percent = 28))
                            )
                        }
                    }
                }
                is LauncherItem.Widget -> {
                    if (appWidgetHost != null) {
                        AndroidView(
                            factory = { ctx ->
                                appWidgetHost.createView(ctx, item.widgetId, null).apply {
                                    setPadding(0, 0, 0, 0)
                                }
                            },
                            update = { _ -> },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "🧩", fontSize = 24.sp)
                        }
                    }
                }
                is LauncherItem.Empty -> {
                    if (isEditMode) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "+", color = OnWallpaperColor.copy(alpha = 0.2f), fontSize = 20.sp)
                        }
                    } else if (isFocused) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(OnWallpaperColor.copy(alpha = 0.5f)))
                        }
                    }
                }
            }
        }
        if (item !is LauncherItem.Widget || isEditMode) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (item is LauncherItem.Empty) "" else (item.customLabel ?: item.label),
                style = MaterialTheme.typography.labelSmall.copy(shadow = OnWallpaperShadow),
                color = OnWallpaperColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun EditModeButton(label: String, icon: ImageVector, isSelected: Boolean, theme: FutureTheme = FutureTheme(), onClick: () -> Unit) {
    val bgColor by animateColorAsState(if (isSelected) theme.accentColor.copy(alpha = 0.3f) else Color.Transparent)
    val borderAlpha by animateFloatAsState(if (isSelected) 0.6f else 0.05f)
    val scale by animateFloatAsState(if (isSelected) 1.05f else 1f)
    val fgColor = if (isSelected) theme.accentColor else OnWallpaperColor

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(14.dp),
            color = bgColor,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, OnWallpaperColor.copy(alpha = borderAlpha)),
            onClick = onClick
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = fgColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = fgColor,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.labelSmall.copy(
                shadow = Shadow(color = Color.Black, offset = Offset(0.5f, 0.5f), blurRadius = 1f)
            )
        )
    }
}
