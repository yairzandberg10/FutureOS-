package com.future.frixa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.future.frixa.data.RecipeCatalog
import com.future.frixa.ui.FrixaRoute
import com.future.frixa.ui.HomeScreen
import com.future.frixa.ui.RecipeDetailScreen
import com.future.frixa.ui.RecipesScreen
import com.future.frixa.ui.StoresScreen
import com.future.sharednav.components.AnimatedScreenHost
import com.future.sharednav.theme.FutureAppTheme
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.onReadableAccentColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.rememberFutureTheme

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית (זהה לכל שאר האפליקציות בסוויטה).
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var route by remember { mutableStateOf<FrixaRoute>(FrixaRoute.Home) }
            val goBack = {
                route = when (route) {
                    is FrixaRoute.RecipeDetail -> FrixaRoute.Recipes
                    else -> FrixaRoute.Home
                }
            }
            BackHandler(enabled = route != FrixaRoute.Home) { goBack() }

            // מתעדכן בזמן אמת כשמצב כהה/בהיר או צבע ההדגשה משתנים (ר' rememberFutureTheme).
            val theme = rememberFutureTheme()

            val tabs = listOf(
                Triple(FrixaRoute.Home, "ראשי", Icons.Rounded.Home),
                Triple(FrixaRoute.Recipes, "מתכונים", Icons.Rounded.Restaurant),
                Triple(FrixaRoute.Stores, "חנויות", Icons.Rounded.Storefront),
            )
            val currentTabRoute = if (route is FrixaRoute.RecipeDetail) FrixaRoute.Recipes else route
            val currentTabIndex = tabs.indexOfFirst { it.first == currentTabRoute }.coerceAtLeast(0)

            // עוגן פוקוס בסיסי - Box בגודל אפס בתוך תוכן המסך (לא על ה-Scaffold
            // עצמו!). בלי פוקוס אמיתי על משהו, DirectionLeft/Right לא מגיעים
            // ל-onKeyEvent של ה-Scaffold בכלל (למשל במסך הבית, שאין בו רכיב
            // פוקוסבילי משלו). אבל למקד את ה-Scaffold עצמו (או כל אב-קדמון של
            // ה-BackHandler) שבר את מקש ה-Back הפיזי - ברגע שה-Scaffold עצמו
            // "מחזיק פוקוס", KEYCODE_BACK כבר לא מגיע ל-OnBackPressedDispatcher.
            // מסכים עם רשימה (מתכונים/חנויות/פרטי מתכון) גוזלים את הפוקוס בחזרה
            // מיד עם ה-LaunchedEffect(Unit) הפנימי שלהם.
            val rootFocusRequester = remember { FocusRequester() }
            LaunchedEffect(route) { rootFocusRequester.requestFocus() }

            FutureAppTheme(theme) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .onKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                            if (route is FrixaRoute.RecipeDetail) return@onKeyEvent false
                            val nextIndex = when (event.key) {
                                Key.DirectionRight -> currentTabIndex - 1
                                Key.DirectionLeft -> currentTabIndex + 1
                                else -> return@onKeyEvent false
                            }
                            if (nextIndex !in tabs.indices) return@onKeyEvent false
                            route = tabs[nextIndex].first
                            true
                        },
                    containerColor = theme.backgroundColor,
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            contentColor = theme.textColor,
                        ) {
                            tabs.forEach { (tabRoute, label, icon) ->
                                NavigationBarItem(
                                    // D-pad ימינה/שמאלה כבר עוברים בין הטאבים (ר' onKeyEvent
                                    // למעלה) - בלי זה, המיקוד ההתחלתי של Compose נוחת על
                                    // אחד מפריטי הסרגל (בגלל RTL, בדרך כלל האחרון - "חנויות"),
                                    // ואז לחיצת OK "לוחצת" עליו ישירות ומדלגת טאב במפתיע.
                                    modifier = Modifier.focusProperties { canFocus = false },
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label) },
                                    selected = currentTabRoute == tabRoute,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = theme.onReadableAccentColor,
                                        selectedTextColor = theme.readableAccentColor,
                                        indicatorColor = theme.readableAccentColor,
                                        unselectedIconColor = theme.mutedTextColor,
                                        unselectedTextColor = theme.mutedTextColor,
                                    ),
                                    onClick = { route = tabRoute }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        color = theme.backgroundColor,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(0.dp)
                                .focusRequester(rootFocusRequester)
                                .focusable(),
                        )
                        // הטאבים באותה רמה (fade); פרטי מתכון עמוק מהם (החלקה).
                        AnimatedScreenHost(
                            targetState = route,
                            depthOf = { if (it is FrixaRoute.RecipeDetail) 1 else 0 },
                        ) { shown ->
                            when (shown) {
                                FrixaRoute.Home -> HomeScreen(theme = theme)
                                FrixaRoute.Recipes -> RecipesScreen(theme = theme, onOpenRecipe = { route = FrixaRoute.RecipeDetail(it) })
                                is FrixaRoute.RecipeDetail -> {
                                    val recipe = RecipeCatalog.all.first { it.id == shown.id }
                                    RecipeDetailScreen(recipe = recipe, theme = theme, onBack = goBack)
                                }
                                FrixaRoute.Stores -> StoresScreen(theme = theme)
                            }
                        }
                    }
                }
            }
        }
    }
}
