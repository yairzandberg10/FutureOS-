The screen header — title plus up to two 36dp icon buttons; pinned at the top of every screen that isn't the launcher home.

```jsx
<TopBar title="אנשי קשר" onBack={goBack} onMenu={openMenu} />
```

In RTL the back button is at the right edge and the title is right-aligned next to it; overflow goes to the left edge. The title is never truncated in practice because titles are one or two Hebrew words. The bar does not receive focus as a unit — each button focuses individually.
