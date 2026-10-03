# Navigation (ניווט ותחבורה)

`com.future.navigation` — driving navigation and public-transit journey planner.

Map rendering is MapLibre Native + OpenFreeMap vector tiles (free, no API key, no Google Play Services — this device has none, like the rest of FutureOS). Driving routes/turn-by-turn come from OSRM's public routing API; addresses are geocoded via Nominatim (OpenStreetMap). Public-transit data is a real, geographically-filtered import of the Israeli Ministry of Transport's GTFS static feed (`GtfsImporter`/`GtfsDatabase`/`TransitJourneyPlanner`) — schedule-based departure times, not live vehicle tracking. The map camera and every list are driven entirely by D-pad key events (`NavMapView`/`MapCameraController`), matching the rest of the suite: this device has no touchscreen.

See `GtfsConfig` to change the imported region (defaults to greater Tel Aviv) or the feed URL.

## Paying for a ride

The transit itinerary screen ends with a **Pay for the trip · ₪X** button (`ui/payment`). This is a kosher device: no browser, no Google services, no app store - so payment goes through a phone call.

- **Fare** (`data/payment/FareCalculator`): the "Derech Shava" distance radii, priced by the air-line distance between the boarding and alighting stops, with the free 90-minute transfer inside the yellow (≤15 km) radius. Israel Railways legs (GTFS `route_type` 2) use the rail column and get no free transfer. Prices are from the official tariff page published by Rav-Pass for the Ministry of Transport (fetched 1 Oct 2026); they change every 25 June and live in one enum.
- **No public payment API exists**, and the licensed mobile-validation apps need Google Play. The way to pay from a kosher phone is the **automated Rav-Kav loading line, 03-7207406**: card number, profile, an amount of ₪30-₪300 stored value, credit card; then the card is placed on a loading machine and loads by itself. Stored value only, no passes.
- **"Paying for you"**: the app opens the system dialer with `037207406;<card number>`. After the call connects, the dialer's call screen shows the card number with a **Send** button (`CallService.onPostDialWait` in `dialer`), so the digits go out as tones when the voice menu asks for them - not in the middle of the recorded greeting. The user presses Call themselves; nothing dials on its own.
- **My Rav-Kav number** (`PaymentProfileStore`): AES-GCM encrypted with an Android Keystore key, on the device only. Credit-card details are never stored - they are keyed in during the call. The screen is `FLAG_SECURE`.

### Scan the bus barcode (`ui/scan`, `data/scan`)

Like Moovit and Rav-Pass, but built into the app (no browser, no embedded third-party app). Entry points: the home screen in transit mode, and the trip payment screen.

1. **Scan** - CameraX + ML Kit barcode (the bundled model, same as the Tools scanner, no Google Play Services), QR only. The camera is released when the screen closes. "Choose a line without scanning" skips straight to the list.
2. **Identify the bus** - `BusQrParser` extracts a vehicle (licence plate) number. SIRI Stop Monitoring returns `VehicleRef` (plate) and `LineRef` (= GTFS `route_id`) for every bus arriving at the nearby stops; if the scanned vehicle is there, the line is picked automatically. Otherwise the user picks from the departures at stops within 400 m (GTFS, 15 min back to 45 min ahead, one per line per stop).
3. **Where are you getting off?** - every remaining stop of that trip, each with its exact fare (`FareCalculator`).
4. **Confirm and pay** through `FarePaymentGateway.current`, the only integration point. There is no public fare-payment API: charging goes through the Ministry of Transport clearing system, which only licensed operators reach (Pango, HopOn Rav-Pass, Cellopark, Isracard, Rav-Kav Online). Until there is an agreement with one of them, `NotConnectedGateway` charges nothing and issues no ticket, and the screen says so. Connecting one = writing one class against its API and pointing `current` at it.

**Open items**

- The QR sticker format is not published. `BusQrParser` is deliberately cautious (a `vehicle`-like URL parameter, or a single standalone 7-8 digit run) and returns no vehicle otherwise - the flow then falls back to picking the line. Add a real scanned sample to `BusQrParserTest` and adjust the parser.
- SIRI `MonitoringRef` is sent as the GTFS `stop_id`, like the existing real-time enrichment does. If the MoT service expects `stop_code` instead, automatic line identification (and the existing real-time delays) will not match, and the manual list is used.

