package com.future.music.ui

/** מסלולי הניווט - מחסנית ידנית (בלי navigation-compose, כמו שאר אפליקציות
 * הסוויטה) כדי לתמוך במעבר חופשי בין רשימות עם הקשר שונה. */
sealed class Route {
    data object Home : Route()
    data object AllSongs : Route()
    data object Artists : Route()
    data class ArtistSongs(val artist: String) : Route()
    data object Albums : Route()
    data class AlbumSongs(val albumId: Long, val name: String) : Route()
    data object Playlists : Route()
    data class PlaylistSongs(val playlistId: Long, val name: String) : Route()
    data object Favorites : Route()
    data object Search : Route()
    data object NowPlaying : Route()
    data object Queue : Route()
    data object Sound : Route()
    data object Devices : Route()
    data class PlaylistAdd(val playlistId: Long, val name: String) : Route()
}

private const val SEP = '\u001F'

/** ייצוג מחרוזתי של מסלול לשמירה. שמות מפורשים (לא simpleName), כי R8 משנה שמות מחלקות. */
internal fun Route.encodeRoute(): String = when (this) {
    Route.Home -> "Home"
    Route.AllSongs -> "AllSongs"
    Route.Artists -> "Artists"
    is Route.ArtistSongs -> "ArtistSongs$SEP$artist"
    Route.Albums -> "Albums"
    is Route.AlbumSongs -> "AlbumSongs$SEP$albumId$SEP$name"
    Route.Playlists -> "Playlists"
    is Route.PlaylistSongs -> "PlaylistSongs$SEP$playlistId$SEP$name"
    Route.Favorites -> "Favorites"
    Route.Search -> "Search"
    Route.NowPlaying -> "NowPlaying"
    Route.Queue -> "Queue"
    Route.Sound -> "Sound"
    Route.Devices -> "Devices"
    is Route.PlaylistAdd -> "PlaylistAdd$SEP$playlistId$SEP$name"
}

internal fun decodeRoute(encoded: String): Route? {
    val parts = encoded.split(SEP, limit = 3)
    return when (parts[0]) {
        "Home" -> Route.Home
        "AllSongs" -> Route.AllSongs
        "Artists" -> Route.Artists
        "ArtistSongs" -> encoded.substringAfter(SEP, "").let { Route.ArtistSongs(it) }
        "Albums" -> Route.Albums
        "AlbumSongs" -> parts.getOrNull(1)?.toLongOrNull()?.let { id -> Route.AlbumSongs(id, parts.getOrElse(2) { "" }) }
        "Playlists" -> Route.Playlists
        "PlaylistSongs" -> parts.getOrNull(1)?.toLongOrNull()?.let { id -> Route.PlaylistSongs(id, parts.getOrElse(2) { "" }) }
        "Favorites" -> Route.Favorites
        "Search" -> Route.Search
        "NowPlaying" -> Route.NowPlaying
        "Queue" -> Route.Queue
        "Sound" -> Route.Sound
        "Devices" -> Route.Devices
        "PlaylistAdd" -> parts.getOrNull(1)?.toLongOrNull()?.let { id -> Route.PlaylistAdd(id, parts.getOrElse(2) { "" }) }
        else -> null
    }
}

