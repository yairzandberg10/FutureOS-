import React from "react";

/* RadioButton: one-of-many, for the trailing slot of a row (accent picker,
   ringtone, text size). Ring at 40% text when off, accent ring with a filled
   accent dot when on — never a fill, so a white accent still reads as a ring. */
export function RadioButton({ selected = false, size = 48, style, onChange, ...rest }) {
  return (
    <div
      onClick={onChange}
      role="radio"
      aria-checked={selected}
      style={{
        width: size,
        height: size,
        flex: "0 0 auto",
        borderRadius: "var(--fos-radius-full)",
        border: `var(--fos-focus-border-control) solid ${selected ? "var(--fos-accent)" : "var(--fos-text-40)"}`,
        boxSizing: "border-box",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        cursor: "pointer",
        transition: "border-color var(--fos-transition-focus)",
        ...style
      }}
      {...rest}
    >
      {selected && (
        <div style={{ width: Math.round(size * 0.5), height: Math.round(size * 0.5), borderRadius: "var(--fos-radius-full)", background: "var(--fos-accent)" }} />
      )}
    </div>
  );
}
