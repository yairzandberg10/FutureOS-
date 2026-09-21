import React from "react";
import { AbsoluteFill, Easing, Interactive, interpolate, useCurrentFrame } from "remotion";
import { Phone } from "../components/Phone";
import { heebo } from "../fonts";
import { activePress, stepAt } from "../lib/util";
import { MessagesScreen } from "../screens/MessagesScreen";

// Typing "אני בדרך" with T9DigitMap.HEBREW: 3=אבג 4=מנ 5=יכל 2=דהו 7=רשת, 0 = space.
type Step = { at: number; key: string; digits: string; cands: string[]; committed: string };

const STEPS: Step[] = [
  { at: 20, key: "3", digits: "3", cands: ["א", "ב", "ג"], committed: "" },
  { at: 36, key: "4", digits: "34", cands: ["אנ", "במ", "גמ"], committed: "" },
  { at: 52, key: "5", digits: "345", cands: ["אני", "בני", "גמל"], committed: "" },
  { at: 74, key: "0", digits: "", cands: [], committed: "אני " },
  { at: 92, key: "3", digits: "3", cands: ["א", "ב", "ג"], committed: "אני " },
  { at: 106, key: "2", digits: "32", cands: ["בד", "או", "גו"], committed: "אני " },
  { at: 120, key: "7", digits: "327", cands: ["בדר", "אור", "גור"], committed: "אני " },
  { at: 134, key: "5", digits: "3275", cands: ["בדרך", "אורך", "גורל"], committed: "אני " },
  { at: 158, key: "0", digits: "", cands: [], committed: "אני בדרך" },
  { at: 178, key: "ok", digits: "", cands: [], committed: "" },
];

export const S05Typing: React.FC = () => {
  const frame = useCurrentFrame();
  const { key, t } = activePress(STEPS, frame);
  const step = stepAt(STEPS, frame);
  const sent = interpolate(frame, [180, 194], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: Easing.bezier(0.16, 1, 0.3, 1),
  });

  return (
    <AbsoluteFill style={{ backgroundColor: "#000000", fontFamily: heebo, direction: "rtl" }}>
      <AbsoluteFill style={{ background: "radial-gradient(28% 48% at 27% 52%, rgba(50,215,75,0.16) 0%, rgba(0,0,0,0) 70%)" }} />
      <Interactive.Div
        name="Phone"
        style={{
          position: "absolute",
          left: 330,
          top: 80,
          scale: interpolate(frame, [0, 225], [1, 1.04], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
        }}
      >
        <Phone height={920} pressed={key} pressT={t}>
          <MessagesScreen
            committed={step?.committed ?? ""}
            composing={step?.cands[0] ?? ""}
            digits={step?.digits ?? ""}
            candidates={step?.cands ?? []}
            sent={sent}
          />
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
            fontSize: 128,
            fontWeight: 800,
            lineHeight: 1.1,
            letterSpacing: -3,
            opacity: interpolate(frame, [6, 26], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          }}
        >
          מקלדת T9
        </Interactive.Div>
        <Interactive.Div
          name="Title 2"
          style={{
            fontSize: 128,
            fontWeight: 800,
            lineHeight: 1.15,
            letterSpacing: -3,
            paddingBottom: 10,
            backgroundImage: "linear-gradient(90deg, #32D74B 0%, #64D2FF 100%)",
            backgroundClip: "text",
            WebkitBackgroundClip: "text",
            color: "transparent",
            opacity: interpolate(frame, [24, 44], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          }}
        >
          שמנחשת מילים.
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
          21 מצבי הקלדה.
          <br />
          החזיקו 0 כדי להכתיב.
        </Interactive.Div>
      </div>
    </AbsoluteFill>
  );
};
