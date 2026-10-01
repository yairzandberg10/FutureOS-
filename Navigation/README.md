# Navigation (ניווט ותחבורה)

`com.future.navigation` — driving navigation and public-transit journey planner.

Map rendering is MapLibre Native + OpenFreeMap vector tiles (free, no API key, no Google Play Services — this device has none, like the rest of FutureOS). Driving routes/turn-by-turn come from OSRM's public routing API; addresses are geocoded via Nominatim (OpenStreetMap). Public-transit data is a real, geographically-filtered import of the Israeli Ministry of Transport's GTFS static feed (`GtfsImporter`/`GtfsDatabase`/`TransitJourneyPlanner`) — schedule-based departure times, not live vehicle tracking. The map camera and every list are driven entirely by D-pad key events (`NavMapView`/`MapCameraController`), matching the rest of the suite: this device has no touchscreen.

See `GtfsConfig` to change the imported region (defaults to greater Tel Aviv) or the feed URL.

## Paying for a ride

The transit itinerary screen ends with a **Pay for the trip · ₪X** button (`ui/payment`).

- **Fare** (`data/payment/FareCalculator`): the "Derech Shava" distance radii, priced by the air-line distance between the boarding and alighting stops, with the free 90-minute transfer inside the yellow (≤15 km) radius. Israel Railways legs (GTFS `route_type` 2) use the rail column and get no free transfer. Prices are from the official tariff page published by Rav-Pass for the Ministry of Transport (fetched 1 Oct 2026); they change every 25 June and live in one enum.
- **No public payment API exists.** Only operators licensed by the Ministry of Transport charge for rides, so the app hands the user to one of them instead of charging itself:
  - **Rav-Kav Online, in an in-app browser** (`KeypadBrowser`): the "contract reservation" page (card number, profile, contract, credit card; the contract is loaded onto the card later and charged only once loaded). The browser is keypad-driven: the arrows move an on-screen cursor, OK clicks, 2/8 scroll, Back goes back. The user's saved details are filled into empty fields automatically, **only** on `ravkavonline.co.il` over HTTPS, never into password or credit-card fields. The URL can be replaced remotely (`payment_web_url` in Remote Config).
  - **Licensed mobile-validation apps**, shown only when installed: Rav-Pass by HopOn (`co.hopon.client`), Moovit (`com.tranzmate`), ANYWAY by Isracard (`com.isracard.payments`) and Rav-Kav Online (`com.pcentra.ravkavonlinemobile`).
- **My details** (`PaymentProfileStore`): first/last name, ID number (check digit validated), phone, email and Rav-Kav card number, AES-GCM encrypted with an Android Keystore key, on the device only. No credit-card data is ever stored. The details and browser screens are `FLAG_SECURE`.
