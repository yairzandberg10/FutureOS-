import React from "react";

/* FocusableItem: 112px row, 4dp inner padding, title 17sp/500, summary 13sp/60%.
   Focus = 14% accent background + 1.5dp accent border + 1.02 scale. */
export function ListItem({ title, summary, trailing, focused = false, onClick, style, ...rest }) {
  return (
    <div
      onClick={onClick}
      style={{
        direction: "rtl",
        fontFamily: "var(--fos-font)",
        display: "flex",
        alignItems: "center",
        gap: "var(--fos-space-5)",
        height: "var(--fos-row-list)",
        padding: "0 var(--fos-space-5)",
        borderRadius: "var(--fos-radius-row)",
        background: focused ? "var(--fos-focus-bg-item)" : "transparent",
        border: `var(--fos-focus-border-item) solid ${focused ? "var(--fos-accent)" : "transparent"}`,
        transform: focused ? "scale(var(--fos-focus-scale))" : "none",
        transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus), transform var(--fos-transition-focus)",
        cursor: "pointer",
        boxSizing: "border-box",
        ...style
      }}
      {...rest}
    >
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: "var(--fos-size-title)", fontWeight: "var(--fos-weight-medium)", color: "var(--fos-text)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{title}</div>
        {summary && <div style={{ fontSize: "var(--fos-size-summary)", color: "var(--fos-text-60)", marginTop: 4, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{summary}</div>}
      </div>
      {trailing}
    </div>
  );
}
