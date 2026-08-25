package com.future.music.ui.components

import android.net.Uri
import android.util.Size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** טוען עטיפת אלבום אמיתית מ-MediaStore (loadThumbnail, לא מדומה) - חלק
 * מהשירים לא ייכללו artwork מוטבע, ואז מוחזר null (fallback לאייקון כללי). */
@Composable
fun rememberAlbumArt(uri: Uri?): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uri) {
        bitmap = if (uri == null) null else withContext(Dispatchers.IO) {
            try {
                context.contentResolver.loadThumbnail(uri, Size(512, 512), null).asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }
    return bitmap
}
