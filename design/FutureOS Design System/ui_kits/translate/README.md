# UI kit · תרגום (Translate)

A standalone translation app for FutureOS at 640 × 960, keypad-only, composed from the
shipped components.

## Screens

- **תרגום (root)** — language bar of two capsules with a `swap_horiz` `IconButton` between
  them; input card (label, character count, wrapping entry box); result card with the target
  label, the translation and a four-action row (השמע · העתק · שתף · שמור); two recent
  translations. `EmptyState` when the input is empty.
- **תרגם מ / תרגם אל** — picker: `TextField` search, אחרונות card, כל השפות, `check` on the
  current language. זיהוי שפה appears in the source list only.
- **היסטוריה** — `TabRow` (הכול / שמורים); saved rows carry the favorite star; selecting a row
  loads it back into the root screen. `EmptyState` when שמורים is empty.
- **שיחה** — alternating bubbles showing each line and its translation, over a mic capsule.
- **Options menu** — `m`: שיחה, היסטוריה, הורדת שפה, הגדרות, נקה היסטוריה (`ConfirmDialog`).

## Keys

Arrows move focus; left/right move within a row group (language capsules + swap, the four
result actions, the history tabs). Enter activates, Backspace goes back, `m` opens the menu.
On the input field and the search, Enter starts typing — letters type, Backspace deletes.

Translation is canned: a small phrase dictionary per pair, with an in-flight "מתרגם" state.

## Non-component surfaces

The input box repeats the `TextField` tokens (`TextField` is single-line by design and
translation input wraps). The language capsules, action cells and mic capsule are focusable
shapes the system has no component for; they use the standard focus recipe.

## Files

- `index.html` — entry card: state, keys, menu, dialog, toasts, focus scrolling
- `TranslateScreens.jsx` — the four screens and the sample data
