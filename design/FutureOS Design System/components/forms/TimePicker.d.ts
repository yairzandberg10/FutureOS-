/**
 * TimePickerOverlay — the alarm editor: two stepper columns, a repeat row, and two actions.
 */
export interface TimePickerProps {
  title?: string;
  /** Two digits. Rendered in Roboto Mono at 48sp / 300 — the only mono text in the UI. */
  hours?: string;
  minutes?: string;
  /** Indices of selected days, 0 = א. */
  repeat?: number[];
  /** Index of the focused day chip, or -1. */
  focusedDay?: number;
  onCancel?: () => void;
  onSave?: () => void;
  style?: React.CSSProperties;
}
export function TimePicker(props: TimePickerProps): JSX.Element;
