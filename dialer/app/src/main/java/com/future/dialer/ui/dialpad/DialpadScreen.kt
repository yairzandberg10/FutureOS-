package com.future.dialer.ui.dialpad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.dialer.ui.CallsViewModel
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.t9.T9DigitMap
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureAccent
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.calcButtonColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.rememberFutureType
import com.future.sharednav.theme.subtleTextColor
import com.future.sharednav.theme.textAlpha

/**
 * טאב המקלדת (ui_kits/calls): המספר בגדול למעלה, ומתחתיו השם של איש הקשר
 * שהמספר שייך לו; רשת 3×4 של המקשים עם האותיות; וכפתור "התקשר" ראשי.
 *
 * המקשים שעל המסך הם הד של המקשים הפיזיים ולא יעד פוקוס - הספרות מגיעות
 * מהמקלדת של המכשיר (MainActivity.onKeyDown). הפוקוס היחיד הוא הכפתור, והוא
 * מקבל אותו ברגע שיש מספר לחייג.
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
                .padding(start = FutureDimens.spacingLg, end = FutureDimens.spacingLg, bottom = FutureDimens.spacingMd),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        ) {
            // מספרים לא מתהפכים בתוך ממשק RTL (README של המערכת).
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    digits.ifEmpty { "הקלד מספר" },
                    color = if (digits.isEmpty()) theme.textAlpha(30) else theme.textColor,
                    fontSize = if (digits.length > 12) type.headline else type.display,
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

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = FutureDimens.spacingXl),
            verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm, Alignment.CenterVertically),
        ) {
            Keys.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm)) {
                    row.forEach { key ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(KeyHeight)
                                .clip(FutureShapes.lg)
                                .background(theme.calcButtonColor),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
                        ) {
                            Text(
                                key.toString(),
                                color = theme.textColor,
                                fontSize = type.screenTitle,
                                fontWeight = FutureTypography.weightMedium,
                            )
                            val letters = lettersOf(key)
                            if (letters.isNotEmpty()) {
                                Text(
                                    letters,
                                    color = theme.subtleTextColor,
                                    fontSize = type.badge,
                                    letterSpacing = 0.5.sp,
                                )
                            }
                        }
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

private val Keys = listOf('1', '2', '3', '4', '5', '6', '7', '8', '9', '*', '0', '#')

/** האותיות מתחת לספרה, כמו על המקשים עצמם; ו-"+" מתחת ל-0. */
private fun lettersOf(key: Char): String = when (key) {
    '0' -> "+"
    in '2'..'9' -> T9DigitMap.ENGLISH[key].orEmpty().uppercase()
    else -> ""
}

/** 58dp - מקש (116px בערכה). */
private val KeyHeight = 58.dp

/** 64dp - המקום של המספר והשם מעל המקשים (128px בערכה). */
private val DisplayMinHeight = 64.dp
