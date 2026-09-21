/**
 * The four recurring FutureOS button patterns on DialogButton geometry.
 */
export interface ButtonProps {
  children?: React.ReactNode;
  /** primary = accent fill, destructive = danger fill, secondary = 70% text fill, quiet = 10% text fill. */
  variant?: "primary" | "destructive" | "secondary" | "quiet";
  /** Keypad focus. Adds a 2dp white border and lifts the fill to full opacity. */
  focused?: boolean;
  fullWidth?: boolean;
  onClick?: () => void;
  style?: React.CSSProperties;
}
export function Button(props: ButtonProps): JSX.Element;
