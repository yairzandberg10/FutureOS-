The system-shell panel: Control Center and Notification Center. It is the **one place in FutureOS that blurs** — an app screen never does.

```jsx
<GlassPanel wallpaper="url(wallpaper.jpg)">
  <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", justifyItems: "center" }}>
    <GlassTile on radius="var(--fos-radius-full)" style={{ width: 76, height: 76, display: "grid", placeItems: "center" }}><FosIcon name="wifi" size={40} color="currentColor" /></GlassTile>
    <GlassTile focused radius="var(--fos-radius-full)" style={{ width: 76, height: 76, display: "grid", placeItems: "center" }}><FosIcon name="bluetooth" size={40} color="currentColor" /></GlassTile>
  </div>
  <Slider icon="brightness_6" label="בהירות מסך" value={0.6} focused style={{ margin: 0 }} />
  <NotificationCard appName="הודעות" title="מיכל לוי" body="נתראה בערב" focused />
</GlassPanel>
```

**The panel.** When it opens it takes a screenshot of whatever is on screen, draws it blurred at a **40dp radius** (`--fos-backdrop-blur`, 80px) and lays a **20% white wash** over it (`--fos-backdrop-wash`). If the screenshot cannot be taken in time (350ms) the panel falls back to the wallpaper; with neither, it is a flat 80% white veil with dark ink — a degraded state, not a theme. Content sits on top in a column with 16dp side padding, 8dp vertical padding (the Control Center uses 4dp) and 10dp between elements. `wallpaper` is the stand-in for the screenshot — any CSS background; leave it out and the panel blurs whatever is live behind it with `backdrop-filter`. It is a full-screen layer, so it has no corners of its own; clip it with the screen frame. Ink on it is always white, in light mode too, because what is under it is the user's picture, not a theme surface, and text carries a faint legibility shadow (`--fos-shell-text-shadow`).

**The elements.** Everything on the panel is a `GlassTile`: **white at 15%** at rest, **white at 24%** when focused, plus a **2dp white focus ring** (no scale). The ring is white, not accent: accent is the one user-chosen color and glass stays neutral. The shapes in use:

| element | shape |
|---|---|
| toggle pill (`TogglePill`) | 55dp tall, 28dp radius, 6dp padding; inside, a 40dp disc with an 18dp icon, 10dp gap, then a 12sp / 500 label |
| icon toggle (`FocusableIcon`) | a 38dp circle with a 20dp icon, an optional 11sp label beneath |
| button (`נקה הכל`, `השתק`) | a 38dp pill with an 18dp icon and a 12sp / 600 label |
| notification card | `NotificationCard`, 28dp radius |
| the section that holds a grid of toggles | 28dp radius, 12dp padding, a 0.5dp 15% white hairline (2dp full white while being edited) |

An **on** toggle is the exception to the glass fill: a **solid accent fill with `--fos-on-accent` ink**, the same selected-beats-focused rule the rest of the system uses — on the pill, only the 40dp disc goes accent. Draw its icon in `currentColor` so the ink flips with it. Known gap: an on tile that is also focused is accent fill plus a white ring, and with the default white accent those are the same white; do not let focus on an on tile be the only thing a screen relies on.

**Sliders on the panel** are the ordinary `Slider` (brightness, volume), not a tile: its row is the focus target, its ring stays the user's accent, and its alphas are the dark-theme white ones.

**Danger on glass** is `--fos-danger-on-glass` (`#FF6B6B`), always, because the panel is over a dark blurred picture; the themed `--fos-danger` flips to a dark red in light mode and would sink into it.

**Two glasses, one word.** `--fos-glass` (`#2C2C2E`) is the opaque raised tone apps use for avatars, calc keys and action cells. `--fos-glass-fill` (white 15%) is translucent and exists only on a shell panel. Never use the translucent one inside an app, and never put an opaque `--fos-glass` tile on a panel: on the panel it would read as a flat patch in the blur.

Related: `NotificationCard` (the notification-center element), `Slider` (the Control Center brightness and volume rows), `StatusCapsule` and `Widget` (the other shell surfaces; translucent but not blurred).
