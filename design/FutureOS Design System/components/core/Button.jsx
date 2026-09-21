import React from "react";

/* No shared button exists in the Kotlin source. This consolidates the four patterns
   that recur, on DialogButton's geometry: 20dp radius, 24/12dp padding, 16sp/700,
   2dp white focus border, and 70% fill opacity when not focused. */
const FILL = {
  primary: { bg: "var(--fos-accent)", fg: "var(--fos-on-accent)", base: 1 },
  destructive: { bg: "var(--fos-danger)", fg: "var(--fos-on-accent)", base: 1 },
  secondary: { bg: "var(--fos-text)", fg: "var(--fos-on-accent)", base: 0.7 },
  quiet: { bg: "var(--fos-text-10)", fg: "var(--fos-text)", base: 1 }
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
        fontSize: quiet ? "var(--fos-size-body)" : "var(--fos-size-base)",
        fontWeight: quiet ? "var(--fos-weight-medium)" : "var(--fos-weight-bold)",
        lineHeight: 1.25,
        color: v.fg,
        background: v.bg,
        opacity: quiet ? 1 : (focused ? v.base : v.base * 0.7),
        border: focused && !quiet ? "var(--fos-focus-border-control) solid var(--fos-text)" : "var(--fos-focus-border-control) solid transparent",
        borderRadius: quiet ? "var(--fos-radius-full)" : "var(--fos-radius-dialog)",
        padding: quiet ? "0 var(--fos-space-9)" : "var(--fos-space-5) var(--fos-space-9)",
        height: quiet ? 80 : "auto",
        minHeight: quiet ? 80 : 88,
        width: fullWidth ? "100%" : "auto",
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        cursor: "pointer",
        transition: "opacity var(--fos-transition-focus), border-color var(--fos-transition-focus)",
        ...style
      }}
      {...rest}
    >
      {children}
    </button>
  );
}
