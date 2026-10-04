import React from "react";
import { FosIcon } from "./FosIcon.jsx";

/* ActionGrid (FutureActionCell): the focusable icon+label cells that carry a screen's
   actions — four in a row under a translation, 2x2 as call controls, a column of contact
   actions. Cell = 20% accent fill when active, 4px accent border when focused, a 44%-of-height
   glyph (20-40dp) over an optional 13sp label. `columns` sets the shape; `height` the cell.
   Two variants of the glyph: a FosIcon (`icon`), or a text symbol (`glyph`: "+", "sin", "π")
   drawn at 90% of the icon size in the same 70% alpha, accent when active. A `legend` cell is
   the second variant made non-focusable: a reminder of what a physical key does (glyph = the
   operation, label = the key name), so it never takes the focus border and never activates. */
export function ActionGrid({ items = [], focusedIndex = -1, columns = 2, height = 132, legend = false, onSelect, style, ...rest }) {
  const iconPx = Math.max(40, Math.min(80, Math.round(height * 0.44)));
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
        const isLegend = it.legend ?? legend;
        const focused = focusedIndex === i && !isLegend;
        const ink = it.color || (it.active ? "var(--fos-accent)" : "var(--fos-text-70)");
        return (
          <div
            key={(it.label || it.glyph || "") + i}
            onClick={onSelect && !isLegend ? () => onSelect(i) : undefined}
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
              cursor: isLegend ? "default" : "pointer",
              transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus)"
            }}
          >
            {it.glyph != null ? (
              <div style={{ direction: "ltr", fontSize: Math.round(iconPx * 0.9), lineHeight: 1, fontWeight: "var(--fos-weight-regular)", color: ink, textAlign: "center", whiteSpace: "nowrap" }}>{it.glyph}</div>
            ) : (
              <FosIcon name={it.icon} size={iconPx} strokeWidth={1.3} color={ink} />
            )}
            {it.label && <div style={{ maxWidth: "100%", fontSize: "var(--fos-size-summary)", color: "var(--fos-text-60)", textAlign: "center", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{it.label}</div>}
          </div>
        );
      })}
    </div>
  );
}
