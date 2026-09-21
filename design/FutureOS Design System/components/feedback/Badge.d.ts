/**
 * The unread count badge — an 18dp accent circle with black digits.
 */
export interface BadgeProps {
  count: number | string;
  style?: React.CSSProperties;
}
export function Badge(props: BadgeProps): JSX.Element;
