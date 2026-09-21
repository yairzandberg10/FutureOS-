import React from "react";
import { AbsoluteFill, Easing, Interactive, interpolate, useCurrentFrame } from "remotion";
import { Phone } from "../components/Phone";
import { heebo } from "../fonts";
import { activePress, Press } from "../lib/util";
import { HomeScreen } from "../screens/HomeScreen";

const PRESSES: Press[] = [
  { at: 16, key: "5" },
  { at: 34, key: "2" },
  { at: 52, key: "ok" },
  { at: 70, key: "8" },
  { at: 88, key: "down" },
  { at: 106, key: "0" },
  { at: 124, key: "#" },
];

export const S03Keys: React.FC = () => {
  const frame = useCurrentFrame();
  const { key, t } = activePress(PRESSES, frame);
  return (
    <AbsoluteFill style={{ backgroundColor: "#000000", fontFamily: heebo, direction: "rtl" }}>
      <Interactive.Div
        name="Keypad close-up"
        style={{
          position: "absolute",
          left: 110,
          top: -1180,
          rotate: "-5deg",
          translate: interpolate(frame, [0, 150], ["0px 40px", "0px -40px"], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
          scale: interpolate(frame, [0, 30], [1.08, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
        }}
      >
        <Phone height={2200} pressed={key} pressT={t} accent="#64D2FF">
          <HomeScreen />
        </Phone>
      </Interactive.Div>
      <AbsoluteFill
        style={{
          background:
            "linear-gradient(90deg, rgba(0,0,0,0) 50%, rgba(0,0,0,0.9) 62%, #000000 100%), linear-gradient(180deg, #000000 0%, rgba(0,0,0,0) 14%, rgba(0,0,0,0) 86%, #000000 100%)",
        }}
      />
      <div
        style={{
          position: "absolute",
          right: 150,
          top: 0,
          bottom: 0,
          width: 820,
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
          כל פעולה.
        </Interactive.Div>
        <Interactive.Div
          name="Title 2"
          style={{
            fontSize: 160,
            fontWeight: 800,
            lineHeight: 1.15,
            letterSpacing: -3,
            paddingBottom: 10,
            backgroundImage: "linear-gradient(90deg, #64D2FF 0%, #32D74B 100%)",
            backgroundClip: "text",
            WebkitBackgroundClip: "text",
            color: "transparent",
            opacity: interpolate(frame, [26, 46], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          }}
        >
          במקש אחד.
        </Interactive.Div>
        <Interactive.Div
          name="Subtitle"
          style={{
            color: "#B0B0B0",
            fontSize: 78,
            fontWeight: 400,
            lineHeight: 1.2,
            marginTop: 24,
            opacity: interpolate(frame, [50, 72], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          }}
        >
          חצים, OK
          <br />
          וקיצורי מספרים.
        </Interactive.Div>
      </div>
    </AbsoluteFill>
  );
};
