import React from "react";

/* ViewModeTabRow: 16/6dp outer padding, 8dp gap, 12dp radius, 8dp item padding,
   13sp label. focused = 18% text, idle = 8% text, selected = solid accent. */
export function TabRow({ items = [], selected = 0, focusedIndex = -1, onSelect, style, ...rest }) {
  return (
    <div
      style={{
        direction: "rtl",
        fontFamily: "var(--fos-font)",
        display: "flex",
        gap: "var(--fos-space-3)",
        padding: "var(--fos-space-2) var(--fos-space-7)",
        ...style
      }}
      {...rest}
    >
      {items.map((label, i) => {
        const sel = i === selected;
        const foc = i === focusedIndex;
        return (
          <div
            key={label}
            onClick={() => onSelect && onSelect(i)}
            style={{
              flex: 1,
              textAlign: "center",
              fontSize: "var(--fos-size-summary)",
              fontWeight: "var(--fos-weight-medium)",
              padding: "var(--fos-space-3) 0",
              borderRadius: "var(--fos-radius-tab)",
              color: sel ? "var(--fos-on-accent)" : "var(--fos-text)",
              background: sel ? "var(--fos-accent)" : (foc ? "var(--fos-focus-bg-chip)" : "var(--fos-text-08)"),
              cursor: "pointer",
              transition: "background var(--fos-transition-focus), color var(--fos-transition-focus)"
            }}
          >
            {label}
          </div>
        );
      })}
    </div>
  );
}
