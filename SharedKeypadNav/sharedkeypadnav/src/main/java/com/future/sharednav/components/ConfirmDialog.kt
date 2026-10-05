package com.future.sharednav.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.future.sharednav.theme.FutureContrast
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureAccent
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.rememberFutureType

/**
 * דיאלוג אישור אחיד לפעולות הרסניות (מחיקת קובץ/איש קשר/הודעה/התראה).
 * מנווט לגמרי במקלדת: הכפתור הממוקד מסומן גם בצבע וגם בטבעת.
 *
 * הגיאומטריה היא של components/feedback/ConfirmDialog.jsx: ריפוד 20dp,
 * הודעה 15sp מודגשת וממורכזת, 16dp עד הכפתורים ו-12dp ביניהם. הכפתורים
 * הם [FutureButton] עצמו (משני + הרסני) - קודם הם היו כפתור נפרד בצורת
 * גלולה שגדל בפוקוס, כלומר כפתור שלישי שאין לו מקבילה בעיצוב.
 *
 * ביטול בימין (תחילת השורה ב-RTL), הפעולה בשמאל. הפוקוס ההתחלתי על
 * ביטול: OK מקרי לא מוחק כלום.
 */
@Composable
fun ConfirmDialog(
    message: String,
    surfaceColor: Color,
    textColor: Color,
    dangerColor: Color,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    cancelLabel: String = "ביטול",
    confirmLabel: String = "מחק",
) {
    ConfirmDialogContent(
        message = message,
        surfaceColor = surfaceColor,
        textColor = textColor,
        confirmFill = dangerColor,
        onCancel = onCancel,
        onConfirm = onConfirm,
        cancelLabel = cancelLabel,
        confirmLabel = confirmLabel,
    )
}

/**
 * אותו דיאלוג, מתוך ערכה שלמה. [destructive] = false הופך את כפתור הפעולה
 * לראשי (בצבע ההדגשה) - לשאלה שאינה מוחקת דבר ("לצאת מהאימון?").
 */
@Composable
fun ConfirmDialog(
    message: String,
    theme: FutureTheme,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    cancelLabel: String = "ביטול",
    confirmLabel: String = "מחק",
    destructive: Boolean = true,
) {
    val accent = LocalFutureAccent.current ?: theme.readableAccentColor
    ConfirmDialogContent(
        message = message,
        surfaceColor = theme.surfaceColor,
        textColor = theme.textColor,
        confirmFill = if (destructive) theme.dangerColor else accent,
        onCancel = onCancel,
        onConfirm = onConfirm,
        cancelLabel = cancelLabel,
        confirmLabel = confirmLabel,
    )
}

@Composable
private fun ConfirmDialogContent(
    message: String,
    surfaceColor: Color,
    textColor: Color,
    confirmFill: Color,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    cancelLabel: String,
    confirmLabel: String,
) {
    val type = rememberFutureType()
    // קודם לא הוגדר פוקוס התחלתי, וה-OK/החץ הראשון נחת על הכפתור ההרסני.
    // הבקשה רק אחרי שהכפתור נמדד: Dialog רץ בחלון נפרד, ובקשה מוקדמת נבלעת.
    val cancelFocus = remember { FocusRequester() }
    var initialFocusDone by remember { mutableStateOf(false) }
    AppDialog(onDismissRequest = onCancel) {
        DialogSurface(surfaceColor) {
            Text(
                message,
                color = textColor,
                fontWeight = FutureTypography.weightBold,
                fontSize = type.dialog,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = FutureDimens.spacingLg),
                horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingMd, androidx.compose.ui.Alignment.CenterHorizontally),
            ) {
                FutureButtonCore(
                    text = cancelLabel,
                    fill = textColor.copy(alpha = 0.2f),
                    contentColor = textColor,
                    onClick = onCancel,
                    focusRequester = cancelFocus,
                    modifier = Modifier.onGloballyPositioned {
                        if (!initialFocusDone) {
                            initialFocusDone = true
                            runCatching { cancelFocus.requestFocus() }
                        }
                    },
                )
                FutureButtonCore(
                    text = confirmLabel,
                    fill = confirmFill,
                    contentColor = FutureContrast.onColor(confirmFill),
                    onClick = onConfirm,
                )
            }
        }
    }
}
