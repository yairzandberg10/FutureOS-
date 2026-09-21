package com.future.fitness.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.future.fitness.bluetooth.HeartRateMonitor
import com.future.fitness.data.UserProfile
import com.future.fitness.data.WorkoutActivityTypes
import com.future.fitness.data.WorkoutRepository
import com.future.fitness.data.WorkoutStore
import com.future.fitness.ui.screens.ActiveWorkoutScreen
import com.future.fitness.ui.screens.ActivityTypesScreen
import com.future.fitness.ui.screens.HealthTipsScreen
import com.future.fitness.ui.screens.HistoryScreen
import com.future.fitness.ui.screens.HomeScreen
import com.future.fitness.ui.screens.ProgressScreen
import com.future.fitness.ui.screens.SettingsScreen
import com.future.fitness.ui.screens.SummaryScreen
import com.future.fitness.ui.screens.WorkoutBuilderScreen
import com.future.fitness.ui.screens.WorkoutDetailScreen
import com.future.fitness.ui.screens.WorkoutsScreen
import com.future.sharednav.theme.FutureTheme

/** מסלולי-שורש שהסרגל התחתון מייצג - מיפוי בשני הכיוונים (route<->tab)
 * לצורך הדגשת הטאב הנוכחי וגם מעבר עם חיצי ימינה/שמאלה. */
private fun Route.asRootTab(): FitnessTab? = when (this) {
    Route.Home -> FitnessTab.HOME
    Route.Workouts -> FitnessTab.WORKOUTS
    Route.Progress -> FitnessTab.PROGRESS
    else -> null
}

private fun FitnessTab.toRoute(): Route = when (this) {
    FitnessTab.HOME -> Route.Home
    FitnessTab.WORKOUTS -> Route.Workouts
    FitnessTab.PROGRESS -> Route.Progress
}

private val TAB_ORDER = listOf(FitnessTab.HOME, FitnessTab.WORKOUTS, FitnessTab.PROGRESS)

