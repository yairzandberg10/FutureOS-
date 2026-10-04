/**
 * GlassPanel — a system-shell panel (Control Center, Notification Center): a blurred snapshot of the
 * screen behind it, a 20% white wash, and glass elements on top. The only blur in FutureOS; app
 * screens stay flat. Also exports GlassTile, the element that sits on it.
 */
export interface GlassPanelProps {
  /** Stand-in for the blurred screen snapshot: any CSS `background` value (an image url(...), a
   *  gradient). Omit it to blur whatever is live behind the panel with `backdrop-filter`. */
  wallpaper?: string;
  /** Blur radius. Default `var(--fos-backdrop-blur)` = 40dp = 80px. */
  blur?: string;
  /** Glass elements, stacked with 20px (10dp) between them inside 32px (16dp) side padding. */
  children?: React.ReactNode;
  style?: React.CSSProperties;
}
export function GlassPanel(props: GlassPanelProps): JSX.Element;

/**
 * GlassTile — an element on a GlassPanel: white 15% at rest, 24% focused with a 2dp white ring;
 * `on` is a solid accent fill with on-accent ink.
 */
export interface GlassTileProps {
  /** Keypad focus: white 24% fill and a 2dp white ring (not the accent). No scale. */
  focused?: boolean;
  /** On state (a toggle that is engaged): solid accent fill, `--fos-on-accent` ink. Wins over focused fill. */
  on?: boolean;
  /** Corner radius. Default `var(--fos-radius-headsup)` (28dp, the shell radius); pass
   *  `var(--fos-radius-full)` for a round toggle or a pill button. */
  radius?: string;
  onClick?: () => void;
  children?: React.ReactNode;
  style?: React.CSSProperties;
}
export function GlassTile(props: GlassTileProps): JSX.Element;
