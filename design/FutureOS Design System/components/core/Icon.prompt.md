Renders one Material Symbols Rounded glyph; use it anywhere FutureOS needs an icon instead of hand-rolling SVG.

```jsx
<Icon name="chevron_left" size={36} color="var(--fos-text-30)" />
```

Icons are outlined (`fill={0}`) everywhere except the favorite star (`fill={1}`, `color="var(--fos-favorite)"`) and a selected bottom-nav icon (black on the accent pill). Sizes are device pixels: 36 in a top bar or row chevron, 40 in a menu row, 44 in a setting row, 68 for an app icon in a notification, 112 for an empty-state glyph. `AutoMirrored` icons in the source mean the back arrow mirrors in RTL — use `"arrow_forward"` for back in Hebrew, and note the row chevron is `"chevron_left"` and does **not** mirror, because left is forward.
