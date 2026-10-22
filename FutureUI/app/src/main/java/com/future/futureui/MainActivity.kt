package com.future.futureui

import com.future.sharednav.theme.FutureTypography
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.core.content.ContextCompat
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import com.future.sharednav.components.FutureButton
import com.future.sharednav.theme.rememberFutureTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.future.futureui.controlcenter.service.ControlCenterAccessibilityService
import com.future.futureui.notificationcenter.service.NotificationCenterAccessibilityService
import com.future.futureui.statusbar.service.StatusBarAccessibilityService
import com.future.futureui.ui.theme.FutureUITheme

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית. לא פוגע בניווט/הפעלה במקשים -
    // dispatchKeyEvent הוא נתיב נפרד לגמרי מ-dispatchTouchEvent.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    // עולה בכל חזרה למסך (למשל מהגדרות הנגישות) - מרענן את סטטוס השירותים
    private val resumeTick = mutableIntStateOf(0)

    override fun onResume() {
        super.onResume()
        resumeTick.intValue++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FutureUITheme {
                var isControlEnabled by remember { mutableStateOf(false) }
                var isNotificationEnabled by remember { mutableStateOf(false) }
                var isStatusBarEnabled by remember { mutableStateOf(false) }

                // מתעדכן גם בחזרה למסך (resumeTick), לא רק בהרכבה הראשונה
                LaunchedEffect(resumeTick.intValue) {
                    isControlEnabled = isAccessibilityServiceEnabled(this@MainActivity, ControlCenterAccessibilityService::class.java)
                    isNotificationEnabled = isAccessibilityServiceEnabled(this@MainActivity, NotificationCenterAccessibilityService::class.java)
                    isStatusBarEnabled = isAccessibilityServiceEnabled(this@MainActivity, StatusBarAccessibilityService::class.java)
                }

                // בקשת ההרשאות המסוכנות שכבר מוצהרות ב-manifest אך לא נשאלות בזמן
                // ריצה בשום מקום אחר. בלי זה שורת המצב זורקת SecurityException על
                // כל בדיקת מצב שיחה כל 15 שניות לנצח, ואייקון השיחה לעולם לא יוצג.
                val dangerousPermissionsLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { /* no-op - הלולאה בשורת המצב תשתמש בהרשאה בסבב הבא */ }

                LaunchedEffect(Unit) {
                    val missing = listOf(
                        android.Manifest.permission.READ_PHONE_STATE,
                        android.Manifest.permission.CAMERA,
                        // חיבור Bluetooth (מצב 'מחובר' בשורת המצב) דורש הרשאת זמן ריצה (SU-02)
                        android.Manifest.permission.BLUETOOTH_CONNECT
                    ).filter {
                        ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                    }
                    if (missing.isNotEmpty()) {
                        dangerousPermissionsLauncher.launch(missing.toTypedArray())
                    }
                }

                val theme = rememberFutureTheme()
                val firstButton = remember { FocusRequester() }
                // פוקוס ראשוני על הכפתור הראשון (F20)
                LaunchedEffect(Unit) { runCatching { firstButton.requestFocus() } }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "ניהול FutureUI", fontSize = FutureTypography.headline, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        ServiceStatusRow("שורת מצב", isStatusBarEnabled)
                        ServiceStatusRow("מרכז בקרה", isControlEnabled)
                        ServiceStatusRow("מרכז התראות", isNotificationEnabled)
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // כפתורי מערכת העיצוב: טבעת פוקוס ברורה. ↑ בראשון ו-↓ באחרון לא
                        // עוטפים לצד השני (N-04).
                        FutureButton(
                            text = "הפעל שירותי נגישות",
                            theme = theme,
                            onClick = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                            modifier = Modifier.focusProperties { up = FocusRequester.Cancel },
                            focusRequester = firstButton,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        FutureButton(
                            text = "התאמה אישית",
                            theme = theme,
                            onClick = { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) },
                            modifier = Modifier.focusProperties { down = FocusRequester.Cancel },
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "הנחיות:\n1. הפעל את השירותים הרצויים.\n2. במסך הנעילה: לחיצה ארוכה על מקש ההגדרות לעריכה.\n3. לחץ OK לביטול נעילה.",
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun ServiceStatusRow(label: String, isEnabled: Boolean) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label)
            Text(
                text = if (isEnabled) "✅ פעיל" else "❌ כבוי",
                color = if (isEnabled) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.error
            )
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        for (info in enabledServices) {
            if (info.resolveInfo.serviceInfo.packageName == context.packageName &&
                info.resolveInfo.serviceInfo.name == service.name) {
                return true
            }
        }
        return false
    }
}
