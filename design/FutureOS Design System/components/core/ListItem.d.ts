/**
 * FocusableItem — the 112px list row that carries most of the content in FutureOS.
 */
export interface ListItemProps {
  title: string;
  /** Second line: a value or a phone number, at 13sp / 60%. Numerals stay LTR. */
  summary?: string;
  /** Optional trailing node — a Badge, a favorite star, a chevron. Sits on the left in RTL. */
  trailing?: React.ReactNode;
  /** Keypad focus: 14% accent background, 1.5dp accent border, 1.02 scale. */
  focused?: boolean;
  onClick?: () => void;
  style?: React.CSSProperties;
}
export function ListItem(props: ListItemProps): JSX.Element;
