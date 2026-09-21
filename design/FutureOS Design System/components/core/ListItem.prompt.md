The standard focusable list row — contacts, messages, files, anything scrollable.

```jsx
<ListItem title="מיכל לוי" summary="052-3334455" focused />
<ListItem title="דני כהן" summary="נתראה בערב" trailing={<Badge count={2} />} />
```

Rows are separated by `itemSpacing` (12dp) on the screen background — never by a divider. Exactly one row on a screen is focused, and the list scrolls to keep it visible. The row scales 1.02 on focus; setting rows and menu rows do not, so don't copy the scale elsewhere.
