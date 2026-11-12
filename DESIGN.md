---
name: FutureOS Landing
description: A luxury black-and-red Hebrew landing page for a keypad phone, where red is drawn like engineering marks on the real device render and a working launcher sits on the real front photo.
colors:
  ground-black: "#050505"
  panel-black: "#0D0D0D"
  screen-black: "#070707"
  device-graphite: "#151516"
  ink: "#F4F2F0"
  signal-red: "#FF3434"
  signal-red-hover: "#E01438"
  signal-red-bright: "#FF3434"
  ink-74: "rgba(244,242,240,.74)"
  ink-62: "rgba(244,242,240,.62)"
  hairline: "rgba(244,242,240,.13)"
  hairline-red: "rgba(200,16,46,.7)"
  on-red: "#FFFFFF"
typography:
  display:
    fontFamily: "Frank Ruhl Libre, Georgia, serif"
    fontSize: "clamp(3.7rem, 19vw, 6rem)"
    fontWeight: 900
    lineHeight: 0.98
    letterSpacing: "-0.01em"
  statement:
    fontFamily: "Frank Ruhl Libre, Georgia, serif"
    fontSize: "clamp(2.6rem, 12.6vw, 6rem)"
    fontWeight: 900
    lineHeight: 1.04
    letterSpacing: "-0.01em"
  headline:
    fontFamily: "Frank Ruhl Libre, Georgia, serif"
    fontSize: "clamp(2.2rem, 9vw, 3.8rem)"
    fontWeight: 700
    lineHeight: 1.06
    letterSpacing: "-0.005em"
  title:
    fontFamily: "Frank Ruhl Libre, Georgia, serif"
    fontSize: "clamp(1.8rem, 7.6vw, 3.2rem)"
    fontWeight: 500
    lineHeight: 1.3
  subhead:
    fontFamily: "Frank Ruhl Libre, Georgia, serif"
    fontSize: "21px"
    fontWeight: 700
    lineHeight: 1.2
  lede:
    fontFamily: "Heebo, system-ui, sans-serif"
    fontSize: "clamp(1.15rem, 4.8vw, 1.4rem)"
    fontWeight: 300
    lineHeight: 1.55
  body:
    fontFamily: "Heebo, system-ui, sans-serif"
    fontSize: "17px"
    fontWeight: 400
    lineHeight: 1.55
  button:
    fontFamily: "Heebo, system-ui, sans-serif"
    fontSize: "19px"
    fontWeight: 700
    lineHeight: 1.55
  label:
    fontFamily: "Heebo, system-ui, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: 1.55
rounded:
  none: "0px"
  hotspot: "14px"
  mini-screen: "28px"
  mini-device: "40px"
  pill: "999px"
spacing:
  gutter-mobile: "22px"
  gutter-wide: "44px"
  plate-padding: "28px"
  gap-sm: "14px"
  gap-md: "22px"
  gap-lg: "56px"
  section-mobile: "92px"
  section-wide: "132px"
components:
  button-primary:
    backgroundColor: "{colors.signal-red}"
    textColor: "{colors.on-red}"
    typography: "{typography.button}"
    rounded: "{rounded.pill}"
    padding: "0 36px"
    height: "62px"
  button-primary-hover:
    backgroundColor: "{colors.signal-red-hover}"
  button-on-red:
    backgroundColor: "{colors.on-red}"
    textColor: "{colors.signal-red}"
    typography: "{typography.button}"
    rounded: "{rounded.pill}"
    padding: "0 36px"
    height: "62px"
  nav-cta:
    textColor: "{colors.ink}"
    rounded: "{rounded.pill}"
    padding: "0 22px"
    height: "44px"
  nav-cta-hover:
    backgroundColor: "{colors.signal-red}"
  plate:
    backgroundColor: "{colors.panel-black}"
    rounded: "{rounded.none}"
    padding: "28px"
  band-red:
    backgroundColor: "{colors.signal-red}"
    textColor: "{colors.on-red}"
  chip-selected:
    backgroundColor: "{colors.signal-red}"
    textColor: "{colors.on-red}"
    rounded: "{rounded.pill}"
  device-row-group:
    backgroundColor: "{colors.device-graphite}"
    textColor: "{colors.ink}"
  faq-item:
    textColor: "{colors.ink}"
    typography: "{typography.subhead}"
    height: "72px"
