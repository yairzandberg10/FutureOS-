/**
 * MonoValue — a running numeric readout in Roboto Mono, tabular and LTR.
 */
export interface MonoValueProps {
  /** The value string, e.g. "01:24" or "14:38:07". */
  children?: React.ReactNode;
  /** Any type-size token. Default --fos-size-header. */
  size?: string;
  /** Numeric weight. Default 300 — the light face the clock surfaces use. */
  weight?: number;
  /** Value color. Pass the success color for a live timer. */
  color?: string;
  /** Optional caption under the value, e.g. a city or a lap number. */
  label?: string;
  style?: React.CSSProperties;
}
export function MonoValue(props: MonoValueProps): JSX.Element;
