import React from "react";
import { AbsoluteFill, Easing, Img, Interactive, interpolate, staticFile, useCurrentFrame } from "remotion";
import { heebo } from "../fonts";

// App icons, unchanged. Remote is left out: its icon file is identical to Clock's.
const ICONS = [
  "dialer", "messages", "contact", "camera", "gallery", "music", "calendar", "clock",
  "sfarim", "navigation", "notes", "assistant", "fitness", "calculator", "tools", "settings",
  "files", "keyboard", "guide", "terminal", "futureui", "futurelauncher",
];

const SIZE = 150;
const GAP = 44;

export const S08Wall: React.FC = () => {
  const frame = useCurrentFrame();
  return (
    <AbsoluteFill style={{ backgroundColor: "#F2F2F7", fontFamily: heebo, direction: "rtl" }}>
      <AbsoluteFill
        style={{
          background:
            "radial-gradient(35% 45% at 12% 20%, rgba(100,210,255,0.35) 0%, rgba(0,0,0,0) 70%), radial-gradient(30% 40% at 90% 15%, rgba(255,214,10,0.30) 0%, rgba(0,0,0,0) 70%), radial-gradient(35% 45% at 85% 95%, rgba(50,215,75,0.28) 0%, rgba(0,0,0,0) 70%), radial-gradient(30% 40% at 15% 95%, rgba(255,107,107,0.25) 0%, rgba(0,0,0,0) 70%)",
        }}
      />
      <Interactive.Div
        name="Title"
        style={{
          position: "absolute",
          top: 100,
          left: 0,
          right: 0,
          textAlign: "center",
          color: "#000000",
          fontSize: 160,
          fontWeight: 800,
          letterSpacing: -3,
          opacity: interpolate(frame, [4, 22], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          translate: interpolate(frame, [4, 34], ["0px 40px", "0px 0px"], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
        }}
      >
        הכל כאן.
      </Interactive.Div>
      {ICONS.map((icon, i) => {
        const row = Math.floor(i / 8);
        const col = i % 8;
        const inRow = row < 2 ? 8 : ICONS.length - 16;
        const rowWidth = inRow * SIZE + (inRow - 1) * GAP;
        const start = 14 + i * 2.5;
        return (
          <Img
            key={icon}
            src={staticFile(`icons/${icon}.webp`)}
            style={{
              position: "absolute",
              left: (1920 - rowWidth) / 2 + col * (SIZE + GAP),
              top: 390 + row * (SIZE + GAP),
              width: SIZE,
              height: SIZE,
              opacity: interpolate(frame, [start, start + 6], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
              scale: `${interpolate(frame, [start, start + 16], [0.2, 1], {
                extrapolateLeft: "clamp",
                extrapolateRight: "clamp",
                easing: Easing.bezier(0.34, 1.56, 0.64, 1),
              })}`,
              translate: `0px ${Math.sin(frame / 18 + i) * 6}px`,
              filter: "drop-shadow(0 12px 24px rgba(0,0,0,0.14))",
            }}
          />
        );
      })}
    </AbsoluteFill>
  );
};
