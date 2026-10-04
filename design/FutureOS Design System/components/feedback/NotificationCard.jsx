import React from "react";
import { FosIcon } from "../core/FosIcon.jsx";
import { GlassTile } from "./GlassPanel.jsx";

/* NotificationCard: one notification in the Notification Center, a GlassTile (white 15% at
   rest, 24% + 2dp white ring when focused) at 28dp radius, 14dp / 13dp padding. Collapsed: a
   38dp app disc (white 20%) and a text column — "app: title" at 14sp/700, then the body at
   13sp in two lines, all of it when `expanded` (long OK). `optionsOpen` (the Options key)
   replaces that content with the notification's actions as OptionsMenu rows stacked
   vertically: 50dp tall, 16dp radius, 15sp, the selected row a 12% white fill with no border,
   a destructive row in the danger-on-glass color; a 12sp app-name header sits above them.
   Always white ink — it sits on a GlassPanel. */
export function NotificationCard({ appName, title, body, icon = "chat", focused = false, expanded = false, optionsOpen = false, options = [], focusedOption = 0, onSelect, style, ...rest }) {
  return (
    <GlassTile
      focused={focused}
      radius="var(--fos-radius-headsup)"
      style={{ padding: "26px var(--fos-space-6)", transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus), height var(--fos-transition-focus)", ...style }}
      {...rest}
    >
      {optionsOpen ? (
        <div>
          <div style={{ fontSize: "var(--fos-size-label)", color: "var(--fos-shell-ink-sub)", padding: "0 var(--fos-space-5) var(--fos-space-1)" }}>{appName}</div>
          {options.map((it, i) => (
            <div
              key={it.label}
              onClick={() => onSelect && onSelect(i)}
              style={{
                display: "flex",
                alignItems: "center",
                height: "var(--fos-row-menu)",
                padding: "0 var(--fos-space-5)",
                borderRadius: "var(--fos-radius-card)",
                background: i === focusedOption ? "var(--fos-shell-ink-12)" : "transparent",
                color: it.destructive ? "var(--fos-danger-on-glass)" : "var(--fos-shell-ink)",
                cursor: "pointer",
                transition: "background var(--fos-transition-focus)"
              }}
            >
              <div style={{ minWidth: 0, fontSize: "var(--fos-size-dialog)", fontWeight: "var(--fos-weight-medium)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{it.label}</div>
            </div>
          ))}
        </div>
      ) : (
        <div style={{ display: "flex", alignItems: "center", gap: "var(--fos-space-6)" }}>
          <div style={{ flex: "0 0 auto", width: 76, height: 76, borderRadius: "var(--fos-radius-full)", background: "var(--fos-shell-ink-20)", display: "grid", placeItems: "center", color: "var(--fos-shell-ink-sub)" }}>
            <FosIcon name={icon} size={36} color="currentColor" />
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: "var(--fos-size-body)", fontWeight: "var(--fos-weight-bold)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{title ? `${appName}: ${title}` : appName}</div>
            {body && (
              <div
                style={{
                  fontSize: "var(--fos-size-summary)",
                  color: "var(--fos-shell-ink-sub)",
                  ...(expanded ? null : { display: "-webkit-box", WebkitBoxOrient: "vertical", WebkitLineClamp: 2, overflow: "hidden" })
                }}
              >
                {body}
              </div>
            )}
          </div>
        </div>
      )}
    </GlassTile>
  );
}
