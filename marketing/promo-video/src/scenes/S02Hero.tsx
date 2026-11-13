import React from "react";
import { AbsoluteFill, Easing, Interactive, interpolate, useCurrentFrame } from "remotion";
import { Phone } from "../components/Phone";
import { heebo } from "../fonts";
import { LockScreen } from "../screens/LockScreen";

export const S02Hero: React.FC = () => {
  const frame = useCurrentFrame();
  return (
    <AbsoluteFill style={{ backgroundColor: "#000000", fontFamily: heebo, direction: "rtl" }}>
      <Interactive.Div
        name="Glow"
        style={{
          position: "absolute",
          inset: 0,
          background: "radial-gradient(30% 50% at 28% 55%, rgba(100,210,255,0.24) 0%, rgba(0,0,0,0) 70%)",
          opacity: interpolate(frame, [10, 60], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
        }}
      />
      <Interactive.Div
        name="Phone"
        style={{
          position: "absolute",
          left: 360,
          top: 100,
          translate: interpolate(frame, [0, 50], ["0px 860px", "0px 0px"], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
          rotate: interpolate(frame, [0, 50], ["-8deg", "0deg"], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
          scale: interpolate(frame, [0, 165], [0.97, 1.03], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
        }}
      >
        <Phone height={880}>
          <LockScreen />
        </Phone>
      </Interactive.Div>
      <div
        style={{
          position: "absolute",
          right: 150,
          top: 0,
          bottom: 0,
          width: 960,
          display: "flex",
          flexDirection: "column",
          justifyContent: "center",
          gap: 20,
        }}
      >
        <Interactive.Div
          name="Title"
          style={{
            color: "#FFFFFF",
            fontSize: 210,
            fontWeight: 800,
            lineHeight: 1,
            letterSpacing: -6,
            opacity: interpolate(frame, [30, 55], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
            translate: interpolate(frame, [30, 62], ["0px 40px", "0px 0px"], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
              easing: Easing.bezier(0.16, 1, 0.3, 1),
            }),
          }}
        >
          FutureOS
        </Interactive.Div>
        <Interactive.Div
          name="Subtitle"
          style={{
            color: "#B0B0B0",
            fontSize: 80,
            fontWeight: 400,
            lineHeight: 1.2,
            opacity: interpolate(frame, [50, 74], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
            translate: interpolate(frame, [50, 82], ["0px 40px", "0px 0px"], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
              easing: Easing.bezier(0.16, 1, 0.3, 1),
            }),
          }}
        >
          מערכת הפעלה שלמה
          <br />
          לטלפון מקשים.
        </Interactive.Div>
      </div>
    </AbsoluteFill>
  );
};
