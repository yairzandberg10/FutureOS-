import React from "react";
import { FosIcon } from "./FosIcon.jsx";

/* ActionGrid: the focusable icon+label cells that carry a screen's actions —
   four in a row under a translation, 2x2 as call controls, a column of contact
   actions. Cell = 20% accent fill when active, 4px accent border when focused,
   40dp glyph over a 13sp label. `columns` sets the shape; `height` the cell. */
export function ActionGrid({ items = [], focusedIndex = -1, columns = 2, height = 132, onSelect, style, ...rest }) {
  return (
    <div
      style={{
        direction: "rtl",
        fontFamily: "var(--fos-font)",
        display: "grid",
        gridTemplateColumns: `repeat(${columns}, minmax(0, 1fr))`,
        gap: "var(--fos-space-3)",
        ...style
      }}
      {...rest}
    >
      {items.map((it, i) => {
        const focused = focusedIndex === i;
        const glyph = Math.max(40, Math.min(80, Math.round(height * 0.44)));
        return (
          <div
            key={(it.label || "") + i}
            onClick={onSelect ? () => onSelect(i) : undefined}
            style={{
              height,
              borderRadius: "var(--fos-radius-card)",
              background: it.active ? "var(--fos-accent-20)" : "var(--fos-glass)",
              border: `4px solid ${focused ? "var(--fos-accent)" : "transparent"}`,
              boxSizing: "border-box",
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              justifyContent: "center",
              gap: "var(--fos-space-1)",
              cursor: "pointer",
              transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus)"
            }}
          >
            <FosIcon name={it.icon} size={glyph} strokeWidth={1.3} color={it.color || (it.active ? "var(--fos-accent)" : "var(--fos-text-70)")} />
            {it.label && <div style={{ fontSize: "var(--fos-size-summary)", color: "var(--fos-text-60)", textAlign: "center" }}>{it.label}</div>}
          </div>
        );
      })}
    </div>
  );
}
