# FutureOS protection against attacks: design and the AOSP move

Status: running on the QA device, a rooted Android 12 phone, since 2026-11-13.
Code lives in `FutureUI/.../security/`. The UI is in Settings › Security.

## What runs where

| Part | Where | What it does |
|---|---|---|
| `ThreatScanner` | FutureUI | Finds foreign apps (not system, not FutureOS-signed) with dangerous capabilities. Also matches the known-malware list, sideloaded installs and suspicious combinations. |
| `ThreatGuard` | FutureUI | Policy: baseline, allowed, blocked, auto-block. The state is HMAC-signed with an Android Keystore key. |
| `ThreatBlocker` | FutureUI | Revokes one capability, or suspends an app, and stores what it needs to undo the change. |
| `ThreatWatcher` | FutureUI | Rescans on setting changes, installs, screen on and trust-store changes, then alerts. |
| `SecurityAudit` | FutureUI | Local audit log, encrypted with AES-256-GCM using a Keystore key. Shown in Settings › Security › Security history. |
| `MalwareIndicators` | FutureUI | Known-stalkerware list. Uses the bundled copy, or a signed update if it is newer and valid. |
| `MalwareDbUpdateJob` | Settings | Downloads the signed list from the repo once a day and hands it to FutureUI. FutureUI verifies it. |

Trust is decided by comparing certificate digests (`SameSigner`), never by
`checkSignatures`. On the QA ROM, `checkSignatures` matches any pair of apps.

## Sideloaded installs

- An app installed by anything other than a known store becomes a
  `SIDELOADED` finding and raises an alert.
- If it is also a known-malware match, or a new suspicious combination, it is
  suspended right after install, before its first launch. It stays suspended
  until the user approves it in Settings.
- **AOSP:** checking *before* the install finishes needs a package
  verification agent. That means a privileged app holding
  `PACKAGE_VERIFICATION_AGENT` and handling `PACKAGE_NEEDS_VERIFICATION`. FutureUI
  would answer `VERIFICATION_REJECT` for a malware match. This is not
  possible on the QA device without root hooks, so it is not built yet.

## Suspicious combinations

These heuristics live in `ThreatScanner.suspicion`. Scoring:

| Signal | Points |
|---|---|
| Accessibility enabled | 3 |
| Notification listener | 2 |
| SMS | 2 |
| Device admin | 2 |
| Overlay | 1 |
| Install apps | 1 |
| No launcher icon | 2 |
| Sideloaded | 2 |

An app is flagged when either rule holds:
- it has INTERNET, at least two takeover capabilities, and a score of 5 or more;
- it is sideloaded, hidden, and has at least one takeover capability.

A Play-installed watch app that reads notifications and SMS scores 4 and is
not flagged.

## Signed malware-list updates

- `security/malware-db/update.sh` downloads Echap's `ioc.yaml` and converts it
  with `convert.py`. It signs the result with ECDSA P-256 / SHA-256.
- The private key is at `~/.futureos/malware-db-signing-key.pem`. It is never
  in the repo and never on the phone.
- The public key is embedded in `MalwareIndicators.PUBLIC_KEY`.
- The phone accepts an update only if all of these hold:
  - the signature is valid;
  - the date is newer than the current list (no rollback);
  - the file is well formed and under 4 MB.
- The stored update is verified again on every load.
- Ed25519 was not used because Android 12 has no platform Ed25519 verifier.
  Switch when the AOSP base is Android 13 or newer, or keep ECDSA. Either is
  fine.
- Phones fetch from the `main` branch. Updates start reaching phones only
  after this branch is merged.

## Tamper-evident state

- The guard state is signed with an HMAC-SHA256 Keystore key. The state
  covers what was seen, the baseline, what is allowed, what is blocked, and
  the auto-block switch.
- An edit from outside FutureUI fails the check. The allowed list and the
  baseline are then wiped (fail closed). The event is logged and the user is
  alerted.
- On the QA device only root can edit FutureUI's private files. The app
  sandbox already keeps other apps out.
- **AOSP:** move the state into a system service, or into `/data/system/…`
  owned by `system`. FutureUI then becomes a client of that service. Root is
  gone on user builds, so the Keystore MAC becomes a second layer.

## Root (QA only) → AOSP APIs

| Action | QA device (root) | AOSP (privileged FutureUI) |
|---|---|---|
| Remove an accessibility service | `settings put secure enabled_accessibility_services` | `Settings.Secure.putString` + `WRITE_SECURE_SETTINGS` |
| Revoke notification access | `cmd notification disallow_listener` | `NotificationManager.setNotificationListenerAccessGranted` (system) |
| Revoke SMS | `pm revoke` | `PackageManager.revokeRuntimePermission` + `REVOKE_RUNTIME_PERMISSIONS` |
| Overlay / install apps | `appops set … ignore` | `AppOpsManager.setMode` + `MANAGE_APP_OPS_MODES` |
| Suspend an app | `pm suspend` | `PackageManager.setPackagesSuspended` + `SUSPEND_APPS` |
| Read app-ops of other apps | `appops get` | `AppOpsManager.unsafeCheckOpNoThrow` + `GET_APP_OPS_STATS` |
| Proxy reset | `settings put global http_proxy :0` | `Settings.Global.putString` + `WRITE_SECURE_SETTINGS` |

On AOSP, FutureUI should be signed with the platform key, or listed in
`privapp-permissions`. The `RootShell` paths in `ThreatBlocker` are then
replaced by the APIs above.

## Who may call the security API

- `content://com.future.futureui.systemui/{security,threats,threat_log}` and
  every `call()` method accept only `com.future.settings`, the provider's own
  process, or root (QA). The caller must also be FutureOS-signed, checked by
  `CallerCheck`.
- The status-bar settings, `ThemeProvider` and the keyboard's provider accept
  any FutureOS-signed app.
