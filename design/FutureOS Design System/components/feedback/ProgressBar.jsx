import React from "react";

/* LinearProgressIndicator: 4dp tall (2dp in the music player), 10% text track,
   accent fill growing from the RIGHT. */
export function ProgressBar({ value = 0, mini = false, style, ...rest }) {
  const h = mini ? 4 : 8;
  const pct = Math.max(0, Math.min(1, value)) * 100;
  return (
    <div
      style={{
        direction: "rtl",
        height: h,
        borderRadius: h / 2,
        background: "var(--fos-text-10)",
        overflow: "hidden",
        ...style
      }}
      {...rest}
    >
      <div style={{ width: pct + "%", height: "100%", borderRadius: h / 2, background: "var(--fos-accent)", transition: "width var(--fos-duration-standard) var(--fos-ease-linear)" }} />
    </div>
  );
}
