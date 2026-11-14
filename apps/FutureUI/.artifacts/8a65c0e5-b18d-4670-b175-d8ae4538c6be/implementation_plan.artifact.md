# Fix Unresolved Reference Errors in ControlCenter.kt

The project build is failing due to the use of incorrect property names for shadow colors within the `graphicsLayer` scope in `ControlCenter.kt`. Specifically, `spotColor` and `ambientColor` are being used instead of the correct `spotShadowColor` and `ambientShadowColor`.

## Proposed Changes

### [UI Components]

#### [MODIFY] [ControlCenter.kt](file:///C:/Users/yairz/AndroidStudioProjects/FutureUI/app/src/main/java/com/future/futureui/ControlCenter.kt)

Rename incorrect shadow color properties in `graphicsLayer` blocks:
- Replace `spotColor` with `spotShadowColor`
- Replace `ambientColor` with `ambientShadowColor`

This will be applied in three locations:
1. `MediaControlButton`
2. `Modifier.focusEffect`
3. `HeaderActionButton`

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to verify the build error is resolved.
