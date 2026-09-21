import React from "react";
import { FosIcon } from "./../core/FosIcon.jsx";

/* ToggleButton: a pressable that shows its own state — the third switch pattern,
   for when a Switch is too small and a Chip carries no state. Off is the glass
   surface at 70% text; on is the 20% accent fill with an accent glyph and label. */
export function ToggleButton({ children, icon, on = false, focused = false, fullWidth = false, onChange, style, ...rest }) {
  return (
    <button
      type="button"
      onClick={onChange}
      role="switch"
      aria-checked={on}
      style={{
        direction: "rtl",
        fontFamily: "var(--fos-font)",
        fontSize: "var(--fos-size-base)",
        fontWeight: "var(--fos-weight-medium)",
        color: on ? "var(--fos-accent)" : "var(--fos-text-70)",
        background: on ? "var(--fos-accent-20)" : "var(--fos-glass)",
        border: `4px solid ${focused ? "var(--fos-accent)" : "transparent"}`,
        borderRadius: "var(--fos-radius-full)",
        padding: "0 var(--fos-space-7)",
        minHeight: 88,
        width: fullWidth ? "100%" : "auto",
        boxSizing: "border-box",
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        gap: "var(--fos-space-3)",
        cursor: "pointer",
        transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus), color var(--fos-transition-focus)",
        ...style
      }}
      {...rest}
    >
      {icon && <FosIcon name={icon} size={40} color={on ? "var(--fos-accent)" : "var(--fos-text-60)"} />}
      {children}
    </button>
  );
}
