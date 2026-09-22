package com.future.remote.ui
import com.future.sharednav.theme.subtleTextColor
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.components.FutureChip

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.remote.data.ButtonEncoding
import com.future.remote.data.IrTransmitter
import com.future.remote.data.NecEncoder
import com.future.remote.data.RemoteButton
import com.future.remote.data.RemoteRepository
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.theme.FutureTheme

@Composable
fun AddButtonScreen(theme: FutureTheme, deviceId: String, onBack: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { RemoteRepository(context) }
    val irTransmitter = remember { IrTransmitter(context) }

    var label by remember { mutableStateOf("") }
    var encoding by remember { mutableStateOf(ButtonEncoding.NEC) }
    var necAddress by remember { mutableStateOf("") }
    var necCommand by remember { mutableStateOf("") }
    var rawFrequency by remember { mutableStateOf(NecEncoder.CARRIER_FREQUENCY_HZ.toString()) }
    var rawPattern by remember { mutableStateOf("") }

    fun buildButton(): RemoteButton? {
        if (label.isBlank()) return null
        return when (encoding) {
            ButtonEncoding.NEC -> {
                val address = necAddress.trim().toIntOrNull(16) ?: return null
                val command = necCommand.trim().toIntOrNull(16) ?: return null
                RemoteButton(label = label.trim(), encoding = ButtonEncoding.NEC, necAddress = address, necCommand = command)
            }
            ButtonEncoding.RAW -> {
                val frequency = rawFrequency.trim().toIntOrNull() ?: return null
                if (rawPattern.isBlank()) return null
                RemoteButton(label = label.trim(), encoding = ButtonEncoding.RAW, rawFrequencyHz = frequency, rawPattern = rawPattern.trim())
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.escapeTextFieldFocusTrap().fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                RemoteHeader(title = "כפתור חדש", theme = theme, onBack = onBack)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    FieldLabel("שם הכפתור", theme)
                    RemoteTextField(value = label, onValueChange = { label = it }, placeholder = "למשל: הפעלה/כיבוי", theme = theme)

                    FieldLabel("סוג הקוד", theme, topPadding = 20.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        EncodingChip("NEC (כתובת+פקודה)", encoding == ButtonEncoding.NEC, theme) { encoding = ButtonEncoding.NEC }
                        EncodingChip("תבנית גולמית", encoding == ButtonEncoding.RAW, theme) { encoding = ButtonEncoding.RAW }
                    }

                    if (encoding == ButtonEncoding.NEC) {
                        FieldLabel("כתובת (הקסדצימלי, למשל 20)", theme, topPadding = 16.dp)
                        RemoteTextField(value = necAddress, onValueChange = { necAddress = it }, placeholder = "20", theme = theme, keyboardType = KeyboardType.Text)
                        FieldLabel("פקודה (הקסדצימלי, למשל 02)", theme, topPadding = 12.dp)
                        RemoteTextField(value = necCommand, onValueChange = { necCommand = it }, placeholder = "02", theme = theme, keyboardType = KeyboardType.Text)
                        Text(
                            "הקודים האלה בדרך כלל מתפרסמים באתרי היצרן או בפורומים של שלטים אוניברסליים, לפי דגם המכשיר.",
                            color = theme.textColor.copy(alpha = 0.5f),
                            fontSize = FutureTypography.label,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        FieldLabel("תדר נשא (Hz)", theme, topPadding = 16.dp)
                        RemoteTextField(value = rawFrequency, onValueChange = { rawFrequency = it }, placeholder = "38000", theme = theme, keyboardType = KeyboardType.Number)
                        FieldLabel("תבנית פולסים (מיקרו-שניות, מופרדים בפסיקים)", theme, topPadding = 12.dp)
                        RemoteTextField(value = rawPattern, onValueChange = { rawPattern = it }, placeholder = "9000,4500,560,560,560,1690,...", theme = theme, keyboardType = KeyboardType.Text)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        RemoteRow(
                            icon = Icons.Rounded.PlayArrow,
                            label = "בדיקה",
                            subtitle = "שלח עכשיו",
                            theme = theme,
                            onClick = {
                                val button = buildButton()
                                val transmission = button?.toTransmission()
                                if (transmission == null) {
                                    Toast.makeText(context, "יש להשלים את כל השדות קודם", Toast.LENGTH_SHORT).show()
                                } else {
                                    val sent = irTransmitter.transmit(transmission.first, transmission.second)
                                    Toast.makeText(context, if (sent) "נשלח" else "השליחה נכשלה", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        RemoteRow(
                            icon = Icons.Rounded.Save,
                            label = "שמור",
                            subtitle = "",
                            theme = theme,
                            onClick = {
                                val button = buildButton()
                                if (button == null) {
                                    Toast.makeText(context, "יש להשלים את כל השדות קודם", Toast.LENGTH_SHORT).show()
                                } else {
                                    repository.addButton(deviceId, button)
                                    onSaved()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String, theme: FutureTheme, topPadding: androidx.compose.ui.unit.Dp = 0.dp) {
    Text(text, color = theme.textColor.copy(alpha = 0.6f), fontSize = FutureTypography.summary, modifier = Modifier.padding(top = topPadding, bottom = 6.dp))
}

@Composable
private fun RemoteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    theme: FutureTheme,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    FutureTextField(
        value = value,
        onValueChange = onValueChange,
        theme = theme,
        placeholder = placeholder,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun EncodingChip(text: String, selected: Boolean, theme: FutureTheme, onClick: () -> Unit) {
    FutureChip(text, theme, selected = selected, onClick = onClick)
}
