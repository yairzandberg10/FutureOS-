package com.future.sharednav.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em

/**
 * הגשר בין [FutureTheme] (מקור האמת של המערכת) לבין MaterialTheme.
 *
 * למה זה נחוץ: נמצאו שבע ערכות Material נפרדות בריפו - dialer, Messages,
 * notes, Navigation, FutureLauncher, Settings ו-FutureUI - שכולן מגדירות
 * כמעט את אותו דבר בהעתקה ידנית, וכל אחת סטתה במשהו. שתיים מהן היו מנותקות
 * לגמרי מהעיצוב המשותף: FutureUI (מעטפת המערכת עצמה!) רצה על Material You
 * דינמי לפי isSystemInDarkTheme, כלומר צבעי הסגול של אנדרואיד ולא צבע
 * ההדגשה שהמשתמש בחר, ו-FutureLauncher קיבעה מצב כהה בלבד. בפועל זה אומר
 * שכל רכיב Material מוכן (Switch, NavigationBar, AlertDialog,
 * OutlinedTextField, Slider) נראה אחרת בכל אפליקציה.
 *
 * מעכשיו כל ערכה כזו היא מעטפת דקה סביב הפונקציה הזו.
 */

/** הערכה הפעילה, לרכיבים משותפים שלא רוצים לקבל אותה כפרמטר. */
val LocalFutureTheme = staticCompositionLocalOf { FutureTheme() }

@Composable
fun FutureMaterialTheme(
    theme: FutureTheme,
    content: @Composable () -> Unit,
) {
    val colorScheme = remember(theme) { theme.toColorScheme() }
    val type = rememberFutureType()

    // ההדגשה המתוקנת מסופקת גם כאן ולא רק ב-ScreenScaffold, כדי שאפליקציה
    // שנשענת על FutureMaterialTheme (החייגן, ההודעות) תקבל את אותו תיקון.
    val accent = remember(theme) { theme.readableAccentColor }

    CompositionLocalProvider(
        LocalFutureTheme provides theme,
        LocalFutureType provides type,
        LocalFutureAccent provides accent,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = futureTypography(type),
            shapes = FutureMaterialShapes,
            content = content,
        )
    }
}

/**
 * שורש מסך שלם: הערכה + RTL. זה מה שכל Activity אמורה לעטוף בו את
 * setContent - הכפייה של LayoutDirection.Rtl הייתה עד עכשיו שורה מועתקת
 * בכל אפליקציה בנפרד (ובחלקן פשוט נשכחה).
 */
@Composable
fun FutureAppTheme(
    theme: FutureTheme = rememberFutureTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        FutureMaterialTheme(theme = theme, content = content)
    }
}

/**
 * מיפוי הטוקנים של המערכת ל-ColorScheme של Material. onPrimary/onError
 * נגזרים מהניגודיות ([FutureContrast]) ולא נכתבים כ-Color.Black קבוע -
 * זה בדיוק המקום שבו צבע הדגשה כהה שהמשתמש בוחר הפך טקסט לבלתי קריא.
 */
fun FutureTheme.toColorScheme(): ColorScheme {
    val base = if (isDarkMode) darkColorScheme() else lightColorScheme()
    // primary הוא ההדגשה *הקריאה*, ולכן onPrimary נגזר ממנה ולא מההדגשה
    // הגולמית (ר' onReadableAccentColor).
    return base.copy(
        primary = readableAccentColor,
        onPrimary = onReadableAccentColor,
        primaryContainer = accentColor.copy(alpha = if (isDarkMode) 0.24f else 0.18f),
        onPrimaryContainer = textColor,
        secondary = readableAccentColor,
        onSecondary = onReadableAccentColor,
        secondaryContainer = textColor.copy(alpha = if (isDarkMode) 0.12f else 0.06f),
        onSecondaryContainer = textColor,
        tertiary = readableAccentColor,
        onTertiary = onReadableAccentColor,
        background = backgroundColor,
        onBackground = textColor,
        surface = surfaceColor,
        onSurface = textColor,
        surfaceVariant = elevatedSurfaceColor,
        onSurfaceVariant = mutedTextColor,
        surfaceContainerLowest = backgroundColor,
        surfaceContainerLow = surfaceColor,
        surfaceContainer = surfaceColor,
        surfaceContainerHigh = elevatedSurfaceColor,
        surfaceContainerHighest = raisedSurfaceColor,
        inverseSurface = textColor,
        inverseOnSurface = backgroundColor,
        error = dangerColor,
        onError = FutureContrast.onColor(dangerColor),
        errorContainer = dangerColor.copy(alpha = 0.20f),
        onErrorContainer = textColor,
        outline = textColor.copy(alpha = 0.30f),
        outlineVariant = dividerColor,
        scrim = Color.Black,
    )
}

/** אותה סקאלה של [FutureTypography], בשמות של Material. */
fun futureTypography(type: FutureType = FutureType()): Typography {
    fun style(size: TextUnit, weight: FontWeight = FontWeight.Normal) =
        TextStyle(fontSize = size, fontWeight = weight, lineHeight = 1.35.em)

    return Typography(
        displayLarge = style(type.hero, FontWeight.Bold),
        displayMedium = style(type.display, FontWeight.Bold),
        displaySmall = style(type.headline, FontWeight.Bold),
        headlineLarge = style(type.display, FontWeight.Bold),
        headlineMedium = style(type.headline, FontWeight.Bold),
        headlineSmall = style(type.screenTitle, FontWeight.Bold),
        titleLarge = style(type.screenTitle, FontWeight.Bold),
        titleMedium = style(type.title, FontWeight.SemiBold),
        titleSmall = style(type.body, FontWeight.SemiBold),
        bodyLarge = style(type.bodyLarge),
        bodyMedium = style(type.body),
        bodySmall = style(type.summary),
        labelLarge = style(type.body, FontWeight.Medium),
        labelMedium = style(type.label, FontWeight.Medium),
        labelSmall = style(type.caption, FontWeight.Medium),
    )
}

/** אותה סקאלה של [FutureShapes], בשמות של Material. */
val FutureMaterialShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(FutureShapes.radiusXs),
    small = RoundedCornerShape(FutureShapes.radiusSm),
    medium = RoundedCornerShape(FutureShapes.radiusMd),
    large = RoundedCornerShape(FutureShapes.radiusLg),
    extraLarge = RoundedCornerShape(FutureShapes.radiusXl),
)
