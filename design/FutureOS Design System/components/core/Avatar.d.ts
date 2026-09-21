/**
 * Avatar — the round person/device mark: initials from the name, or a glyph.
 */
export interface AvatarProps {
  /** Up to two initials are taken from this when no icon is given. */
  name?: string;
  /** Material glyph name; replaces the initials. */
  icon?: string;
  /** Diameter in device px. 88 in lists, 160-200 in a ScreenHero. */
  size?: number;
  /** Glyph / initials color. Default 60% text for a glyph, 70% for initials. */
  color?: string;
  /** Circle fill. Default the glass surface. */
  background?: string;
  style?: React.CSSProperties;
}
export function Avatar(props: AvatarProps): JSX.Element;
