import React from "react";
import { Button } from "../core/Button.jsx";
import { TextField } from "../forms/TextField.jsx";

/* Same shell as ConfirmDialog with a field in the middle. The source's files-app
   version omits the focus border on its buttons; this follows ConfirmDialog, which
   is the shared and correct one. */
export function InputDialog({ title, value, placeholder, confirmLabel = "שמור", cancelLabel = "ביטול", onConfirm, onCancel, style, ...rest }) {
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
          background: "var(--fos-surface-card)",
          borderRadius: "var(--fos-radius-dialog)",
          padding: "var(--fos-space-8)",
          boxSizing: "border-box",
          transform: "scale(var(--fos-focus-scale))"
        }}
      >
        <div style={{ fontSize: "var(--fos-size-dialog)", fontWeight: "var(--fos-weight-bold)", color: "var(--fos-text)", textAlign: "center", marginBottom: "var(--fos-space-7)" }}>{title}</div>
        <TextField value={value} placeholder={placeholder} focused />
        <div style={{ display: "flex", gap: "var(--fos-space-5)", justifyContent: "center", marginTop: "var(--fos-space-7)" }}>
          <Button variant="secondary" onClick={onCancel}>{cancelLabel}</Button>
          <Button variant="primary" onClick={onConfirm}>{confirmLabel}</Button>
        </div>
      </div>
    </div>
  );
}
