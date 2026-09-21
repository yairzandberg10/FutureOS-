import React from "react";

/* 18dp accent circle with black 10sp/700 digits. Unread message count only. */
export function Badge({ count, style, ...rest }) {
  return (
    <div
      style={{
        minWidth: 36,
        height: 36,
        flex: "0 0 auto",
        padding: "0 8px",
        borderRadius: "var(--fos-radius-full)",
        background: "var(--fos-accent)",
        color: "var(--fos-on-accent)",
        fontFamily: "var(--fos-font)",
        fontSize: "var(--fos-size-badge)",
        fontWeight: "var(--fos-weight-bold)",
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        boxSizing: "border-box",
        ...style
      }}
      {...rest}
    >
      {count}
    </div>
  );
}
