package com.resonance

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore

object TagEditor {
    fun update(
        context: Context,
        song: Song,
        title: String,
        artist: String,
        album: String,
        genre: String,
        trackNumber: String = "",
        year: String = "",
        composer: String = "",
        albumArtist: String = "",
    ): Boolean {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.TITLE, title.trim())
                put(MediaStore.Audio.Media.ARTIST, artist.trim())
                put(MediaStore.Audio.Media.ALBUM, album.trim())
                if (genre.isNotBlank()) put(MediaStore.Audio.Media.GENRE, genre.trim())
                if (trackNumber.isNotBlank()) put(MediaStore.Audio.Media.TRACK, trackNumber.trim().toIntOrNull() ?: 0)
                if (year.isNotBlank()) put(MediaStore.Audio.Media.YEAR, year.trim().toIntOrNull() ?: 0)
                if (composer.isNotBlank()) put(MediaStore.Audio.Media.COMPOSER, composer.trim())
                if (albumArtist.isNotBlank()) put(MediaStore.Audio.Media.ALBUM_ARTIST, albumArtist.trim())
            }
            context.contentResolver.update(song.uri, values, null, null) > 0
        } catch (_: SecurityException) {
            false
        } catch (_: Throwable) {
            false
        }
    }
}
