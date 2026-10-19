package com.resonance

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

object MusicScanner {

    private fun text(c: Cursor, index: Int): String? =
        if (index >= 0 && !c.isNull(index)) c.getString(index) else null

    private fun clean(value: String?, fallback: String): String =
        value?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: fallback

    @Suppress("DEPRECATION")
    fun getSongs(context: Context): List<Song> {
        val songs = ArrayList<Song>()
        val excludedFolders = AppSettings.excludedFolders(context)
        val selectedRoots = AppSettings.scanFolderUris(context)
            .mapNotNull(AppSettings::scanFolderPath)
            .map { it.trimEnd('/') }
        // Unlike the old global scan, an empty selection intentionally means an empty library.
        // The user must choose at least one Music Folder, preventing notification/WhatsApp audio
        // from entering the library by accident.
        if (selectedRoots.isEmpty()) return emptyList()
        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val columns = arrayListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.COMPOSER,
            MediaStore.Audio.Media.DATE_ADDED,
        )
        val hasGenre = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        if (hasGenre) {
            columns.add(MediaStore.Audio.Media.GENRE)
            columns.add(MediaStore.Audio.Media.ALBUM_ARTIST)
        }

        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ${AppSettings.minTrackSeconds(context) * 1000L}"
        val sort = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        context.contentResolver.query(
            collection, columns.toTypedArray(), selection, null, sort
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndex(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
            val durCol = c.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val dataCol = c.getColumnIndex(MediaStore.Audio.Media.DATA)
            val trackCol = c.getColumnIndex(MediaStore.Audio.Media.TRACK)
            val mimeCol = c.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
            val yearCol = c.getColumnIndex(MediaStore.Audio.Media.YEAR)
            val composerCol = c.getColumnIndex(MediaStore.Audio.Media.COMPOSER)
            val addedCol = c.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
            val genreCol = if (hasGenre) c.getColumnIndex(MediaStore.Audio.Media.GENRE) else -1
            val albumArtistCol =
                if (hasGenre) c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST) else -1

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val path = text(c, dataCol) ?: ""
                if (selectedRoots.none { path == it || path.startsWith("$it/") }) continue
                if (excludedFolders.any { path == it || path.startsWith("$it/") }) continue
                val artist = clean(text(c, artistCol), "Unknown artist")
                val uri = ContentUris.withAppendedId(collection, id)
                val gain = if (AppSettings.replayGainMode(context) > 0) {
                    ReplayGain.read(context, uri)
                } else {
                    ReplayGain.Values(null, null)
                }
                songs.add(
                    Song(
                        id = id,
                        title = clean(text(c, titleCol), "Unknown"),
                        artist = artist,
                        album = clean(text(c, albumCol), "Unknown album"),
                        albumId = if (albumIdCol >= 0) c.getLong(albumIdCol) else 0L,
                        duration = if (durCol >= 0) c.getLong(durCol) else 0L,
                        uri = uri,
                        path = path,
                        folder = path.substringBeforeLast('/', ""),
                        trackNumber = if (trackCol >= 0) c.getInt(trackCol) % 1000 else 0,
                        mime = text(c, mimeCol) ?: "",
                        year = if (yearCol >= 0) c.getInt(yearCol) else 0,
                        genre = clean(text(c, genreCol), ""),
                        composer = clean(text(c, composerCol), ""),
                        albumArtist = clean(text(c, albumArtistCol), artist),
                        dateAdded = if (addedCol >= 0) c.getLong(addedCol) else 0L,
                        replayGainTrackDb = gain.trackDb,
                        replayGainAlbumDb = gain.albumDb,
                    )
                )
            }
        }
        return songs
    }
}
