import React from "react";

export type IconProps = { size?: number; color?: string; flip?: boolean };

const Stroke: React.FC<IconProps & { children: React.ReactNode }> = ({
  size = 24,
  color = "#FFFFFF",
  children,
}) => (
  <svg
    width={size}
    height={size}
    viewBox="0 0 24 24"
    fill="none"
    stroke={color}
    strokeWidth={2}
    strokeLinecap="round"
    strokeLinejoin="round"
  >
    {children}
  </svg>
);

const Filled: React.FC<IconProps & { children: React.ReactNode }> = ({
  size = 24,
  color = "#FFFFFF",
  children,
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill={color}>
    {children}
  </svg>
);

export const BluetoothIcon: React.FC<IconProps> = (p) => (
  <Stroke {...p}>
    <path d="M7 7l10 10-5 5V2l5 5L7 17" />
  </Stroke>
);

export const FlashlightIcon: React.FC<IconProps> = (p) => (
  <Stroke {...p}>
    <path d="M7 2h10v4l-3 4v12h-4V10L7 6z" />
    <path d="M12 13v2" />
  </Stroke>
);

export const AirplaneIcon: React.FC<IconProps> = (p) => (
  <Filled {...p}>
    <path d="M21 16v-2l-8-5V3.5a1.5 1.5 0 0 0-3 0V9l-8 5v2l8-2.5V19l-2 1.5V22l3.5-1 3.5 1v-1.5L13 19v-5.5z" />
  </Filled>
);

export const DndIcon: React.FC<IconProps> = (p) => (
  <Stroke {...p}>
    <circle cx="12" cy="12" r="9" />
    <path d="M8 12h8" />
  </Stroke>
);

export const LocationIcon: React.FC<IconProps> = (p) => (
  <Stroke {...p}>
    <path d="M12 22s-7-7.2-7-12.5a7 7 0 0 1 14 0C19 14.8 12 22 12 22z" />
    <circle cx="12" cy="9.5" r="2.5" />
  </Stroke>
);

export const RotateIcon: React.FC<IconProps> = (p) => (
  <Stroke {...p}>
    <path d="M20 12a8 8 0 0 1-13.7 5.6" />
    <path d="M4 12a8 8 0 0 1 13.7-5.6" />
    <path d="M18 2.5v4.2h-4.2" />
    <path d="M6 21.5v-4.2h4.2" />
  </Stroke>
);

export const SunIcon: React.FC<IconProps> = (p) => (
  <Stroke {...p}>
    <circle cx="12" cy="12" r="4" />
    <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
  </Stroke>
);

export const VolumeIcon: React.FC<IconProps> = (p) => (
  <Stroke {...p}>
    <path d="M11 5L6 9H3v6h3l5 4z" />
    <path d="M15.5 8.5a5 5 0 0 1 0 7" />
    <path d="M18.5 5.5a9 9 0 0 1 0 13" />
  </Stroke>
);

export const MoonIcon: React.FC<IconProps> = (p) => (
  <Stroke {...p}>
    <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z" />
  </Stroke>
);

export const MicIcon: React.FC<IconProps> = (p) => (
  <Stroke {...p}>
    <rect x="9" y="3" width="6" height="11" rx="3" />
    <path d="M5 11a7 7 0 0 0 14 0" />
    <path d="M12 18v3" />
  </Stroke>
);

export const HandsetIcon: React.FC<IconProps> = (p) => (
  <Filled {...p}>
    <path d="M6.6 10.8c1.4 2.8 3.8 5.1 6.6 6.6l2.2-2.2c.3-.3.7-.4 1-.2 1.1.4 2.3.6 3.6.6.6 0 1 .4 1 1V20c0 .6-.4 1-1 1C10.6 21 3 13.4 3 4c0-.6.4-1 1-1h3.5c.6 0 1 .4 1 1 0 1.3.2 2.5.6 3.6.1.3 0 .7-.2 1z" />
  </Filled>
);

export const CameraIcon: React.FC<IconProps> = (p) => (
  <Stroke {...p}>
    <path d="M3 8a2 2 0 0 1 2-2h2.5l1.5-2h6l1.5 2H19a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
    <circle cx="12" cy="13" r="3.5" />
  </Stroke>
);

export const GearIcon: React.FC<IconProps> = (p) => (
  <Stroke {...p}>
    <circle cx="12" cy="12" r="3" />
    <path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z" />
  </Stroke>
);

export const SearchIcon: React.FC<IconProps> = (p) => (
  <Stroke {...p}>
    <circle cx="11" cy="11" r="7" />
    <path d="M20 20l-3.5-3.5" />
  </Stroke>
);

/** Points right by default (back, in RTL); flip points left (forward, in RTL). */
export const ChevronIcon: React.FC<IconProps> = (p) => (
  <Stroke {...p}>
    <path d={p.flip ? "M15 5l-7 7 7 7" : "M9 5l7 7-7 7"} />
  </Stroke>
);

export const SendIcon: React.FC<IconProps> = (p) => (
  <Stroke {...p}>
    <path d="M2 2l11 11" />
    <path d="M2 2l7 20 4-9 9-4z" />
  </Stroke>
);
