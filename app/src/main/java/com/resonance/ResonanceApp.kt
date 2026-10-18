package com.resonance

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private sealed interface Route {
    object Home : Route
    class Groups(val kind: GroupKind) : Route
    class FolderTree(val path: String) : Route
    data class Songs(
        val title: String,
        val songs: List<Song>,
        val icon: ImageVector,
        val playlistId: String? = null,
        val alphabetical: Boolean = false,
    ) : Route
    object Playlists : Route
    object Queue : Route
    object Search : Route
    object Eq : Route
    object Settings : Route
    object Player : Route
}

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
    var scanTick by remember { mutableIntStateOf(0) }
    var likedIds by remember { mutableStateOf(RatingManager.likedIds(context)) }
    var dislikedIds by remember { mutableStateOf(RatingManager.dislikedIds(context)) }
    var playlists by remember { mutableStateOf(PlaylistStore.all(context)) }
    var sleepEnd by remember { mutableLongStateOf(AudioPrefs.sleepEnd(context)) }
    val stack = remember { mutableStateListOf<Route>(Route.Home) }
    var showMenu by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }
    var showPlayerMenu by remember { mutableStateOf(false) }
    var playlistTarget by remember { mutableStateOf<List<Long>?>(null) }
    var folderMenuSong by remember { mutableStateOf<Song?>(null) }
    var settingsTick by remember { mutableIntStateOf(0) }
    var exportTarget by remember { mutableStateOf<Playlist?>(null) }
    val smartPlaylists = remember(songs, settingsTick) {
        SmartPlaylistStore.all(context).map { rule ->
            Playlist(
                id = "smart:${rule.id}",
                name = "${rule.name} · Smart",
                songIds = SmartPlaylistStore.evaluate(context, rule, songs).map { it.id },
            )
        }
    }

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
            songs = withContext(Dispatchers.IO) { MusicScanner.getSongs(context) }
        }
    }

    // Keep the progress bar moving
    LaunchedEffect(Unit) {
        while (true) {
            remote.refreshPosition()
            delay(250)
        }
    }

    val songById = remember(songs) { songs.associateBy { it.id } }
    val importPlaylistLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val name = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Imported playlist"
            val count = PlaylistStore.importM3u(context, uri, name, songs)
            playlists = PlaylistStore.all(context)
            toast("Imported $count matching songs")
        }
    }
    val exportPlaylistLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/x-mpegurl"),
    ) { uri ->
        val target = exportTarget
        if (uri != null && target != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                    it.write(PlaylistStore.toM3u(target, songById))
                }
                toast("Playlist exported")
            } catch (_: Throwable) {
                toast("Could not export playlist")
            }
        }
        exportTarget = null
    }
    val current = songById[remote.currentId]
    val tint = rememberArtTint(current)

    val keepOn = remember(settingsTick) { AppSettings.keepScreenOn(context) }
    val hostView = LocalView.current
    LaunchedEffect(keepOn) { hostView.keepScreenOn = keepOn }
    val bgIntensity = remember(settingsTick) { AppSettings.bgIntensity(context) }
    val bgGradient = remember(settingsTick) { AppSettings.bgGradient(context) }
    val palette = remember(settingsTick) { ThemeStore.theme(context) }
    val visualizerMode = remember(settingsTick) { ThemeStore.visualizer(context) }

    fun push(r: Route) {
        stack.add(r)
    }

    fun pop() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun reset(vararg routes: Route) {
        stack.clear()
        routes.forEach { stack.add(it) }
    }

    fun toast(text: String) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    BackHandler(enabled = stack.size > 1) { pop() }

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

    val route = stack.last()
    val bgTint = lerp(palette.background, tint, bgIntensity / 100f)
    val bg = if (bgGradient == 0) {
        Brush.verticalGradient(listOf(bgTint, bgTint))
    } else {
        Brush.verticalGradient(listOf(bgTint, lerp(bgTint, palette.background, bgGradient / 9f), palette.background))
    }

    val playList: (List<Song>, Int) -> Unit = { list, index -> remote.playQueue(list, index) }
    val shuffleList: (List<Song>) -> Unit = { list -> remote.playShuffled(list) }
    val playNext: (List<Song>) -> Unit = { list ->
        remote.playNext(list)
        toast("Will play next")
    }
    val addToQueue: (List<Song>) -> Unit = { list ->
        remote.addToQueue(list)
        toast("Added to queue")
    }
    val addToPlaylist: (List<Song>) -> Unit = { list -> playlistTarget = list.map { it.id } }

    CompositionLocalProvider(LocalResonancePalette provides palette) {
    Column(
        Modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (route) {
                is Route.Home -> {
                    val entries = listOf(
                        HomeEntry("All Songs", Icons.Filled.MusicNote, Color(0xFF6C7FD8), null) {
                            push(Route.Songs("All Songs", songs, Icons.Filled.MusicNote, alphabetical = true))
                        },
                        HomeEntry("Folders", groupIcon(GroupKind.FOLDERS), Color(0xFF4A86D8), null) {
                            push(Route.Groups(GroupKind.FOLDERS))
                        },
                        HomeEntry("Folders Hierarchy", Icons.Filled.FolderOpen, Color(0xFF4A5BE0), null) {
                            push(Route.FolderTree(commonRoot(songs)))
                        },
                        HomeEntry("Albums", groupIcon(GroupKind.ALBUMS), Color(0xFF5B5BC0), null) {
                            push(Route.Groups(GroupKind.ALBUMS))
                        },
                        HomeEntry("Artists", groupIcon(GroupKind.ARTISTS), Color(0xFF6A5F9A), null) {
                            push(Route.Groups(GroupKind.ARTISTS))
                        },
                        HomeEntry("Album Artists", groupIcon(GroupKind.ALBUM_ARTISTS), Color(0xFF7A6BAA), null) {
                            push(Route.Groups(GroupKind.ALBUM_ARTISTS))
                        },
                        HomeEntry("Genres", groupIcon(GroupKind.GENRES), Color(0xFF9A4F8A), null) {
                            push(Route.Groups(GroupKind.GENRES))
                        },
                        HomeEntry("Years", groupIcon(GroupKind.YEARS), Color(0xFF4F9AA8), null) {
                            push(Route.Groups(GroupKind.YEARS))
                        },
                        HomeEntry("Composers", groupIcon(GroupKind.COMPOSERS), Color(0xFF2F8F5A), null) {
                            push(Route.Groups(GroupKind.COMPOSERS))
                        },
                        HomeEntry("Playlists", Icons.Filled.QueueMusic, Color(0xFF9A5A4A), null) {
                            playlists = PlaylistStore.all(context)
                            push(Route.Playlists)
                        },
                        HomeEntry(
                            "Queue", Icons.Filled.PlaylistPlay, Color(0xFF9A8A3A),
                            if (remote.queueSize > 0) "${remote.currentIndex + 1} / ${remote.queueSize}" else null
                        ) {
                            push(Route.Queue)
                        },
                        HomeEntry("Most Played", Icons.Filled.TrendingUp, Color(0xFFB07FD0), null) {
                            val counts = PlayStats.counts(context)
                            val list = songs.filter { (counts[it.id] ?: 0) > 0 }
                                .sortedByDescending { counts[it.id] ?: 0 }
                            push(Route.Songs("Most Played", list, Icons.Filled.TrendingUp))
                        },
                        HomeEntry("Top Rated", Icons.Filled.ThumbUp, Color(0xFF5A6A7A), null) {
                            push(Route.Songs("Top Rated", songs.filter { it.id in likedIds }, Icons.Filled.ThumbUp))
                        },
                        HomeEntry("Low Rated", Icons.Filled.ThumbDown, Color(0xFF5A5A5A), null) {
                            push(Route.Songs("Low Rated", songs.filter { it.id in dislikedIds }, Icons.Filled.ThumbDown))
                        },
                        HomeEntry("Recently Added", Icons.Filled.Add, Color(0xFF6A7A4A), null) {
                            push(
                                Route.Songs(
                                    "Recently Added",
                                    songs.sortedByDescending { it.dateAdded }.take(100),
                                    Icons.Filled.Add,
                                )
                            )
                        },
                        HomeEntry("Long", Icons.Filled.Schedule, Color(0xFF6A4FC0), null) {
                            push(
                                Route.Songs(
                                    "Long",
                                    songs.filter { it.duration >= 6 * 60_000L }.sortedByDescending { it.duration },
                                    Icons.Filled.Schedule,
                                )
                            )
                        },
                    )
                    LibraryHome(entries)
                }

                is Route.Groups -> {
                    val groups = remember(route.kind, songs) { buildGroups(route.kind, songs) }
                    GroupsScreen(
                        title = route.kind.title,
                        icon = groupIcon(route.kind),
                        groups = groups,
                        onBack = { pop() },
                        onOpen = { g -> push(Route.Songs(g.title, g.songs, g.icon)) },
                    )
                }

                is Route.FolderTree -> FolderTreeScreen(
                    path = route.path,
                    allSongs = songs,
                    current = current,
                    onBack = { pop() },
                    onOpenFolder = { p -> push(Route.FolderTree(p)) },
                    onPlay = playList,
                    onSongMenu = { s -> folderMenuSong = s },
                )

                is Route.Songs -> SongsScreen(
                    title = route.title,
                    icon = route.icon,
                    songs = route.songs,
                    current = current,
                    alphabetical = route.alphabetical,
                    onBack = { pop() },
                    onPlay = playList,
                    onShuffle = shuffleList,
                    onPlayNext = playNext,
                    onAddToQueue = addToQueue,
                    onAddToPlaylist = addToPlaylist,
                    onRemoveFromPlaylist = if (route.playlistId != null) {
                        { s ->
                            PlaylistStore.removeSong(context, route.playlistId, s.id)
                            playlists = PlaylistStore.all(context)
                            stack[stack.lastIndex] = route.copy(songs = route.songs.filter { it.id != s.id })
                        }
                    } else {
                        null
                    },
                    onSearch = { reset(Route.Home, Route.Search) },
                )

                is Route.Playlists -> PlaylistsScreen(
                    playlists = playlists + smartPlaylists,
                    onBack = { pop() },
                    onOpen = { p ->
                        val list = p.songIds.mapNotNull { songById[it] }
                        push(Route.Songs(p.name, list, Icons.Filled.QueueMusic, playlistId = p.id))
                    },
                    onCreate = { name ->
                        PlaylistStore.create(context, name)
                        playlists = PlaylistStore.all(context)
                    },
                    onDelete = { p ->
                        if (p.id.startsWith("smart:")) {
                            SmartPlaylistStore.delete(context, p.id.removePrefix("smart:"))
                            settingsTick++
                        } else {
                            PlaylistStore.delete(context, p.id)
                            playlists = PlaylistStore.all(context)
                        }
                    },
                    onImport = { importPlaylistLauncher.launch(arrayOf("audio/x-mpegurl", "audio/mpegurl", "text/plain")) },
                    onExport = { p ->
                        if (p.id.startsWith("smart:")) toast("Smart playlists cannot be exported yet")
                        else {
                            exportTarget = p
                            exportPlaylistLauncher.launch("${p.name}.m3u")
                        }
                    },
                )

                is Route.Queue -> {
                    val ids = remember(remote.queueVersion) { remote.queueIds() }
                    QueueScreen(
                        queueIds = ids,
                        songById = songById,
                        currentIndex = remote.currentIndex,
                        onBack = { pop() },
                        onJump = { i -> remote.jumpTo(i) },
                        onRemove = { i -> remote.removeAt(i) },
                    )
                }

                is Route.Search -> SearchScreen(
                    songs = songs,
                    current = current,
                    onPlay = playList,
                    onPlayNext = playNext,
                    onAddToQueue = addToQueue,
                    onAddToPlaylist = addToPlaylist,
                )

                is Route.Eq -> EqualizerScreen(onBack = { pop() })

                is Route.Settings -> SettingsScreen(
                    songCount = songs.size,
                    onBack = { pop() },
                    onOpenEqualizer = { reset(Route.Home, Route.Eq) },
                    onRescan = {
                        scanTick++
                        toast("Rescanning your music…")
                    },
                    onChanged = { settingsTick++ },
                )

                is Route.Player -> if (current != null) {
                    PlayerHost(
                        remote = remote,
                        song = current,
                        visualizerMode = visualizerMode,
                        swipeTracks = AppSettings.swipeTracks(context),
                        sleepEnd = sleepEnd,
                        onOpenSleep = { showSleep = true },
                        onMore = { showPlayerMenu = true },
                        onRatingChanged = {
                            likedIds = RatingManager.likedIds(context)
                            dislikedIds = RatingManager.dislikedIds(context)
                        },
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Pick a song from the Library", color = Color(0xFF9A9A9A), fontSize = 15.sp)
                    }
                }
            }
        }

        if (route !is Route.Player && current != null) {
            MiniPlayerHost(remote = remote, song = current, onOpen = { push(Route.Player) })
        }

        val selectedTab = when (route) {
            is Route.Eq -> 1
            is Route.Search -> 2
            is Route.Player -> -1
            is Route.Settings -> -1
            else -> 0
        }
        BottomBar(
            selected = selectedTab,
            onLibrary = { reset(Route.Home) },
            onEq = { reset(Route.Home, Route.Eq) },
            onSearch = { reset(Route.Home, Route.Search) },
            onMenu = { showMenu = true },
        )
    }
    }

    // ------------------------------------------------------------------ dialogs

    val pt = playlistTarget
    if (pt != null) {
        AddToPlaylistDialog(
            songIds = pt,
            onDismiss = { playlistTarget = null },
            onDone = { name ->
                playlists = PlaylistStore.all(context)
                playlistTarget = null
                toast("Added to $name")
            },
        )
    }

    val fm = folderMenuSong
    if (fm != null) {
        SongActionsDialog(
            song = fm,
            onDismiss = { folderMenuSong = null },
            onPlayNext = { playNext(listOf(fm)) },
            onAddToQueue = { addToQueue(listOf(fm)) },
            onAddToPlaylist = { addToPlaylist(listOf(fm)) },
            onSelect = null,
            onRemoveFromPlaylist = null,
        )
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

    if (showPlayerMenu) {
        AlertDialog(
            onDismissRequest = { showPlayerMenu = false },
            confirmButton = { TextButton(onClick = { showPlayerMenu = false }) { Text("Close") } },
            title = { Text("This song") },
            text = {
                Column {
                    TextButton(onClick = {
                        showPlayerMenu = false
                        if (current != null) playlistTarget = listOf(current.id)
                    }) { Text("Add to playlist") }
                    TextButton(onClick = {
                        showPlayerMenu = false
                        push(Route.Queue)
                    }) { Text("Show queue") }
                }
            },
        )
    }

    if (showMenu) {
        AlertDialog(
            onDismissRequest = { showMenu = false },
            confirmButton = { TextButton(onClick = { showMenu = false }) { Text("Close") } },
            title = { Text("Resonance") },
            text = {
                Column {
                    TextButton(onClick = {
                        showMenu = false
                        if (route !is Route.Settings) push(Route.Settings)
                    }) { Text("Settings") }
                    TextButton(onClick = {
                        showMenu = false
                        scanTick++
                        toast("Rescanning your music…")
                    }) { Text("Rescan library") }
                    TextButton(onClick = {
                        showMenu = false
                        showSleep = true
                    }) { Text("Sleep timer") }
                    TextButton(onClick = {
                        showMenu = false
                        reset(Route.Home, Route.Eq)
                    }) { Text("Equalizer") }
                }
            },
        )
    }
}

