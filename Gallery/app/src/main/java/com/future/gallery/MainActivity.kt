package com.future.gallery

import android.content.Intent
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
import com.future.gallery.data.Album
import com.future.gallery.data.MediaItem
import com.future.gallery.data.MediaRepository
import com.future.gallery.ui.AlbumDetailScreen
import com.future.gallery.ui.GalleryHomeScreen
import com.future.gallery.ui.MediaViewerScreen
import com.future.gallery.ui.PhotoEditorScreen
import com.future.sharednav.components.AnimatedScreenHost
import com.future.sharednav.theme.FutureMaterialTheme
import com.future.sharednav.theme.rememberFutureTheme

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית. לא פוגע בניווט/הפעלה במקשים -
    // dispatchKeyEvent הוא נתיב נפרד לגמרי מ-dispatchTouchEvent.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    // GET_CONTENT/PICK - Gallery נקראת ע"י אפליקציה אחרת (כמו Messages) כדי
    // לבחור תמונה/וידאו בודדים, לא נפתחת כאפליקציה עצמאית. במצב הזה לחיצה על
    // פריט לא פותחת אותו בצפייה אלא מחזירה אותו כתוצאה לאפליקציה הקוראת.
    private val isPickMode: Boolean
        get() = intent?.action == Intent.ACTION_GET_CONTENT || intent?.action == Intent.ACTION_PICK

    private fun finishWithPickedItem(uri: android.net.Uri) {
        val result = Intent().apply {
            data = uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        setResult(RESULT_OK, result)
        finish()
    }

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
            // נשמרים גם אחרי selectedItem/selectedAlbum חוזרים ל-null - כדי
            // שכשחוזרים "אחורה", הפוקוס ישוב בדיוק לפריט/לאלבום שממנו נכנסנו.
            var lastSelectedItemId by remember { mutableStateOf<Long?>(null) }
            var lastSelectedAlbumId by remember { mutableStateOf<String?>(null) }
            var editingItem by remember { mutableStateOf<MediaItem?>(null) }
            // תמונה שנפתחה מאפליקציה אחרת (VIEW) - נפתחת ישר לצפייה, ו-BACK סוגר.
            val viewUri = remember { intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data }
            LaunchedEffect(viewUri) {
                val uri = viewUri ?: return@LaunchedEffect
                val single = MediaItem(
                    id = uri.hashCode().toLong(), uri = uri, dateAdded = System.currentTimeMillis() / 1000,
                    displayName = uri.lastPathSegment ?: "", size = 0L, bucketId = "", bucketName = "",
                )
                viewerList = listOf(single)
                selectedItem = single
            }
            // מתעדכן בזמן אמת כשמצב כהה/בהיר או צבע ההדגשה משתנים (ר' rememberFutureTheme).
            val theme = rememberFutureTheme()

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
                    selectedItem != null -> if (viewUri != null) finish() else selectedItem = null
                    else -> selectedAlbum = null
                }
            }

            FutureMaterialTheme(theme) {
                Surface(modifier = Modifier.fillMaxSize(), color = theme.backgroundColor) {
                    // המסך נמסר כצילום מצב ולא נקרא מהמשתנים ישירות: המסך שיוצא
                    // באנימציה ממשיך לצייר את הפריט/האלבום שלו גם אחרי שהמשתנים
                    // כבר התאפסו ל-null. המפתח הוא העומק בלבד, כך שדפדוף בין
                    // תמונות בתוך המציג לא מנפיש את כל המסך מחדש.
                    AnimatedScreenHost(
                        targetState = GalleryScreenState(editingItem, selectedItem, selectedAlbum),
                        depthOf = { it.depth },
                        contentKey = { it.depth },
                    ) { shown ->
                        val editing = shown.editing
                        val current = shown.current
                        val album = shown.album

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
                                onBack = { if (viewUri != null) finish() else selectedItem = null },
                                onNavigate = { selectedItem = it; lastSelectedItemId = it.id },
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
                                onItemClick = { list, item ->
                                    if (isPickMode) {
                                        finishWithPickedItem(item.uri)
                                    } else {
                                        viewerList = list; selectedItem = item; lastSelectedItemId = item.id
                                    }
                                },
                                lastSelectedItemId = lastSelectedItemId,
                            )
                            else -> GalleryHomeScreen(
                                items = items,
                                albums = albums,
                                hasPermission = hasPermission,
                                onRequestPermission = { permissionLauncher.launch(repository.requiredPermission()) },
                                onItemClick = { list, item ->
                                    if (isPickMode) {
                                        finishWithPickedItem(item.uri)
                                    } else {
                                        viewerList = list; selectedItem = item; lastSelectedItemId = item.id
                                    }
                                },
                                onAlbumClick = { selectedAlbum = it; lastSelectedAlbumId = it.bucketId },
                                theme = theme,
                                lastSelectedItemId = lastSelectedItemId,
                                lastSelectedAlbumId = lastSelectedAlbumId,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** המסך הנוכחי של הגלריה כערך אחד - עומק 0 בית, 1 אלבום, 2 מציג, 3 עורך. */
private data class GalleryScreenState(
    val editing: MediaItem?,
    val current: MediaItem?,
    val album: Album?,
) {
    val depth: Int
        get() = when {
            editing != null -> 3
            current != null -> 2
            album != null -> 1
            else -> 0
        }
}
