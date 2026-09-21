import React from "react";
import { MicIcon } from "../components/Icons";
import { alpha } from "../lib/util";
import { StatusBar } from "./StatusBar";

type Props = {
  accent?: string;
  pulse: number;
  listening: number;
  heard: string;
  answer: number;
};

// Voice assistant: on-device Whisper.cpp speech recognition, Piper speech output.
// "15 percent of 240" is one of the command types CommandProcessor handles (tryPercentage).
export const AssistantScreen: React.FC<Props> = ({ accent = "#64D2FF", pulse, listening, heard, answer }) => (
  <div
    style={{
      position: "absolute",
      inset: 0,
      direction: "rtl",
      color: "#FFFFFF",
      background: `radial-gradient(60% 40% at 50% 42%, ${alpha(accent, 0.22 * listening + 0.06)} 0%, rgba(0,0,0,0) 70%), #000000`,
    }}
  >
    <StatusBar />
    <div style={{ textAlign: "center", fontSize: 34, fontWeight: 700, marginTop: 26 }}>עוזר קולי</div>

    <div style={{ position: "absolute", left: 210, top: 300, width: 220, height: 220 }}>
      {[0, 1, 2].map((i) => {
        const ph = (pulse + i / 3) % 1;
        return (
          <div
            key={i}
            style={{
              position: "absolute",
              inset: 0,
              borderRadius: "50%",
              border: `3px solid ${accent}`,
              scale: `${1 + ph * 0.9}`,
              opacity: (1 - ph) * 0.55 * listening,
            }}
          />
        );
      })}
      <div
        style={{
          position: "absolute",
          inset: 0,
          borderRadius: "50%",
          background: accent,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          boxShadow: `0 0 60px ${alpha(accent, 0.5)}`,
        }}
      >
        <MicIcon size={96} color="#101012" />
      </div>
    </div>

    <div style={{ position: "absolute", top: 580, left: 0, right: 0, textAlign: "center", fontSize: 30, color: "#B0B0B0", opacity: listening }}>
      מקשיב…
    </div>
    <div style={{ position: "absolute", top: 636, left: 40, right: 40, textAlign: "center", fontSize: 38, color: "rgba(255,255,255,0.75)" }}>
      {heard}
    </div>

    <div
      style={{
        position: "absolute",
        top: 740,
        left: 40,
        right: 40,
        borderRadius: 44,
        background: "#1C1C1E",
        border: "2px solid rgba(255,255,255,0.10)",
        padding: "26px 36px",
        display: "flex",
        alignItems: "center",
        gap: 28,
        opacity: answer,
        translate: `0px ${(1 - answer) * 40}px`,
      }}
    >
      <span style={{ fontSize: 110, fontWeight: 700, lineHeight: 1, color: accent }}>36</span>
      <span style={{ fontSize: 32, fontWeight: 500, direction: "rtl" }}>15% מתוך 240</span>
    </div>
  </div>
);
