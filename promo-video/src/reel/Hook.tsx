import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { ACCENT, GREEN, RED, ramp } from "./ease";

// 0-90: "touch screen?" gets struck through, then "back to keys."
export const Hook: React.FC = () => {
  const frame = useCurrentFrame();
  const inT = ramp(frame, 6, 24);
  const struck = ramp(frame, 34, 46, 0, 1, (t) => t);
  const outT = ramp(frame, 48, 58, 0, 1, (t) => t);
  const newIn = ramp(frame, 58, 76);
  const zoomOut = ramp(frame, 80, 92, 0, 1, (t) => t * t);

  return (
    <AbsoluteFill>
      <AbsoluteFill
        style={{
          background: `radial-gradient(60% 34% at 50% 50%, rgba(100,210,255,${0.10 + 0.16 * newIn}) 0%, rgba(0,0,0,0) 70%)`,
        }}
      />

      <div
        style={{
          position: "absolute",
          left: 0,
          right: 0,
          top: 740,
          display: "flex",
          justifyContent: "center",
          opacity: inT * (1 - outT),
          translate: `0px ${-70 * outT}px`,
          filter: `blur(${(1 - inT) * 18 + outT * 14}px)`,
          scale: `${interpolate(inT, [0, 1], [1.14, 1])}`,
        }}
      >
        <div style={{ position: "relative", display: "inline-block" }}>
          <span
            style={{
              fontSize: 204,
              fontWeight: 800,
              letterSpacing: -4,
              color: struck > 0.5 ? "#6E6E73" : "#FFFFFF",
              lineHeight: 1.1,
            }}
          >
            מסך מגע?
          </span>
          <span
            style={{
              position: "absolute",
              right: -30,
              top: "52%",
              height: 18,
              width: `${struck * 108}%`,
              marginTop: -9,
              borderRadius: 9,
              background: RED,
              boxShadow: `0 0 40px rgba(255,107,107,0.85)`,
              rotate: "-3deg",
            }}
          />
        </div>
      </div>

      <div
        style={{
          position: "absolute",
          left: 0,
          right: 0,
          top: 640,
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          opacity: newIn * (1 - zoomOut),
          scale: `${interpolate(newIn, [0, 1], [0.78, 1]) * (1 + 0.6 * zoomOut)}`,
          filter: `blur(${zoomOut * 16}px)`,
        }}
      >
        <span style={{ fontSize: 232, fontWeight: 800, letterSpacing: -4, color: "#FFFFFF", lineHeight: 1.05 }}>נחזור</span>
        <span
          style={{
            fontSize: 232,
            fontWeight: 800,
            letterSpacing: -4,
            lineHeight: 1.12,
            paddingBottom: 12,
            backgroundImage: `linear-gradient(90deg, ${ACCENT} 0%, ${GREEN} 100%)`,
            backgroundClip: "text",
            WebkitBackgroundClip: "text",
            color: "transparent",
          }}
        >
          למקשים.
        </span>
      </div>
    </AbsoluteFill>
  );
};

