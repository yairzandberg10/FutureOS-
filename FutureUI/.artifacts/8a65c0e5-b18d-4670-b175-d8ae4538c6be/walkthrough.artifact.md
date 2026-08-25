# Walkthrough - Fixing Shadow Color Property Names

I have fixed the build error `Unresolved reference 'spotColor'` in `ControlCenter.kt`.

## Changes Made

### [UI Components]

#### [ControlCenter.kt](file:///C:/Users/yairz/AndroidStudioProjects/FutureUI/app/src/main/java/com/future/futureui/ControlCenter.kt)

The property names for shadow colors within the `graphicsLayer` scope were corrected:
- `spotColor` was renamed to `spotShadowColor`.
- `ambientColor` was renamed to `ambientShadowColor`.

These corrections were applied in:
1. `MediaControlButton` (line 650)
2. `Modifier.focusEffect` (line 767)
3. `HeaderActionButton` (line 1006)

## Verification Results

### Automated Tests
- Ran `./gradlew :app:compileDebugKotlin` and the build passed successfully.
