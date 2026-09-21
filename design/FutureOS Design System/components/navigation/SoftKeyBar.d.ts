/**
 * SoftKeyBar — bottom legend naming the two soft keys and the OK key on this screen.
 */
export interface SoftKeyBarProps {
  /** Right-hand label in RTL: the left soft key, usually the contextual action. */
  left?: string;
  /** The OK key's action, in the accent color. */
  center?: string;
  /** Left-hand label in RTL: the right soft key, usually חזור or תפריט. */
  right?: string;
  style?: React.CSSProperties;
}
export function SoftKeyBar(props: SoftKeyBarProps): JSX.Element;
