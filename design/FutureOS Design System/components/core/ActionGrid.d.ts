/**
 * ActionGrid — a grid of focusable icon+label action cells (FutureActionCell). The glyph is either a
 * FosIcon or a text symbol; a legend cell is the text-symbol cell made non-focusable.
 */
export interface ActionGridItem {
  /** FosIcon name. Give either `icon` or `glyph`, not both. */
  icon?: string;
  /** Text symbol instead of an icon — "+", "sin", "π", "C". Drawn at 90% of the icon size
   *  in the same 70% text alpha (accent when `active`). Used by the calculator keys. */
  glyph?: string;
  /** Optional 13sp / 60% label under the glyph. One line, ellipsized. */
  label?: string;
  /** Renders the cell in the accent-filled on state (mute engaged, item saved, operation pending). */
  active?: boolean;
  /** Overrides the glyph color — e.g. the favorite color on a starred item. */
  color?: string;
  /** Key legend: the cell only reminds the user what a physical key does — glyph = the operation,
   *  label = the key name ("*", "#", "חזור", "תפריט"). * and # are short presses only: their long press belongs to the shell (control / notification center). It takes no focus and cannot be activated.
   *  Overrides the grid-level `legend` for this cell. */
  legend?: boolean;
}
export interface ActionGridProps {
  items?: ActionGridItem[];
  /** Index of the focused cell, or -1. A legend cell never takes the focus border. */
  focusedIndex?: number;
  /** Cells per row. 4 for an action strip, 2 for call controls, 1 for a column. */
  columns?: number;
  /** Cell height in device px. Default 132. */
  height?: number;
  /** Default for every cell: render the whole grid as a non-focusable key legend. */
  legend?: boolean;
  onSelect?: (index: number) => void;
  style?: React.CSSProperties;
}
export function ActionGrid(props: ActionGridProps): JSX.Element;
