/**
 * Capsule — pill control with a small label over its value; `round` for the mic button.
 */
export interface CapsuleProps {
  /** 12sp tracked label above the value, e.g. תרגם מ. */
  label?: string;
  /** The value line. */
  children?: React.ReactNode;
  /** Glyph before the value, or the only content when round. */
  icon?: string;
  /** Keypad focus: 4px accent border. */
  focused?: boolean;
  /** On state: 20% accent fill and accent text. */
  active?: boolean;
  /** Render as a large circular button (the mic). */
  round?: boolean;
  /** Diameter when round. Default 168. */
  size?: number;
  onClick?: () => void;
  style?: React.CSSProperties;
}
export function Capsule(props: CapsuleProps): JSX.Element;
