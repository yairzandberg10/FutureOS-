/**
 * The FutureOS empty state: one outlined glyph, a statement of absence, and the key that fixes it.
 */
export interface EmptyStateProps {
  /** Material Symbols Rounded name, rendered at 56dp / 40% opacity. */
  icon?: string;
  /** Neutral statement of absence, e.g. "אין אנשי קשר". */
  title: string;
  /** One imperative clause naming the key that fixes it, e.g. "לחץ על מקש התפריט כדי להוסיף". */
  subtitle?: string;
  style?: React.CSSProperties;
}
export function EmptyState(props: EmptyStateProps): JSX.Element;
