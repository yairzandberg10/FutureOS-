# SharedKeypadNav (`com.future.sharednav`)

The one real shared library in FutureOS, and the **canonical design system**
referenced by `CLAUDE.md`'s "נאמנות מלאה לדיזיין סיסטם" (full design-system
fidelity) rule. If you're adding or changing UI anywhere in the suite, the
tokens and components below are what that rule points at; don't hand-roll a
color, radius, font size, duration, or focus behavior that already exists here.

It is a Gradle `com.android.library` module (package `com.future.sharednav`)
living as a sibling folder to every app, not a submodule of any one of them —
each consumer wires it in with a cross-directory `include(":sharedkeypadnav")`
+ `projectDir` override in its own `settings.gradle.kts` (see any app's file
for the exact snippet). **All 28 apps in the repo depend on it.** Do not move
a single app folder out of the repo without this one too, and do not move
this folder without every app.

## Design tokens (`theme/`)

The source of truth is **`design/futureos-ds/`** — the FutureOS Design System.
Its `tokens/*.css` and `guidelines/*.html` are what the tokens below are
reconciled against; where this README cites a rule, the file that states it is
named. (`design/keyboard-panel/Tokens.dc.html` remains the older, narrower spec
for the keyboard panel only.)

### Color — `FutureTheme` + roles

- **`FutureTheme`** — the base set: `backgroundColor`, `surfaceColor`,
  `textColor`, `dangerColor`, `successColor`, `warningColor`, derived from
  `isDarkMode` + `accentColor`.
- **Roles** (`FutureColorRoles.kt`, extension properties so the constructor
  every app calls stays unchanged):

  | Role | Use for |
  |---|---|
  | `textAlpha(n)` | the ladder itself: 70/60/55/50/40/30/20/18/15/12/10/8/6 |
  | `mutedTextColor` | 60% — row summary, slider label |
  | `secondaryTextColor` | 70% — empty-state title, secondary dialog button |
  | `sectionHeaderColor` | 55% — section header (with 1sp tracking) |
  | `subtleTextColor` | 40% — empty-state subtitle and glyph |
  | `chevronColor` | 30% — the entry chevron |
  | `dividerColor` | 12% dark / 10% light — separators between rows |
  | `elevatedSurfaceColor` | chips, keys, input fields on a card ("glass") |
  | `raisedSurfaceColor` | one step above elevated |
  | `focusFillColor` | 14% accent — focused list row |
  | `focusFillIconColor` | 30% accent — focused top-bar icon button (no border) |
  | `focusFillSettingColor` | 6% — focused setting row |
  | `focusFillMenuColor` | 12% — focused menu row (no border) |
  | `focusFillChipColor` | 18% — focused chip or slider row |
  | `idleChipColor` / `idleFieldColor` | 6% / 8% — the same at rest |
  | `headsUpSurfaceColor` | the floating notification, always dark |
  | `switchTrackOffColor` | a switch's off track, fixed |
  | `readableAccentColor` | the accent **as it may be drawn** on the theme's surfaces |
  | `onReadableAccentColor` | text/icons on `readableAccentColor` |
  | `onAccentColor` | text/icons on the raw `accentColor` |
  | `scrimColor` | dimming behind dialogs |

- **`FutureContrast`** — WCAG luminance math. The accent is a free user choice
  and defaults to white, so anything that assumes "white text on the accent" or
  "the accent on a light surface" renders white-on-white in light mode. Never
  hardcode `Color.Black`/`Color.White` on top of the accent; use the roles
  above. The same math backs `Keyboard/KeyboardPalette`.
- App-specific colors stay extension properties in `FutureTheme.kt`
  (`calcButtonColor`, `inputBarColor`, `favoriteColor`, ...).

### Shape — `FutureShapes`

