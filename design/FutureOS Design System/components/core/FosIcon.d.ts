/**
 * FosIcon — the FutureOS custom icon set ("Future Glyphs"), drawn on a 24 grid with a
 * 1.6 stroke, round caps and joins. Names match the Material Symbols names the system
 * already uses, and any name the set does not define falls through to Material Symbols,
 * so no screen breaks while the set grows.
 */
export interface FosIconProps {
  /** Icon name, e.g. "call". Falls back to a Material Symbols glyph when undrawn. */
  name: string;
  /** Rendered box in px. Default 24. */
  size?: number;
  /** Stroke color. Default currentColor. */
  color?: string;
  /** 0–1. Default 1. */
  opacity?: number;
  /** 1 fills the closed shapes (star, bookmark, play) — used for selected states. */
  fill?: 0 | 1 | boolean;
  /** Override the 1.6 default only for oversized display glyphs. */
  strokeWidth?: number;
  style?: React.CSSProperties;
}

/** Every name the custom set draws, sorted. */
export declare const FOS_ICON_NAMES: string[];
export declare function FosIcon(props: FosIconProps): JSX.Element;
