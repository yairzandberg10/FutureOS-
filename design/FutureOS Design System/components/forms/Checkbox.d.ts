/**
 * Checkbox — multi-select control, used in a row's trailing slot.
 */
export interface CheckboxProps {
  checked?: boolean;
  /** Box side in device px. Default 48 (24dp). */
  size?: number;
  onChange?: () => void;
  style?: React.CSSProperties;
}
export function Checkbox(props: CheckboxProps): JSX.Element;
