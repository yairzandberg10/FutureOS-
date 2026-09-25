package com.future.remote.ui

import com.future.sharednav.icons.FutureIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.future.remote.data.AcCompany
import com.future.remote.data.AcState
import com.future.remote.data.DeviceCategory
import com.future.remote.data.IrTransmitter
import com.future.remote.data.RemoteDevice
import com.future.remote.data.RemoteRepository
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography

/**
 * האפליקציה היא שלט למזגן בלבד: המסך הראשי הוא רשימת החברות הגדולות
 * בישראל, וכל חברה היא שלט. בפתיחה הראשונה נוצר לחברה שלט שמור (עם
 * המצב האחרון ודגם השלט שנבחר), ובפעמים הבאות נפתח אותו שלט.
 */
@Composable
fun RemoteHomeScreen(
    theme: FutureTheme,
    refreshKey: Int,
    onOpenRemote: (deviceId: String) -> Unit,
) {
    val context = LocalContext.current
    val repository = remember { RemoteRepository(context) }
    val irTransmitter = remember { IrTransmitter(context) }
    var remotes by remember { mutableStateOf(mapOf<AcCompany, RemoteDevice>()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(refreshKey) {
        remotes = repository.loadDevices()
            .filter { it.acCompany != null && it.acProtocol != null }
            .associateBy { it.acCompany!! }
    }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    fun open(company: AcCompany) {
        val existing = remotes[company]
        if (existing != null) {
            onOpenRemote(existing.id)
            return
        }
        val device = RemoteDevice(
            name = "מזגן ${company.label}",
            category = DeviceCategory.AC,
            acCompany = company,
            acProtocol = company.protocols.first(),
            acState = AcState(),
        )
        repository.addDevice(device)
        onOpenRemote(device.id)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                RemoteHeader(title = "שלט למזגן", theme = theme)

                if (!irTransmitter.isAvailable) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "המכשיר הזה לא כולל משדר אינפרא אדום, אז אי אפשר להשתמש בשלט",
                            color = theme.textColor.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                } else {
                    Text(
                        "בחרו את החברה של המזגן. לא מגיב? בשלט: Options ← דגם שלט אחר.",
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
                            val state = remotes[company]?.acState
                            RemoteRow(
                                icon = FutureIcons.AcUnit,
                                label = company.label,
                                subtitle = when {
                                    state == null -> company.protocols.joinToString(" / ") { it.label }
                                    state.power -> "דלוק · ${state.temp}° · ${state.mode.label}"
                                    else -> "כבוי · ${state.temp}° · ${state.mode.label}"
                                },
                                theme = theme,
                                onClick = { open(company) },
                                focusRequester = if (index == 0) focusRequester else null
                            )
                        }
                    }
                }
            }
        }
    }
}
