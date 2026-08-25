package com.future.sfarim.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.future.sfarim.ui.theme.FutureTheme
import com.future.sharednav.focus.FocusableItem as SharedFocusableItem

/** עטיפה דקה סביב הרכיב המשותף (מודול SharedKeypadNav) ששומרת על חתימת
 * הקריאה המקורית של Sfarim (theme: FutureTheme) כך שכל אתרי הקריאה במסכים
 * נשארים ללא שינוי. צבע הפוקוס נגזר מ-theme.accentColor (מסונכרן עם
 * ההגדרות) - כל שאר הפרמטרים משתמשים בברירות המחדל של הרכיב המשותף, שהן
 * בדיוק ההתנהגות שהייתה כאן קודם (בורדר 1.5dp, פינות 8dp, scale 1.02, ריפוד 4dp). */
@Composable
fun FocusableItem(
    onClick: () -> Unit,
    theme: FutureTheme,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    content: @Composable BoxScope.(isFocused: Boolean) -> Unit,
) {
    SharedFocusableItem(
        onClick = onClick,
        accentColor = theme.accentColor,
        modifier = modifier,
        focusRequester = focusRequester,
        content = content,
    )
}
