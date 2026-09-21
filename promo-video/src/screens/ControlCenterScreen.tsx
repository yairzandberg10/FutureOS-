import React from "react";
import {
  AirplaneIcon,
  BluetoothIcon,
  DndIcon,
  FlashlightIcon,
  IconProps,
  LocationIcon,
  MoonIcon,
  RotateIcon,
  SunIcon,
  VolumeIcon,
} from "../components/Icons";
import { StatusBar } from "./StatusBar";
import { wallpaper } from "./wallpaper";

// FutureUI control center: the six default tiles from ControlLayoutManager,
// brightness / volume sliders and the dark-mode toggle, on 28dp glass surfaces.
const TILES: Array<[React.FC<IconProps>, string, boolean]> = [
  [BluetoothIcon, "Bluetooth", true],
  [FlashlightIcon, "פנס", false],
  [AirplaneIcon, "מצב טיסה", false],
  [DndIcon, "נא לא להפריע", true],
  [LocationIcon, "מיקום", true],
  [RotateIcon, "סיבוב", false],
];

const SLIDERS: Array<[React.FC<IconProps>, string]> = [
  [SunIcon, "64%"],
  [VolumeIcon, "42%"],
];

export const ControlCenterScreen: React.FC<{ accent: string }> = ({ accent }) => (
  <div style={{ position: "absolute", inset: 0, direction: "rtl", color: "#FFFFFF", background: wallpaper(accent) }}>
    <div style={{ position: "absolute", inset: 0, background: "rgba(0,0,0,0.5)" }} />
    <div style={{ position: "relative" }}>
      <StatusBar />
    </div>
    <div
      style={{
        position: "absolute",
        top: 76,
        left: 20,
        right: 20,
        borderRadius: 56,
        background: "rgba(28,28,30,0.80)",
        border: "2px solid rgba(255,255,255,0.10)",
        padding: 26,
        display: "flex",
        flexDirection: "column",
        gap: 22,
      }}
    >
      <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 18 }}>
        {TILES.map(([Icon, label, on]) => (
          <div
            key={label}
            style={{
              height: 170,
              borderRadius: 56,
              background: on ? accent : "rgba(255,255,255,0.10)",
              color: on ? "#101012" : "#FFFFFF",
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              justifyContent: "center",
              gap: 12,
            }}
          >
            <Icon size={56} color={on ? "#101012" : "#FFFFFF"} />
            <span style={{ fontSize: 22, fontWeight: 500, whiteSpace: "nowrap" }}>{label}</span>
          </div>
        ))}
      </div>

      {SLIDERS.map(([Icon, width]) => (
        <div
          key={width}
          style={{ position: "relative", height: 96, borderRadius: 48, background: "rgba(255,255,255,0.10)", overflow: "hidden" }}
        >
          <div style={{ position: "absolute", top: 0, bottom: 0, right: 0, width, background: accent }} />
          <div style={{ position: "absolute", top: 0, bottom: 0, right: 28, display: "flex", alignItems: "center" }}>
            <Icon size={44} color="#101012" />
          </div>
        </div>
      ))}

      <div
        style={{
          height: 150,
          borderRadius: 56,
          background: "rgba(255,255,255,0.10)",
          display: "flex",
          alignItems: "center",
          padding: "0 36px",
          gap: 22,
        }}
      >
        <MoonIcon size={52} color="#FFFFFF" />
        <span style={{ fontSize: 30, fontWeight: 500 }}>מצב כהה</span>
        <span style={{ flexGrow: 1 }} />
        <span style={{ width: 96, height: 56, borderRadius: 28, background: accent, position: "relative" }}>
          <span style={{ position: "absolute", top: 6, left: 6, width: 44, height: 44, borderRadius: 22, background: "#101012" }} />
        </span>
      </div>
    </div>
  </div>
);
