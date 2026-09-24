package com.future.messages.ui.components

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

private const val GalleryPackage = "com.future.gallery"

/**
 * בחירת תמונה לצירוף - ישר בגלריה של FutureOS, במצב בחירה (GET_CONTENT).
 * GetContent הכללי פתח את בורר המערכת, ושם הופיעה גם אפליקציית Files,
 * שמצהירה על GET_CONTENT אבל לא מחזירה תוצאה - הצירוף "לא עבד".
 */
@Composable
fun rememberGalleryImagePicker(onPicked: (Uri) -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnPicked by rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null) currentOnPicked(uri)
    }
    return remember {
        {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                setPackage(GalleryPackage)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                launcher.launch(intent)
            } else {
                Toast.makeText(context, "אפליקציית הגלריה לא מותקנת", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
