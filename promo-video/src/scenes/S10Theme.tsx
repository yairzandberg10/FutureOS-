import React from "react";
import { AbsoluteFill, Interactive, interpolate, interpolateColors, useCurrentFrame } from "remotion";
import { Phone } from "../components/Phone";
import { heebo } from "../fonts";
import { alpha } from "../lib/util";
import { ControlCenterScreen } from "../screens/ControlCenterScreen";

export const S10Theme: React.FC = () => {
  const frame = useCurrentFrame();
  // The accent is a free user choice, applied live across every app.
  const accent = interpolateColors(
    frame,
    [0, 40, 48, 88, 96, 136, 144],
    ["#FFFFFF", "#FFFFFF", "#64D2FF", "#64D2FF", "#FFD60A", "#FFD60A", "#32D74B"],
  );

  return (
    <AbsoluteFill style={{ backgroundColor: "#000000", fontFamily: heebo, direction: "rtl" }}>
      <AbsoluteFill style={{ background: `radial-gradient(32% 55% at 27% 52%, ${alpha(accent, 0.3)} 0%, rgba(0,0,0,0) 70%)` }} />
      <Interactive.Div
        name="Phone"
        style={{
          position: "absolute",
          left: 330,
          top: 80,
          scale: interpolate(frame, [0, 180], [1, 1.04], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
        }}
      >
        <Phone height={920} accent={accent}>
          <ControlCenterScreen accent={accent} />
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
            fontSize: 180,
            fontWeight: 800,
            lineHeight: 1.05,
            letterSpacing: -4,
            opacity: interpolate(frame, [6, 26], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          }}
        >
          העיצוב.
        </Interactive.Div>
        <div style={{ color: accent, fontSize: 180, fontWeight: 800, lineHeight: 1.1, letterSpacing: -4 }}>שלכם.</div>
        <Interactive.Div
          name="Subtitle"
          style={{
            color: "#B0B0B0",
            fontSize: 78,
            fontWeight: 400,
            lineHeight: 1.2,
            marginTop: 24,
            opacity: interpolate(frame, [30, 52], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          }}
        >
          מצב כהה או בהיר,
          <br />
          וצבע הדגשה לבחירתכם.
        </Interactive.Div>
      </div>
    </AbsoluteFill>
  );
};
