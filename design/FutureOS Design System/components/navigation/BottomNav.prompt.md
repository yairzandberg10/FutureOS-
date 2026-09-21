A pinned bottom tab bar; only the clock, dialer and fitness apps have one.

```jsx
<BottomNav selected={0} items={[
  { label: "ראשי", icon: "home" },
  { label: "אימונים", icon: "fitness_center" },
  { label: "התקדמות", icon: "trending_up" }
]} />
```

Two or three items, laid out right-to-left. The selected item gets an accent pill with a black icon and an accent label; the rest sit at 50% opacity. The items themselves **never take focus** — tab switching happens at the screen level from the arrow keys, which is why there is no focus state here at all.
