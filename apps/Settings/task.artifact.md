# Tasks - Future Phone Hardware "Full Activation" (V7)

- [x] Robustify `SystemInteractor.kt` with root permission granting and getters
- [x] Implement `syncWithSystem()` in `SettingsViewModel.kt` to load current phone state
- [x] Link `ConnectionsScreen` UI to real Airplane/Data/Bluetooth states
- [x] Link `DisplayScreen` UI to real Brightness state
- [x] Link `SoundScreen` UI to real Volume/Ringer states
- [x] Map all remaining `onClick` handlers in `SettingsScreens.kt` to real actions (or feedback toasts)
- [x] Trigger Root activation and initial sync from `MainActivity.kt` via `isReady` state
- [x] Verify focus frames and DPAD navigation on all updated screens
- [x] Add loading splash screen for clean startup
- [x] Build and verify on device (Build successful)
