package com.future.frixa.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.frixa.data.HighScores
import com.future.frixa.ui.theme.FrixaColors

private data class ArcadeGame(val title: String, val subtitle: String, val icon: ImageVector, val highScore: Int)

@Composable
fun ArcadeHomeScreen(onOpenSnake: () -> Unit, onOpen2048: () -> Unit, focusRequester: FocusRequester? = null) {
    val context = LocalContext.current
    val games = listOf(
        Triple("נחש", "קלאסיקה בלתי מתפשרת - אכול, גדל, אל תפגע בעצמך", onOpenSnake) to HighScores.getSnakeHighScore(context),
        Triple("2048", "מזג אריחים עד שתגיע לשיא החדש שלך", onOpen2048) to HighScores.get2048HighScore(context),
    )

    Column(modifier = Modifier.fillMaxSize().background(FrixaColors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = FrixaColors.primary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("פריקסה", color = FrixaColors.onSurface, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("ארקייד רטרו", color = FrixaColors.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(games) { index, (info, highScore) ->
                val (title, subtitle, onClick) = info
                GameCard(
                    title = title,
                    subtitle = subtitle,
                    highScore = highScore,
                    icon = if (title == "נחש") Icons.Rounded.Bolt else Icons.Rounded.GridView,
                    onClick = onClick,
                    focusRequester = if (index == 0) focusRequester else null,
                )
            }
        }
    }
}

private fun <T> androidx.compose.foundation.lazy.LazyListScope.itemsIndexed(list: List<T>, content: @Composable (Int, T) -> Unit) {
    items(list.size) { i -> content(i, list[i]) }
}

@Composable
private fun GameCard(title: String, subtitle: String, highScore: Int, icon: ImageVector, onClick: () -> Unit, focusRequester: FocusRequester?) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bg by animateColorAsState(if (isFocused) FrixaColors.surfaceContainerHigh else FrixaColors.surfaceContainer, label = "gameCardBg")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.size(52.dp).clip(CircleShape).background(FrixaColors.primary.copy(alpha = if (isFocused) 0.3f else 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = FrixaColors.primary, modifier = Modifier.size(26.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = FrixaColors.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = FrixaColors.onSurfaceVariant, fontSize = 12.sp, maxLines = 2)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("שיא", color = FrixaColors.onSurfaceVariant, fontSize = 10.sp)
            Text("$highScore", color = FrixaColors.tertiary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
