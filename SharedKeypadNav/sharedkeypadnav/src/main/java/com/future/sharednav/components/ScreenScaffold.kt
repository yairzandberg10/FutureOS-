package com.future.sharednav.components
import androidx.compose.ui.focus.FocusRequester

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.future.sharednav.theme.FutureContrast
import com.future.sharednav.theme.FutureType
import com.future.sharednav.theme.LocalFutureAccent
import com.future.sharednav.theme.LocalFutureType
import com.future.sharednav.theme.ThemeClient

/**
 * שלד מסך תקני: רקע מהתמה + כפיית RTL + ScreenTopBar אופציונלי. מחליף את
 * הדפוס שהיה חוזר ידנית בעשרות מסכים:
 * ```
 * CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
 *     Box(Modifier.fillMaxSize().background(theme.backgroundColor)) { ... }
 * }
 * ```
 * כולל את שורת ה-ScreenTopBar אם title לא null.
 */
@Composable
fun ScreenScaffold(
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    title: String? = null,
    textColor: Color = Color.White,
    accentColor: Color = Color.White,
    onBack: (() -> Unit)? = null,
    trailingIcon: ImageVector? = null,
    trailingContentDescription: String? = null,
    onTrailingClick: (() -> Unit)? = null,
    trailingFocusRequester: FocusRequester? = null,
    content: @Composable () -> Unit,
) {
    // קוראים את מכפיל הגופן פעם אחת לכל מסך ומספקים אותו הלאה, כך שכל רכיב
    // משותף בתוך המסך לא ישאל את ה-ContentProvider בנפרד.
    val context = LocalContext.current
    val type = remember(context) { FutureType(ThemeClient.getFontSizeMultiplier(context)) }
    // ההדגשה מתוקנת פעם אחת למסך ולא בכל שורה: ברירת המחדל שלה היא לבן,
    // ובמצב בהיר היא נבלעת במשטח אם מציירים אותה כמו שהיא.
    val accent = remember(accentColor, textColor) {
        FutureContrast.accentForText(accentColor, textColor)
    }
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalFutureType provides type,
        LocalFutureAccent provides accent,
    ) {
        Box(modifier = modifier.fillMaxSize().background(backgroundColor)) {
            if (title != null) {
                Column(modifier = Modifier.fillMaxSize()) {
                    ScreenTopBar(
                        title = title,
                        textColor = textColor,
                        accentColor = accentColor,
                        onBack = onBack,
                        trailingIcon = trailingIcon,
                        trailingContentDescription = trailingContentDescription,
                        onTrailingClick = onTrailingClick,
                        trailingFocusRequester = trailingFocusRequester,
                    )
                    Box(modifier = Modifier.fillMaxSize()) { content() }
                }
            } else {
                content()
            }
        }
    }
}
