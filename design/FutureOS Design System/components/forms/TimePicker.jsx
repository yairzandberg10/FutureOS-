import React from "react";
import { FosIcon } from "../core/FosIcon.jsx";
import { DayChip } from "./DayChip.jsx";
import { Button } from "../core/Button.jsx";

const DAYS = ["א", "ב", "ג", "ד", "ה", "ו", "ש"];

/* TimePickerOverlay: a full-screen overlay, not a dialog. 48sp/300 mono values,
   36dp stepper buttons, 12sp/50% unit labels, hours on the RIGHT (RTL first column). */
function Step({ dir, focused }) {
  return (
    <button type="button" style={{
      width: 72, height: 72, borderRadius: "var(--fos-radius-full)", display: "inline-flex",
      alignItems: "center", justifyContent: "center", border: "none", cursor: "pointer",
      background: focused ? "var(--fos-accent-30)" : "var(--fos-text-08)"
    }}>
      <FosIcon name={dir === "up" ? "keyboard_arrow_up" : "keyboard_arrow_down"} size={36} color="var(--fos-accent)" />
    </button>
  );
}

const VALUE = {
  fontFamily: "var(--fos-font-mono)", fontSize: "var(--fos-size-clock)",
  fontWeight: "var(--fos-weight-light)", color: "var(--fos-text)", lineHeight: 1,
  fontVariantNumeric: "tabular-nums", textAlign: "center"
};
const UNIT_LABEL = { fontSize: "var(--fos-size-label)", color: "var(--fos-text-50)", textAlign: "center" };

export function TimePicker({ title = "ערוך שעה", hours = "07", minutes = "18", repeat = [0, 1, 2, 3, 4], focusedDay = 5, onCancel, onSave, style, ...rest }) {
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
      <div style={{
        display: "grid",
        gridTemplateColumns: "104px 40px 104px",
        gridTemplateRows: "auto auto auto auto",
        justifyItems: "center",
        alignItems: "center",
        columnGap: "var(--fos-space-5)",
        rowGap: "var(--fos-space-3)"
      }}>
        <Step dir="up" focused />
        <div />
        <Step dir="up" />
        <div style={VALUE}>{hours}</div>
        <div style={VALUE}>:</div>
        <div style={VALUE}>{minutes}</div>
        <Step dir="down" />
        <div />
        <Step dir="down" />
        <div style={UNIT_LABEL}>שעות</div>
        <div />
        <div style={UNIT_LABEL}>דקות</div>
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