@Composable
fun FitnessNavHost(store: WorkoutStore, heartRateMonitor: HeartRateMonitor, theme: FutureTheme) {
    val backStack = remember { mutableStateListOf<Route>(Route.Home) }
    val current = backStack.last()

    BackHandler(enabled = backStack.size > 1) { backStack.removeAt(backStack.lastIndex) }
    fun push(route: Route) = backStack.add(route)
    fun pop() { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }

    // מעבר בין טאבי-שורש (ראשי/אימונים/התקדמות) לא בונה מחסנית שגדלה בלי
    // סוף - מחליף את הראש אם כבר עמוק מ-Home, ומאפס בחזרה ל-[Home] בלבד
    // כשעוברים לטאב "ראשי" (במקום לצבור [Home, Home, Home...]).
    fun switchTab(tab: FitnessTab) {
        val target = tab.toRoute()
        if (target == Route.Home) {
            while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        } else if (backStack.size <= 1) {
            backStack.add(target)
        } else {
            backStack[backStack.lastIndex] = target
        }
    }

    val currentTab = current.asRootTab()
    val currentTabIndex = currentTab?.let { TAB_ORDER.indexOf(it) } ?: -1

    // עוגן פוקוס בסיסי בתוך אזור התוכן (לא על ה-Scaffold עצמו - זה שבר את
    // מקש ה-Back הפיזי, ר' Frixa/MainActivity.kt לתיעוד מלא) - נותן למשהו
    // אמיתי להחזיק פוקוס עם הכניסה לכל מסך, כדי שחיצי ימינה/שמאלה תמיד
    // יגיעו ל-onKeyEvent של ה-Scaffold. כל מסך עם רשימה/תוכן פוקוסבילי משלו
    // (בית/אימונים/התקדמות) גוזל את הפוקוס בחזרה מיד עם LaunchedEffect(Unit)
    // הפנימי שלו.
    val rootFocusRequester = remember { FocusRequester() }
    LaunchedEffect(current) { rootFocusRequester.requestFocus() }

    var statsVersion by remember { mutableIntStateOf(0) }
    val stats = remember(statsVersion) { store.getStats() }
    val history = remember(statsVersion) { store.getHistory() }
    var units by remember { mutableStateOf(store.getUnits()) }
    var profile by remember { mutableStateOf(store.getProfile()) }
    val weightKg = profile.weightKg ?: WorkoutStore.DEFAULT_WEIGHT_KG

    var customWorkoutsVersion by remember { mutableIntStateOf(0) }
    val workouts = remember(customWorkoutsVersion) { WorkoutRepository.workouts + store.getCustomWorkouts() }
    // "האימון הבא" מסתובב על פני כל הקטלוג ככל שמשלימים אימונים (במקום
    // תמיד להצביע על הראשון ברשימה) - שיטה פשוטה וקבועה שלא דורשת לוגיקת
    // תזמון: אחרי N אימונים שהושלמו, ההצעה היא האימון ה-N (מודולו) בקטלוג.
    val nextWorkout = workouts[history.size % workouts.size]

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                if (currentTabIndex < 0) return@onKeyEvent false
                val nextIndex = when (event.key) {
                    Key.DirectionRight -> currentTabIndex - 1
                    Key.DirectionLeft -> currentTabIndex + 1
                    else -> return@onKeyEvent false
                }
                if (nextIndex !in TAB_ORDER.indices) return@onKeyEvent false
                switchTab(TAB_ORDER[nextIndex])
                true
            },
        containerColor = theme.backgroundColor,
        bottomBar = {
            if (currentTab != null) {
                FitnessBottomNav(selectedTab = currentTab, theme = theme)
            }
        },
    ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding).background(theme.backgroundColor)) {
        Box(modifier = Modifier.size(0.dp).focusRequester(rootFocusRequester).focusable())
        // כל push/pop מחליק בכיוון הניווט (ר' AnimatedBackStackHost).
        com.future.sharednav.components.AnimatedBackStackHost(backStack) { route ->
        when (route) {
            is Route.Home -> HomeScreen(
                theme = theme,
                stats = stats,
                history = history,
                nextWorkout = nextWorkout,
                onOpenHistory = { push(Route.History) },
                onOpenSettings = { push(Route.Settings) },
                onOpenHealth = { push(Route.HealthTips) },
                onOpenNextWorkoutDetail = { push(Route.WorkoutDetail(nextWorkout.id)) },
            )

            is Route.Workouts -> WorkoutsScreen(
                workouts = workouts,
                weightKg = weightKg,
                theme = theme,
                onOpenWorkout = { workoutId -> push(Route.WorkoutDetail(workoutId)) },
                onOpenBuilder = { push(Route.WorkoutBuilder) },
                onDeleteCustom = { id ->
                    store.deleteCustomWorkout(id)
                    customWorkoutsVersion++
                },
                onOpenRun = { push(Route.Run) },
                onOpenActivityTypes = { push(Route.ActivityTypes) },
            )

            is Route.WorkoutBuilder -> WorkoutBuilderScreen(
                theme = theme,
                onBack = ::pop,
                onSave = { workout ->
                    store.addCustomWorkout(workout)
                    customWorkoutsVersion++
                    pop()
                },
            )

            is Route.WorkoutDetail -> {
                val workout = workouts.find { it.id == route.workoutId } ?: workouts[0]
                WorkoutDetailScreen(
                    workout = workout,
                    theme = theme,
                    weightKg = weightKg,
                    onBack = ::pop,
                    onStart = { push(Route.ActiveWorkout(workout.id)) },
                )
            }

            is Route.ActiveWorkout -> {
                val workout = workouts.find { it.id == route.workoutId } ?: workouts[0]
                ActiveWorkoutScreen(
                    workout = workout,
                    theme = theme,
                    weightKg = weightKg,
                    heartRateMonitor = heartRateMonitor,
                    onBack = ::pop,
                    onFinish = { minutes, calories, totalSets, avgHr, maxHr ->
                        store.recordCompletedWorkout(workout.name, minutes, calories, avgHr, maxHr)
                        statsVersion++
                        backStack.removeAt(backStack.lastIndex)
                        push(Route.Summary(workout.name, minutes, calories, totalSets, avgHr, maxHr))
                    },
                )
            }

            is Route.Summary -> {
                val stats2 = mutableListOf(route.minutes.toString() to "דקות", route.calories.toString() to "קלוריות", route.totalSets.toString() to "סטים")
                route.avgHr?.let { stats2.add(it.toString() to "דופק ממוצע") }
                SummaryScreen(
                    title = "אימון הושלם!",
                    subtitle = route.workoutName,
                    stats = stats2,
                    theme = theme,
                    onDone = {
                        backStack.clear()
                        backStack.add(Route.Home)
                    },
                )
            }

            is Route.Run -> {
                val freeRunType = remember {
                    com.future.fitness.data.WorkoutActivityType(
                        id = "free_run",
                        displayName = "ריצה חופשית",
                        category = com.future.fitness.data.ActivityCategory.RUNNING,
                        met = 9.0,
                        icon = androidx.compose.material.icons.Icons.AutoMirrored.Rounded.DirectionsRun,
                        usesGps = true,
                    )
                }
                com.future.fitness.ui.screens.WorkoutTemplateScreen(
                    activityType = freeRunType,
                    theme = theme,
                    weightKg = weightKg,
                    age = profile.age,
                    heartRateMonitor = heartRateMonitor,
                    finishLabel = "סיום ריצה",
                    onBack = ::pop,
                    onFinish = { minutes, distanceKm, calories, avgHr, maxHr ->
                        store.recordCompletedWorkout("ריצה חופשית", minutes, calories, avgHr, maxHr, distanceKm)
                        statsVersion++
                        backStack.removeAt(backStack.lastIndex)
                        push(Route.RunSummary(minutes, distanceKm ?: 0.0, calories))
                    },
                )
            }

            is Route.RunSummary -> SummaryScreen(
                title = "ריצה הושלמה!",
                subtitle = "ריצה חופשית",
                stats = listOf(
                    route.minutes.toString() to "דקות",
                    "%.2f".format(route.distanceKm) to "ק״מ",
                    route.calories.toString() to "קלוריות",
                ),
                theme = theme,
                onDone = {
                    backStack.clear()
                    backStack.add(Route.Home)
                },
            )

            is Route.Progress -> ProgressScreen(history = history, stats = stats, theme = theme)

            is Route.HealthTips -> HealthTipsScreen(theme = theme, store = store, onBack = ::pop)

            is Route.Settings -> SettingsScreen(
                theme = theme,
                units = units,
                profile = profile,
                heartRateMonitor = heartRateMonitor,
                onBack = ::pop,
                onSetUnits = { u -> units = u; store.setUnits(u) },
                onSetProfile = { w, a -> profile = UserProfile(w, a); store.setProfile(w, a) },
                onDeviceConnected = { address, name -> store.setPairedDevice(com.future.fitness.data.PairedDevice(address, name)) },
                onDeviceDisconnected = { store.setPairedDevice(null) },
            )

            is Route.History -> HistoryScreen(history = history, theme = theme, onBack = ::pop)

            is Route.ActivityTypes -> ActivityTypesScreen(
                theme = theme,
                onBack = ::pop,
                onSelect = { type ->
                    if (type.usesGps) push(Route.GpsActivity(type.id)) else push(Route.QuickStart(type.id))
                },
            )

            is Route.GpsActivity -> {
                val type = WorkoutActivityTypes.byId(route.activityTypeId) ?: WorkoutActivityTypes.all[0]
                com.future.fitness.ui.screens.WorkoutTemplateScreen(
                    activityType = type,
                    theme = theme,
                    weightKg = weightKg,
                    age = profile.age,
                    heartRateMonitor = heartRateMonitor,
                    finishLabel = "סיום",
                    onBack = ::pop,
                    onFinish = { minutes, distanceKm, calories, avgHr, maxHr ->
                        store.recordCompletedWorkout(type.displayName, minutes, calories, avgHr, maxHr, distanceKm)
                        statsVersion++
                        backStack.removeAt(backStack.lastIndex)
                        push(Route.RunSummary(minutes, distanceKm ?: 0.0, calories))
                    },
                )
            }

            is Route.QuickStart -> {
                val type = WorkoutActivityTypes.byId(route.activityTypeId) ?: WorkoutActivityTypes.all[0]
                com.future.fitness.ui.screens.WorkoutTemplateScreen(
                    activityType = type,
                    theme = theme,
                    weightKg = weightKg,
                    age = profile.age,
                    heartRateMonitor = heartRateMonitor,
                    onBack = ::pop,
                    onFinish = { minutes, _, calories, avgHr, _ ->
                        store.recordCompletedWorkout(type.displayName, minutes, calories, avgHr)
                        statsVersion++
                        backStack.removeAt(backStack.lastIndex)
                        push(Route.QuickStartSummary(type.displayName, minutes, calories, avgHr))
                    },
                )
            }

            is Route.QuickStartSummary -> {
                val stats3 = mutableListOf(route.minutes.toString() to "דקות", route.calories.toString() to "קלוריות")
                route.avgHr?.let { stats3.add(it.toString() to "דופק ממוצע") }
                SummaryScreen(
                    title = "הפעילות הושלמה!",
                    subtitle = route.activityName,
                    stats = stats3,
                    theme = theme,
                    onDone = {
                        backStack.clear()
                        backStack.add(Route.Home)
                    },
                )
            }
        }
        }
    }
    }
}
