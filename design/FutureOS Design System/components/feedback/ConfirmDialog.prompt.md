Asks before anything destructive; render it inside a positioned screen container so its scrim covers the screen.

```jsx
<ConfirmDialog message="למחוק את איש הקשר?" confirmLabel="מחק" focus="confirm" />
```

The message is a question in the infinitive and the buttons answer it with bare verbs — never "כן"/"לא". Cancel sits on the right (the RTL start), the action on the left. Focus is trapped inside: the back key closes, the arrow keys move only between the two buttons. The dialog carries the 1.02 focus scale as a whole.
