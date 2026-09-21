import React from "react";

/* SettingDivider: 0.8dp hairline in the text color at 12% (dark) / 10% (light),
   inset 16dp from each edge of the card. */
export function Divider({ inset = true, style, ...rest }) {
  return (
    <div
      role="separator"
      style={{
        height: "var(--fos-border-divider-row)",
        background: "var(--fos-border-divider)",
        margin: inset ? "0 var(--fos-space-7)" : 0,
        ...style
      }}
      {...rest}
    />
  );
}
