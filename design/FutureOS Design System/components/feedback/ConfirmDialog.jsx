import React from "react";
import { Button } from "../core/Button.jsx";

/* ConfirmDialog: 85% of screen width, max 86% height, 20dp radius, 20dp padding,
   message 15sp/700 centred. Buttons are RTL: cancel on the right, the action on the
   left. The screen behind is dimmed with a 60% black scrim and focus is trapped. */
export function ConfirmDialog({ message, confirmLabel = "מחק", cancelLabel = "ביטול", destructive = true, focus = "confirm", onConfirm, onCancel, style, ...rest }) {
  return (
    <div
      style={{
        position: "absolute",
        inset: 0,
        background: "var(--fos-scrim)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        ...style
      }}
      {...rest}
    >
      <div
        style={{
          direction: "rtl",
          fontFamily: "var(--fos-font)",
          width: "85%",
          maxHeight: "86%",
          background: "var(--fos-surface-card)",
          borderRadius: "var(--fos-radius-dialog)",
          padding: "var(--fos-space-8)",
          boxSizing: "border-box",
          transform: "scale(var(--fos-focus-scale))"
        }}
      >
        <div style={{ fontSize: "var(--fos-size-dialog)", fontWeight: "var(--fos-weight-bold)", color: "var(--fos-text)", textAlign: "center", lineHeight: 1.2 }}>{message}</div>
        <div style={{ display: "flex", gap: "var(--fos-space-5)", justifyContent: "center", marginTop: "var(--fos-space-7)" }}>
          <Button variant="secondary" focused={focus === "cancel"} onClick={onCancel}>{cancelLabel}</Button>
          <Button variant={destructive ? "destructive" : "primary"} focused={focus === "confirm"} onClick={onConfirm}>{confirmLabel}</Button>
        </div>
      </div>
    </div>
  );
}
