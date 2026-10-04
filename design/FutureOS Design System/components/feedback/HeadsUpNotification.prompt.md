The floating notification that appears over any screen; the top layer of the system.

```jsx
<HeadsUpNotification appName="הודעות" title="מיכל לוי" body="נתראה בערב" icon="chat" />
```

Its colors are hard-coded, not themed: `#1C1C1E` at 90% with a 0.5dp 15%-white hairline, white text throughout. It stays dark in light mode by design. Three lines maximum, the body truncated to one. Note the source has **no themed toast** — short messages still go through Android's system toast in 26 files, which ignores the user's accent and text size. If you need a toast, say so; it should be built on this layer.

**Not the Notification Center card.** This is the notification that floats over an app. The same notification, once it lands in the Notification Center, is a `NotificationCard` — a glass tile on a `GlassPanel`, with `app: title` over a two-line body, and its options as `OptionsMenu`-style rows that replace the content when the Options key is pressed. OK on that card opens the notification (its contentIntent) and closes the center; long OK expands the text. The heads-up has no options and no expanded state.
