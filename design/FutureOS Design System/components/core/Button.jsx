import React from "react";

/* Pill, 88px tall, 16sp/700 (DialogButton geometry).
   Fills are always solid: no element-level opacity, so labels stay at full contrast.
   Idle and focused share the same fill. Focus = a ring in the button's own color, drawn outside the pill after a small gap (outline + offset, never changes layout) and a 1.02 lift. */
const FILL = {
  primary: { bg: "var(--fos-accent)", fg: "var(--fos-on-accent)", fbg: "var(--fos-accent)", ffg: "var(--fos-on-accent)", ring: "var(--fos-accent)" },
  destructive: { bg: "var(--fos-danger)", fg: "var(--fos-on-accent)", fbg: "var(--fos-danger)", ffg: "var(--fos-on-accent)", ring: "var(--fos-danger)" },
  secondary: { bg: "var(--fos-text-20)", fg: "var(--fos-text)", fbg: "var(--fos-text-20)", ffg: "var(--fos-text)", ring: "var(--fos-text-20)" },
  quiet: { bg: "var(--fos-text-10)", fg: "var(--fos-text)", fbg: "var(--fos-text-10)", ffg: "var(--fos-text)", ring: "var(--fos-text-10)" }
};

export function Button({ children, variant = "primary", focused = false, fullWidth = false, onClick, style, ...rest }) {
  const v = FILL[variant] || FILL.primary;
  const quiet = variant === "quiet";
  return (
    <button
      type="button"
      onClick={onClick}
      style={{
        direction: "rtl",
        fontFamily: "var(--fos-font)",
        fontSize: "var(--fos-size-base)",
        fontWeight: quiet ? "var(--fos-weight-medium)" : "var(--fos-weight-bold)",
        lineHeight: 1.25,
        color: focused ? v.ffg : v.fg,
        background: focused ? v.fbg : v.bg,
        border: "none",
        outline: focused ? "var(--fos-focus-border-control) solid " + v.ring : "var(--fos-focus-border-control) solid transparent",
        outlineOffset: 4,
        transform: focused ? "scale(var(--fos-focus-scale))" : "none",
        borderRadius: "var(--fos-radius-full)",
        padding: "0 var(--fos-space-9)",
        height: 88,
        minWidth: 176,
        boxSizing: "border-box",
        width: fullWidth ? "100%" : "auto",
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        cursor: "pointer",
        transition: "background var(--fos-transition-focus), color var(--fos-transition-focus), outline-color var(--fos-transition-focus), transform var(--fos-transition-focus)",
        ...style
      }}
      {...rest}
    >
      {children}
    </button>
  );
}
