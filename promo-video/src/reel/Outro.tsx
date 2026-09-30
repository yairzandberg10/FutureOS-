import React from "react";
import { AbsoluteFill, Easing, interpolate, useCurrentFrame } from "remotion";
import { Phone } from "../components/Phone";
import { LockScreen } from "../screens/LockScreen";
import { ACCENT, beatPulse, GREEN, ramp } from "./ease";

// The ftr mark, vectorised from logo-ftr.png (units of the 104 x 112 PNG), as in motion-futureos-launch.
const LOGO = {
  w: 104,
  h: 112,
  stroke: 10,
  color: "#FF3434",
  strokes: [
    "M5 107V23.5A16.5 16.5 0 0 1 38 23.5V28",
    "M38 35V90.48A16.5 16.5 0 0 0 71 90.48V85.8",
    "M71 80V0",
    "M71 40A33 33 0 0 1 104 7",
    "M10 58H27",
    "M43 58H60",
  ],
};
const H = 1250;

export const Outro: React.FC = () => {
  const frame = useCurrentFrame();
  const iris = interpolate(frame, [0, 24], [0, 1600], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: Easing.bezier(0.6, 0, 0.3, 1),
  });
  const phoneIn = ramp(frame, 12, 56);
  const fadeOut = ramp(frame, 104, 115, 1, 0, (t) => t);
  const sheen = ramp(frame, 44, 110, 100, 0, (t) => t);
  const LS = 2.1;

  return (
    <AbsoluteFill style={{ clipPath: `circle(${iris}px at 50% 50%)`, backgroundColor: "#000000", opacity: fadeOut }}>
      <AbsoluteFill
        style={{
          background: `radial-gradient(60% 30% at 50% 66%, rgba(100,210,255,${0.26 + 0.08 * beatPulse(frame + 5)}) 0%, rgba(0,0,0,0) 70%)`,
        }}
      />
      <div
        style={{
          position: "absolute",
          left: (1080 - (61 / 151) * H) / 2,
          top: 1040 + (1 - phoneIn) * 900,
        }}
      >
        <Phone height={H}>
          <LockScreen />
        </Phone>
      </div>
      <AbsoluteFill
        style={{
          background: "linear-gradient(180deg, #000000 0px, #000000 560px, rgba(0,0,0,0) 900px)",
        }}
      />

      <svg
        width={LOGO.w * LS}
        height={LOGO.h * LS}
        viewBox={`0 0 ${LOGO.w} ${LOGO.h}`}
        style={{
          position: "absolute",
          left: (1080 - LOGO.w * LS) / 2,
          top: 270,
          overflow: "visible",
          scale: `${1 + 0.012 * beatPulse(frame + 5)}`,
        }}
      >
        {LOGO.strokes.map((d, i) => (
          <path
            key={i}
            d={d}
            pathLength={1}
            fill="none"
            stroke={LOGO.color}
            strokeWidth={LOGO.stroke}
            strokeLinecap="butt"
            strokeDasharray="1 1.02"
            strokeDashoffset={1 - ramp(frame, 14 + i * 4, 40 + i * 4, 0, 1, Easing.bezier(0.45, 0, 0.15, 1))}
          />
        ))}
      </svg>

      <div
        style={{
          position: "absolute",
          top: 540,
          left: 0,
          right: 0,
          textAlign: "center",
          direction: "ltr",
          fontSize: 200,
          fontWeight: 800,
          lineHeight: 1.1,
          letterSpacing: -7,
          paddingBottom: 12,
          backgroundImage: "linear-gradient(90deg, #FFFFFF 0%, #FFFFFF 35%, #64D2FF 50%, #FFFFFF 65%, #FFFFFF 100%)",
          backgroundSize: "300% 100%",
          backgroundPosition: `${sheen}% 0%`,
          backgroundClip: "text",
          WebkitBackgroundClip: "text",
          color: "transparent",
          opacity: ramp(frame, 34, 52, 0, 1, (t) => t),
          scale: `${interpolate(frame, [34, 80], [0.94, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" })}`,
        }}
      >
        FutureOS
      </div>

      {[
        { t: "בלי מסך מגע.", at: 50, grad: false },
        { t: "בלי פשרות.", at: 62, grad: true },
      ].map((l, i) => (
        <div
          key={l.t}
          style={{
            position: "absolute",
            top: 770 + i * 100,
            left: 0,
            right: 0,
            textAlign: "center",
            fontSize: 84,
            fontWeight: 600,
            color: l.grad ? "transparent" : "#E4E4E9",
            ...(l.grad
              ? {
                  backgroundImage: `linear-gradient(90deg, ${ACCENT} 0%, ${GREEN} 100%)`,
                  backgroundClip: "text",
                  WebkitBackgroundClip: "text",
                }
              : {}),
            opacity: ramp(frame, l.at, l.at + 14, 0, 1, (t) => t),
            translate: `0px ${ramp(frame, l.at, l.at + 18, 26, 0)}px`,
          }}
        >
          {l.t}
        </div>
      ))}
    </AbsoluteFill>
  );
};
