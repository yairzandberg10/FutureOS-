/**
 * InputDialog — rename / create, on the ConfirmDialog shell with a TextField in the middle.
 */
export interface InputDialogProps {
  /** A noun phrase, not a question: "שם חדש". */
  title: string;
  value?: string;
  placeholder?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  onConfirm?: () => void;
  onCancel?: () => void;
  style?: React.CSSProperties;
}
export function InputDialog(props: InputDialogProps): JSX.Element;
