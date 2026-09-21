/**
 * Spinner — indeterminate circular progress, for waits with no known length.
 */
export interface SpinnerProps {
  /** Diameter in device px. Default 72 (36dp). */
  size?: number;
  /** Ring thickness in device px. Default 8 (4dp, matching ProgressBar). */
  thickness?: number;
  /** Optional label beside the ring, e.g. מחפש מכשירים. */
  label?: string;
  style?: React.CSSProperties;
}
export function Spinner(props: SpinnerProps): JSX.Element;
