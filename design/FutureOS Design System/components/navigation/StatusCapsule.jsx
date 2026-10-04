import React from "react";
import { FosIcon } from "../core/FosIcon.jsx";

const PAD = 18; /* 9dp */
const MAX_GLYPHS = 4;

/* StatusCapsule: a status-bar capsule (DynamicCapsule) — #1C1C1E at 55% with a 0.5dp 15% white
   hairline, 22dp (44px) tall, a pill, 9dp inside each end, over a transparent bar. The leading
   capsule is dynamic: it widens (200ms, standard easing, no bounce) to show live activity — a call
   (success fill, black ink, call icon + duration), charging (bolt in success + label), a new
   notification (app glyph + app name, label at most 90dp), media (music note + title, label at
   most 110dp, ellipsized) — then returns to the time and the app glyphs (at most four, then +N).
   Pass children for a static capsule (the trailing device-status capsule). Ink is always white. */
export function StatusCapsule({ state = "idle", time, label, apps = [], icon = "chat", children, style, ...rest }) {
  const innerRef = React.useRef(null);
  const [width, setWidth] = React.useState(null);
  React.useLayoutEffect(() => {
    const el = innerRef.current;
    if (!el) return undefined;
    const measure = () => setWidth(Math.ceil(el.getBoundingClientRect().width) + 2);
    measure();
    if (typeof ResizeObserver === "undefined") return undefined;
    const ro = new ResizeObserver(measure);
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  const call = !children && state === "call";
  const ink = call ? "var(--fos-on-accent)" : "var(--fos-shell-ink)";
  const lead = state === "charging" ? { name: "bolt", size: 24, color: "var(--fos-shell-success)" }
    : state === "notification" ? { name: icon, size: 28, color: ink }
    : state === "media" ? { name: "music_note", size: 28, color: ink }
    : call ? { name: "call", size: 24, color: ink }
    : null;
  const labelMax = state === "notification" ? 180 : state === "media" ? 220 : undefined;
  const shown = apps.slice(0, MAX_GLYPHS);

  let content;
  if (children) content = children;
  else if (state === "idle") {
    content = (
      <>
        <span style={{ fontSize: "var(--fos-size-body)", fontWeight: "var(--fos-weight-semibold)", fontVariantNumeric: "tabular-nums" }}>{time}</span>
        {shown.length > 0 && (
          <span style={{ display: "flex", alignItems: "center", gap: "var(--fos-space-1)" }}>
            {shown.map((a, i) => <FosIcon key={a + i} name={a} size={28} color="currentColor" />)}
            {apps.length > MAX_GLYPHS && <span style={{ fontSize: "var(--fos-size-label)", fontWeight: "var(--fos-weight-semibold)", color: "var(--fos-shell-ink-70)" }}>+{apps.length - MAX_GLYPHS}</span>}
          </span>
        )}
      </>
    );
  } else {
    content = (
      <>
        {lead && <FosIcon name={lead.name} size={lead.size} color={lead.color} />}
        <span
          style={{
            maxWidth: labelMax,
            fontSize: "var(--fos-size-label)",
            fontWeight: call ? "var(--fos-weight-bold)" : "var(--fos-weight-medium)",
            fontVariantNumeric: "tabular-nums",
            whiteSpace: "nowrap",
            overflow: "hidden",
            textOverflow: "ellipsis"
          }}
        >
          {label}
        </span>
      </>
    );
  }

  return (
    <div
      style={{
        direction: "rtl",
        fontFamily: "var(--fos-font)",
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "flex-start",
        height: 44,
        width: width == null ? "max-content" : width,
        boxSizing: "border-box",
        overflow: "hidden",
        borderRadius: "var(--fos-radius-full)",
        background: call ? "var(--fos-shell-success)" : "var(--fos-capsule-bg)",
        border: `var(--fos-border-headsup) solid ${call ? "var(--fos-shell-success)" : "var(--fos-hairline-dark)"}`,
        color: ink,
        transition: "width var(--fos-transition-focus), background var(--fos-transition-focus), border-color var(--fos-transition-focus)",
        ...style
      }}
      {...rest}
    >
      <div
        ref={innerRef}
        style={{
          flex: "0 0 auto",
          display: "flex",
          alignItems: "center",
          gap: children ? 14 /* 7dp */ : state === "idle" ? "var(--fos-space-2)" : state === "notification" || state === "media" ? 10 /* 5dp */ : "var(--fos-space-1)",
          padding: `0 ${PAD}px`,
          width: "max-content"
        }}
      >
        {content}
      </div>
    </div>
  );
}
