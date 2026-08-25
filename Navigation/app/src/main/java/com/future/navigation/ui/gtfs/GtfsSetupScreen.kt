package com.future.navigation.ui.gtfs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.future.navigation.R
import com.future.navigation.data.gtfs.ImportPhase
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.focus.FocusableItem

@Composable
fun GtfsSetupScreen(viewModel: GtfsSetupViewModel, onBack: () -> Unit) {
    val progress by viewModel.progress.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = stringResource(R.string.gtfs_setup_title),
            textColor = MaterialTheme.colorScheme.onBackground,
            accentColor = MaterialTheme.colorScheme.primary,
            onBack = onBack
        )

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                stringResource(R.string.gtfs_wifi_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            val phase = progress?.phase
            val label = when (phase) {
                ImportPhase.DOWNLOADING -> stringResource(R.string.gtfs_downloading)
                ImportPhase.PARSING_STOPS, ImportPhase.PARSING_STOP_TIMES, ImportPhase.PARSING_TRIPS, ImportPhase.PARSING_ROUTES, ImportPhase.PARSING_CALENDAR -> stringResource(R.string.gtfs_parsing)
                ImportPhase.DONE -> stringResource(R.string.gtfs_done)
                ImportPhase.ERROR -> progress?.error ?: "שגיאה"
                null -> null
            }

            if (label != null) {
                Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress?.fraction ?: 0f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            FocusableItem(
                onClick = viewModel::startImport,
                accentColor = MaterialTheme.colorScheme.primary,
                idleBackgroundColor = MaterialTheme.colorScheme.primary,
                focusedBackgroundColor = MaterialTheme.colorScheme.primary,
                cornerRadius = 16.dp,
                scaleOnFocus = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.gtfs_update_button),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
