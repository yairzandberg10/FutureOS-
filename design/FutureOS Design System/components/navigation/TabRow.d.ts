/**
 * ViewModeTabRow — equal-width segmented tabs; calendar view modes and the gallery.
 */
export interface TabRowProps {
  /** Two to four short Hebrew labels. */
  items?: string[];
  selected?: number;
  /** Index of the focused tab, or -1. */
  focusedIndex?: number;
  onSelect?: (index: number) => void;
  style?: React.CSSProperties;
}
export function TabRow(props: TabRowProps): JSX.Element;
