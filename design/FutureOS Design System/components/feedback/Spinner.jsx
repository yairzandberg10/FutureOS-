import React from "react";

/* Spinner: the circular counterpart to ProgressBar, for waits with no known
   length (scanning, connecting, sending). 10% text track, a round-capped accent
   arc over a quarter of the circle, one rotation per 900ms — the only rotating
   element in the system. */
const KEYFRAMES = "@keyframes fos-spin{to{transform:rotate(360deg)}}";

export function Spinner({ size = 72, thickness = 8, label, style, ...rest }) {
  const r = (size - thickness) / 2;
  const c = 2 * Math.PI * r;
  return (
    <div style={{ direction: "rtl", fontFamily: "var(--fos-font)", display: "flex", alignItems: "center", gap: "var(--fos-space-5)", ...style }} {...rest}>
      <style>{KEYFRAMES}</style>
      <svg
        width={size}
        height={size}
        viewBox={`0 0 ${size} ${size}`}
        style={{ flex: "0 0 auto", animation: "fos-spin 900ms var(--fos-ease-linear) infinite" }}
      >
        <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke="var(--fos-text-10)" strokeWidth={thickness} />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={r}
          fill="none"
          stroke="var(--fos-accent)"
          strokeWidth={thickness}
          strokeLinecap="round"
          strokeDasharray={`${c * 0.26} ${c}`}
          transform={`rotate(-90 ${size / 2} ${size / 2})`}
        />
      </svg>
      {label && <div style={{ fontSize: "var(--fos-size-body)", color: "var(--fos-text-60)" }}>{label}</div>}
    </div>
  );
}
