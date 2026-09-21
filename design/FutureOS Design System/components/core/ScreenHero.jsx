import React from "react";
import { Avatar } from "./Avatar.jsx";

/* ScreenHero: the centred identity block a detail screen opens with — a large
   Avatar, the name at 22sp/700 in the display face, and one status line at 14sp/60%.
   `eyebrow` is the 12sp tracked label above (שיחה נכנסת); `statusColor` carries the
   live state (success for a running call). Children sit under the status line. */
export function ScreenHero({ name, icon, eyebrow, status, statusColor, size = 176, children, style, ...rest }) {
  return (
    <div
      style={{
        direction: "rtl",
        fontFamily: "var(--fos-font)",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        gap: "var(--fos-space-3)",
        padding: "var(--fos-space-5) var(--fos-space-screen) var(--fos-space-8)",
        textAlign: "center",
        ...style
      }}
      {...rest}
    >
      {eyebrow && (
        <div style={{ fontSize: "var(--fos-size-label)", letterSpacing: "var(--fos-tracking-section)", color: "var(--fos-text-60)" }}>{eyebrow}</div>
      )}
      <Avatar name={name} icon={icon} size={size} />
      <div style={{ fontSize: "var(--fos-size-screen-title)", fontWeight: "var(--fos-weight-bold)", fontFamily: "var(--fos-font-display)", textWrap: "pretty" }}>{name}</div>
      {status && (
        <div style={{ fontSize: "var(--fos-size-body)", color: statusColor || "var(--fos-text-60)" }}>{status}</div>
      )}
      {children}
    </div>
  );
}
