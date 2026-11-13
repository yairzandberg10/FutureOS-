import React from "react";
import { Img, staticFile } from "remotion";
import { alpha } from "../lib/util";
import { StatusBar } from "./StatusBar";
import { wallpaper } from "./wallpaper";

// FutureLauncher: 4x4 grid, RTL (column 0 is the rightmost).
const APPS: Array<[string, string]> = [
  ["dialer", "טלפון"],
  ["messages", "הודעות"],
  ["contact", "אנשי קשר"],
  ["camera", "מצלמה"],
  ["gallery", "גלריה"],
  ["music", "מוזיקה"],
  ["calendar", "לוח שנה"],
  ["clock", "שעון"],
  ["sfarim", "בלכתך בדרך"],
  ["navigation", "ניווט"],
  ["notes", "פתקים"],
  ["assistant", "עוזר קולי"],
  ["fitness", "כושר"],
  ["calculator", "מחשבון"],
  ["tools", "כלים"],
  ["settings", "הגדרות"],
];

type Props = {
  accent?: string;
  ringCol?: number;
  ringRow?: number;
  ringOpacity?: number;
  pop?: number;
};

export const HomeScreen: React.FC<Props> = ({
  accent = "#64D2FF",
  ringCol = 0,
  ringRow = 0,
  ringOpacity = 0,
  pop = 0,
}) => {
  const focusCol = Math.round(ringCol);
  const focusRow = Math.round(ringRow);
  return (
    <div style={{ position: "absolute", inset: 0, direction: "rtl", color: "#FFFFFF", background: wallpaper(accent) }}>
      <div style={{ position: "absolute", inset: 0, background: "rgba(0,0,0,0.35)" }} />
      <div style={{ position: "relative" }}>
        <StatusBar />
        <div style={{ display: "flex", flexDirection: "column", alignItems: "center", marginTop: 30 }}>
          <div style={{ fontSize: 100, fontWeight: 300, lineHeight: 1, direction: "ltr" }}>09:41</div>
          <div style={{ fontSize: 30, fontWeight: 300, opacity: 0.85, marginTop: 8 }}>יום שני, 14 בספטמבר</div>
        </div>
      </div>

      <div
        style={{
          position: "absolute",
          top: 262 + ringRow * 170 + 4,
          right: 16 + ringCol * 152 + 6,
          width: 140,
          height: 162,
          borderRadius: 32,
          background: alpha(accent, 0.16),
          border: `4px solid ${alpha(accent, 0.95)}`,
          opacity: ringOpacity,
          scale: `${1 + 0.05 * pop}`,
        }}
      />

      {APPS.map(([icon, label], i) => {
        const col = i % 4;
        const row = Math.floor(i / 4);
        const focused = ringOpacity > 0 && col === focusCol && row === focusRow;
        return (
          <div
            key={icon}
            style={{
              position: "absolute",
              top: 262 + row * 170,
              right: 16 + col * 152,
              width: 152,
              height: 170,
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              justifyContent: "center",
              gap: 10,
              scale: focused ? `${1.06 + 0.1 * pop}` : "1",
            }}
          >
            <Img src={staticFile(`icons/${icon}.webp`)} style={{ width: 96, height: 96 }} />
            <span style={{ fontSize: 24, fontWeight: 500, whiteSpace: "nowrap", textShadow: "0 1px 4px rgba(0,0,0,0.6)" }}>
              {label}
            </span>
          </div>
        );
      })}
    </div>
  );
};
