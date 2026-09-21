import React from "react";

/* Spinner: the circular counterpart to ProgressBar, for waits with no known
   length (scanning, connecting, sending). 10% text track, accent arc, one
   rotation per 900ms — the only rotating element in the system. */
const KEYFRAMES = "@keyframes fos-spin{to{transform:rotate(360deg)}}";

export function Spinner({ size = 72, thickness = 8, label, style, ...rest }) {
  return (
    <div style={{ direction: "rtl", fontFamily: "var(--fos-font)", display: "flex", alignItems: "center", gap: "var(--fos-space-5)", ...style }} {...rest}>
      <style>{KEYFRAMES}</style>
      <div
        style={{
          width: size,
          height: size,
          flex: "0 0 auto",
          borderRadius: "var(--fos-radius-full)",
          border: `${thickness}px solid var(--fos-text-10)`,
          borderTopColor: "var(--fos-accent)",
          boxSizing: "border-box",
          animation: "fos-spin 900ms var(--fos-ease-linear) infinite"
        }}
      />
      {label && <div style={{ fontSize: "var(--fos-size-body)", color: "var(--fos-text-60)" }}>{label}</div>}
    </div>
  );
}
