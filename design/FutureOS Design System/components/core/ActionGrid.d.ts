/**
 * ActionGrid — a grid of focusable icon+label action cells.
 */
export interface ActionGridItem {
  icon: string;
  label?: string;
  /** Renders the cell in the accent-filled on state (mute engaged, item saved). */
  active?: boolean;
  /** Overrides the glyph color — e.g. the favorite color on a starred item. */
  color?: string;
}
export interface ActionGridProps {
  items?: ActionGridItem[];
  /** Index of the focused cell, or -1. */
  focusedIndex?: number;
  /** Cells per row. 4 for an action strip, 2 for call controls, 1 for a column. */
  columns?: number;
  /** Cell height in device px. Default 132. */
  height?: number;
  onSelect?: (index: number) => void;
  style?: React.CSSProperties;
}
export function ActionGrid(props: ActionGridProps): JSX.Element;
