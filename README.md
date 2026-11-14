# FutureOS

A custom Android-based OS for a **keys-only feature phone** — no touchscreen anywhere. Base: Android 12 (SDK 31), target device: 640×960px keypad phone (T9 numeric keypad, `\*`/`#`, D-pad nav). Every screen across every app is operable by keypad focus traversal and numeric shortcuts.

Since there's no custom ROM, "System UI" (status bar, lock screen, control center, notification center) is implemented by [FutureUI](apps/FutureUI/) as a set of `AccessibilityService` overlays — the practical way to build a custom system shell on stock Android 12. Design language: dark/glass, Material Symbols Rounded, gradient accents.

## Apps

|Folder|App name|Package|Role|
|-|-|-|-|
|[FutureUI](apps/FutureUI/)|FutureUI|`com.future.futureui`|System UI — status bar, lock screen, control center, notification center|
|[SystemUI](apps/SystemUI/)|System UI|`com.android.sistemui`|A second, parallel implementation of the System UI role — the same six `AccessibilityService`s as FutureUI, plus Do-Not-Disturb scheduling. Not installed on the test device; see the note below|
|[FutureLauncher](apps/FutureLauncher/)|מסך הבית|`com.future.futurelauncher`|Home screen / launcher|
|[Settings](apps/Settings/)|הגדרות|`com.future.settings`|System settings|
|[Dialer](apps/Dialer/)|טלפון|`com.future.dialer`|Phone dialer|
|[Messages](apps/Messages/)|הודעות|`com.future.messages`|SMS messaging|
|[Notes](apps/Notes/)|פתקים|`com.future.notes`|Notes|
|[Tasks](apps/Tasks/)|משימות|`com.future.tasks`|Task management - to-do list with priorities|
|[Bluetooth](apps/Bluetooth/)|בלוטוס|`com.future.bluetooth`|Bluetooth device manager - scan, pair, unpair|
|[Calendar](apps/Calendar/)|לוח שנה|`com.future.calendar`|Hebrew calendar, Daf Yomi, location-based zmanim|
|[Contact](apps/Contact/)|אנשי קשר|`com.future.contact`|Contacts|
|[Navigation](apps/Navigation/)|ניווט ותחבורה|`com.future.navigation`|Driving navigation \& public-transit journey planner|
|[Files](apps/Files/)|קבצים|`com.future.files`|File browser|
|[Keyboard](apps/Keyboard/)|מקלדת|`com.future.keyboard`|System-wide T9 predictive text input method|
|[Gallery](apps/Gallery/)|גלריה|`com.future.gallery`|Photo/video gallery|
|[Camera](apps/Camera/)|מצלמה|`com.future.camera`|Camera - live preview, capture, flash, front/back switch|
|[Music](apps/Music/)|מוזיקה|`com.future.music`|Local music player|
|[Sfarim](apps/Sfarim/)|בלכתך בדרך|`com.future.sfarim`|Torah text library|
|[Terminal](apps/Terminal/)|טרמינל|`com.future.terminal`|Root shell terminal|
|[Tools](apps/Tools/)|כלים|`com.future.tools`|Unit converter, QR/text scanner, quick notes, random picker and other small utilities|
|[Flashlight](apps/Flashlight/)|פנס|`com.future.flashlight`|Standalone flashlight — the duplicate inside Tools was removed; FutureUI keeps a torch toggle in the control center, which is the system shell, not a second app|
|[Recorder](apps/Recorder/)|רשמקול|`com.future.recorder`|Voice recorder — records in a foreground service (keeps going with the screen off), live level bars, pause/resume, playback with key seeking, rename/share/delete from the Options menu|
|[Calculator](apps/Calculator/)|מחשבון|`com.future.calculator`|Calculator|
|[Clock](apps/Clock/)|שעון|`com.future.clock`|Alarms, world clock, stopwatch, timer|
|[Fitness](apps/Fitness/)|כושר|`com.future.fitness`|Workout tracking, Bluetooth heart-rate monitor|
|[Remote](apps/Remote/)|שלט|`com.future.remote`|Universal infrared remote (non-TV devices: A/C, fan, audio, custom)|
|[Guide](apps/Guide/)|מדריך למשתמש|`com.future.guide`|On-device user guide for every app|
|[Assistant](apps/Assistant/)|עוזר קולי|`com.future.assistant`|Voice assistant - also opens on a global double-press of OK, see FutureUI|
|[Translate](apps/Translate/)|תרגום|`com.future.translate`|On-device translation (ML Kit), language download for offline use, conversation mode|
|[Wallpapers](apps/Wallpapers/)|רקעים|`com.future.wallpapers`|Wallpaper picker (online catalog). No launcher icon: opens from "Wallpaper" in the launcher's edit mode|

