package com.resonance

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val uri: Uri,
    val path: String,
    val folder: String,
    val trackNumber: Int,
    val mime: String,
    val year: Int,
    val genre: String,
    val composer: String,
    val albumArtist: String,
    val dateAdded: Long,
    val replayGainTrackDb: Float? = null,
    val replayGainAlbumDb: Float? = null,
)

/** Short file type shown in lists, e.g. "mp3". */
val Song.ext: String
    get() {
        val fromPath = path.substringAfterLast('.', "").lowercase()
        return if (fromPath.isNotBlank() && fromPath.length <= 5) fromPath else mime.substringAfter('/')
    }

fun formatTime(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0L) / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
