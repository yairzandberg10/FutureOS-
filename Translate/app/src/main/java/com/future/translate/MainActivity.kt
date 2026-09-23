package com.future.translate

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.future.sharednav.theme.FutureAppTheme
import com.future.sharednav.theme.rememberFutureTheme
import com.future.translate.ui.TranslateActions
import com.future.translate.ui.TranslateApp
import com.future.translate.ui.TranslateViewModel

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית (זהה לכל שאר האפליקציות בסוויטה).
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    private val viewModel: TranslateViewModel by viewModels()

    /** מה להריץ כשהרשאת המיקרופון תאושר (מצב שיחה). */
    private var onMicGranted: (() -> Unit)? = null

    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onMicGranted?.invoke()
        onMicGranted = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // הערכה מתעדכנת חי (ContentObserver) - גם כשמצב כהה/בהיר משתנה
            // ממרכז הבקרה שנפתח מעל האפליקציה.
            val theme = rememberFutureTheme()
            val actions = remember {
                TranslateActions(
                    copy = ::copyToClipboard,
                    share = ::shareText,
                    requestMic = ::withMicPermission,
                )
            }
            // טקסט שהגיע מבחוץ ("תרגם" על טקסט מסומן, או שיתוף) נכנס לשדה.
            LaunchedEffect(Unit) {
                sharedText(intent)?.let { viewModel.setText(it) }
            }

            FutureAppTheme(theme) {
                TranslateApp(viewModel = viewModel, theme = theme, actions = actions)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedText(intent)?.let { viewModel.setText(it) }
    }

    override fun onStop() {
        super.onStop()
        // יציאה מהאפליקציה שומרת את התרגום שעל המסך בהיסטוריה.
        viewModel.commit()
        viewModel.speaker.stop()
        viewModel.listener.stop()
    }

    private fun sharedText(intent: Intent?): String? = when (intent?.action) {
        Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
        Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
        else -> null
    }?.takeIf { it.isNotBlank() }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("תרגום", text))
    }

    private fun shareText(text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        try {
            com.future.sharednav.share.FutureShare.open(this, send, "שתף תרגום")
        } catch (e: Exception) {
            // אין אפליקציה שמקבלת טקסט - אין לאן לשתף.
        }
    }

    private fun withMicPermission(onGranted: () -> Unit) {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            onGranted()
            return
        }
        onMicGranted = onGranted
        micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }
}
