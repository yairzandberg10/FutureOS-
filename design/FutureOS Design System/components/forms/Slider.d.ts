/**
 * VolumeSlider — the only continuous control in FutureOS: media volume, screen brightness, text size.
 */
export interface SliderProps {
  /** 14sp / 60% label above the track, e.g. "עוצמת מדיה". */
  label: string;
  /** 0..1. Left and right keys step by 0.05. */
  value?: number;
  /** Keypad focus: 18% text background (6% idle) plus a 2dp accent border. */
  focused?: boolean;
  style?: React.CSSProperties;
}
export function Slider(props: SliderProps): JSX.Element;
