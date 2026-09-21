A settings row: icon, title, current value, and an entry chevron; always inside a `Card`.

```jsx
<Card>
  <SettingItem title="בהירות" summary="אוטומטית" icon="brightness_6" focused />
  <Divider />
  <SettingItem title="מצב כהה" summary="מופעל" icon="dark_mode" trailing={<Switch on />} />
</Card>
```

The summary is a value, never an instruction. Pass `trailing` for a switch (which replaces the chevron — a row never has both). Unlike `ListItem` this row does **not** scale on focus; it tints 6% and takes a 2dp accent border. The chevron points left because left is forward in RTL.
