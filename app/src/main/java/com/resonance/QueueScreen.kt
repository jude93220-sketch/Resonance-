package com.resonance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QueueScreen(
    queueIds: List<Long>,
    songById: Map<Long, Song>,
    currentIndex: Int,
    onBack: () -> Unit,
    onJump: (Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    var removeIndex by remember { mutableStateOf<Int?>(null) }

    Column(Modifier.fillMaxSize()) {
        LibraryPill(onBack)
        TitleRow("Queue", Icons.Filled.PlaylistPlay)
        if (queueIds.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("The queue is empty. Play a song first.", color = Color(0xFF9A9A9A), fontSize = 15.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(queueIds) { index, id ->
                    val song = songById[id]
                    if (song != null) {
                        SongRow(
                            song = song,
                            isCurrent = index == currentIndex,
                            selected = null,
                            onTap = { onJump(index) },
                            onLong = { removeIndex = index },
                        )
                    }
                }
            }
        }
    }

    val ri = removeIndex
    if (ri != null) {
        AlertDialog(
            onDismissRequest = { removeIndex = null },
            confirmButton = {
                TextButton(onClick = {
                    onRemove(ri)
                    removeIndex = null
                }) { Text("Remove from queue") }
            },
            dismissButton = { TextButton(onClick = { removeIndex = null }) { Text("Cancel") } },
            title = { Text("Queue") },
        )
    }
}

@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    onBack: () -> Unit,
    onOpen: (Playlist) -> Unit,
    onCreate: (String) -> Unit,
    onDelete: (Playlist) -> Unit,
    onImport: () -> Unit,
    onExport: (Playlist) -> Unit,
) {
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<Playlist?>(null) }

    Column(Modifier.fillMaxSize()) {
        LibraryPill(onBack)
        TitleRow("Playlists", Icons.Filled.QueueMusic)
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            OutlinePill(onClick = { showCreate = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("New playlist", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        TextButton(onClick = onImport) { Text("Import M3U") }
        Spacer(Modifier.size(8.dp))
        if (playlists.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No playlists yet", color = Color(0xFF9A9A9A), fontSize = 15.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(playlists, key = { _, p -> p.id }) { _, p ->
                    PlaylistRow(p, onTap = { onOpen(p) }, onLong = { deleteTarget = p }, onExport = { onExport(p) })
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            confirmButton = {
                TextButton(
                    enabled = newName.isNotBlank(),
                    onClick = {
                        onCreate(newName.trim())
                        newName = ""
                        showCreate = false
                    },
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } },
            title = { Text("New playlist") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text("Name") },
                )
            },
        )
    }

    val dt = deleteTarget
    if (dt != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(dt)
                    deleteTarget = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
            title = { Text("Delete \"${dt.name}\"?") },
        )
    }
}

@Composable
private fun PlaylistRow(p: Playlist, onTap: () -> Unit, onLong: () -> Unit, onExport: () -> Unit) {
    val tap by rememberUpdatedState(onTap)
    val long by rememberUpdatedState(onLong)
    Row(
        Modifier
            .fillMaxWidth()
            .pointerInput(Unit) { detectTapGestures(onTap = { tap() }, onLongPress = { long() }) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(Color(0x33FFFFFF)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.QueueMusic, null, tint = Color.White.copy(alpha = 0.8f))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                p.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (p.songIds.size == 1) "1 song" else "${p.songIds.size} songs",
                color = Color(0xFFD0D0D0), fontSize = 13.sp,
            )
        }
        TextButton(onClick = onExport) { Text("Export") }
    }
}

/** Pick a playlist (or make a new one) and add the given songs to it. */
@Composable
fun AddToPlaylistDialog(songIds: List<Long>, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    val context = LocalContext.current
    val lists = remember { PlaylistStore.all(context) }
    var newName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Add to playlist") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                lists.forEach { p ->
                    TextButton(onClick = {
                        PlaylistStore.addSongs(context, p.id, songIds)
                        onDone(p.name)
                    }) { Text("${p.name} (${p.songIds.size})") }
                }
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text("New playlist") },
                )
                TextButton(
                    enabled = newName.isNotBlank(),
                    onClick = {
                        val p = PlaylistStore.create(context, newName.trim())
                        PlaylistStore.addSongs(context, p.id, songIds)
                        onDone(p.name)
                    },
                ) { Text("Create and add") }
            }
        },
    )
}
