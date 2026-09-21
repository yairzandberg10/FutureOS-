# assets

**Empty of brand marks, on purpose.**

The attached source (`design-system/`, the FutureOS Figma-export pipeline) contains no logo, no
wordmark, no app icon artwork and no illustration. Its own SVG spec sheets draw deliberate
placeholder shapes and say so in their notes. Nothing was drawn, reconstructed or approximated
here to fill that gap.

Where a mark would go, the name **FutureOS** is set in plain type (Heebo 800) — see
`guidelines/wordmark.html` and `thumbnail.html`.

Icons are not files either: the system uses **Material Symbols Rounded** (`Icons.Rounded.*` in the
Kotlin source), loaded as a variable icon font in `tokens/fonts.css` and wrapped by the `Icon`
component.

## If you have the real assets

Drop them in here and they will be picked up:

| file | used by |
|---|---|
| `logo.svg` | `thumbnail.html`, `guidelines/wordmark.html` |
| `app-icons/*.svg` | `ui_kits/launcher` tiles, which currently use glyphs |
| fonts (`.ttf`/`.woff2`) | `tokens/fonts.css`, replacing the Google Fonts import |
