import React from "react";

/* BasicTextField: 8% text background, card radius, 12dp padding, 15sp text,
   2dp accent focus border, and a 3px accent caret. */
export function TextField({ value, placeholder, focused = false, showCaret = true, style, ...rest }) {
  const empty = !value;
  return (
    <div
      style={{
        direction: "rtl",
        fontFamily: "var(--fos-font)",
        fontSize: "var(--fos-size-dialog)",
        color: empty ? "var(--fos-text-40)" : "var(--fos-text)",
        background: "var(--fos-idle-bg-field)",
        borderRadius: "var(--fos-radius-textfield)",
        border: `var(--fos-focus-border-control) solid ${focused ? "var(--fos-accent)" : "transparent"}`,
        padding: "var(--fos-space-5)",
        display: "flex",
        alignItems: "center",
        gap: 8,
        boxSizing: "border-box",
        transition: "border-color var(--fos-transition-focus)",
        ...style
      }}
      {...rest}
    >
      <span style={{ whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{empty ? placeholder : value}</span>
      {focused && showCaret && (
        <span style={{ width: 3, height: 36, background: "var(--fos-accent)", flex: "0 0 auto" }} />
      )}
    </div>
  );
}
