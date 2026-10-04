/**
 * Widget — the home-screen widget template the launcher hosts (2 x 1 cells, not resizable,
 * refreshed every 30 minutes and whenever the launcher returns home). Always dark, even in light mode:
 * it sits on the user's wallpaper, not on an app surface.
 */
export interface WidgetProps {
  /** FosIcon name standing in for the app's launcher icon, drawn 18dp and untinted before the title.
   *  In the app this is the app's own icon, as is. Default "widgets". */
  icon?: string;
  /** App name or topic, 12sp at 60% white — one line, ellipsized. e.g. "שעון". */
  title: string;
  /** The headline value, 20sp bold white — a time, a count, a status. In the `clock` variant
   *  it is a time at 28sp, fed live by the app (the template only draws the string). */
  value: string;
  /** One supporting line of context, 13sp at 60% white, two lines at most. */
  subtitle?: string;
  /** "clock" sets the value at 28sp, for a live time (the app draws a live HH:mm clock instead of
   *  a string). */
  variant?: "default" | "clock";
  style?: React.CSSProperties;
}
export function Widget(props: WidgetProps): JSX.Element;
