package com.future.music

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.future.music.data.PlaylistStore
import com.future.music.data.SongRepository
import com.future.music.playback.PlayerController
import com.future.sharednav.theme.ThemeClient
import com.future.music.ui.MusicNavHost
import com.future.music.ui.screens.PermissionScreen
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית. לא פוגע בניווט/הפעלה במקשים -
    // dispatchKeyEvent הוא נתיב נפרד לגמרי מ-dispatchTouchEvent.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MusicApp() }
    }
}

@Composable
private fun MusicApp() {
    val context = LocalContext.current

    val repository = remember { SongRepository(context) }
    val playlistStore = remember { PlaylistStore(context) }
    val playerController = remember { PlayerController(context) }

    // ברירת מחדל (כהה, לבן) עד שהתוצאה האמיתית מגיעה מ-ThemeProvider - השאילתה
    // חוצה-תהליכים (ContentResolver.query) רצה על Dispatchers.IO כדי לא לחסום
    // את ה-UI thread, באותו אידיום של rememberIoState/rememberAlbumArt.
    var theme by remember {
        mutableStateOf(FutureTheme(isDarkMode = true, accentColor = Color.White))
    }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val shared = withContext(Dispatchers.IO) { ThemeClient.getTheme(context) }
        theme = FutureTheme(isDarkMode = shared.isDarkMode, accentColor = Color(shared.primaryColor))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    val shared = withContext(Dispatchers.IO) { ThemeClient.getTheme(context) }
                    theme = FutureTheme(isDarkMode = shared.isDarkMode, accentColor = Color(shared.primaryColor))
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(Unit) {
        playerController.connect()
        onDispose { playerController.disconnect() }
    }

    var hasAudioPermission by remember { mutableStateOf(repository.hasAudioPermission()) }
    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasAudioPermission = granted
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission && Build.VERSION.SDK_INT >= 33) {
            val granted = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = theme.backgroundColor) {
        if (!hasAudioPermission) {
            PermissionScreen(theme = theme, onRequestPermission = { audioPermissionLauncher.launch(repository.requiredPermission()) })
        } else {
            MusicNavHost(repository = repository, playlistStore = playlistStore, playerController = playerController, theme = theme)
        }
    }
}
