/**
 * SettingSwitch — Material 3 switch geometry with FutureOS colors. Sits in a SettingItem's trailing slot.
 */
export interface SwitchProps {
  on?: boolean;
  onChange?: () => void;
  style?: React.CSSProperties;
}
export function Switch(props: SwitchProps): JSX.Element;
