package com.future.navigation.ui.scan

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.future.navigation.R
import com.future.navigation.data.common.LatLng
import com.future.navigation.data.payment.FarePaymentGateway
import com.future.navigation.data.payment.FarePaymentRequest
import com.future.navigation.data.payment.FarePaymentResult
import com.future.navigation.data.scan.AlightingOption
import com.future.navigation.data.scan.BoardingOption
import com.future.navigation.data.scan.BusQr
import com.future.navigation.data.scan.BusQrParser
import com.future.navigation.data.scan.BusScanRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** השלבים של "סורקים ומשלמים", לפי הסדר. */
sealed interface ScanStep {
    data object Scanning : ScanStep
    data object Identifying : ScanStep
    data class PickLine(val qr: BusQr, val options: List<BoardingOption>) : ScanStep

    /** [stops] = null בזמן טעינה. [identified] - הקו נבחר אוטומטית לפי מספר הרכב. */
    data class PickStop(
        val qr: BusQr,
        val options: List<BoardingOption>,
        val boarding: BoardingOption,
        val identified: Boolean,
        val stops: List<AlightingOption>?,
    ) : ScanStep

    data class Confirm(val pick: PickStop, val alighting: AlightingOption) : ScanStep
    data class Paying(val confirm: Confirm) : ScanStep
    data class Done(val confirm: Confirm, val result: FarePaymentResult) : ScanStep
    data class Problem(@StringRes val message: Int) : ScanStep
}

/**
 * סריקה -> זיהוי הקו (SIRI לפי מספר הרכב, אחרת בחירה מהרשימה) -> תחנת
 * ירידה עם מחיר -> אישור -> תשלום דרך [FarePaymentGateway].
 */
class BusScanViewModel(
    private val repository: BusScanRepository,
    private val gateway: FarePaymentGateway,
    private val currentLocation: () -> LatLng?,
) : ViewModel() {

    private val _step = MutableStateFlow<ScanStep>(ScanStep.Scanning)
    val step: StateFlow<ScanStep> = _step.asStateFlow()

    private var job: Job? = null

    fun onScanned(raw: String) {
        if (_step.value != ScanStep.Scanning) return
        start(BusQrParser.parse(raw))
    }

    /** ברקוד קרוע, אין מצלמה, או סתם מהר יותר - ישר לרשימת הקווים. */
    fun skipScan() {
        if (_step.value != ScanStep.Scanning) return
        start(BusQr(raw = "", vehicleNumber = null))
    }

    private fun start(qr: BusQr) {
        _step.value = ScanStep.Identifying
        job = viewModelScope.launch {
            val location = currentLocation()
            if (location == null) {
                _step.value = ScanStep.Problem(R.string.scan_no_location)
                return@launch
            }
            val options = runCatching { repository.boardingOptions(location, System.currentTimeMillis() / 1000) }
                .getOrDefault(emptyList())
            val identified = if (options.isEmpty()) null else runCatching { repository.identify(qr, options) }.getOrNull()
            if (identified != null) showStops(qr, options, identified, identified = true)
            else _step.value = ScanStep.PickLine(qr, options)
        }
    }

    fun selectLine(option: BoardingOption) {
        val s = _step.value as? ScanStep.PickLine ?: return
        showStops(s.qr, s.options, option, identified = false)
    }

    /** "זה לא הקו שלי" - מהזיהוי האוטומטי חזרה לרשימה. */
    fun changeLine() {
        val s = _step.value as? ScanStep.PickStop ?: return
        job?.cancel()
        _step.value = ScanStep.PickLine(s.qr, s.options)
    }

    private fun showStops(qr: BusQr, options: List<BoardingOption>, boarding: BoardingOption, identified: Boolean) {
        val pick = ScanStep.PickStop(qr, options, boarding, identified, stops = null)
        _step.value = pick
        job = viewModelScope.launch {
            val stops = runCatching { repository.alightingOptions(boarding) }.getOrDefault(emptyList())
            if (_step.value == pick) _step.value = pick.copy(stops = stops)
        }
    }

    fun selectStop(stop: AlightingOption) {
        val s = _step.value as? ScanStep.PickStop ?: return
        if (stop.fare.agorot == null) return
        _step.value = ScanStep.Confirm(s, stop)
    }

    fun pay() {
        val c = _step.value as? ScanStep.Confirm ?: return
        val agorot = c.alighting.fare.agorot ?: return
        _step.value = ScanStep.Paying(c)
        val boarding = c.pick.boarding
        val request = FarePaymentRequest(
            qrRaw = c.pick.qr.raw,
            vehicleNumber = c.pick.qr.vehicleNumber,
            routeId = boarding.departure.routeId,
            lineName = boarding.lineName,
            tripId = boarding.departure.tripId,
            boardingStopId = boarding.stop.stopId,
            alightingStopId = c.alighting.stop.stopId,
            distanceKm = c.alighting.fare.distanceKm ?: 0.0,
            agorot = agorot,
        )
        job = viewModelScope.launch {
            val result = runCatching { gateway.pay(request) }
                .getOrElse { FarePaymentResult.Declined(it.message.orEmpty()) }
            _step.value = ScanStep.Done(c, result)
        }
    }

    fun restart() {
        job?.cancel()
        _step.value = ScanStep.Scanning
    }

    /** צעד אחורה. false = כבר בצעד הראשון (או בסוף), והמסך צריך להיסגר. */
    fun back(): Boolean = when (val s = _step.value) {
        ScanStep.Scanning -> false
        ScanStep.Identifying, is ScanStep.PickLine, is ScanStep.Problem -> { restart(); true }
        is ScanStep.PickStop -> {
            job?.cancel()
            if (s.options.isEmpty()) restart() else _step.value = ScanStep.PickLine(s.qr, s.options)
            true
        }
        is ScanStep.Confirm -> { _step.value = s.pick; true }
        // התשלום כבר נשלח - לא חוזרים באמצע ולא שולחים פעמיים.
        is ScanStep.Paying -> true
        is ScanStep.Done -> false
    }
}
