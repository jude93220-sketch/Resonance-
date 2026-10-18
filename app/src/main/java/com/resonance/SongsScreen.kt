package com.resonance

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val Letters: List<Char> = listOf('0') + ('A'..'Z').toList() + '#'

private fun keyOf(song: Song): Char {
    val c = song.title.firstOrNull { it.isLetterOrDigit() } ?: return '#'
    val u = c.uppercaseChar()
    return if (u.isDigit()) '0' else if (u in 'A'..'Z') u else '#'
}

/** One song line: art, title, "artist - album", and "3:19 | mp3". */
@Composable
internal fun SongRow(
    song: Song,
    isCurrent: Boolean,
    selected: Boolean?,
    onTap: () -> Unit,
    onLong: () -> Unit,
) {
    val tap by rememberUpdatedState(onTap)
    val long by rememberUpdatedState(onLong)

    Row(
        Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { tap() }, onLongPress = { long() })
            }
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            AlbumArt(song, 128, Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)))
            if (selected == true) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Accent.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Check, null, tint = Color.Black)
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = if (isCurrent) Accent else Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${song.artist} - ${song.album}",
                    color = Color(0xFFD0D0D0),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${formatTime(song.duration)} | ${song.ext}",
                    color = Color(0xFF9AA5B1),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
internal fun OutlinePill(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
internal fun LibraryPill(onBack: () -> Unit) {
    Row(Modifier.padding(start = 16.dp, top = 8.dp)) {
        OutlinePill(onClick = onBack) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Library", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
internal fun TitleRow(title: String, icon: ImageVector) {
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(Color(0xFF7F90E0)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Color(0xFF14213D), modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(
            title,
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SongsScreen(
    title: String,
    icon: ImageVector,
    songs: List<Song>,
    current: Song?,
    alphabetical: Boolean,
    onBack: () -> Unit,
    onPlay: (List<Song>, Int) -> Unit,
    onShuffle: (List<Song>) -> Unit,
    onPlayNext: (List<Song>) -> Unit,
    onAddToQueue: (List<Song>) -> Unit,
    onAddToPlaylist: (List<Song>) -> Unit,
    onRemoveFromPlaylist: ((Song) -> Unit)?,
    onSearch: () -> Unit,
) {
    val sorted = remember(songs, alphabetical) {
        if (alphabetical) {
            songs.sortedWith(
                compareBy<Song>({ Letters.indexOf(keyOf(it)) }, { it.title.lowercase() })
            )
        } else {
            songs
        }
    }
    var selectMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var menuSong by remember { mutableStateOf<Song?>(null) }
    var showListMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    BackHandler(enabled = selectMode) {
        selectMode = false
        selected = emptySet()
    }

    val firstIndex = remember(sorted) {
        val arr = IntArray(Letters.size) { -1 }
        sorted.forEachIndexed { i, s ->
            val li = Letters.indexOf(keyOf(s))
            if (arr[li] == -1) arr[li] = i
        }
        for (k in Letters.indices.reversed()) {
            if (arr[k] == -1) {
                arr[k] = if (k == Letters.lastIndex) (sorted.size - 1).coerceAtLeast(0) else arr[k + 1]
            }
        }
        arr
    }

    Column(Modifier.fillMaxSize()) {
        LibraryPill(onBack)
        TitleRow(title, icon)

        if (selectMode) {
            val picked = sorted.filter { it.id in selected }
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinePill(onClick = {
                    selectMode = false
                    selected = emptySet()
                }) { Text("Done", color = Color.White, fontWeight = FontWeight.SemiBold) }
                OutlinePill(onClick = { selected = sorted.map { it.id }.toSet() }) {
                    Text("All", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Text("${picked.size} selected", color = Color.White, fontSize = 14.sp)
                if (picked.isNotEmpty()) {
                    OutlinePill(onClick = { onPlay(picked, 0) }) {
                        Text("Play", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinePill(onClick = { onPlayNext(picked) }) {
                        Text("Next", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinePill(onClick = { onAddToQueue(picked) }) {
                        Text("Queue", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinePill(onClick = { onAddToPlaylist(picked) }) {
                        Text("+ List", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinePill(onClick = { onShuffle(sorted) }) {
                    Icon(Icons.Filled.Shuffle, "Shuffle", tint = Color.White)
                }
                OutlinePill(onClick = { onPlay(sorted, 0) }) {
                    Icon(Icons.Filled.PlayArrow, "Play", tint = Color.White)
                }
                OutlinePill(onClick = onSearch) {
                    Icon(Icons.Filled.Search, "Search", tint = Color.White)
                }
                OutlinePill(onClick = { selectMode = true }) {
                    Text("Select", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.weight(1f))
                OutlinePill(onClick = { showListMenu = true }) {
                    Icon(Icons.Filled.MoreVert, "More", tint = Color.White)
                }
            }
        }

        Spacer(Modifier.size(8.dp))

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (sorted.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nothing here yet", color = Color(0xFF9A9A9A), fontSize = 15.sp)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(end = if (alphabetical) 28.dp else 0.dp),
                ) {
                    itemsIndexed(sorted, key = { _, s -> s.id }) { index, song ->
                        SongRow(
                            song = song,
                            isCurrent = song.id == current?.id,
                            selected = if (selectMode) song.id in selected else null,
                            onTap = {
                                if (selectMode) {
                                    selected = if (song.id in selected) selected - song.id else selected + song.id
                                } else {
                                    onPlay(sorted, index)
                                }
                            },
                            onLong = { menuSong = song },
                        )
                    }
                }
                if (alphabetical && sorted.size > 20) {
                    AlphabetStrip(
                        firstIndex = firstIndex,
                        onJump = { idx -> scope.launch { listState.scrollToItem(idx) } },
                        modifier = Modifier.align(Alignment.CenterEnd),
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
            onSelect = {
                selectMode = true
                selected = setOf(ms.id)
            },
            onRemoveFromPlaylist = if (onRemoveFromPlaylist != null) {
                { onRemoveFromPlaylist(ms) }
            } else {
                null
            },
        )
    }

    if (showListMenu) {
        AlertDialog(
            onDismissRequest = { showListMenu = false },
            confirmButton = { TextButton(onClick = { showListMenu = false }) { Text("Close") } },
            title = { Text(title) },
            text = {
                Column {
                    TextButton(onClick = {
                        onPlayNext(sorted)
                        showListMenu = false
                    }) { Text("Play all next") }
                    TextButton(onClick = {
                        onAddToQueue(sorted)
                        showListMenu = false
                    }) { Text("Add all to queue") }
                    TextButton(onClick = {
                        onAddToPlaylist(sorted)
                        showListMenu = false
                    }) { Text("Add all to playlist") }
                }
            },
        )
    }
}

@Composable
private fun AlphabetStrip(firstIndex: IntArray, onJump: (Int) -> Unit, modifier: Modifier) {
    val jump by rememberUpdatedState(onJump)

    Column(
        modifier
            .fillMaxHeight()
            .width(26.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val li = (offset.y / size.height * Letters.size).toInt().coerceIn(0, Letters.lastIndex)
                    jump(firstIndex[li])
                }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        val li = (change.position.y / size.height * Letters.size).toInt()
                            .coerceIn(0, Letters.lastIndex)
                        jump(firstIndex[li])
                    }
                )
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Letters.forEach { ch ->
            Text(ch.toString(), color = Color(0xFFB8C2CC), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
internal fun SongActionsDialog(
    song: Song,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onSelect: (() -> Unit)?,
    onRemoveFromPlaylist: (() -> Unit)?,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text(song.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                TextButton(onClick = {
                    onPlayNext()
                    onDismiss()
                }) { Text("Play next") }
                TextButton(onClick = {
                    onAddToQueue()
                    onDismiss()
                }) { Text("Add to queue") }
                TextButton(onClick = {
                    onAddToPlaylist()
                    onDismiss()
                }) { Text("Add to playlist") }
                if (onSelect != null) {
                    TextButton(onClick = {
                        onSelect()
                        onDismiss()
                    }) { Text("Select") }
                }
                if (onRemoveFromPlaylist != null) {
                    TextButton(onClick = {
                        onRemoveFromPlaylist()
                        onDismiss()
                    }) { Text("Remove from this playlist") }
                }
            }
        },
    )
}
