package com.future.futureui.settings.ui
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureButtonVariant
import androidx.compose.runtime.ReadOnlyComposable

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
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
            modifier = Modifier.fillMaxSize().background(shellTheme.backgroundColor).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = when (stage) {
                    Stage.ConfirmExisting -> "הזן את קוד הנעילה הנוכחי"
                    Stage.EnterNew -> "בחר קוד נעילה חדש (4 ספרות)"
                    Stage.ConfirmNew -> "הזן שוב לאישור"
                },
                color = shellTheme.textColor,
                fontSize = FutureTypography.title,
                fontWeight = FontWeight.Bold
            )
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMessage!!, color = shellTheme.dangerColor, fontSize = FutureTypography.summary)
            }
            Spacer(modifier = Modifier.height(24.dp))

            // שדה הקלט של הדיזיין סיסטם, מוסתר כסיסמה (היה OutlinedTextField בתכלת
            // קבוע #64D2FF - צבע הדגשה שלא נבחר ע"י המשתמש).
            FutureTextField(
                value = input,
                onValueChange = { new ->
                    if (new.length <= PIN_LENGTH && new.all { it.isDigit() }) {
                        input = new
                        if (new.length == PIN_LENGTH) submit()
                    }
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                theme = shellTheme,
                autoFocus = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
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
            modifier = Modifier.fillMaxSize().background(shellTheme.backgroundColor).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("לבטל את קוד הנעילה?", color = shellTheme.textColor, fontSize = FutureTypography.title, fontWeight = FontWeight.Bold)
            Text("מסך הנעילה יפתח בלחיצת OK בלבד, בלי קוד.", color = shellTheme.textColor.copy(alpha = 0.6f), fontSize = FutureTypography.summary)
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
    FutureButton(
        text,
        shellTheme,
        onClick,
        variant = if (isDestructive) FutureButtonVariant.Destructive else FutureButtonVariant.Secondary,
    )
}

/** הערכה הפעילה - מסכי המעטפת עוקבים אחרי מצב כהה/בהיר וצבע ההדגשה, כמו כל אפליקציה. */
private val shellTheme: com.future.sharednav.theme.FutureTheme
    @Composable @ReadOnlyComposable get() = com.future.sharednav.theme.LocalFutureTheme.current
