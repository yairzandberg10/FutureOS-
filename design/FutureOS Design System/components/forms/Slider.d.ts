/**
 * VolumeSlider / FutureSlider — the only continuous control in FutureOS: media volume, screen
 * brightness, text size. Used in Settings and in the Control Center.
 */
export interface SliderProps {
  /** 14sp / 60% label above the track, e.g. "עוצמת מדיה". */
  label: string;
  /** Optional leading FosIcon name, drawn 20dp at 60% text before the label, e.g. "volume_up". */
  icon?: string;
  /** 0..1. The left key raises it and the right key lowers it, 0.05 per press. */
  value?: number;
  /** Keypad focus: 18% text background (6% idle) plus a 2dp accent border. */
  focused?: boolean;
  style?: React.CSSProperties;
}
export function Slider(props: SliderProps): JSX.Element;
