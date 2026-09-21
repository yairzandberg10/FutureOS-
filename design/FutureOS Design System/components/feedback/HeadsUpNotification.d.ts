/**
 * HeadsUpNotificationScreen — the floating notification. Always dark, even in light mode.
 */
export interface HeadsUpNotificationProps {
  /** App name at 12sp / 65% white. */
  appName: string;
  /** Sender or subject at 15sp / 700 white. */
  title: string;
  /** One line of content at 13sp / 55% white. */
  body?: string;
  icon?: string;
  style?: React.CSSProperties;
}
export function HeadsUpNotification(props: HeadsUpNotificationProps): JSX.Element;
