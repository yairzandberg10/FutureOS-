import React from "react";
import { AbsoluteFill, Easing, Interactive, interpolate, useCurrentFrame } from "remotion";
import { heebo } from "../fonts";

export const S01Intro: React.FC = () => {
  const frame = useCurrentFrame();
  return (
    <AbsoluteFill
      style={{
        backgroundColor: "#000000",
        fontFamily: heebo,
        direction: "rtl",
        justifyContent: "center",
        alignItems: "center",
        gap: 6,
      }}
    >
      <Interactive.Div
        name="Line 1"
        style={{
          color: "#FFFFFF",
          fontSize: 170,
          fontWeight: 800,
          lineHeight: 1.15,
          letterSpacing: -3,
          opacity: interpolate(frame, [8, 30], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
          translate: interpolate(frame, [8, 40], ["0px 60px", "0px 0px"], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
        }}
      >
        בלי מסך מגע.
      </Interactive.Div>
      <Interactive.Div
        name="Line 2"
        style={{
          fontSize: 170,
          fontWeight: 800,
          lineHeight: 1.15,
          letterSpacing: -3,
          paddingBottom: 12,
          backgroundImage: "linear-gradient(90deg, #32D74B 0%, #64D2FF 50%, #FFD60A 100%)",
          backgroundClip: "text",
          WebkitBackgroundClip: "text",
          color: "transparent",
          opacity: interpolate(frame, [42, 64], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
          translate: interpolate(frame, [42, 74], ["0px 60px", "0px 0px"], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
        }}
      >
        בלי פשרות.
      </Interactive.Div>
    </AbsoluteFill>
  );
};
