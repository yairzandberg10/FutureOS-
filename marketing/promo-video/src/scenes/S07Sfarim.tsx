import React from "react";
import { AbsoluteFill, Easing, Interactive, interpolate, useCurrentFrame } from "remotion";
import { Phone } from "../components/Phone";
import { heebo } from "../fonts";
import { activePress, Press } from "../lib/util";
import { SfarimScreen } from "../screens/SfarimScreen";

const PRESSES: Press[] = [
  { at: 30, key: "down" },
  { at: 60, key: "down" },
  { at: 90, key: "down" },
];

const PILLS: Array<[string, string]> = [
  ["לוח שנה עברי", "#FFD60A"],
  ["דף יומי", "#64D2FF"],
  ["זמני היום", "#32D74B"],
];

export const S07Sfarim: React.FC = () => {
  const frame = useCurrentFrame();
  const { key, t } = activePress(PRESSES, frame);
  const focus = interpolate(frame, [30, 38, 60, 68, 90, 98], [0, 1, 1, 2, 2, 3], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: Easing.bezier(0.2, 0, 0, 1),
  });

  return (
    <AbsoluteFill style={{ backgroundColor: "#000000", fontFamily: heebo, direction: "rtl" }}>
      <AbsoluteFill style={{ background: "radial-gradient(28% 48% at 27% 52%, rgba(255,214,10,0.14) 0%, rgba(0,0,0,0) 70%)" }} />
      <Interactive.Div
        name="Phone"
        style={{
          position: "absolute",
          left: 330,
          top: 80,
          scale: interpolate(frame, [0, 165], [1, 1.04], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
        }}
      >
        <Phone height={920} pressed={key} pressT={t} accent="#FFD60A">
          <SfarimScreen focus={focus} accent="#FFD60A" />
        </Phone>
      </Interactive.Div>
      <div
        style={{
          position: "absolute",
          right: 150,
          top: 0,
          bottom: 0,
          width: 1000,
          display: "flex",
          flexDirection: "column",
          justifyContent: "center",
        }}
      >
        <Interactive.Div
          name="Title"
          style={{
            color: "#FFFFFF",
            fontSize: 160,
            fontWeight: 800,
            lineHeight: 1.1,
            letterSpacing: -3,
            opacity: interpolate(frame, [6, 26], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          }}
        >
          6,211 ספרים.
        </Interactive.Div>
        <Interactive.Div
          name="Title 2"
          style={{
            fontSize: 160,
            fontWeight: 800,
            lineHeight: 1.15,
            letterSpacing: -3,
            paddingBottom: 10,
            backgroundImage: "linear-gradient(90deg, #FFD60A 0%, #FF9F0A 100%)",
            backgroundClip: "text",
            WebkitBackgroundClip: "text",
            color: "transparent",
            opacity: interpolate(frame, [24, 44], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          }}
        >
          בכיס.
        </Interactive.Div>
        <Interactive.Div
          name="Subtitle"
          style={{
            color: "#B0B0B0",
            fontSize: 78,
            fontWeight: 400,
            lineHeight: 1.2,
            marginTop: 16,
            opacity: interpolate(frame, [46, 68], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          }}
        >
          ספריית ספרי קודש מלאה,
          <br />
          בלי חיבור לרשת.
        </Interactive.Div>
        <div style={{ display: "flex", gap: 20, marginTop: 44, flexWrap: "wrap" }}>
          {PILLS.map(([label, color], i) => (
            <div
              key={label}
              style={{
                display: "flex",
                alignItems: "center",
                gap: 14,
                fontSize: 44,
                fontWeight: 500,
                color: "#FFFFFF",
                padding: "14px 28px",
                borderRadius: 999,
                background: "rgba(255,255,255,0.06)",
                border: "2px solid rgba(255,255,255,0.18)",
                opacity: interpolate(frame, [80 + i * 10, 96 + i * 10], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
                translate: `0px ${interpolate(frame, [80 + i * 10, 100 + i * 10], [30, 0], {
                  extrapolateLeft: "clamp",
                  extrapolateRight: "clamp",
                  easing: Easing.bezier(0.16, 1, 0.3, 1),
                })}px`,
              }}
            >
              <span style={{ width: 22, height: 22, borderRadius: 11, background: color }} />
              {label}
            </div>
          ))}
        </div>
      </div>
    </AbsoluteFill>
  );
};
