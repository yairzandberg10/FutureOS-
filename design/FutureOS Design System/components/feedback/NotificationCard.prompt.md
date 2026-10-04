One notification in the Notification Center. It lives on a `GlassPanel` and is itself a `GlassTile`; the floating notification that appears over an app is `HeadsUpNotification`, a different surface.

```jsx
<NotificationCard appName="הודעות" title="מיכל לוי" body="נתראה בערב, אני מביאה את הדברים" focused />

<NotificationCard appName="הודעות" title="מיכל לוי" body="נתראה בערב, אני מביאה את הדברים" focused expanded />

<NotificationCard appName="הודעות" focused optionsOpen focusedOption={1}
  options={[
    { label: "סמן כנקרא" },
    { label: "כבה התראות", destructive: true },
    { label: "חזור" }
  ]} />
```

**Layout.** A glass tile at **28dp radius** with 14dp side and 13dp vertical padding: a **38dp disc** (white 20%) holding the app's glyph, 14dp from the text column. The first line is **`app: title`** at 14sp bold, one line, ellipsized; under it the body at 13sp in the secondary shell ink (`--fos-shell-ink-sub`), **two lines**, or all of it when expanded. The card is glass: white 15% at rest, 24% with a 2dp white ring when it holds focus.

**Options are menu rows.** The Options key swaps the card's content for the notification's actions, stacked vertically — the same rows `OptionsMenu` draws, not a custom button strip: **50dp tall, 15sp label** (medium weight), **16dp radius**, 12dp side padding, under a 12sp app-name header. The selected row is a **12% white fill with no border and no scale** — the same exception to border-on-focus that `OptionsMenu` makes — and a **destructive row is in `--fos-danger-on-glass`**. The card itself keeps its glass fill and ring while its rows are navigated. Labels are bare verbs. The set is fixed by the app: up to two real actions the source app attached to the notification (`ענה`, `דחה`, `סמן כנקרא` — never ones that need typed text), then **`כבה התראות`** (destructive: opens that app's notification settings), then **`חזור`** (closes the menu). The first row is selected when the menu opens.

**Keys.**

| key | on the card | with the options open |
|---|---|---|
| OK | opens the notification (its contentIntent) and **closes the center** | runs the selected row, then closes the menu |
| long OK | **expands** the body to full text (or collapses it again) | none |
| Options | **opens** the options | closes them |
| up / down | previous or next card | previous or next row, wrapping |
| long left | switches to the Control Center | none |
| long right | **dismisses** this notification | none |

Dismissing needs a hold on purpose: right is also an ordinary navigation key, and an accidental tap must not delete a notification with no undo. Because OK opens the notification, expanding and the options are never the default path, which is why the collapsed card carries no hint of them. With no notifications the list shows `אין התראות חדשות` at 16sp in the secondary shell ink.
