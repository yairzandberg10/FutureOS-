package com.future.frixa.ui

import com.future.sharednav.theme.FutureTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.frixa.data.RecipeCatalog
import com.future.frixa.data.StoreCatalog
import com.future.sharednav.theme.FutureTheme

@Composable
fun HomeScreen(theme: FutureTheme) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Frixa3DModel(accentColor = theme.accentColor)
        Spacer(modifier = Modifier.height(20.dp))
        Text("פריקסה", color = theme.textColor, fontSize = FutureTypography.headline, fontWeight = FontWeight.ExtraBold)
        Text(
            "המטבח שלכם, במרחק לחיצה",
            color = theme.textColor.copy(alpha = 0.6f),
            fontSize = FutureTypography.body,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(modifier = Modifier.height(28.dp))
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${RecipeCatalog.all.size} מתכונים מוכנים לבישול",
                color = theme.accentColor,
                fontSize = FutureTypography.body,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${StoreCatalog.all.size} חנויות באזור שלכם",
                color = theme.accentColor,
                fontSize = FutureTypography.body,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
