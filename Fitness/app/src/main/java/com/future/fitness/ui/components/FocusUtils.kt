package com.future.fitness.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager

/** Compose's text fields צורכות DPAD מעלה/מטה פנימית לתזוזת סמן טקסט, אז
 * בלי זה משתמש שנכנס עם החץ למטה לשדה טקסט "נתקע" בו - אין שום דרך להמשיך
 * הלאה עם המקלדת/ה-D-pad הפיזי (המכשיר הזה בלי מסך מגע כלל, ראו
 * MainActivity.dispatchTouchEvent). עוטפים מכל מסך שמכיל שדות טקסט
 * (onPreviewKeyEvent פועל בשלב ה-capture, לפני שהשדה הממוקד מקבל את
 * האירוע) כדי שמעלה/מטה תמיד יעבירו פוקוס הלאה במקום להיבלע. */
fun Modifier.escapeTextFieldFocusTrap(): Modifier = composed {
    val focusManager = LocalFocusManager.current
    onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (event.key) {
            Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Down)
            Key.DirectionUp -> focusManager.moveFocus(FocusDirection.Up)
            else -> false
        }
    }
}
