/**
 * The options overlay opened by the hardware menu key — FutureOS has no drawer and no dropdown; this covers both.
 */
export interface OptionsMenuItem {
  label: string;
  icon: string;
  /** Renders the row in the danger color. Always last. */
  destructive?: boolean;
}
export interface OptionsMenuProps {
  /** Optional context line at 12sp / 50% — usually the name of the item being acted on. */
  header?: string;
  items?: OptionsMenuItem[];
  /** Index of the focused row, or -1. */
  focusedIndex?: number;
  onSelect?: (index: number) => void;
  style?: React.CSSProperties;
}
export function OptionsMenu(props: OptionsMenuProps): JSX.Element;
