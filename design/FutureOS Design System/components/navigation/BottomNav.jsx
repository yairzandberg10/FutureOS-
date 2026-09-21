import React from "react";
import { FosIcon } from "../core/FosIcon.jsx";

/* FitnessBottomNav: a floating rounded bar inset from the screen edges. The selected
   item carries an accent pill behind a filled icon and is the only item that shows a
   label; unselected items are outline icons at 50% alpha.
   Deliberately NOT focusable — the screen-level arrow keys switch tabs. */
export function BottomNav({ items = [], selected = 0, onSelect, style, ...rest }) {
  return (
    <div
      style={{
        direction: "rtl",
        fontFamily: "var(--fos-font)",
        background: "var(--fos-surface-card)",
        borderRadius: "var(--fos-radius-full)",
        margin: "0 var(--fos-space-7) var(--fos-space-7)",
        height: 132,
        display: "flex",
        alignItems: "center",
        padding: "0 var(--fos-space-3)",
        boxSizing: "border-box",
        ...style
      }}
      {...rest}
    >
      {items.map((it, i) => {
        const sel = i === selected;
        return (
          <div
            key={it.label}
            onClick={() => onSelect && onSelect(i)}
            style={{ flex: sel ? "1.4" : "1", display: "flex", alignItems: "center", justifyContent: "center", cursor: "pointer", minWidth: 0 }}
          >
            <div
              style={{
                height: 92,
                borderRadius: "var(--fos-radius-full)",
                background: sel ? "var(--fos-accent)" : "transparent",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                gap: "var(--fos-space-3)",
                padding: sel ? "0 var(--fos-space-6)" : "0",
                minWidth: 0,
                transition: "background var(--fos-transition-focus)"
              }}
            >
              <FosIcon name={it.icon} size={40} color={sel ? "var(--fos-on-accent)" : "var(--fos-text)"} opacity={sel ? 1 : 0.5} fill={sel ? 1 : 0} />
              {sel && (
                <span style={{ fontSize: "var(--fos-size-body)", fontWeight: "var(--fos-weight-medium)", color: "var(--fos-on-accent)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{it.label}</span>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}
