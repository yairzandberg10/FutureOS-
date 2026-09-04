package com.future.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.future.navigation.data.common.LatLng
import com.future.navigation.data.geocoding.GeocodingRepository
import com.future.navigation.data.gtfs.GtfsConfig
import com.future.navigation.data.gtfs.GtfsDatabase
import com.future.navigation.data.gtfs.GtfsImportState
import com.future.navigation.data.gtfs.GtfsImporter
import com.future.navigation.data.gtfs.ImportPhase
import com.future.navigation.data.gtfs.TransitJourneyPlanner
import com.future.navigation.data.places.SavedPlaceRepository
import com.future.navigation.data.routing.OsrmRoutingRepository
import com.future.navigation.data.routing.RoutingRepository
import com.future.sharednav.theme.ThemeClient
import com.future.navigation.ui.gtfs.GtfsSetupScreen
import com.future.navigation.ui.gtfs.GtfsSetupViewModel
import com.future.navigation.ui.home.HomeScreen
import com.future.navigation.ui.home.HomeViewModel
import com.future.navigation.ui.navigate.NavigateScreen
import com.future.navigation.ui.navigate.NavigateViewModel
import com.future.navigation.ui.navigation.Destination
import com.future.navigation.ui.navigation.NavSessionViewModel
import com.future.navigation.ui.navigation.Screen
import com.future.navigation.ui.navigation.TravelMode
import com.future.navigation.ui.routes.RouteOptionsScreen
import com.future.navigation.ui.routes.RouteOptionsViewModel
import com.future.navigation.ui.saved.SavedPlacesScreen
import com.future.navigation.ui.saved.SavedPlacesViewModel
import com.future.navigation.ui.theme.NavigationTheme
import com.future.navigation.ui.transit.TransitScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית, בדיוק כמו בשאר אפליקציות FutureOS.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    private val gtfsDatabase by lazy { GtfsDatabase.getInstance(this) }
    private val savedPlaceRepository by lazy { SavedPlaceRepository(gtfsDatabase.savedPlaceDao()) }
    private val geocodingRepository by lazy { GeocodingRepository() }
    private val routingRepository: RoutingRepository by lazy { OsrmRoutingRepository() }
    private val transitJourneyPlanner by lazy { TransitJourneyPlanner(gtfsDatabase.gtfsDao()) }
    private val gtfsImporter by lazy { GtfsImporter(gtfsDatabase) }

    private val navSessionViewModel: NavSessionViewModel by viewModels()

    private val homeViewModel: HomeViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(applicationContext, savedPlaceRepository, gtfsDatabase.gtfsDao(), geocodingRepository) as T
            }
        }
    }

    private val routeOptionsViewModel: RouteOptionsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return RouteOptionsViewModel(applicationContext, routingRepository, transitJourneyPlanner, gtfsImporter) as T
            }
        }
    }

    private val savedPlacesViewModel: SavedPlacesViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SavedPlacesViewModel(savedPlaceRepository, geocodingRepository) as T
            }
        }
    }

    private val gtfsSetupViewModel: GtfsSetupViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return GtfsSetupViewModel(
                    applicationContext,
                    gtfsImporter,
                    currentLocation = { homeViewModel.currentLocation.value },
                    destination = { navSessionViewModel.destination.value?.location }
                ) as T
            }
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            homeViewModel.refreshLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var sharedTheme by remember { mutableStateOf(ThemeClient.getTheme(this)) }
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        sharedTheme = ThemeClient.getTheme(this@MainActivity)
                        homeViewModel.refreshLocation()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            var hasLocationPermission by remember { mutableStateOf(hasLocationPermission()) }
            LaunchedEffect(Unit) {
                if (!hasLocationPermission) {
                    locationPermissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                }
            }
            // מתחיל להוריד/לייבא נתוני תחבורה ציבורית ברקע ברגע שהמיקום ידוע לראשונה
            // - לא מחכים לזה שהמשתמש יגיע למסך תוצאות מסלול (ר' prefetchTransitDataInBackground).
            LaunchedEffect(Unit) {
                homeViewModel.currentLocation.collect { location ->
                    if (location != null) prefetchTransitDataInBackground(location)
                }
            }
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        hasLocationPermission = hasLocationPermission()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            NavigationTheme(isDarkMode = sharedTheme.isDarkMode, accentColor = Color(sharedTheme.primaryColor)) {
                // ה-windowBackground ב-themes.xml קבוע לשחור (ר' ההערה שם) ולא זז לפי
                // מצב יום/לילה - בלי ה-Surface הזה, כשה-theme המשותף עובר לבהיר
                // (lightColorScheme: טקסט כהה, משטחים בהירים) הרקע שמאחורי הכל היה
                // נשאר שחור וכל התוכן הופך לבלתי-קריא (למשל טקסט שחור על שחור).
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!hasLocationPermission) {
                        MissingLocationPermissionScreen { locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
                    } else {
                        AppNavHost(
                            navSessionViewModel = navSessionViewModel,
                            homeViewModel = homeViewModel,
                            routeOptionsViewModel = routeOptionsViewModel,
                            savedPlacesViewModel = savedPlacesViewModel,
                            gtfsSetupViewModel = gtfsSetupViewModel,
                            routingRepository = routingRepository
                        )
                    }
                }
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private var transitPrefetchStarted = false

    /**
     * מריץ ברקע (בלי לחסום שום מסך) הורדה/ייבוא של נתוני GTFS סביב המיקום
     * הנוכחי, ברגע שהמיקום ידוע - כדי שכשמשתמש בפועל יבחר יעד לתחבורה
     * ציבורית, הנתונים כבר יהיו מוכנים (או כבר באמצע הורדה) ברוב המקרים,
     * במקום לגרום למסך תוצאות המסלול לחכות. ר' RouteOptionsViewModel
     * ל"רשת הביטחון" שמשלימה הורדה ממוקדת-יעד אם היעד נופל מחוץ לאזור הזה.
     */
    private fun prefetchTransitDataInBackground(location: LatLng) {
        if (transitPrefetchStarted) return
        val neededBbox = GtfsConfig.boundingBoxCovering(location, location, paddingDegrees = PREFETCH_PADDING_DEGREES)
        val alreadyImported = GtfsImportState.lastImportedBoundingBox(applicationContext)
        if (alreadyImported != null && alreadyImported.covers(neededBbox)) return

        transitPrefetchStarted = true
        lifecycleScope.launch {
            gtfsImporter.importFeed(applicationContext, neededBbox).collect { progress ->
                if (progress.phase == ImportPhase.DONE) {
                    GtfsImportState.recordImportedBoundingBox(applicationContext, neededBbox)
                } else if (progress.phase == ImportPhase.ERROR) {
                    transitPrefetchStarted = false
                }
            }
        }
    }

    companion object {
        private const val PREFETCH_PADDING_DEGREES = 0.3
    }
}

