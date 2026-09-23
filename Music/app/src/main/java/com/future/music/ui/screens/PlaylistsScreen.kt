package com.future.music.ui.screens
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.theme.subtleTextColor
import com.future.sharednav.theme.rememberFutureType
import com.future.sharednav.theme.FutureDimens

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.music.data.Playlist
import com.future.music.ui.components.NameInputDialog
import com.future.music.ui.components.ScreenTopBar
import com.future.sharednav.theme.FutureTheme

@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    theme: FutureTheme,
    onBack: () -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    onCreatePlaylist: (String) -> Unit,
) {
    val type = rememberFutureType()
    var showCreateDialog by remember { mutableStateOf(false) }
    val firstItemFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstItemFocusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = "פלייליסטים",
            theme = theme,
            onBack = onBack,
            trailingIcon = Icons.Rounded.Add,
            trailingContentDescription = "פלייליסט חדש",
            onTrailingClick = { showCreateDialog = true },
        )

        if (playlists.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("אין עדיין פלייליסטים - לחצו + כדי ליצור", color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.body)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = FutureDimens.screenPadding),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(FutureDimens.itemSpacing),
            ) {
                itemsIndexed(playlists, key = { _, playlist -> playlist.id }) { index, playlist ->
                    FutureListItem(
                        title = playlist.name,
                        theme = theme,
                        onClick = { onOpenPlaylist(playlist) },
                        focusRequester = if (index == 0) firstItemFocusRequester else null,
                        leading = { FutureAvatar(theme = theme, icon = Icons.AutoMirrored.Rounded.QueueMusic) },
                        trailing = { Text("${playlist.songIds.size} שירים", color = theme.subtleTextColor, fontSize = type.summary) },
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        NameInputDialog(
            title = "פלייליסט חדש",
            theme = theme,
            confirmLabel = "צור",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                onCreatePlaylist(name)
                showCreateDialog = false
            },
        )
    }
}
