# Changelog

This repo has no carried-over git history (see root [README](README.md)), so this file tracks project-wide status and decisions going forward rather than a commit-by-commit history. Each app may additionally document its own development history in its own `README.md`.

## Unreleased

### System pass: share window, recents, power menu, widgets

- **One share window for the whole system.** `FutureShare` (SharedKeypadNav)
  already pointed every app at `com.future.futureui.ACTION_SHARE`, but the
  window had been reverted, so every share fell back to Android's chooser.
  FutureUI has a `ShareActivity` again, in FutureUI's own look: a bottom sheet
  with what is being shared, a "copy" chip for text, and a 4-column grid of the
  apps that accept it, most recently used first. Settings' Bluetooth fallback
  now uses it too.
- **Gallery viewer: action bar instead of the soft-key bar.** New shared
  component `FutureActionBar` (not in the design system before): share, edit,
  details, delete. Down arrow from the photo enters it, left/right move, Up or
  BACK return to the photo.
- **Recents shows only apps you opened, laid out like Redmi.** The list used to
  come from three days of UsageStats, so apps that only surfaced briefly showed
  up. The status bar service now records each window that comes to the front
  and keeps it only if it is a real activity of a launchable app. Two-column
  cards (icon and name above, app-colored card), free memory at the top, a round
  X to close everything; Options closes the focused app, 0 closes all.
- **Power menu.** Holding the power key opened Android's dialog; the status bar
  service now recognises it, closes it and opens FutureUI's menu instead. The
  Control Center power button opens the same menu. Power off and restart ask
  for a second OK; airplane mode, silent mode and screenshot were added.
- **Control Center sliders**: dark track, near-opaque white fill, percentage
  shown. The old light-gray-on-gray was barely readable.
- **Notifications are a little larger** (heads-up and Notification Center):
  more padding, bigger icon, text on two lines.
- **Launcher widgets are sized by the widget.** Every widget used to go into a
  single 1x1 cell on the home page. Now the span comes from the provider
  (`targetCellWidth/Height`, else the classic `(dp + 30) / 70`), it goes to the
  first free area that fits on the page you are on, the provider is told its
  real size, and OK on a focused widget clicks it.
- **Launcher edit mode: the apps window is a window.** Instead of a full-screen
  list, a window in the Options-menu shell with all apps in a 4-column grid.
- **Settings > Sounds.** Sound / vibrate / mute had two focus targets each (one
  invisible, one that ignored OK); now one. Volume sliders could not go up: the
  5% step was truncated to the same stream level (and 7/15*15 came out as
  6.9999). Levels are rounded, and a press always moves at least one level.
- **Guide**: Clock, Calculator, Flashlight, Fricasse and Wallpapers showed a
  glyph instead of their icon, because they were missing from `<queries>`.
- **SystemUI is a mirror of FutureUI.** `SystemUI/sync-from-futureui.sh` copies
  every source file, the manifest and the non-icon resources, renaming only the
  package. SystemUI's own DND schedules and low-battery overlay were not in
  FutureUI and are gone from SystemUI with this (they are in git history).

### Stability pass: package visibility, recents, commentaries

Found by reading the device's own crash/ANR history (`dumpsys dropbox`) and
logcat. The R8 crashes there (Translate ML Kit, Sfarim JNI) and the Dialer ANRs
were already fixed; these are the remaining real faults.

- **Apps lost sight of the keyboard and FutureUI (package visibility).**
  targetSdk 31 hides other packages unless they are declared. FutureUI saw the
  keyboard's settings provider only by accident (while it was the IME, or right
  after it touched FutureUI); after a keyboard reinstall with Gboard as the IME
  the Control Center logged "Failed to find provider info" every 15 seconds and
  its predictive-text pill showed a default for an hour. `<queries>` for
  `com.future.futureui` and `com.future.keyboard` now sit in the SharedKeypadNav
  manifest, so every app gets them on its next build.
- **FutureUI recents showed only a handful of apps.** It could see about ten
  packages, so Dialer, Contacts, Settings, Camera and every third-party app were
  missing from recents, and notification labels/icons fell back. FutureUI (and
  SystemUI) now hold `QUERY_ALL_PACKAGES`, as a system UI does. Recents also
  skips the current home app, whichever launcher that is, and loads icons only
  for the 15 apps it shows: it runs on the main thread of the service that
  filters every key press, and used to load an icon for every app used in three
  days.
- **Sfarim: opening commentaries is faster.** Each "X on Y" book was scanned
  from its first segment to find the verse. Simple books now narrow to the one
  chapter through `idx_segments_book_chapter` (`top_index` is `path[0]` there);
  complex books keep the old query. Same results, 2-4x faster on the real
  database even cold.
- **Settings: the keyboard languages screen no longer freezes on open.** It
  queried the keyboard's provider (another process, sometimes cold) on the main
  thread during composition; it now loads on IO.

### Control Center grid and lock screen removal

- **FutureUI: the Control Center icon grid has a new set of controls.** The
  18 old grid controls are gone and the grid starts empty. In edit mode,
  Options on the grid opens the add list; up to 50 controls, from a new
  catalog of 50 (`controlcenter/logic/GridControls.kt`): 24 real toggles with
  state read from the system (dark theme, battery saver, data saver,
  silent/vibrate, night light, extra dim, font size, grayscale, invert
  colours, animations, and more - written through root `settings`/`cmd`,
  falling back to the public API), 6 actions (screenshot, screen off, media
  play/pause/next/previous, clear memory), 13 FutureOS app shortcuts and 7
  system settings pages. The fixed pill rows (Wi-Fi/Bluetooth,
  airplane/DND) stay as they were. (2c8a22f had applied this to the pills by
  mistake; a section order saved with `pills` migrates back.)
- **The lock screen is deleted from FutureUI and SystemUI.** Its service,
  screen, layout manager, PIN store and PIN screens are gone, as are the lock
  screen rows in "התאמה אישית" and the clock style row in the Settings app
  (`clock_style` is no longer in the SystemUI settings provider). The lock
  screen service also handled non-lock-screen work: foreground-app tracking,
  CALL/ENDCALL for a ringing call, opening the call screen over the home
  screen, and double-OK for Assistant. All of that moved to
  `StatusBarAccessibilityService`, which is always enabled.

### Performance: release builds, a quieter system shell, draw-phase focus

A pass over the whole system for speed and smoothness. Nothing here changes
how anything looks or behaves.

- **The device runs release builds now, compiled ahead of time.** Every app
  was installed as a debuggable debug build: no R8, and `debuggable` turns off
  ART's ahead-of-time compilation, so all of Compose ran interpreted/JIT
  (`dumpsys package dexopt` showed `run-from-apk` for every app). Release is
  now signed with the local debug key when there is no `keystore.properties`,
  so it installs over the existing debug install without losing data, and
  every install is followed by `cmd package compile -m speed`. The build
  picker and `build-all.sh --install` do both; CLAUDE.md has the new commands.
  The shared R8 rules also kept every `@Composable` method in every class,
  which stopped R8 from optimizing any UI code at all — removed. Release lint
  skips `ExpiredTargetSdkVersion`, a Google Play rule (targetSdk is 31 on
  purpose, for the Android 12 device).
- **Calls (dialer): dialing, incoming calls and tab keys fixed.** The main
  activity had an `ACTION_CALL` intent-filter, so every call it placed (and
  every `ACTION_CALL` from Messages) resolved back to the dialer itself and
  never reached Telecom. The filter is gone and calls go through
  `TelecomManager.placeCall`. A ringing call now brings the call screen up
  directly (an InCallService is exempt from background-start limits); before,
  it was only a heads-up notification, which this device has no system bar to
  show. The activity is `singleTop`, so the call screen and `ACTION_DIAL`
  reuse the open dialer instead of rebuilding it. Left/right on a tab with
  nothing focused (empty favorites, or a lazy list not yet laid out) now
  still switches tabs, and the first row of a lazy list is focused once it is
  attached.
- **Calls (dialer): the call log is a lazy list.** The log tab built every
  call on the phone at once (500+ rows, each a focusable row with its own
  animations) on every visit to the tab. It is now a `LazyColumn` with one
  item per day, so the same day header + card are built only when on screen.
  Day grouping and the row texts are computed once per load in the
  ViewModel on `Dispatchers.Default`; the call log query asks for the six
  columns it shows instead of all of them; overlapping refreshes (init,
  resume, call ended) no longer run duplicate queries; the dialpad's contact
  match runs off the main thread; the caller-name lookup (a ContentProvider
  query) moved out of composition to IO; and the search screen's contact list
  is loaded when search opens instead of at app start.
