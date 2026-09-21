# UI kit · Clock and alarms

Three tabs behind a `BottomNav`. The alarm list is a `Card` of `SettingItem` rows with a
`Switch` in each trailing slot; pressing confirm on a row opens `TimePicker` as a **full-screen
overlay**, which is where `DayChip` and the Roboto Mono numerals live.

## Screens

| screen | built from |
|---|---|
| שעון | mono time at 48sp / 300 plus a date line |
| מעורר | `Card`, `SettingItem`, `Switch` → `TimePicker` |
| סטופר | `EmptyState` when stopped, mono time + `ProgressBar` when running |

## Keys

Left/right switch tabs, up/down move between alarms, confirm opens the picker (and starts or stops
the stopwatch), `s` toggles the focused alarm. Inside the picker, left/right walk the repeat row.

## Deliberate gaps

The clock tab's exact composition is not documented in the sources — it is shown here as the
system's largest type on a flat background and nothing more. The stopwatch's lap list is absent for
the same reason.
