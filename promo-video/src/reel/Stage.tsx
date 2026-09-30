import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { Phone } from "../components/Phone";
import { HomeScreen } from "../screens/HomeScreen";
import { LockScreen } from "../screens/LockScreen";
import { MessagesScreen } from "../screens/MessagesScreen";
import { AssistantScreen } from "../screens/AssistantScreen";
import { activePress } from "../lib/util";
import { ACCENT, beatPulse, expo, ramp, smooth } from "./ease";
import timeline from "./timeline.json";

const PRESSES = timeline.presses;
const H = 1300; // phone height in stage pixels: TOP + H = 1920, the keypad bleeds off the bottom edge
const TOP = 620;
const PHONE_LEFT = (1080 - (61 / 151) * H) / 2;

// Typing "אני בדרך" with T9DigitMap.HEBREW: 3=אבג 4=מנ 5=יכל 2=דהו 7=רשת, 0 = space.
const TYPING = [
  { digits: "3", cands: ["א", "ב", "ג"], committed: "" },
  { digits: "34", cands: ["אנ", "במ", "גמ"], committed: "" },
  { digits: "345", cands: ["אני", "בני", "גמל"], committed: "" },
  { digits: "", cands: [], committed: "אני " },
  { digits: "3", cands: ["א", "ב", "ג"], committed: "אני " },
  { digits: "32", cands: ["בד", "או", "גו"], committed: "אני " },
  { digits: "327", cands: ["בדר", "אור", "גור"], committed: "אני " },
  { digits: "3275", cands: ["בדרך", "אורך", "גורל"], committed: "אני " },
  { digits: "", cands: [], committed: "אני בדרך" },
  { digits: "", cands: [], committed: "" },
];
const typingPresses = PRESSES.filter((p) => p.at >= 285 && p.at <= 420);

// Camera: the point (cx, cy) of the stage lands on (540, fy) of the frame, at scale s.
const CAM = [
  { f: 78, cx: 540, cy: 1240, s: 1.0, fy: 1240 },
  { f: 130, cx: 540, cy: 1240, s: 1.03, fy: 1240 },
  { f: 172, cx: 540, cy: 1240, s: 1.06, fy: 1240 },
  // home: the whole screen under the two-line title
  { f: 204, cx: 540, cy: 1011, s: 1.36, fy: 1130 },
  { f: 270, cx: 540, cy: 1011, s: 1.4, fy: 1130 },
  // typing: the composing bar just under the title, every key down to "0" above Instagram's bottom band
  { f: 300, cx: 540, cy: 1476, s: 1.2, fy: 1100 },
  { f: 440, cx: 540, cy: 1476, s: 1.24, fy: 1100 },
  // assistant
  { f: 470, cx: 540, cy: 1011, s: 1.34, fy: 1170 },
  { f: 565, cx: 540, cy: 1011, s: 1.4, fy: 1170 },
];
const cam = (frame: number, k: "cx" | "cy" | "s" | "fy"): number =>
  interpolate(frame, CAM.map((c) => c.f), CAM.map((c) => c[k]), {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: smooth,
  });

const Layer: React.FC<{ opacity: number; children: React.ReactNode }> = ({ opacity, children }) =>
  opacity > 0.002 ? <div style={{ position: "absolute", inset: 0, opacity }}>{children}</div> : null;

