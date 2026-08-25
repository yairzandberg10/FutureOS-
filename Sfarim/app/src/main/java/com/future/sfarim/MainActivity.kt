package com.future.sfarim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.future.sfarim.data.LibraryDatabase
import com.future.sfarim.data.LibraryRepository
import com.future.sfarim.theme.ThemeClient
import com.future.sfarim.ui.LibraryNavHost
import com.future.sfarim.ui.screens.LibraryNotInstalledScreen
import com.future.sfarim.ui.theme.FutureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית. לא פוגע בניווט/הפעלה במקשים -
    // dispatchKeyEvent הוא נתיב נפרד לגמרי מ-dispatchTouchEvent.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SfarimApp() }
    }
}

@Composable
private fun SfarimApp() {
    val context = LocalContext.current

    var theme by remember {
        mutableStateOf(
            ThemeClient.getTheme(context).let {
                FutureTheme(isDarkMode = it.isDarkMode, accentColor = Color(it.primaryColor))
            }
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val shared = ThemeClient.getTheme(context)
                theme = FutureTheme(isDarkMode = shared.isDarkMode, accentColor = Color(shared.primaryColor))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // פתיחת sefaria.db (עד כ-2.1GB) הייתה מתבצעת synchronously בתוך composition,
    // כלומר על ה-main thread - עלול לתקוע/להאט את עליית האפליקציה. עכשיו
    // הפתיחה עוברת ל-Dispatchers.IO ומוצגת מסך טעינה קצר עד שהיא מסתיימת,
    // כדי שמסך "הספרייה לא מותקנת" יוצג רק אחרי שבאמת אימתנו שאין קובץ -
    // לא בזמן שהבדיקה עדיין רצה.
    var isLoadingDb by remember { mutableStateOf(true) }
    val db by produceState<android.database.sqlite.SQLiteDatabase?>(initialValue = null, context) {
        value = withContext(Dispatchers.IO) { LibraryDatabase.openOrNull(context) }
        isLoadingDb = false
    }
    val repository = remember(db) { db?.let { LibraryRepository(it) } }

    Surface(modifier = Modifier.fillMaxSize(), color = theme.backgroundColor) {
        when {
            isLoadingDb -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = theme.accentColor)
                }
            }
            repository == null -> {
                LibraryNotInstalledScreen(expectedPath = LibraryDatabase.expectedPath(context).path, theme = theme)
            }
            else -> {
                LibraryNavHost(repository = repository, theme = theme)
            }
        }
    }
}
