import React from "react";
import { alpha } from "../lib/util";
import { HandsetIcon } from "./Icons";

// Proportions follow hardware/openscad/qin_f22_pro_case_keyboard.scad:
// 151 x 61 mm body, a 3.54" 640x960 screen 8 mm from the top edge.
// Hebrew keycaps follow SharedKeypadNav's T9DigitMap (final forms are not printed).

type Props = {
  height: number;
  pressed?: string | null;
  pressT?: number;
  accent?: string;
  children?: React.ReactNode;
};

const NUMPAD: Array<[string, string, string]> = [
  ["1", "", ""],
  ["2", "דהו", "abc"],
  ["3", "אבג", "def"],
  ["4", "מנ", "ghi"],
  ["5", "יכל", "jkl"],
  ["6", "זחט", "mno"],
  ["7", "רשת", "pqrs"],
  ["8", "צק", "tuv"],
  ["9", "סעפ", "wxyz"],
  ["*", "", ""],
  ["0", "␣", ""],
  ["#", "", ""],
];

const DIRS: Array<[string, number, number, string]> = [
  ["up", 0, -1, "0deg"],
  ["right", 1, 0, "90deg"],
  ["down", 0, 1, "180deg"],
  ["left", -1, 0, "270deg"],
];

export const Phone: React.FC<Props> = ({
  height,
  pressed = null,
  pressT = 0,
  accent = "#64D2FF",
  children,
}) => {
  const s = height / 151;
  const mm = (v: number) => v * s;
  const glow = (id: string) => (pressed === id ? pressT : 0);

  const key = (
    id: string,
    x: number,
    y: number,
    w: number,
    h: number,
    radius: number,
  ): React.CSSProperties => {
    const g = glow(id);
    return {
      position: "absolute",
      left: mm(x),
      top: mm(y),
      width: mm(w),
      height: mm(h),
      borderRadius: mm(radius),
      background: `linear-gradient(180deg, ${alpha(accent, 0.6 * g)}, ${alpha(accent, 0.3 * g)}), linear-gradient(180deg, #313135 0%, #1c1c1f 100%)`,
      boxShadow: `inset 0 ${mm(0.3)}px 0 rgba(255,255,255,0.12), inset 0 -${mm(0.3)}px 0 rgba(0,0,0,0.5), 0 ${mm(0.5)}px ${mm(1)}px rgba(0,0,0,0.7), 0 0 ${mm(5) * g}px ${alpha(accent, 0.7 * g)}`,
      scale: `${1 - 0.06 * g}`,
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      color: "#F2F2F7",
      direction: "ltr",
    };
  };

  const screenW = mm(49.9);
  const screenH = mm(74.8);

  return (
    <div
      style={{
        position: "relative",
        width: mm(61),
        height,
        borderRadius: mm(7),
        background:
          "linear-gradient(155deg, #3b3b40 0%, #202023 30%, #111113 70%, #0a0a0b 100%)",
        boxShadow: `inset 0 0 0 ${mm(0.35)}px rgba(255,255,255,0.14), inset 0 ${mm(0.8)}px ${mm(1.5)}px rgba(255,255,255,0.08), 0 ${mm(12)}px ${mm(24)}px rgba(0,0,0,0.65)`,
      }}
    >
      {/* Front glass and earpiece */}
      <div
        style={{
          position: "absolute",
          left: mm(3),
          top: mm(5),
          width: mm(55),
          height: mm(81.5),
          borderRadius: mm(4),
          background: "#050506",
          boxShadow: "inset 0 0 0 1px rgba(255,255,255,0.05)",
        }}
      />
      <div
        style={{
          position: "absolute",
          left: mm(24.5),
          top: mm(2.1),
          width: mm(12),
          height: mm(1.1),
          borderRadius: mm(0.6),
          background: "#000000",
          boxShadow: "inset 0 1px 2px rgba(255,255,255,0.08)",
        }}
      />

      {/* Screen: content is laid out at the device's native 640x960 and scaled */}
      <div
        style={{
          position: "absolute",
          left: mm(5.55),
          top: mm(8),
          width: screenW,
          height: screenH,
          borderRadius: mm(1.2),
          overflow: "hidden",
          background: "#000000",
        }}
      >
        <div
          style={{
            position: "absolute",
            left: 0,
            top: 0,
            width: 640,
            height: 960,
            transformOrigin: "0 0",
            scale: `${screenW / 640}`,
          }}
        >
          {children}
        </div>
        <div
          style={{
            position: "absolute",
            inset: 0,
            background:
              "linear-gradient(118deg, rgba(255,255,255,0.10) 0%, rgba(255,255,255,0.02) 38%, rgba(255,255,255,0) 60%)",
          }}
        />
      </div>

      {/* Soft keys, call / end */}
      <div style={key("softL", 4, 88.5, 14, 7, 2.2)}>
        <span style={{ width: mm(4.5), height: mm(0.6), borderRadius: mm(0.3), background: "#B0B0B0" }} />
      </div>
      <div style={key("softR", 43, 88.5, 14, 7, 2.2)}>
        <span style={{ width: mm(4.5), height: mm(0.6), borderRadius: mm(0.3), background: "#B0B0B0" }} />
      </div>
      <div style={key("call", 4, 98.5, 14, 7, 2.2)}>
        <HandsetIcon size={mm(3.8)} color="#32D74B" />
      </div>
      <div style={key("end", 43, 98.5, 14, 7, 2.2)}>
        <span style={{ display: "flex", rotate: "135deg" }}>
          <HandsetIcon size={mm(3.8)} color="#FF6B6B" />
        </span>
      </div>

      {/* D-pad ring */}
      <div
        style={{
          position: "absolute",
          left: mm(21),
          top: mm(88),
          width: mm(19),
          height: mm(19),
          borderRadius: "50%",
          background:
            "radial-gradient(circle at 50% 35%, #36363a 0%, #1f1f22 60%, #161618 100%)",
          boxShadow: `inset 0 ${mm(0.3)}px 0 rgba(255,255,255,0.12), 0 ${mm(0.6)}px ${mm(1.2)}px rgba(0,0,0,0.7)`,
        }}
      >
        {DIRS.map(([id, dx, dy, rot]) => (
          <React.Fragment key={id}>
            <div
              style={{
                position: "absolute",
                left: mm(9.5 + dx * 6.4 - 4),
                top: mm(9.5 + dy * 6.4 - 4),
                width: mm(8),
                height: mm(8),
                borderRadius: "50%",
                background: `radial-gradient(circle, ${alpha(accent, 0.9 * glow(id))} 0%, rgba(0,0,0,0) 70%)`,
              }}
            />
            <svg
              width={mm(2.4)}
              height={mm(2.4)}
              viewBox="0 0 10 10"
              style={{
                position: "absolute",
                left: mm(9.5 + dx * 7 - 1.2),
                top: mm(9.5 + dy * 7 - 1.2),
                rotate: rot,
              }}
            >
              <path d="M2 6.5 L5 3.5 L8 6.5" fill="none" stroke="#8A8A90" strokeWidth={1.4} strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </React.Fragment>
        ))}
      </div>
      <div style={key("ok", 26.25, 93.25, 8.5, 8.5, 4.25)}>
        <span style={{ fontSize: mm(2.3), fontWeight: 700, letterSpacing: mm(0.1) }}>OK</span>
      </div>

      {/* Numeric keypad */}
      {NUMPAD.map(([d, he, en], i) => {
        const c = i % 3;
        const r = Math.floor(i / 3);
        return (
          <div key={d} style={{ ...key(d, 4 + c * 18.5, 110 + r * 9.3, 16, 7.5, 2.2), gap: mm(1.4) }}>
            <span style={{ fontSize: mm(3.8), fontWeight: 600, lineHeight: 1 }}>{d}</span>
            {he || en ? (
              <span style={{ display: "flex", flexDirection: "column", alignItems: "flex-start", lineHeight: 1.05 }}>
                <span style={{ fontSize: mm(1.9), fontWeight: 500 }}>{he}</span>
                <span style={{ fontSize: mm(1.35), color: "#8A8A90" }}>{en}</span>
              </span>
            ) : null}
          </div>
        );
      })}
    </div>
  );
};
