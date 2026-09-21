/**
 * ScreenTopBar — the header every FutureOS screen except the launcher home starts with.
 */
export interface TopBarProps {
  /** Screen title at 20sp / 700. One or two words. */
  title: string;
  /** Omit to drop the back button (root screens). */
  onBack?: () => void;
  /** Omit to drop the overflow button. Opens OptionsMenu; also bound to the hardware menu key. */
  onMenu?: () => void;
  backFocused?: boolean;
  menuFocused?: boolean;
  style?: React.CSSProperties;
}
export function TopBar(props: TopBarProps): JSX.Element;
