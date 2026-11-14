package com.future.sfarim.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme

/** אייקון מוביל לשורת רשימה בלי עיגול האווטאר - הגליף לבד, בצבע המבטא. */
@Composable
fun RowIcon(icon: ImageVector, theme: FutureTheme) {
    Icon(icon, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(FutureDimens.iconSettingRow))
}
