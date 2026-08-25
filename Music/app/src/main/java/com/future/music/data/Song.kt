package com.future.music.data

import android.net.Uri

data class Song(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
)

data class Playlist(val id: Long, val name: String, val songIds: List<Long>)

data class ArtistGroup(val name: String, val songCount: Int)

data class AlbumGroup(val albumId: Long, val name: String, val artist: String, val songCount: Int)
