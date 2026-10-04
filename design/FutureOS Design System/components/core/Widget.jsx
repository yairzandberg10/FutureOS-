import React from "react";
import { FosIcon } from "./FosIcon.jsx";

/* Home-screen widget template (FutureContentWidget, 2x1 cells): #1C1C1E at 90% with a 0.5dp 15%
   white hairline, 22dp radius, 16dp / 10dp padding. Row: 18dp app icon, 6dp, 12sp title at 60%
   white. Value 20sp bold white (28sp in the clock variant, a live clock; 2dp tighter above it).
   Subtitle 13sp at 60%, two lines max. The icon is the app's own launcher icon, drawn as is and
   not tinted; `icon` here is a FosIcon stand-in for it. Always dark, even in light mode — it
   sits on the user's wallpaper — hence the shell tokens. */
export function Widget({ icon = "widgets", title, value, subtitle, variant = "default", style, ...rest }) {
  const clock = variant === "clock";
  return (
    <div
      style={{
        direction: "rtl",
        fontFamily: "var(--fos-font)",
        boxSizing: "border-box",
        padding: "var(--fos-space-4) var(--fos-space-7)",
        borderRadius: "var(--fos-radius-main)",
        background: "var(--fos-widget-bg)",
        border: "var(--fos-border-headsup) solid var(--fos-hairline-dark)",
        color: "var(--fos-shell-ink)",
        ...style
      }}
      {...rest}
    >
      <div style={{ display: "flex", alignItems: "center", gap: "var(--fos-space-2)" }}>
        <FosIcon name={icon} size={36} color="var(--fos-shell-ink)" />
        <div style={{ minWidth: 0, fontSize: "var(--fos-size-label)", color: "var(--fos-shell-ink-60)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{title}</div>
      </div>
      <div
        style={{
          marginTop: clock ? 4 : "var(--fos-space-1)",
          fontSize: clock ? "var(--fos-size-widget-clock)" : "var(--fos-size-screen-title)",
          fontWeight: "var(--fos-weight-bold)",
          lineHeight: "var(--fos-line-height)",
          fontVariantNumeric: "tabular-nums"
        }}
      >
        {value}
      </div>
      {subtitle && (
        <div
          style={{
            fontSize: "var(--fos-size-summary)",
            lineHeight: "var(--fos-line-height)",
            color: "var(--fos-shell-ink-60)",
            display: "-webkit-box",
            WebkitBoxOrient: "vertical",
            WebkitLineClamp: 2,
            overflow: "hidden"
          }}
        >
          {subtitle}
        </div>
      )}
    </div>
  );
}
