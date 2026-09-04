# FutureOS

A custom Android-based OS for a **keys-only feature phone** — no touchscreen anywhere. Base: Android 12 (SDK 31), target device: 640×960px keypad phone (T9 numeric keypad, `\*`/`#`, D-pad nav). Every screen across every app is operable by keypad focus traversal and numeric shortcuts.

Since there's no custom ROM, "System UI" (status bar, lock screen, control center, notification center) is implemented by [FutureUI](FutureUI/) as a set of `AccessibilityService` overlays — the practical way to build a custom system shell on stock Android 12. Design language: dark/glass, Material Symbols Rounded, gradient accents.

## Apps

|Folder|App name|Package|Role|
|-|-|-|-|
|[FutureUI](FutureUI/)|FutureUI|`com.future.futureui`|System UI — status bar, lock screen, control center, notification center|
|[FutureLauncher](FutureLauncher/)|FutureLauncher|`com.future.futurelauncher`|Home screen / launcher|
|[Settings](Settings/)|Settings|`com.future.settings`|System settings|
|[dialer](dialer/)|טלפון|`com.future.dialer`|Phone dialer|
|[Messages](Messages/)|Messages|`com.future.messages`|SMS messaging|
|[notes](notes/)|פתקים|`com.future.notes`|Notes|
|[Calendar](Calendar/)|לוח שנה|`com.future.calendar`|Hebrew calendar, Daf Yomi, location-based zmanim|
|[Contact](Contact/)|אנשי קשר|`com.future.contact`|Contacts|
|[Navigation](Navigation/)|ניווט ותחבורה|`com.future.navigation`|Driving navigation \& public-transit journey planner|
|[Files](Files/)|קבצים|`com.future.files`|File browser|
|[Keyboard](Keyboard/)|מקלדת|`com.future.keyboard`|System-wide T9 predictive text input method|
|[Gallery](Gallery/)|גלריה|`com.future.gallery`|Photo/video gallery|
|[Camera](Camera/)|מצלמה|`com.future.camera`|Camera - live preview, capture, flash, front/back switch|
|[Music](Music/)|מוזיקה|`com.future.music`|Local music player|
|[Sfarim](Sfarim/)|בלכתך בדרך|`com.future.sfarim`|Torah text library|
|[Terminal](Terminal/)|טרמינל|`com.future.terminal`|Root shell terminal|
|[Tools](Tools/)|כלים|`com.future.tools`|Unit converter, flashlight, QR/text scanner, and other small utilities|
|[Calculator](Calculator/)|מחשבון|`com.future.calculator`|Calculator|
|[Clock](Clock/)|שעון|`com.future.clock`|Alarms, world clock, stopwatch, timer|
|[Fitness](Fitness/)|כושר|`com.future.fitness`|Workout tracking, Bluetooth heart-rate monitor|
|[Remote](Remote/)|שלט|`com.future.remote`|Universal infrared remote (non-TV devices: A/C, fan, audio, custom)|
|[Guide](Guide/)|מדריך למשתמש|`com.future.guide`|On-device user guide for every app|
|[Assistant](Assistant/)|עוזר קולי|`com.future.assistant`|Voice assistant - also opens on a global double-press of OK, see FutureUI|

Two more top-level folders are not standalone apps: [SharedKeypadNav](SharedKeypadNav/) is a shared Gradle library module (`com.future.sharednav`) that **all 23** apps above depend on for D-pad focus handling, T9 digit mapping, and the cross-app design system (colors, spacing, type scale) — see [`SharedKeypadNav/README.md`](SharedKeypadNav/README.md), which is also the canonical design-system reference for this project. [hardware](hardware/) holds CAD files (Onshape motherboard keep-out volumes, an OpenSCAD case/keycap model) for the target device, a Qin F22 Pro.

Each app is a standalone Android Studio project (its own `build.gradle.kts`/`settings.gradle.kts`) living in its own top-level folder here.

## Notes

* `Sfarim/tools/output/sefaria.db` (Torah-text database, ~1.55GB / 6,211 books) is present on disk but **not** tracked in this repo's git — GitHub blocks files over 100MB. It is built directly from Sefaria's public sources via `Sfarim/tools/build_library.py`; see `Sfarim/README.md` for the exact rebuild command. `Sfarim/tools/output/reassemble_sefaria_db.sh` documents an earlier, abandoned plan to split/reassemble a pre-built ~2.1GB copy — no longer needed now that the DB is built from source, but the script is left in place for reference.
* This repo is a fresh consolidation of the individual app projects (no prior per-app git history was carried over).