- **FutureUI's accessibility services no longer receive every event on the
  phone.** All four shared one config with `typeAllMask`, so every scroll,
  content change and text change in every app was delivered four times to
  FutureUI's main thread — the same thread that filters every key press.
  Three of them had an empty `onAccessibilityEvent`; the lock screen only
  reads `TYPE_WINDOW_STATE_CHANGED`. Each now subscribes to exactly that one
  event (`accessibility_service_keys.xml` / `accessibility_service_config.xml`).
- **System state is read off the main thread.** `ControlManager.updateStates`
  (a dozen binder calls, two reflection lookups and a ContentProvider query to
  the keyboard) ran on the main thread every 500ms while the control center was
  open and every 15s in the status bar. It now reads on `Dispatchers.IO`,
  coalesces overlapping refreshes, and caches the reflection lookups; the media
  controller lookup moved off the main thread too. The status bar clock and
  battery are driven by `TIME_TICK` / `BATTERY_CHANGED` instead of the poll, so
  the clock also changes exactly on the minute.
- **Focus animations are drawn, not recomposed.** Every focus component in
  SharedKeypadNav read its animated colours during composition, so each frame
  of the 90ms focus animation recomposed the row it was on — two rows per key
  press, continuously while an arrow key is held. `FocusableItem`,
  `dpadFocusBorder`, `cardRowFocus`, `FutureSettingItem`, `FutureButton`,
  `FutureOptionsMenu`, `FutureTabRow`, `FutureTextField`, `ScreenTopBar`,
  `FutureBottomNav`, `FutureSwitch`/`Chip`/`DayChip`/`Checkbox` and the
  capsules now read them in the draw phase (`animatedFocusSurface`,
  `animatedFill`, and a lambda overload of `cardRowFocus`), and Settings' own
  rows use the same overload.
- **Keyboard.** The T9 index for a language was built on the IME's main thread
  when the engine was created — for English (~370,000 words) that froze the
  keyboard for seconds on the first switch. It now builds on a background
  thread (typing falls back to multi-tap until it is ready), `digitsFor` uses a
  letter→digit table instead of scanning every key string per letter, the
  in-memory Hebrew index is only built if the SQLite dictionary is not ready,
  Hebrew lookups are cached (LRU, 256 sequences), and the key legend is no
  longer rebuilt from new views on every key press.
- **Launcher icons** are decoded on a background thread and preloaded together
  with the app list, instead of `loadIcon().toBitmap()` inside composition on
  the first frame of the home screen.

### Card focus, the calls redesign, Bluetooth, and a translation app

Three UI kits that shipped with the design system but had no code behind them
(`ui_kits/calls`, `ui_kits/bluetooth`, `ui_kits/translate`) are now built.

- **The focus of a row inside a card** follows `SettingItem.jsx`: it fills the
  full width of the card, the first row keeps the card's top corners, the last
  row its bottom corners, and a middle row has none — instead of a rounded
  pill floating inside the card with a gap on each side. A row finds where it
  sits by measuring itself against the card (`FutureCard` + the new
  `Modifier.cardRowFocus`), the way `Card.jsx` finds its rows through wrapper
  elements, so nothing has to be counted by hand at the call site. Settings,
  Clock's alarms, the call log and the new screens all pick it up.
- **Calls** (`dialer`) was rebuilt on the kit: three tabs — יומן / מקלדת /
  מועדפים — a keypad tab with the number, the matching contact and the 3×4
  key grid, a contact screen with actions and the history for that number, a
  search screen, and incoming / active / ended call screens (avatar, timer,
  2-column control tiles, "התקשר שוב"). The options menu carries חיפוש,
  אנשי קשר, הגדרות and נקה יומן. A digit pressed in the log or in favourites
  jumps to the keypad with that digit, like any feature phone. Hold
  (`Call.hold`) is a real control now, and the quick-reply list answers a
  ringing call with a message and rejects it.
- **Bluetooth** was rebuilt on its kit: the radio and the device name in one
  card, paired devices with their state (`מחובר · 80%` / `מתחבר` / `מותאם`),
  a scanning section for nearby devices, and a device screen with a hero
  glyph, per-profile switches (שיחות ואודיו / מדיה), rename, connect and
  forget. Connecting a paired device and switching its profiles are not public
  Android APIs; they are attempted through the profile proxies and, on a
  device that refuses them (`BLUETOOTH_PRIVILEGED`), the app opens the
  system's own Bluetooth screen instead of failing silently.
- **Translate** (`com.future.translate`, new app) — the 29th app. Translation
  runs on the device through ML Kit: a language bar with the two capsules and
  a swap button, an input card, a result card with השמע · העתק · שתף · שמור,
  history with a שמורים tab, a conversation mode that speaks each side's
  translation out loud, per-language download for offline use, and settings.
  Each language model downloads once (~30MB); after that there is no network
  in the loop. On a device with no speech recogniser the conversation mode
  falls back to typing.
- New shared components for all of the above: `FutureCapsule` /
  `FutureRoundCapsule` (`Capsule.jsx`), `FutureSnackbar` + host
  (`Snackbar.jsx`), and an indeterminate `FutureProgressBar` for a scan or a
  download. `FutureSettingItem` now takes a null `onClick` for a display-only
  row (the call history), because a row that cannot be activated must not take
  focus.

### Design-system update: corners, list rows, buttons, avatar, compose bar

`design/FutureOS Design System` was updated; the code now follows it.

- **Corners** (`tokens/shape.css`: "no tight corners anywhere"). 12dp is the
  floor: `radiusXs`/`radiusSm` 4/8dp to 12dp. Text fields, tabs and chips move
  to 16dp. List rows get their own token, `radiusRow` = 20dp
  (`--fos-radius-row`), which is now `FocusableItem`'s default; hand-built rows
  in Calendar, Contact, Frixa, Fitness, Sfarim, Tools and Recents use it too.
- **List rows** are 65dp tall (`--fos-row-list` 130px, was 56dp).
- **Buttons** are one shape: a pill at a fixed 44dp, 16sp for every variant,
  focus ring inside the box. The quiet variant is used where the time picker
  spec calls for it.
- **Avatar** has its own fill, `avatarFillColor` (#3A3A3C dark / #D3D3DC
  light), and its initials/icon are in the full text color.
- **Spinner** is a round-capped arc over 26% of the circle.
- **ActionGrid**: new `FutureActionCell`; the glyph is 44% of the cell height
  (20-40dp). The dialer's call controls and Sfarim's home shortcuts use it.
- **Time picker** (Clock): the arrows, values and colon sit on one grid
  (52/20/52dp) instead of two stacked columns, the arrows are accent-colored,
  and cancel is the quiet button.
- **Message compose bar** (new template `templates/message-compose`): recipient
  line, a pill field on the surface with attach and dictation buttons inside
  (dictation only when the device has a speech recognizer), a 48dp accent send
  circle, 20dp bubbles with a 6dp tail, and a status line under each message
  (sending / sent at a time / not sent in red).
- Music, dialer, Sfarim and Bluetooth rows moved from hand-built rows onto
  `FutureListItem` + `FutureAvatar` (accent-tinted icon circles removed).

### Design-system audit: every app moved onto the shared components

A suite-wide audit against `design/FutureOS Design System` found that most
screens used the token values but not the components: each app had its own
options menu, dialog, dialog button, text field, list row, tab chip and icon
button, each slightly off the spec (22dp dialogs instead of 20dp, 16sp menu
labels instead of 15sp, `Color.Black` text on the accent, a light-grey focus
ring in the system shell, Material `OutlinedTextField`/`AlertDialog`/`Switch`
with their own geometry).

#### Added to SharedKeypadNav

`FutureOptionsMenu`/`FutureMenuRow`, `FutureDialog`, `InputDialog`,
`FutureTextField`, `FutureFormField`, `FutureListItem`, `FutureAvatar`,
`FutureBadge`, `FutureTabRow`/`FutureTabItem`, `FutureCheckbox`,
`FutureSpinner`, `FutureAccents`. See `SharedKeypadNav/README.md`.

#### Fixed in SharedKeypadNav

- `FocusableItem` defaults to 8dp corners again - `--fos-radius-item` is the
  list-row radius; 16dp is the card radius.
- `FutureSwitch` follows `Switch.jsx`: the thumb was fixed white, which with
  the default white accent is the white-on-white switch the DS calls a defect.
- `ConfirmDialog` uses the DS button (20dp radius) instead of a pill button
  that existed nowhere else, and 20dp dialog padding.
- `FutureChip` tracks its own focus; `FutureButton` and `TopBarIconButton`
  gained `enabled` (not focusable when they cannot act).

#### Per app

