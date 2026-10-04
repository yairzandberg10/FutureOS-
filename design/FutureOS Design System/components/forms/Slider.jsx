import React from "react";

/* VolumeSlider: full-width row at 16dp card radius, 16dp inner padding,
   label 14sp/60%, 6dp track at 15% text, accent fill starting from the RIGHT.
   Left/right keys step 5%. Focus = 18% text background + 2dp accent border. */
export function Slider({ label, value = 0.5, focused = false, style, ...rest }) {
  const pct = Math.max(0, Math.min(1, value)) * 100;
  return (
    <div
      style={{
        direction: "rtl",
        fontFamily: "var(--fos-font)",
        margin: "0 var(--fos-space-7)",
        padding: "var(--fos-space-7)",
        borderRadius: "var(--fos-radius-card)",
        background: focused ? "var(--fos-focus-bg-slider)" : "var(--fos-idle-bg-slider)",
        border: `var(--fos-focus-border-control) solid ${focused ? "var(--fos-accent)" : "transparent"}`,
        boxSizing: "border-box",
        transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus)",
        ...style
      }}
      {...rest}
    >
      <div style={{ fontSize: "var(--fos-size-body)", color: "var(--fos-text-60)", marginBottom: "var(--fos-space-5)" }}>{label}</div>
      <div style={{ height: 12, borderRadius: 6, background: "var(--fos-text-15)", overflow: "hidden" }}>
        <div style={{ width: pct + "%", height: "100%", borderRadius: 6, background: "var(--fos-accent)", transition: "width var(--fos-transition-focus)" }} />
      </div>
    </div>
  );
}
