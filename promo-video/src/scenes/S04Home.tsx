import React from "react";
import { AbsoluteFill, Easing, Interactive, interpolate, useCurrentFrame } from "remotion";
import { Phone } from "../components/Phone";
import { heebo } from "../fonts";
import { activePress, Press } from "../lib/util";
import { HomeScreen } from "../screens/HomeScreen";

// Focus walks the grid: left, left, down, down, right, then OK.
const PRESSES: Press[] = [
  { at: 24, key: "left" },
  { at: 50, key: "left" },
  { at: 76, key: "down" },
  { at: 102, key: "down" },
  { at: 128, key: "right" },
  { at: 150, key: "ok" },
];

export const S04Home: React.FC = () => {
  const frame = useCurrentFrame();
  const { key, t } = activePress(PRESSES, frame);
  const step = Easing.bezier(0.2, 0, 0, 1);
  const ringCol = interpolate(frame, [24, 32, 50, 58, 128, 136], [0, 1, 1, 2, 2, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: step,
  });
  const ringRow = interpolate(frame, [76, 84, 102, 110], [0, 1, 1, 2], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: step,
  });
  const ringOpacity = interpolate(frame, [8, 18], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const pop = interpolate(frame, [150, 156, 172], [0, 1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });

  return (
    <AbsoluteFill style={{ backgroundColor: "#000000", fontFamily: heebo, direction: "rtl" }}>
      <AbsoluteFill style={{ background: "radial-gradient(28% 48% at 27% 52%, rgba(100,210,255,0.18) 0%, rgba(0,0,0,0) 70%)" }} />
      <Interactive.Div
        name="Phone"
        style={{
          position: "absolute",
          left: 330,
          top: 80,
          scale: interpolate(frame, [0, 180], [1, 1.04], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
        }}
      >
        <Phone height={920} pressed={key} pressT={t}>
          <HomeScreen ringCol={ringCol} ringRow={ringRow} ringOpacity={ringOpacity} pop={pop} />
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
          gap: 24,
        }}
      >
        <Interactive.Div
          name="Title"
          style={{
            color: "#FFFFFF",
            fontSize: 150,
            fontWeight: 800,
            lineHeight: 1.1,
            letterSpacing: -3,
            opacity: interpolate(frame, [10, 30], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
            translate: interpolate(frame, [10, 40], ["0px 40px", "0px 0px"], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
              easing: Easing.bezier(0.16, 1, 0.3, 1),
            }),
          }}
        >
          28 אפליקציות.
        </Interactive.Div>
        <Interactive.Div
          name="Subtitle"
          style={{
            color: "#B0B0B0",
            fontSize: 80,
            fontWeight: 400,
            lineHeight: 1.2,
            opacity: interpolate(frame, [30, 52], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          }}
        >
          כל אחת נבנתה מחדש
          <br />
          לעבודה עם מקשים.
        </Interactive.Div>
      </div>
    </AbsoluteFill>
  );
};
