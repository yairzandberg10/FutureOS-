# FutureOS Design System

FutureOS is a Hebrew-language Android system for a **keypad feature phone** — a launcher plus a
suite of first-party apps (contacts, dialer, messages, clock, settings, calculator, terminal,
gallery, calendar, notes, music, files, fitness). It is not a touch product. Every interaction
arrives from a physical key: the four-way pad, the confirm key, the back key, the menu key, and
the number keys 1–9. There is no pointer, so there is no hover state, no press state and no drag
state anywhere in the system. **Focus is the entire interaction model.**

Two constants shape every decision below:

- **One screen size, forever.** 640 × 960 device pixels at density 2.0. `px = dp × 2`, exactly,
  everywhere. There are no breakpoints and no responsive rules — a 640 × 960 frame *is* the device.
  All token values in this project are stated in device pixels, with the dp/sp original in a comment.
- **Right-to-left, forced.** Every screen is wrapped and has RTL direction imposed on it at the
  scaffold level (`ScreenScaffold`). Layout, focus order and even the meaning of the arrow keys
  follow from that: **left is forward**, right is back.

## Sources

Everything here is derived from one mounted, read-only codebase:

- **`design-system/`** — a local folder attached to this project (read-only mount). It is the
  Figma-export pipeline for FutureOS, not the app itself. Contents:
  - `tokens.py` — the source of truth. Every color, size, radius, duration and component
    measurement, each annotated with the Kotlin file it was lifted from.
  - `futureos.tokens.json` — the same values in DTCG format (Tokens Studio for Figma).
  - `generate.py`, `generate2.py`, `svg_lib.py`, `build.py` — renderers that draw the 15 spec
    sheets. Reading them gives the exact composition of each component.
  - `00-coverage.svg` … `14-more-components.svg` — the rendered spec sheets.
  - `check_svg.py`, `contrast.py` — validators; `contrast.py` computes the contrast table.
  - `README.md` — Hebrew instructions for importing into Figma.

The app source itself (`FutureTheme.kt`, `ThemeConfig.kt`, `FutureDimens.kt`, `FutureType.kt`,
`SettingsScreens.kt`, `FocusableItem`, `ConfirmDialog`, `SettingItem`, `VolumeSlider`,
`GalleryTabChip`, `TimePickerOverlay`, `HeadsUpNotificationScreen`, …) was **not** attached. It is
cited by name throughout `tokens.py`, and this design system cites the same names, but the Kotlin
was read second-hand through the token pipeline. No Figma file and no slide deck were provided.

**There is no logo or brand mark in the sources.** Nothing was drawn to fill the gap: wherever a
mark would go, the word *FutureOS* is set in plain type. If a mark exists, drop the SVG into
`assets/` and it can be wired in.

## Content fundamentals

Copy is **Hebrew**, short, and functional. The system never explains itself and never has a voice.

- **Length.** Labels are one or two words: `בהירות`, `גודל טקסט`, `שפה`, `שתף`, `העתק`, `פרטים`.
  Nothing in the interface is a sentence except an empty state's second line.
- **Person.** The interface addresses the user in **second-person masculine singular imperative**,
  the Android Hebrew convention: `לחץ על מקש התפריט כדי להוסיף` ("press the menu key to add").
  It never says "I", never says "we", and never refers to itself as an assistant or a system.
- **Destructive confirmations are questions in the infinitive**, subject last:
  `למחוק את איש הקשר?` ("delete the contact?"). The buttons answer the question with bare verbs —
  `מחק` / `ביטול` — never "yes"/"no", never a restated sentence.
- **The title/summary pair** is the workhorse pattern. Title states the thing, summary states its
  current value or a one-clause qualifier: `בהירות` / `אוטומטית`, `גודל טקסט` / `רגיל`,
  `מצב כהה` / `מופעל`, `בהירות אדפטיבית` / `התאמה לתאורת הסביבה`. Values, not descriptions.
  Never "tap to change", never a marketing clause.
- **Empty states are two lines**: a neutral statement of absence at 70% opacity, then the single
  key that fixes it at 40% — `אין אנשי קשר` / `לחץ על מקש התפריט כדי להוסיף`. No illustration
  beyond one outlined glyph, no "get started", no exclamation mark.
- **Punctuation.** No terminal period on labels or summaries. Question marks only on
  confirmations. No ellipses, no em dashes, no parentheses.
- **Casing.** Hebrew has no case. Where Latin text appears it is sentence case; section headers
  (`תצוגה`, "Display") are set at 13sp / 55% opacity with 1sp letter-spacing rather than uppercased.
- **Numerals stay LTR inside Hebrew lines.** Phone numbers, times and counts do not mirror:
  `052-3334455` reads left-to-right inside a right-to-left row.
