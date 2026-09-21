/**
 * GalleryTabChip — a filter chip. Selected always beats focused.
 */
export interface ChipProps {
  children?: React.ReactNode;
  /** idle = 6% text, focused = 18% text, selected = solid accent with black label. */
  state?: "idle" | "focused" | "selected";
  onClick?: () => void;
  style?: React.CSSProperties;
}
export function Chip(props: ChipProps): JSX.Element;
