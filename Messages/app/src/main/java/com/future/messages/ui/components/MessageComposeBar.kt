package com.future.messages.ui.components

import com.future.sharednav.icons.FutureIcons
import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.future.sharednav.focus.bringIntoViewOnFocus
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureAccent
import com.future.sharednav.theme.FutureContrast
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.rememberFutureType
import com.future.sharednav.theme.secondaryTextColor
import com.future.sharednav.theme.subtleTextColor

/**
 * שורת הכתיבה בתחתית מסך הודעה - התבנית "Message compose bar" של
 * הדיזיין סיסטם (templates/message-compose). מלמעלה למטה:
 *
 *  - שורת הנמען: "אל" ב-40%, והשם ב-70% במשקל בינוני (או "בחר נמען").
 *  - שדה בצורת גלולה על משטח, 48dp לפחות, ובתוכו כפתור צירוף, הטיוטה
 *    (15sp, עד שלוש שורות) וכפתור הכתבה. כשהשדה בפוקוס - מסגרת בהדגשה
 *    והילה של 22% מההדגשה סביבה.
 *  - לידו כפתור שליחה עגול, 48dp, מלא בהדגשה. בלי טקסט ובלי צירוף הוא
 *    לא מקבל פוקוס - אין מה לשלוח.
 *
 * [onDictate] null - אין במכשיר מנוע הכתבה, והמיקרופון לא מוצג בכלל
 * (כפתור שלא עושה כלום גרוע מכפתור חסר).
 */
@Composable
fun MessageComposeBar(
    theme: FutureTheme,
    recipient: String?,
    text: String,
    onTextChange: (String) -> Unit,
    canSend: Boolean,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onDictate: (() -> Unit)?,
    fieldFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val type = rememberFutureType()
    val accent = LocalFutureAccent.current ?: theme.readableAccentColor

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(theme.backgroundColor)
            .padding(start = FutureDimens.spacingMd, end = FutureDimens.spacingMd, top = 6.dp, bottom = 14.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(start = 6.dp, end = 6.dp, bottom = 5.dp)
                .heightIn(min = 19.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("אל", color = theme.subtleTextColor, fontSize = type.label)
            if (recipient != null) {
                Text(
                    recipient,
                    color = theme.secondaryTextColor,
                    fontSize = type.label,
                    fontWeight = FutureTypography.weightMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text("בחר נמען", color = theme.subtleTextColor, fontSize = type.label)
            }
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
        ) {
            val fieldInteraction = remember { MutableInteractionSource() }
            val fieldFocused by fieldInteraction.collectIsFocusedAsState()
            val border by animateColorAsState(
                if (fieldFocused) accent else Color.Transparent,
                FutureMotion.focusColorSpec,
                label = "composeFieldBorder",
            )
            val halo by animateColorAsState(
                if (fieldFocused) accent.copy(alpha = 0.22f) else Color.Transparent,
                FutureMotion.focusColorSpec,
                label = "composeFieldHalo",
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = ComposeFieldMinHeight)
                    // ההילה מצוירת לפני ה-clip, ולכן היא יוצאת 2dp מחוץ לגלולה.
                    .drawBehind {
                        val spread = HaloSpread.toPx()
                        drawRoundRect(
                            color = halo,
                            topLeft = Offset(-spread, -spread),
                            size = Size(size.width + spread * 2, size.height + spread * 2),
                            cornerRadius = CornerRadius(ComposeFieldRadius.toPx() + spread),
                        )
                    }
                    .clip(ComposeFieldShape)
                    .background(theme.surfaceColor)
                    .border(1.dp, border, ComposeFieldShape)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ComposeIconButton(FutureIcons.Add, "צרף תמונה", theme, onAttach)
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 11.dp)
                        .focusRequester(fieldFocusRequester)
                        .bringIntoViewOnFocus(),
                    textStyle = TextStyle(color = theme.textColor, fontSize = type.dialog, lineHeight = 1.35.em),
                    cursorBrush = SolidColor(accent),
                    maxLines = 3,
                    interactionSource = fieldInteraction,
                    decorationBox = { inner ->
                        Box {
                            if (text.isEmpty()) {
                                Text("הודעה", color = theme.subtleTextColor, fontSize = type.dialog, maxLines = 1)
                            }
                            inner()
                        }
                    },
                )
                if (onDictate != null) {
                    ComposeIconButton(FutureIcons.Mic, "הכתבה", theme, onDictate)
                }
            }

            ComposeSendButton(theme = theme, accent = accent, enabled = canSend, onClick = onSend)
        }
    }
}

/** כפתור בתוך הגלולה - עיגול 28dp בלי רקע, אייקון 20dp ב-40%, וטבעת 1.5dp בהדגשה בפוקוס. */
@Composable
private fun ComposeIconButton(icon: ImageVector, contentDescription: String, theme: FutureTheme, onClick: () -> Unit) {
    val accent = LocalFutureAccent.current ?: theme.readableAccentColor
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val ring by animateColorAsState(
        if (isFocused) accent else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "composeIconRing",
    )
    Box(
        modifier = Modifier
            .size(ComposeIconButtonSize)
            .clip(CircleShape)
            .border(FutureDimens.focusBorderItem, ring, CircleShape)
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = theme.subtleTextColor, modifier = Modifier.size(20.dp))
    }
}

/**
 * שליחה - עיגול 48dp בהדגשה, והאייקון בדיו שמתאים לה. בפוקוס: פס של צבע
 * הרקע ואחריו טבעת 2dp בהדגשה, כך שהטבעת נפרדת מהמילוי גם כשהם באותו צבע.
 */
@Composable
private fun ComposeSendButton(theme: FutureTheme, accent: Color, enabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val ring by animateColorAsState(
        if (isFocused) accent else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "composeSendRing",
    )
    Box(
        modifier = Modifier
            .size(SendButtonSize)
            // הטבעת מצוירת מחוץ לעיגול ולא מגדילה אותו - כך הכפתור נשאר
            // מיושר לתחתית הגלולה גם כשהוא בפוקוס.
            .drawBehind {
                val stroke = SendRingWidth.toPx()
                drawCircle(
                    color = ring,
                    radius = size.minDimension / 2 + SendRingGap.toPx() + stroke / 2,
                    style = Stroke(stroke),
                )
            }
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(accent)
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            FutureIcons.AutoMirrored.Send,
            contentDescription = "שלח",
            tint = FutureContrast.onColor(accent),
            modifier = Modifier.size(23.dp),
        )
    }
}

/** 96px / 48px / 56px / 4px - מהתבנית (min-height, border-radius, כפתורי הגלולה, box-shadow). */
private val ComposeFieldMinHeight: Dp = 48.dp
private val ComposeFieldRadius: Dp = 24.dp
private val ComposeFieldShape = RoundedCornerShape(ComposeFieldRadius)
private val HaloSpread: Dp = 2.dp
private val ComposeIconButtonSize: Dp = 28.dp
private val SendButtonSize: Dp = 48.dp

/** box-shadow של 4px ברקע ו-8px בהדגשה - רווח של 2dp ואחריו טבעת של 2dp. */
private val SendRingGap: Dp = 2.dp
private val SendRingWidth: Dp = 2.dp