// ---------------------------------------------------------------------- pieces

/** Reads the fast-changing player state (position etc.) in its own scope so the rest of the app doesn't recompose. */
@Composable
private fun PlayerHost(
    remote: PlayerRemote,
    song: Song,
    visualizerMode: Int,
    swipeTracks: Boolean,
    sleepEnd: Long,
    onOpenSleep: () -> Unit,
    onMore: () -> Unit,
    onRatingChanged: () -> Unit,
) {
    NowPlayingScreen(
        song = song,
        visualizerMode = visualizerMode,
        swipeTracks = swipeTracks,
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
        onOpenSleep = onOpenSleep,
        onMore = onMore,
        onRatingChanged = onRatingChanged,
    )
}

@Composable
private fun MiniPlayerHost(remote: PlayerRemote, song: Song, onOpen: () -> Unit) {
    val duration = if (remote.durationMs > 0) remote.durationMs else song.duration
    val progress = if (duration > 0) (remote.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
            .background(Color(0xFF1B1B1B))
            .padding(top = 10.dp, bottom = 4.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArt(song, 128, Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    song.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${song.artist} - ${song.album}", color = Color(0xFFD0D0D0), fontSize = 13.sp,
                    fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = { remote.togglePlayPause() }) {
                Icon(
                    if (remote.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    "Play or pause", tint = Color.White, modifier = Modifier.size(34.dp),
                )
            }
        }
        SlimSeekBar(progress = progress, onSeek = { remote.seekToFraction(it) })
    }
}

@Composable
private fun SlimSeekBar(progress: Float, onSeek: (Float) -> Unit) {
    val latest by rememberUpdatedState(onSeek)
    var drag by remember { mutableStateOf<Float?>(null) }
    val shown = drag ?: progress

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(30.dp)
            .padding(horizontal = 14.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset -> latest((offset.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> drag = (offset.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = {
                        drag?.let { latest(it) }
                        drag = null
                    },
                    onDragCancel = { drag = null },
                    onHorizontalDrag = { change, _ ->
                        drag = (change.position.x / size.width).coerceIn(0f, 1f)
                    },
                )
            }
    ) {
        val trackHeight = 14.dp.toPx()
        val y = size.height / 2f
        val radius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
        drawRoundRect(
            Color(0xFF2E2E2E),
            topLeft = Offset(0f, y - trackHeight / 2f),
            size = Size(size.width, trackHeight),
            cornerRadius = radius,
        )
        val thumbWidth = 34.dp.toPx()
        val x = (size.width - thumbWidth) * shown
        drawRoundRect(
            Color(0xFF9A9A9A),
            topLeft = Offset(x, y - trackHeight / 2f),
            size = Size(thumbWidth, trackHeight),
            cornerRadius = radius,
        )
    }
}

@Composable
private fun BottomBar(
    selected: Int,
    onLibrary: () -> Unit,
    onEq: () -> Unit,
    onSearch: () -> Unit,
    onMenu: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Color(0xFF1B1B1B)),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        NavIcon(Icons.Filled.Apps, "Library", selected == 0, onLibrary, Modifier.weight(1f))
        NavIcon(Icons.Filled.Equalizer, "Equalizer", selected == 1, onEq, Modifier.weight(1f))
        NavIcon(Icons.Filled.Search, "Search", selected == 2, onSearch, Modifier.weight(1f))
        NavIcon(Icons.Filled.Menu, "Menu", false, onMenu, Modifier.weight(1f))
    }
}

