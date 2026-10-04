The alarm time editor; a full-screen overlay (not a dialog), used only when editing an alarm.

```jsx
<TimePicker hours="07" minutes="30" repeat={[0,1,2,3,4]} focusedWheel="hours" />
```

Two **wheels**: hours on the **right**, minutes on the left (the row is RTL and hours are the first child). Each wheel is a single focus target with the field treatment (8% fill at rest; 14% accent fill and a 2dp accent ring when focused) and shows the next value above and the previous value below at 30%, so the user sees where up/down will go. The value is the only Roboto Mono in the system (48sp/300).

Keys: up/down step the focused wheel (a held key repeats), digits type the value directly (two hour digits jump to minutes), OK leaves the wheel for the repeat row, left/right move between wheels. The earlier version had four separate arrow buttons that each had to be focused and pressed once per step; do not bring them back. There is no date picker in FutureOS — do not build one on this pattern without asking.
