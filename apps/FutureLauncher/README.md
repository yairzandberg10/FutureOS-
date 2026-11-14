# FutureLauncher

`com.future.futurelauncher` — Home screen / launcher.

Real `PackageManager` app queries, real `AppWidgetHost`/`AppWidgetManager` widget hosting, JSON-backed page/folder/widget layout persisted in `SharedPreferences`. Custom 4×4 grid `Layout`. RTL-correct via `placeRelative()` rather than absolute placement, so D-pad left/right map correctly under mirroring without any explicit `LocalLayoutDirection` override. Wallpaper picker deep-links to the system picker (`ACTION_SET_WALLPAPER`); a "reset layout" action clears saved layout and falls back to alphabetical sorting.