- **No emoji. Anywhere.** Not in labels, not in empty states, not in notifications. The device
  font renders them poorly at this size and the brand has no playful register.
- **Vibe.** Utilitarian and quiet, close to stock Android Settings in Hebrew. The product's
  promise is legibility and calm, not personality. When writing new copy, ask "what would the
  Hebrew Android Settings screen say" and cut a word.

## Visual foundations

### Color

Dark is the default and the designed-for mode; light is a faithful inversion, not a separate
identity. Seven core colors per theme, and that is the whole palette:

| role | dark | light |
|---|---|---|
| screen background | `#000000` | `#F2F2F7` |
| surface / card | `#1C1C1E` | `#FFFFFF` |
| raised surface (glass) | `#2C2C2E` | `#EDEDF2` |
| primary text | `#FFFFFF` | `#000000` |
| danger | `#FF6B6B` | `#D32F2F` |
| success | `#32D74B` | `#1E8E3E` |
| warning | `#FFD60A` | `#B8860B` |

**Accent is the only color the user controls** — one choice, in settings, from five values:
white (default), cyan `#64D2FF`, orange `#FF9F0A`, green `#30D158`, purple `#BF5AF2`. Accent means
exactly one thing: *this is where your focus or your selection is*. It is never decoration, never a
brand fill, never a gradient. Because the default accent is white, **nothing in the system may rely
on the accent being chromatic** — a white-on-white switch thumb is a real defect in the source, and
new components must survive a white accent. `Switch` is where this was fixed: an on switch fills its
track with the accent and puts the thumb in the screen color, so the state reads at any accent.

Depth and separation come from **alpha over the text color**, not from new hues. The ladder is
fixed and small: 70% (empty-state title, secondary dialog button), 60% (row summary, slider label),
55% (section header), 40% (empty-state subtitle and glyph), 30% (chevron), 18% (focused chip /
slider row), 14% (focused list row, as accent), 12% (divider, focused menu row), 10% (quiet button),
8% (idle field, idle icon button), 6% (idle chip, focused setting row).

Imagery: there is none. No photography, no gradients, no illustration, no texture, no pattern, no
noise. The background is a flat fill. Anything that looks like a picture in the UI is a user's own
photo in the gallery.

### Type

One family, seven sizes, and a global multiplier. The device renders in the Android system font
(Roboto); the Figma pipeline substitutes **Heebo**, which is metrically close and covers Hebrew, so
Heebo is what this project ships. **Roboto Mono** carries numeric values only — the time picker,
and nothing else prose-like.

| token | sp | px | weight | used for |
|---|---|---|---|---|
| `headerFontSize` | 34 | 68 | 700 | the one big title on a launch screen |
| `screenTitleFontSize` | 20 | 40 | 700 | `ScreenTopBar` |
| `titleFontSize` | 17 | 34 | 500 | list row title, empty-state title |
| `baseFontSize` | 16 | 32 | 400 | body, buttons |
| `dialogFontSize` | 15 | 30 | 700 | `ConfirmDialog` message, menu row, field |
| `bodyFontSize` | 14 | 28 | 400 | empty-state subtitle, slider label |
| `summaryFontSize` | 13 | 26 | 400 | row summary, section header |

Line height is 1.3 throughout. Letter-spacing is 0 everywhere except section headers (1sp). The
settings text-size slider multiplies the whole scale by 0.8 / 1.0 / 1.2 / 1.4 — **any new layout
must survive 1.4×**, so never set a fixed height on a box that holds text.

### Spacing and shape

Two named spacing tokens only: `itemSpacing` 12dp (24px) and `screenPadding` 16dp (32px). Other
paddings are per-component and are recorded verbatim in `tokens/spacing.css` — do not snap them to
a 4/8 grid, the source's 0.8dp divider and 1.5dp focus border are real.

Radii, five steps plus full round: item 8dp, field 10dp, tab 12dp, chip 14dp, card 16dp, dialog
20dp, main/glass 22dp, heads-up 28dp, full (buttons, switch track, day chip, nav indicator).
Corners are always rounded — there is not one square-cornered element in the system.

Cards: a filled `surfaceColor` rectangle at 22dp radius, inset 16dp from the screen edge, holding
**transparent** rows separated by a 0.8dp hairline divider inset 16dp from the card edge. No card
border. No colored left border. The only real shadow in the system lives here: 4dp in dark, 1dp in
light. Everything else is flat.

### Elevation

Six levels, and only two of them use a shadow:

0 screen background · 1–2 surface (card, with its shadow) · 3 raised "glass" — *a lighter tone,
no shadow at all* · 4 dialog — surface plus a 60% black scrim over the screen behind ·
5 heads-up notification — `#1C1C1E` at 90% with a 0.5dp 15% white hairline, floating over
everything and **always dark even in light mode**.

### States

