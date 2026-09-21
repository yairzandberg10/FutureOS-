import React from "react";

/* Snackbar: transient feedback for something that already happened, anchored to
   the bottom of the screen — where HeadsUpNotification is an incoming event at the
   top. Card surface, dialog radius, 15sp message, one optional accent action label
   (the action is reached with the left soft key, so it is a label, not a button). */
export function Snackbar({ message, action, onAction, style, ...rest }) {
  return (
    <div
      style={{
        direction: "rtl",
        fontFamily: "var(--fos-font)",
        margin: "0 var(--fos-space-5) var(--fos-space-5)",
        padding: "var(--fos-space-5) var(--fos-space-6)",
        borderRadius: "var(--fos-radius-dialog)",
        background: "var(--fos-surface-card)",
        boxShadow: "var(--fos-shadow-headsup)",
        display: "flex",
        alignItems: "center",
        gap: "var(--fos-space-6)",
        boxSizing: "border-box",
        ...style
      }}
      {...rest}
    >
      <div style={{ flex: 1, minWidth: 0, fontSize: "var(--fos-size-dialog)", color: "var(--fos-text)", textWrap: "pretty" }}>{message}</div>
      {action && (
        <div onClick={onAction} style={{ flex: "0 0 auto", fontSize: "var(--fos-size-dialog)", fontWeight: "var(--fos-weight-bold)", color: "var(--fos-accent)", cursor: "pointer" }}>{action}</div>
      )}
    </div>
  );
}
