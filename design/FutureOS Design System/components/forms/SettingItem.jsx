import React from "react";
import { FosIcon } from "../core/FosIcon.jsx";

/* SettingItem: 108px row inside a Card. 4dp outer / 12dp inner horizontal padding,
   22dp accent icon, 16dp gap, 18dp chevron at 30%, title 17sp/600, summary 13sp/60%.
   Focus = 6% text background + 2dp accent border. No scale.
   The focus fills the full width of the card: the first row keeps the card's top
   corners, the last row its bottom corners, a middle row none. Card assigns this
   automatically to the SettingItem rows inside it; pass `position` to override. */
const CARD_R = "var(--fos-radius-main)";
const focusRadius = {
  only: CARD_R,
  first: `${CARD_R} ${CARD_R} 0 0`,
  last: `0 0 ${CARD_R} ${CARD_R}`,
  middle: "0"
};
const bleed = "calc(-1 * var(--fos-space-1))"; /* eats the card's 6dp vertical padding */

export function SettingItem({ title, summary, icon, trailing, chevron = true, focused = false, position = "only", onClick, style, ...rest }) {
  const top = position === "first" || position === "only" ? bleed : "0";
  const bottom = position === "last" || position === "only" ? bleed : "0";
  return (
    <div
      onClick={onClick}
      style={{
        direction: "rtl",
        fontFamily: "var(--fos-font)",
        display: "flex",
        alignItems: "center",
        gap: "var(--fos-space-7)",
        height: "var(--fos-row-setting)",
        margin: `${top} 0 ${bottom}`,
        padding: "0 var(--fos-space-7)",
        borderRadius: focusRadius[position] || CARD_R,
        background: focused ? "var(--fos-focus-bg-setting)" : "transparent",
        border: `var(--fos-focus-border-control) solid ${focused ? "var(--fos-accent)" : "transparent"}`,
        boxSizing: "border-box",
        cursor: "pointer",
        transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus)",
        ...style
      }}
      {...rest}
    >
      {icon && <FosIcon name={icon} size={44} color="var(--fos-accent)" />}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: "var(--fos-size-title)", fontWeight: "var(--fos-weight-semibold)", color: "var(--fos-text)" }}>{title}</div>
        {summary && <div style={{ fontSize: "var(--fos-size-summary)", color: "var(--fos-text-60)", marginTop: 4 }}>{summary}</div>}
      </div>
      {trailing}
      {!trailing && chevron && <FosIcon name="chevron_left" size={36} color="var(--fos-text)" opacity={0.3} />}
    </div>
  );
}
SettingItem.__fosCardRow = true;
