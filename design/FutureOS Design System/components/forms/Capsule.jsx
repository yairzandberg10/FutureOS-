import React from "react";
import { FosIcon } from "./../core/FosIcon.jsx";

/* Capsule: a pill-shaped focusable control that carries a small label above its
   value — the language pickers in תרגום, and, at `round`, the big mic button.
   Focus is the 4px accent border the app surfaces use, not Button's white one. */
export function Capsule({ label, children, icon, focused = false, active = false, round = false, size = 168, onClick, style, ...rest }) {
  const fill = active ? "var(--fos-accent-20)" : "var(--fos-text-08)";
  const glyph = active ? "var(--fos-accent)" : "var(--fos-text-60)";
  if (round) {
    return (
      <div
        onClick={onClick}
        style={{
          width: size, height: size, flex: "0 0 auto",
          borderRadius: "var(--fos-radius-full)",
          background: fill,
          border: `4px solid ${focused ? "var(--fos-accent)" : "transparent"}`,
          boxSizing: "border-box",
          display: "flex", alignItems: "center", justifyContent: "center",
          cursor: "pointer",
          transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus)",
          ...style
        }}
        {...rest}
      >
        <FosIcon name={icon} size={Math.round(size * 0.48)} color={glyph} />
      </div>
    );
  }
  return (
    <div
      onClick={onClick}
      style={{
        direction: "rtl",
        fontFamily: "var(--fos-font)",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        gap: 2,
        padding: "var(--fos-space-3) var(--fos-space-6)",
        borderRadius: "var(--fos-radius-full)",
        background: fill,
        border: `4px solid ${focused ? "var(--fos-accent)" : "transparent"}`,
        boxSizing: "border-box",
        cursor: "pointer",
        transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus)",
        ...style
      }}
      {...rest}
    >
      {label && <div style={{ fontSize: "var(--fos-size-label)", color: "var(--fos-text-55)", letterSpacing: "var(--fos-tracking-section)" }}>{label}</div>}
      <div style={{ display: "flex", alignItems: "center", gap: "var(--fos-space-2)", fontSize: "var(--fos-size-title)", fontWeight: "var(--fos-weight-medium)", color: active ? "var(--fos-accent)" : "var(--fos-text)" }}>
        {icon && <FosIcon name={icon} size={36} color={active ? "var(--fos-accent)" : "var(--fos-text-60)"} />}
        {children}
      </div>
    </div>
  );
}
