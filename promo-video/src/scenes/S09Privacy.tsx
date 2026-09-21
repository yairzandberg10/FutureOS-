import React from "react";
import { AbsoluteFill, Easing, Interactive, interpolate, useCurrentFrame } from "remotion";
import { heebo } from "../fonts";

// Facts from the repo: no Google Play Services on the device, on-device
// Whisper.cpp speech recognition in Assistant, no INTERNET permission in Music.
const PILLS = ["בלי שירותי Google", "זיהוי דיבור על המכשיר", "מוזיקה בלי אינטרנט"];

export const S09Privacy: React.FC = () => {
  const frame = useCurrentFrame();
  return (
    <AbsoluteFill
      style={{
        backgroundColor: "#000000",
        fontFamily: heebo,
        direction: "rtl",
        justifyContent: "center",
        alignItems: "center",
      }}
    >
      <Interactive.Div
        name="Line 1"
        style={{
          color: "#FFFFFF",
          fontSize: 160,
          fontWeight: 800,
          lineHeight: 1.15,
          letterSpacing: -3,
          opacity: interpolate(frame, [6, 26], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          translate: interpolate(frame, [6, 36], ["0px 50px", "0px 0px"], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
        }}
      >
        הפרטיות שלכם.
      </Interactive.Div>
      <Interactive.Div
        name="Line 2"
        style={{
          fontSize: 160,
          fontWeight: 800,
          lineHeight: 1.15,
          letterSpacing: -3,
          paddingBottom: 12,
          backgroundImage: "linear-gradient(90deg, #FF6B6B 0%, #FFD60A 100%)",
          backgroundClip: "text",
          WebkitBackgroundClip: "text",
          color: "transparent",
          opacity: interpolate(frame, [28, 48], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          translate: interpolate(frame, [28, 58], ["0px 50px", "0px 0px"], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
        }}
      >
        קודם כל.
      </Interactive.Div>
      <div style={{ display: "flex", gap: 24, marginTop: 56, flexWrap: "wrap", justifyContent: "center", maxWidth: 1620 }}>
        {PILLS.map((label, i) => (
          <div
            key={label}
            style={{
              fontSize: 48,
              fontWeight: 500,
              color: "#FFFFFF",
              padding: "16px 36px",
              borderRadius: 999,
              background: "rgba(255,255,255,0.06)",
              border: "2px solid rgba(255,255,255,0.18)",
              opacity: interpolate(frame, [56 + i * 10, 72 + i * 10], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
            }}
          >
            {label}
          </div>
        ))}
      </div>
    </AbsoluteFill>
  );
};
