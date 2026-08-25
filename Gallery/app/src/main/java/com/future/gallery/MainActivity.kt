package com.future.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.future.gallery.data.Album
import com.future.gallery.data.MediaItem
import com.future.gallery.data.MediaRepository
import com.future.gallery.theme.ThemeClient
import com.future.gallery.ui.AlbumDetailScreen
import com.future.gallery.ui.GalleryHomeScreen
import com.future.gallery.ui.MediaViewerScreen
import com.future.gallery.ui.PhotoEditorScreen
import com.future.gallery.ui.theme.FutureTheme

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית. לא פוגע בניווט/הפעלה במקשים -
    // dispatchKeyEvent הוא נתיב נפרד לגמרי מ-dispatchTouchEvent.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val repository = remember { MediaRepository(this) }
            var hasPermission by remember { mutableStateOf(repository.hasMediaPermission()) }
            var items by remember { mutableStateOf(listOf<MediaItem>()) }
            var selectedAlbum by remember { mutableStateOf<Album?>(null) }
            var selectedItem by remember { mutableStateOf<MediaItem?>(null) }
            var viewerList by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
            var editingItem by remember { mutableStateOf<MediaItem?>(null) }
            var theme by remember {
                mutableStateOf(
                    ThemeClient.getTheme(this@MainActivity).let {
                        FutureTheme(isDarkMode = it.isDarkMode, accentColor = Color(it.primaryColor))
                    }
                )
            }

            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                hasPermission = granted
                if (granted) items = repository.getAllMedia()
            }

            LaunchedEffect(hasPermission) {
                if (hasPermission) items = repository.getAllMedia()
            }

            val albums = remember(items) {
                items.groupBy { it.bucketId }
                    .mapNotNull { (bucketId, its) ->
                        if (bucketId.isBlank() || its.isEmpty()) null
                        else Album(bucketId = bucketId, bucketName = its.first().bucketName, coverUri = its.first().uri, count = its.size)
                    }
                    .sortedByDescending { it.count }
            }

            BackHandler(enabled = editingItem != null || selectedItem != null || selectedAlbum != null) {
                when {
                    editingItem != null -> editingItem = null
                    selectedItem != null -> selectedItem = null
                    else -> selectedAlbum = null
                }
            }

            // מרענן את העיצוב המשותף בכל חזרה למסך (למשל אחרי שינוי מצב כהה/בהיר
            // או צבע הדגשה באפליקציית ההגדרות) בלי לבנות מחדש את כל ה-Activity.
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        theme = ThemeClient.getTheme(this@MainActivity).let {
                            FutureTheme(isDarkMode = it.isDarkMode, accentColor = Color(it.primaryColor))
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            Surface(modifier = Modifier.fillMaxSize(), color = theme.backgroundColor) {
                val editing = editingItem
                val current = selectedItem
                val album = selectedAlbum

                when {
                    editing != null -> PhotoEditorScreen(
                        item = editing,
                        theme = theme,
                        onBack = { editingItem = null },
                        onSaved = {
                            editingItem = null
                            items = repository.getAllMedia()
                        }
                    )
                    current != null -> MediaViewerScreen(
                        item = current,
                        items = viewerList,
                        onBack = { selectedItem = null },
                        onNavigate = { selectedItem = it },
                        onDeleted = {
                            selectedItem = null
                            items = repository.getAllMedia()
                        },
                        onEdit = { editingItem = current },
                        theme = theme
                    )
                    album != null -> AlbumDetailScreen(
                        albumName = album.bucketName,
                        items = items.filter { it.bucketId == album.bucketId },
                        theme = theme,
                        onBack = { selectedAlbum = null },
                        onItemClick = { list, item -> viewerList = list; selectedItem = item }
                    )
                    else -> GalleryHomeScreen(
                        items = items,
                        albums = albums,
                        hasPermission = hasPermission,
                        onRequestPermission = { permissionLauncher.launch(repository.requiredPermission()) },
                        onItemClick = { list, item -> viewerList = list; selectedItem = item },
                        onAlbumClick = { selectedAlbum = it },
                        theme = theme
                    )
                }
            }
        }
    }
}
