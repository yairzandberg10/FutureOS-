package com.future.remote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
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
import com.future.remote.data.AcBrand
import com.future.remote.data.AcPresets
import com.future.remote.data.RemoteRepository
import com.future.sharednav.theme.FutureTheme

@Composable
fun AcPresetsScreen(theme: FutureTheme, onBack: () -> Unit, onDeviceCreated: (String) -> Unit) {
    val context = LocalContext.current
    val repository = remember { RemoteRepository(context) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                RemoteHeader(title = "שלט מוכן למזגן", theme = theme, onBack = onBack)

                Text(
                    "כפתורי הפעלה/כיבוי, קירור וחימום בטמפרטורות נפוצות - כבר מוכנים לשידור. " +
                        "בלי גישה למזגן אמיתי לבדיקה, ייתכן שיידרש כיוונון - אם כפתור לא עובד, נסי מקרוב לחיישן של המזגן.",
                    color = theme.textColor.copy(alpha = 0.55f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AcBrand.entries) { brand ->
                        RemoteRow(
                            icon = Icons.Rounded.AcUnit,
                            label = brand.label,
                            subtitle = "מוסיף מכשיר עם כפתורים מוכנים",
                            theme = theme,
                            onClick = {
                                val device = AcPresets.buildDevice(brand, name = brand.label.substringBefore(" ("))
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
