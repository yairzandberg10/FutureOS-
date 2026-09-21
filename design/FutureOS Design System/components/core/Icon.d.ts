/**
 * Material Symbols Rounded glyph — the icon set used throughout FutureOS.
 */
export interface IconProps {
  /** Material Symbols Rounded ligature name, e.g. "arrow_back", "chevron_left". */
  name: string;
  /** Device pixels (dp * 2). Top bar 36, menu row 40, setting row 44, empty state 112. */
  size?: number;
  color?: string;
  /** 0 = outlined (the default everywhere), 1 = filled (favorite star only). */
  fill?: 0 | 1;
  weight?: 300 | 400 | 500 | 600 | 700;
  opacity?: number;
  style?: React.CSSProperties;
}
export function Icon(props: IconProps): JSX.Element;
