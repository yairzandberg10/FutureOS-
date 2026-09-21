import React from "react";

/* DayToggleChip: 36dp circle, 14sp/700. selected = accent + black;
   focused = 20% text with a 2dp accent ring; idle = 8% text. */
export function DayChip({ children, selected = false, focused = false, onClick, style, ...rest }) {
  return (
    <button
      type="button"
      onClick={onClick}
      style={{
        width: 72,
        height: 72,
        flex: "0 0 auto",
        borderRadius: "var(--fos-radius-full)",
        fontFamily: "var(--fos-font)",
        fontSize: "var(--fos-size-body)",
        fontWeight: "var(--fos-weight-bold)",
        color: selected ? "var(--fos-on-accent)" : "var(--fos-text)",
        background: selected ? "var(--fos-accent)" : (focused ? "var(--fos-accent-20)" : "var(--fos-text-08)"),
        border: `var(--fos-focus-border-control) solid ${focused && !selected ? "var(--fos-accent)" : "transparent"}`,
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        cursor: "pointer",
        boxSizing: "border-box",
        transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus)",
        ...style
      }}
      {...rest}
    >
      {children}
    </button>
  );
}
