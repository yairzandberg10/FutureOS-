import React from "react";

/* GlassPanel: a system-shell panel (Control Center, Notification Center) — the one place in
   FutureOS that blurs. It draws a snapshot of the screen behind it blurred at 40dp (80px),
   under a 20% white wash, and lays its children out in a column with 16dp side / 8dp vertical
   padding and 10dp between elements (the Control Center uses 4dp vertical).
   `wallpaper` stands in for the snapshot (any CSS background); without it the panel blurs
   whatever is live behind it with backdrop-filter. App screens never use this. Ink is always
   white, so the panel reads the same in light and dark. */
export function GlassPanel({ wallpaper, blur = "var(--fos-backdrop-blur)", children, style, ...rest }) {
  return (
    <div
      style={{
        position: "relative",
        isolation: "isolate",
        overflow: "hidden",
        boxSizing: "border-box",
        direction: "rtl",
        fontFamily: "var(--fos-font)",
        color: "var(--fos-shell-ink)",
        ...(wallpaper ? null : { backdropFilter: `blur(${blur})`, WebkitBackdropFilter: `blur(${blur})` }),
        ...style
      }}
      {...rest}
    >
      {wallpaper && (
        <div
          aria-hidden="true"
          style={{ position: "absolute", inset: `calc(-1 * ${blur})`, zIndex: -2, background: wallpaper, backgroundSize: "cover", backgroundPosition: "center", filter: `blur(${blur})` }}
        />
      )}
      <div aria-hidden="true" style={{ position: "absolute", inset: 0, zIndex: -1, background: "var(--fos-backdrop-wash)" }} />
      <div style={{ position: "relative", boxSizing: "border-box", minHeight: "100%", display: "flex", flexDirection: "column", gap: "var(--fos-space-4)", padding: "var(--fos-space-3) var(--fos-space-screen)" }}>
        {children}
      </div>
    </div>
  );
}

/* GlassTile: any element that sits on a GlassPanel — toggle, notification card, media card,
   button. 28dp radius (the shell radius), white at 15% at rest, 24% when focused with a 2dp
   white ring (4px, inside, no scale).
   `on` is the exception: a solid accent fill with on-accent ink, so a toggle reads at any accent.
   Ink is white with a faint legibility shadow; children that draw icons should use currentColor
   so `on` can flip them. */
export function GlassTile({ focused = false, on = false, radius = "var(--fos-radius-headsup)", onClick, children, style, ...rest }) {
  return (
    <div
      onClick={onClick}
      style={{
        boxSizing: "border-box",
        borderRadius: radius,
        background: on ? "var(--fos-accent)" : focused ? "var(--fos-glass-fill-focused)" : "var(--fos-glass-fill)",
        color: on ? "var(--fos-on-accent)" : "var(--fos-shell-ink)",
        textShadow: on ? "none" : "var(--fos-shell-text-shadow)",
        border: `var(--fos-focus-border-control) solid ${focused ? "var(--fos-shell-ink)" : "transparent"}`,
        cursor: onClick ? "pointer" : undefined,
        transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus)",
        ...style
      }}
      {...rest}
    >
      {children}
    </div>
  );
}
