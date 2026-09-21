import React from "react";

/* TextArea: TextField's surface grown to a paragraph — same 8% fill, 10dp radius
   and accent focus border, but 16sp text on the body line-height, a minimum height
   and top alignment. `dir`/`align` let a field hold a foreign-language string. */
export function TextArea({ value, placeholder, focused = false, showCaret = true, minHeight = 150, dir = "rtl", align, style, onClick, ...rest }) {
  const empty = !value;
  return (
    <div
      onClick={onClick}
      style={{
        fontFamily: "var(--fos-font)",
        fontSize: "var(--fos-size-base)",
        lineHeight: "var(--fos-line-height)",
        color: empty ? "var(--fos-text-40)" : "var(--fos-text)",
        background: "var(--fos-idle-bg-field)",
        borderRadius: "var(--fos-radius-textfield)",
        border: `var(--fos-focus-border-control) solid ${focused ? "var(--fos-accent)" : "transparent"}`,
        padding: "var(--fos-space-5)",
        minHeight,
        boxSizing: "border-box",
        direction: dir,
        textAlign: align || (dir === "rtl" ? "right" : "left"),
        textWrap: "pretty",
        cursor: onClick ? "pointer" : "default",
        transition: "border-color var(--fos-transition-focus)",
        ...style
      }}
      {...rest}
    >
      {empty ? placeholder : value}
      {focused && showCaret && (
        <span style={{ display: "inline-block", width: 3, height: 34, background: "var(--fos-accent)", verticalAlign: "-6px", marginInlineStart: 6 }} />
      )}
    </div>
  );
}
