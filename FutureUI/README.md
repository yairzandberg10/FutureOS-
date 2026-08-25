# FutureUI

`com.future.futureui` — FutureOS's System UI.

Since FutureOS runs on stock Android 12 rather than a custom ROM, "system UI" here means four `AccessibilityService`-based overlay windows drawn on top of everything else:

- **Status bar** — persistent (shown from `onServiceConnected`), clock/battery/connectivity icons, intercepts volume keys with its own overlay instead of the stock dialog.
- **Lock screen** — clock, notifications, keypad-shortcut launchers, long-press to enter layout-edit mode.
- **Control Center** — quick toggles (Bluetooth, DND, airplane mode, brightness/volume sliders, etc.), long-press to add/remove/reorder.
- **Notification Center** — listens via `NotificationListenerService`.

Many toggles are backed by root shell commands (`ControlManager`/`SystemInteractor`, device is expected to be rooted) rather than public APIs where no public API exists. Also hosts a settings screen (`SettingsActivity`, exported) for customizing the other three overlays, and cross-app broadcasts (e.g. `ACTION_OPTIONS_SHORT_PRESS`) so other FutureOS apps can react to hardware keys this service otherwise swallows system-wide.
