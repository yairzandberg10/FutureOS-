package com.future.sharednav.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureElevation
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureAccent
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.rememberFutureType
import kotlinx.coroutines.delay

/**
 * משוב חולף על משהו שכבר קרה - "הועתק", "נשמר" (components/feedback/Snackbar.jsx).
 * מעוגן לתחתית המסך, בניגוד להתראה צפה שהיא אירוע נכנס ומגיעה מלמעלה.
 * משטח כרטיס ברדיוס דיאלוג, הודעה ב-15sp, ותווית פעולה אופציונלית בהדגשה.
 * הוא לא מקבל פוקוס: הוא לא דורש כלום מהמשתמש ונעלם לבד.
 */
@Composable
fun FutureSnackbar(
    message: String,
    theme: FutureTheme,
    modifier: Modifier = Modifier,
    action: String? = null,
) {
    val type = rememberFutureType()
    val accent = LocalFutureAccent.current ?: theme.readableAccentColor
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = FutureDimens.spacingMd, end = FutureDimens.spacingMd, bottom = FutureDimens.spacingMd)
            .shadow(FutureElevation.headsUp, FutureShapes.dialog)
            .clip(FutureShapes.dialog)
            .background(theme.surfaceColor)
            .padding(horizontal = 14.dp, vertical = FutureDimens.spacingMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            message,
            color = theme.textColor,
            fontSize = type.dialog,
            modifier = Modifier.weight(1f),
        )
        if (action != null) {
            Text(action, color = accent, fontSize = type.dialog, fontWeight = FutureTypography.weightBold)
        }
    }
}

/** ההודעה שמוצגת כרגע, אם יש. [show] מחליף הודעה קודמת ומאפס את הטיימר. */
@Stable
class FutureSnackbarState internal constructor() {
    var message by mutableStateOf<String?>(null)
        private set
    internal var serial by mutableIntStateOf(0)
        private set

    fun show(message: String) {
        this.message = message
        serial++
    }

    fun dismiss() {
        message = null
    }
}

@Composable
fun rememberFutureSnackbarState(): FutureSnackbarState = remember { FutureSnackbarState() }

/**
 * מציג את [state] בתחתית ה-Box שמסביב, ומעלים אותו אחרי [SnackbarVisibleMillis].
 * נכנס מלמטה ויוצא בדהייה, בתנועה הרגילה של המערכת.
 */
@Composable
fun BoxScope.FutureSnackbarHost(state: FutureSnackbarState, theme: FutureTheme) {
    val message = state.message
    LaunchedEffect(state.serial) {
        if (state.message != null) {
            delay(SnackbarVisibleMillis)
            state.dismiss()
        }
    }
    // ההודעה האחרונה נשמרת לזמן היציאה, כדי שהטקסט לא ייעלם לפני הרכיב.
    // מחזיק רגיל ולא state - הוא רק זוכר, ואף אחד לא צריך להגיב לשינוי שלו.
    val last = remember { arrayOf("") }
    if (message != null) last[0] = message
    AnimatedVisibility(
        visible = message != null,
        modifier = Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically(FutureMotion.enter()) { it / 2 } + fadeIn(FutureMotion.enter()),
        exit = fadeOut(FutureMotion.exit()),
    ) {
        Box { FutureSnackbar(message ?: last[0], theme) }
    }
}

/** כמה זמן הודעה נשארת על המסך. לא תנועה אלא זמן קריאה של שתיים-שלוש מילים. */
private const val SnackbarVisibleMillis = 1800L
