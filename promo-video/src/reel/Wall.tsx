import React from "react";
import { AbsoluteFill, Easing, Img, interpolate, staticFile, useCurrentFrame } from "remotion";
import { expo, ramp } from "./ease";

// App icons, unchanged. Remote is left out: its icon file is identical to Clock's.
const ICONS = [
  "dialer", "messages", "contact", "camera", "gallery", "music", "calendar", "clock",
  "sfarim", "navigation", "notes", "assistant", "fitness", "calculator", "tools", "settings",
  "files", "keyboard", "guide", "terminal", "futureui", "futurelauncher",
];
const ROWS = [4, 5, 4, 5, 4];
const SIZE = 170;
const GAP = 34;
const TOP = 620;

export const Wall: React.FC = () => {
  const frame = useCurrentFrame();
  const iris = interpolate(frame, [0, 24], [0, 1500], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: Easing.bezier(0.6, 0, 0.3, 1),
  });

  let n = 0;
  return (
    <AbsoluteFill style={{ clipPath: `circle(${iris}px at 50% 50%)`, backgroundColor: "#F2F2F7" }}>
      <AbsoluteFill
        style={{
          background:
            "radial-gradient(45% 26% at 10% 14%, rgba(100,210,255,0.40) 0%, rgba(0,0,0,0) 70%), radial-gradient(40% 24% at 92% 12%, rgba(255,214,10,0.32) 0%, rgba(0,0,0,0) 70%), radial-gradient(45% 26% at 88% 94%, rgba(50,215,75,0.30) 0%, rgba(0,0,0,0) 70%), radial-gradient(40% 24% at 12% 92%, rgba(255,107,107,0.28) 0%, rgba(0,0,0,0) 70%)",
        }}
      />
      <div
        style={{
          position: "absolute",
          top: 250,
          left: 64,
          right: 64,
          textAlign: "center",
          color: "#000000",
          fontSize: 190,
          fontWeight: 800,
          lineHeight: 1.08,
          letterSpacing: -4,
          opacity: ramp(frame, 12, 26, 0, 1, (t) => t),
          translate: `0px ${ramp(frame, 12, 36, 40, 0)}px`,
        }}
      >
        הכל כאן.
      </div>
      <div
        style={{
          position: "absolute",
          top: 474,
          left: 64,
          right: 64,
          textAlign: "center",
          color: "#6B6B70",
          fontSize: 52,
          fontWeight: 400,
          opacity: ramp(frame, 28, 42, 0, 1, (t) => t),
        }}
      >
        טלפון. הודעות. ניווט. מוזיקה. ועוד.
      </div>

      {ROWS.map((count, row) => {
        const rowWidth = count * SIZE + (count - 1) * GAP;
        return Array.from({ length: count }, (_, col) => {
          const i = n++;
          const icon = ICONS[i];
          const start = 16 + i * 2.2;
          const p = interpolate(frame, [start, start + 8, start + 16], [0, 1.1, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: expo,
          });
          return (
            <Img
              key={icon}
              src={staticFile(`icons/${icon}.webp`)}
              style={{
                position: "absolute",
                left: (1080 - rowWidth) / 2 + col * (SIZE + GAP),
                top: TOP + row * (SIZE + GAP),
                width: SIZE,
                height: SIZE,
                opacity: Math.min(1, p * 1.4),
                scale: `${p}`,
                translate: `0px ${Math.sin((frame + i * 7) / 14) * 4}px`,
                filter: "drop-shadow(0 10px 22px rgba(0,0,0,0.16))",
              }}
            />
          );
        });
      })}
    </AbsoluteFill>
  );
};
