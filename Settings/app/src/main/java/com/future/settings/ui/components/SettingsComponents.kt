package com.future.settings.ui.components

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.components.FutureSwitch
import com.future.sharednav.components.FutureDivider
import com.future.sharednav.components.FutureCard
import com.future.sharednav.components.cardRowFocus
import com.future.sharednav.components.LocalFutureCard
import com.future.sharednav.theme.readableAccentColor

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.settings.ui.theme.ThemeConfig

@Composable
fun SettingItem(
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,
    theme: ThemeConfig,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = FutureShapes.lg
    val inCard = LocalFutureCard.current != null
    // שורות מידע-בלבד (onClick == null) לא מקבלות אף אחד מהאפקטים של פוקוס/לחיצה
    // למטה - בלעדי זה שורה שלחיצה עליה היא no-op הייתה מקבלת בדיוק אותה הדגשת
    // פוקוס מלאה כמו פריט לחיץ אמיתי, ומטעה את המשתמש לחשוב שיש לה פעולה.
    val isInteractive = onClick != null
    // שורה רגילה שקופה ויושבת ישירות על הכרטיס, ורק קו מפריד דק מפריד בינה
    // לבין הבאה אחריה (ר' העיצוב). קודם היה הפוך - לכל שורה היה מילוי אפור
    // משלה, מה שהפך כרטיס אחד עם שלוש שורות לשלוש גלולות נפרדות שנראות כמו
    // שלושה כרטיסים, והקווים המפרידים נבלעו ביניהן. הפוקוס הוא זה שמוסיף
    // מילוי עדין ומסגרת, בלי הגדלה - עקבי עם הפוקוס בשאר המערכת.
    val bgColor = animateColorAsState(
        if (isInteractive && isFocused) theme.textColor.copy(alpha = 0.06f) else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "settingItemBg"
    )

    // הפוקוס ממלא את כל רוחב הכרטיס ולוקח את הפינות שלו בשורה הראשונה
    // והאחרונה (cardRowFocus, SettingItem.jsx). קודם השורה הייתה מוסטת 4dp
    // מכל צד עם פינות משלה, כלומר גלולה שצפה בתוך הכרטיס.
    val borderColor = animateColorAsState(
        if (isInteractive && isFocused) theme.futureTheme.readableAccentColor else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "settingItemBorder"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isInteractive) Modifier.onFocusChanged { isFocused = it.isFocused } else Modifier)
            .then(
                if (isInteractive) Modifier.onKeyEvent {
                    if (it.type == KeyEventType.KeyDown && (it.key == Key.DirectionCenter || it.key == Key.Enter || it.key == Key.NumPadEnter)) {
                        onClick!!()
                        true
                    } else false
                } else Modifier
            )
            .then(if (isInteractive) Modifier.focusable() else Modifier)
            // בכרטיס השורה נוגעת בשפות שלו (והריפוד הפנימי גדל ב-2dp, כך
            // שהגובה והמיקום של הטקסט לא זזו). מחוץ לכרטיס - מוסטת כמו קודם.
            .then(if (inCard) Modifier else Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
            .cardRowFocus({ bgColor.value }, { borderColor.value }, fallbackShape = shape)
            .then(if (isInteractive) Modifier.clickable { onClick!!() } else Modifier)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = if (inCard) 16.dp else 12.dp, vertical = if (inCard) 16.dp else 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = theme.primaryColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = theme.titleFontSize,
                    color = theme.textColor,
                    fontWeight = FontWeight.SemiBold
                )
                if (summary != null) {
                    Text(
                        text = summary,
                        fontSize = theme.summaryFontSize,
                        color = theme.textColor.copy(alpha = 0.6f)
                    )
                }
            }
            if (showChevron) {
                // חץ שמצביע שמאלה, *לא* הגרסה ה-AutoMirrored: הממשק כולו בעברית
                // (RTL), ושם כיוון ההתקדמות פנימה הוא שמאלה. הווריאנט המשקף הפך
                // את החץ ימינה, כלומר לכיוון ההפוך מזה שהלחיצה על השורה מובילה אליו.
                Icon(
                    imageVector = FutureIcons.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = theme.textColor.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SettingSwitch(
    title: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    theme: ThemeConfig
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = FutureShapes.lg
    val inCard = LocalFutureCard.current != null
    // אותה לוגיקה כמו ב-SettingItem: השורה שקופה על הכרטיס, והפוקוס מוסיף מילוי ומסגרת.
    val bgColor = animateColorAsState(
        if (isFocused) theme.textColor.copy(alpha = 0.06f) else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "settingSwitchBg"
    )

    val borderColor = animateColorAsState(
        if (isFocused) theme.futureTheme.readableAccentColor else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "settingSwitchBorder"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent {
                if (it.type == KeyEventType.KeyDown && (it.key == Key.DirectionCenter || it.key == Key.Enter || it.key == Key.NumPadEnter)) {
                    onCheckedChange(!checked)
                    true
                } else false
            }
            .focusable()
            // בכרטיס השורה נוגעת בשפות שלו (והריפוד הפנימי גדל ב-2dp, כך
            // שהגובה והמיקום של הטקסט לא זזו). מחוץ לכרטיס - מוסטת כמו קודם.
            .then(if (inCard) Modifier else Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
            .cardRowFocus({ bgColor.value }, { borderColor.value }, fallbackShape = shape)
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = if (inCard) 16.dp else 12.dp, vertical = if (inCard) 14.dp else 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = theme.titleFontSize,
                    color = theme.textColor,
                    fontWeight = FontWeight.SemiBold
                )
                if (summary != null) {
                    Text(
                        text = summary,
                        fontSize = theme.summaryFontSize,
                        color = theme.textColor.copy(alpha = 0.6f)
                    )
                }
            }
            // המתג של הדיזיין סיסטם: הכפתור בצבע הרקע כשדלוק, ולא לבן קבוע -
            // עם ההדגשה הלבנה של ברירת המחדל Switch של Material היה לבן על לבן.
            FutureSwitch(checked = checked, theme = theme.futureTheme)
        }
    }
}

/**
 * הכרטיס של המערכת (FutureCard) - אותן מידות וצל כמו קודם, אבל עכשיו
 * השורות שבתוכו יודעות איפה הן יושבות, והפוקוס שלהן לוקח את הפינות שלו.
 */
@Composable
fun SettingsCard(theme: ThemeConfig, content: @Composable ColumnScope.() -> Unit) {
    FutureCard(theme = theme.futureTheme, content = content)
}

@Composable
fun SettingDivider(theme: ThemeConfig? = null) {
    FutureDivider(theme = theme?.futureTheme ?: com.future.sharednav.theme.FutureTheme())
}

@Composable
fun LargeHeader(title: String, theme: ThemeConfig) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp)
    ) {
        Text(
            text = title,
            fontSize = theme.headerFontSize,
            fontWeight = FontWeight.Bold,
            color = theme.textColor
        )
    }
}

@Composable
fun SettingHeader(title: String, theme: ThemeConfig) {
    Text(
        text = title,
        modifier = Modifier
            .padding(start = 24.dp, top = 20.dp, bottom = 8.dp)
            .fillMaxWidth(),
        // כותרת קטע היא תווית ולא פריט - היא מודפסת בטקסט מעומעם ולא בצבע
        // ההדגשה, כדי שהמבט ייפול על שמות הפריטים שבכרטיס ולא על הכותרת שמעליו.
        color = theme.textColor.copy(alpha = 0.55f),
        fontWeight = FontWeight.Bold,
        fontSize = FutureTypography.summary,
        letterSpacing = FutureTypography.trackingSection
    )
}
