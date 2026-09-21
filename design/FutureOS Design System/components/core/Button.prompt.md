A FutureOS button; use for dialog actions and full-screen overlay actions — there is no button anywhere else in the system.

```jsx
<Button variant="primary" focused>אישור</Button>
<Button variant="destructive">מחק</Button>
<Button variant="secondary">ביטול</Button>
<Button variant="quiet">ביטול</Button>
```

Labels are bare Hebrew verbs, never sentences. `primary` and `destructive` put black text on a colored fill; `secondary` is the same shape in 70% text color; `quiet` is the full-round 10%-text variant used only in the time-picker overlay and has no focus border in the source. Unfocused buttons render their fill at 70% of its opacity — that is the idle state, not a disabled state (there is no disabled state).
