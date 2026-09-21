/**
 * Snackbar — bottom-anchored transient feedback, with one optional action label.
 */
export interface SnackbarProps {
  /** What happened, e.g. ההודעה נמחקה. */
  message?: React.ReactNode;
  /** Action label, e.g. בטל. Reached with the left soft key. */
  action?: string;
  onAction?: () => void;
  style?: React.CSSProperties;
}
export function Snackbar(props: SnackbarProps): JSX.Element;
