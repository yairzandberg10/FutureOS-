/**
 * BasicTextField — text entry, driven by the numeric keypad.
 */
export interface TextFieldProps {
  value?: string;
  /** Shown at 40% text when empty. */
  placeholder?: string;
  /** Keypad focus: 2dp accent border and an accent caret. */
  focused?: boolean;
  showCaret?: boolean;
  style?: React.CSSProperties;
}
export function TextField(props: TextFieldProps): JSX.Element;
