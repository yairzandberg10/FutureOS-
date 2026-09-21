/**
 * ConfirmDialog — the shared confirmation, and the canonical dialog of the system.
 */
export interface ConfirmDialogProps {
  /** A question in the infinitive: "למחוק את איש הקשר?" */
  message: string;
  /** A bare verb: "מחק", "שמור". */
  confirmLabel?: string;
  cancelLabel?: string;
  /** Danger fill on the confirm button. True for anything that loses data. */
  destructive?: boolean;
  /** Which button holds keypad focus. */
  focus?: "confirm" | "cancel";
  onConfirm?: () => void;
  onCancel?: () => void;
  style?: React.CSSProperties;
}
export function ConfirmDialog(props: ConfirmDialogProps): JSX.Element;
