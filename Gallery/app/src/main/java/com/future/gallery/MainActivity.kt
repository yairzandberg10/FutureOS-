package com.future.gallery

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import kotlinx.coroutines.launch
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
            // true אחרי שהטעינה הראשונה מ-MediaStore הסתיימה (מבדיל "טוען" מ"אין תמונות").
            var loaded by remember { mutableStateOf(false) }
            // "אל תשאל שוב" - המערכת לא תציג את חלון ההרשאה שוב, ולחיצה פותחת את הגדרות האפליקציה.
            var permissionDeniedForever by rememberSaveable { mutableStateOf(false) }
            var selectedAlbum by remember { mutableStateOf<Album?>(null) }
            var selectedItem by remember { mutableStateOf<MediaItem?>(null) }
            var viewerList by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
            // נשמרים גם אחרי selectedItem/selectedAlbum חוזרים ל-null - כדי
            // שכשחוזרים "אחורה", הפוקוס ישוב בדיוק לפריט/לאלבום שממנו נכנסנו.
            var lastSelectedItemId by remember { mutableStateOf<Long?>(null) }
            var lastSelectedAlbumId by remember { mutableStateOf<String?>(null) }
            var editingItem by remember { mutableStateOf<MediaItem?>(null) }
            // תמונה שנפתחה מאפליקציה אחרת (VIEW) - נפתחת ישר לצפייה, ו-BACK סוגר.
            // רק content:// - ומספק זר רק אם השולח נתן עליו הרשאה. בלי זה אפליקציה אחרת
            // יכלה לפתוח כאן file:// או ספק שרק לגלריה יש גישה אליו (ומשם למחוק/לערוך
            // אותו בשם הגלריה).
            val viewUri = remember {
                intent?.takeIf { it.action == Intent.ACTION_VIEW }?.let { i ->
                    i.data?.takeIf { uri ->
                        uri.scheme == "content" &&
                            (uri.authority == android.provider.MediaStore.AUTHORITY ||
                                i.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
                    }
                }
            }
            LaunchedEffect(viewUri) {
                val uri = viewUri ?: return@LaunchedEffect
                // פרטים אמיתיים מהספק כשהוא חושף אותם; אחרת השדות נשארים ריקים - לא מזויפים.
                val info = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    runCatching {
                        contentResolver.query(
                            uri,
                            arrayOf(android.provider.MediaStore.Images.Media.DATE_ADDED, android.provider.MediaStore.Images.Media.SIZE, android.provider.MediaStore.Images.Media.DISPLAY_NAME),
                            null, null, null,
                        )?.use { c -> if (c.moveToFirst()) Triple(c.getLong(0), c.getLong(1), c.getString(2)) else null }
                    }.getOrNull()
                }
                val single = MediaItem(
                    id = uri.hashCode().toLong(), uri = uri, dateAdded = info?.first ?: 0L,
                    displayName = info?.third ?: uri.lastPathSegment ?: "", size = info?.second ?: 0L, bucketId = "", bucketName = "",
                )
                viewerList = listOf(single)
                selectedItem = single
            }
            // מתעדכן בזמן אמת כשמצב כהה/בהיר או צבע ההדגשה משתנים (ר' rememberFutureTheme).
            val theme = rememberFutureTheme()

            // קריאת MediaStore רצה ב-IO - על ה-main thread היא תקעה את המסך עם הרבה תמונות
            val loadScope = androidx.compose.runtime.rememberCoroutineScope()
            fun reloadItems() {
                loadScope.launch {
                    items = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { repository.getAllMedia() }
                    loaded = true
                }
            }

            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                hasPermission = granted
                permissionDeniedForever = !granted && !shouldShowRequestPermissionRationale(repository.requiredPermission())
                if (granted) reloadItems()
            }

            LaunchedEffect(hasPermission) {
                if (hasPermission) reloadItems()
            }

            // ההרשאה יכולה להשתנות בהגדרות המערכת בזמן שהאפליקציה ברקע - בודקים שוב בכל חזרה.
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        val now = repository.hasMediaPermission()
                        if (now != hasPermission) hasPermission = now
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
                                    reloadItems()
                                }
                            )
                            current != null -> MediaViewerScreen(
                                item = current,
                                items = viewerList,
                                onBack = { if (viewUri != null) finish() else selectedItem = null },
                                onNavigate = { selectedItem = it; lastSelectedItemId = it.id },
                                onDeleted = {
                                    // תמונה שנפתחה מאפליקציה אחרת - אחרי מחיקה סוגרים, לא מציגים את הגלריה.
                                    if (viewUri != null) finish() else selectedItem = null
                                    reloadItems()
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
                                permissionDeniedForever = permissionDeniedForever,
                                onRequestPermission = {
                                    if (permissionDeniedForever) {
                                        runCatching {
                                            startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", packageName, null)))
                                        }
                                    } else {
                                        permissionLauncher.launch(repository.requiredPermission())
                                    }
                                },
                                loaded = loaded,
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
