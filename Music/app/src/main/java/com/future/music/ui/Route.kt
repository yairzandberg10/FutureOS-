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
}
