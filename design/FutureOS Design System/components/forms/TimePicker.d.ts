/**
 * TimePickerOverlay — the alarm editor: two wheels (hours, minutes), a repeat row, and two actions.
 * A wheel is one focus target: up/down step it, digits type into it, OK moves on.
 */
export interface TimePickerProps {
  title?: string;
  /** Two digits. Rendered in Roboto Mono at 48sp / 300 — the only mono text in the UI. */
  hours?: string;
  minutes?: string;
  /** Indices of selected days, 0 = א. */
  repeat?: number[];
  /** Which wheel holds focus, or "none" when focus is on the repeat row or buttons. */
  focusedWheel?: "hours" | "minutes" | "none";
  /** Index of the focused day chip, or -1. */
  focusedDay?: number;
  onCancel?: () => void;
  onSave?: () => void;
  style?: React.CSSProperties;
}
export function TimePicker(props: TimePickerProps): JSX.Element;
