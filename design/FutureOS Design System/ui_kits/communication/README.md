# UI kit · Calls, contacts and messages

The dialer surface at 640 × 960: three tabs behind a `BottomNav`, each a focus-scrolling list
of `ListItem` rows, with `OptionsMenu` on the menu key, `ConfirmDialog` in front of a delete,
and `HeadsUpNotification` floating over all of it.

## Screens

| screen | built from |
|---|---|
| שיחות (recents) | `TopBar`, `ListItem` + trailing call-direction `Icon` |
| אנשי קשר (contacts) | `ListItem` + filled `star` at `--fos-favorite`; `EmptyState` variant |
| הודעות (threads) | `ListItem` + `Badge` |

## Keys

Up/down moves focus inside the list. **Left/right switch tabs** — the nav items themselves are
unfocusable by design, exactly as in `FitnessBottomNav`. `m` opens the options menu, `n` fires a
heads-up notification, `e` toggles the contacts empty state.

## Deliberate gaps

The **message thread view** and the **dial pad** are not documented anywhere in the sources, so
they are left out rather than invented; the messages screen says so on screen. A missed call uses
`dangerColor` on its trailing icon, which follows the semantic colour rules but was not observed
directly.
