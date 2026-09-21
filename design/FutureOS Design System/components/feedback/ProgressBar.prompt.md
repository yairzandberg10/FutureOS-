Shows determinate progress — a download, a track position.

```jsx
<ProgressBar value={0.62} />
<ProgressBar value={0.3} mini />
```

The fill grows from the right, in the accent color, on a 10% text track, and animates linearly (never eased). Pair it with a 14sp / 60% label above it; the bar itself carries no percentage text.
