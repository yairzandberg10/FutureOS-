package com.future.futureui.controlcenter.ui
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.scrimColor
import com.future.sharednav.theme.textAlpha
import com.future.sharednav.theme.FutureDimens

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.futureui.controlcenter.ui.components.focusEffect
import kotlinx.coroutines.delay

/**
 * תפריט כיבוי בעיצוב FutureUI, מוצג מעל Control Center. קיים כי הדיאלוג
 * המקורי של אנדרואיד (GLOBAL_ACTION_POWER_DIALOG) לא בנוי לניווט בשלט/מקלדת
 * T9 בלבד ולא תואם ויזואלית לשאר המערכת. כיבוי/הפעלה-מחדש בפועל דורשים
 * הרשאת root (כמו שאר הפעולות ב-ControlManager) כי לאפליקציה רגילה אין
 * גישה ציבורית לפעולות האלה.
 */
@Composable
fun PowerMenuScreen(
    onPowerOff: () -> Unit,
    onRestart: () -> Unit,
    onCancel: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(100)
        try { focusRequester.requestFocus() } catch (t: Throwable) {
            android.util.Log.w("PowerMenuScreen", "PowerMenuScreen failed", t)
        }
    }

    val theme = LocalFutureTheme.current
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        // תפריט האפשרויות של הדיזיין סיסטם (OptionsMenu.jsx): הכהיה של 60%
        // מאחור, משטח אטום ברדיוס 20dp, כותרת 12sp ב-50%, שורות 50dp עם מילוי
        // פוקוס של 12% בלבד, והשורה ההרסנית באדום.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.scrimColor),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(FutureShapes.dialog)
                    .background(theme.surfaceColor)
                    .padding(vertical = FutureDimens.spacingSm)
            ) {
                Text(
                    text = "אפשרויות כיבוי",
                    color = theme.textAlpha(50),
                    fontSize = FutureTypography.label,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = FutureDimens.spacingSm)
                )
                FutureMenuRow("כיבוי", Icons.Rounded.PowerSettingsNew, theme, onPowerOff, destructive = true, focusRequester = focusRequester)
                FutureMenuRow("הפעלה מחדש", Icons.Rounded.RestartAlt, theme, onRestart)
                FutureMenuRow("ביטול", Icons.Rounded.Close, theme, onCancel)
            }
        }
    }
}
