package com.resonance

import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlin.random.Random

fun Song.toMediaItem(): MediaItem {
    val art: Uri = ContentUris.withAppendedId(
        Uri.parse("content://media/external/audio/albumart"), albumId
    )
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(art)
                .build()
        )
        .build()
}

/**
 * The UI's handle on the background player. Talks to PlaybackService through a MediaController
 * and exposes the player state as Compose state.
 */
class PlayerRemote(private val appContext: Context) {

    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var pending: (() -> Unit)? = null

    var isPlaying by mutableStateOf(false)
        private set
    var currentId by mutableLongStateOf(-1L)
        private set
    var shuffle by mutableStateOf(false)
        private set
    var repeatMode by mutableIntStateOf(Player.REPEAT_MODE_OFF)
        private set
    var positionMs by mutableLongStateOf(0L)
        private set
    var durationMs by mutableLongStateOf(0L)
        private set

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            this@PlayerRemote.isPlaying = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            currentId = mediaItem?.mediaId?.toLongOrNull() ?: -1L
            refreshPosition()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            shuffle = shuffleModeEnabled
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            this@PlayerRemote.repeatMode = repeatMode
        }
    }

    fun connect() {
        if (future != null) return
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val f = MediaController.Builder(appContext, token).buildAsync()
        future = f
        f.addListener({
            try {
                val c = f.get()
                controller = c
                c.addListener(listener)
                isPlaying = c.isPlaying
                currentId = c.currentMediaItem?.mediaId?.toLongOrNull() ?: -1L
                shuffle = c.shuffleModeEnabled
                repeatMode = c.repeatMode
                refreshPosition()
                pending?.invoke()
                pending = null
            } catch (t: Throwable) {
                // service could not be reached; UI keeps working, playback just won't start
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    fun disconnect() {
        controller?.removeListener(listener)
        future?.let { MediaController.releaseFuture(it) }
        future = null
        controller = null
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) block(c) else pending = { controller?.let(block) }
    }

    fun refreshPosition() {
        val c = controller ?: return
        positionMs = c.currentPosition.coerceAtLeast(0L)
        val d = c.duration
        durationMs = if (d == C.TIME_UNSET) 0L else d.coerceAtLeast(0L)
        isPlaying = c.isPlaying
    }

    // ---------------------------------------------------------------- commands

    fun playQueue(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        val items = songs.map { it.toMediaItem() }
        withController { c ->
            c.setMediaItems(items, startIndex.coerceIn(0, items.size - 1), 0L)
            c.prepare()
            c.play()
        }
    }

    fun playShuffled(songs: List<Song>) {
        if (songs.isEmpty()) return
        val items = songs.map { it.toMediaItem() }
        withController { c ->
            c.shuffleModeEnabled = true
            c.setMediaItems(items, Random.nextInt(items.size), 0L)
            c.prepare()
            c.play()
        }
    }

    fun togglePlayPause() = withController { c ->
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() = withController { it.seekToNext() }

    fun previous() = withController { it.seekToPrevious() }

    fun seekToFraction(fraction: Float) = withController { c ->
        val d = c.duration
        if (d != C.TIME_UNSET && d > 0) {
            c.seekTo((d * fraction.coerceIn(0f, 1f)).toLong())
            refreshPosition()
        }
    }

    fun skipBy(deltaMs: Long) = withController { c ->
        val d = c.duration
        val max = if (d == C.TIME_UNSET) Long.MAX_VALUE else d
        c.seekTo((c.currentPosition + deltaMs).coerceIn(0L, max))
        refreshPosition()
    }

    fun toggleShuffle() = withController { it.shuffleModeEnabled = !it.shuffleModeEnabled }

    fun cycleRepeat() = withController {
        it.repeatMode = when (it.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }
}
