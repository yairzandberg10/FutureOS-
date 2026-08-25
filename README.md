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
|[Music](Music/)|מוזיקה|`com.future.music`|Local music player|
|[Sfarim](Sfarim/)|בלכתך בדרך|`com.future.sfarim`|Torah text library|
|[Terminal](Terminal/)|טרמינל|`com.future.terminal`|Root shell terminal|
|[Tools](Tools/)|כלים|`com.future.tools`|Calculator|
|[Guide](Guide/)|מדריך למשתמש|`com.future.guide`|On-device user guide for every app|

Each app is a standalone Android Studio project (its own `build.gradle.kts`/`settings.gradle.kts`) living in its own top-level folder here.

## Notes

* `Sfarim/tools/output/sefaria.db` (\~2.1GB Torah-text database) is **not** in this repo — GitHub blocks files over 100MB, and there wasn't enough local disk space to split it at the time this repo was created. See `Sfarim/tools/output/reassemble\_sefaria\_db.sh` and the root `.gitignore` for the plan to add it back.
* This repo is a fresh consolidation of the individual app projects (no prior per-app git history was carried over).