- **No blur, no protection gradients** (DS: "there is no blur in this
  system", "no protection gradients"): FutureUI control/notification center,
  lock screen edit mode, dialer call screen, Gallery crop overlay, Camera.
- **Accent means focus or selection only**: decorative accent (icon circles,
  headings, value text, filled banners) replaced with neutral tokens in
  Navigation, Fitness, Tools, Messages, Music, Guide, Frixa, dialer.
- **Readable accent**: selected/primary fills use `readableAccentColor` with
  matching ink, instead of the raw accent with `Color.Black`/`onAccentColor`.
- **Private palettes removed**: Fitness "Kinetic Obsidian" (24dp corners,
  lerped surfaces), Settings `ThemeConfig` hex copy, Keyboard setup
  ColorScheme, Pomodoro/Navigation/Launcher/Status-bar hex colors, Set PIN's
  fixed cyan accent, Navigation's blue default accent.
- **Copy**: no emoji (Calendar weather now uses icons), no ellipses in UI
  strings. The hidden Settings easter eggs are left as they are.
- Filled Material icons in notes and Navigation -> Rounded.

#### Not changed

- `SystemUI/` (the parallel, uninstalled System UI implementation) was not
  migrated.
- Wallpaper presets in Settings keep their gradients - they are wallpaper
  content, not interface.
- Clock faces and the camera countdown keep sizes above `hero`; the DS
  treats them as graphics.

### Reconciled the shared tokens with the FutureOS Design System

`design/futureos-ds/` (extracted from `design/FutureOS Design System.zip`) is now
the source of truth for the token layer. It was derived from this codebase
through a Figma-export pipeline, so most values already agreed; the entries
below are the places where they did not. Everything here is in `SharedKeypadNav`,
so all 28 apps pick it up without an app-side change.

#### Fixed

- **The divider was inverted between themes.** `dividerColor` was white at 10%
  in dark and black at 12% in light. `tokens/colors.css` and
  `guidelines/colors-alpha-ladder.html` both put the divider at **12% dark /
  10% light**. Every card hairline in the suite was one step off in both modes.
- **`Clock`'s world-clock row did not compile.** An earlier find/replace turned
  `androidx.compose.foundation.shape.RoundedCornerShape(12.dp)` into
  `androidx.compose.foundation.shape.FutureShapes.md`, which is not a real name.
  Restored to `FutureShapes.md` and moved its off-ladder 5% background onto
  `idleChipColor` (6%).

#### Added

- **Three radius steps that were being collapsed.** `radiusTextField` (10dp),
  `radiusChip` (14dp) and `radiusDialog` (20dp). Code asking for them landed on
  the neighbouring step, so a text field rounded like a key, a chip like a
  button, and a dialog like the glass surface. `snap()` now routes each to its
  own step, and `ConfirmDialog` uses the 20dp it was specified at rather than 22.
- **The full text alpha ladder**, as `textAlpha(percent)` plus the named roles
  `guidelines/colors-alpha-ladder.html` assigns: `secondaryTextColor` (70%),
  `sectionHeaderColor` (55%), `chevronColor` (30%), the focus and idle
  backgrounds (`focusFillIconColor`, `focusFillSettingColor`, `focusFillMenuColor`,
  `focusFillChipColor`, `idleChipColor`, `idleFieldColor`), and the fixed
  heads-up and switch-track colors. Depth in this system is alpha over the text
  color, and the ladder is a closed table rather than a per-screen choice.
- **Both focus border widths.** `guidelines/focus-spec.html` separates them on
  purpose: **1.5dp on a list row, 2dp on a control**. They had been unified to
  2dp. `focusBorderWidth` stays as an alias for the control width, so no call
  site broke, and `FocusableItem` now defaults to the list width.
- **Focus row heights** (`rowHeightList` 56dp, `rowHeightSetting` 54dp,
  `rowHeightMenu` 50dp, `rowHeightDialogButton` 44dp, `rowHeightTopBarButton`
  36dp), the `focusScale` constant (1.02), and the fixed screen size
  (320×480dp = 640×960px at density 2.0). There is no touch here, so Material's
  48dp rule does not apply; what applies is that the focused row is unmistakable.
- **`FutureElevation`** — the six levels, of which only the card carries a real
  shadow (4dp dark / 1dp light). Glass is tone with no shadow and dialogs are
  separated by the scrim, which is what the code already did without saying so.
- **Type tokens the scale was missing**: `dialog` (15sp) and `badge` (10sp),
  plus weight, line-height (1.3) and section letter-spacing (1sp) tokens.

#### Changed

- **`dialogFontSize` is 15sp again.** It had been folded into `bodyLarge` (16sp)
  as an orphan value; `tokens/typography.css` counts it as one of the seven
  sizes, carrying dialog content, menu rows and fields.
- **`mutedTextColor` is the text color at 60%**, not a fixed `#B0B0B0`/`#444444`.
  Both remain above the AA threshold on their surfaces.
- **`scrimColor` is 60% black in both themes**, not 62%/42%. The design system
  states it as a theme-independent value.
- **The top-bar icon button is focused by a 30% accent fill and no border.** It
  was 22% plus a border; the focus spec gives it the stronger fill precisely so
  it needs no ring. `raisedSurfaceColor`'s light value moved to `#D1D1D6` to
  match the token.

#### Not changed, and why

- **Motion.** `tokens/motion.css` proposes 0/150/200/300/450ms and the four
  stock Android easing curves. `FutureMotion` is 90/140/200/280ms with three
  custom curves, and the reason is written into it: on a keypad device every
  transition is a key press, and animation time accumulates into lag. The two
  agree on the one that matters most (200ms standard). The design system labels
  its own scale "the proposed unification", so this is a live decision rather
  than a deviation, and it would change the feel of all 28 apps.
- **The component gap.** The design system documents 25 components;
  `SharedKeypadNav` ships 8. `ListItem`, `SettingItem`, `Switch`, `Slider`,
  `TextField`, `Chip`, `TimePicker`, `OptionsMenu`, `BottomNav`, `TabRow`,
  `Badge`, `ProgressBar`, `HeadsUpNotification`, `Card`, `SectionHeader`,
  `Divider` and `Button` are still re-implemented per app.
- **`SystemUI` is outside the design system.** It depends on `SharedKeypadNav`
  for navigation and actions but never imports `com.future.sharednav.theme`:
  40 hand-written colors, 23 literal radii and 61 literal font sizes. Its
  control centre, power menu and lock screen therefore do not respond to
  dark/light mode or to the user's accent color. `Frixa` has a smaller version
  of the same problem (14/4/20).

### Design system completion and system-wide motion

The suite had partial tokens that almost nothing used, and no motion. A survey
before this change found 18 different corner radii, 29 different font sizes,
20 hand-written animation durations, 119 raw hex colors, seven separately
maintained Material themes, and screen changes that swapped content in a single
frame in every app except `Settings` (a 120ms fade). Everything below lives in
`SharedKeypadNav` and is documented in its README.

#### Tokens

- **`FutureShapes`** — six radius steps (4/8/12/16/22/28dp) plus `pill`. 16, 8
  and 22 were already `FutureDimens` tokens and the 22 is in
  `design/keyboard-panel/Tokens.dc.html`; the others are the clusters the code
  already used. `FutureDimens`' radius names are now aliases onto this scale.
- **`FutureTypography`** — ten size roles (11/12/13/14/16/17/20/24/34/48sp).
  13, 17, 20 and 34 are the sizes already approved on the device in `Settings`,
  so those screens don't move. `FutureType(multiplier)` is now the same scale
  times the font-size slider, with its old property names kept. Sizes above 48sp
  (clock faces, calculator display) stay literal.
- **Color roles** — `mutedTextColor`, `subtleTextColor`, `dividerColor`,
  `elevatedSurfaceColor`, `raisedSurfaceColor`, `focusFillColor`,
  `readableAccentColor`, `onAccentColor`, `onReadableAccentColor`, `scrimColor`,
  as extension properties so `FutureTheme`'s constructor is unchanged.
- **`FutureContrast`** — the WCAG math that fixed the keyboard's white-on-white
  candidate, moved out of `Keyboard` (`KeyboardPalette` now delegates to it and
  its tests still pass) because the same bug class existed everywhere.
- **Spacing** — a 4dp-based `spacingXxs`…`spacingXxl` scale and one
  `focusBorderWidth` (2dp; it was 1.5dp in `FocusableItem` and 2dp elsewhere).
- **`FutureMotion`** — four durations (90/140/200/280ms), three easings, focus
  color/scale specs and a list-placement spec. Deliberately shorter than Material
  defaults, following the reasoning already written in `Settings`: on a keypad
  every screen change is a key press, and animation time accumulates into lag.
- **`FutureTransitions`** — `forward`/`backward` (RTL: inward slides in from the
  left), `fadeThrough` for same-level tabs, `appear`, dialog enter/exit, and the
  same motion as `NavHost` values.

#### One Material theme

`FutureMaterialTheme(theme)` maps the tokens onto `ColorScheme`, `Typography`
and `Shapes`. `DialerTheme`, `MessagesTheme`, `NotesTheme`, `NavigationTheme`,
`SettingsTheme`, `FutureLauncherTheme` and `FutureUITheme` are now thin wrappers
around it, so stock Material components look the same in every app. Two of them
had been disconnected from the shared theme entirely: **`FutureUI` — the system
shell itself — ran on dynamic Material You colors from `isSystemInDarkTheme`**,
ignoring the dark/light mode and accent the user picks inside that same shell,
and `Settings` defaulted to a hardcoded iOS blue. The Launcher stays dark (it
draws over the wallpaper) but now takes the user's accent.

`rememberFutureTheme()` replaces the `ThemeClient.getTheme` + `ON_RESUME`
observer block copied into ~20 activities with a `ContentObserver` on
`ThemeProvider`, which already called `notifyChange`. The old block only
refreshed when returning to an app, so a mode change from the control center,
opened over the app without pausing it, didn't show until leaving and coming back.

#### Motion

- **Screen transitions in 19 apps.** `AnimatedScreenHost` / `AnimatedBackStackHost`
  replace the `when (route)` switches in `Tools`, `Clock`, `Frixa`, `Guide`,
  `Remote`, `Gallery`, `Contact`, `Tasks` (list ↔ editor), `Files` (viewer ↔ list),
  `Calendar` (settings), `Messages`, `Music`, `Sfarim`, `Fitness` and `FutureUI`'s
  settings; `Settings`,
  `notes`, `Navigation` and `dialer` got the shared `NavHost` transitions (dialer
  fades between its two tabs and slides only into the call screen). Screens are
  handed to the host as a snapshot, so an outgoing screen keeps drawing its own
  photo/contact/conversation while it animates out.
- **Focus and press.** `FocusableItem`, `dpadFocusBorder`, `TopBarIconButton` and
  the `ConfirmDialog` buttons share one motion: the fill *and* the ring fade in
  (the ring used to pop in), the item scales up on focus with a spring (so a held
  arrow key doesn't restart it on each row), and shrinks briefly on OK — the only
  press feedback a device with no touchscreen has. `clickable` already turns
  DPAD_CENTER into a `PressInteraction`, so no key handling changed.
- **Dialogs** open with a short scale-and-fade (`AppDialog`), **empty states**
  stagger in, **top-bar titles** crossfade when they change, and
  `KeypadLazyColumn` animates insert/delete when given keys and staggers its
  first rows in when the list opens.
- **Opening and closing apps.** Every app used to inherit the ROM's window
  animation, so launching an app looked nothing like moving inside it.
  `@style/FutureActivityAnimation` (SharedKeypadNav `res/values/future_motion.xml`)
  applies the same motion as `forward`/`backward`: the incoming app slides in from
  1/8 of the width on the left and fades in over 200ms, a closing app slides left
  and fades out over 140ms, and the window underneath stays put so no black edge
  shows. It covers activity, task and wallpaper (home screen) transitions and is set
  in the `themes.xml` of 26 apps; `FutureUI` and `SystemUI` are left out because the
  lock screen and control center are overlay windows with their own motion.

#### Migration across the apps

A scripted pass over 157 files in 26 apps replaced 252 corner radii and 641 font
sizes with the nearest token, moved 27 animation durations in `FutureUI` and
`Gallery` onto `FutureMotion` (deliberate effects — the coin spin, the pulsing
mic, the call-timer pulse, the easter eggs — kept their own timing), and replaced
36 places that drew `Color.Black` or the background color on top of the accent
with `onAccentColor`. `SystemUI` was excluded, as in earlier passes.

#### Bugs fixed on the way

- `ConfirmDialog` in light mode wrote black text on its black-70% cancel button
  and drew a white focus ring on a white dialog.
- The bottom-bar indicator in `Clock` and `Frixa` drew a fixed black icon on the
  accent; with a dark accent the selected tab's icon was invisible.
- `TopBarIconButton` marked focus only with a 30%-accent fill, invisible in light
  mode with the default white accent.

#### Visible changes to check

Snapping to the scale moves some values by a step: list rows that used the old
8dp `FocusableItem` default (dialer, Sfarim) are now 16dp; the control-center and
notification tiles went from 35dp to 28dp; 15sp text (69 places) is now 16sp;
dialog text is 16sp instead of 15sp.

Fixes for the findings raised in the 2026-09-08 system review. Every app touched
below compiles (`compileDebugKotlin`, run per app); `:sharedkeypadnav` unit tests
pass; `Tasks` also produced a minified release APK end to end. Nothing here has
been run on the `F22 Pro` device yet.

#### Blocking

- **`Messages` read the entire SMS inbox on the main thread.** `getConversations()`
  scans all of `content://sms` and then ran `resolveContact()` — another
  `ContentResolver` query — once per conversation (N+1). All three call sites
  (`LaunchedEffect`, the conversation-click callback, the `BackHandler`) were on
  the main dispatcher, which is a guaranteed ANR on an inbox of a few thousand
  messages. Every provider call in the app now goes through `Dispatchers.IO` with
  only the state assignment back on main, and `SmsRepository` memoizes contact
  lookups in a `ConcurrentHashMap` (cleared through `clearContactCache()`), which
  removes the N+1 entirely. The conversation list also distinguishes "loading"
  from "empty" for the first time.
- **`DefaultApps.PACKAGES` named a package that does not exist.** It listed
  `com.future.acremote` while the remote app's real `applicationId` is
  `com.future.remote`, so with home-screen restriction on, the remote could never
  be added to the home screen and failed silently. `com.future.assistant`,
  `com.future.flashlight` and `com.future.frixa` were missing from the same list
  and were added.
- **Text fields trapped keyboard focus across eleven apps.** Compose text fields
  consume DPAD up/down internally for cursor movement, so on a device with no
  touchscreen a user who arrowed into a field could not leave it. `Fitness` had
  solved this locally in `ui/components/FocusUtils.kt`; that modifier moved to
  `com.future.sharednav.focus.escapeTextFieldFocusTrap` and is now applied in
  `Tasks`, `notes`, `Tools`, `Remote`, `Files`, `Contact`, `Calendar`, `Messages`,
  `Music` and `Terminal`. Four apps (`dialer`, `Navigation`, `Settings`, `Sfarim`)
  each carried a down-only copy of the same hack inline; all four now call the
  shared modifier, which also handles up — so it is possible to get back *to* the
  search field from the results, not only away from it. Applied at the text field
  rather than at the screen root wherever the screen has its own arrow-key
  handling (`Calendar`'s month grid, `Contact`'s list), so nothing is intercepted
  in the capture phase that a screen was already handling.
- **`targetSdk` was 37 on 25 apps and 31 on `FutureUI` and `Settings`** — two
  different system behaviors on the same Android 12 device. All 28 are now on
  `targetSdk = 31` (`compileSdk` stays 37), matching the hardware and what
  `README.md` and `DEVICE_SETUP.md` already describe. Raising the whole suite to
  37 instead would mean opting every app into edge-to-edge, foreground-service
  and broadcast behaviors that have never been tested on this ROM.

#### Reliability and security

- **The lock-screen PIN was `SHA-256(salt + pin)` in a plain preferences file,
  with no attempt limiting and a `==` comparison.** Four digits is 10,000
  possibilities and a single hash round breaks offline instantly. New `PinStore`
  signs the PIN with an HMAC key generated in the Android Keystore and marked
  non-exportable, so the file alone is useless without the device's hardware;
  comparison is `MessageDigest.isEqual`; five failures start a lockout that
  doubles from 30s to a 5-minute ceiling, surfaced on the lock screen as a
  countdown rather than a silently rejected correct code. Existing PINs keep
  working and are upgraded to the new scheme on the next successful unlock.
- **Two root-shell implementations, one of which always reported success.**
  `FutureUI/ControlManager.executeRoot` wrote to a long-lived pipe and returned
  `true` unconditionally — no wait, no exit code, and `stdout`/`stderr` never
  read, so the pipe could fill and block the process; every toggle showed "on"
  even when the command failed. `Settings/SystemInteractor` returned a real
  success flag but its fallback ran `Runtime.exec(cmd)` with no shell, splitting
  the command on spaces. Both now call one shared `com.future.sharednav.root.RootShell`,
  which drains both pipes on parallel threads (the deadlock `Terminal/ShellSession`
  already avoided), enforces a timeout, returns a real exit code, and falls back
  through `sh -c` so quoting and pipes survive. `ControlManager`'s toggles moved
  onto its IO scope since the call now blocks, and the accessibility services use
  a new non-blocking `runRootCommandAsync`.
- **A failed dictionary copy took down the whole IME.** `HebrewDictionaryDb`'s
  345MB `assets` copy ran in a bare `Thread` with no `try/catch`, so a full disk
  or a corrupt asset killed the input method process — on a device with no
  touchscreen, that is the loss of all text input. The copy now writes to a temp
  file and renames only on completion (an interrupted copy no longer leaves a
  file that looks valid and fails forever after), and the caller catches, logs,
  and shows a message in the candidate row while prediction falls back to the
  small built-in dictionary.
- **The font-size slider in `Settings` affected only the settings screen.**
  `FutureType` existed in the shared module but no shared component used it —
  `ScreenTopBar` hardcoded `20.sp`, `EmptyState` `17.sp`, `ConfirmDialog` `15.sp`.
  The multiplier now lives in `ThemeProvider` alongside the rest of the theme,
  `ThemeClient` reads and writes it, and the shared components resolve it through
  `rememberFutureType()`; `ScreenScaffold` provides it once per screen so nested
  components do not each query the provider. Old `FutureUI` builds that do not
  return the column degrade to 1.0 rather than throwing.

#### Consistency and hardening

- **Three `ContentProvider`s were `exported` with no permission.** `ThemeProvider`,
  `SystemUiSettingsProvider` and `KeyboardSettingsProvider` let any installed APK
  change the system theme, the status-bar configuration and the prediction toggle.
  `FutureUI` and `Keyboard` now each declare a `signature`-level permission and
  guard their own providers with it; all 28 manifests request both.
- **There was no release build path.** All 28 apps disabled R8, no
  `proguard-rules.pro` existed anywhere, and no app had a `signingConfig` — there
  was no way to produce a shrunk, signed APK for a device whose storage is already
  under pressure. R8 and resource shrinking are now on for `release`, each app has
  a `proguard-rules.pro` (crash-report line numbers, manifest-instantiated
  components, Room, Compose), and signing reads `keystore.properties` from the
  repo root (gitignored) or CI environment, falling back to an unsigned build
  rather than a failed one. **Release builds have not been run on the device and
  the minified output is unverified beyond assembling.**
- **`keypadListNav(horizontal = true)` mapped the arrows backwards for RTL.** It
  bound right to "next", but the whole system forces `LayoutDirection.Rtl`, where
  the next item is to the left. There is still no caller, which is exactly why it
  was worth fixing now.
- **Holding an arrow key made list scrolling stall.** `rememberFocusListState` ran
  `animateScrollToItem` on every index change, and Android's key repeat cancelled
  each animation to start another. A single press still animates; a repeat within
  150ms scrolls instantly, so a held key tracks the focus in real time.
- **29 empty `catch` blocks, now 54 across 19 files, all log.** Silent failure is
  the one thing that cannot be diagnosed from the device on a system built on `su`
  and non-public APIs. `SystemUI` was left alone deliberately — it is the parallel
  implementation that is not installed on the test device.
- **`Navigation` re-queried Nominatim for repeated searches.** Its usage policy
  allows one request per second; T9 typing regenerates the same query constantly.
  Added a 50-entry LRU cache and a log on failure where the result was previously
  an empty list with no explanation.

#### Experience

- **`MarqueeText` had zero users.** Wired into `Files`' file names and `Messages`'
  conversation previews, the two places where a 640px row cuts text with no way to
  read the rest.
- **Five delete paths had no confirmation.** `ConfirmDialog` now guards alarm
  deletion in `Clock`, custom workouts in `Fitness`, events in `Calendar` (both the
  options menu and the edit dialog, through one shared confirmation) and messages
  in `Messages`. `notes` already had one.
- **`digitForKey` existed in seven byte-identical copies** (`Calculator`, `Clock`,
  `Contact`, `Fitness`, `Music`, `Sfarim`, `Tools`), plus a private copy in
  `FutureUI`'s lock screen. One shared `com.future.sharednav.nav.digitForKey`, the
  seven files deleted, with a unit test covering both key rows.
- **Touch language on a device with no touchscreen.** "הקישו על +", "הקש כדי
  לשכוח", "לחץ כאן" and five more now name the OK key and the navigation that
  actually exists.
- **The guide covered 15 topics for 28 apps** and described a Contact feature that
  does not exist (jump-to-letter; `Contact` filters by T9 digits). Added tasks,
  bluetooth, navigation, camera, fitness, remote and assistant, and corrected the
  contact instructions.
- **`Music` registered its own `BroadcastReceiver` for the Options key** instead of
  the shared `onOptionsKeyPress`, duplicating the logic in the one place the shared
  constant was meant to centralize.
- **`Sfarim`'s reader font size reset on every book.** It is now persisted.

#### Known gaps from the same review

Not addressed here, and still open: 3,347 hardcoded Hebrew strings versus empty
`strings.xml` files; the 21 apps holding state in `remember` rather than a
`ViewModel`; the four accessibility services' parallel polling loops; the 394MB of
`.idea/` and debug output in git history; and the 127 uncommitted files in the
working tree, 60 of them `ic_launcher*.webp` changes that contradict the explicit
rule in `CLAUDE.md` — that one needs a decision, not a patch.

### Keyboard panel and Settings main screen redesign

Both screens were rebuilt to the designs in `design/keyboard-panel` (the four panel states in `Panel.dc.html`, the measurement and color table in `Tokens.dc.html`). Verified on the `F22 Pro` device, all four panel states and the new Settings sections captured live.

- **`Keyboard`: the IME input view is now a real panel instead of a hint line and a chip row.** `KeyboardService.onCreateInputView` built one `TextView` of status text plus a candidate row, and every mode (punctuation grid, language list, voice) reused that same hint line for a sentence of instructions — the language menu's was "בחירת שפה - חצים למעלה/למטה, מרכז לבחירה, חזור לביטול". The panel now has four built-once states swapped by visibility (`showOnly`), each with a title rail, a content area and a key legend along the bottom: typing shows the language badge, the mode tag, the digit sequence and a `1/9` candidate counter; punctuation shows the selected symbol, a `3/34` counter and the 6-column grid; the language list shows each mode's full name, dictionary scope and short code; voice shows a mic ring, the recognition language and a level meter. The legend replaces the instruction sentences with the keys themselves — the only place the bindings are visible on a device with no on-screen keyboard.
- **`Keyboard`: the selected candidate could be invisible.** The text on a selected item was hardcoded `Color.WHITE` while its background was the system accent, and `ThemeClient`'s accent defaults to `Color.WHITE` — so on a default install the selected word was white on white, and the accent was also unreadable against the light theme's own light panel. New `KeyboardPalette` (unit-tested, pure `Int` math so it runs without a device) derives the ink from the background's WCAG luminance, and falls back to the theme's text color when the accent has less than 1.6:1 contrast against the panel. The four accent colors in the design's contrast table are covered by `KeyboardPaletteTest`.
- **`Keyboard`: releasing `0` now ends voice transcription**, which is what the panel's legend says it does ("שחרר את המקש כדי לסיים"). `onKeyUp` previously only cleared the long-press latch and left the recognizer running until it timed out on its own. It now calls `SpeechRecognizer.stopListening()`, which transcribes what was captured — as opposed to the service's own `stopListening()`, which destroys the recognizer and discards the result. `onRmsChanged`, previously an empty override, drives the level meter.
- **`Keyboard`: `*` and `#` never reached the keyboard at all on the device.** `ControlCenterAccessibilityService` (STAR) and `NotificationCenterAccessibilityService` (POUND) each return `true` for every event on their key — DOWN, UP and repeats — so both keys are consumed system-wide, and their own comment says no short-press action is defined. A long press opens the control center / notification center; a short press did nothing anywhere. The punctuation menu and the language switch were therefore unreachable with real hardware keys, which the new panel legend made obvious by advertising them. Both services now broadcast the short press (`ACTION_STAR_SHORT_PRESS` / `ACTION_POUND_SHORT_PRESS`, added to `FutureUIActions`), exactly as `StatusBarAccessibilityService` already does for the Options key, and `KeyboardService` listens for both. The long presses are untouched. Mirrored into the `SystemUI` copies of both services so the two implementations don't drift. Verified on the device: the broadcast opens and closes the punctuation menu and steps the input mode. Note this explains a class of bug beyond the keyboard — `dialer`, `Calculator`, `Sfarim`, `Tools` and `Navigation` all bind `KEYCODE_STAR`/`KEYCODE_POUND` as key events and are subject to the same interception.
- **`Settings`: rows were drawn as separate filled pills inside a shared card.** Each `SettingItem` painted its own 6%-fill rounded rectangle, so one card holding three rows read as three cards and the dividers between them were lost in the gaps. Rows are now transparent on the card with the divider doing the separating, and the focus state carries the fill plus the ring — matching the design.
- **`Settings`: the main screen is grouped into named sections.** It was six anonymous cards whose grouping was never stated, so "חיבורים" sat alone in one card while "צלילים" and "תצוגה" shared the next. Now: עיקרי (connections, sounds, display), מכשיר (lock screen, battery, storage, performance), שימוש (notifications, screen time), מערכת (general, apps, about). Only the first two names come from the design image; the other two are ours. The three subtitles in the design were also shortened to keep every row one line high — a two-line row is twice as tall and pushes items off a 960px screen.
- **`Settings`: the disclosure chevron pointed the wrong way.** `SettingItem` used `Icons.AutoMirrored.Rounded.KeyboardArrowLeft`, and the auto-mirrored variant flips to point *right* under the app's RTL layout — away from where tapping the row goes. Now the plain `Icons.Rounded.KeyboardArrowLeft`. Affects every settings row in the app.
- **`Settings`: section headers are no longer painted in the accent color.** `SettingHeader` used `theme.primaryColor`, which made the label above a card louder than the items inside it; it is now muted text, as in the design. Affects the sub-screens that already used it as well as the new main-screen sections.
- **`Settings`: "שורת מצב, מסך נעילה ורקע" is now "מסך נעילה ורקע"** on the main screen, in the search catalog and in its own screen title, with "שורת מצב" moved into the subtitle and kept as a search keyword.

### Added
- `Keyboard/README.md` — documents the previously-unlisted `Keyboard` app (system-wide T9 predictive IME) and registers it in the root README's app table.
- This `CHANGELOG.md`.
- **`Keyboard`: hold-0 voice transcription.** `KeyboardService.onKeyDown` now treats a long-press on `0` (the same `repeatCount` threshold already used for `*`'s long-press language toggle) as a trigger to start `android.speech.SpeechRecognizer` in the IME's current language (Hebrew/English) and commit the recognized text at the cursor; the candidate bar shows a listening indicator while active. Since an `InputMethodService` cannot request runtime permissions itself, `Keyboard/MainActivity`'s setup screen now also offers a button to grant `RECORD_AUDIO`; holding `0` before that's granted (or on a device with no speech-recognition service) shows an inline message in the candidate bar instead of crashing.
- **`FutureUI`: a small green phone icon in the status bar** while `TelecomManager.isInCall()` is true (polled on the same ~15s loop as the other indicators) — previously there was no way to tell a call was still running once you'd left `dialer`'s own screens for another app. Requires the newly-added `READ_PHONE_STATE` permission; degrades silently (icon just doesn't show) if it's ever missing.
- **`Keyboard`: a visible "prediction button."** The single plain `TextView` candidate strip is now a themed (light/dark, via a new `Keyboard/theme/ThemeClient.kt` mirroring `dialer`'s) row of candidate chips with the active suggestion highlighted, plus a `▸ *` hint chip making the existing-but-undiscoverable short-press-`*`-to-cycle gesture visible for the first time. `T9Engine`'s built-in dictionaries grew from ~60 to ~250 words per language, and a new per-language word-frequency map (persisted in the existing `t9_keyboard_prefs`, bumped on every dictionary-word commit) now biases candidate ordering toward words the user actually types, so predictions improve with use instead of being fixed at install time.
- **Extracted the duplicated keypad-nav/T9 stack into a shared module, `SharedKeypadNav/sharedkeypadnav/`** (an `com.android.library` Gradle module, package `com.future.sharednav`, living as a sibling top-level folder — wired into each consumer via cross-directory `include(":sharedkeypadnav")` + a `projectDir` override in that app's own `settings.gradle.kts`; each app remains a fully independent, individually-buildable Gradle/Android Studio project, just with one extra module dependency, no `rootProject.name` changes, no multi-project merge):
  - `T9DigitMap` — the single canonical Hebrew+English T9 digit map, now used by `dialer`'s and `Music`'s `T9Search.kt` and `Keyboard`'s `T9Engine.kt` instead of three hand-rolled copies. Fixed a real drift bug in the process: `Music`'s own Hebrew T9 map was missing the final-form letters (ךםןףץ) on key 9 that `Keyboard`'s copy already had correctly — Hebrew T9 search in Music was silently incomplete for any word ending in a final letter.
  - `FocusableItem` and `ScreenTopBar` — unified into the shared module with primitive `Color`/`Dp`/`Boolean` parameters instead of any app's own `FutureTheme` type, preserving each app's exact prior visual behavior (border width, corner radius, scale-vs-no-scale focus animation, background alpha) as explicit parameter values. `dialer` (which only ever used `MaterialTheme` directly, no custom theme type) was fully migrated to call the shared composables directly — its own `FocusableItem.kt` deleted, all 6 call sites updated. `Music` and `Sfarim` each keep a small app-local wrapper file (`FocusableItem(theme: FutureTheme, ...)`, `ScreenTopBar(theme: FutureTheme, ...)`) that adapts their own theme's colors onto the shared components' parameters, so none of their existing screen-level call sites needed to change while still removing 100% of the duplicated implementation logic.
  - By the end of that migration, 16 of the (then-23) apps depended on `:sharedkeypadnav` — not the 4 (`dialer`, `Music`, `Sfarim`, `Keyboard`) an earlier draft of this entry named; the migration continued past what was recorded here at the time. See the "system-wide design unification" entry below for where that number stands now.
- **Sfarim's `sefaria.db` rebuilt from scratch** (~1.55GB, 6,211 books / ~1.75M segments / 1,258 categories) via `Sfarim/tools/build_library.py` (Sefaria API + Sefaria-Export GCS bucket) + `add_root_category.py` + `add_fulltext_search.py`. The old split/reassemble plan (`reassemble_sefaria_db.sh`) is superseded — see `Sfarim/README.md` for the rebuild command. Integrity-checked (`PRAGMA integrity_check` = ok); one book (`Penei Moshe on Jerusalem Talmud Sanhedrin`) failed on the first pass due to a network error and succeeded on retry (the script is resumable/idempotent by design).

### Fixed
- **CI never built 5 of the 28 apps, so a break in any of them could land on `main` with every job green.** `.github/workflows/build.yml`'s `build-app` matrix carried a hardcoded 23-name list; `Bluetooth`, `Flashlight`, `Frixa`, `SystemUI` and `Tasks` were missing from it. This is the third instance of the same hardcoded-app-list drift found in one pass (after `build-all.sh` and `DEVICE_SETUP.md`) and by far the most damaging, since CI is the one place specifically meant to catch regressions. Fixed by generating the matrix at run time: a new `discover-apps` job finds every top-level directory containing a `settings.gradle.kts`, emits the list as JSON, and `build-app` consumes it via `fromJSON(needs.discover-apps.outputs.apps)`, so a newly added app gets a CI job with no workflow edit. The discovery job also fails loudly if it finds zero apps, so a future layout change surfaces as a red build instead of silently shrinking the matrix to nothing. Verified: the `find`/`jq` pipeline emits all 28 app names as a valid JSON array, and the workflow parses as valid YAML.
- **`Settings`: removed an unused `detectDragGestures` import.** Dead code — it was the only mention of the symbol in the file, and nothing in the app uses drag gestures (correctly so, since this is a keypad-only device with no touchscreen). Confirmed `Settings` still compiles after removal.
- **`build-all.sh` silently skipped 3 of the 28 apps.** The script carried a hardcoded 25-name `APPS` array, and while the repo grew to 28 standalone Gradle projects, `Flashlight`, `Frixa` and `SystemUI` were never added to it — so "builds all apps" quietly meant 25, with no error and no missing-folder warning (the script's existing `-d` guard only fires for a *listed* app whose folder is gone, which is the opposite failure). This is the same drift the script's own header comment was written to complain about in `DEVICE_SETUP.md` §5/§6. Fixed by deriving the list from the filesystem instead: every top-level directory containing a `settings.gradle.kts` is a standalone Gradle project and is therefore an app, so a newly added app is picked up automatically and the list cannot go stale again. `SharedKeypadNav` is correctly excluded by that criterion — it is a `com.android.library` module with no `settings.gradle.kts` of its own.
- **`build-all.sh --install` would fail only after every build had finished.** `adb` is not on `PATH` in Git Bash on Windows (the usual shell for this repo), so the first `adb install -r` failed, and it failed once per app rather than up front. The script now resolves `adb` once before the build loop — via `ANDROID_HOME`, `ANDROID_SDK_ROOT`, and the default SDK locations for Windows, macOS and Linux — and exits immediately with an actionable message if `--install` was requested and no `adb` can be found.
- **The root `README.md` app table was missing `Flashlight`, `Frixa` and `SystemUI`**, and still said `SharedKeypadNav` is depended on by "all 25" apps. Verified by grep that all 28 apps do depend on it; the table now lists all 28 and the count is corrected.
- **`README.md` did not record that `FutureUI` and `SystemUI` are two implementations of the same role.** Both declare the same six services (`StatusBarAccessibilityService`, `LockScreenAccessibilityService`, `NotificationCenterAccessibilityService`, `ControlCenterAccessibilityService`, `MediaControlService`, `HeadsUpNotificationService`) under different application ids — `com.future.futureui` and `com.android.sistemui`, the latter spelled "sistemui" because `com.android.systemui` is a reserved system package that cannot be installed. Enabling both sets of `AccessibilityService`s would put two overlay stacks in competition for the same key events, and since the status bar service swallows `KEYCODE_MENU`/`KEYCODE_SETTINGS` system-wide, a duplicate would break the Options key everywhere. Confirmed against the `F22 Pro` test device: only `com.future.futureui` is installed and only its four overlay services are in `enabled_accessibility_services`. Documented as a note in the README; **which of the two is meant to win long-term is still not recorded anywhere in this repo.**
- **`DEVICE_SETUP.md` said "23 apps" in four places** (the JDK requirement in §2, the `SharedKeypadNav` warning in §5, and the `build-all.sh` description and its code comment) — now 28, matching reality. Its Java 21 list was also incomplete: it named `Fitness`, `Music`, `Navigation` and `notes`, but `Tasks` also sets `sourceCompatibility`/`jvmTarget` to 21, so a JDK-17-only machine would fail on `Tasks` without the doc explaining why. Verified by reading `jvmTarget`/`sourceCompatibility` out of all 28 apps' `build.gradle.kts`: those 5 are on Java 21, the other 23 on Java 11.
- **Verified that all 28 apps build end to end** — `./gradlew assembleDebug` per app, run for real, one app at a time, each confirmed to have produced an `app/build/outputs/apk/debug/app-debug.apk`. This covers resource merging, manifest merging, dexing and packaging, not just Kotlin compilation, so it is the real check that `compileDebugKotlin` alone would not have given. All 5 apps that have real unit tests (`Clock`, `dialer`, `Fitness`, `Keyboard`, plus `:sharedkeypadnav` via `Terminal`) also pass `testDebugUnitTest`. No app is broken; the stale build list, the stale CI matrix and the stale docs above were the actual defects.
- **`dialer`: incoming calls were completely invisible unless the app was already open.** `CallService.onCallAdded`/`onStateChanged` only ever updated in-memory `StateFlow`s — nothing launched an `Activity`, turned the screen on, or asked `FutureUI`'s custom lock-screen overlay to step aside. Fixed by: `CallService` now starts `MainActivity` (`FLAG_ACTIVITY_NEW_TASK`) the moment a call reaches `STATE_RINGING`; `MainActivity.onCreate` calls `setShowWhenLocked(true)`/`setTurnScreenOn(true)` so it can actually appear over a locked/asleep screen; and `CallService` broadcasts new `com.future.futureui.ACTION_CALL_RINGING`/`ACTION_CALL_ENDED` actions (added to `FutureUIActions`) that `FutureUI`'s `LockScreenAccessibilityService` now listens for, hiding its own overlay (and skipping its next `ACTION_SCREEN_ON`-triggered show) while a call is ringing — otherwise that overlay, being the topmost focusable window, would keep grabbing key input away from the in-call screen underneath it even after `dialer`'s side was fixed. All broadcasts are best-effort/try-caught since `FutureUI` isn't guaranteed to be installed in every build.
- **`dialer`: the in-call "keypad" toggle showed a passive digit readout with nothing to actually press.** `InCallScreen` now renders a real 3x4 DTMF grid (0-9, `*`, `#`) of focusable keys above the readout, wired to the existing `onDtmfDigitPressed`/`onDtmfDigitReleased`.
- **`dialer`: a failed call recording looked identical to a successful one.** `InCallViewModel.toggleRecording` now surfaces `CallService.startRecording`'s failure return value as a `Toast` instead of discarding it silently.
- **`FutureUI`: the heads-up notification banner ignored theme entirely**, hardcoding `Color.White`/`Color.Black`/`Color.DarkGray`. Now reads `MaterialTheme.colorScheme` (already available — the screen was already wrapped in `FutureUITheme{}`), so it follows system dark mode instead of always rendering as light glass. Note: `FutureUITheme` itself still isn't wired to the cross-app `shared_theme_prefs`/`ThemeProvider` toggle (only to `isSystemInDarkTheme()`/Material You) — that remains a separate, larger change.
- **`Keyboard`: backspace could silently do nothing once a word was already committed** (cursor past any composing text) — `KEYCODE_DEL` now explicitly falls back to `currentInputConnection.deleteSurroundingText(1, 0)` in that case instead of relying on an unverified default.
- **`FutureLauncher`: double-click on the physical Options key never entered edit mode.** Root cause: `FutureUI`'s `StatusBarAccessibilityService` swallows `KEYCODE_MENU`/`KEYCODE_SETTINGS` system-wide and only ever re-broadcasts a short-press event (`com.future.futureui.ACTION_OPTIONS_SHORT_PRESS`) — the raw key event never reaches any foreground app (this is documented in `Music`'s equivalent code, which already listens for the broadcast correctly). `FutureLauncher/MainActivity.kt` was instead listening for the raw `KEYCODE_MENU` via Compose `onKeyEvent`, which could never fire. Replaced with a `BroadcastReceiver` for `ACTION_OPTIONS_SHORT_PRESS`, mirroring `Music/MusicNavHost.kt`'s pattern; the existing double-click/single-click timing logic (350ms window) is preserved, now driven by broadcast arrivals instead of raw key up/down events.

### System-wide design + infrastructure unification, Clock alarm engine rewrite
Large cross-app pass, verified with real `./gradlew compileDebugKotlin`/`assembleDebug` builds per app (not just code review) and, for the alarm work, a live device (`F22 Pro`).

- **Unified `FutureTheme`/`ThemeClient` into `SharedKeypadNav`.** 17 hand-copied, independently-drifting `FutureTheme.kt` files and 22 hand-copied `ThemeClient.kt` files (three different feature levels: full read/write, read-only, and a trimmed read-only variant) are now one canonical copy each in `com.future.sharednav.theme`, with the app-specific extensions (Calculator's button colors, Terminal's input/output-bar colors, Contact's favorite-star color) exposed as extension properties instead of being duplicated per app. Also added, alongside the theme: `FutureDimens`/`FutureType` (spacing and the font-size-multiplier-aware type scale that previously only existed inside `Settings/ThemeConfig`, so the Settings font-size slider now has somewhere else to apply to), `FutureScreen` (the 640×960 constant that previously existed only as prose in four separate comments), `FocusListState`/`KeypadLazyColumn`/`keypadListNav`/`numericShortcuts` (D-pad list-navigation primitives — see below), `ScreenScaffold`/`EmptyState`/`ConfirmDialog`/`MarqueeText`, and the cross-app broadcast-action constants (`FutureUIActions`, moved out of `FutureUI` with a backward-compatible alias left behind). All new shared code has unit tests (`T9DigitMapTest`, `FocusListStateTest`) — previously the module had none.
- **All 23 apps now depend on `:sharedkeypadnav`** (up from 16) — the 7 that didn't (`Assistant`, `Calculator`, `Camera`, `Clock`, `notes`, `Remote`, `Settings`) are wired in the same way as the others. Wiring in a `com.android.library` module alongside a `com.android.application` module in the same build requires declaring **both** plugins in the root `build.gradle.kts` (`alias(libs.plugins.android.library) apply false`) — omitting it produces an opaque "plugin already on the classpath with an unknown version" failure at the shared module, not at the app that's missing the declaration. All 7 needed this plus a missing `android-library` catalog alias added to their `libs.versions.toml`.
- **`notes` needed a real compatibility fix, not just wiring.** It's the one app that sets `android.builtInKotlin=false` (a workaround for a KSP/Room conflict) — since that Gradle property is global to the whole build tree including cross-directory included modules, it silently left `:sharedkeypadnav` with no Kotlin compilation task at all when built from `notes` (no `compileDebugKotlin` task registered → `Unresolved reference` on every shared symbol). Fixed in the shared module's own `build.gradle.kts`: conditionally `apply(plugin = "org.jetbrains.kotlin.android")` only when that property is `"false"`, plus an explicit `jvmTarget = 11` via `tasks.withType<KotlinCompile>` (the classic plugin's default `jvmTarget` otherwise follows the *consuming* app — 21 for `notes` — instead of the shared module's own `compileOptions`, breaking the other 21 apps' assumption of Java 11 there). This is exactly the class of cross-consumer compatibility gap the previous entry in this file flagged as unverified for `dialer`/`Keyboard`; here it was real, for a different app, and is now fixed and confirmed by a passing build.
- **Deleted the Android Studio default `Color.kt`/`Type.kt`/`Theme.kt` trio in `Calendar`, `Contact`, `Files`, `Gallery`, `Terminal`, `Tools`** after confirming (by grep, not assumption) that none of them ever actually invoke their own generated `XxxTheme{}` wrapper — all six apps theme exclusively through `FutureTheme`/`ThemeClient`. The same-looking trio in `FutureUI`, `Settings`, `dialer`, `FutureLauncher`, `Messages`, `Navigation`, and `notes` is genuinely used (confirmed by the same check) and was left alone.
- **`notes`: deleted `ui/components/FocusUtils.kt`**, a file whose own doc comment said it duplicated the shared `FocusableItem`. Its `Modifier.dpadFocusBorder(isFocused, shape)` is a `Modifier`-extension, not a content-wrapping composable like `FocusableItem` — the two aren't drop-in replacements for `notes`'s 9 call sites (`OutlinedTextField`, `Card`, `FloatingActionButton`, `IconButton`, each already using `onFocusChanged` + a local `isFocused` var), so rather than force a riskier rewrite of `EditorScreen`/`ListScreen`, the same `Modifier` extension moved into the shared module (`com.future.sharednav.focus.dpadFocusBorder`) verbatim — a pure import swap at every call site.
- **`Clock`: the alarm clock did not actually work.** `AlarmLogic.scheduleAlarm` never read `Alarm.days`, so every alarm — including ones a future UI might mark as recurring — was scheduled as a one-shot for the next occurrence of its time and, on top of that, `AlarmReceiver` never rescheduled anything after firing, so even that one-shot never repeated. Separately, the receiver was also registered for `BOOT_COMPLETED` but never checked `intent.action`, so every device reboot made the phone vibrate and show a "the alarm is ringing" toast for no reason, while real alarms were *not* rescheduled after that same reboot (`AlarmManager` doesn't persist alarms across reboots on its own). Fixed:
  - `AlarmLogic.nextTriggerMillis` now honors `days` (weekly recurrence, computed against the real device calendar/timezone) with `days.isEmpty()` meaning one-shot, matching the field's original intent.
  - `AlarmReceiver` branches on `intent.action`: `ACTION_BOOT_COMPLETED` reschedules every enabled alarm from storage; a new explicit `com.future.clock.ACTION_ALARM_FIRED` (set on the `PendingIntent`, not inferred) handles an alarm actually firing.
  - Firing reschedules just that one alarm to its next occurrence (recurring) or disables it (one-shot, so it doesn't silently vanish from the list without explanation) — not a full re-scan/re-schedule of every alarm on every fire.
  - `AlarmManager.canScheduleExactAlarms()` is checked before `setExactAndAllowWhileIdle`, falling back to inexact scheduling instead of crashing with `SecurityException` if the user has revoked the permission (revocable at any time on API 31+).
  - A new `AlarmRingActivity` (`showWhenLocked`/`turnScreenOn`, keypad-only: OK dismisses, any other key snoozes 5 minutes via a second, independent `PendingIntent`) replaces the previous `Toast`-only "ringing" experience, which had no sound, no way to actually dismiss it, and no snooze.
  - `AlarmScreen`'s time editor gained a 7-day repeat picker (`א`-`ש`) — previously `days` had no UI path to ever become non-empty, so recurrence was unreachable regardless of the scheduling logic.
  - Safe JSON parsing (`getAlarms` no longer throws on a corrupted `alarms_prefs` entry) and `Calendar.MILLISECOND` is now zeroed alongside `SECOND` when computing trigger times.
  - Verified on a physical device: a two-day recurring alarm (Thu+Sat) scheduled from a Friday correctly resolved to the nearer Saturday (`dumpsys alarm` `origWhen`), `exactAllowReason=permission` confirms the exact-alarm path, and re-firing the same alarm id repeatedly updates its single `PendingIntent` in place rather than accumulating duplicates.
- Root README and `DEVICE_SETUP.md` updated to match the state above: the app table now lists `Calculator`, `Clock`, and `Fitness` (previously present in the repo but missing from the table); `DEVICE_SETUP.md`'s build/install loops now cover all 23 apps instead of 19; its JDK requirement now mentions the 4 apps needing Java 21; its `SharedKeypadNav` dependency warning now says "all 23 apps," not "`dialer`, `Music`, `Sfarim`, `Keyboard`"; and it now points at `hardware/openscad/qin_f22_pro_case_keyboard.scad` instead of saying no device model is documented.

### Known issues / open work
- **`FutureUI`, `Settings` and `SystemUI` sit at `targetSdk = 31` while the other 25 apps are at 37, and nothing records whether that is deliberate.** `compileSdk = 37` and `minSdk = 31` are uniform across all 28 apps, so `targetSdk` is the only axis that diverges — and it diverges on exactly the three apps that do the most privileged work (accessibility overlays, system-settings writes, root operations). That is consistent with a deliberate choice to stay below the API 33/34 behavior changes those apps would be subject to: the runtime notification permission, restricted context-registered receivers, mandatory foreground-service types, and restricted implicit intents. It is equally consistent with the three simply having been missed by the "align build config" pass. There is no comment in any of the three `build.gradle.kts` files either way. **Left unchanged deliberately** — bumping the system shell's `targetSdk` on a guess could break the overlays that the whole OS depends on, and the test device is API 31 so the higher target buys nothing today. Needs a decision, and once made, a comment in those three files so the next config-alignment pass does not silently "fix" it.
- **Two packages are installed on the `F22 Pro` test device with no corresponding folder in this repo: `com.future.gotitdone` and `com.future.mikdash`.** Neither appears in `enabled_accessibility_services`, so they are inert leftovers rather than active components, but they most likely still occupy launcher entries. They look like apps that were renamed or dropped during the repo consolidation (`gotitdone` plausibly predates `Tasks`). Not uninstalled — that is a device-state change, and it is worth confirming they hold no data worth keeping first.
- **Test coverage is still thin — 8 real test files across 4 of 28 apps.** `Fitness` (`HeartRateMonitorTest`, `WorkoutTemplateMappingTest`, `WorkoutStoreCalculationsTest`), `Keyboard` (`T9EngineTest`), `Clock` (`AlarmLogicTest`) and `dialer` (`T9SearchTest`), plus the shared module's `FocusListStateTest` and `T9DigitMapTest`. The other 24 apps still ship only the Android Studio default `ExampleUnitTest`/`ExampleInstrumentedTest` boilerplate. CI now exists (`.github/workflows/build.yml`) and runs `testDebugUnitTest` on every app, so those 24 apps' CI test steps pass without asserting anything real.
- **The 28 debug APKs total about 2.0GB installed**, on a feature phone. Four apps dominate: `Assistant` (189MB, the native speech/TTS libraries), `Keyboard` (173MB, the bundled dictionaries), `Tools` (129MB) and `Navigation` (117MB); the other 24 sit near a ~60MB floor, which is itself high for apps this small and points at unstripped native ABIs and the un-minified Compose runtime being packaged whole. Release builds would be smaller, but with R8 off everywhere (next item) that gap is currently unrealised, and no app sets `abiFilters` or an ABI split.
- **R8/minify is off in all 28 apps' release builds**, and there is no `proguard-rules.pro` anywhere in the repo. Note the mechanism: `isMinifyEnabled` is not set to `false` in any app, it is absent entirely, so all 28 rely on the AGP default. Turning it on would need real keep rules per app — reflection-driven code (Room entities, `AccessibilityService` subclasses named in XML, the `KeyboardService` IME) is exactly what R8 strips without them.
- `dialer`'s and `Terminal`'s theme choices (a Material3 alpha-based `surface` tonal system in `dialer`, a green `accentColor` default in `Terminal`) look like coherent, self-consistent design decisions on inspection, not drift — left as-is rather than force-aligned to the majority's flat colors, per this project's "don't invent new designs" rule.

## History (reconstructed from existing docs)

- **Settings** — most recent significant work was a "V7 Full Activation" effort (root-permission auto-grant, real-time sync of toggles to system state), documented in `Settings/implementation_plan.artifact.md`, `Settings/task.artifact.md`, and `Settings/walkthrough.artifact.md`. All tracked tasks in that effort are complete.
- **Sfarim** — architecture (manual backstack `Route`, `T9Search`, `FocusableItem`, `ScreenTopBar`) was established here first and explicitly used as the template for Music's implementation (see `Sfarim/README.md`).
- **Repo consolidation** — this repo is a fresh consolidation of previously-separate per-app projects; no prior git history was carried over (see root README).
