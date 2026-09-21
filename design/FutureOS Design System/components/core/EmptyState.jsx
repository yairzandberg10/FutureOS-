import React from "react";
import { FosIcon } from "./FosIcon.jsx";

/* Two lines and one 56dp outlined glyph, all centred, all in the text color at
   fixed alphas: glyph 40%, title 70% at 17sp/500, subtitle 40% at 14sp/400. */
export function EmptyState({ icon = "inbox", title, subtitle, style, ...rest }) {
  return (
    <div
      style={{
        direction: "rtl",
        fontFamily: "var(--fos-font)",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        gap: "var(--fos-space-3)",
        padding: "var(--fos-space-10)",
        textAlign: "center",
        ...style
      }}
      {...rest}
    >
      <FosIcon name={icon} size={112} color="var(--fos-text)" opacity={0.4} strokeWidth={1.1} />
      <div style={{ fontSize: "var(--fos-size-title)", fontWeight: "var(--fos-weight-medium)", color: "var(--fos-text-70)", marginTop: "var(--fos-space-1)" }}>{title}</div>
      {subtitle && <div style={{ fontSize: "var(--fos-size-body)", color: "var(--fos-text-40)" }}>{subtitle}</div>}
    </div>
  );
}
