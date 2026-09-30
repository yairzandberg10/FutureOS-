import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { ACCENT, GRAY, GREEN, pop, ramp, window4 } from "./ease";

const gradient = (a: string, b: string): React.CSSProperties => ({
  backgroundImage: `linear-gradient(90deg, ${a} 0%, ${b} 100%)`,
  backgroundClip: "text",
  WebkitBackgroundClip: "text",
  color: "transparent",
  paddingBottom: 10,
});

// Screen-space titles, inside Instagram's safe area: below the top 250 px, inside 64 px of the sides.
const Block: React.FC<{ show: number; children: React.ReactNode }> = ({ show, children }) =>
  show > 0.002 ? (
    <div
      style={{
        position: "absolute",
        top: 250,
        left: 64,
        right: 64,
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        textAlign: "center",
        opacity: show,
      }}
    >
      {children}
    </div>
  ) : null;

const Word: React.FC<{ frame: number; at: number; size: number; style?: React.CSSProperties; children: React.ReactNode }> = ({
  frame,
  at,
  size,
  style,
  children,
}) => {
  const p = pop(frame, at);
  return (
    <div
      style={{
        fontSize: size,
        fontWeight: 800,
        lineHeight: 1.08,
        letterSpacing: -3,
        color: "#FFFFFF",
        opacity: p.opacity,
        scale: `${p.scale}`,
        translate: p.translate,
        ...style,
      }}
    >
      {children}
    </div>
  );
};

const Sub: React.FC<{ frame: number; at: number; size?: number; children: React.ReactNode }> = ({ frame, at, size = 58, children }) => (
  <div
    style={{
      marginTop: 18,
      fontSize: size,
      fontWeight: 400,
      color: GRAY,
      opacity: ramp(frame, at, at + 14, 0, 1, (t) => t),
      translate: `0px ${ramp(frame, at, at + 18, 24, 0)}px`,
    }}
  >
    {children}
  </div>
);

export const Captions: React.FC = () => {
  const frame = useCurrentFrame();
  return (
    <AbsoluteFill>
      {/* Darkens the top of the zoomed phone so the titles stay readable. */}
      <AbsoluteFill
        style={{
          opacity: ramp(frame, 176, 204, 0, 1, (t) => t) * ramp(frame, 556, 572, 1, 0, (t) => t),
          background:
            "linear-gradient(180deg, rgba(0,0,0,0.95) 0px, rgba(0,0,0,0.95) 460px, rgba(0,0,0,0) 650px)",
        }}
      />

      {/* Reveal */}
      <Block show={window4(frame, 94, 104, 160, 172)}>
        <Word frame={frame} at={94} size={188} style={{ direction: "ltr", letterSpacing: -6 }}>
          FutureOS
        </Word>
        <Sub frame={frame} at={112} size={60}>
          טלפון מקשים. מערכת שלמה.
        </Sub>
      </Block>

      {/* Home: arrows, then OK */}
      <Block show={window4(frame, 194, 200, 262, 274)}>
        <Word frame={frame} at={196} size={128}>
          חצים.
        </Word>
        {frame >= 240 ? (
          <Word frame={frame} at={240} size={128} style={gradient(ACCENT, GREEN)}>
            ואז OK.
          </Word>
        ) : null}
      </Block>

      {/* Typing */}
      <Block show={window4(frame, 284, 290, 436, 450)}>
        <Word frame={frame} at={286} size={112}>
          כותבים
        </Word>
        {frame >= 300 ? (
          <Word frame={frame} at={300} size={112} style={gradient(GREEN, ACCENT)}>
            עם המספרים.
          </Word>
        ) : null}
        <Sub frame={frame} at={330} size={50}>
          מקלדת T9 שמנחשת מילים.
        </Sub>
      </Block>

      {/* Assistant */}
      <Block show={window4(frame, 450, 456, 552, 566)}>
        <Word frame={frame} at={451} size={120}>
          לחיצה כפולה
        </Word>
        {frame >= 459 ? (
          <Word frame={frame} at={459} size={120} style={gradient(ACCENT, GREEN)}>
            על OK.
          </Word>
        ) : null}
        <Sub frame={frame} at={482} size={50}>
          והעוזר הקולי מקשיב.
        </Sub>
      </Block>
    </AbsoluteFill>
  );
};
