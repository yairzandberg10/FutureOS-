/**
 * TextArea — multi-line entry on the TextField surface (the translation input).
 */
export interface TextAreaProps {
  value?: string;
  /** Shown at 40% text when empty. */
  placeholder?: string;
  /** Keypad focus: 2dp accent border. */
  focused?: boolean;
  /** Caret after the text — set false while focused but not typing. */
  showCaret?: boolean;
  /** Minimum box height in device px. Default 150. */
  minHeight?: number;
  /** Text direction of the content, for foreign-language strings. */
  dir?: "rtl" | "ltr";
  align?: "right" | "left";
  onClick?: () => void;
  style?: React.CSSProperties;
}
export function TextArea(props: TextAreaProps): JSX.Element;
