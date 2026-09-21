import React from "react";

/* GalleryTabChip: 14dp radius, 18/8dp padding, 13sp/500.
   selected = solid accent with black text; focused = 18% text; idle = 6% text. */
export function Chip({ children, state = "idle", onClick, style, ...rest }) {
  const selected = state === "selected";
  return (
    <button
      type="button"
      onClick={onClick}
      style={{
        direction: "rtl",
        fontFamily: "var(--fos-font)",
        fontSize: "var(--fos-size-summary)",
        fontWeight: "var(--fos-weight-medium)",
        lineHeight: 1.25,
        color: selected ? "var(--fos-on-accent)" : "var(--fos-text)",
        background: selected ? "var(--fos-accent)" : (state === "focused" ? "var(--fos-focus-bg-chip)" : "var(--fos-idle-bg-chip)"),
        border: "none",
        borderRadius: "var(--fos-radius-chip)",
        padding: "var(--fos-space-3) var(--fos-space-4)",
        paddingInline: 36,
        cursor: "pointer",
        flex: "0 0 auto",
        transition: "background var(--fos-transition-focus), color var(--fos-transition-focus)",
        ...style
      }}
      {...rest}
    >
      {children}
    </button>
  );
}
