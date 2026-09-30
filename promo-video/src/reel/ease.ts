import { Easing, interpolate } from "remotion";

export const ACCENT = "#64D2FF";
export const GREEN = "#32D74B";
export const RED = "#FF6B6B";
export const GRAY = "#B0B0B0";

export const expo = Easing.bezier(0.16, 1, 0.3, 1);
export const smooth = Easing.bezier(0.45, 0, 0.15, 1);

const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;

/** Eased 0..1 (or from..to) ramp between two frames. */
export const ramp = (
  frame: number,
  a: number,
  b: number,
  from = 0,
  to = 1,
  easing: (t: number) => number = expo,
): number => interpolate(frame, [a, b], [from, to], { ...clamp, easing });

/** Fade in at `a`, hold, fade out ending at `d`. */
export const window4 = (frame: number, a: number, b: number, c: number, d: number): number =>
  ramp(frame, a, b, 0, 1, (t) => t) * ramp(frame, c, d, 1, 0, (t) => t);

/** A word landing on a key press: overshoots a little, then settles. */
export const pop = (frame: number, at: number): { opacity: number; scale: number; translate: string } => ({
  opacity: ramp(frame, at, at + 5, 0, 1, (t) => t),
  scale: interpolate(frame, [at, at + 5, at + 13], [0.7, 1.05, 1], { ...clamp, easing: expo }),
  translate: `0px ${ramp(frame, at, at + 14, 34, 0)}px`,
});

/** Beat phase at 120 BPM, 30 fps: a hit every 15 frames, decaying fast. */
export const beatPulse = (frame: number, beat = 15): number => Math.exp(-(frame % beat) / 3.5);
