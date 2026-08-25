# Keyboard (מקלדת)

`com.future.keyboard` — System-wide T9 predictive text input method (IME).

The physical device has no touchscreen, so this isn't an on-screen keyboard — `KeyboardService` (an `InputMethodService`) intercepts physical digit-key presses (`onKeyDown`) and translates them into real words via [`T9Engine`](app/src/main/java/com/future/keyboard/T9Engine.kt), the same way old feature-phone T9 input worked:

- Each digit key (2–9) maps to a group of letters (Hebrew and English digit-maps included; the English map matches the one already used by `dialer`'s `T9Search.kt`).
- Typing a digit sequence looks up matching words in a small built-in dictionary, ranked by frequency; `*` cycles between candidate matches.
- If no dictionary match exists, input falls back to multi-tap mode (repeated presses on the same key cycle through its letters), like classic SMS entry.
- `*` held down toggles between Hebrew and English.
- `#` commits the current word and starts a new one; `0`/`1` insert a space/literal `1` since they aren't letter-mapped.
- `0` held down starts voice transcription (`android.speech.SpeechRecognizer`, in the current IME language) and commits the recognized text at the cursor; the candidate bar shows a listening indicator while active. Requires `RECORD_AUDIO`, which can't be requested from an `InputMethodService` — the user grants it once from the app's own setup screen (`MainActivity`).

`MainActivity` is a minimal setup screen (touch input is disabled to match the real hardware) that deep-links to `Settings.ACTION_INPUT_METHOD_SETTINGS` so the user can enable the IME, and also offers a button to grant the `RECORD_AUDIO` permission needed for hold-0 voice transcription.

**Known gaps:**
- The dictionary is a small hand-picked word list per language, not a full frequency dictionary.
- No tests.
- Duplicates the T9 digit-map that already exists in `dialer`'s `T9Search.kt` — a candidate for extraction into a shared module alongside the other keypad-nav components.
- Voice transcription depends on the device having a working speech-recognition service (e.g. the Google app); if `SpeechRecognizer.isRecognitionAvailable` returns false, holding `0` just shows an "unavailable" message in the candidate bar.
