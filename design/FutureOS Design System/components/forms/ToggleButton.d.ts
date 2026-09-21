/**
 * ToggleButton — a button that carries its own on/off state (mute, speaker, repeat).
 */
export interface ToggleButtonProps {
  /** The label. */
  children?: React.ReactNode;
  /** Glyph before the label. */
  icon?: string;
  /** On state: 20% accent fill, accent glyph and label. */
  on?: boolean;
  /** Keypad focus: 4px accent border. */
  focused?: boolean;
  fullWidth?: boolean;
  onChange?: () => void;
  style?: React.CSSProperties;
}
export function ToggleButton(props: ToggleButtonProps): JSX.Element;
