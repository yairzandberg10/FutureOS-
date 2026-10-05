package com.future.clock

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.clock.logic.EXTRA_ALARM_ID
import com.future.clock.logic.SNOOZE_MINUTES
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureButtonVariant
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureAppTheme
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.rememberFutureTheme

/**
 * מסך הצלצול. שני כפתורים של המערכת - "עצור" (ממוקד כברירת מחדל) ו"נודניק" -
 * ו-BACK דוחה לנודניק, כך שלחיצה מקרית לא משתיקה אזעקה לגמרי.
 *
 * קודם ה-onKeyEvent ישב על Surface שלא מקבל פוקוס, ולכן אף מקש לא הגיע
 * אליו, ו-BACK היה חסום לגמרי: אי אפשר היה לכבות את הצלצול. והמסך ישב
 * בתוך המשימה של האפליקציה, כך שבכל פתיחה של "שעון" הוא עלה שוב.
 * עכשיו יש לו משימה משלו (taskAffinity במניפסט) והוא נסגר תמיד.
 *
 * הצליל, הרטט והנודניק האוטומטי רצים ב-AlarmRingService, כדי שהאזעקה
 * תצלצל גם כשהמערכת לא פותחת את המסך. המסך רק שולט בהם, ונסגר כשהצלצול
 * נגמר (גם כש"עצור" נלחץ בהתראה).
 */
class AlarmRingActivity : ComponentActivity() {
    private var alarmId = -1
    private var handled = false

    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)

        alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        val hour = intent.getIntExtra(EXTRA_HOUR, 0)
        val minute = intent.getIntExtra(EXTRA_MINUTE, 0)
        val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()

        // מסך שנפתח בלי צלצול פעיל מאחוריו (אחרי שהתהליך נהרג, או מהתראה ישנה) נסגר מיד.
        if (savedInstanceState != null || !AlarmRingService.ringing.value) {
            finishAndRemoveTask()
            return
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                AlarmRingService.ringing.collect { ringing ->
                    if (!ringing) {
                        handled = true
                        finishAndRemoveTask()
                    }
                }
            }
        }

        setContent {
            val theme = rememberFutureTheme()
            val stopFocus = remember { FocusRequester() }
            LaunchedEffect(Unit) { runCatching { stopFocus.requestFocus() } }

            // פעימה עדינה של העיגול סביב השעה - הסימן היחיד שמשהו "חי" על המסך.
            val pulse = rememberInfiniteTransition(label = "ringPulse")
            val scale by pulse.animateFloat(
                initialValue = 1f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                label = "ringScale",
            )

            FutureAppTheme(theme) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column(
                        modifier = Modifier.fillMaxSize().background(theme.backgroundColor).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .graphicsLayer { scaleX = scale; scaleY = scale }
                                .clip(CircleShape)
                                .background(theme.readableAccentColor.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(FutureIcons.Alarm, contentDescription = null, tint = theme.readableAccentColor, modifier = Modifier.size(FutureDimens.iconEmptyState * 0.7f))
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        // פני שעון - גרפיקה ולא טקסט ממשק (ר' הערת הדיזיין סיסטם על גדלים מעל hero).
                        Text(
                            "%02d:%02d".format(hour, minute),
                            color = theme.textColor,
                            fontSize = 64.sp,
                            fontWeight = FutureTypography.weightLight,
                            fontFamily = FutureTypography.monoFamily,
                        )
                        Text(
                            label.ifBlank { "שעון מעורר" },
                            color = theme.mutedTextColor,
                            fontSize = FutureTypography.title,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(40.dp))
                        FutureButton(
                            text = "עצור",
                            theme = theme,
                            onClick = ::dismiss,
                            fillMaxWidth = true,
                            focusRequester = stopFocus,
                        )
                        // לטיימר (בלי מזהה אזעקה) אין נודניק - הכפתור לא היה עושה כלום
                        if (alarmId != -1) {
                            Spacer(modifier = Modifier.height(FutureDimens.spacingMd))
                            FutureButton(
                                text = "נודניק · $SNOOZE_MINUTES דקות",
                                theme = theme,
                                onClick = ::snooze,
                                variant = FutureButtonVariant.Secondary,
                                fillMaxWidth = true,
                            )
                        }
                    }
                }
            }
        }
    }

    /** BACK - נודניק; מקשי הווליום משתיקים את הצליל (הרטט ממשיך) בלי לסגור. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        when (event.keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                if (event.action == KeyEvent.ACTION_UP) snooze()
                return true
            }
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (event.action == KeyEvent.ACTION_DOWN) AlarmRingService.send(this, AlarmRingService.ACTION_SILENCE)
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun dismiss() {
        if (handled) return
        handled = true
        AlarmRingService.send(this, AlarmRingService.ACTION_DISMISS)
        finishAndRemoveTask()
    }

    private fun snooze() {
        if (handled) return
        handled = true
        AlarmRingService.send(this, AlarmRingService.ACTION_SNOOZE)
        finishAndRemoveTask()
    }

    companion object {
        const val EXTRA_HOUR = "EXTRA_HOUR"
        const val EXTRA_MINUTE = "EXTRA_MINUTE"
        const val EXTRA_LABEL = "EXTRA_LABEL"
    }
}
