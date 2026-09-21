import React from "react";
import { AbsoluteFill, Easing, Interactive, interpolate, useCurrentFrame } from "remotion";
import { Phone } from "../components/Phone";
import { heebo } from "../fonts";
import { LockScreen } from "../screens/LockScreen";

export const S11Outro: React.FC = () => {
  const frame = useCurrentFrame();
  return (
    <AbsoluteFill style={{ backgroundColor: "#000000", fontFamily: heebo, direction: "rtl" }}>
      <Interactive.Div
        name="Phone"
        style={{
          position: "absolute",
          left: 782,
          top: 100,
          opacity: interpolate(frame, [0, 45, 72], [1, 1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          scale: interpolate(frame, [0, 72], [1, 0.86], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.4, 0, 0.2, 1),
          }),
          translate: interpolate(frame, [40, 72], ["0px 0px", "0px -60px"], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      >
        <Phone height={880}>
          <LockScreen />
        </Phone>
      </Interactive.Div>
      <AbsoluteFill style={{ justifyContent: "center", alignItems: "center", gap: 10 }}>
        <div
          style={{
            fontSize: 240,
            fontWeight: 800,
            lineHeight: 1.1,
            letterSpacing: -8,
            paddingBottom: 12,
            direction: "ltr",
            backgroundImage: "linear-gradient(90deg, #FFFFFF 0%, #FFFFFF 35%, #64D2FF 50%, #FFFFFF 65%, #FFFFFF 100%)",
            backgroundSize: "300% 100%",
            backgroundPosition: `${interpolate(frame, [70, 165], [100, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" })}% 0%`,
            backgroundClip: "text",
            WebkitBackgroundClip: "text",
            color: "transparent",
            opacity: interpolate(frame, [62, 86], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
            scale: `${interpolate(frame, [62, 110], [0.94, 1], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
              easing: Easing.bezier(0.16, 1, 0.3, 1),
            })}`,
          }}
        >
          FutureOS
        </div>
        <Interactive.Div
          name="Tagline"
          style={{
            color: "#B0B0B0",
            fontSize: 80,
            fontWeight: 400,
            opacity: interpolate(frame, [84, 106], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          }}
        >
          בלי מסך מגע. בלי פשרות.
        </Interactive.Div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