export const Stage: React.FC = () => {
  const frame = useCurrentFrame();
  if (frame < 76 || frame > 592) return null;

  const { key, t } = activePress(PRESSES, frame, 9);

  const punch = 1 + 0.012 * beatPulse(frame - 75);
  const s = cam(frame, "s") * punch;
  const cx = cam(frame, "cx");
  const cy = cam(frame, "cy");
  const fy = cam(frame, "fy");

  // Screen layers: each fades in over the previous one, which leaves once the new one is fully in.
  const homeIn = ramp(frame, 156, 168, 0, 1, (x) => x);
  const msgIn = ramp(frame, 248, 262, 0, 1, (x) => x);
  const astIn = ramp(frame, 452, 466, 0, 1, (x) => x);

  const ringOpacity = ramp(frame, 172, 184, 0, 1, (x) => x);
  const ringCol = ramp(frame, 210, 218, 0, 1);
  const ringRow = interpolate(frame, [195, 203, 225, 233], [0, 1, 1, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: expo,
  });
  const pop = interpolate(frame, [240, 246, 262], [0, 1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });

  const tStep = [...typingPresses.map((p, i) => ({ at: p.at, ...TYPING[i] }))].filter((s) => frame >= s.at).pop();
  const sent = ramp(frame, 422, 436);

  const listening = interpolate(frame, [474, 482, 540, 552], [0, 1, 1, 0.6], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });
  const HEARD = "כמה זה 15 אחוז מ-240?";
  const chars = Math.floor(interpolate(frame, [482, 530], [0, HEARD.length], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }));
  const answer = ramp(frame, 536, 550);

  const rise = ramp(frame, 78, 130);
  const greenGlow = ramp(frame, 270, 300, 0, 1, (x) => x) * (1 - ramp(frame, 440, 466, 0, 1, (x) => x));

  return (
    <AbsoluteFill>
      <AbsoluteFill
        style={{
          opacity: 0.55 * ramp(frame, 78, 110, 0, 1, (x) => x),
          backgroundImage: "radial-gradient(rgba(255,255,255,0.30) 2px, rgba(255,255,255,0) 2.6px)",
          backgroundSize: "64px 64px",
          backgroundPosition: `0px ${-frame * 0.7}px`,
          maskImage: "radial-gradient(75% 50% at 50% 55%, #000 0%, rgba(0,0,0,0) 100%)",
          WebkitMaskImage: "radial-gradient(75% 50% at 50% 55%, #000 0%, rgba(0,0,0,0) 100%)",
          scale: `${1 + (s - 1) * 0.35}`,
        }}
      />
      <AbsoluteFill
        style={{
          background: `radial-gradient(52% 30% at 50% 52%, rgba(100,210,255,${0.22 + 0.06 * beatPulse(frame - 75)}) 0%, rgba(0,0,0,0) 70%)`,
        }}
      />
      <AbsoluteFill
        style={{
          opacity: greenGlow,
          background: `radial-gradient(52% 30% at 50% 60%, rgba(50,215,75,0.20) 0%, rgba(0,0,0,0) 70%)`,
        }}
      />
      <div
        style={{
          position: "absolute",
          left: 0,
          top: 0,
          width: 1080,
          height: 1920,
          transformOrigin: "0 0",
          transform: `translate(${540 - cx * s}px, ${fy - cy * s}px) scale(${s})`,
        }}
      >
        <div
          style={{
            position: "absolute",
            left: PHONE_LEFT,
            top: TOP,
            opacity: ramp(frame, 78, 92, 0, 1, (x) => x),
            translate: `0px ${(1 - rise) * 1100}px`,
            rotate: `${(1 - rise) * -7}deg`,
          }}
        >
          <Phone height={H} pressed={key} pressT={t} accent={ACCENT}>
            {msgIn < 1 ? (
              <Layer opacity={1}>
                <LockScreen />
              </Layer>
            ) : null}
            {homeIn > 0 && msgIn < 1 ? (
              <Layer opacity={homeIn}>
                <HomeScreen ringCol={ringCol} ringRow={ringRow} ringOpacity={ringOpacity} pop={pop} />
              </Layer>
            ) : null}
            {msgIn > 0 && astIn < 1 ? (
              <Layer opacity={msgIn}>
                <MessagesScreen
                  committed={tStep?.committed ?? ""}
                  composing={tStep?.cands[0] ?? ""}
                  digits={tStep?.digits ?? ""}
                  candidates={tStep?.cands ?? []}
                  sent={sent}
                />
              </Layer>
            ) : null}
            {astIn > 0 ? (
              <Layer opacity={astIn}>
                <AssistantScreen pulse={(frame % 40) / 40} listening={listening} heard={HEARD.slice(0, chars)} answer={answer} />
              </Layer>
            ) : null}
          </Phone>
        </div>
      </div>
    </AbsoluteFill>
  );
};

