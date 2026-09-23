package com.future.dialer.ui.dialpad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.dialer.ui.CallsViewModel
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.LocalFutureAccent
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.rememberFutureType
import com.future.sharednav.theme.subtleTextColor
import com.future.sharednav.theme.textAlpha

/**
 * טאב המקלדת: המספר בגדול, השם של איש הקשר שהמספר שייך לו, אנשי קשר
 * שמתאימים למה שהוקלד (בספרות או ב-T9), וכפתור "התקשר".
 *
 * אין רשת מקשים על המסך - היא רק שכפלה את המקלדת הפיזית ולקחה חצי מסך.
 * הספרות מגיעות מהמקשים (MainActivity.onKeyDown), BACK מוחק ספרה, וכשהשדה
 * ריק BACK חוזר ליומן.
 */
@Composable
fun DialpadScreen(
    viewModel: CallsViewModel,
    onCall: (number: String) -> Unit,
) {
    val theme = LocalFutureTheme.current
    val type = rememberFutureType()
    val accent = LocalFutureAccent.current ?: theme.readableAccentColor
    val digits by viewModel.dialedNumber.collectAsState()
    val match by viewModel.dialMatch.collectAsState()
    val suggestions by viewModel.dialSuggestions.collectAsState()

    val callButton = remember { FocusRequester() }
    LaunchedEffect(digits.isNotEmpty()) {
        if (digits.isNotEmpty()) runCatching { callButton.requestFocus() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(title = "מקלדת", textColor = theme.textColor, accentColor = theme.accentColor)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = DisplayMinHeight)
                .padding(horizontal = FutureDimens.spacingLg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        ) {
            // מספרים לא מתהפכים בתוך ממשק RTL.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    digits.ifEmpty { "הקלד מספר" },
                    color = if (digits.isEmpty()) theme.textAlpha(30) else theme.textColor,
                    fontSize = when {
                        digits.isEmpty() -> type.headline
                        digits.length > 12 -> type.headline
                        else -> type.display
                    },
                    fontWeight = FontWeight.Light,
                    letterSpacing = if (digits.isEmpty()) 0.sp else 1.sp,
                    maxLines = 1,
                )
            }
            Text(
                match?.name.orEmpty(),
                color = accent,
                fontSize = type.body,
                maxLines = 1,
                modifier = Modifier.heightIn(min = 17.dp),
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (suggestions.isNotEmpty()) {
                LazyColumn(contentPadding = PaddingValues(horizontal = FutureDimens.spacingMd)) {
                    items(suggestions, key = { it.id }) { contact ->
                        FutureListItem(
                            title = contact.name,
                            summary = contact.phoneNumber,
                            theme = theme,
                            onClick = { onCall(contact.phoneNumber) },
                            leading = { FutureAvatar(theme = theme, name = contact.name, photoUri = contact.photoUri) },
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = FutureDimens.spacingXl, end = FutureDimens.spacingXl, top = FutureDimens.spacingMd, bottom = FutureDimens.spacingLg),
        ) {
            FutureButton(
                text = "התקשר",
                theme = theme,
                onClick = { if (digits.isNotEmpty()) onCall(digits) },
                fillMaxWidth = true,
                focusRequester = callButton,
                enabled = digits.isNotEmpty(),
            )
        }
    }
}

/** 72dp - המקום של המספר והשם. */
private val DisplayMinHeight = 72.dp
