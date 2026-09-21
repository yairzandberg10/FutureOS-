A filter chip in a horizontal row; gallery categories, calendar views, tool groups.

```jsx
<Chip state="selected">הכל</Chip>
<Chip state="focused">תמונות</Chip>
<Chip>סרטונים</Chip>
```

Chips sit in a scrolling row with an 8dp gap, gutters at 16dp, and **left is the next chip** — the left arrow key advances. Selected is a solid accent fill with black text and outranks focus; a chip can be both, in which case selected wins and focus adds nothing. The source has two local variants with slightly different vertical padding (`CategoryChip` in Tools and Remote); this is the canonical one.
