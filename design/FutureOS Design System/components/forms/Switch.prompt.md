A toggle; only ever appears in the trailing slot of a `SettingItem`.

```jsx
<SettingItem title="מצב כהה" summary="מופעל" icon="dark_mode" trailing={<Switch on />} />
```

The switch itself never takes focus — **the whole row does**, and the confirm key toggles it. The track is the accent color when on and `#C8C8CC` when off; the thumb is always white, which means an "on" switch disappears under the default white accent. That is a real defect in the source, reproduced here on purpose; flag it rather than silently fixing it.
