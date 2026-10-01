# Music (מוזיקה)

`com.future.music` — Local music player.

Local `MediaStore.Audio` player — no streaming service and no radio; the library is the songs already on the device. The one network feature is **Jam** (below), which is why the app now holds `INTERNET`. Built on `androidx.media3`: `MusicPlaybackService` is a `MediaSessionService` foreground service owning a real `ExoPlayer`, `PlayerController` wraps a `MediaController` client as Compose state. Real `android.media.audiofx.Equalizer` bound to the player's session (4 presets: normal/bass/treble/vocal). Favorites/playlists/last-queue-position persisted as JSON in `SharedPreferences`. Opens directly into the NowPlaying screen; the song/artist/album/playlist menu is reached via a header button and the FutureUI-broadcast Options-key path (see [FutureUI](../FutureUI/)) rather than the physical Menu key, which FutureUI's status bar service reserves system-wide.


## Jam (ג'אם) — shared listening, like Spotify Premium's Jam

A host opens a jam and gets a 6-digit code; friends join with the code; everyone adds songs from **their own** phone to one shared queue, and every member hears the same song in sync on their own device (or turns "listen on this device" off and acts as a remote, when everyone is next to the host's speaker). The host controls playback and can let everyone control it.

Runs on the **same Firebase project as Messages' FutureOS chat** — see [`Messages/firebase/README.md`](../Messages/firebase/README.md) for the one-time setup (add `com.future.music` to the project, enable Anonymous sign-in, deploy). Without a `google-services.json` that knows `com.future.music`, the Firebase plugin is not applied and the Jam screen only explains the setup; everything else works as before.

| Piece | Where |
|---|---|
| Firebase access | `jam/JamBackend.kt` — anonymous Auth, Firestore, Storage |
| Session engine | `jam/JamSession.kt` — process-wide (keeps going with the screen off or the app closed), its own `MediaController` to `MusicPlaybackService` |
| Screens | `ui/screens/JamScreen.kt` — start/join, the jam, add songs, members |
| Rules + cleanup | `Messages/firebase/firestore.rules`, `storage.rules`, functions `jamCleanup` / `jamExpiry` |

How it works:

- **Shared state** is one Firestore document: current queue item, playing, position, and the server time of that position. Each device computes "where are we now" from it, so nothing is written every second. Server time comes from a one-off round trip (`estimateClockOffset`); devices more than 1.5 s off seek back in line.
- **Songs** are uploaded to Storage when queued (one at a time, up to 40 MB each). The device that added a song plays it from its own file; everyone else streams it. A song plays for the group only once it has finished uploading.
- **The host advances** to the next song when the current one ends (or by the clock, if the host's device isn't playing the jam). Media-notification and headset pause on the host pauses everyone; on a guest it pauses only that guest.
- **During a jam**, OK on a song anywhere in the library adds it to the queue instead of replacing playback, and the Now Playing controls (5 / 4 / 6 / ← →) drive the jam.
- **Ending**: the host's "end jam" deletes the jam document; `jamCleanup` removes members, queue and files. `jamExpiry` removes any jam older than 12 hours.

Keys on the jam screen: `1` add songs · `2` members · `3` settings (listen on this device, everyone controls, open to join, leave/end) · `4`/`5`/`6` previous / play-pause / next · `0` leave or end · OK on a queued song → play now, move up/down, remove. `0` on Now Playing (or the group icon in its top bar) and `0` on the main menu open the jam.
