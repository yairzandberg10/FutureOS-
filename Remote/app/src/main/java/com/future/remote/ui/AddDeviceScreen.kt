package com.future.remote.ui
import com.future.sharednav.focus.bringIntoViewOnFocus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.remote.data.DeviceCategory
import com.future.remote.data.RemoteDevice
import com.future.remote.data.RemoteRepository
import com.future.sharednav.theme.FutureTheme

@Composable
fun AddDeviceScreen(theme: FutureTheme, onBack: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { RemoteRepository(context) }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(DeviceCategory.AC) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                RemoteHeader(title = "מכשיר חדש", theme = theme, onBack = onBack)

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("שם המכשיר", color = theme.textColor.copy(alpha = 0.6f), fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("למשל: מזגן סלון") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = theme.textColor.copy(alpha = 0.08f),
                            unfocusedContainerColor = theme.textColor.copy(alpha = 0.05f),
                            focusedIndicatorColor = theme.accentColor,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = theme.textColor,
                            unfocusedTextColor = theme.textColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("סוג המכשיר", color = theme.textColor.copy(alpha = 0.6f), fontSize = 13.sp, modifier = Modifier.padding(top = 20.dp, bottom = 6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(DeviceCategory.entries) { entry ->
                            CategoryChip(category = entry, selected = entry == category, theme = theme, onClick = { category = entry })
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f))

                Box(modifier = Modifier.padding(16.dp)) {
                    RemoteRow(
                        icon = iconForCategory(category),
                        label = "שמור מכשיר",
                        subtitle = if (name.isBlank()) "יש להזין שם" else name,
                        theme = theme,
                        onClick = {
                            if (name.isNotBlank()) {
                                repository.addDevice(RemoteDevice(name = name.trim(), category = category))
                                onSaved()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(category: DeviceCategory, selected: Boolean, theme: FutureTheme, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    val bg = if (selected) theme.accentColor.copy(alpha = 0.85f) else theme.textColor.copy(alpha = 0.08f)
    val textColor = if (selected) Color.Black else theme.textColor

    Box(
        modifier = Modifier
            .height(40.dp)
            .background(bg, shape)
            .then(if (isFocused) Modifier.border(width = 2.dp, color = theme.accentColor, shape = shape) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
            .padding(horizontal = 16.dp),
    ) {
        Text(category.label, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
    }
}
