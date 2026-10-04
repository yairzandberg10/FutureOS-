The grid of action cells that carries a screen's actions: four across under a translation, 2x2 as call controls, a column of contact actions, and — with text glyphs — the keys of the calculator.

```jsx
<ActionGrid columns={2} focusedIndex={1} items={[
  { icon: "mic_off", label: "בטל השתקה", active: true },
  { icon: "volume_up", label: "רמקול" }
]} />

{/* text glyphs: the scientific calculator's keys (44dp cells, no label) */}
<ActionGrid columns={4} height={88} focusedIndex={0} items={[
  { glyph: "sin" }, { glyph: "cos" }, { glyph: "tan" }, { glyph: "xʸ" }
]} />

{/* key legend: non-focusable, glyph = the operation, label = the physical key */}
<ActionGrid columns={4} height={112} legend items={[
  { glyph: "C", label: "*" }, { glyph: ".", label: "* ארוך" },
  { glyph: "%", label: "#" }, { glyph: "⌫", label: "חזור" }
]} />
```

A cell is a `--fos-glass` rectangle at card radius; **active** swaps the fill for 20% accent and the glyph for the accent color; **focused** adds the 4px accent border (no scale). The glyph is 44% of the cell height, between 20dp and 40dp, over an optional 13sp / 60% label.

**Icon or glyph.** `icon` draws a `FosIcon`. `glyph` draws a string instead — an operation symbol such as `+`, `sin`, `π` or `C` — at 90% of the icon size (a letter's box is larger than an icon's ink), in the same 70% text alpha, accent when active. The cell, ring and label are identical, so the two mix freely in one grid. A glyph is text, so it stays LTR inside the RTL cell; it is sized like an icon, so it does not follow the text-size multiplier (the 13sp label does).

**Legend.** A `legend` cell is a glyph cell that cannot take focus (`focusable = false` in the app). The calculator uses it for the four physical keys that have no place on its D-pad: the glyph is what the key does, the label is the key (`C` on `*`, `.` on long `*`, `%` on `#`, `⌫` on `חזור`), in 56dp cells. It keeps the cell look so the legend reads as part of the keypad, but it never shows the focus border, never reacts to `onSelect`, and the focus order skips it — a design rule, not a styling choice: whatever cannot take focus cannot be activated. The D-pad pill below it is a map in the same spirit: it takes no focus either. Set `legend` on the grid for an all-legend block, or on single cells in a mixed one.

Never use a legend cell as a button. If the user must be able to press it, it is an ordinary cell.
