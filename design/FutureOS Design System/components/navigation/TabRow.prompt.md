Switches between views of the same content — calendar day/week/month, gallery modes.

```jsx
<TabRow items={["יום", "שבוע", "חודש"]} selected={0} focusedIndex={1} />
```

Equal-width items filling the row, right-to-left, with an 8dp gap. Idle tabs sit on 8% text, focus lifts to 18%, and the selected tab is a solid accent fill with black text — selected always outranks focused. Use `Chip` instead when the set scrolls or when more than one can be picked.
