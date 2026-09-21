import React from "react";
import { FosIcon } from "./FosIcon.jsx";

/* TopBarIconButton: 36dp circle, 8% text background idle, 30% accent when focused,
   18dp glyph in the text color. */
export function IconButton({ icon, focused = false, color = "var(--fos-text)", onClick, style, ...rest }) {
  return (
    <button
      type="button"
      onClick={onClick}
      style={{
        width: "var(--fos-row-topbar-btn)",
        height: "var(--fos-row-topbar-btn)",
        flex: "0 0 auto",
        borderRadius: "var(--fos-radius-full)",
        border: "none",
        background: focused ? "var(--fos-focus-bg-icon)" : "var(--fos-idle-bg-icon)",
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        cursor: "pointer",
        transition: "background var(--fos-transition-focus)",
        ...style
      }}
      {...rest}
    >
      <FosIcon name={icon} size={36} color={color} />
    </button>
  );
}
