# Music (מוזיקה)

`com.future.music` — Local music player.

Pure local `MediaStore.Audio` player — **no `INTERNET` permission at all**, by explicit product decision (no network streaming/radio, songs already on the device only). Built on `androidx.media3`: `MusicPlaybackService` is a `MediaSessionService` foreground service owning a real `ExoPlayer`, `PlayerController` wraps a `MediaController` client as Compose state. Real `android.media.audiofx.Equalizer` bound to the player's session (4 presets: normal/bass/treble/vocal). Favorites/playlists/last-queue-position persisted as JSON in `SharedPreferences`. Opens directly into the NowPlaying screen; the song/artist/album/playlist menu is reached via a header button and the FutureUI-broadcast Options-key path (see [FutureUI](../FutureUI/)) rather than the physical Menu key, which FutureUI's status bar service reserves system-wide.