Only three states exist: **normal, focused, selected**. Hover, pressed and dragged are physically
impossible on the device, and there is no uniform disabled treatment in the source (a row you
cannot activate looks like a row you can — a known gap).

- **Focused** = a background tint + a border, and *sometimes* a 1.02 scale. List rows and dialogs
  scale; setting rows, menu rows, chips and fields do not. Border is 1.5dp on list rows, 2dp on
  controls, always in the accent color. Menu rows are the exception: background tint only, no border.
- **Selected** is always stronger than focused: a **solid accent fill with black text**
  (`--fos-on-accent`). Selected chips, the selected day, the bottom-nav indicator and the primary
  dialog button all read this way.
- Anything that cannot receive focus cannot be activated at all. That is a design rule, not an
  implementation detail — the bottom nav is deliberately unfocusable because the screen-level arrow
  keys move between its tabs.

### Motion

Motion is functional and short. The shared components declare no `animationSpec`, so focus moves on
Compose's default spring — **no bounce**, medium stiffness. Explicit tweens across the apps cluster
at 200ms (by far the most common), with 150/180/220/250 nearby, 300ms for screen transitions and
450–600ms for long content. The proposed unified scale: instant 0 (user disabled animation),
fast 150 (color/alpha in place), standard 200 (focus move, menu open), emphasized 300 (screen
change), slow 450.

Easings are the four Android curves and nothing else: `FastOutSlowIn` (0.4, 0, 0.2, 1) as the
two-way default, `LinearOutSlowIn` for entering, `FastOutLinearIn` for leaving, `Linear` for
progress bars and timers. No spring overshoot, no bounce, no parallax, no cross-fade between
screens beyond a plain fade.

### Layout rules

The screen is a fixed scaffold: top bar pinned at the top (16dp / 12dp padding, 36dp icon buttons),
content scrolling beneath it *to the focused item*, and — in three apps only — a 160px bottom nav
pinned at the bottom. Dialogs are 85% of screen width, max 86% of screen height, centered, focus
trapped inside. Menus are 85% width, anchored 40px below the top of the overlay. The heads-up
notification is inset 12dp from the sides, 8dp from the top.

Grids exist in the launcher only, at fixed counts: app drawer 4 columns, folder contents 3 columns,
folder preview 2 columns. No fluid grid anywhere.

Transparency and blur: **there is no blur in this system.** "Glass" is a lighter opaque tone, not a
backdrop filter. Transparency is used for exactly two jobs — the alpha ladder over text, and the
60% black scrim behind dialogs and menus. No protection gradients; a scrim or a solid capsule does
that job instead.

### Focus target sizes

Touch targets do not apply (no touch). What matters is that the focused row is unmistakable at a
glance, which is why rows are tall: list row 112px, setting row 108px, menu row 100px, dialog
button 88px, top-bar icon button 72px.

### Accessibility notes from the source

Contrast is computed in `contrast.py`; **five color pairs fail**. The known offenders are the
low-alpha text steps (40% and below) and warning/success on their own surfaces. Treat 40% text as
decoration-adjacent: never put the only copy of a piece of information there. RTL mirroring has
five explicit rules, including two arrows that deliberately do *not* mirror: the chevron at the end
of a row points **left** because left is forward, while the back arrow mirrors to point right.

## Iconography

- **The icon set is Material Symbols Rounded** — the source uses `Icons.Rounded.*` and
  `Icons.AutoMirrored.Rounded.*` (e.g. `Icons.AutoMirrored.Rounded.ArrowBack`,
  `Icons.Rounded.KeyboardArrowLeft`). No custom icon was found anywhere in the pipeline.
- **No icon binaries shipped with the source** (the SVG spec sheets draw deliberate placeholder
  shapes — circles and rounded rectangles — and say so in their own notes). So this project loads
  **Material Symbols Rounded from Google Fonts as a variable icon font** and exposes it through the
  `Icon` component. This is the original set, not a lookalike substitution, but it is loaded from a
  CDN rather than copied from the repo — flagged in the caveats.
- Sizes are small and fixed: 18dp in the top bar and the row chevron, 20dp in a menu row, 22dp in a
  setting row, 34dp for an app icon in a notification, 56dp for the empty-state glyph.
- Icons are **outlined at weight 300–400, never filled**, except the favorite star (`#FFC107`,
  filled) and the selected bottom-nav icon (which goes black on the accent pill).
- Icons take the accent color in setting rows, the text color in the top bar and menus, and the
  danger color on a destructive menu row. An icon never carries its own brand color.
- **No emoji, ever.** No Unicode dingbats used as icons either — the colon in the time picker and
  the chevrons are the only glyph-as-graphic in the system, and the chevrons are real icons.

## Intentional additions

Three components here have no single shared implementation in the source; they are consolidations
of repeated local patterns, and they are noted so no one mistakes them for existing API:

