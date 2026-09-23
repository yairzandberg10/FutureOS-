A FutureOS button; use for dialog actions and full-screen overlay actions — there is no button anywhere else in the system.

```jsx
<Button variant="primary" focused>אישור</Button>
<Button variant="destructive">מחק</Button>
<Button variant="secondary">ביטול</Button>
<Button variant="quiet">ביטול</Button>
```

Labels are bare Hebrew verbs, never sentences. `primary` and `destructive` put black text on a solid colored fill; `secondary` (ביטול) is a 20% text-tint pill with white text; `quiet` a 10% tint. Fills look the same idle and focused, never faded. Focus adds a small gap and then a ring in the button's own color, plus a 1.02 lift — layout never shifts. There is no disabled state.