@Composable
private fun MissingLocationPermissionScreen(onRequest: () -> Unit) {
    // מכשיר היעד חסר מסך מגע לחלוטין - כפתור בודד חייב לקבל פוקוס באופן
    // יזום כדי שאפשר יהיה בכלל להפעיל אותו דרך ה-D-pad (ר' התבנית הזהה
    // ב-Music/ui/screens/PermissionScreen.kt).
    val buttonFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        buttonFocusRequester.requestFocus()
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.location_permission_required),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRequest,
                modifier = Modifier.focusRequester(buttonFocusRequester)
            ) { Text(androidx.compose.ui.res.stringResource(R.string.grant_permission)) }
        }
    }
}

@Composable
private fun AppNavHost(
    navSessionViewModel: NavSessionViewModel,
    homeViewModel: HomeViewModel,
    routeOptionsViewModel: RouteOptionsViewModel,
    savedPlacesViewModel: SavedPlacesViewModel,
    gtfsSetupViewModel: GtfsSetupViewModel,
    routingRepository: RoutingRepository
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                onDestinationPicked = { result ->
                    navSessionViewModel.setMode(homeViewModel.mode.value)
                    navSessionViewModel.setDestination(
                        Destination(name = result.label.substringBefore(","), address = result.label, location = result.location)
                    )
                    navController.navigate(Screen.RouteOptions.route)
                },
                onOpenSavedPlaces = { navController.navigate(Screen.SavedPlaces.route) },
                onOpenGtfsSetup = { navController.navigate(Screen.GtfsSetup.route) }
            )
        }

        composable(Screen.RouteOptions.route) {
            val mode by navSessionViewModel.mode.collectAsState()
            val destination by navSessionViewModel.destination.collectAsState()
            val dest = destination
            if (dest == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                RouteOptionsScreen(
                    viewModel = routeOptionsViewModel,
                    mode = mode,
                    destination = dest,
                    onBack = { navController.popBackStack() },
                    onStartDriving = { route ->
                        navSessionViewModel.setSelectedDrivingRoute(route)
                        navController.navigate(Screen.Navigate.route)
                    },
                    onStartTransit = { itinerary ->
                        navSessionViewModel.setSelectedItinerary(itinerary)
                        navController.navigate(Screen.Transit.route)
                    }
                )
            }
        }

        composable(Screen.Navigate.route) {
            val route = navSessionViewModel.selectedDrivingRoute.collectAsState().value
            val destination = navSessionViewModel.destination.collectAsState().value
            if (route == null || destination == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                val context = androidx.compose.ui.platform.LocalContext.current
                val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<NavigateViewModel>(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return NavigateViewModel(context.applicationContext, routingRepository, route, destination.location) as T
                        }
                    }
                )
                NavigateScreen(viewModel, onClose = { navController.popBackStack(Screen.Home.route, inclusive = false) })
            }
        }

        composable(Screen.Transit.route) {
            val itinerary = navSessionViewModel.selectedItinerary.collectAsState().value
            if (itinerary == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                TransitScreen(itinerary = itinerary, onBack = { navController.popBackStack() })
            }
        }

        composable(Screen.SavedPlaces.route) {
            SavedPlacesScreen(
                viewModel = savedPlacesViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToPlace = { place ->
                    navSessionViewModel.setMode(homeViewModel.mode.value)
                    navSessionViewModel.setDestination(
                        Destination(name = place.label, address = place.address, location = LatLng(place.lat, place.lon))
                    )
                    navController.navigate(Screen.RouteOptions.route)
                }
            )
        }

        composable(Screen.GtfsSetup.route) {
            GtfsSetupScreen(viewModel = gtfsSetupViewModel, onBack = { navController.popBackStack() })
        }
    }
}
