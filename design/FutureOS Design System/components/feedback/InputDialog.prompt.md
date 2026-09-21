Collects one short string — rename a file, name a new note.

```jsx
<InputDialog title="שם חדש" value="מסמך חדש" confirmLabel="שמור" />
```

The field holds focus when the dialog opens, so typing works immediately; the down key moves to the buttons. Title is a noun phrase, not a question — questions belong to `ConfirmDialog`.
