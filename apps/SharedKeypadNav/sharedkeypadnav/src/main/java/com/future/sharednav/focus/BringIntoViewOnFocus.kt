package com.future.sharednav.focus

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import kotlinx.coroutines.launch

/**
 * גורם לפריט הממוקד לגלול לתוך התצוגה בתוך כל מכל גלילה (LazyColumn/
 * LazyRow/Column עם verticalScroll וכו') כשהוא מקבל פוקוס - הפתרון
 * המתועד הרשמי של Compose לבעיה הזאת (BringIntoViewRequester), לא תלוי
 * במעקב "איזה אינדקס ממוקד" ברמת ההורה. זה הפתרון שנבחר לבעיה השיטתית
 * שנמצאה ב-audit: מתוך ~59 קבצים במערכת עם רשימות גלילה, רק 2 גללו בפועל
 * לפריט הממוקד - בכל שאר הרשימות, אחרי כמה לחיצות DOWN הפריט הממוקד יוצא
 * מתחום המסך.
 *
 * בניגוד ל-KeypadLazyColumn/FocusListState (שדורשים state מורם ברמת
 * ההורה ומתאימים למסכים חדשים), זה מודיפייר פר-פריט שאפשר להוסיף לכל
 * רכיב פוקוס-ניתן קיים (FocusableItem, שורה מקומית וכו') בלי לשנות את
 * מבנה ה-state של המסך שמכיל אותו - זו הסיבה שהוא נבחר כאן במקום לאלץ
 * מיגרציה מלאה של כל 59 הקבצים לרכיב חדש.
 *
 * מאותה סיבה בדיוק הוא גם המקום של "חזרה אחורה מחזירה את הפוקוס לאותו
 * פריט" (ר' [FocusMemory]): כמעט כל פריט פוקוס במערכת עובר כאן. זה עובד
 * כשהמודיפייר בא לפני focusable/clickable של הפריט, כמו ברכיבים המשותפים.
 * [restoreFocus] = false לשדות טקסט - שחזור פוקוס לשדה פותח מקלדת.
 */
@Composable
fun Modifier.bringIntoViewOnFocus(restoreFocus: Boolean = true): Modifier {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val restoreRequester = remember { FocusRequester() }
    val markFocused = rememberFocusRestore(restoreRequester, restoreFocus)
    return this
        .focusRequester(restoreRequester)
        .bringIntoViewRequester(bringIntoViewRequester)
        .onFocusEvent { focusState ->
            if (focusState.isFocused) {
                markFocused()
                coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
            }
        }
}
