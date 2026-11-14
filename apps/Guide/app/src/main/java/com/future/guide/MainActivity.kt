package com.future.guide
import com.future.sharednav.systemui.StatusBarInset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.future.guide.data.findGuideApp
import com.future.guide.ui.GuideDetailScreen
import com.future.guide.ui.GuideHomeScreen
import com.future.guide.ui.GuideRoute
import com.future.sharednav.components.AnimatedScreenHost
import com.future.sharednav.theme.FutureMaterialTheme
import com.future.sharednav.theme.rememberFutureTheme

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית. לא פוגע בניווט/הפעלה במקשים -
    // dispatchKeyEvent הוא נתיב נפרד לגמרי מ-dispatchTouchEvent.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var route by remember { mutableStateOf<GuideRoute>(GuideRoute.Home) }
            // האפליקציה שנפתחה אחרונה - החזרה מהמדריך שלה מסמנת אותה שוב, לא את
            // השורה הראשונה (עם 29 שורות, לחזור לראש הרשימה אחרי כל פריט מתיש).
            var lastOpened by remember { mutableStateOf<String?>(null) }
            BackHandler(enabled = route != GuideRoute.Home) { route = GuideRoute.Home }

            // מתעדכן בזמן אמת כשמצב כהה/בהיר או צבע ההדגשה משתנים (ר' rememberFutureTheme).
            val theme = rememberFutureTheme()

            FutureMaterialTheme(theme) {
                Surface(modifier = Modifier.fillMaxSize(), color = theme.backgroundColor) {
                    AnimatedScreenHost(
                        modifier = Modifier.padding(top = StatusBarInset.TITLE_GAP_DP.dp),
                        targetState = route,
                        depthOf = { if (it is GuideRoute.Detail) 1 else 0 },
                    ) { currentRoute ->
                        when (currentRoute) {
                            GuideRoute.Home -> GuideHomeScreen(
                                theme = theme,
                                focusedAppId = lastOpened,
                                onOpen = { lastOpened = it; route = GuideRoute.Detail(it) },
                            )
                            is GuideRoute.Detail -> {
                                val app = findGuideApp(currentRoute.appId)
                                if (app != null) {
                                    GuideDetailScreen(app = app, theme = theme, onBack = { route = GuideRoute.Home })
                                } else {
                                    // שינוי state בתוך הקומפוזיציה עצמה הוא side effect אסור
                                    LaunchedEffect(currentRoute) { route = GuideRoute.Home }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
