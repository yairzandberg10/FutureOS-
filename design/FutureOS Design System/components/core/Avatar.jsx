import React from "react";
import { FosIcon } from "./FosIcon.jsx";

/* Avatar: the circle every person- or device-facing surface repeats — glass fill,
   full radius, and either a glyph at 44% of the circle or the name's initials at
   30% in the display face. Sizes are free; 88 is the list size, 176 the hero size. */
function initials(name) {
  const parts = String(name || "").trim().split(/\s+/).filter(Boolean).slice(0, 2);
  return parts.map((p) => p[0]).join("");
}

export function Avatar({ name, icon, size = 88, color, background, style, ...rest }) {
  return (
    <div
      style={{
        width: size,
        height: size,
        flex: "0 0 auto",
        borderRadius: "var(--fos-radius-full)",
        background: background || "var(--fos-glass)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        ...style
      }}
      {...rest}
    >
      {icon
        ? <FosIcon name={icon} size={Math.round(size * 0.44)} color={color || "var(--fos-text-60)"} />
        : <span style={{
            fontFamily: "var(--fos-font-display)",
            fontSize: Math.round(size * 0.3),
            fontWeight: "var(--fos-weight-medium)",
            color: color || "var(--fos-text-70)"
          }}>{initials(name)}</span>}
    </div>
  );
}
Avatar.initials = initials;
