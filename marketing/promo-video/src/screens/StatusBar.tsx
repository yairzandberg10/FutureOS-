import React from "react";

export const StatusBar: React.FC<{ color?: string }> = ({ color = "#FFFFFF" }) => (
  <div
    style={{
      height: 52,
      display: "flex",
      alignItems: "center",
      padding: "0 26px",
      color,
      direction: "rtl",
    }}
  >
    <span style={{ fontSize: 26, fontWeight: 500, fontVariantNumeric: "tabular-nums" }}>09:41</span>
    <span style={{ flexGrow: 1 }} />
    <div style={{ display: "flex", alignItems: "center", gap: 12, direction: "ltr" }}>
      <div style={{ display: "flex", alignItems: "flex-end", gap: 3, height: 20 }}>
        {[8, 12, 16, 20].map((h) => (
          <span key={h} style={{ width: 5, height: h, borderRadius: 2, background: color }} />
        ))}
      </div>
      <span style={{ fontSize: 20, fontWeight: 600 }}>4G</span>
      <div style={{ display: "flex", alignItems: "center", gap: 2 }}>
        <div
          style={{
            width: 40,
            height: 20,
            borderRadius: 6,
            border: `2px solid ${color}`,
            padding: 2,
            boxSizing: "border-box",
          }}
        >
          <div style={{ width: "72%", height: "100%", borderRadius: 3, background: color }} />
        </div>
        <span style={{ width: 3, height: 8, borderRadius: 2, background: color }} />
      </div>
    </div>
  </div>
);
