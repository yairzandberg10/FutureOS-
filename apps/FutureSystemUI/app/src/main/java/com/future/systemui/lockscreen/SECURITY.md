# FutureOS lock screen - security review

The lock screen is FutureOS's own layer (stock keyguard and its credentials are untouched). It runs FutureUI's lock
UI (fui/lockscreen) inside SystemUI. The PIN stays in FutureUI: SystemUI asks FutureUI's provider
(`content://com.future.futureui.systemui`, `lock_verify`) to check it, so the user's existing PIN keeps working and no
PIN or HMAC is ever copied. `com.future.futureui.permission.SYSTEM_SETTINGS` was changed from `signature` to
`signature|privileged` so that the platform-signed priv-app SystemUI can call it.

## Issues found in FutureUI's lock screen and the fix

| # | Issue | Fix |
|---|-------|-----|
| 1 | The lock was a `TYPE_ACCESSIBILITY_OVERLAY` window of an accessibility service. If the service was disabled, killed or not yet started, there was no lock at all. | Lock is created by SystemUI itself (a persistent system process) and started in `LockScreenFeature.start` (boot and after every SystemUI restart/crash). `HandoverFeature` switching the FutureUI services off no longer matters. |
| 2 | The window was `NOT_FOCUSABLE` and keys came from the accessibility key filter. | Window is focusable and `LockView.dispatchKeyEvent` consumes every key (only VOLUME/POWER, which the system handles before dispatch, pass). |
| 3 | The window was `NOT_TOUCHABLE`: touches fell through to the app under the lock. | Touches are swallowed by the window. |
| 4 | Window layer: below the status bar / shade in some cases, could be covered by them. | `TYPE_KEYGUARD_DIALOG` (2009, above shade and status bar, no singleton rule), fallback `TYPE_STATUS_BAR_SUB_PANEL`, `TYPE_NOTIFICATION_SHADE`, `TYPE_APPLICATION_OVERLAY`. If every type fails the controller keeps retrying (2 s) and the disable flags below stay on. |
| 5 | Status bar pull-down, home, recents, search stayed usable behind the overlay. | While secured: `IStatusBarService.disable(EXPAND\|NOTIFICATION_ALERTS\|HOME\|RECENT\|SEARCH)` and `disable2(QUICK_SETTINGS\|NOTIFICATION_SHADE)`, cleared on unlock. The binder token dies with the process, so a SystemUI crash can not leave flags stuck. |
| 6 | Other UI could show private content (shade, recents, heads-up). | Public `LockState.locked` / `LockState.secured` (start as `true`, fail closed) plus the existing `FutureUIState.isLocked/isSecured`. |
| 7 | Auto-lock delay applied even with a PIN: after screen off the device stayed open for up to 5 minutes. | With a PIN the device locks on `ACTION_SCREEN_OFF` immediately. The delay only applies to a device without a PIN. |
| 8 | No lock at boot until the accessibility service connected. | `start()` locks immediately when a PIN exists. "Has PIN" is fail-closed: unknown + FutureUI installed means locked. |
| 9 | Failed-attempt counter lived in one store, written after the check. | `LockoutGuard` (own store, `commit()` BEFORE the check, removed only on success, refunded only when FutureUI did not answer). 5 free tries then 30 s, 1, 2, 4, 8, 15 min (cap). Persisted across reboot (elapsed clock in the same boot, wall clock after a reboot, capped). FutureUI's own lockout still applies on top; the longer one wins. |
| 10 | PIN check ran on the main thread (a blocked keystore froze the lock). | Check runs on a dedicated thread; result is dropped if the session changed (screen off) meanwhile. |
| 11 | Wrong-PIN logs carried exceptions of the PIN path. | No PIN, HMAC or argument is logged anywhere; failures log only the exception class. |
| 12 | PIN comparison. | Done inside FutureUI: HMAC-SHA256 under a non-exportable Keystore key and `MessageDigest.isEqual` (constant time). SystemUI never compares PINs. The digit buffer is overwritten after use. |
| 13 | Lock settings token (10 min session) stayed valid after the lock screen verified a PIN. | The lock screen ends the FutureUI session right after reading the settings. |
| 14 | If FutureUI is slow/unavailable the old code would fail open or count it as a failed guess. | Unavailable = not counted, message shown, retry every 2 s; device stays locked. |
| 15 | Face unlock fallback bypass: after a reboot / 48 h / 5 failures PIN is required. | Kept as is (`strongAuthRequired`). |
| 16 | Call/alarm suspension relied on accessibility window events. | `LockScreenFeature` polls the top activity (700 ms locked, 250 ms while stepped aside) and the telephony state; only the in-call dialer, the alarm ring screen and the emergency dialer are allowed, anything else brings the lock back. |
| 17 | No emergency call from the lock. | CALL key on the PIN screen starts the emergency dialer (`com.android.phone.EmergencyDialer.DIAL`); the lock steps aside only when that app is really in front. Works during a lockout. |
| 18 | `LockRemote` (settings server) lived next to the PIN. | In SystemUI it is inert (`handle` returns null); the face-enroll activity validates the FutureUI token against FutureUI. |

## Residual risks / notes

* While the lock steps aside for a call/alarm there is up to 250 ms between a foreign app coming to the front and the
  lock returning.
* HOME/POWER are handled by the system before the window; HOME starts the launcher behind the lock (the lock stays on top).
* Any app holding `SYSTEM_SETTINGS` (now also priv-apps) can call `lock_verify`; FutureUI's lockout limits guessing.
* Lock appearance settings (clock, colours, widgets, shortcuts, privacy, owner message, enabled) are copied from
  FutureUI after a correct PIN (or when no PIN is set). FutureUI does not expose the wallpaper id or face data.
* Face templates are not shared with FutureUI (separate Keystore): face unlock works only after enrolling inside SystemUI
  (`fui.lockscreen.settings.LockSettingsActivity`).
