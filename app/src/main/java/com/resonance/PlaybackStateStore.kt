package com.resonance

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.json.JSONArray
import org.json.JSONObject

/** Persists the local playback session so the player can resume after process death. */
object PlaybackStateStore {
    private const val PREFS = "playback_state"
    private const val KEY_ITEMS = "items"
    private const val KEY_INDEX = "index"
    private const val KEY_POSITION = "position"
    private const val KEY_PLAYING = "playing"
    private const val KEY_SHUFFLE = "shuffle"
    private const val KEY_REPEAT = "repeat"

    data class State(
        val items: List<MediaItem>,
        val index: Int,
        val positionMs: Long,
        val playing: Boolean,
        val shuffle: Boolean,
        val repeatMode: Int,
    )

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(
        context: Context,
        playerItems: List<MediaItem>,
        index: Int,
        positionMs: Long,
        playing: Boolean,
        shuffle: Boolean,
        repeatMode: Int,
    ) {
        val items = JSONArray()
        playerItems.forEach { item ->
            val metadata = item.mediaMetadata
            items.put(JSONObject().apply {
                put("id", item.mediaId)
                put("uri", item.localConfiguration?.uri?.toString() ?: "")
                put("title", metadata.title?.toString() ?: "")
                put("artist", metadata.artist?.toString() ?: "")
                put("album", metadata.albumTitle?.toString() ?: "")
            })
        }
        prefs(context).edit()
            .putString(KEY_ITEMS, items.toString())
            .putInt(KEY_INDEX, index.coerceAtLeast(0))
            .putLong(KEY_POSITION, positionMs.coerceAtLeast(0L))
            .putBoolean(KEY_PLAYING, playing)
            .putBoolean(KEY_SHUFFLE, shuffle)
            .putInt(KEY_REPEAT, repeatMode)
            .apply()
    }

    fun load(context: Context): State? {
        val raw = prefs(context).getString(KEY_ITEMS, null) ?: return null
        return try {
            val array = JSONArray(raw)
            if (array.length() == 0) return null
            val items = buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val uri = item.optString("uri")
                    if (uri.isBlank()) continue
                    add(
                        MediaItem.Builder()
                            .setMediaId(item.optString("id"))
                            .setUri(uri)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(item.optString("title"))
                                    .setArtist(item.optString("artist"))
                                    .setAlbumTitle(item.optString("album"))
                                    .build()
                            )
                            .build()
                    )
                }
            }
            if (items.isEmpty()) return null
            State(
                items = items,
                index = prefs(context).getInt(KEY_INDEX, 0).coerceIn(0, items.lastIndex),
                positionMs = prefs(context).getLong(KEY_POSITION, 0L).coerceAtLeast(0L),
                playing = prefs(context).getBoolean(KEY_PLAYING, false),
                shuffle = prefs(context).getBoolean(KEY_SHUFFLE, false),
                repeatMode = prefs(context).getInt(KEY_REPEAT, 0),
            )
        } catch (_: Throwable) {
            null
        }
    }
}
