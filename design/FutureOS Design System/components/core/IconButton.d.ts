/**
 * TopBarIconButton — a 36dp circular icon button; only appears in a TopBar.
 */
export interface IconButtonProps {
  /** Material Symbols Rounded name. */
  icon: string;
  /** Keypad focus: background goes from 8% text to 30% accent. */
  focused?: boolean;
  color?: string;
  onClick?: () => void;
  style?: React.CSSProperties;
}
export function IconButton(props: IconButtonProps): JSX.Element;
