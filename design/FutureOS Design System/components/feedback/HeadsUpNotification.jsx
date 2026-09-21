import React from "react";
import { FosIcon } from "../core/FosIcon.jsx";

/* HeadsUpNotificationScreen: floats over everything, inset 12dp from the sides and
   8dp from the top. 28dp radius, #1C1C1E at 90%, 0.5dp white hairline at 15%.
   Always dark, even when the system is in light mode — hence the literal colors. */
export function HeadsUpNotification({ appName, title, body, icon = "chat", style, ...rest }) {
  return (
    <div
      style={{
        direction: "rtl",
        fontFamily: "var(--fos-font)",
        margin: "var(--fos-space-3) var(--fos-space-5)",
        padding: "var(--fos-space-4) var(--fos-space-6)",
        borderRadius: "var(--fos-radius-headsup)",
        background: "var(--fos-headsup-bg)",
        border: "var(--fos-border-headsup) solid rgba(255,255,255,0.15)",
        boxShadow: "var(--fos-shadow-headsup)",
        display: "flex",
        alignItems: "center",
        gap: "var(--fos-space-5)",
        boxSizing: "border-box",
        ...style
      }}
      {...rest}
    >
      <div style={{ width: 68, height: 68, flex: "0 0 auto", borderRadius: "var(--fos-radius-full)", background: "rgba(255,255,255,0.12)", display: "flex", alignItems: "center", justifyContent: "center" }}>
        <FosIcon name={icon} size={36} color="#FFFFFF" />
      </div>
      <div style={{ minWidth: 0 }}>
        <div style={{ fontSize: "var(--fos-size-label)", color: "rgba(255,255,255,0.65)" }}>{appName}</div>
        <div style={{ fontSize: "var(--fos-size-dialog)", fontWeight: "var(--fos-weight-bold)", color: "#FFFFFF", marginTop: 2 }}>{title}</div>
        {body && <div style={{ fontSize: "var(--fos-size-summary)", color: "rgba(255,255,255,0.55)", marginTop: 4, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{body}</div>}
      </div>
    </div>
  );
}
