/**
 * DayToggleChip — a 36dp round day toggle; the alarm repeat row is its only home.
 */
export interface DayChipProps {
  /** One Hebrew letter: א ב ג ד ה ו ש. */
  children?: React.ReactNode;
  selected?: boolean;
  focused?: boolean;
  onClick?: () => void;
  style?: React.CSSProperties;
}
export function DayChip(props: DayChipProps): JSX.Element;
