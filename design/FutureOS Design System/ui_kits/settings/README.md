# UI kit · Settings

A click-through recreation of the FutureOS settings app at 640 × 960. It is the densest
surface in the system: `SectionHeader` + `Card` + `SettingItem`, with `Switch` in trailing
slots, `Slider` rows on the screen background, `OptionsMenu` on the menu key and
`ConfirmDialog` in front of anything destructive.

## Screens

| screen | built from |
|---|---|
| root | `TopBar`, `SectionHeader`, `Card`, `Divider`, `SettingItem` |
| תצוגה (display) | `Slider` ×2, `SettingItem` + `Switch` ×3 |
| צבע הדגשה (accent) | `SettingItem` with a check in the trailing slot |
| צלילים (sound) | `Slider` ×3 |
| אודות (about) | `SettingItem`, `ConfirmDialog` |

## Keys

Arrow up/down moves focus. Arrow left/right steps a focused slider by 5% (left increases —
left is forward in RTL). Enter activates. Backspace or Escape leaves the screen. `m` stands in
for the hardware menu key.

Two things are wired live, because they are real system behaviour: **picking an accent** rewrites
`--fos-accent` for the whole device, and **the text-size slider** drives `--fos-font-scale`
across the 0.8–1.4 range, so you can watch every layout absorb it.

## Faithfulness notes

The group names and row inventory are a plausible arrangement of the settings surfaces named in
`SettingsScreens.kt` (brightness, text size, volume, language, accent choice). The five accent
values, the 0.8–1.4 text range and the 5% slider step are exact. Nothing here introduces a visual
pattern that is not in `components/`.
