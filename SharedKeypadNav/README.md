# SharedKeypadNav (`com.future.sharednav`)

The one real shared library in FutureOS, and — as of this consolidation — the
**canonical design system** referenced by `CLAUDE.md`'s "נאמנות מלאה
לדיזיין סיסטם" (full design-system fidelity) rule. If you're adding or
changing UI anywhere in the suite, the tokens and components below are what
that rule points at; don't hand-roll a color, spacing value, or focus
behavior that already exists here.

It is a Gradle `com.android.library` module (package `com.future.sharednav`)
living as a sibling folder to every app, not a submodule of any one of them —
each consumer wires it in with a cross-directory `include(":sharedkeypadnav")`
+ `projectDir` override in its own `settings.gradle.kts` (see any app's file
for the exact snippet). **All 23 apps in the repo depend on it.** Do not move
a single app folder out of the repo without this one too, and do not move
this folder without every app.

## Theme (`theme/`)

- **`FutureTheme`** — the single color-token set for the whole suite:
  `backgroundColor`, `surfaceColor`, `textColor`, `dangerColor`,
  `successColor`, `warningColor`, derived from `isDarkMode` + `accentColor`.
  App-specific extras are extension properties on `FutureTheme` in the same
  file, not new fields on the class: `calcButtonColor` /
  `calcMutedButtonColor` / `calcButtonFocusedColor` (Calculator),
  `inputBarColor` / `outputTextColor` (Terminal), `favoriteColor` (Contact).
  Add new app-specific colors the same way — an extension property here, not
  a duplicated theme class in the app.
- **`ThemeClient`** — reads/writes the cross-app theme via `FutureUI`'s
  `ThemeProvider` `ContentProvider` (`content://com.future.futureui.theme/theme`).
  Falls back to `(isDarkMode = true, primaryColor = Color.WHITE)` if
  `FutureUI` isn't installed or the query fails.
- **`FutureDimens`** — `itemSpacing` (12dp), `borderRadius` (22dp),
  `itemCornerRadius` (8dp), `cardCornerRadius` (16dp), `screenPadding` (16dp).
- **`FutureType`** — the type scale (`baseFontSize` 16sp, `titleFontSize`
  17sp, `summaryFontSize` 13sp, `headerFontSize` 34sp, `screenTitleFontSize`
  20sp), parameterized by `fontSizeMultiplier` so Settings' font-size slider
  can drive it system-wide instead of only affecting the Settings screen.

## Screen constant (`FutureScreen.kt`)

`FutureScreen.WIDTH_DP` / `HEIGHT_DP` (640×960, matching `CLAUDE.md`) and
`.width` / `.height` as `Dp`. Use this instead of a bare `640`/`960` literal
or a comment — e.g. `@Preview(widthDp = FutureScreen.WIDTH_DP, heightDp =
FutureScreen.HEIGHT_DP)`.

## Keypad navigation (`nav/`)

- **`rememberFocusListState(itemCount, initialIndex)`** — tracks which item
  in a list is focused (`rememberSaveable`, survives rotation/process death)
  and exposes a `LazyListState` that's kept in sync.
- **`Modifier.keypadListNav(state, onSelect, horizontal)`** — `DirectionUp`/
  `DirectionDown` (or `Left`/`Right` when `horizontal = true`) move focus,
  `DirectionCenter`/`Enter`/`NumPadEnter` select. This is the one place that
  should own D-pad-move-and-select logic — don't reimplement the
  up/down/select `when` block per screen.
- **`Modifier.numericShortcuts(itemCount, onSelect)`** — maps digit keys 1-9
  to direct selection by position, for short menus (≤9 items).
- BACK is deliberately **not** handled here — the established pattern
  (`androidx.activity.compose.BackHandler`) is a screen-level concern, not a
  list-navigation one.

## Components (`components/`)

- **`FocusableItem`** — a focus-highlighted `Box` (border + background tint
  + optional scale on focus) around arbitrary `content`. Colors/dimensions
  are plain `Color`/`Dp` parameters — this file has no dependency on any
  app's `FutureTheme`, so pass yours in at the call site.
- **`ScreenTopBar`** / **`TopBarIconButton`** — the standard back-button +
  title + optional trailing-action row.
- **`ScreenScaffold`** — a full screen: theme background + forced RTL +
  optional `ScreenTopBar`. Prefer this over manually repeating
  `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)`
  around a `Box`.
- **`KeypadLazyColumn`** — a `LazyColumn` pre-wired to `keypadListNav` that
  **auto-scrolls to the focused item**. Before this existed, only 2 of the
  59 files in the suite using `LazyColumn`/`LazyRow`/`LazyVerticalGrid`
  actually did that — everywhere else, the focused row could scroll off
  screen and the user would lose track of it. Prefer this over a raw
  `LazyColumn` for any focusable list.
- **`EmptyState`** — icon + title + optional subtitle for an empty list.
- **`ConfirmDialog`** — the standard cancel/confirm dialog for destructive
  actions (delete, etc.), keypad-navigable with a visible focus ring (not
  color-only).
- **`MarqueeText`** — single-line text that scrolls (`basicMarquee`) while
  focused and ellipsizes otherwise, for content that can overflow the fixed
  640px width (file names, song titles, message subjects).

## Focus utilities (`focus/`)

- **`Modifier.dpadFocusBorder(isFocused, shape)`** — the `Modifier`-extension
  sibling of `FocusableItem`, for wrapping an *existing* focusable component
  (`OutlinedTextField`, `Card`, `FloatingActionButton`, a plain `IconButton`)
  instead of putting it inside a new `Box`. Use this when you already have a
  focusable Material3 component and just need the focus ring; use
  `FocusableItem` when you're building a focusable row from scratch.

## T9 (`t9/`)

**`T9DigitMap`** — the one canonical Hebrew+English digit→letters map
(`ENGLISH`, `HEBREW`, `isHebrew(Char)`). The Hebrew map matches the physical
keycaps of the target device, not the ISO T9 standard, and includes final-form
letters (ךםןףץ) folded onto the key of their base letter. This is the raw
map only — search/prediction algorithms (`T9Search`, `T9Engine`) stay
per-app since they're genuinely different algorithms that just happen to
share this table.

## Cross-app broadcast actions (`actions/`)

**`FutureUIActions`** — the `com.future.futureui.ACTION_*` /
`com.future.dialer.ACTION_*` broadcast-action string constants shared between
`FutureUI` and its counterparts (`dialer`'s call-ringing signaling, the
status-bar Options-key short-press broadcast, etc.). Import this instead of
retyping the string literal.

## Testing

`src/test/java/com/future/sharednav/` has unit tests for `T9DigitMap` and
`FocusListState`. Run with `./gradlew :sharedkeypadnav:testDebugUnitTest`
from any consuming app's directory. Add tests here, not just in the
consuming apps, when you change shared logic — this module is depended on
by all 23 apps and had zero test coverage before.

## A note for `notes/`

`notes/` sets `android.builtInKotlin=false` in its own `gradle.properties`
(a workaround for a KSP/Room conflict). That Gradle property is global to
the whole build tree, including this module when built from `notes`'s
context — so this module's `build.gradle.kts` conditionally applies the
classic `org.jetbrains.kotlin.android` plugin (with an explicit `jvmTarget`)
only in that case. If you add a new consumer with an unusual Gradle setup,
build it (`./gradlew :app:compileDebugKotlin`) before assuming this module
"just works" under it — it didn't, the first time, for exactly this reason.
