import React from "react";
import { Img, staticFile } from "remotion";
import { ChevronIcon, SearchIcon } from "../components/Icons";
import { alpha } from "../lib/util";
import { StatusBar } from "./StatusBar";

const ROWS: Array<[string, string]> = [
  ["תנ״ך", "תורה, נביאים וכתובים"],
  ["משנה", "שישה סדרי משנה"],
  ["תלמוד", "בבלי וירושלמי"],
  ["מדרש", "מדרשי חז״ל"],
  ["הלכה", "פוסקים ושו״ת"],
  ["מחשבת ישראל", "ספרי יסוד ומוסר"],
];

export const SfarimScreen: React.FC<{ accent?: string; focus: number }> = ({ accent = "#64D2FF", focus }) => {
  const focused = Math.round(focus);
  return (
    <div style={{ position: "absolute", inset: 0, direction: "rtl", color: "#FFFFFF", background: "#000000" }}>
      <StatusBar />
      <div style={{ height: 110, display: "flex", alignItems: "center", padding: "0 30px", gap: 16 }}>
        <Img src={staticFile("icons/sfarim.webp")} style={{ width: 60, height: 60 }} />
        <span style={{ fontSize: 40, fontWeight: 700 }}>בלכתך בדרך</span>
        <span style={{ flexGrow: 1 }} />
        <SearchIcon size={44} color="#B0B0B0" />
      </div>

      <div
        style={{
          position: "absolute",
          top: 176 + focus * 132,
          left: 24,
          right: 24,
          height: 120,
          borderRadius: 32,
          background: alpha(accent, 0.14),
          border: `4px solid ${accent}`,
        }}
      />

      {ROWS.map(([title, summary], i) => (
        <div
          key={title}
          style={{
            position: "absolute",
            top: 176 + i * 132,
            left: 24,
            right: 24,
            height: 120,
            display: "flex",
            alignItems: "center",
            gap: 24,
            padding: "0 30px",
            scale: i === focused ? "1.02" : "1",
          }}
        >
          <span
            style={{
              width: 64,
              height: 64,
              borderRadius: 32,
              background: i === focused ? accent : "#2C2C2E",
              color: i === focused ? "#101012" : "#FFFFFF",
              fontSize: 30,
              fontWeight: 700,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
            }}
          >
            {i + 1}
          </span>
          <div style={{ display: "flex", flexDirection: "column", gap: 2, flexGrow: 1 }}>
            <span style={{ fontSize: 34, fontWeight: 500 }}>{title}</span>
            <span style={{ fontSize: 26, color: "#B0B0B0" }}>{summary}</span>
          </div>
          <ChevronIcon size={36} color="#8A8A90" flip />
        </div>
      ))}
    </div>
  );
};
