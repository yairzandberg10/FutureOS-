/**
 * LinearProgressIndicator — determinate linear progress. The circular one is Material's default, untouched.
 */
export interface ProgressBarProps {
  /** 0..1. */
  value?: number;
  /** 2dp instead of 4dp — the music player's variant. */
  mini?: boolean;
  style?: React.CSSProperties;
}
export function ProgressBar(props: ProgressBarProps): JSX.Element;
