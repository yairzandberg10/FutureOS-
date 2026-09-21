# UI kit · Calls (redesign)

A standalone calls app at 640 × 960, log first, on `BottomNav`: יומן / מקלדת / מועדפים.

## Screens

- **יומן** — grouped by day into `Card`s of `SettingItem` rows (direction icon, duration,
  time), with `Chip` filters הכל / לא נענו and an `EmptyState` when nothing is missed.
- **מקלדת** — 3×4 grid of digit keys with letters, a large display that resolves the number
  against contacts, and a primary התקשר button.
- **מועדפים** — `ListItem` rows; Enter dials directly.
- **חיפוש** (`s`) — `TextField` over live results by name or digits.
- **מסך איש קשר** — centered avatar, `Card` of actions (התקשר, שלח הודעה, מועדפים), and the
  recent history with that number.
- **שיחה נכנסת** (`i`) — avatar, name, ענה / דחה.
- **שיחה פעילה** — running timer, 2×2 grid of control tiles (השתק, רמקול, המתנה, מקלדת),
  destructive סיים שיחה.
- **סיום שיחה** — duration summary, התקשר שוב / חזור ליומן.

Options menu (`m`): חיפוש, שיחה נכנסת, אנשי קשר, הגדרות, נקה יומן (`ConfirmDialog`).

## Keys

Arrows move focus, left/right switch tabs, Enter activates, Backspace goes back (and ends an
active call), `m` menu, `s` חיפוש, `i` שיחה נכנסת, `f` toggles the missed filter. Digits type
in the keypad tab; letters type in חיפוש.

## Files

- `index.html` — entry card: tabs, state, keys, menu, dialog, focus scrolling
- `ScreensA.jsx` — the eight screens
- `data.js` — sample log, favorites, contacts, keypad
