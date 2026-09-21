import React from "react";

/* MonoValue: a running number — call timer, stopwatch, world clock. Roboto Mono,
   tabular figures and LTR always, so the glyphs never shift as digits change.
   `size` is a token value; the three that recur are clock, header and title. */
export function MonoValue({ children, size = "var(--fos-size-header)", weight = 300, color = "var(--fos-text)", label, style, ...rest }) {
  return (
    <div style={{ direction: "rtl", fontFamily: "var(--fos-font)", display: "flex", flexDirection: "column", alignItems: "center", gap: "var(--fos-space-1)", ...style }} {...rest}>
      <div style={{
        fontFamily: "var(--fos-font-mono)",
        fontSize: size,
        fontWeight: weight,
        fontVariantNumeric: "tabular-nums",
        fontFeatureSettings: '"tnum"',
        direction: "ltr",
        color,
        lineHeight: 1.1
      }}>{children}</div>
      {label && <div style={{ fontSize: "var(--fos-size-summary)", color: "var(--fos-text-60)" }}>{label}</div>}
    </div>
  );
}
