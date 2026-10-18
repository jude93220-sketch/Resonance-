package com.resonance

import android.content.Context
import android.net.Uri
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

data class Playlist(val id: String, val name: String, val songIds: List<Long>)

/** Playlists saved on the phone (simple JSON in app storage). */
object PlaylistStore {
    private const val KEY = "playlists"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("playlists_store", Context.MODE_PRIVATE)

    fun all(context: Context): List<Playlist> {
        val raw = prefs(context).getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val out = ArrayList<Playlist>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val idsJson = o.getJSONArray("songs")
                val ids = ArrayList<Long>()
                for (j in 0 until idsJson.length()) ids.add(idsJson.getLong(j))
                out.add(Playlist(o.getString("id"), o.getString("name"), ids))
            }
            out
        } catch (t: Throwable) {
            emptyList()
        }
    }

    private fun save(context: Context, lists: List<Playlist>) {
        val arr = JSONArray()
        for (p in lists) {
            val o = JSONObject()
            o.put("id", p.id)
            o.put("name", p.name)
            val ids = JSONArray()
            for (id in p.songIds) ids.put(id)
            o.put("songs", ids)
            arr.put(o)
        }
        prefs(context).edit().putString(KEY, arr.toString()).apply()
    }

    fun create(context: Context, name: String): Playlist {
        val p = Playlist(System.currentTimeMillis().toString(), name, emptyList())
        save(context, all(context) + p)
        return p
    }

    fun delete(context: Context, id: String) {
        save(context, all(context).filter { it.id != id })
    }

    fun addSongs(context: Context, id: String, songIds: List<Long>) {
        save(context, all(context).map { p ->
            if (p.id == id) {
                p.copy(songIds = p.songIds + songIds.filter { s -> s !in p.songIds })
            } else {
                p
            }
        })
    }

    fun removeSong(context: Context, id: String, songId: Long) {
        save(context, all(context).map { p ->
            if (p.id == id) p.copy(songIds = p.songIds.filter { it != songId }) else p
        })
    }

    fun toM3u(playlist: Playlist, songsById: Map<Long, Song>): String = buildString {
        appendLine("#EXTM3U")
        playlist.songIds.forEach { id ->
            val song = songsById[id] ?: return@forEach
            appendLine("#EXTINF:${song.duration / 1000},${song.artist} - ${song.title}")
            appendLine(song.path.ifBlank { song.uri.toString() })
        }
    }

    fun importM3u(context: Context, uri: Uri, name: String, songs: List<Song>): Int {
        val lines = try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readLines() }
                ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
        val candidates = lines.filter { it.isNotBlank() && !it.startsWith("#") }
        val normalized = songs.associateBy { it.path.lowercase(Locale.ROOT) }
        val ids = candidates.mapNotNull { line ->
            val direct = normalized[line.trim().lowercase(Locale.ROOT)]
            direct?.id ?: songs.firstOrNull {
                it.uri.toString() == line.trim() ||
                    it.path.substringAfterLast('/').equals(line.trim().substringAfterLast('/'), true)
            }?.id
        }.distinct()
        val playlist = create(context, name.ifBlank { "Imported playlist" })
        if (ids.isNotEmpty()) addSongs(context, playlist.id, ids)
        return ids.size
    }
}
