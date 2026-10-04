/**
 * NotificationCard — one notification in the Notification Center, drawn as a GlassTile. Collapsed it is
 * an app disc and "app: title" over a two-line body (all of it when expanded); with the options open the
 * same card shows the notification's actions as menu rows instead.
 */
export interface NotificationCardOption {
  label: string;
  /** Renders the row in the danger-on-glass color (`--fos-danger-on-glass`, #FF6B6B). The app's
   *  "כבה התראות" is the one destructive option. */
  destructive?: boolean;
}
export interface NotificationCardProps {
  /** App name. Joined to the title as "app: title" at 14sp / 700; also the 12sp header of the options. */
  appName: string;
  /** Sender or subject. Without it the line is the app name alone. */
  title?: string;
  /** Content at 13sp. Two lines when collapsed, wrapped in full when `expanded`. */
  body?: string;
  /** FosIcon name for the app glyph in the 38dp disc. Default "chat". In the app this is the
   *  notification's own icon. */
  icon?: string;
  /** Keypad focus on the card: glass 24% fill and a 2dp white ring. It stays on while the options are open. */
  focused?: boolean;
  /** Long OK. Shows the whole body instead of two lines. */
  expanded?: boolean;
  /** Options key. Replaces the content with `options` as vertical menu rows under the app name. */
  optionsOpen?: boolean;
  /** The notification's actions, shown as OptionsMenu rows while `optionsOpen`. In the app: up to
   *  two real actions from the source app (no text-input ones), then "כבה התראות" (destructive),
   *  then "חזור". */
  options?: NotificationCardOption[];
  /** Index of the selected option row (the first, when the options open). Its fill is 12% white, with no border. */
  focusedOption?: number;
  onSelect?: (index: number) => void;
  style?: React.CSSProperties;
}
export function NotificationCard(props: NotificationCardProps): JSX.Element;
