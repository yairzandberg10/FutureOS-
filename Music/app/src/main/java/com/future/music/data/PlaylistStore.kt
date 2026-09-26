package com.future.music.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class LastQueueState(val songIds: List<Long>, val index: Int, val positionMs: Long)

/** מועדפים, פלייליסטים, והמשך-מהיכן-שהפסקת - JSON ב-SharedPreferences, בדיוק
 * כמו שה-layout של FutureLauncher נשמר (אין DB מקומי בסוויטה הזו לנתונים
 * קטנים ומובְנים כאלה). */
class PlaylistStore(context: Context) {
    private val prefs = context.getSharedPreferences("music_store", Context.MODE_PRIVATE)

    // JSON פגום (כתיבה שנקטעה, דיסק מלא) זרק קודם חריגה בכל פתיחה - האפליקציה
    // קרסה לתמיד עד מחיקת הנתונים. עכשיו רשומה פגומה נקראת כריקה.
    private fun favoritesSet(): MutableSet<Long> {
        val raw = prefs.getString(KEY_FAVORITES, null) ?: return linkedSetOf()
        val set = linkedSetOf<Long>()
        runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) set.add(arr.getLong(i))
        }
        return set
    }

    private fun saveFavorites(ids: Set<Long>) {
        val arr = JSONArray()
        ids.forEach { arr.put(it) }
        prefs.edit().putString(KEY_FAVORITES, arr.toString()).apply()
    }

    fun getFavoriteIds(): Set<Long> = favoritesSet()

    fun isFavorite(songId: Long): Boolean = songId in favoritesSet()

    fun toggleFavorite(songId: Long) {
        val set = favoritesSet()
        if (!set.remove(songId)) set.add(songId)
        saveFavorites(set)
    }

    fun getPlaylists(): List<Playlist> {
        val raw = prefs.getString(KEY_PLAYLISTS, null) ?: return emptyList()
        val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        val result = mutableListOf<Playlist>()
        for (i in 0 until arr.length()) {
            runCatching {
                val obj = arr.getJSONObject(i)
                val songIdsArr = obj.getJSONArray("songIds")
                val songIds = (0 until songIdsArr.length()).map { songIdsArr.getLong(it) }
                result.add(Playlist(obj.getLong("id"), obj.getString("name"), songIds))
            }
        }
        return result
    }

    private fun savePlaylists(playlists: List<Playlist>) {
        val arr = JSONArray()
        playlists.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            val songIdsArr = JSONArray()
            p.songIds.forEach { songIdsArr.put(it) }
            obj.put("songIds", songIdsArr)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_PLAYLISTS, arr.toString()).apply()
    }

    fun createPlaylist(name: String): Playlist {
        val playlist = Playlist(id = System.currentTimeMillis(), name = name, songIds = emptyList())
        savePlaylists(getPlaylists() + playlist)
        return playlist
    }

    fun renamePlaylist(id: Long, name: String) {
        savePlaylists(getPlaylists().map { if (it.id == id) it.copy(name = name) else it })
    }

    fun deletePlaylist(id: Long) {
        savePlaylists(getPlaylists().filterNot { it.id == id })
    }

    fun addToPlaylist(playlistId: Long, songId: Long) {
        savePlaylists(getPlaylists().map {
            if (it.id == playlistId && songId !in it.songIds) it.copy(songIds = it.songIds + songId) else it
        })
    }

    fun removeFromPlaylist(playlistId: Long, songId: Long) {
        savePlaylists(getPlaylists().map {
            if (it.id == playlistId) it.copy(songIds = it.songIds.filterNot { id -> id == songId }) else it
        })
    }

    fun saveLastQueue(songIds: List<Long>, index: Int, positionMs: Long) {
        val arr = JSONArray()
        songIds.forEach { arr.put(it) }
        prefs.edit()
            .putString(KEY_LAST_QUEUE, arr.toString())
            .putInt(KEY_LAST_INDEX, index)
            .putLong(KEY_LAST_POSITION, positionMs)
            .apply()
    }

    fun getLastQueue(): LastQueueState? {
        val raw = prefs.getString(KEY_LAST_QUEUE, null) ?: return null
        val songIds = runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getLong(it) }
        }.getOrNull().orEmpty()
        if (songIds.isEmpty()) return null
        return LastQueueState(
            songIds = songIds,
            index = prefs.getInt(KEY_LAST_INDEX, 0),
            positionMs = prefs.getLong(KEY_LAST_POSITION, 0L),
        )
    }

    companion object {
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_PLAYLISTS = "playlists"
        private const val KEY_LAST_QUEUE = "last_queue"
        private const val KEY_LAST_INDEX = "last_index"
        private const val KEY_LAST_POSITION = "last_position"
    }
}
