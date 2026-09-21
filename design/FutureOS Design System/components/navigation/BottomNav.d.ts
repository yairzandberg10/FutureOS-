/**
 * FitnessBottomNav — a floating bottom tab bar, inset from the screen edges. Only the
 * selected item is labelled, inside an accent pill. Exists in three apps: clock, dialer, fitness.
 */
export interface BottomNavItem {
  label: string;
  /** Material Symbols Rounded name. */
  icon: string;
}
export interface BottomNavProps {
  items?: BottomNavItem[];
  selected?: number;
  onSelect?: (index: number) => void;
  style?: React.CSSProperties;
}
export function BottomNav(props: BottomNavProps): JSX.Element;
