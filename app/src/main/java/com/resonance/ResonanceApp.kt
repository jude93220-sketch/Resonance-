package com.resonance

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private enum class Screen { LIBRARY, PLAYER, EQ }

@Composable
fun ResonanceApp(remote: PlayerRemote) {
    val context = LocalContext.current

    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED

    var granted by remember { mutableStateOf(hasAudioPermission()) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var scanTick by remember { mutableIntStateOf(0) }
    var screen by remember { mutableStateOf(Screen.LIBRARY) }
    var eqFrom by remember { mutableStateOf(Screen.LIBRARY) }
    var showSleep by remember { mutableStateOf(false) }
    var likedIds by remember { mutableStateOf(RatingManager.likedIds(context)) }
    var sleepEnd by remember { mutableLongStateOf(AudioPrefs.sleepEnd(context)) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted = hasAudioPermission() }

    fun requestPermissions() {
        val wanted = ArrayList<String>()
        if (!hasAudioPermission()) wanted.add(audioPermission)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            wanted.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (wanted.isNotEmpty()) launcher.launch(wanted.toTypedArray())
    }

    LaunchedEffect(Unit) { requestPermissions() }

    LaunchedEffect(granted, scanTick) {
        if (granted) {
            loading = true
            songs = withContext(Dispatchers.IO) { MusicScanner.getSongs(context) }
        }
        loading = false
    }

    // Keep the progress bar moving
    LaunchedEffect(Unit) {
        while (true) {
            remote.refreshPosition()
            delay(250)
        }
    }

    val songById = remember(songs) { songs.associateBy { it.id } }
    val current = songById[remote.currentId]

    BackHandler(enabled = screen != Screen.LIBRARY) {
        screen = if (screen == Screen.EQ) eqFrom else Screen.LIBRARY
    }

    if (!granted) {
        Column(
            Modifier.fillMaxSize().background(Color.Black).padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Resonance needs access to your music files.", color = Color.White, fontSize = 16.sp)
            Button(onClick = { requestPermissions() }, modifier = Modifier.padding(top = 16.dp)) {
                Text("Allow access")
            }
        }
        return
    }

    val effectiveScreen = if (screen == Screen.PLAYER && current == null) Screen.LIBRARY else screen

    when (effectiveScreen) {
        Screen.LIBRARY -> LibraryScreen(
            songs = songs,
            loading = loading,
            current = current,
            isPlaying = remote.isPlaying,
            likedIds = likedIds,
            onPlay = { list, index ->
                remote.playQueue(list, index)
                screen = Screen.PLAYER
            },
            onShuffleAll = { list ->
                remote.playShuffled(list)
                screen = Screen.PLAYER
            },
            onOpenPlayer = { screen = Screen.PLAYER },
            onPlayPause = { remote.togglePlayPause() },
            onNext = { remote.next() },
            onOpenEq = {
                eqFrom = Screen.LIBRARY
                screen = Screen.EQ
            },
            onRescan = { scanTick++ },
        )

        Screen.PLAYER -> if (current != null) {
            PlayerHost(
                remote = remote,
                song = current,
                sleepEnd = sleepEnd,
                onOpenEq = {
                    eqFrom = Screen.PLAYER
                    screen = Screen.EQ
                },
                onOpenSleep = { showSleep = true },
                onRatingChanged = { likedIds = RatingManager.likedIds(context) },
                onBack = { screen = Screen.LIBRARY },
            )
        }

        Screen.EQ -> EqualizerScreen(onBack = { screen = eqFrom })
    }

    if (showSleep) {
        val active = sleepEnd > System.currentTimeMillis()
        AlertDialog(
            onDismissRequest = { showSleep = false },
            confirmButton = { TextButton(onClick = { showSleep = false }) { Text("Close") } },
            title = { Text("Sleep timer") },
            text = {
                Column {
                    listOf(15, 30, 45, 60, 90).forEach { minutes ->
                        TextButton(onClick = {
                            AudioPrefs.startSleep(context, minutes)
                            sleepEnd = AudioPrefs.sleepEnd(context)
                            showSleep = false
                        }) { Text("$minutes minutes") }
                    }
                    if (active) {
                        TextButton(onClick = {
                            AudioPrefs.cancelSleep(context)
                            sleepEnd = 0L
                            showSleep = false
                        }) { Text("Turn timer off") }
                    }
                }
            },
        )
    }
}

/** Reads the fast-changing player state (position etc.) in its own scope so the rest of the app doesn't recompose. */
@Composable
private fun PlayerHost(
    remote: PlayerRemote,
    song: Song,
    sleepEnd: Long,
    onOpenEq: () -> Unit,
    onOpenSleep: () -> Unit,
    onRatingChanged: () -> Unit,
    onBack: () -> Unit,
) {
    NowPlayingScreen(
        song = song,
        isPlaying = remote.isPlaying,
        positionMs = remote.positionMs,
        durationMs = if (remote.durationMs > 0) remote.durationMs else song.duration,
        shuffle = remote.shuffle,
        repeatMode = remote.repeatMode,
        sleepActive = sleepEnd > System.currentTimeMillis(),
        onPlayPause = { remote.togglePlayPause() },
        onPrev = { remote.previous() },
        onNext = { remote.next() },
        onSeekFraction = { remote.seekToFraction(it) },
        onSkipBy = { remote.skipBy(it) },
        onToggleShuffle = { remote.toggleShuffle() },
        onCycleRepeat = { remote.cycleRepeat() },
        onOpenEq = onOpenEq,
        onOpenSleep = onOpenSleep,
        onRatingChanged = onRatingChanged,
        onBack = onBack,
    )
}
