An unread count; in the source it appears only on conversation rows.

```jsx
<ListItem title="דני כהן" summary="נתראה בערב" trailing={<Badge count={2} />} />
```

Numbers only — never a dot, never a label, never a color other than the accent. It goes in a `ListItem`'s trailing slot, which in RTL puts it at the left end of the row.
