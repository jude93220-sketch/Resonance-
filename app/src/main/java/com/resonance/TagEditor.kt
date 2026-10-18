package com.resonance

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore

object TagEditor {
    fun update(context: Context, song: Song, title: String, artist: String, album: String, genre: String): Boolean {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.TITLE, title.trim())
                put(MediaStore.Audio.Media.ARTIST, artist.trim())
                put(MediaStore.Audio.Media.ALBUM, album.trim())
                if (genre.isNotBlank()) put(MediaStore.Audio.Media.GENRE, genre.trim())
            }
            context.contentResolver.update(song.uri, values, null, null) > 0
        } catch (_: SecurityException) {
            false
        } catch (_: Throwable) {
            false
        }
    }
}
