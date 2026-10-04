import React from "react";
import { DayChip } from "./DayChip.jsx";
import { Button } from "../core/Button.jsx";

const DAYS = ["א", "ב", "ג", "ד", "ה", "ו", "ש"];

/* TimePickerOverlay: a full-screen overlay, not a dialog. Two wheels — hours on
   the RIGHT (RTL first column), minutes on the left. A wheel is ONE focus target:
   field fill (8% text) at rest, 14% accent + 2px-dp accent ring when focused.
   Inside: the next value (above) and previous value (below) at 30%, the value
   itself at 48sp/300 mono, and a 12sp/50% unit label. Up/down step the focused
   wheel, digits type the value directly, OK moves on to the repeat row. */
function Wheel({ value, max, label, focused }) {
  const pad = (n) => String(n).padStart(2, "0");
  const v = Number(value);
  const next = v === max ? 0 : v + 1;
  const prev = v === 0 ? max : v - 1;
  const side = {
    fontFamily: "var(--fos-font-mono)", fontSize: "calc(48px * var(--fos-font-scale))" /* 24sp headline */,
    color: "var(--fos-text-30)", lineHeight: 1.2, fontVariantNumeric: "tabular-nums"
  };
  return (
    <div style={{
      width: 208, display: "flex", flexDirection: "column", alignItems: "center",
      padding: "var(--fos-space-3) 0", borderRadius: "var(--fos-radius-textfield)",
      background: focused ? "var(--fos-accent-14)" : "var(--fos-text-08)",
      boxShadow: focused ? "inset 0 0 0 4px var(--fos-accent)" : "none"
    }}>
      <div style={side}>{pad(next)}</div>
      <div style={VALUE}>{pad(v)}</div>
      <div style={side}>{pad(prev)}</div>
      <div style={UNIT_LABEL}>{label}</div>
    </div>
  );
}

const VALUE = {
  fontFamily: "var(--fos-font-mono)", fontSize: "var(--fos-size-clock)",
  fontWeight: "var(--fos-weight-light)", color: "var(--fos-text)", lineHeight: 1,
  fontVariantNumeric: "tabular-nums", textAlign: "center"
};
const UNIT_LABEL = { fontSize: "var(--fos-size-label)", color: "var(--fos-text-50)", textAlign: "center" };

export function TimePicker({ title = "ערוך שעה", hours = "07", minutes = "18", repeat = [0, 1, 2, 3, 4], focusedWheel = "hours", focusedDay = -1, onCancel, onSave, style, ...rest }) {
  return (
    <div
      style={{
        direction: "rtl",
        fontFamily: "var(--fos-font)",
        background: "var(--fos-bg)",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        gap: "var(--fos-space-7)",
        padding: "var(--fos-space-7)",
        ...style
      }}
      {...rest}
    >
      <div style={{ fontSize: "var(--fos-size-screen-title)", fontWeight: "var(--fos-weight-bold)", color: "var(--fos-text)" }}>{title}</div>
      <div style={{ display: "flex", alignItems: "center", gap: "var(--fos-space-3)" }}>
        <Wheel value={hours} max={23} label="שעות" focused={focusedWheel === "hours"} />
        <div style={VALUE}>:</div>
        <Wheel value={minutes} max={59} label="דקות" focused={focusedWheel === "minutes"} />
      </div>
      <div style={{ fontSize: "var(--fos-size-summary)", color: "var(--fos-text-60)" }}>↑↓ שינוי · ספרות הקלדה · OK הבא</div>
      <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: "var(--fos-space-3)" }}>
        <div style={{ fontSize: "var(--fos-size-summary)", color: "var(--fos-text-60)" }}>{repeat.length ? "חוזרת" : "חד-פעמית"}</div>
        <div style={{ display: "flex", gap: "var(--fos-space-2)" }}>
          {DAYS.map((d, i) => (
            <DayChip key={d} selected={repeat.includes(i)} focused={i === focusedDay}>{d}</DayChip>
          ))}
        </div>
      </div>
      <div style={{ display: "flex", gap: "var(--fos-space-7)", width: "100%" }}>
        <Button variant="quiet" fullWidth onClick={onCancel}>ביטול</Button>
        <Button variant="primary" fullWidth onClick={onSave}>שמור</Button>
      </div>
    </div>
  );
}
