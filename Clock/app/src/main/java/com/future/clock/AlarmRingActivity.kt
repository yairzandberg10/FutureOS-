package com.future.clock

import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.clock.logic.AlarmLogic
import com.future.clock.logic.EXTRA_ALARM_ID
import com.future.clock.logic.SNOOZE_MINUTES
import com.future.sharednav.theme.ThemeClient
import com.future.sharednav.theme.FutureTheme

/**
 * מסך צלצול ייעודי - מחליף את ה-UX הקודם שהיה Toast בלבד (בלי צליל, בלי
 * מסך מלא, בלי אפשרות נודניק/ביטול אמיתית). מוצג גם מעל מסך נעילה
 * (showWhenLocked/turnScreenOn, זמינים מ-API 27 - תמיד קיימים ב-minSdk 31).
 *
 * ניווט מקלדת בלבד, כמו בכל האפליקציה: מקש OK מבטל את הצלצול, כל מקש אחר
 * דוחה (נודניק) ב-SNOOZE_MINUTES דקות. BACK לא סוגר את המסך בכוונה - אחרת
 * לחיצה מקרית הייתה משתיקה אזעקה בלי שום פעולה מודעת מהמשתמש.
 */
class AlarmRingActivity : ComponentActivity() {
    private var ringtone: Ringtone? = null
    private var vibratorManager: VibratorManager? = null

    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        val hour = intent.getIntExtra(EXTRA_HOUR, 0)
        val minute = intent.getIntExtra(EXTRA_MINUTE, 0)
        val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()

        startRinging()

        setContent {
            val theme = remember {
                ThemeClient.getTheme(this@AlarmRingActivity).let {
                    FutureTheme(isDarkMode = it.isDarkMode, accentColor = Color(it.primaryColor))
                }
            }
            var dismissed by remember { mutableStateOf(false) }

            BackHandler(enabled = true) { /* מכוון: לא מבטל צלצול בטעות עם BACK */ }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .onKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown || dismissed) return@onKeyEvent false
                        when (event.key) {
                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                dismissed = true
                                stopRinging()
                                finish()
                            }
                            else -> {
                                dismissed = true
                                stopRinging()
                                if (alarmId != -1) AlarmLogic.scheduleSnooze(this@AlarmRingActivity, alarmId)
                                finish()
                            }
                        }
                        true
                    },
                color = theme.backgroundColor,
            ) {
                androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                "השעון המעורר מצלצל",
                                color = theme.accentColor,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "%02d:%02d".format(hour, minute),
                                color = theme.textColor,
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Light,
                            )
                            if (label.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(label, color = theme.textColor.copy(alpha = 0.7f), fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.height(48.dp))
                            Text("OK - ביטול", color = theme.textColor.copy(alpha = 0.6f), fontSize = 14.sp)
                            Text(
                                "כל מקש אחר - נודניק ($SNOOZE_MINUTES דקות)",
                                color = theme.textColor.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun startRinging() {
        try {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ringtone = RingtoneManager.getRingtone(this, uri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                isLooping = true
                play()
            }
        } catch (e: Exception) {
            // מכשירים בלי צליל ברירת מחדל מוגדר (או בלי הרשאת אודיו כלשהי) לא
            // אמורים למנוע את שאר חוויית הצלצול (רטט + מסך) - רק הצליל נעדר.
        }

        try {
            val manager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager = manager
            val pattern = longArrayOf(0, 800, 400)
            manager.vibrate(
                CombinedVibration.createParallel(
                    VibrationEffect.createWaveform(pattern, 0)
                )
            )
        } catch (e: Exception) {
            // ללא רטט זמין, ממשיכים עם צליל+מסך בלבד.
        }
    }

    private fun stopRinging() {
        try {
            ringtone?.stop()
        } catch (e: Exception) {
        }
        ringtone = null
        try {
            vibratorManager?.cancel()
        } catch (e: Exception) {
        }
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_HOUR = "EXTRA_HOUR"
        const val EXTRA_MINUTE = "EXTRA_MINUTE"
        const val EXTRA_LABEL = "EXTRA_LABEL"
    }
}
