import React from "react";
import { FosIcon } from "../core/FosIcon.jsx";
import { DayChip } from "./DayChip.jsx";
import { Button } from "../core/Button.jsx";

const DAYS = ["א", "ב", "ג", "ד", "ה", "ו", "ש"];

/* TimePickerOverlay: a full-screen overlay, not a dialog. 48sp/300 mono values,
   36dp stepper buttons, 12sp/50% unit labels, hours on the RIGHT (RTL first child). */
function Unit({ value, label, upFocused }) {
  const step = {
    width: 72, height: 72, borderRadius: 36, display: "inline-flex",
    alignItems: "center", justifyContent: "center", border: "none", cursor: "pointer"
  };
  return (
    <div style={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
      <button type="button" style={{ ...step, background: upFocused ? "var(--fos-accent-30)" : "var(--fos-text-08)" }}>
        <FosIcon name="keyboard_arrow_up" size={36} color="var(--fos-accent)" />
      </button>
      <div style={{ fontFamily: "var(--fos-font-mono)", fontSize: "var(--fos-size-clock)", fontWeight: "var(--fos-weight-light)", color: "var(--fos-text)", padding: "var(--fos-space-3) 0", lineHeight: 1.2 }}>{value}</div>
      <button type="button" style={{ ...step, background: "var(--fos-text-08)" }}>
        <FosIcon name="keyboard_arrow_down" size={36} color="var(--fos-accent)" />
      </button>
      <div style={{ fontSize: "var(--fos-size-label)", color: "var(--fos-text-50)", marginTop: "var(--fos-space-2)" }}>{label}</div>
    </div>
  );
}

export function TimePicker({ title = "ערוך שעה", hours = "07", minutes = "30", repeat = [0, 1, 2, 3, 4], focusedDay = 5, onCancel, onSave, style, ...rest }) {
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
        gap: "var(--fos-space-9)",
        padding: "var(--fos-space-7)",
        ...style
      }}
      {...rest}
    >
      <div style={{ fontSize: "var(--fos-size-screen-title)", fontWeight: "var(--fos-weight-bold)", color: "var(--fos-text)" }}>{title}</div>
      <div style={{ display: "flex", alignItems: "flex-start", gap: "var(--fos-space-7)" }}>
        <Unit value={hours} label="שעות" upFocused />
        <div style={{ fontFamily: "var(--fos-font-mono)", fontSize: "var(--fos-size-clock)", fontWeight: "var(--fos-weight-light)", color: "var(--fos-text)", lineHeight: 1, marginTop: 96 }}>:</div>
        <Unit value={minutes} label="דקות" />
      </div>
      <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: "var(--fos-space-3)" }}>
        <div style={{ fontSize: "var(--fos-size-summary)", color: "var(--fos-text-60)" }}>חוזרת</div>
        <div style={{ display: "flex", gap: "var(--fos-space-2)" }}>
          {DAYS.map((d, i) => (
            <DayChip key={d} selected={repeat.includes(i)} focused={i === focusedDay}>{d}</DayChip>
          ))}
        </div>
      </div>
      <div style={{ display: "flex", gap: "var(--fos-space-7)", width: "100%" }}>
        <Button variant="quiet" fullWidth onClick={onCancel}>ביטול</Button>
        <Button variant="primary" fullWidth focused onClick={onSave}>שמור</Button>
      </div>
    </div>
  );
}
