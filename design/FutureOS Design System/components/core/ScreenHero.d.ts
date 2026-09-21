/**
 * ScreenHero — the identity block a detail screen opens with: avatar, name, status.
 */
export interface ScreenHeroProps {
  /** The name, shown large and used for the avatar's initials. */
  name?: string;
  /** Glyph for the avatar instead of initials (a device, a call direction). */
  icon?: string;
  /** Small tracked label above the avatar, e.g. שיחה נכנסת. */
  eyebrow?: string;
  /** One status line under the name, e.g. מחובר · 80%. */
  status?: string;
  /** Color of the status line — pass the success color for a live call. */
  statusColor?: string;
  /** Avatar diameter. Default 176. */
  size?: number;
  /** Extra content under the status line (a timer, a ProgressBar). */
  children?: React.ReactNode;
  style?: React.CSSProperties;
}
export function ScreenHero(props: ScreenHeroProps): JSX.Element;
