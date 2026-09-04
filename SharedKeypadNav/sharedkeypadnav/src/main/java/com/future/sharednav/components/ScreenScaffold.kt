package com.future.sharednav.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

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
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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
                    )
                    Box(modifier = Modifier.fillMaxSize()) { content() }
                }
            } else {
                content()
            }
        }
    }
}
