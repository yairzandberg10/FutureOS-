package com.future.sharednav.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.future.sharednav.theme.FutureTransitions

/**
 * מעטפת משותפת לכל דיאלוג/תפריט-אפשרויות באפליקציות - קובעת את מיקום,
 * רוחב וגובה החלון החיצוני (הרוחב ברירת המחדל של Dialog לא מבטיח ריווח
 * מהקצוות על מסך צר של 640px), ואת אנימציית הפתיחה.
 *
 * הפתיחה: התוכן מופיע מעט מוקטן ומתייצב ([FutureTransitions.dialogEnter]).
 * התוכן מורכב כבר בפריים הראשון (רק שקוף), ולכן בקשות פוקוס שהדיאלוגים
 * הקיימים עושים עם הפתיחה ממשיכות לעבוד בלי שינוי.
 *
 * אין אנימציית סגירה: הקורא מסיר את הדיאלוג מהעץ ברגע שהמצב שלו משתנה,
 * ולכן אין לו פריים שבו אפשר להנפיש יציאה בלי לשנות כל קריאה בנפרד.
 */
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    widthFraction: Float = 0.85f,
    maxHeightFraction: Float = 0.86f,
    anchorTop: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val visibility = remember { MutableTransitionState(false) }
        visibility.targetState = true
        // תפריט האפשרויות לא ממורכז כמו דיאלוג אלא תלוי 40dp מראש המסך
        // (components/navigation/OptionsMenu.jsx) - כך הוא נפתח תמיד באותו
        // מקום, ולא "קופץ" לגובה אחר לפי מספר השורות שבו.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .then(if (anchorTop) Modifier.padding(top = MenuTopOffset) else Modifier),
            contentAlignment = if (anchorTop) Alignment.TopCenter else Alignment.Center,
        ) {
            val maxContentHeight = maxHeight * maxHeightFraction
            AnimatedVisibility(
                visibleState = visibility,
                enter = FutureTransitions.dialogEnter,
                exit = FutureTransitions.dialogExit,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(widthFraction)
                        .heightIn(max = maxContentHeight),
                    content = content,
                )
            }
        }
    }
}

/** 40dp - המרחק של תפריט האפשרויות מראש המסך (80px ב-OptionsMenu.jsx). */
private val MenuTopOffset = 40.dp
