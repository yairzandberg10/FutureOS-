# Reverting to Clean White Design & Fixing Core Functionality

This plan covers reverting the visual design to a clean white look, making Hebrew the default language, and ensuring the calling feature works as expected on the physical keypad device.

## User Review Required

> [!IMPORTANT]
> **Default Language**: Hebrew will be moved to the default `strings.xml` to ensure it is visible regardless of device locale.
> **Design Revert**: The "Liquid Glass" effect will be removed in favor of a solid white/clean background.
> **Focus Border**: Interactive elements will have a 1.5dp border when focused.
> **Real Calling**: I will ensure the call permission is properly handled and the calling intent is correctly fired.

## Proposed Changes

### 1. Localization (Hebrew as Default)
- **[MODIFY] [strings.xml](file:///C:/Users/yairz/AndroidStudioProjects/dialer/app/src/main/res/values/strings.xml)**: Replace contents with the Hebrew strings.
- **[MODIFY] [strings.xml (Hebrew)](file:///C:/Users/yairz/AndroidStudioProjects/dialer/app/src/main/res/values-he/strings.xml)**: (Optional) Can be removed or kept as a duplicate.

### 2. UI Revert & Focus Border
- **[MODIFY] [Theme.kt](file:///C:/Users/yairz/AndroidStudioProjects/dialer/app/src/main/java/com/future/dialer/ui/theme/Theme.kt)**: Revert `background` and `surface` colors to solid, non-transparent colors.
- **[MODIFY] [FocusableItem.kt](file:///C:/Users/yairz/AndroidStudioProjects/dialer/app/src/main/java/com/future/dialer/ui/components/FocusableItem.kt)**:
    - Remove glassmorphism effects (transparency, blurs).
    - Set background to a solid color (e.g., `surface`).
    - Add a **1.5dp border** when `isFocused` is true.
- **[MODIFY] [Screens]**:
    - Remove `LiquidBackground` from `DialpadScreen`, `CallHistoryScreen`, `ContactsScreen`, and `InCallScreen`.
    - Revert `Surface` and `Box` backgrounds to standard Material3 surface colors (remove custom `copy(alpha = ...)`).

### 3. Calling & Permissions Fix
- **[MODIFY] [MainActivity.kt](file:///C:/Users/yairz/AndroidStudioProjects/dialer/app/src/main/java/com/future/dialer/MainActivity.kt)**:
    - Update `requestPermissionLauncher` to check for `Manifest.permission.CALL_PHONE` as well.
    - Ensure `makeRealCall` is robust and handles the case where permission is granted but the intent fails (rare, but good to handle).
    - Remove `window.setBackgroundBlurRadius` (since we're reverting the blurred background design).
    - Update the theme/window flags to a standard opaque background.

## Verification Plan

### Manual Verification
- **Hebrew UI**: Launch the app and verify all text is in Hebrew by default.
- **Clean Design**: Verify the background is solid white (or light gray depending on theme) and the "liquid blobs" are gone.
- **Focus Border**: Use the D-pad to navigate and verify a 1.5dp border appears around focused items.
- **Real Calling**: Dial a number and press the Call key. Verify the phone initiates a real call.
- **Back Key**: Verify the Back key deletes digits in the dialpad before navigating away.
