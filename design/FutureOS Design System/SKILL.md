---
name: futureos-design
description: Use this skill to generate well-branded interfaces and assets for FutureOS, either for production or throwaway prototypes/mocks/etc. Contains essential design guidelines, colors, type, fonts, assets, and UI kit components for protoyping.
user-invocable: true
---

Read the README.md file within this skill, and explore the other available files.
If creating visual artifacts (slides, mocks, throwaway prototypes, etc), copy assets out and create static HTML files for the user to view. If working on production code, you can copy assets and read the rules here to become an expert in designing with this brand.
If the user invokes this skill without any other guidance, ask them what they want to build or design, ask some questions, and act as an expert designer who outputs HTML artifacts _or_ production code, depending on the need.

## Five things to get right before anything else

1. **640 × 960, density 2.0.** One screen size, no breakpoints. Every value in `tokens/` is in
   device pixels: `px = dp × 2`.
2. **RTL, Hebrew, forced at the screen level.** Left is forward; the entry chevron points left and
   does not mirror; numerals stay LTR.
3. **Keypad only.** No touch, no pointer. There is no hover state and no press state — build
   `focused` and `selected`, and nothing else.
4. **Accent is the only chromatic decision, and it defaults to white.** Never rely on the accent
   being colourful.
5. **Depth is tone, not shadow.** Two real shadows exist in the whole system, both on the settings
   card. No gradients, no blur, no imagery, no emoji.
