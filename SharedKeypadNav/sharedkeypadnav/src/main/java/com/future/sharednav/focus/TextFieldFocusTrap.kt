package com.future.sharednav.focus

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager

/**
 * שדות טקסט של Compose צורכות DPAD מעלה/מטה פנימית לתזוזת סמן טקסט, אז
 * בלי זה משתמש שנכנס עם החץ למטה לשדה טקסט "נתקע" בו - אין שום דרך להמשיך
 * הלאה עם המקלדת/ה-D-pad הפיזי (המכשיר הזה בלי מסך מגע כלל, ראו
 * MainActivity.dispatchTouchEvent). עוטפים מכל מסך שמכיל שדות טקסט
 * (onPreviewKeyEvent פועל בשלב ה-capture, לפני שהשדה הממוקד מקבל את
 * האירוע) כדי שמעלה/מטה תמיד יעבירו פוקוס הלאה במקום להיבלע.
 *
 * המימוש נכתב במקור כקובץ מקומי ב-Fitness (ui/components/FocusUtils.kt)
 * והועלה לכאן כי אותה מלכודת פוקוס בדיוק קיימת בכל אפליקציה עם שדה טקסט -
 * Tasks, notes, Tools, Remote, Files, Contact, Calendar, Gallery, Messages,
 * Music ו-Terminal. מחילים על ה-Modifier של המסך (ה-root composable), לא על
 * השדה עצמו: בשלב ה-capture האירוע מגיע קודם להורה.
 */
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
