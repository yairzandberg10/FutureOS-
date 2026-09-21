import React from "react";
import { FosIcon } from "../core/FosIcon.jsx";

/* MenuRow inside the options overlay: opened by the hardware menu key.
   85% width, 20dp radius, 20/14dp row padding, 20dp icon, 14dp gap, 15sp label.
   Focus is a 12% text background only — no border, no scale. Destructive rows go red. */
export function OptionsMenu({ header, items = [], focusedIndex = -1, onSelect, style, ...rest }) {
  return (
    <div
      style={{
        position: "absolute",
        inset: 0,
        background: "var(--fos-scrim)",
        display: "flex",
        justifyContent: "center",
        alignItems: "flex-start",
        paddingTop: 80,
        ...style
      }}
      {...rest}
    >
      <div
        style={{
          direction: "rtl",
          fontFamily: "var(--fos-font)",
          width: "85%",
          background: "var(--fos-surface-card)",
          borderRadius: "var(--fos-radius-dialog)",
          padding: "var(--fos-space-3) 0",
          overflow: "hidden"
        }}
      >
        {header && (
          <div style={{ fontSize: "var(--fos-size-label)", color: "var(--fos-text-50)", padding: "var(--fos-space-2) var(--fos-space-8) var(--fos-space-3)" }}>{header}</div>
        )}
        {items.map((it, i) => (
          <div
            key={it.label}
            onClick={() => onSelect && onSelect(i)}
            style={{
              display: "flex",
              alignItems: "center",
              gap: "var(--fos-space-6)",
              height: "var(--fos-row-menu)",
              padding: "0 var(--fos-space-8)",
              background: i === focusedIndex ? "var(--fos-focus-bg-menu)" : "transparent",
              color: it.destructive ? "var(--fos-danger)" : "var(--fos-text)",
              cursor: "pointer",
              transition: "background var(--fos-transition-focus)"
            }}
          >
            <FosIcon name={it.icon} size={40} color="currentColor" />
            <div style={{ fontSize: "var(--fos-size-dialog)" }}>{it.label}</div>
          </div>
        ))}
      </div>
    </div>
  );
}
