import React from "react";
import { AbsoluteFill, Easing, Interactive, interpolate, useCurrentFrame } from "remotion";
import { Phone } from "../components/Phone";
import { heebo } from "../fonts";
import { activePress, Press } from "../lib/util";
import { AssistantScreen } from "../screens/AssistantScreen";

// A double press of OK opens the assistant from any screen (LockScreenAccessibilityService).
const PRESSES: Press[] = [
  { at: 8, key: "ok" },
  { at: 18, key: "ok" },
];

const HEARD = "כמה זה 15 אחוז מ-240?";

export const S06Assistant: React.FC = () => {
  const frame = useCurrentFrame();
  const { key, t } = activePress(PRESSES, frame, 8);
  const listening = interpolate(frame, [26, 34, 110, 120], [0, 1, 1, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });
  const chars = Math.floor(
    interpolate(frame, [40, 100], [0, HEARD.length], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
  );
  const answer = interpolate(frame, [118, 136], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: Easing.bezier(0.16, 1, 0.3, 1),
  });

  return (
    <AbsoluteFill style={{ backgroundColor: "#000000", fontFamily: heebo, direction: "rtl" }}>
      <AbsoluteFill style={{ background: "radial-gradient(28% 48% at 73% 52%, rgba(100,210,255,0.20) 0%, rgba(0,0,0,0) 70%)" }} />
      <Interactive.Div
        name="Phone"
        style={{
          position: "absolute",
          right: 330,
          top: 80,
          scale: interpolate(frame, [0, 180], [1, 1.04], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
        }}
      >
        <Phone height={920} pressed={key} pressT={t}>
          <AssistantScreen pulse={(frame % 40) / 40} listening={listening} heard={HEARD.slice(0, chars)} answer={answer} />
        </Phone>
      </Interactive.Div>
      <div
        style={{
          position: "absolute",
          left: 150,
          top: 0,
          bottom: 0,
          width: 960,
          display: "flex",
          flexDirection: "column",
          justifyContent: "center",
        }}
      >
        <Interactive.Div
          name="Kicker"
          style={{
            color: "#64D2FF",
            fontSize: 60,
            fontWeight: 500,
            marginBottom: 12,
            opacity: interpolate(frame, [4, 20], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          }}
        >
          לחיצה כפולה על OK
        </Interactive.Div>
        <Interactive.Div
          name="Title"
          style={{
            color: "#FFFFFF",
            fontSize: 170,
            fontWeight: 800,
            lineHeight: 1.05,
            letterSpacing: -3,
            opacity: interpolate(frame, [20, 40], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          }}
        >
          שאלו.
        </Interactive.Div>
        <Interactive.Div
          name="Title 2"
          style={{
            fontSize: 130,
            fontWeight: 800,
            lineHeight: 1.2,
            letterSpacing: -2,
            paddingBottom: 10,
            backgroundImage: "linear-gradient(90deg, #64D2FF 0%, #FFD60A 100%)",
            backgroundClip: "text",
            WebkitBackgroundClip: "text",
            color: "transparent",
            opacity: interpolate(frame, [40, 60], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          }}
        >
          גם בלי אינטרנט.
        </Interactive.Div>
        <Interactive.Div
          name="Subtitle"
          style={{
            color: "#B0B0B0",
            fontSize: 78,
            fontWeight: 400,
            lineHeight: 1.2,
            marginTop: 20,
            opacity: interpolate(frame, [64, 86], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          }}
        >
          זיהוי דיבור והקראה
          <br />
          על המכשיר עצמו.
        </Interactive.Div>
      </div>
    </AbsoluteFill>
  );
};
