The alarm time editor; a full-screen overlay (not a dialog), used only when editing an alarm.

```jsx
<TimePicker hours="07" minutes="30" repeat={[0,1,2,3,4]} focusedDay={5} />
```

Hours sit on the **right**, minutes on the left, because the row is RTL and hours are the first child — in the source this occasionally reads as minutes-before-hours, a known bug worth watching. The values are the only Roboto Mono in the system. Up/down keys step the focused column; left/right move between columns and into the repeat row. There is no date picker in FutureOS — do not build one on this pattern without asking.
