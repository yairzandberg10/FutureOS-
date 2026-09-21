# Keyboard (מקלדת)

`com.future.keyboard` — System-wide T9 predictive text input method (IME).

The physical device has no touchscreen, so this isn't an on-screen keyboard — `KeyboardService` (an `InputMethodService`) intercepts physical digit-key presses (`onKeyDown`) and translates them into real words via [`T9Engine`](app/src/main/java/com/future/keyboard/T9Engine.kt), the same way old feature-phone T9 input worked:

- Each digit key (2–9) maps to a group of letters (Hebrew and English digit-maps included; the English map matches the one already used by `dialer`'s `T9Search.kt`).
- Typing a digit sequence looks up matching words in a small built-in dictionary, ranked by frequency; `*` cycles between candidate matches.
- If no dictionary match exists, input falls back to multi-tap mode (repeated presses on the same key cycle through its letters), like classic SMS entry.
- `*` opens the punctuation menu; `#` steps to the next input mode, and holding `#` opens the language list directly (21 modes, so stepping through them one at a time is impractical).
- `0` inserts a space, `1` a literal `1`, since neither is letter-mapped.
- `0` held down starts voice transcription (`android.speech.SpeechRecognizer`, in the current IME language) and commits the recognized text at the cursor; releasing the key ends the recording. Requires `RECORD_AUDIO`, which can't be requested from an `InputMethodService` — the user grants it once from the app's own setup screen (`MainActivity`).

## The panel

The IME's input view is not a keyboard — the keys are physical — it is a status/feedback panel, built in code in `KeyboardService.onCreateInputView`. It has four states, one per mode the user can be in: typing and prediction, the punctuation grid, the language list, and voice transcription. Each one is a title rail, a content area, and a key legend along the bottom; the legend is the only place the key bindings above are ever shown, since there is no on-screen keyboard to read them off.

The panel is drawn from the design in [`design/keyboard-panel`](../design/keyboard-panel) — `Panel.dc.html` is the four states and `Tokens.dc.html` is the measurement and color table. Those values are in device pixels for the 640×960 screen at density 2.0, so every `dp` in the code is half the value in the design.

Colors follow the system theme (`ThemeClient`), including the user's accent color. Because the accent defaults to white, the text on a selected item is derived from the accent's luminance rather than fixed (see `KeyboardPalette`), and a near-invisible accent (white on the light theme) falls back to the theme's ink color — otherwise the selected candidate would be white on white.

`MainActivity` is a minimal setup screen (touch input is disabled to match the real hardware) that deep-links to `Settings.ACTION_INPUT_METHOD_SETTINGS` so the user can enable the IME, and also offers a button to grant the `RECORD_AUDIO` permission needed for hold-0 voice transcription.

## `*` and `#` do not arrive as key events

Both keys are consumed system-wide by `FutureUI`'s accessibility services — `ControlCenterAccessibilityService` for `*` and `NotificationCenterAccessibilityService` for `#` — which hold them to open the control center and the notification center. They return `true` for every event on those keys, so `onKeyDown` here never sees them on real hardware (an injected `adb shell input keyevent 17` does arrive, which is why this only reproduces on the device). Each service now broadcasts the short press instead of dropping it, the same mechanism the Options key has always used, and `KeyboardService` listens for `ACTION_STAR_SHORT_PRESS` / `ACTION_POUND_SHORT_PRESS`. `onStarShortPress`/`onPoundShortPress` are shared by both routes so the behaviour can't drift.

The long press on `#` still belongs to the notification center, so **holding `#` for the language list does not work on a device with `FutureUI` installed** — that code path is only reachable where `FutureUI` is absent. Reaching a specific language means stepping through the modes with short `#` presses.

**Known gaps:**
- Only Hebrew and English have a real corpus-backed dictionary. The other 15 languages ship a starter list of a few hundred common words, which is why the language list labels them "מילון בסיסי".
- `T9EngineTest` covers the prediction engine and `KeyboardPaletteTest` the panel's color rules; nothing covers `KeyboardService` itself, which is where the key handling and the panel live.
- Voice transcription depends on the device having a working speech-recognition service (`Assistant`'s local engine is preferred when installed); if neither is available, holding `0` just shows an "unavailable" message in the panel.
