# Multi-Selection Feature Implementation Plan

The goal is to allow users to select multiple files and folders and perform bulk actions like Delete and Share. This is specifically designed for a T9 keyboard-driven device (no touch).

## User Review Required

> [!IMPORTANT]
> Since the device uses a T9 keyboard, I've chosen the **'#' (Pound)** key as the shortcut to toggle selection for the currently focused item. Alternatively, a long-press on the **OK (Center DPAD)** key can be used if preferred. I will implement both if possible or stick to a clear shortcut.

> [!NOTE]
> Sharing multiple items will focus on files. Folders might be excluded from the sharing intent as standard Android sharing primarily handles file URIs.

## Proposed Changes

### [app module]

#### [MODIFY] [FilesScreen.kt](file:///C:/Users/yairz/Downloads/FutureOS-master/FutureOS-master/Files/app/src/main/java/com/future/files/ui/FilesScreen.kt)
- Add `selectedEntries` state (Set of `FileEntry`).
- Update `FilesScreen` parameters to include bulk action callbacks.
- Modify `FileRow` to:
    - Display a selection indicator (e.g., a checkmark icon or distinct border).
    - Handle selection toggle via key events (e.g., `#` key).
- Implement a conditional top bar:
    - If `selectedEntries` is empty, show the regular top bar.
    - If `selectedEntries` is NOT empty, show a selection top bar with:
        - "Selected: X" count.
        - Bulk Share button.
        - Bulk Delete button.
        - Clear selection button.
- Ensure the "OK" key toggles selection if already in selection mode, or opens the entry if not.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/yairz/Downloads/FutureOS-master/FutureOS-master/Files/app/src/main/java/com/future/files/MainActivity.kt)
- Implement `onDeleteMultiple`: Shows a confirmation dialog and then deletes all selected items in a background coroutine.
- Implement `onShareMultiple`: Uses `Intent.ACTION_SEND_MULTIPLE` with a list of file URIs.

## Verification Plan

### Automated Tests
- I will check if the build passes after changes.

### Manual Verification
- Deploy to the device/emulator.
- Navigate the list using DPAD.
- Press `#` on multiple items to select them.
- Verify selection indicators appear.
- Perform "Delete" and confirm all selected items are removed.
- Perform "Share" and verify the share sheet opens with multiple items (if supported by system apps).
- Press "Back" or the "Clear" button to exit selection mode.
