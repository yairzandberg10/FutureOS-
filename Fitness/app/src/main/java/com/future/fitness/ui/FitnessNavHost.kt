package com.future.fitness.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.future.fitness.ui.screens.QuickStartScreen
import com.future.fitness.ui.screens.RunScreen
import com.future.fitness.ui.screens.SettingsScreen
import com.future.fitness.ui.screens.SummaryScreen
import com.future.fitness.ui.screens.WorkoutBuilderScreen
import com.future.fitness.ui.screens.WorkoutDetailScreen
import com.future.fitness.ui.screens.WorkoutsScreen
import com.future.fitness.ui.theme.FutureTheme

@Composable
fun FitnessNavHost(store: WorkoutStore, heartRateMonitor: HeartRateMonitor, theme: FutureTheme) {
    val backStack = remember { mutableStateListOf<Route>(Route.Home) }
    val current = backStack.last()

    BackHandler(enabled = backStack.size > 1) { backStack.removeAt(backStack.lastIndex) }
    fun push(route: Route) = backStack.add(route)
    fun pop() { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }

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

    Box(modifier = Modifier.fillMaxSize()) {
        when (val route = current) {
            is Route.Home -> HomeScreen(
                theme = theme,
                stats = stats,
                nextWorkout = nextWorkout,
                onOpenWorkouts = { push(Route.Workouts) },
                onOpenHistory = { push(Route.History) },
                onOpenProgress = { push(Route.Progress) },
                onOpenSettings = { push(Route.Settings) },
                onOpenRun = { push(Route.Run) },
                onOpenHealth = { push(Route.HealthTips) },
                onOpenNextWorkoutDetail = { push(Route.WorkoutDetail(nextWorkout.id)) },
                onOpenActivityTypes = { push(Route.ActivityTypes) },
            )

            is Route.Workouts -> WorkoutsScreen(
                workouts = workouts,
                theme = theme,
                onBack = ::pop,
                onOpenWorkout = { workoutId -> push(Route.WorkoutDetail(workoutId)) },
                onOpenBuilder = { push(Route.WorkoutBuilder) },
                onDeleteCustom = { id ->
                    store.deleteCustomWorkout(id)
                    customWorkoutsVersion++
                },
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

            is Route.Run -> RunScreen(
                theme = theme,
                heartRateMonitor = heartRateMonitor,
                weightKg = weightKg,
                onBack = ::pop,
                onFinish = { minutes, distanceKm, calories ->
                    store.recordCompletedWorkout("ריצה חופשית", minutes, calories, distanceKm = distanceKm)
                    statsVersion++
                    backStack.removeAt(backStack.lastIndex)
                    push(Route.RunSummary(minutes, distanceKm, calories))
                },
            )

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

            is Route.Progress -> ProgressScreen(history = history, theme = theme, onBack = ::pop)

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
                RunScreen(
                    theme = theme,
                    heartRateMonitor = heartRateMonitor,
                    weightKg = weightKg,
                    title = type.displayName,
                    met = type.met,
                    finishLabel = "סיום",
                    onBack = ::pop,
                    onFinish = { minutes, distanceKm, calories ->
                        store.recordCompletedWorkout(type.displayName, minutes, calories, distanceKm = distanceKm)
                        statsVersion++
                        backStack.removeAt(backStack.lastIndex)
                        push(Route.RunSummary(minutes, distanceKm, calories))
                    },
                )
            }

            is Route.QuickStart -> {
                val type = WorkoutActivityTypes.byId(route.activityTypeId) ?: WorkoutActivityTypes.all[0]
                QuickStartScreen(
                    activityType = type,
                    theme = theme,
                    weightKg = weightKg,
                    heartRateMonitor = heartRateMonitor,
                    onBack = ::pop,
                    onFinish = { minutes, calories, avgHr, _ ->
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