---

# Design System: FutureOS Landing

## Overview

**Creative North Star: "The Instrument Drawing"**

The page treats the phone as a precision object photographed in a dark studio and then annotated like an engineering drawing. Black ground, near-white ink and a single red carry everything. The real device renders (keyed-out Blender renders of the body, not stock imagery) are the only pictures. Red is the draftsman's pen: dimension brackets, callout leader lines with dot terminals, corner ticks on plates, a hairline red border on the header button. A working launcher screen is composited over the real front render and its keys are live hotspots, so the visitor learns the device by pressing it.

Density is low and the type is loud. Hebrew display is a high-contrast serif (Frank Ruhl Libre, 900 and 700) set tight; reading text is light Heebo in softened ink. Red floods exactly two sections, the "what is absent" statement band and the closing call, and is otherwise a line, a ring, a word or a small label. Hebrew, right-to-left throughout; numerals and model names are isolated as LTR runs.

Imagery is honest about being simulation: captions and footer state that images are computer renders of the body model, not photographs of a finished product.

**Key Characteristics:**
- Ground #050505, panel #0D0D0D, ink #F4F2F0; no gradients, no second hue.
- One red in two roles: the deep fill and large type, and a bright variant for small text and thin lines on black.
- Serif display plus light sans body, both self-hosted (Hebrew and Latin subsets).
- Engineering annotation (brackets, leaders, corner ticks) drawn in red over real renders.
- Device screens are drawn in a 640-unit coordinate system so they scale exactly.

## Colors

A black room with one red pen. Everything neutral is the ink at stepped opacity; everything chromatic is red.

### Primary
- **Signal Red** (`signal-red`): fills and large type only. Primary button, the statement band, the closing section, the second line of the H1, corner ticks on plates, the selected chip, the tint under launcher selection (26% alpha), text selection. Never used for body-size text on black.
- **Bright Signal Red** (`signal-red-bright`): the small-text and thin-line variant for use on black (about 6:1). Focus outline (2px, 3px offset), the "OS" in the wordmark, dimension brackets and labels, callout leaders and dots, selection ring and caret inside the device, FAQ chevron, lit item in the toolbox list, the pulsing hint ring on the OK key.
- **Signal Red Hover** (`signal-red-hover`): primary button hover only.
- **Hairline Red** (`hairline-red`): the 1px border of the header button and the thin vertical rule.

### Neutral
- **Ground Black** (`ground-black`): page, header (at 94% opacity), dimension-label backing.
- **Panel Black** (`panel-black`): plates that hold render images.
- **Screen Black** (`screen-black`) and **Device Graphite** (`device-graphite`): inside device screens only, for app panes and for the widget and list groups.
- **Ink** (`ink`): headings and primary text. On red fills, text is pure white (`on-red`).
- **Ink 74 / Ink 62**: ledes, paragraphs and captions at 74%; notes, spec terms, idle toolbox names and footer at 62%.
- **Hairline** (`hairline`): every divider, header rule, spec table, FAQ borders, story separators.

### Named Rules
**The One Red Rule.** Red is the only chromatic value. Fill and display size use the deep red; anything small or hairline-thin on black uses the bright red. Never swap them.

**The Two Floods Rule.** Exactly two sections are drenched red: the statement band and the closing section. Elsewhere red is a line, a ring, a tick or a word, never a panel.

**The Ink Alpha Rule.** Secondary text is the ink at 74 or 62 percent, not gray hex values.

## Typography

**Display Font:** Frank Ruhl Libre (with Georgia, serif)
**Body Font:** Heebo (with system-ui, sans-serif)
**Label/Mono Font:** none

**Character:** A Hebrew book-face at poster size for authority, against a light, plain sans for reading. The serif speaks; the sans explains.

