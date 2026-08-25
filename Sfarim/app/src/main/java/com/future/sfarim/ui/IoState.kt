package com.future.sfarim.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** מריץ שאילתת SQLite (סינכרונית) על Dispatchers.IO בכל שינוי של key, בלי לחסום
 * את ה-UI thread. מחזיר null עד שהתוצאה מוכנה. */
@Composable
fun <T> rememberIoState(key: Any?, compute: suspend () -> T): T? {
    var state by remember(key) { mutableStateOf<T?>(null) }
    LaunchedEffect(key) {
        state = withContext(Dispatchers.IO) { compute() }
    }
    return state
}
