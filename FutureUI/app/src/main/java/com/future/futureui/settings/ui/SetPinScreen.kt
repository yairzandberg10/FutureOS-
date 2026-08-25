package com.future.futureui.settings.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.futureui.lockscreen.logic.LockScreenLayoutManager

private val AccentColor = Color(0xFF64D2FF)
private const val PIN_LENGTH = 4

/**
 * מסך הגדרת/שינוי קוד נעילה - מבקש להזין קוד בן 4 ספרות פעמיים (הזנה +
 * אישור), ושומר רק hash שלו (ר' LockScreenLayoutManager). מיושם כ-Activity
 * רגילה (לא overlay), אז אפשר להשתמש בשדה טקסט רגיל עם מקלדת מספרים.
 */
@Composable
fun SetPinScreen(onDone: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val layoutManager = remember { LockScreenLayoutManager(context) }

    var stage by remember { mutableStateOf(if (layoutManager.hasPin()) Stage.ConfirmExisting else Stage.EnterNew) }
    var firstEntry by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun submit() {
        errorMessage = null
        when (stage) {
            Stage.ConfirmExisting -> {
                if (layoutManager.verifyPin(input)) {
                    stage = Stage.EnterNew
                    input = ""
                } else {
                    errorMessage = "קוד שגוי"
                    input = ""
                }
            }
            Stage.EnterNew -> {
                if (input.length != PIN_LENGTH) return
                firstEntry = input
                input = ""
                stage = Stage.ConfirmNew
            }
            Stage.ConfirmNew -> {
                if (input == firstEntry) {
                    layoutManager.setPin(input)
                    onDone()
                } else {
                    errorMessage = "הקודים לא תואמים, נסה שוב"
                    input = ""
                    stage = Stage.EnterNew
                    firstEntry = ""
                }
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = when (stage) {
                    Stage.ConfirmExisting -> "הזן את קוד הנעילה הנוכחי"
                    Stage.EnterNew -> "בחר קוד נעילה חדש (4 ספרות)"
                    Stage.ConfirmNew -> "הזן שוב לאישור"
                },
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMessage!!, color = Color(0xFFFF6B6B), fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = input,
                onValueChange = { new ->
                    if (new.length <= PIN_LENGTH && new.all { it.isDigit() }) {
                        input = new
                        if (new.length == PIN_LENGTH) submit()
                    }
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = AccentColor,
                    cursorColor = AccentColor
                ),
                singleLine = true,
                modifier = Modifier.width(160.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
            FocusableTextButton("ביטול", onCancel)
        }
    }
}

/** מבטל קוד נעילה קיים לגמרי. */
@Composable
fun RemovePinConfirmScreen(onConfirm: () -> Unit, onCancel: () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("לבטל את קוד הנעילה?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("מסך הנעילה יפתח בלחיצת OK בלבד, בלי קוד.", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FocusableTextButton("ביטול", onCancel)
                FocusableTextButton("הסר קוד", onConfirm, isDestructive = true)
            }
        }
    }
}

private enum class Stage { ConfirmExisting, EnterNew, ConfirmNew }

@Composable
private fun FocusableTextButton(text: String, onClick: () -> Unit, isDestructive: Boolean = false) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val base = if (isDestructive) Color(0xFFFF6B6B) else AccentColor
    val bgColor by animateColorAsState(if (isFocused) base else base.copy(alpha = 0.7f), label = "btnBg")
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(text, color = Color.Black, fontWeight = FontWeight.Bold)
    }
}