### Hierarchy
- **Display** (900, clamp(3.7rem, 19vw, 6rem), 0.98): the H1 only; two lines, words rise from a clipped line, the second line in Signal Red.
- **Statement** (900, clamp(2.6rem, 12.6vw, 6rem), 1.04): the three negations in the red band, white on red. The closing H2 uses the same weight at clamp(2.8rem, 12vw, 5.4rem).
- **Headline** (700, clamp(2.2rem, 9vw, 3.8rem), 1.06): section H2s.
- **Title** (500, clamp(1.8rem, 7.6vw, 3.2rem), 1.3): the toolbox name list, in serif.
- **Subhead** (700, 21px, 1.2): figure captions' lead word, FAQ questions; the device caption lead is 26px.
- **Lede** (300, clamp(1.15rem, 4.8vw, 1.4rem) for the hero, 1.1 to 1.3rem elsewhere, 74% ink, 30 to 34ch, `text-wrap: pretty`).
- **Body** (400, 17px, 1.55): default; FAQ answers capped at 54ch.
- **Label** (400 to 500, 13 to 16px): notes 14px, dimension labels 13px 500 in bright red, callouts 14.5px, spec terms 16px, spec values 500 18px.
- **Button** (Heebo 700, 19px).
- **In-device type** (Heebo 500 to 800, sized in device units): list title 34u/500, sub-line 26u, app title 40u/700, grid label 25u/500, widget clock 60u/700, assistant answer 150u/800.

### Named Rules
**The Serif Speaks Rule.** Headings, statements and names are Frank Ruhl Libre; explanation and interface are Heebo. Do not set a heading in Heebo outside a device screen, and do not add a third face.

**The LTR Island Rule.** Numbers, clocks, dimensions and model names are isolated runs (`direction:ltr; unicode-bidi:isolate`) inside RTL text.

## Layout

Single column on mobile; content container max 1180px, gutter 22px (44px from 960px). Sections breathe: 92 to 96px blocks on mobile, 130 to 160px on wide screens (band 150/160, tools 132, stories 116). The hero is a two-column grid from 960px (text, then the angled device render which may overhang to 112% of its column). The try section is a three-part row on wide screens: copy, the interactive unit, and 270px of space for callout labels to the unit's leading side. Stories are two columns (100px gap) with alternate articles flipped. Spec list (720px) and FAQ (760px) are capped and sit at the reading edge. The closing section is the only centered one.

**Device unit.** Inside any device screen, `--px` = 100cqw / 640, so `calc(34*var(--px))` is 34 units of the real 640x960 screen. The launcher screen is absolutely positioned over the front render by percentage (left 5.7%, top 5.3%, width 88.6%, height 51.3%), and key hotspots are placed by percentages computed from coordinates in the 1000px render. New in-device elements are specified in device units, never CSS pixels; new hotspots are specified in render coordinates.

Breakpoint: 960px (layout). No other breakpoints.

## Elevation & Depth

