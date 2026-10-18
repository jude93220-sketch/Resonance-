package com.resonance

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class LibTab(val label: String) {
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    FOLDERS("Folders"),
    LIKED("Liked"),
}

private data class Group(
    val key: String,
    val title: String,
    val subtitle: String,
    val songs: List<Song>,
    val icon: ImageVector,
)

@Composable
fun LibraryScreen(
    songs: List<Song>,
    loading: Boolean,
    current: Song?,
    isPlaying: Boolean,
    likedIds: Set<Long>,
    onPlay: (List<Song>, Int) -> Unit,
    onShuffleAll: (List<Song>) -> Unit,
    onOpenPlayer: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onOpenEq: () -> Unit,
    onRescan: () -> Unit,
) {
    var tab by remember { mutableStateOf(LibTab.SONGS) }
    var query by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<Group?>(null) }

    BackHandler(enabled = detail != null) { detail = null }

    val filtered = remember(songs, query) {
        if (query.isBlank()) songs
        else songs.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
        }
    }
    val albums = remember(filtered) {
        filtered.groupBy { it.album }.map { (name, list) ->
            val sorted = list.sortedWith(compareBy({ it.trackNumber }, { it.title.lowercase() }))
            Group("album:$name", name, "${sorted.first().artist} • ${list.size} songs", sorted, Icons.Filled.Album)
        }.sortedBy { it.title.lowercase() }
    }
    val artists = remember(filtered) {
        filtered.groupBy { it.artist }.map { (name, list) ->
            Group("artist:$name", name, "${list.size} songs", list, Icons.Filled.Person)
        }.sortedBy { it.title.lowercase() }
    }
    val folders = remember(filtered) {
        filtered.groupBy { it.folder }.map { (path, list) ->
            Group(
                "folder:$path",
                path.substringAfterLast('/').ifBlank { "/" },
                "${list.size} songs • $path",
                list,
                Icons.Filled.Folder,
            )
        }.sortedBy { it.title.lowercase() }
    }
    val liked = remember(filtered, likedIds) { filtered.filter { it.id in likedIds } }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val d = detail
        if (d != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { detail = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        d.title, color = Color.White, fontSize = 18.sp,
                        fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        d.subtitle, color = Color(0xFF9A9A9A), fontSize = 12.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { onShuffleAll(d.songs) }) {
                    Icon(Icons.Filled.Shuffle, "Shuffle", tint = Color.White)
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                SongList(d.songs, current, onPlay)
            }
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(start = 20.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Resonance", color = Color.White, fontSize = 24.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onShuffleAll(filtered) }) {
                    Icon(Icons.Filled.Shuffle, "Shuffle all", tint = Color.White)
                }
                IconButton(onClick = onOpenEq) {
                    Icon(Icons.Filled.GraphicEq, "Equalizer", tint = Color.White)
                }
                IconButton(onClick = onRescan) {
                    Icon(Icons.Filled.Refresh, "Rescan", tint = Color.White)
                }
            }

            TextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Search songs, artists, albums", color = Color(0xFF8A8A8A)) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color(0xFF8A8A8A)) },
                shape = RoundedCornerShape(50),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = PillGrey,
                    unfocusedContainerColor = PillGrey,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Accent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LibTab.values().forEach { t ->
                    val selected = t == tab
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) Accent else PillGrey)
                            .clickable { tab = t }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            t.label,
                            color = if (selected) Color.Black else Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                    }
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    loading -> CenterText("Scanning your music…")
                    songs.isEmpty() -> CenterText("No music found on this device")
                    else -> when (tab) {
                        LibTab.SONGS -> SongList(filtered, current, onPlay)
                        LibTab.ALBUMS -> GroupList(albums) { detail = it }
                        LibTab.ARTISTS -> GroupList(artists) { detail = it }
                        LibTab.FOLDERS -> GroupList(folders) { detail = it }
                        LibTab.LIKED -> if (liked.isEmpty()) {
                            CenterText("Tap the thumbs-up on the player to like songs")
                        } else {
                            SongList(liked, current, onPlay)
                        }
                    }
                }
            }
        }

        if (current != null) {
            MiniPlayer(current, isPlaying, onOpenPlayer, onPlayPause, onNext)
        }
    }
}

@Composable
private fun CenterText(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = Color(0xFF9A9A9A), fontSize = 15.sp)
    }
}

@Composable
private fun SongList(list: List<Song>, current: Song?, onPlay: (List<Song>, Int) -> Unit) {
    if (list.isEmpty()) {
        CenterText("No results")
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        itemsIndexed(list, key = { _, s -> s.id }) { index, song ->
            SongRow(song, song.id == current?.id) { onPlay(list, index) }
        }
    }
}

@Composable
private fun SongRow(song: Song, isCurrent: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(song, 128, Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = if (isCurrent) Accent else Color.White,
                fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${song.artist} • ${song.album}",
                color = Color(0xFF9A9A9A), fontSize = 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(formatTime(song.duration), color = Color(0xFF9A9A9A), fontSize = 12.sp)
    }
}

@Composable
private fun GroupList(groups: List<Group>, onOpen: (Group) -> Unit) {
    if (groups.isEmpty()) {
        CenterText("No results")
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        itemsIndexed(groups, key = { _, g -> g.key }) { _, g ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(g) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (g.icon == Icons.Filled.Album) {
                    AlbumArt(g.songs.first(), 128, Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)))
                } else {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PillGrey),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(g.icon, null, tint = Color.White.copy(alpha = 0.7f))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        g.title, color = Color.White, fontSize = 16.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        g.subtitle, color = Color(0xFF9A9A9A), fontSize = 13.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF111111))
            .clickable(onClick = onOpen)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(song, 128, Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title, color = Color.White, fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                song.artist, color = Color(0xFF9A9A9A), fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onPlayPause) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                "Play or pause", tint = Color.White,
            )
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.SkipNext, "Next", tint = Color.White)
        }
    }
}
