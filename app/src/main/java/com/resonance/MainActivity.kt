package com.resonance

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color.Black, surface = Color.Black)) {
                val context = LocalContext.current
                var hasPermission by remember { mutableStateOf(false) }
                var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
                var currentSong by remember { mutableStateOf<Song?>(null) }
                var showPlayer by remember { mutableStateOf(false) }

                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasPermission = isGranted
                    if (isGranted) {
                        songs = MusicScanner.getSongs(context)
                        if (currentSong == null) currentSong = songs.firstOrNull()
                    }
                }

                LaunchedEffect(Unit) {
                    val alreadyGranted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                    if (alreadyGranted) {
                        hasPermission = true
                        songs = MusicScanner.getSongs(context)
                        if (currentSong == null) currentSong = songs.firstOrNull()
                    } else {
                        permissionLauncher.launch(permission)
                    }
                }

                val exoPlayer = remember { ExoPlayer.Builder(context).build() }

                LaunchedEffect(currentSong) {
                    if (currentSong != null) {
                        exoPlayer.setMediaItem(MediaItem.fromUri(currentSong!!.uri))
                        exoPlayer.prepare()
                    }
                }

                var isPlaying by remember { mutableStateOf(false) }
                var progress by remember { mutableStateOf(0f) }
                var elapsedText by remember { mutableStateOf("0:00") }

                LaunchedEffect(Unit) {
                    while (true) {
                        isPlaying = exoPlayer.isPlaying
                        val current = exoPlayer.currentPosition.coerceAtLeast(0L)
                        val total = exoPlayer.duration.coerceAtLeast(1L)
                        progress = if (total > 0) current.toFloat() / total.toFloat() else 0f
                        elapsedText = formatDuration(current)
                        delay(500)
                    }
                }

                val togglePlayPause = {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                }

                if (showPlayer && currentSong != null) {
                    NowPlayingScreen(
                        songId = currentSong!!.id,
                        title = currentSong!!.title,
                        artist = currentSong!!.artist,
                        duration = formatDuration(currentSong!!.duration),
                        elapsed = elapsedText,
                        isPlaying = isPlaying,
                        progress = progress,
                        onPlayPause = { togglePlayPause() },
                        onSeek = { newProgress ->
                            val targetMs = (exoPlayer.duration.toFloat() * newProgress).toLong()
                            exoPlayer.seekTo(targetMs)
                        },
                        onBack = { showPlayer = false }
                    )
                } else {
                    LibraryScreen(
                        songs = songs,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        onSongSelect = { selectedSong ->
                            currentSong = selectedSong
                            exoPlayer.setMediaItem(MediaItem.fromUri(selectedSong.uri))
                            exoPlayer.prepare()
                            exoPlayer.play()
                            showPlayer = true
                        },
                        onOpenPlayer = { showPlayer = true },
                        onPlayPause = { togglePlayPause() }
                    )
                }
            }
        }
    }

    private fun formatDuration(ms: Long): String {
        val safeMs = ms.coerceAtLeast(0L)
        val totalSeconds = safeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}
