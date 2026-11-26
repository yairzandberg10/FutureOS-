package com.future.systemui.fui.lockscreen.settings

import com.future.sharednav.i18n.tr

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.future.systemui.fui.lockscreen.face.FaceEnroller
import com.future.systemui.fui.lockscreen.face.FaceStatus
import com.future.systemui.fui.lockscreen.logic.LockSettings
import com.future.sharednav.theme.FutureTypography

private val Danger = Color(0xFFFF6B6B)

/**
 * רישום פנים בלבד. כל שאר הגדרות מסך הנעילה (קוד, נעילה אוטומטית, זיהוי פנים,
 * התראות ועיצוב) נמצאות באפליקציית ההגדרות ונשמרות דרך LockRemote. הרישום נשאר
 * כאן כי המצלמה ותבנית הפנים שייכות לתהליך של FutureUI.
 */
@Composable
fun LockEnrollScreen(modifier: Modifier = Modifier, onExit: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { LockSettings(context) }
    CompositionLocalProvider(LocalLayoutDirection provides com.future.sharednav.i18n.Tr.layoutDirection) {
        Box(modifier.background(Color.Black)) {
            EnrollPage(settings, onFinish = { ok ->
                if (ok) {
                    settings.faceEnabled = true
                    Toast.makeText(context, tr("הפנים נרשמו"), Toast.LENGTH_SHORT).show()
                }
                onExit()
            })
        }
    }
}

// ------------------------------------------------------------- רישום פנים

@Composable
private fun EnrollPage(settings: LockSettings, onFinish: (Boolean) -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        if (!ok) onFinish(false)
    }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var count by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf(FaceStatus.NO_FACE) }
    var error by remember { mutableStateOf<String?>(null) }
    val enroller = remember { FaceEnroller(context, settings) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(granted) {
        if (!granted) { launcher.launch(Manifest.permission.CAMERA); return@LaunchedEffect }
        runCatching { focus.requestFocus() }
        enroller.start(
            onPreview = { preview = it },
            onProgress = { c, s -> count = c; status = s },
            onDone = { onFinish(true) },
            onError = { error = it },
        )
    }
    DisposableEffect(Unit) { onDispose { enroller.stop() } }
    BackHandler { enroller.stop(); onFinish(false) }

    Column(
        Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusable()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(tr("רישום פנים"), color = Color.White, fontSize = FutureTypography.screenTitle, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .size(180.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(3.dp, if (status == FaceStatus.SCANNING) Color.White else Color.White.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            preview?.let {
                Image(it.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { count / FaceEnroller.TARGET.toFloat() },
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.fillMaxWidth(0.7f)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = error ?: when (status) {
                FaceStatus.TOO_FAR -> tr("קרב את הטלפון לפנים")
                FaceStatus.TOO_DARK -> tr("חשוך מדי - עבור למקום מואר")
                FaceStatus.SCANNING -> tr("הזז את הראש לאט במעגל")
                else -> tr("הבט במצלמה הקדמית")
            },
            color = if (error != null) Danger else Color.White.copy(alpha = 0.85f),
            fontSize = FutureTypography.body,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Text(tr("חזור - ביטול"), color = Color.White.copy(alpha = 0.45f), fontSize = FutureTypography.caption)
    }
}
