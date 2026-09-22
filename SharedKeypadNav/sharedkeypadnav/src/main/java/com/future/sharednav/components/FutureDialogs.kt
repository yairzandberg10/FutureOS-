package com.future.sharednav.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.rememberFutureType

/**
 * המשטח של כל דיאלוג במערכת: משטח מלא, רדיוס 20dp, ריפוד 20dp
 * (components/feedback/ConfirmDialog.jsx ו-InputDialog.jsx).
 *
 * כל אפליקציה בנתה את המשטח הזה בעצמה, ורובן לקחו את הרדיוס של הכרטיס
 * (22dp) במקום של הדיאלוג, וריפוד שנע בין 16 ל-24dp.
 */
@Composable
internal fun DialogSurface(
    surfaceColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(FutureShapes.dialog)
            .background(surfaceColor)
            .padding(DialogPadding),
        content = content,
    )
}

/** 20dp - הריפוד של הדיאלוג (--fos-space-8). */
internal val DialogPadding = 20.dp

/**
 * דיאלוג כללי: כותרת אופציונלית (15sp מודגש, ממורכז), תוכן, ושורת כפתורים.
 * לשאלה הרסנית יש [ConfirmDialog]; לקליטת שם יש [InputDialog]; זה לכל
 * השאר - דיאלוג פרטים, עריכה של כמה שדות, בחירה מרשימה קצרה.
 *
 * התוכן נגלל כשהוא ארוך מהמסך, כך שדיאלוג עם הרבה שדות לא נחתך.
 */
@Composable
fun FutureDialog(
    theme: FutureTheme,
    onDismissRequest: () -> Unit,
    title: String? = null,
    buttons: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val type = rememberFutureType()
    AppDialog(onDismissRequest = onDismissRequest) {
        DialogSurface(theme.surfaceColor) {
            if (title != null) {
                Text(
                    title,
                    color = theme.textColor,
                    fontSize = type.dialog,
                    fontWeight = FutureTypography.weightBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = FutureDimens.spacingLg),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                content = content,
            )
            if (buttons != null) {
                FutureDialogButtons(content = buttons)
            }
        }
    }
}

/** שורת הכפתורים בתחתית דיאלוג - ממורכזת, 12dp בין כפתור לכפתור. */
@Composable
fun FutureDialogButtons(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = FutureDimens.spacingLg),
        horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingMd, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * שורת "תווית / ערך" בדיאלוג פרטים (גודל, נתיב, תאריך שינוי). התווית
 * בשורה העליונה בגודל תווית ו-60%, הערך מתחתיה בגודל הסיכום.
 */
@Composable
fun FutureDetailRow(label: String, value: String, theme: FutureTheme) {
    val type = rememberFutureType()
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = FutureDimens.spacingXs)) {
        Text(label, color = theme.mutedTextColor, fontSize = type.label)
        Text(value, color = theme.textColor, fontSize = type.summary)
    }
}

/**
 * קליטת מחרוזת קצרה אחת - שינוי שם של קובץ, שם לפלייליסט חדש
 * (components/feedback/InputDialog.jsx). אותו משטח של [ConfirmDialog]
 * עם שדה באמצע.
 *
 * השדה מקבל פוקוס עם הפתיחה, כך שאפשר להקליד מיד; חץ למטה עובר לכפתורים.
 * הכותרת היא שם עצם ("שם חדש") ולא שאלה - שאלות שייכות ל-ConfirmDialog.
 */
@Composable
fun InputDialog(
    title: String,
    theme: FutureTheme,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    initialValue: String = "",
    placeholder: String? = null,
    confirmLabel: String = "שמור",
    cancelLabel: String = "ביטול",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    allowBlank: Boolean = false,
) {
    val type = rememberFutureType()
    var text by remember { mutableStateOf(initialValue) }
    val submit = { if (allowBlank || text.isNotBlank()) onConfirm(text.trim()) }

    AppDialog(onDismissRequest = onDismiss) {
        DialogSurface(theme.surfaceColor, modifier = Modifier.escapeTextFieldFocusTrap()) {
            Text(
                title,
                color = theme.textColor,
                fontSize = type.dialog,
                fontWeight = FutureTypography.weightBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = FutureDimens.spacingLg),
            )
            FutureTextField(
                value = text,
                onValueChange = { text = it },
                theme = theme,
                placeholder = placeholder,
                autoFocus = true,
                keyboardOptions = keyboardOptions,
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth(),
            )
            FutureDialogButtons {
                FutureButton(cancelLabel, theme, onDismiss, variant = FutureButtonVariant.Secondary)
                FutureButton(confirmLabel, theme, { submit() })
            }
        }
    }
}
