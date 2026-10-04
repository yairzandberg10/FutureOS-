A value slider in its own full-width row; used for volume, brightness and text size, in Settings and in the Control Center.

```jsx
<Slider label="עוצמת מדיה" value={0.7} />
<Slider label="בהירות מסך" icon="brightness_6" value={0.4} focused />
```

The fill grows **from the right**, because the interface is RTL — left is forward, so **left raises the value and right lowers it**, 5% per key press, clamped at 0 and 100%. There is no thumb and no numeric readout; the label carries the meaning and the fill carries the value. The row is the focus target, not the track.

`icon` is optional. When given it sits **before the label** (on its right, in RTL) at 20dp and 60% text — the same alpha as the label, so icon and label read as one line. Nothing else about the row changes: same padding, same focus, same fill. The icon decorates the label; it takes no focus and no key of its own. Settings and the Control Center use this one component (`FutureSlider`), so a slider looks the same in both places; on the Control Center's glass panel it keeps its own look, with the user's accent as the ring, rather than becoming a glass tile.
