# Control Center Movable Toggles and Grid Fixes Plan

This plan addresses the requirement to make bottom toggles movable, fix the grid's "add" icons and separator visibility, and refine the long-press interaction for edit mode.

## User Review Required

> [!IMPORTANT]
> **Movable Toggles**: The Bluetooth and Wi-Fi toggles will no longer be a fixed section at the bottom. They will be integrated into the `sectionOrder`, allowing them to be rearranged like any other component.
> **Grid Interaction**: Long-press on the grid card (via touch or D-pad OK) will activate a "Red Border" edit mode. In this mode, short clicks will add/remove icons.

## Proposed Changes

### Dynamic Layout & Sections
#### [MODIFY] [ControlCenter.kt](file:///C:/Users/yairz/AndroidStudioProjects/FutureUI/app/src/main/java/com/future/futureui/ControlCenter.kt)
- Remove the hardcoded "Bottom Toggles" block at the end of the main `Column`.
- Ensure the `sectionOrder` loop handles all movable sections, including the toggle pills.

### Grid UI & Logic Fixes
#### [MODIFY] [ControlCenter.kt](file:///C:/Users/yairz/AndroidStudioProjects/FutureUI/app/src/main/java/com/future/futureui/ControlCenter.kt)
- **Visibility**: Make the separator line more prominent (higher alpha) and ensure "Add" icons are rendered clearly above it when `isGridEditing` is true.
- **Pointer Input**: Use a more robust `pointerInput` block on the card to capture long presses even if children are clickable (possibly using `pass-through` or adjusting inner clickables).
- **Key Events**: Implement a simple timer-based long-press for the D-pad Center key if standard focus events don't suffice.

### Section Handling
#### [MODIFY] [ControlLayoutManager.kt](file:///C:/Users/yairz/AndroidStudioProjects/FutureUI/app/src/main/java/com/future/futureui/ControlLayoutManager.kt)
- (Optional) Adjust `defaultSectionOrder` if needed to ensure "toggles" or a similar identifier is present for the movable pills.

## Verification Plan

### Automated Tests
- Use `render_compose_preview` with `isGridEditing = true` to verify the separator and "Add" icons visibility.

### Manual Verification
- Rearrange the toggle pills section and verify it stays in the new position.
- Long-press the grid to enter red-border mode.
- Short-click to add/remove icons in that mode.
