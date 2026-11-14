package com.future.sfarim.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** מריץ שאילתת SQLite (סינכרונית) על Dispatchers.IO בכל שינוי של key, בלי לחסום
 * את ה-UI thread. מחזיר null עד שהתוצאה מוכנה.
 * keepPrevious = true: כשה-key משתנה הערך הקודם נשאר עד שהחדש מוכן (רשימות
 * שמתרעננות אחרי כתיבה - בלי הבהוב ובלי לאבד פוקוס).
 * חריגה מהשאילתה (S06) נרשמת ביומן ולא מפילה את האפליקציה; הערך נשאר null. */
@Composable
fun <T> rememberIoState(key: Any?, keepPrevious: Boolean = false, compute: suspend () -> T): T? {
    var state by if (keepPrevious) remember { mutableStateOf<T?>(null) } else remember(key) { mutableStateOf<T?>(null) }
    LaunchedEffect(key) {
        try {
            state = withContext(Dispatchers.IO) { compute() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("IoState", "query failed", e)
        }
    }
    return state
}
