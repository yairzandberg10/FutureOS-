package com.future.navigation.ui.gtfs

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.future.navigation.data.common.LatLng
import com.future.navigation.data.gtfs.GtfsConfig
import com.future.navigation.data.gtfs.GtfsImporter
import com.future.navigation.data.gtfs.ImportPhase
import com.future.navigation.data.gtfs.ImportProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * currentLocation/destination הן פונקציות (לא ערכים) כדי לקרוא את המיקום
 * העדכני ביותר בזמן הלחיצה על "עדכן", לא את הערך שהיה כש-ViewModel נבנה.
 */
class GtfsSetupViewModel(
    private val appContext: Context,
    private val importer: GtfsImporter,
    private val currentLocation: () -> LatLng?,
    private val destination: () -> LatLng?
) : ViewModel() {

    private val _progress = MutableStateFlow<ImportProgress?>(null)
    val progress = _progress.asStateFlow()

    private var importing = false

    fun startImport() {
        if (importing) return
        importing = true
        val origin = currentLocation()
        val dest = destination()
        val bbox = if (origin != null && dest != null) {
            GtfsConfig.boundingBoxCovering(origin, dest)
        } else {
            GtfsConfig.TEL_AVIV_BBOX
        }
        viewModelScope.launch {
            importer.importFeed(appContext, bbox).collect { update ->
                _progress.value = update
                if (update.phase == ImportPhase.DONE || update.phase == ImportPhase.ERROR) importing = false
            }
        }
    }
}
