package com.resonance

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class SmartPlaylist(
    val id: String,
    val name: String,
    val genre: String = "",
    val artist: String = "",
    val album: String = "",
    val minDurationMs: Long = 0,
    val maxDurationMs: Long = 0,
    val minPlays: Int = 0,
    val likedOnly: Boolean = false,
    val addedWithinDays: Int = 0,
)

object SmartPlaylistStore {
    private const val KEY = "smart_playlists"
    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("smart_playlists_store", Context.MODE_PRIVATE)

    fun all(context: Context): List<SmartPlaylist> = try {
        val json = JSONArray(prefs(context).getString(KEY, "[]"))
        List(json.length()) { i ->
            val o = json.getJSONObject(i)
            SmartPlaylist(
                id = o.getString("id"), name = o.getString("name"),
                genre = o.optString("genre"), artist = o.optString("artist"), album = o.optString("album"),
                minDurationMs = o.optLong("minDurationMs"), maxDurationMs = o.optLong("maxDurationMs"),
                minPlays = o.optInt("minPlays"), likedOnly = o.optBoolean("likedOnly"),
                addedWithinDays = o.optInt("addedWithinDays"),
            )
        }
    } catch (_: Throwable) { emptyList() }

    private fun save(context: Context, values: List<SmartPlaylist>) {
        val json = JSONArray()
        values.forEach { p ->
            json.put(JSONObject().apply {
                put("id", p.id); put("name", p.name); put("genre", p.genre); put("artist", p.artist)
                put("album", p.album); put("minDurationMs", p.minDurationMs); put("maxDurationMs", p.maxDurationMs)
                put("minPlays", p.minPlays); put("likedOnly", p.likedOnly); put("addedWithinDays", p.addedWithinDays)
            })
        }
        prefs(context).edit().putString(KEY, json.toString()).apply()
    }

    fun add(context: Context, value: SmartPlaylist) {
        save(context, all(context).filterNot { it.id == value.id } + value)
    }

    fun delete(context: Context, id: String) = save(context, all(context).filterNot { it.id == id })

    fun evaluate(context: Context, rule: SmartPlaylist, songs: List<Song>): List<Song> {
        val counts = PlayStats.counts(context)
        val liked = RatingManager.likedIds(context)
        val cutoff = if (rule.addedWithinDays > 0) {
            System.currentTimeMillis() / 1000 - rule.addedWithinDays * 86_400L
        } else 0L
        return songs.filter { song ->
            (rule.genre.isBlank() || song.genre.equals(rule.genre, true)) &&
                (rule.artist.isBlank() || song.artist.equals(rule.artist, true)) &&
                (rule.album.isBlank() || song.album.equals(rule.album, true)) &&
                (rule.minDurationMs == 0L || song.duration >= rule.minDurationMs) &&
                (rule.maxDurationMs == 0L || song.duration <= rule.maxDurationMs) &&
                ((counts[song.id] ?: 0) >= rule.minPlays) &&
                (!rule.likedOnly || song.id in liked) &&
                (cutoff == 0L || song.dateAdded >= cutoff)
        }
    }
}
