import React from "react";

/* Material Symbols Rounded, the set the Kotlin source uses (Icons.Rounded.*).
   size is in device pixels: dp * 2. 18dp = 36px, 22dp = 44px. */
export function Icon({ name, size = 40, color = "currentColor", fill = 0, weight = 400, opacity, style, ...rest }) {
  return (
    <span
      className="fos-icon"
      data-fill={fill}
      aria-hidden="true"
      style={{
        fontFamily: '"Material Symbols Rounded"',
        fontSize: size,
        lineHeight: 1,
        width: size,
        height: size,
        flex: "0 0 auto",
        color,
        opacity,
        fontVariationSettings: `"FILL" ${fill}, "wght" ${weight}, "GRAD" 0, "opsz" 24`,
        ...style
      }}
      {...rest}
    >
      {name}
    </span>
  );
}
