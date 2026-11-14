# Fitness (כושר)

`com.future.fitness` — Workout tracker built into the FutureOS suite.

Home dashboard (today's calories/active minutes/streak, "next up" workout card, digit-shortcut menu 1–4) → workout list → workout detail → a live session screen with a real elapsed-time timer and a real countdown rest timer between sets → completion summary → progress (weekly activity chart and monthly totals, computed from real history — no fabricated numbers) and history, all built from a `WorkoutStore` that persists completed sessions, daily stats, and the streak as JSON in `SharedPreferences` (same pattern as `PlaylistStore` in [Music](../Music/)).

Dark/light mode and the accent color are shared OS-wide via `ThemeClient`/FutureUI's `ContentProvider`, read on launch and refreshed on resume — like every other app in the suite, Fitness has no appearance picker of its own; only the central [FutureUI](../FutureUI/) Settings screen controls that. The app's own Settings screen only holds the one thing that's genuinely local to it: the kg/lb unit toggle.

No touchscreen input (`dispatchTouchEvent` is swallowed) — navigation is keypad/D-pad only, with digit-key shortcuts on the home menu, matching the real hardware target of the suite.
