# Navigation (ניווט ותחבורה)

`com.future.navigation` — driving navigation and public-transit journey planner.

Map rendering is MapLibre Native + OpenFreeMap vector tiles (free, no API key, no Google Play Services — this device has none, like the rest of FutureOS). Driving routes/turn-by-turn come from OSRM's public routing API; addresses are geocoded via Nominatim (OpenStreetMap). Public-transit data is a real, geographically-filtered import of the Israeli Ministry of Transport's GTFS static feed (`GtfsImporter`/`GtfsDatabase`/`TransitJourneyPlanner`) — schedule-based departure times, not live vehicle tracking. The map camera and every list are driven entirely by D-pad key events (`NavMapView`/`MapCameraController`), matching the rest of the suite: this device has no touchscreen.

See `GtfsConfig` to change the imported region (defaults to greater Tel Aviv) or the feed URL.
