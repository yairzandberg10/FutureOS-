package com.future.futureui.statusbar.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.future.futureui.ui.theme.ShellGlass
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.textAlpha

/**
 * חלונית הווליום - גלולת זכוכית שיורדת משורת המצב: אייקון, פס דק שמתמלא
 * בתנועה, והאחוז. בשפת המעטפת (ShellGlass) - שקופה, ובלי צבע ההדגשה.
 * [visible] false מנגן את היציאה לפני שהשירות מסיר את החלון.
 */
@Composable
fun VolumeOverlay(level: Float, visible: Boolean = true, modifier: Modifier = Modifier) {
    val theme = LocalFutureTheme.current
    val shown = remember { MutableTransitionState(false) }
    LaunchedEffect(visible) { shown.targetState = visible }
    val animatedLevel by animateFloatAsState(level.coerceIn(0f, 1f), FutureMotion.fast(), label = "volumeLevel")
    val muted = level <= 0f

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visibleState = shown,
            enter = slideInVertically(FutureMotion.enter()) { -it } + fadeIn(FutureMotion.enter()) + scaleIn(FutureMotion.enter(), initialScale = 0.9f),
            exit = slideOutVertically(FutureMotion.exit()) { -it / 2 } + fadeOut(FutureMotion.exit()),
        ) {
            Row(
                modifier = Modifier
                    .padding(top = 40.dp)
                    .width(232.dp)
                    .height(52.dp)
                    .clip(FutureShapes.pill)
                    .background(ShellGlass.panel(theme))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = if (muted) FutureIcons.AutoMirrored.VolumeOff else FutureIcons.AutoMirrored.VolumeUp,
                    contentDescription = null,
                    tint = ShellGlass.ink(theme),
                    modifier = Modifier.size(22.dp),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(FutureShapes.pill)
                        .background(theme.textAlpha(18)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedLevel.coerceAtLeast(0.001f))
                            .clip(FutureShapes.pill)
                            .background(ShellGlass.on(theme)),
                    )
                }
                Text(
                    "${(level * 100).toInt()}",
                    color = ShellGlass.inkMuted(theme),
                    fontSize = FutureTypography.summary,
                    fontFamily = FutureTypography.monoFamily,
                    modifier = Modifier.width(28.dp),
                )
            }
        }
    }
}
