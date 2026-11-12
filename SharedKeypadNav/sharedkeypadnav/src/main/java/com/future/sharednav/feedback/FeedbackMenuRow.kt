package com.future.sharednav.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureTheme

/**
 * שורת "שלח משוב" לתפריט האפשרויות של אפליקציה ([com.future.sharednav.components.FutureOptionsMenu]).
 * לא נוספת אוטומטית לכל תפריט: כמעט כל התפריטים במערכת הם תפריטי הקשר של פריט (איש קשר, קובץ,
 * שיחה), ושם היא לא שייכת. אפליקציה שמה אותה בתפריט הראשי שלה, כשורה האחרונה.
 * בתוך Settings עצמה, או כש-Settings לא מותקנת, השורה לא מוצגת.
 */
@Composable
fun FeedbackMenuRow(theme: FutureTheme, onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val visible = remember { context.packageName != FeedbackShortcut.SETTINGS_PKG && FeedbackShortcut.isAvailable(context) }
    if (!visible) return
    FutureMenuRow(
        label = "שלח משוב",
        icon = FutureIcons.AutoMirrored.Chat,
        theme = theme,
        onClick = {
            onDismissRequest()
            FeedbackShortcut.open(context)
        },
    )
}