| Token | Radius | Use for |
|---|---|---|
| `xs` / `radiusXs` | 12dp | progress bars, thin gauges, tiny tags (clamps to a pill on a thin bar) |
| `sm` / `radiusSm` | 12dp | small controls: checkbox, key, action tile (`--fos-radius-item`) |
| `row` / `radiusRow` | 20dp | list rows (`FocusableItem`, `ListItem.jsx`, `--fos-radius-row`) |
| `textField` / `radiusTextField` | 16dp | text fields, text areas |
| `md` / `radiusMd` | 16dp | tab items (`--fos-radius-tab`) |
| `chip` / `radiusChip` | 16dp | chips |
| `lg` / `radiusLg` | 16dp | cards, panels, slider rows, ActionGrid cells (`--fos-radius-card`) |
| `dialog` / `radiusDialog` | 20dp | dialogs, options menu |
| `xl` / `radiusXl` | 22dp | central surfaces, the settings card, "glass" |
| `xxl` / `radiusXxl` | 28dp | heads-up notification, system-shell glass |
| `pill` | 50% | buttons, switch track, day chip, nav indicator |

House rule from `tokens/shape.css`: no tight corners anywhere — 12dp is the
smallest radius in the system, and text surfaces and tabs sit at 16dp. A
radius larger than half a side clamps, so a 4dp bar with `xs` renders as a
pill (the same as `ProgressBar.jsx`'s `h / 2`). `FutureDimens.itemCornerRadius`
(the list row) / `cardCornerRadius` / `borderRadius` are aliases onto this
scale.

### Type — `FutureTypography` / `FutureType`

| Token | Size | Use for |
|---|---|---|
| `caption` | 11sp | key legends, counters |
| `label` | 12sp | field labels, tags |
| `summary` | 13sp | the line under an item title |
| `body` | 14sp | running text |
| `bodyLarge` | 16sp | emphasized body, dialog content |
| `title` | 17sp | list item title |
| `screenTitle` | 20sp | top bar title |
| `headline` | 24sp | large section heading |
| `display` | 34sp | the big header at the top of a screen |
| `hero` | 48sp | a single value that is the screen (timer, amount) |

Replaced 29 hand-picked sizes. Sizes above `hero` (clock faces, calculator
display) are graphics, not interface type, and stay literal in their screen.
`FutureTypography` holds fixed values; `FutureType(multiplier)` is the same
scale multiplied by the Settings font-size slider — shared components read it
through `rememberFutureType()`.

### Spacing — `FutureDimens`

`spacingXxs` 2 · `spacingXs` 4 · `spacingSm` 8 · `spacingMd` 12 ·
`spacingLg` 16 · `spacingXl` 24 · `spacingXxl` 32 (dp), plus `itemSpacing` 12
and `screenPadding` 16 — the only two the design system names.

Focus borders are **two** widths, and the split is deliberate
(`guidelines/focus-spec.html`): `focusBorderItem` 1.5dp on a list row,
`focusBorderControl` 2dp on a control. `focusBorderWidth` aliases the control
width. `focusScale` is 1.02 and applies to list rows and dialogs only.

Focus row heights — `rowHeightList` 65 · `rowHeightSetting` 54 ·
`rowHeightMenu` 50 · `rowHeightDialogButton` 44 · `rowHeightTopBarButton` 36
(dp). There is no touch, so Material's 48dp minimum does not apply; the rule is
that a focused row must be unmistakable at a glance. `screenWidth`/
`screenHeight` are 320×480dp — 640×960px at density 2.0, the one screen size.

### Elevation — `FutureElevation`

Six levels, and only the card carries a real shadow (4dp dark, 1dp light).
Glass is a lighter tone with no shadow at all, and a dialog is separated by
`scrimColor` rather than by lifting it. There is no blur anywhere.

### Motion — `FutureMotion` + `FutureTransitions`

Durations are short on purpose: every screen change on a keypad device is a
key press, users press a lot and fast, and animation time accumulates into a
feeling of lag.

| Token | Duration | Use for |
|---|---|---|
| `DurationInstant` | 90ms | focus color changes (must feel immediate) |
| `DurationFast` | 140ms | small in-screen changes, exits |
| `DurationStandard` | 200ms | screen transitions, dialogs |
| `DurationSlow` | 280ms | large reveals only |

Easings: `EasingStandard`, `EasingDecelerate` (entering), `EasingAccelerate`
(leaving). Specs: `focusColorSpec`, `focusScaleSpec` (a spring, so a held
arrow key doesn't restart the scale on every row), `listItemSpec`.

`FutureTransitions`:
- `forward()` / `backward()` — entering a deeper screen / going back. RTL:
  "inward" moves left, so a new screen enters from the left.
- `fadeThrough()` — between same-level screens (tabs).
- `appear()` — content replacing content in place.
- `dialogEnter` / `dialogExit`.
- `navEnter` / `navExit` / `navPopEnter` / `navPopExit` — the same motion as
  values for `androidx.navigation` `NavHost(enterTransition = { ... })`.

Don't write a `tween(…)` with a literal duration in app code.

### Material bridge — `FutureMaterialTheme` / `FutureAppTheme`

- **`FutureMaterialTheme(theme)`** maps the tokens onto `MaterialTheme`
  (`ColorScheme`, `Typography`, `Shapes`) and provides `LocalFutureTheme` and
  `LocalFutureType`. Every app-level Material theme (`DialerTheme`,
  `MessagesTheme`, `NotesTheme`, `NavigationTheme`, `SettingsTheme`,
  `FutureLauncherTheme`, `FutureUITheme`) is a thin wrapper around it, so a
  stock `Switch`, `NavigationBar`, `AlertDialog` or `OutlinedTextField` looks
  the same in every app.
- **`FutureAppTheme(theme)`** — the same plus forced RTL, for an activity root.
- **`rememberFutureTheme()`** — the current theme, **updated live** through a
  `ContentObserver` on `ThemeProvider`. Replaces the ~20-line
  `ThemeClient.getTheme` + `ON_RESUME` observer block that was copied into
  about 20 activities, which only refreshed when returning to the app (so a
  dark/light change from the control center, opened *over* the app, didn't
  show until leaving and coming back).
- **`ThemeClient`** — reads/writes the cross-app theme via `FutureUI`'s
  `ThemeProvider` (`content://com.future.futureui.theme/theme`). Falls back to
  `(isDarkMode = true, primaryColor = Color.WHITE)` if `FutureUI` isn't
  installed or the query fails.

## Screen constant (`FutureScreen.kt`)

`FutureScreen.WIDTH_DP` / `HEIGHT_DP` (640×960, matching `CLAUDE.md`) and
`.width` / `.height` as `Dp`. Use this instead of a bare `640`/`960` literal
— e.g. `@Preview(widthDp = FutureScreen.WIDTH_DP, heightDp =
FutureScreen.HEIGHT_DP)`.

## Keypad navigation (`nav/`)

- **`rememberFocusListState(itemCount, initialIndex)`** — tracks which item
  in a list is focused (`rememberSaveable`) and keeps a `LazyListState` in sync.
- **`Modifier.keypadListNav(state, onSelect, horizontal)`** — `DirectionUp`/
  `DirectionDown` (or `Left`/`Right`) move focus, `DirectionCenter`/`Enter`/
  `NumPadEnter` select.
- **`Modifier.numericShortcuts(itemCount, onSelect)`** — digits 1-9 select by
  position, for short menus.
- BACK is deliberately **not** handled here — use
  `androidx.activity.compose.BackHandler` at screen level.

## Components (`components/`)

- **`AnimatedScreenHost(targetState, depthOf)`** — replaces a manual
  `when (route) { ... }` with a transition in the right direction: deeper
  enters forward, shallower goes back, same depth fades. `depthOf` must be a
  function of the state itself. Pass a snapshot of everything the screen reads
  as the state (see `Gallery/MainActivity`), so the outgoing screen keeps
  drawing its own data while it animates out.
- **`AnimatedBackStackHost(backStack)`** — the same for a
  `mutableStateListOf<Route>` back stack (Music, Sfarim, Fitness); records each
  entry's depth itself.
- **`ScreenTopBar`** / **`TopBarIconButton`** — back button + title + optional
  trailing action. The title crossfades when it changes; the icon button marks
  focus with a fill *and* a ring and scales on focus/press.
- **`ScreenScaffold`** — theme background + forced RTL + optional top bar.
- **`KeypadLazyColumn`** — a `LazyColumn` wired to `keypadListNav` that
  auto-scrolls to the focused item and animates insert/delete/reorder when a
  `key` is given.
- **`EmptyState`** — icon + title + subtitle, entering with a short stagger.
- **`AppDialog`** — the outer window of every dialog: width/height limits for
  the 640px screen and the open animation. Contents are composed on the first
  frame, so focus requests made on open still work.
- **`ConfirmDialog`** — cancel/confirm for destructive actions. Button text and
  focus ring derive from contrast (previously black text on the black-70%
  cancel button, and a white ring on a white dialog, in light mode).
- **`MarqueeText`** — single-line text that scrolls while focused.

Components the FutureOS Design System defines and every app used to hand-roll
(each app had its own options menu, dialog buttons, text field, list row and
tab chips, each slightly off the spec). Use these instead of Material's
`AlertDialog`, `OutlinedTextField`, `Button`, `Switch`, `Checkbox`,
`CircularProgressIndicator`, `NavigationBar` or `TopAppBar`:

- **`FutureOptionsMenu`** + **`FutureMenuRow`** (`OptionsMenu.jsx`) — the menu
  the hardware menu key opens: top-anchored 40dp, 20dp radius, optional 12sp
  header, 50dp rows, 20dp icon, 15sp label, 12% text focus fill with no border
  and no scale; destructive rows in `dangerColor`. The first row takes focus.
- **`FutureDialog`**, **`FutureDialogButtons`**, **`FutureDetailRow`**,
  **`InputDialog`** (`ConfirmDialog.jsx`, `InputDialog.jsx`) — 20dp surface,
  20dp padding, 15sp bold centered title, 12dp between buttons.
- **`FutureButton`** (`Button.jsx`) — Primary / Destructive / Secondary /
  Quiet, all one shape: a pill at a fixed 44dp, 24dp side padding, 16sp, and
  a 2dp focus ring drawn inside the box so focus never changes the height.
  `enabled = false` also removes focus (the DS rule: what cannot be activated
  cannot be focused). `ConfirmDialog` is built from the same button.
- **`FutureTextField`** (`TextField.jsx`) — 8% fill, 16dp radius, 15sp, 2dp
  accent ring; `leading`/`trailing` slots, `autoFocus` that works inside a
  Dialog. **`FutureFormField`** adds a 12sp label row for multi-field forms.
- **`FutureListItem`** (`ListItem.jsx`) — 65dp row, 20dp corners, title
  17sp/500, summary 13sp/60%, `leading`/`trailing` slots, list-row focus.
- **`FutureAvatar`** (`Avatar.jsx`) — a circle in `avatarFillColor`
  (`--fos-avatar-fill`, #3A3A3C / #D3D3DC, which reads on both the screen and
  a card), icon at 44% or initials at 30%, both in the full text color; 44dp in
  a list, 88dp as a hero. **`FutureBadge`** (`Badge.jsx`) — unread count.
- **`FutureActionCell`** (`ActionGrid.jsx`) — one focusable icon+label cell:
  glass fill (20% accent when active), card radius, 2dp accent focus ring,
  glyph at 44% of the cell height clamped to 20-40dp
  (`FutureDimens.iconActionCell`), 13sp label at 60%. Call controls, home
  shortcuts.
- **`FutureTabRow`** / **`FutureTabItem`** (`TabRow.jsx`), **`FutureChip`**
  (tracks its own focus), **`FutureCheckbox`**, **`FutureSwitch`**
  (`Switch.jsx`: on = accent track + thumb in the background color, off =
  empty track with a 30% border — never a white thumb), **`FutureSpinner`**
  (a round-capped accent arc over 26% of a 10% track, 900ms a turn),
  **`FutureProgressBar`** (and **`FutureIndeterminateProgressBar`**, a third
  of the track sweeping right to left, for a scan or a download with no known
  length), **`FutureCard`**, **`FutureDivider`**,
  **`FutureSectionHeader`** (`inset = false` inside an already-padded list),
  **`FutureSettingItem`** (`onClick = null` for a display-only row — a row
  that cannot be activated does not take focus), **`FutureBottomNav`**.
- **`FutureCapsule`** / **`FutureRoundCapsule`** (`Capsule.jsx`) — a pill that
  carries a small label above its value (the language pickers in תרגום), and
  its round variant (the big microphone in conversation mode): 8% fill, 20%
  accent when active, 2dp accent focus ring, no scale.
- **`FutureSnackbar`** + **`rememberFutureSnackbarState`** /
  **`FutureSnackbarHost`** (`Snackbar.jsx`) — transient feedback for something
  that already happened ("הועתק", "נשמר"), anchored to the bottom of the
  screen and gone on its own. It never takes focus. An *incoming* event is a
  heads-up notification instead, and that one belongs to FutureUI.

### Rows inside a card

`FutureCard` holds *transparent* rows separated by a hairline, and a focused
row is drawn by **`Modifier.cardRowFocus(background, border)`**: the fill and
the ring span the full width of the card, the first row keeps the card's top
corners, the last row its bottom corners, and a middle row is a rectangle
(`Card.jsx` + `SettingItem.jsx`). A row finds its own place by measuring
itself against the card, so a row inside a loop, a condition or another
wrapper needs no index from the call site, and outside a card the same
modifier falls back to the ordinary rounded shape. `FutureSettingItem` uses
it; a hand-built row inside a card should too, instead of clipping itself to
its own shape.

## Focus utilities (`focus/`)

- **`FocusableItem`** — a focus-highlighted `Box` around arbitrary content.
  Defaults are the list-row tokens (`radiusRow` = `--fos-radius-row`,
  `focusBorderItem` 1.5dp, 14% accent fill); the fill and ring
  fade in with `focusColorSpec`, the item scales up on focus and down on an OK
  press.
- **`Modifier.focusMotion(interactionSource)`** — just that scale-on-focus /
  shrink-on-press motion, for a hand-built button. Press comes from
  `clickable`, which turns DPAD_CENTER/ENTER into a `PressInteraction`.
- **`Modifier.staggeredEntrance(index)`** — items fade and rise in one after
  another when a screen opens (first 8 only; state kept per lazy-list key, so
  scrolling back doesn't replay it).
- **`Modifier.dpadFocusBorder(isFocused, shape)`** — the same focus look as a
  modifier, for an existing Material component (`OutlinedTextField`, `Card`,
  `FloatingActionButton`).
- **`Modifier.bringIntoViewOnFocus()`** — scrolls a focused item into view in
  any scroll container.
- **`Modifier.escapeTextFieldFocusTrap()`** — lets DPAD up/down leave a text
  field.

## T9 (`t9/`)

**`T9DigitMap`** — the canonical Hebrew+English digit→letters map (`ENGLISH`,
`HEBREW`, `isHebrew(Char)`), matching the device keycaps and including
final-form letters (ךםןףץ). Search/prediction algorithms stay per-app.

## Cross-app broadcast actions (`actions/`)

**`FutureUIActions`** — the `com.future.futureui.ACTION_*` /
`com.future.dialer.ACTION_*` broadcast-action constants. Import this instead of
retyping the string literal.

## Testing

`src/test/java/com/future/sharednav/` covers `T9DigitMap`, `FocusListState`,
`KeypadDigits`, `FutureType`, `FutureContrast` (including that the Material
scheme never puts unreadable text on the accent in either mode) and
`FutureScale` (every radius and font size found in the pre-migration code maps
onto a scale step). Run with `./gradlew :sharedkeypadnav:testDebugUnitTest`
from any consuming app's directory.

## A note for `notes/`

`notes/` sets `android.builtInKotlin=false` in its own `gradle.properties`
(a workaround for a KSP/Room conflict). That Gradle property is global to
the whole build tree, including this module when built from `notes`'s
context — so this module's `build.gradle.kts` conditionally applies the
classic `org.jetbrains.kotlin.android` plugin (with an explicit `jvmTarget`)
only in that case. If you add a new consumer with an unusual Gradle setup,
build it (`./gradlew :app:compileDebugKotlin`) before assuming this module
"just works" under it.
