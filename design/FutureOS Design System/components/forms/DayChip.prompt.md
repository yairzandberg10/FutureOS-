A round single-letter day toggle; used only in the alarm repeat row inside `TimePicker`.

```jsx
<div style={{ display: "flex", gap: 12, direction: "rtl" }}>
  {["א","ב","ג","ד","ה","ו","ש"].map(d => <DayChip key={d} selected>{d}</DayChip>)}
</div>
```

Seven chips, 6dp gap, laid out right-to-left starting with א. Several can be selected at once — this is the one multi-select control in the system.
