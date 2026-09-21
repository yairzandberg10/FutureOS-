/**
 * SettingItem — the 108px row that lives inside a settings Card.
 */
export interface SettingItemProps {
  title: string;
  /** The current value, not a description: "אוטומטית", "רגיל", "עברית". */
  summary?: string;
  /** Material Symbols Rounded name, rendered at 22dp in the accent color. */
  icon?: string;
  /** Replaces the chevron — a Switch, or a value node. */
  trailing?: React.ReactNode;
  /** Show the 18dp / 30% entry chevron. It points LEFT and does not mirror. */
  chevron?: boolean;
  /** Keypad focus: 6% text background, 2dp accent border, no scale. */
  focused?: boolean;
  /** Where the row sits in its Card — the focus shape keeps only the card's own corners.
   *  Card sets this on its direct SettingItem children; pass it when the row is wrapped. */
  position?: "only" | "first" | "middle" | "last";
  onClick?: () => void;
  style?: React.CSSProperties;
}
export function SettingItem(props: SettingItemProps): JSX.Element;
