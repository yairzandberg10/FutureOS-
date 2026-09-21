# UI kit · בלוטות' (Bluetooth)

A Bluetooth app for FutureOS at 640 × 960, keypad-only, composed from the shipped
components. No Bluetooth app exists in the sources — this is designed against the system.

## Screens

- **בלוטות' (root)** — a `Card` with the radio `SettingItem` + `Switch` and the device-name
  row; `EmptyState` (`bluetooth_disabled`) when the radio is off. When on: מכשירים מותאמים —
  `ListItem` rows whose summary is the state (`מחובר · 80%` / `מותאם` / `מתחבר`), with the
  device-type glyph and the entry chevron in the trailing slot, and a `ProgressBar` under a
  row while it connects. מכשירים זמינים — a scanning section header with an indeterminate
  `ProgressBar` while the scan runs, then the nearby rows.
- **מכשיר** — hero (80px glyph in a 160px circle, name, status), a `Card` of profile
  switches (שיחות ואודיו, מדיה) and שנה שם, then נתק / התחבר (`secondary`) and שכח מכשיר
  (`quiet`) as full-width buttons.
- **Pairing** — `ConfirmDialog`, `להתאים את <שם>?` / התאם. Confirming moves the device into
  the paired list in the מתחבר state, which resolves to מחובר with a `HeadsUpNotification`.
- **Forget** — destructive `ConfirmDialog`, `לשכוח את <שם>?` / שכח. Returns to the root list.
- **Rename** — `InputDialog` for both the phone's own name and a paired device's name.
  The component is display-only, so the kit holds the draft string and types into it.
- **Options menu** — `m`: רענן, שם המכשיר, קבצים שהתקבלו, הגדרות, נתק הכל.

## Keys

Arrows move focus down the single column, Enter activates, Backspace goes back, `m` opens
the menu. The switch rows toggle on Enter as well as on the switch itself. In a rename
dialog, letters type and Backspace deletes.

## Notes

Turning the radio off disconnects every device and collapses the screen to the two setting
rows plus the empty state — the paired list is not shown for a radio that is off. The scan
stops on its own after 4s; רענן restarts it.

Device names are generic (אוזניות, מערכת רכב, רמקול סלון) — no vendor names.

## Non-component surfaces

The device hero is a plain glyph-in-a-circle; nothing in the system covers it. Everything
else is a shipped component.

## Files

- `index.html` — entry card: state, keys, pairing flow, dialogs, toasts, focus scrolling
- `BluetoothScreens.jsx` — the two screens, the device data and the menu
