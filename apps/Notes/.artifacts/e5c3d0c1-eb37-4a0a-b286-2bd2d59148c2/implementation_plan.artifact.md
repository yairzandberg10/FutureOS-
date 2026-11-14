# Advanced Notes App (Keys-Only) Implementation Plan

Building an advanced notes application for a 640x960 resolution device that relies entirely on physical keys (D-pad/Keyboard) for interaction.

## User Review Required

> [!IMPORTANT]
> **Interactions:** This app will not support touch. All interactions will be handled via D-pad navigation (Up, Down, Left, Right) and the Center/Enter key. Focus states will be highly prominent.

> [!NOTE]
> **Resolution:** The UI is optimized for 640x960. Layouts will prioritize vertical space and readability on a small screen.

## Proposed Changes

### 1. Build Configuration & Dependencies
Add Room database and Compose Navigation to the project.
- [MODIFY] [libs.versions.toml](file:///C:/Users/yairz/AndroidStudioProjects/notes/gradle/libs.versions.toml)
- [MODIFY] [build.gradle.kts](file:///C:/Users/yairz/AndroidStudioProjects/notes/app/build.gradle.kts)

### 2. Data Layer
Implement Room database for persistent note storage.
- [NEW] `Note.kt`: Entity with id, title, content, timestamp, and pin status.
- [NEW] `NoteDao.kt`: CRUD operations including search and sort.
- [NEW] `NoteDatabase.kt`: Database definition.
- [NEW] `NoteRepository.kt`: Repository for clean data access.

### 3. ViewModel Layer
Handle UI logic, data loading, and focus state management.
- [NEW] `NoteViewModel.kt`: Manages state for list, search, and editing.

### 4. UI Layer (Compose)
Create key-friendly components and screens.
- [NEW] `components/FocusableNoteItem.kt`: Note list item with custom focus border.
- [NEW] `screens/NoteListScreen.kt`: Main list with search and pin sections.
- [NEW] `screens/NoteEditorScreen.kt`: Advanced editor with D-pad friendly text fields.
- [NEW] `ui/theme/Color.kt`: High-contrast colors for focus visibility.
- [MODIFY] `MainActivity.kt`: Set up Navigation and D-pad event handling.

### 5. Focus & Key Handling
- Custom `Modifier` for clear focus visualization.
- `onKeyEvent` handling for global actions (e.g., Back, Menu).
- Automatic focus movement between title and body in the editor.

## Verification Plan

### Automated Tests
- Unit tests for `NoteDao` and `NoteRepository`.
- UI tests for D-pad navigation sequences.

### Manual Verification
- Deploy to an emulator with the specific resolution.
- Verify that every button and text field is reachable via `adb shell input keyevent`.
- Test pinning, search, and editing flows using only keys.
