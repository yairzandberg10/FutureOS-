# Implementation Plan - Launcher Enhancements (Revised)

Improve widget management, selection interface, and home screen settings while maintaining the existing layout structure.

## Proposed Changes

### 1. Delete Option
- **[MODIFY] [MainActivity.kt](file:///C:/Users/yairz/AndroidStudioProjects/FutureLauncher/app/src/main/java/com/future/futurelauncher/MainActivity.kt)**:
    - Add `onDelete` logic to `AppOptionsDialog` usage.
    - Logic: Replace item at index with `LauncherItem.Empty()`. If it was a widget, clear all `isOccupiedBy` references.
- **[MODIFY] [AppOptionsDialog](file:///C:/Users/yairz/AndroidStudioProjects/FutureLauncher/app/src/main/java/com/future/futurelauncher/MainActivity.kt#L1010)**:
    - Add a "Delete" button.

### 2. Widget Resizing & Space Check
- **[MODIFY] [AppOptionsDialog](file:///C:/Users/yairz/AndroidStudioProjects/FutureLauncher/app/src/main/java/com/future/futurelauncher/MainActivity.kt#L1010)**:
    - Implement a `canResize(newSpanX, newSpanY)` check.
    - The check will verify if the target slots are within the 4x4 grid and are either empty or occupied by the *same* widget.
    - Update the UI to disable the "+" buttons if no space is available.
    - Ensure local state updates immediately.

### 3. Enhanced Widget Selection Interface
- **[MODIFY] [WidgetsDialog](file:///C:/Users/yairz/AndroidStudioProjects/FutureLauncher/app/src/main/java/com/future/futurelauncher/MainActivity.kt#L1120)**:
    - Instead of just a "Pick Widget" button, fetch the list of installed `AppWidgetProviderInfo`.
    - Display them in a searchable grid with their icons and labels.
    - On selection, proceed with the existing `configureWidget` flow.

### 4. Standard Home Screen Settings
- **[MODIFY] [LauncherState.kt](file:///C:/Users/yairz/AndroidStudioProjects/FutureLauncher/app/src/main/java/com/future/futurelauncher/ui/LauncherState.kt)**:
    - Add settings: `showLabels` (Boolean).
- **[MODIFY] [LauncherSettingsDialog](file:///C:/Users/yairz/AndroidStudioProjects/FutureLauncher/app/src/main/java/com/future/futurelauncher/MainActivity.kt#L1250)**:
    - Add a Switch/Toggle for "Show App Labels".
    - Add a button to "Set current page as Home".
- **[MODIFY] [ItemPanel](file:///C:/Users/yairz/AndroidStudioProjects/FutureLauncher/app/src/main/java/com/future/futurelauncher/MainActivity.kt#L900)**:
    - Respect `launcherState.showLabels` setting.

## User Review Required
> [!IMPORTANT]
> I will **not** change the `LazyVerticalGrid` layout as requested. To fix "problems when putting widgets next to apps" within the current grid, I will improve the `recalculateOccupiedSlots` logic to ensure that apps correctly flow around widgets, and ensure that `spanY` widgets don't break the row alignment by accurately checking for overlapping indices.

## Verification Plan

### Manual Verification
- **Delete**: Long-press app/widget -> Delete -> Verify it disappears.
- **Resize**: Try to expand a widget into a slot occupied by an app -> Button should be disabled or show "No space".
- **Widget Selection**: Open widgets dialog -> See a list of all available widgets with icons -> Pick one -> verify it's added.
- **Settings**: Toggle "Show Labels" -> verify app names disappear/reappear.
