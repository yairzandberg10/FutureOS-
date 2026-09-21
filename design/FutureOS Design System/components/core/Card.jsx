import React from "react";

const isRow = (n) => React.isValidElement(n) && n.type && n.type.__fosCardRow;

function countRows(nodes) {
  let n = 0;
  React.Children.forEach(nodes, (node) => {
    if (!React.isValidElement(node)) return;
    if (isRow(node)) n += 1;
    else if (node.props && node.props.children) n += countRows(node.props.children);
  });
  return n;
}

function placeRows(nodes, total, state) {
  return React.Children.map(nodes, (node) => {
    if (!React.isValidElement(node)) return node;
    if (isRow(node)) {
      const i = state.i++;
      if (node.props.position) return node;
      const pos = total === 1 ? "only" : i === 0 ? "first" : i === total - 1 ? "last" : "middle";
      return React.cloneElement(node, { position: pos });
    }
    if (node.props && node.props.children) return React.cloneElement(node, { children: placeRows(node.props.children, total, state) });
    return node;
  });
}

/* SettingsCard: filled surface, 22dp radius, 16dp side inset, 6dp vertical inset,
   4dp shadow in dark / 1dp in light. Rows inside are transparent. */
export function Card({ children, style, ...rest }) {
  /* rows learn where they sit, so their focus shape keeps only the card's own corners.
     Rows are found through wrapper elements too — kits wrap them in focus-scroll divs. */
  const count = countRows(children);
  const positioned = count === 0 ? children : placeRows(children, count, { i: 0 });
  return (
    <div
      style={{
        direction: "rtl",
        background: "var(--fos-surface-card)",
        borderRadius: "var(--fos-radius-main)",
        boxShadow: "var(--fos-shadow-card)",
        margin: "var(--fos-space-2) var(--fos-space-7)",
        padding: "var(--fos-space-1) 0",
        overflow: "hidden",
        ...style
      }}
      {...rest}
    >
      {positioned}
    </div>
  );
}
