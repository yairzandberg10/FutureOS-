package com.future.dialer.ui.contacts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.future.dialer.R
import com.future.sharednav.focus.FocusableItem

@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel,
    onCall: (String, String) -> Unit
) {
    val contactList by viewModel.contacts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val focusManager = LocalFocusManager.current
    val searchFocusRequester = remember { FocusRequester() }

    // פוקוס D-pad התחלתי על שדה החיפוש - בלי זה נחיתה על טאב אנשי הקשר משאירה
    // את המסך בלי שום פריט מודגש (בניגוד ל-InCallScreen שכן ממקד אוטומטית).
    LaunchedEffect(Unit) { searchFocusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize()) {
        // צבעים מותאמים לזכוכית הכהה של המערכת (מילוי שקוף בגוון ההדגשה + בורדר
        // בצבע ההדגשה) במקום ברירת המחדל האפורה/כחולה של OutlinedTextField -
        // אותה שפה בדיוק כמו תצוגת המספר המוקש ב-DialpadScreen.
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .focusRequester(searchFocusRequester)
                // בשדה טקסט, Compose "בולע" את מקש למטה פנימית ולא מזיז פוקוס -
                // מכשיר עם מקלדת בלבד (בלי מגע) היה נשאר תקוע בשדה החיפוש בלי
                // דרך לרדת לרשימת אנשי הקשר. מיירטים את המקש כאן ומזיזים פוקוס ידנית.
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionDown) {
                        focusManager.moveFocus(FocusDirection.Down)
                        true
                    } else false
                },
            placeholder = { Text(stringResource(R.string.search_contacts)) },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                unfocusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            ),
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(contactList, key = { it.id }) { contact ->
                FocusableItem(onClick = { onCall(contact.name, contact.phoneNumber) }, accentColor = MaterialTheme.colorScheme.primary) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = contact.name.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contact.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = contact.phoneNumber,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        FavoriteStarButton(isFavorite = contact.isFavorite) { viewModel.toggleFavorite(contact) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteStarButton(isFavorite: Boolean, onToggle: () -> Unit) {
    FocusableItem(onClick = onToggle, accentColor = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = if (isFavorite) "הסר ממועדפים" else "הוסף למועדפים",
                tint = if (isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}
