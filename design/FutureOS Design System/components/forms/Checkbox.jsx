import React from "react";
import { FosIcon } from "../core/FosIcon.jsx";

/* Checkbox: multi-select, for the trailing slot of a SettingItem or ListItem
   (bulk-delete a thread, pick which days to sync). Checked = solid accent with a
   black check, the same fill/ink pair the selected chip uses. Unchecked is an
   empty box with a 40% text border, so it survives a white accent. */
export function Checkbox({ checked = false, size = 48, style, onChange, ...rest }) {
  return (
    <div
      onClick={onChange}
      role="checkbox"
      aria-checked={checked}
      style={{
        width: size,
        height: size,
        flex: "0 0 auto",
        borderRadius: "var(--fos-radius-item)",
        background: checked ? "var(--fos-accent)" : "transparent",
        border: checked ? "var(--fos-focus-border-control) solid var(--fos-accent)" : "var(--fos-focus-border-control) solid var(--fos-text-40)",
        boxSizing: "border-box",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        cursor: "pointer",
        transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus)",
        ...style
      }}
      {...rest}
    >
      {checked && <FosIcon name="check" size={Math.round(size * 0.72)} color="var(--fos-on-accent)" strokeWidth={2.2} />}
    </div>
  );
}