- **`Button`** — the source has no shared button component. Four patterns recur (primary/accent,
  destructive, secondary/70% text, quiet/10% text); `Button` unifies them with the geometry of
  `DialogButton` (20dp radius, 24/12dp padding, 16sp, 2dp white focus border, 70% idle alpha).
- **`FosIcon`** — the custom set ("Future Glyphs"): ~95 icons drawn on a 24 grid with a 1.6
  stroke, round caps and joins, built from the circle, the 3-radius rounded rect and the
  45°/90° line. Names match the Material Symbols names already in use, and an undrawn name
  falls through to `Icon`, so the set can grow without breaking a screen. `fill={1}` fills the
  closed shapes (star, bookmark, play) for selected states.
- **`Icon`** — a thin wrapper over the Material Symbols Rounded font, so consumers stop hand-rolling
  SVG. No visual decisions of its own. Every component that draws a glyph now renders `FosIcon`,
  so `Icon` is only reached as the fallback for a name the custom set has not drawn.
- **`InputDialog`** — exists in the source (files app) but with a focus-border inconsistency; the
  version here follows `ConfirmDialog`, which is the shared and correct one.

Nothing else was invented. Components the coverage map lists as missing — **radio button, date
picker, bottom toolbar** — are deliberately absent here too. Checkbox (one occurrence, in the music
player) and floating action button (one occurrence, in notes) were also left out as
non-systemic; if you need them, they should be designed rather than reconstructed.

## Index

**Foundations** — `styles.css` is the single entry point consumers link. It imports:

| file | contents |
|---|---|
| `tokens/fonts.css` | Heebo, Roboto Mono, Material Symbols Rounded; the `.fos-icon` class |
| `tokens/colors.css` | core dark/light, accent palette, alpha ladder, app extensions |
| `tokens/typography.css` | the seven sizes, weights, the 0.8–1.4 multiplier |
| `tokens/spacing.css` | the two named tokens, the dp ladder, screen size, focus row heights |
| `tokens/shape.css` | nine radii, border widths |
| `tokens/focus.css` | focus borders, scale, and every focus/idle background |
| `tokens/motion.css` | five durations, four easings |
| `tokens/elevation.css` | the two real shadows |

**Components** — `components/<group>/`, each with `.jsx`, `.d.ts`, `.prompt.md`, and one card:

- `core/` — `TopBar`, `IconButton`, `ListItem`, `EmptyState`, `Button`, `Card`, `SectionHeader`,
  `Divider`, `Icon`, `FosIcon`, `Avatar`, `ScreenHero`, `MonoValue`, `ActionGrid`
- `forms/` — `Switch`, `Slider`, `TextField`, `TextArea`, `Chip`, `DayChip`, `Capsule`,
  `ToggleButton`, `Checkbox`, `RadioButton`, `TimePicker`, `DatePicker`, `SettingItem`
- `feedback/` — `ConfirmDialog`, `InputDialog`, `HeadsUpNotification`, `Snackbar`, `ProgressBar`,
  `Spinner`, `Badge`
- `navigation/` — `BottomNav`, `SoftKeyBar`, `OptionsMenu`, `TabRow`

**UI kits** — `ui_kits/<product>/`, click-through recreations at 640 × 960:

- `settings/` — root list, display, accent picker, sound, about; live accent and text-size
- `communication/` — recents, contacts (plus empty state), message threads, options menu, heads-up
- `clock/` — clock, alarm list with switches, the full-screen time picker, stopwatch empty state
- `launcher/` — home, 4-column app drawer, 3-column folder (the only documented launcher surfaces)
- `bluetooth/` — radio toggle, paired and nearby lists, device screen, pairing and forget dialogs
  (designed against the system; no Bluetooth app exists in the sources)
- `calls/` — call log, keypad, favorites, search, contact, incoming / active / ended call
  (a redesign of the calls surface as a standalone app)
- `translate/` — language bar and picker, keypad entry, result actions, history tabs,
  conversation mode (designed against the system; no translation app exists in the sources)

**Guidelines** — `guidelines/` holds the foundation specimen cards that populate the Design System
tab (type, color, spacing, shape, focus, motion, keypad model).

**Assets** — `assets/` holds no logo, by design (see Sources). Two icon sources: the custom
`FosIcon` set drawn in `components/core/FosIcon.jsx`, and the Material Symbols Rounded webfont
declared in `tokens/fonts.css`, which backs any name the custom set has not drawn yet.

**Templates** — `templates/futureos-screen/` is the starting point a consuming project copies: a
640 × 960 device frame with a `TopBar`, a focus-scrolling list of `ListItem` rows, the menu-key
`OptionsMenu` and a `ConfirmDialog`, with keypad navigation already wired.

`SKILL.md` makes this folder usable as an Agent Skill.
