import React from "react";

/* SettingSwitch. The Kotlin source sets only the colors, and its thumb is white in
   both states — which, with the default white accent, makes an "on" switch read as a
   plain white pill. This version keeps Material's geometry but takes the thumb out of
   the accent's way: off is an empty track with a 30% hairline and a 40% thumb, on is
   the accent-filled track with the thumb in the screen color. Nothing here depends on
   the accent being chromatic.
   Position is logical, so in RTL the thumb rests at the right and travels left. */
const TRACK_W = 104;
const TRACK_H = 60;
const PAD = 8;
const THUMB_OFF = 28;
const THUMB_ON = 36;

export function Switch({ on = false, onChange, style, ...rest }) {
  const size = on ? THUMB_ON : THUMB_OFF;
  return (
    <div
      onClick={onChange}
      role="switch"
      aria-checked={on}
      style={{
        width: TRACK_W,
        height: TRACK_H,
        flex: "0 0 auto",
        borderRadius: "var(--fos-radius-full)",
        background: on ? "var(--fos-accent)" : "transparent",
        border: `var(--fos-focus-border-control) solid ${on ? "var(--fos-accent)" : "var(--fos-text-30)"}`,
        boxSizing: "border-box",
        position: "relative",
        cursor: "pointer",
        transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus)",
        ...style
      }}
      {...rest}
    >
      <div
        style={{
          position: "absolute",
          top: "50%",
          marginTop: -size / 2,
          insetInlineStart: on ? TRACK_W - size - PAD - 4 : PAD,
          width: size,
          height: size,
          borderRadius: "var(--fos-radius-full)",
          background: on ? "var(--fos-bg)" : "var(--fos-text-40)",
          transition: "all var(--fos-transition-focus)"
        }}
      />
    </div>
  );
}
