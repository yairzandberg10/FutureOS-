import React from "react";

/* SoftKeyBar: the three labels along the bottom edge that name what the phone's
   two soft keys and the OK key do on this screen — the keypad equivalent of a
   bottom app bar, and the only always-visible affordance legend. Left and right
   sit against the edges, the centre label is the OK action in the accent color.
   Labels are single words; an unused key is left empty, never labelled "—". */
export function SoftKeyBar({ left, center, right, style, ...rest }) {
  const side = { flex: 1, fontSize: "var(--fos-size-summary)", color: "var(--fos-text-60)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" };
  return (
    <div
      style={{
        direction: "rtl",
        fontFamily: "var(--fos-font)",
        display: "flex",
        alignItems: "center",
        gap: "var(--fos-space-5)",
        height: 72,
        padding: "0 var(--fos-space-7)",
        borderTop: "var(--fos-border-hairline) solid var(--fos-text-10)",
        background: "var(--fos-bg)",
        boxSizing: "border-box",
        ...style
      }}
      {...rest}
    >
      <div style={{ ...side, textAlign: "right" }}>{left}</div>
      <div style={{ flex: "0 0 auto", fontSize: "var(--fos-size-summary)", fontWeight: "var(--fos-weight-bold)", color: "var(--fos-accent)" }}>{center}</div>
      <div style={{ ...side, textAlign: "left" }}>{right}</div>
    </div>
  );
}
