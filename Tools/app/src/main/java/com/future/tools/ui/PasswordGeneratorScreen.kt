package com.future.tools.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.tools.ui.theme.FutureTheme
import java.security.SecureRandom

private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
private const val DIGITS = "0123456789"
private const val SYMBOLS = "!@#\$%^&*()-_=+?"

private fun generatePassword(length: Int, useUpper: Boolean, useDigits: Boolean, useSymbols: Boolean): String {
    var pool = LOWER
    if (useUpper) pool += UPPER
    if (useDigits) pool += DIGITS
    if (useSymbols) pool += SYMBOLS
    val random = SecureRandom()
    return (1..length).map { pool[random.nextInt(pool.length)] }.joinToString("")
}

private fun passwordStrength(length: Int, useUpper: Boolean, useDigits: Boolean, useSymbols: Boolean): Pair<String, Color> {
    val variety = 1 + listOf(useUpper, useDigits, useSymbols).count { it }
    val score = length * variety
    return when {
        score < 40 -> "חלשה" to Color(0xFFFF6B6B)
        score < 70 -> "בינונית" to Color(0xFFFFD60A)
        else -> "חזקה" to Color(0xFF32D74B)
    }
}

@Composable
fun PasswordGeneratorScreen(theme: FutureTheme, onBack: () -> Unit) {
    val context = LocalContext.current
    var length by remember { mutableIntStateOf(12) }
    var useUpper by remember { mutableStateOf(true) }
    var useDigits by remember { mutableStateOf(true) }
    var useSymbols by remember { mutableStateOf(true) }
    var password by remember { mutableStateOf(generatePassword(12, true, true, true)) }

    fun regenerate() {
        password = generatePassword(length, useUpper, useDigits, useSymbols)
    }

    val (strengthLabel, strengthColor) = passwordStrength(length, useUpper, useDigits, useSymbols)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(title = "מחולל סיסמאות", theme = theme, onBack = onBack)

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        password,
                        color = theme.textColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(theme.textColor.copy(alpha = 0.06f))
                            .padding(16.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("חוזק: $strengthLabel", color = strengthColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PwActionButton(Icons.Rounded.Refresh, "חדש", theme = theme, modifier = Modifier.weight(1f)) { regenerate() }
                    PwActionButton(Icons.Rounded.ContentCopy, "העתק", theme = theme, modifier = Modifier.weight(1f)) {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        clipboard.setPrimaryClip(ClipData.newPlainText("password", password))
                        Toast.makeText(context, "הסיסמה הועתקה", Toast.LENGTH_SHORT).show()
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("אורך", color = theme.textColor.copy(alpha = 0.5f), fontSize = 13.sp, modifier = Modifier.weight(1f))
                    ToolsStepperButton("-", theme = theme) { length = (length - 1).coerceAtLeast(4); regenerate() }
                    Text("$length", color = theme.textColor, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    ToolsStepperButton("+", theme = theme) { length = (length + 1).coerceAtMost(32); regenerate() }
                }

                Spacer(modifier = Modifier.height(12.dp))

                PwToggleRow("אותיות גדולות (A-Z)", useUpper, theme = theme) { useUpper = it; regenerate() }
                PwToggleRow("ספרות (0-9)", useDigits, theme = theme) { useDigits = it; regenerate() }
                PwToggleRow("סימנים מיוחדים (!@#\$...)", useSymbols, theme = theme) { useSymbols = it; regenerate() }
            }
        }
    }
}

@Composable
private fun PwActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, theme: FutureTheme, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    val bgColor = if (isFocused) theme.accentColor else theme.textColor.copy(alpha = 0.08f)
    Row(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (isFocused) Color.Black else theme.accentColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = if (isFocused) Color.Black else theme.textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PwToggleRow(label: String, checked: Boolean, theme: FutureTheme, onToggle: (Boolean) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(shape)
            .background(if (isFocused) theme.textColor.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null) { onToggle(!checked) }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = theme.textColor, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) theme.accentColor else theme.textColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) Text("✓", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
