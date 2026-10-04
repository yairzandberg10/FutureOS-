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
