import React from "react";
import { Img, staticFile } from "remotion";
import { CameraIcon, FlashlightIcon, GearIcon, HandsetIcon } from "../components/Icons";
import { StatusBar } from "./StatusBar";
import { wallpaper } from "./wallpaper";

// FutureUI lock screen: large light clock over the date, a notification,
// and the default shortcut row (phone, camera, flashlight, settings).
export const LockScreen: React.FC<{ accent?: string }> = ({ accent = "#64D2FF" }) => (
  <div style={{ position: "absolute", inset: 0, direction: "rtl", color: "#FFFFFF", background: wallpaper(accent) }}>
    <StatusBar />
    <div style={{ marginTop: 64, display: "flex", flexDirection: "column", alignItems: "center" }}>
      <div style={{ fontSize: 180, fontWeight: 200, lineHeight: 1, letterSpacing: -4, direction: "ltr" }}>09:41</div>
      <div style={{ fontSize: 40, fontWeight: 300, opacity: 0.9, marginTop: 14 }}>יום שני, 14 בספטמבר</div>
    </div>

    <div
      style={{
        position: "absolute",
        top: 450,
        left: 28,
        right: 28,
        borderRadius: 44,
        padding: "24px 28px",
        background: "rgba(255,255,255,0.12)",
        border: "2px solid rgba(255,255,255,0.10)",
        display: "flex",
        gap: 22,
        alignItems: "center",
      }}
    >
      <Img src={staticFile("icons/messages.webp")} style={{ width: 76, height: 76 }} />
      <div style={{ display: "flex", flexDirection: "column", gap: 4, flexGrow: 1 }}>
        <div style={{ display: "flex", alignItems: "baseline", gap: 12 }}>
          <span style={{ fontSize: 30, fontWeight: 700 }}>אמא</span>
          <span style={{ flexGrow: 1 }} />
          <span style={{ fontSize: 22, color: "#B0B0B0" }}>עכשיו</span>
        </div>
        <span style={{ fontSize: 30, color: "#E4E4E9" }}>מתי אתה מגיע הביתה?</span>
      </div>
    </div>

    <div style={{ position: "absolute", bottom: 56, left: 0, right: 0, display: "flex", justifyContent: "center", gap: 34 }}>
      {[HandsetIcon, CameraIcon, FlashlightIcon, GearIcon].map((Icon, i) => (
        <div
          key={i}
          style={{
            width: 104,
            height: 104,
            borderRadius: 52,
            background: "rgba(255,255,255,0.14)",
            border: "2px solid rgba(255,255,255,0.12)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
          }}
        >
          <Icon size={46} color="#FFFFFF" />
        </div>
      ))}
    </div>
  </div>
);
