The launcher's home-screen widget: one rounded dark panel that shows a single live fact from an app, over the user's wallpaper. Every app's widget is the same template (`FutureContentWidget`) with its own content.

```jsx
<Widget variant="clock" icon="alarm" title="שעון" value="07:30" subtitle="יום רביעי, 1 באוקטובר · מעורר יום ה׳ 07:00" />
<Widget icon="chat" title="הודעות" value="2 הודעות חדשות" subtitle="אמא: מה נשמע" />
```

The surface is `#1C1C1E` at **90%** (`--fos-widget-bg`) with a 0.5dp hairline at 15% white, a **22dp radius** and 16dp / 10dp padding. It is **always dark**, in light mode too — it is the same family as the heads-up notification, because both float over something the user chose rather than over an app — so its colors are the shell tokens (`--fos-widget-bg`, `--fos-hairline-dark`, `--fos-shell-ink`), not the themed ones. The wallpaper shows through the 10%, but nothing is blurred behind it. It is 2 x 1 launcher cells and does not resize.

Three lines of type and no more: an 18dp app icon beside a **12sp title at 60%** (6dp apart; the title defaults to the app name), a **20sp bold value** in white, and a **13sp subtitle at 60%**, clamped to two lines and left out when there is none. The icon is the app's own launcher icon, drawn as it is and **not tinted** — `icon` here is only a stand-in glyph — so the value is the brightest text and the only large one.

The **clock** variant is the same widget with a live HH:mm clock as its value, set at **28sp** with 2dp less above it; the app draws it, so it keeps time without the widget refreshing. The digits are tabular and stay LTR inside the RTL row; the clock is Heebo, not Roboto Mono, which stays reserved for the time picker.

Copy follows the rest of the system: a title that names the app (or the fact, as `שיחה אחרונה`), a value rather than a description (`3 תמונות`, `אין שיחות`), and a subtitle that is one clause, or two joined by `·`, with no terminal period (`מועדפים · 12 אנשי קשר`, `האחרונה נוספה לפני שעה`). A widget that cannot read its data says so in the subtitle: `פתח כדי לאשר גישה`. The widget draws no focus state of its own, and OK opens the app that feeds it.
