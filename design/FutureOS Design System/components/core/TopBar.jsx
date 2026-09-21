import React from "react";
import { IconButton } from "./IconButton.jsx";

/* ScreenTopBar: 16dp horizontal / 12dp vertical padding, 20sp/700 title, 36dp buttons.
   RTL — back sits at the right (the start), overflow at the left. */
export function TopBar({ title, onBack, onMenu, backFocused = false, menuFocused = false, style, ...rest }) {
  return (
    <div
      style={{
        direction: "rtl",
        fontFamily: "var(--fos-font)",
        display: "flex",
        alignItems: "center",
        gap: "var(--fos-space-3)",
        padding: "var(--fos-space-5) var(--fos-space-7)",
        ...style
      }}
      {...rest}
    >
      {onBack && <IconButton icon="arrow_forward" focused={backFocused} onClick={onBack} />}
      <div style={{ flex: 1, minWidth: 0, fontFamily: "var(--fos-font-display)", fontSize: "var(--fos-size-screen-title)", fontWeight: "var(--fos-weight-bold)", color: "var(--fos-text)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{title}</div>
      {onMenu && <IconButton icon="more_vert" focused={menuFocused} onClick={onMenu} />}
    </div>
  );
}
