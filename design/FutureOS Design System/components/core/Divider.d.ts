/**
 * SettingDivider — 0.8dp hairline between rows inside a Card.
 */
export interface DividerProps {
  /** Inset 16dp from the card edges. True everywhere in the source. */
  inset?: boolean;
  style?: React.CSSProperties;
}
export function Divider(props: DividerProps): JSX.Element;