@Composable
private fun NavIcon(
    icon: ImageVector,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier.clickable(onClick = onClick).padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon, description,
            tint = if (selected) Color.White else Color(0xFF8A8A8A),
            modifier = Modifier.size(30.dp),
        )
    }
}

/** Search across the whole library. */
@Composable
private fun SearchScreen(
    songs: List<Song>,
    current: Song?,
    onPlay: (List<Song>, Int) -> Unit,
    onPlayNext: (List<Song>) -> Unit,
    onAddToQueue: (List<Song>) -> Unit,
    onAddToPlaylist: (List<Song>) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var menuSong by remember { mutableStateOf<Song?>(null) }

    val results = remember(songs, query) {
        if (query.isBlank()) {
            emptyList()
        } else {
            songs.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true) ||
                    it.album.contains(query, ignoreCase = true)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        TextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Search songs, artists, albums", color = Color(0xFF8A8A8A)) },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color(0xFF8A8A8A)) },
            shape = RoundedCornerShape(50),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0x55000000),
                unfocusedContainerColor = Color(0x55000000),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Accent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        )
        if (query.isBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Type to search your whole library", color = Color(0xFF9A9A9A), fontSize = 15.sp)
            }
        } else if (results.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No results", color = Color(0xFF9A9A9A), fontSize = 15.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(results, key = { _, s -> s.id }) { index, song ->
                    SongRow(
                        song = song,
                        isCurrent = song.id == current?.id,
                        selected = null,
                        onTap = { onPlay(results, index) },
                        onLong = { menuSong = song },
                    )
                }
            }
        }
    }

    val ms = menuSong
    if (ms != null) {
        SongActionsDialog(
            song = ms,
            onDismiss = { menuSong = null },
            onPlayNext = { onPlayNext(listOf(ms)) },
            onAddToQueue = { onAddToQueue(listOf(ms)) },
            onAddToPlaylist = { onAddToPlaylist(listOf(ms)) },
            onSelect = null,
            onRemoveFromPlaylist = null,
        )
    }
}
