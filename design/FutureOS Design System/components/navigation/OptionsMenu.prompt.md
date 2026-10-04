Every per-screen action lives here, opened by the hardware menu key; it replaces the navigation drawer, the dropdown and the context menu all at once.

```jsx
<OptionsMenu header="מסמך חדש" focusedIndex={1} items={[
  { label: "שתף", icon: "share" },
  { label: "שנה שם", icon: "edit" },
  { label: "העתק", icon: "content_copy" },
  { label: "פרטים", icon: "info" },
  { label: "מחק", icon: "delete", destructive: true }
]} />
```

Labels are bare verbs. The destructive row is last and red. Focus here is a 12% background tint with **no border and no scale** — the one component that breaks the border-on-focus rule. A destructive row still routes through `ConfirmDialog` before anything happens.

The same rows are reused inside `NotificationCard`, where a notification's options stack vertically in the card instead of in an overlay: identical 50dp height, 15sp label, and 12% focused fill with no border. Because the card is glass, the card's rows are text-only, with a 16dp radius and 12dp side padding, and a destructive row takes `--fos-danger-on-glass`. Do not restyle them further for the card.
