Groups a run of setting rows on a filled surface; the one card pattern in FutureOS.

```jsx
<SectionHeader>תצוגה</SectionHeader>
<Card>
  <SettingItem title="בהירות" summary="אוטומטית" icon="brightness_6" focused />
  <Divider />
  <SettingItem title="גודל טקסט" summary="רגיל" icon="format_size" />
</Card>
```

Never give it a border or a colored edge. Rows inside stay transparent so the card's fill shows through; separate them with `Divider`. It carries the only shadow in the system (4dp dark, 1dp light) — do not add shadows anywhere else.
