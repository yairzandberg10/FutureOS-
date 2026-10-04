A value slider in its own full-width row; used for volume, brightness and text size.

```jsx
<Slider label="עוצמת מדיה" value={0.7} />
<Slider label="בהירות מסך" value={0.4} focused />
```

The fill grows **from the right**, because the interface is RTL — left is forward. Steps are 5% per key press. There is no thumb and no numeric readout; the label carries the meaning and the fill carries the value. The row is the focus target, not the track.