Not every folder is a standalone app: [SharedKeypadNav](apps/SharedKeypadNav/) is a shared Gradle library module (`com.future.sharednav`) that **all 30** apps above depend on for D-pad focus handling, T9 digit mapping, and the cross-app design system (colors, spacing, type scale) — see [`apps/SharedKeypadNav/README.md`](apps/SharedKeypadNav/README.md), which is also the canonical design-system reference for this project. [hardware](hardware/) holds CAD files (Onshape motherboard keep-out volumes, an OpenSCAD case/keycap model) for the target device, a Qin F22 Pro.

Each app is a standalone Android Studio project (its own `build.gradle.kts`/`settings.gradle.kts`) living in its own folder under [`apps/`](apps/).

## Repo layout

|Folder|What's in it|
|-|-|
|[apps/](apps/)|All 30 apps plus the shared `SharedKeypadNav` module|
|[docs/](docs/)|Device setup, AI guidelines, QA report, plans and marketing copy|
|[design/](design/)|Design system files and the keyboard panel design|
|[hardware/](hardware/)|Hardware specs and CAD|
|[marketing/](marketing/)|Landing page, promo video and launch motion projects|
|[security/](security/)|`security-check.py` (the CI security gate) and the signed malware DB|
|[server/](server/)|Feedback and update server (Cloudflare Worker)|
|[aosp/](aosp/)|Plans and docs for the AOSP port|
|[ready-apks/](ready-apks/)|Install script for locally built APKs|

## Notes

* `apps/Sfarim/tools/output/sefaria.db` (Torah-text database, ~1.55GB / 6,211 books) is present on disk but **not** tracked in this repo's git — GitHub blocks files over 100MB. It is built directly from Sefaria's public sources via `apps/Sfarim/tools/build_library.py`; see `apps/Sfarim/README.md` for the exact rebuild command. `apps/Sfarim/tools/output/reassemble_sefaria_db.sh` documents an earlier, abandoned plan to split/reassemble a pre-built ~2.1GB copy — no longer needed now that the DB is built from source, but the script is left in place for reference.
* This repo is a fresh consolidation of the individual app projects (no prior per-app git history was carried over).
* **[FutureUI](apps/FutureUI/) and [SystemUI](apps/SystemUI/) both implement the System UI role, and only one of them can be active at a time.** Both declare the same five services (the separate `LockScreenAccessibilityService` was removed on 2026-09-24; FutureUI's lock screen now runs inside its existing services - see `apps/FutureUI/.../lockscreen/` and `LockSettingsActivity` - so a leftover `LockScreenAccessibilityService` entry in `enabled_accessibility_services` on a device is stale and can be removed) — `StatusBarAccessibilityService`, `NotificationCenterAccessibilityService`, `ControlCenterAccessibilityService`, `MediaControlService`, `HeadsUpNotificationService` — under different package names (`com.future.futureui` vs `com.android.sistemui`; the latter is spelled "sistemui" because `com.android.systemui` is a reserved system package and cannot be installed). Enabling both sets of `AccessibilityService`s would make two overlay stacks compete for the same key events, and the status bar service in particular swallows `KEYCODE_MENU`/`KEYCODE_SETTINGS` system-wide, so a duplicate would break the Options key everywhere. On the `F22 Pro` test device only FutureUI is installed and its three overlay services are the ones enabled. Which of the two is intended to win long-term is not recorded anywhere in this repo.
