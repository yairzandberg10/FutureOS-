package com.future.messages.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.future.sharednav.theme.FutureMaterialTheme
import com.future.sharednav.theme.FutureTheme

/** מעטפת דקה סביב FutureMaterialTheme של המודול המשותף (ר' DialerTheme). */
@Composable
fun MessagesTheme(isDarkMode: Boolean = true, accentColor: Color = Color.White, content: @Composable () -> Unit) {
    FutureMaterialTheme(theme = FutureTheme(isDarkMode = isDarkMode, accentColor = accentColor), content = content)
}
