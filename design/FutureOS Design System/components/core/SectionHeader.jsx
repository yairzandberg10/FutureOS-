import React from "react";

/* SettingHeader: 13sp / 700 / 55% text, 1sp letter-spacing, 24dp start, 20dp top, 8dp bottom. */
export function SectionHeader({ children, style, ...rest }) {
  return (
    <div
      style={{
        direction: "rtl",
        fontFamily: "var(--fos-font-display)",
        fontSize: "var(--fos-size-summary)",
        fontWeight: "var(--fos-weight-bold)",
        letterSpacing: "var(--fos-tracking-section)",
        color: "var(--fos-text-55)",
        padding: "var(--fos-space-8) var(--fos-space-9) var(--fos-space-3) var(--fos-space-9)",
        ...style
      }}
      {...rest}
    >
      {children}
    </div>
  );
}
