package com.future.remote.ui

import com.future.remote.data.AcCompany
import com.future.remote.data.AcState
import com.future.remote.data.DeviceCategory
import com.future.remote.data.RemoteDevice
import androidx.compose.foundation.lazy.itemsIndexed
import com.future.sharednav.theme.subtleTextColor

import com.future.sharednav.theme.FutureTypography
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.remote.data.RemoteRepository
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.icons.FutureIcons

@Composable
fun AcPresetsScreen(theme: FutureTheme, onBack: () -> Unit, onDeviceCreated: (String) -> Unit) {
    val context = LocalContext.current
    val repository = remember { RemoteRepository(context) }
    val firstFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                RemoteHeader(title = "שלט מוכן למזגן", theme = theme, onBack = onBack)

                Text(
                    "חמש החברות הגדולות בישראל. בחרו חברה - ייפתח שלט מלא. " +
                        "אם המזגן לא מגיב, בשלט: Options ← דגם שלט אחר.",
                    color = theme.textColor.copy(alpha = 0.55f),
                    fontSize = FutureTypography.summary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(AcCompany.entries) { index, company ->
                        RemoteRow(
                            icon = FutureIcons.AcUnit,
                            label = company.label,
                            subtitle = company.protocols.joinToString(" / ") { it.label },
                            theme = theme,
                            focusRequester = if (index == 0) firstFocus else null,
                            onClick = {
                                val device = RemoteDevice(
                                    name = "מזגן ${company.label}",
                                    category = DeviceCategory.AC,
                                    acCompany = company,
                                    acProtocol = company.protocols.first(),
                                    acState = AcState(),
                                )
                                repository.addDevice(device)
                                onDeviceCreated(device.id)
                            }
                        )
                    }
                }
            }
        }
    }
}
