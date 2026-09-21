import React from "react";
import { ChevronIcon, SendIcon } from "../components/Icons";
import { alpha } from "../lib/util";
import { StatusBar } from "./StatusBar";

// Conversation screen with the T9 keyboard panel from design/keyboard-panel/Panel.dc.html
// (state 1, typing and prediction). Panel values are already device pixels.

type Props = {
  accent?: string;
  committed: string;
  composing: string;
  digits: string;
  candidates: string[];
  sent: number;
};

const Legend: React.FC<{ k: string; label: string; dashed?: boolean }> = ({ k, label, dashed = false }) => (
  <span style={{ display: "flex", alignItems: "center", gap: 8 }}>
    <span
      style={{
        minWidth: 40,
        height: 40,
        borderRadius: 12,
        background: dashed ? "none" : "#2C2C2E",
        border: dashed ? "2px dashed #3A3A3C" : "none",
        color: "#FFFFFF",
        fontSize: 22,
        fontWeight: 500,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
      }}
    >
      {k}
    </span>
    <span style={{ fontSize: 21, color: "#B0B0B0" }}>{label}</span>
  </span>
);

export const MessagesScreen: React.FC<Props> = ({
  accent = "#64D2FF",
  committed,
  composing,
  digits,
  candidates,
  sent,
}) => (
  <div style={{ position: "absolute", inset: 0, direction: "rtl", color: "#FFFFFF", background: "#000000" }}>
    <StatusBar />
    <div
      style={{
        height: 104,
        display: "flex",
        alignItems: "center",
        gap: 20,
        padding: "0 26px",
        borderBottom: "2px solid rgba(255,255,255,0.10)",
      }}
    >
      <ChevronIcon size={40} color="#FFFFFF" />
      <div
        style={{
          width: 68,
          height: 68,
          borderRadius: 34,
          background: "#FFD60A",
          color: "#101012",
          fontSize: 34,
          fontWeight: 700,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
        }}
      >
        א
      </div>
      <div style={{ display: "flex", flexDirection: "column" }}>
        <span style={{ fontSize: 36, fontWeight: 700, lineHeight: 1.1 }}>אמא</span>
        <span style={{ fontSize: 24, color: "#B0B0B0" }}>נייד</span>
      </div>
    </div>

    <div style={{ padding: "30px 26px", display: "flex", flexDirection: "column", gap: 18 }}>
      <div style={{ alignSelf: "flex-start", maxWidth: 460, background: "#2C2C2E", borderRadius: 36, padding: "18px 28px", fontSize: 32 }}>
        מתי אתה מגיע הביתה?
      </div>
      <div
        style={{
          alignSelf: "flex-end",
          maxWidth: 460,
          background: accent,
          color: "#101012",
          borderRadius: 36,
          padding: "18px 28px",
          fontSize: 32,
          fontWeight: 500,
          opacity: sent,
          translate: `0px ${(1 - sent) * 30}px`,
        }}
      >
        אני בדרך
      </div>
    </div>

    <div
      style={{
        position: "absolute",
        left: 24,
        right: 24,
        bottom: 290,
        height: 88,
        borderRadius: 44,
        background: "#1C1C1E",
        border: "2px solid rgba(255,255,255,0.10)",
        display: "flex",
        alignItems: "center",
        padding: "0 30px",
        fontSize: 34,
      }}
    >
      {committed || composing ? (
        <>
          <span style={{ whiteSpace: "pre" }}>{committed}</span>
          <span style={{ textDecoration: "underline", textDecorationColor: accent, textDecorationThickness: 3, textUnderlineOffset: 8 }}>
            {composing}
          </span>
          <span style={{ width: 3, height: 42, background: accent, marginInlineStart: 4 }} />
        </>
      ) : (
        <span style={{ color: "rgba(255,255,255,0.4)" }}>הודעה</span>
      )}
      <span style={{ flexGrow: 1 }} />
      <SendIcon size={40} color={alpha(accent, 0.9)} />
    </div>

    <div
      style={{
        position: "absolute",
        left: 0,
        right: 0,
        bottom: 0,
        background: "#1C1C1E",
        borderTop: "2px solid rgba(255,255,255,0.10)",
        borderRadius: "32px 32px 0 0",
        padding: "18px 0 22px",
        display: "flex",
        flexDirection: "column",
        gap: 14,
      }}
    >
      <div style={{ display: "flex", alignItems: "center", gap: 12, padding: "0 32px", height: 44 }}>
        <span
          style={{
            background: "rgba(255,255,255,0.16)",
            border: "2px solid #FFFFFF",
            borderRadius: 44,
            padding: "8px 20px",
            fontSize: 24,
            fontWeight: 700,
            lineHeight: 1,
          }}
        >
          עב
        </span>
        <span style={{ fontSize: 22, color: "#B0B0B0", lineHeight: 1 }}>ניבוי</span>
        <span style={{ flexGrow: 1 }} />
        <span style={{ fontSize: 22, color: "rgba(255,255,255,0.42)", letterSpacing: 3, direction: "ltr" }}>
          {digits.split("").join(" ")}
        </span>
        {candidates.length > 0 ? (
          <span style={{ fontSize: 22, color: "#B0B0B0", background: "#2C2C2E", borderRadius: 16, padding: "4px 12px", direction: "ltr" }}>
            1/{candidates.length}
          </span>
        ) : null}
      </div>
      <div style={{ height: 84, display: "flex", alignItems: "center", gap: 16, padding: "0 32px" }}>
        {candidates.map((c, i) =>
          i === 0 ? (
            <span key={c} style={{ borderRadius: 24, padding: "14px 30px", fontSize: 32, fontWeight: 700, lineHeight: 1.2, background: accent, color: "#101012" }}>
              {c}
            </span>
          ) : (
            <span key={c} style={{ borderRadius: 24, padding: "12px 26px", fontSize: 30, lineHeight: 1.2, background: "#2C2C2E", color: "#FFFFFF" }}>
              {c}
            </span>
          ),
        )}
      </div>
      <div style={{ display: "flex", alignItems: "center", gap: 18, padding: "14px 32px 0", borderTop: "2px solid rgba(255,255,255,0.10)" }}>
        <Legend k="#" label="שפה" />
        <Legend k="*" label="פיסוק" />
        <Legend k="0" label="רווח" />
        <Legend k="↔" label="מועמדות" />
        <Legend k="0" label="קול" dashed />
      </div>
    </div>
  </div>
);
