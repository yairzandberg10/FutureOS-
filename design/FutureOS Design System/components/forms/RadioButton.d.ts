/**
 * RadioButton — one-of-many control, used in a row's trailing slot.
 */
export interface RadioButtonProps {
  selected?: boolean;
  /** Diameter in device px. Default 48 (24dp). */
  size?: number;
  onChange?: () => void;
  style?: React.CSSProperties;
}
export function RadioButton(props: RadioButtonProps): JSX.Element;
