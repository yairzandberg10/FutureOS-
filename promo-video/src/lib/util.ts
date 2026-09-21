const rgb = (color: string): [number, number, number] => {
  if (color.startsWith("#")) {
    const h = color.slice(1);
    return [
      parseInt(h.slice(0, 2), 16),
      parseInt(h.slice(2, 4), 16),
      parseInt(h.slice(4, 6), 16),
    ];
  }
  const m = color.match(/[\d.]+/g) ?? ["255", "255", "255"];
  return [Number(m[0]), Number(m[1]), Number(m[2])];
};

/** The same color at a given opacity. Accepts #RRGGBB or rgb()/rgba(). */
export const alpha = (color: string, a: number): string => {
  const [r, g, b] = rgb(color);
  return `rgba(${r}, ${g}, ${b}, ${Math.max(0, Math.min(1, a))})`;
};

export type Press = { at: number; key: string };

/** The key held down at this frame; t fades from 1 to 0 over the press. */
export const activePress = (
  presses: Press[],
  frame: number,
  hold = 10,
): { key: string | null; t: number } => {
  let current: Press | null = null;
  for (const p of presses) {
    if (frame >= p.at && frame < p.at + hold) current = p;
  }
  if (!current) return { key: null, t: 0 };
  return { key: current.key, t: 1 - (frame - current.at) / hold };
};

/** The last step whose start frame has been reached. */
export const stepAt = <T extends { at: number }>(
  steps: T[],
  frame: number,
): T | null => {
  let current: T | null = null;
  for (const s of steps) {
    if (frame >= s.at) current = s;
  }
  return current;
};