Tonal layering on black plus drawn lines. Panels are one step lighter than ground (#0D0D0D on #050505) and are marked by red corner ticks rather than edges. Depth in imagery comes from a soft, dark drop-shadow under cutout renders (`drop-shadow(0 40px 40px rgba(0,0,0,.8))` on the hero, `0 30px 30px rgba(0,0,0,.85)` on the unit), which reads as the object sitting in a dark studio. The hero render also gets `contrast(1.12) brightness(1.08)`. UI itself casts no shadow.

### Shadow Vocabulary
- **Studio shadow** (`filter: drop-shadow(0 40px 40px rgba(0,0,0,.8))`): cutout device renders only.
- **Button rim** (`box-shadow: 0 0 0 1px rgba(255,255,255,.14) inset`): primary button.
- **Selection ring** (`inset 0 0 0 calc(3*var(--px)) var(--bright-red)` over a 26% deep-red tint): focused launcher cell and active list row.
- **Mini bezel** (`0 0 0 1px hairline, 0 0 0 7px #0a0a0a, 0 0 0 8px hairline`): the framed mini-screens in the stories.
- **Hotspot hover** (`0 0 0 1.5px bright red`): outline on a hovered key hotspot.

### Named Rules
**The Ring Not Shadow Rule.** UI selection and focus are rings in bright red; only photographic cutouts carry a shadow.

## Shapes

Two vocabularies. The page is square and drafted: plates have no radius, defined by 22px red corner ticks (1px lines on all four corners), hairlines and brackets. Pressable things on the page are pills (999px). The device vocabulary is soft: mini-device frame 40px, mini-screen 28px, launcher screen corners 5%/3.4%, hotspots 14px (50% for the OK and D-pad), in-device cells 28u, widget and list groups 44u, rows 34u. Icons are 24-unit viewBox stroked line icons (1.6 to 2.4 stroke, round caps and joins), inline SVG, currentColor.

## Components

### Buttons
- **Shape:** full pill, 62px tall, 36px side padding.
- **Primary:** Signal Red fill, white Heebo 700 19px, trailing arrow icon (22px) pointing in the reading direction, 1px inner white rim.
- **Hover / Active:** lifts 3px and turns Signal Red Hover; the arrow nudges 7px; active scales to .97. Easing `cubic-bezier(.16,1,.3,1)`, .45s.
- **On-red variant:** inside the red close section the button is white with red text and no rim.
- **Header button:** 44px pill, hairline-red 1px border, ink text 500 15px; fills Signal Red on hover.
- **Text link:** 74% ink, 500, 1px red underline at 7px offset, 44px target; white on hover.

### Plate (signature)
- Panel Black block, 28px padding, no radius, red 22px corner ticks drawn with gradients on two pseudo-elements. Holds a render image at full width. Captions sit beneath in a serif lead plus Ink 74 sentence.

### Dimension bracket and callout (signature)
- Dimension: 1px bright-red line with 11px end ticks, offset 30px from the unit edge, label 13px 500 in bright red on a ground-colored backing so the line breaks around it. Callout: 1px bright-red leader from a point on the render to a 7px dot, ink label 14.5px with a 62% sub-line; the bottom one runs vertically. Callouts show from 960px up. They draw in (scale from the origin, 1.1 to 1.4s) when the unit enters view, then labels fade in.

### Working launcher over the real render (signature)
- The front render carries a live screen: status bar, widget with live clock and he-IL date, 4-column grid of 12 apps with stroked icons, and a ring that glides between cells (.32s). Enter slides an app pane in from the leading edge (.5s); rows walk with up and down. Arrow direction is RTL-aware (left advances). Every key on the render has a transparent hotspot; the OK key pulses a bright-red hint ring until first pressed; a press flashes the hotspot. The real keyboard drives the same handler and flashes the matching key.

### List rows, chips, fields (in device)
- Device Graphite group, 44u radius; active row takes the 26% red tint plus the bright-red inset ring and hides adjacent dividers. Chip: pill on 7% white; selected is Signal Red with white 700 text. Text field: 7% white, 30u radius, bright-red blinking caret.

### FAQ
- Native details, hairline top and bottom borders only, no fill. Summary is serif 700 21px, 72px min height, bright-red chevron that rotates -90 when open. Answers are Ink 74.

### Navigation
- Sticky 68px header, ground at 94%, hairline bottom rule. Wordmark is serif 900 26px with "OS" in bright red. One action only.

### Motion
Expressive and legible. H1 words rise from clipped lines (1.1s, 120ms stagger); the hero render lands with a rotate and rise (1.6s); sections reveal by a 54px rise over 1s; statement lines wipe in from the leading edge; dimension and callout lines draw themselves in sequence; toolbox names light up in turn (900ms); story rows walk focus (1.6s). Primary curve `cubic-bezier(.16,1,.3,1)`. Under `prefers-reduced-motion` only the caret blink, the tap loop, the hint pulse and smooth scroll are disabled; entrance motion still runs.

## Do's and Don'ts

### Do:
- **Do** use Signal Red (#FF3434) for fills and display-size type, and Bright Signal Red (#FF3434) for any small red text, hairline, ring or dot on black.
- **Do** mark panels with red corner ticks and hold renders in Panel Black plates.
- **Do** annotate real renders with red brackets and leader lines; keep labels in LTR islands for numerals.
- **Do** set headings in Frank Ruhl Libre at 900 or 700 and reading text in Heebo 300 to 500.
- **Do** size in-device UI in 640-unit multiples via `--px`, and place hotspots from render coordinates.
- **Do** keep pressables pill-shaped on the page, at least 44px in the target dimension.
- **Do** say plainly that images are computer renders of the body model.

### Don't:
- **Don't** add a second hue, a gradient, an emoji or a stock photo.
- **Don't** flood a third section with red.
- **Don't** set body-size text in Signal Red (#FF3434) on black; use the bright variant.
- **Don't** give UI a shadow; selection and focus are rings.
- **Don't** replace the real renders with drawn illustrations of the phone.
- **Don't** use gray hex values for text; use ink at 74 or 62 percent.
