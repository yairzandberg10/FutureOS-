/**
 * DatePicker — full-screen month grid overlay, TimePicker's sibling.
 */
export interface DatePickerProps {
  title?: string;
  year?: number;
  /** 1-12. The grid is computed from year + month. */
  month?: number;
  /** Day number rendered in the selected state. */
  selected?: number;
  /** Day number carrying the keypad cursor. */
  focused?: number;
  onPrevMonth?: () => void;
  onNextMonth?: () => void;
  onSelect?: (day: number) => void;
  onCancel?: () => void;
  onSave?: () => void;
  style?: React.CSSProperties;
}
export function DatePicker(props: DatePickerProps): JSX.Element;
