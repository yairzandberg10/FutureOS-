import React from "react";
import { FosIcon } from "../core/FosIcon.jsx";
import { Button } from "../core/Button.jsx";

/* DatePicker: TimePickerOverlay's sibling — a full-screen overlay, not a dialog.
   Month stepper at the top, then a 7-column grid that flows right-to-left from
   Sunday. Selected = solid accent with black text (the system's selected state);
   focused = 2dp accent ring. Weeks are computed from year/month; nothing is
   stateful, the screen owns the cursor. */
const DAYS = ["א", "ב", "ג", "ד", "ה", "ו", "ש"];
const MONTHS = ["ינואר", "פברואר", "מרץ", "אפריל", "מאי", "יוני", "יולי", "אוגוסט", "ספטמבר", "אוקטובר", "נובמבר", "דצמבר"];

export function DatePicker({ title = "בחר תאריך", year = 2026, month = 1, selected, focused, onPrevMonth, onNextMonth, onCancel, onSave, onSelect, style, ...rest }) {
  const first = new Date(year, month - 1, 1).getDay();
  const count = new Date(year, month, 0).getDate();
  const cells = [];
  for (let i = 0; i < first; i++) cells.push(null);
  for (let d = 1; d <= count; d++) cells.push(d);
  const step = { width: 72, height: 72, borderRadius: "var(--fos-radius-full)", background: "var(--fos-text-08)", border: "none", display: "inline-flex", alignItems: "center", justifyContent: "center", cursor: "pointer" };
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
        gap: "var(--fos-space-8)",
        padding: "var(--fos-space-7)",
        ...style
      }}
      {...rest}
    >
      <div style={{ fontSize: "var(--fos-size-screen-title)", fontWeight: "var(--fos-weight-bold)" }}>{title}</div>

      <div style={{ display: "flex", alignItems: "center", gap: "var(--fos-space-7)" }}>
        <button type="button" style={step} onClick={onNextMonth}>
          <FosIcon name="keyboard_arrow_left" size={36} color="var(--fos-accent)" />
        </button>
        <div style={{ fontSize: "var(--fos-size-title)", fontWeight: "var(--fos-weight-medium)", minWidth: 260, textAlign: "center" }}>{MONTHS[month - 1]} {year}</div>
        <button type="button" style={step} onClick={onPrevMonth}>
          <FosIcon name="keyboard_arrow_right" size={36} color="var(--fos-accent)" />
        </button>
      </div>

      <div style={{ width: "100%", display: "flex", flexDirection: "column", gap: "var(--fos-space-2)" }}>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(7, 1fr)", gap: "var(--fos-space-2)" }}>
          {DAYS.map((d) => (
            <div key={d} style={{ textAlign: "center", fontSize: "var(--fos-size-label)", color: "var(--fos-text-50)", letterSpacing: "var(--fos-tracking-section)" }}>{d}</div>
          ))}
        </div>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(7, 1fr)", gap: "var(--fos-space-2)" }}>
          {cells.map((d, i) => {
            if (d === null) return <div key={"e" + i} />;
            const sel = d === selected;
            const foc = d === focused;
            return (
              <div
                key={d}
                onClick={onSelect ? () => onSelect(d) : undefined}
                style={{
                  aspectRatio: "1",
                  borderRadius: "var(--fos-radius-full)",
                  background: sel ? "var(--fos-accent)" : "transparent",
                  border: `var(--fos-focus-border-control) solid ${foc && !sel ? "var(--fos-accent)" : "transparent"}`,
                  boxSizing: "border-box",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  fontSize: "var(--fos-size-body)",
                  fontWeight: sel ? "var(--fos-weight-bold)" : "var(--fos-weight-regular)",
                  color: sel ? "var(--fos-on-accent)" : "var(--fos-text)",
                  fontVariantNumeric: "tabular-nums",
                  cursor: "pointer",
                  transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus)"
                }}
              >{d}</div>
            );
          })}
        </div>
      </div>

      <div style={{ display: "flex", gap: "var(--fos-space-7)", width: "100%" }}>
        <Button variant="quiet" fullWidth onClick={onCancel}>ביטול</Button>
        <Button variant="primary" fullWidth focused onClick={onSave}>שמור</Button>
      </div>
    </div>
  );
}
