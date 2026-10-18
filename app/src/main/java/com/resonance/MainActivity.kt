package com.resonance

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(permission)
        }

        val realSongs = MusicScanner.getSongs(this)
        val firstSong = realSongs.firstOrNull()

        // Initialize the audio engine
        val exoPlayer = ExoPlayer.Builder(this).build().apply {
            if (firstSong != null) {
                setMediaItem(MediaItem.fromUri(firstSong.uri))
                prepare()
            }
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color.Black,
                    surface = Color.Black
                )
            ) {
                var isPlaying by remember { mutableStateOf(false) }
                var progress by remember { mutableStateOf(0f) }
                var elapsedText by remember { mutableStateOf("0:00") }

                // Loop to update the UI with the real song progress
                LaunchedEffect(Unit) {
                    while (true) {
                        isPlaying = exoPlayer.isPlaying
                        val current = exoPlayer.currentPosition
                        val total = exoPlayer.duration.coerceAtLeast(1L)
                        
                        // FIX: Changed toFloat() here so Kotlin is happy
                        progress = current.toFloat() / total.toFloat()
                        
                        elapsedText = formatDuration(current)
                        delay(500) // update every half second
                    }
                }

                NowPlayingScreen(
                    title = firstSong?.title ?: "No songs found",
                    artist = firstSong?.artist ?: "Please grant permission",
                    duration = formatDuration(firstSong?.duration ?: 0L),
                    elapsed = elapsedText,
                    isPlaying = isPlaying,
                    progress = progress,
                    onPlayPause = {
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                        } else {
                            exoPlayer.play()
                        }
                    },
                    onSeek = { newProgress ->
                        // FIX: Converted duration to Float before multiplying!
                        val targetMs = (exoPlayer.duration.toFloat() * newProgress).toLong()
                        exoPlayer.seekTo(targetMs)
                    }
                )
            }
        }
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}
