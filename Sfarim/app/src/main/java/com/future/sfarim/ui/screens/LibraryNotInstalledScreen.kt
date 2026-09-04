package com.future.sfarim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.sfarim.ui.components.FocusableItem
import com.future.sharednav.theme.FutureTheme

/** מוצג כשקובץ sefaria.db לא נמצא בנתיב הצפוי - האפליקציה לא יכולה להוריד
 * ~2GB בעצמה (בלי WiFi, סלולרי בלבד), הקובץ מגיע דרך adb push. */
@Composable
fun LibraryNotInstalledScreen(expectedPath: String, theme: FutureTheme, onRetry: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor).padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, tint = theme.accentColor, modifier = Modifier.padding(bottom = 16.dp))
            Text(
                "ספריית הטקסטים לא הותקנה",
                color = theme.textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(6.dp))
            Text(
                "יש להעביר את הקובץ sefaria.db למכשיר:",
                color = theme.textColor.copy(alpha = 0.6f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .background(theme.textColor.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(expectedPath, color = theme.accentColor, fontSize = 12.sp, textAlign = TextAlign.Center)
            }

            // בלי כפתור ניסיון-חוזר, אחרי adb push של הקובץ תוך כדי שהמסך הזה כבר
            // מוצג המשתמש היה חייב לצאת ולפתוח את האפליקציה מחדש כדי שהבדיקה תרוץ שוב.
            FocusableItem(
                onClick = onRetry,
                theme = theme,
                focusRequester = focusRequester,
                modifier = Modifier.padding(top = 20.dp),
            ) { isFocused ->
                Box(
                    modifier = Modifier
                        .background(theme.accentColor.copy(alpha = if (isFocused) 0.28f else 0.14f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, tint = theme.accentColor)
                        Text("נסה שוב", color = theme.accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
