package com.android.sistemui.ui.theme

import androidx.compose.runtime.Composable
import com.future.sharednav.theme.FutureMaterialTheme
import com.future.sharednav.theme.rememberFutureTheme

/**
 * מעטפת המערכת (שורת מצב, מסך נעילה, מרכז התראות, קונטרול סנטר) רצה עד
 * עכשיו על Material You דינמי לפי isSystemInDarkTheme - כלומר על צבעי
 * אנדרואיד ולא על מצב כהה/בהיר וצבע ההדגשה שהמשתמש בוחר *בתוך המעטפת
 * הזו עצמה*. עכשיו היא קוראת את אותה ערכה כמו כל אפליקציה אחרת, ומתעדכנת
 * ברגע שהקונטרול סנטר משנה אותה.
 */
@Composable
fun FutureUITheme(content: @Composable () -> Unit) {
    FutureMaterialTheme(theme = rememberFutureTheme(), content = content)
}
