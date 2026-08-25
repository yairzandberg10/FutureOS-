package com.future.dialer.ui.dialpad

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallMade
import androidx.compose.material.icons.rounded.CallMissed
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.dialer.R
import com.future.dialer.data.model.CallRecord
import com.future.dialer.data.model.CallType
import com.future.dialer.data.model.Contact
import com.future.sharednav.focus.FocusableItem

/**
 * מסך חיוג + היסטוריה מאוחד: תצוגת המספר המוקש תמיד למעלה, ומתחתיה רשימה אחת
 * שמתחלפת בהתאם להקשה - שיחות אחרונות כשהשדה ריק, אנשי קשר תואמים (T9) כשמתחילים
 * להקיש. כך אין צורך במעבר בין טאבים נפרדים במכשיר בלי מסך מגע.
 */
@Composable
fun DialpadScreen(
    viewModel: DialpadViewModel,
    onCall: (String, String) -> Unit
) {
    val dialedNumber by viewModel.dialedNumber.collectAsState()
    val suggestions by viewModel.suggestedContacts.collectAsState()
    val recentCalls by viewModel.recentCalls.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // תצוגת המספר המוקש
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = if (dialedNumber.isEmpty()) 0.06f else 0.12f),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (dialedNumber.isEmpty()) 0.25f else 0.5f))
        ) {
            Box(
                modifier = Modifier.padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dialedNumber.ifEmpty { stringResource(R.string.enter_number) },
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (dialedNumber.isEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }

        if (dialedNumber.isEmpty()) {
            Text(
                text = stringResource(R.string.recent_calls),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            )
            if (recentCalls.isEmpty()) {
                EmptyState(stringResource(R.string.no_recent_calls), modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(recentCalls, key = { it.id }) { record ->
                        CallHistoryItem(record, onCall)
                    }
                }
            }
        } else {
            Text(
                text = stringResource(R.string.matching_contacts),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            )
            if (suggestions.isEmpty()) {
                EmptyState(stringResource(R.string.no_matching_contacts), modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(suggestions, key = { it.id }) { contact ->
                        ContactSuggestionItem(contact, onCall)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun ContactSuggestionItem(contact: Contact, onCall: (String, String) -> Unit) {
    FocusableItem(onClick = { onCall(contact.name, contact.phoneNumber) }, accentColor = MaterialTheme.colorScheme.primary) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        contact.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    contact.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    contact.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun CallHistoryItem(record: CallRecord, onCall: (String, String) -> Unit) {
    FocusableItem(onClick = { onCall(record.name ?: record.phoneNumber, record.phoneNumber) }, accentColor = MaterialTheme.colorScheme.primary) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (icon, color) = when (record.type) {
                CallType.INCOMING -> Icons.Rounded.CallReceived to Color(0xFF4CAF50)
                CallType.OUTGOING -> Icons.Rounded.CallMade to Color(0xFF2196F3)
                CallType.MISSED -> Icons.Rounded.CallMissed to Color(0xFFF44336)
                CallType.REJECTED -> Icons.Rounded.CallMissed to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            }

            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.14f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.name ?: record.phoneNumber,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = record.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Text(
                text = DateUtils.getRelativeTimeSpanString(record.timestamp).toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
        }
    }
}
