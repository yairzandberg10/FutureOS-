package com.future.frixa.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.future.frixa.data.RecipeCatalog
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.icons.FutureIcons

/**
 * מסך הבית של Fricassé: הפריקסה בתלת-ממד, והכניסות לשלושת החלקים - מתכונים,
 * כלי ההכנה ומקומות שמוכרים פריקסה (1-3 במקלדת).
 */
@Composable
fun HomeScreen(theme: FutureTheme, onOpenRecipes: () -> Unit, onOpenTool: () -> Unit, onOpenStores: () -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Frixa3DModel(accentColor = theme.accentColor, size = 132.dp)
        Text("Fricassé", color = theme.textColor, fontSize = FutureTypography.headline, fontWeight = FontWeight.ExtraBold)
        Text("פריקסה טוניסאית", color = theme.mutedTextColor, fontSize = FutureTypography.body, modifier = Modifier.padding(bottom = 8.dp))
        FutureListItem(
            title = "מתכונים",
            summary = "${RecipeCatalog.all.size} מתכונים - בצק, מילוי ותוספות",
            theme = theme,
            onClick = onOpenRecipes,
            focusRequester = first,
            modifier = Modifier.fillMaxWidth(),
            leading = { FutureAvatar(theme = theme, icon = FutureIcons.Restaurant) },
        )
        FutureListItem(
            title = "כלי הכנה",
            summary = "כמויות לפי מספר הפריקסה, וטיימרים",
            theme = theme,
            onClick = onOpenTool,
            modifier = Modifier.fillMaxWidth(),
            leading = { FutureAvatar(theme = theme, icon = FutureIcons.Calculate) },
        )
        FutureListItem(
            title = "איפה קונים",
            summary = "מקומות שמוכרים פריקסה, מהקרוב",
            theme = theme,
            onClick = onOpenStores,
            modifier = Modifier.fillMaxWidth(),
            leading = { FutureAvatar(theme = theme, icon = FutureIcons.Storefront) },
        )
    }
}
