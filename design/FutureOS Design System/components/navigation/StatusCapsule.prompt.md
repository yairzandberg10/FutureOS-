The status bar's capsules: translucent pills over a transparent bar, the leading one dynamic.

```jsx
<StatusCapsule time="07:30" apps={["chat", "email"]} />
<StatusCapsule state="call" label="1:24" />
<StatusCapsule state="charging" label="בטעינה · 76%" />
<StatusCapsule state="notification" icon="chat" label="הודעות" />
<StatusCapsule state="media" label="בוקר טוב" />

{/* the trailing capsule: same shell, static device status */}
<StatusCapsule>
  <FosIcon name="wifi" size={28} color="var(--fos-shell-ink-70)" />
  <FosIcon name="signal_cellular_alt" size={28} color="var(--fos-shell-ink-70)" />
  <span style={{ fontSize: "var(--fos-size-label)", fontWeight: "var(--fos-weight-medium)" }}>76%</span>
  <FosIcon name="battery_full" size={28} color="var(--fos-shell-ink)" />
</StatusCapsule>
```

**The shell.** `#1C1C1E` at **55%** (`--fos-capsule-bg`) with a 0.5dp 15% white hairline, **22dp (44px) tall**, fully round, 9dp of padding at each end, sitting on a bar that is itself **transparent** — the app shows through the bar and, faintly, through the capsules. Only the capsules are glass; unlike the shell panels there is no blur here, and ink is always white (the capsule is dark in light mode too), so a time or glyph never needs a themed color. The bar holds two capsules 4dp apart, inset 8dp from the screen edges: the dynamic one on the start side, the device status on the other.

**Idle** is the time at 14sp / 600 (tabular digits) and, 6dp after it, the glyphs of the apps with pending notifications at 14dp, 4dp apart — four at most, then `+N` at 12sp / 600 in 70% white. The trailing capsule is the same shell with static `children`, 7dp apart: the device glyphs (do-not-disturb, airplane, wifi, bluetooth, signal) at **70% white** so they never read as notifications, the battery percent at 12sp / 500 (the danger color at 15% or less, while not charging), and the battery — a frame at 70% with a white fill, success and a bolt while charging, danger when low, a warning-colored frame in battery saver.

**Live activity.** The leading capsule **widens to show what is happening, then returns to idle.** The width and color change is a **200ms transition on the standard easing, with no bounce** — the system-wide rule that nothing overshoots — while the content crossfades in over 200ms and out over 150ms. Four activities:

| state | look | label (12sp) |
|---|---|---|
| `call` | **success fill and hairline, black ink**, a 12dp call icon, 4dp gap | the duration, bold: `1:24` |
| `charging` | translucent, a 12dp **bolt in success**, 4dp gap | `בטעינה · 76%`, medium |
| `notification` | translucent, the app's 14dp glyph, 5dp gap | the app name, medium, **at most 90dp**, ellipsized; `התראה חדשה` when the name is unknown |
| `media` | translucent, a 14dp music note, 5dp gap | the title, medium, **at most 110dp**, ellipsized |

The call capsule is the only filled one. **Priority** is a live call, then a passing event (charging just plugged in, a new notification), then playing media, then idle. A passing event stays for about **3 seconds** and then the capsule shrinks back; the call lasts as long as the call, and media as long as it plays. The capsule takes no more width than its content, but it cannot push the trailing capsule off the bar — a long title ends in an ellipsis instead. Return to idle is the same 200ms in the other direction.

A capsule takes no focus: it is a status, not a button.
