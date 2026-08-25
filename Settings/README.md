# Settings

`com.future.settings` — System settings.

Every screen is backed by real device state (no mock data) — Connections (Bluetooth/airplane/mobile data/location/roaming, no Wi-Fi since the target hardware has none), Sound & Display (ringer/volumes/brightness/animations, HSV color picker for accent), Apps (per-app force-stop/clear-data for the `com.future.*` suite), Battery/Storage (real `ACTION_BATTERY_CHANGED`/`StatFs` values), General/Advanced (date/time/locale/accessibility deep-links, root-backed reboot/shutdown), About. A "מערכת FutureOS" section deep-links into FutureUI's and FutureLauncher's own settings activities rather than duplicating their toggles here. Root-shell-backed (`SystemInteractor`) for changes with no public API equivalent.
