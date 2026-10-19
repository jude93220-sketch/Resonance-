package com.resonance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class GroupKind(val title: String) {
    FOLDERS("Folders"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    ALBUM_ARTISTS("Album Artists"),
    GENRES("Genres"),
    YEARS("Years"),
    COMPOSERS("Composers"),
}

class Group(
    val key: String,
    val title: String,
    val subtitle: String,
    val songs: List<Song>,
    val icon: ImageVector,
    val showArt: Boolean = false,
)

class HomeEntry(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val badge: String?,
    val onClick: () -> Unit,
)

fun groupIcon(kind: GroupKind): ImageVector = when (kind) {
    GroupKind.FOLDERS -> Icons.Filled.Folder
    GroupKind.ALBUMS -> Icons.Filled.Album
    GroupKind.ARTISTS -> Icons.Filled.Mic
    GroupKind.ALBUM_ARTISTS -> Icons.Filled.Mic
    GroupKind.GENRES -> Icons.Filled.Category
    GroupKind.YEARS -> Icons.Filled.DateRange
    GroupKind.COMPOSERS -> Icons.Filled.Person
}

private fun countText(n: Int) = if (n == 1) "1 song" else "$n songs"

fun buildGroups(kind: GroupKind, songs: List<Song>): List<Group> {
    val icon = groupIcon(kind)
    return when (kind) {
        GroupKind.FOLDERS -> songs.groupBy { it.folder }.map { entry ->
            Group(
                "f:${entry.key}",
                entry.key.substringAfterLast('/').ifBlank { "/" },
                "${countText(entry.value.size)} • ${entry.key}",
                entry.value,
                icon,
            )
        }.sortedBy { it.title.lowercase() }

        GroupKind.ALBUMS -> songs.groupBy { it.album }.map { entry ->
            val sorted = entry.value.sortedWith(
                compareBy<Song>({ it.trackNumber }, { it.title.lowercase() })
            )
            Group(
                "al:${entry.key}",
                entry.key,
                "${sorted.first().artist} • ${countText(sorted.size)}",
                sorted,
                icon,
                showArt = true,
            )
        }.sortedBy { it.title.lowercase() }

        GroupKind.ARTISTS -> songs.groupBy { it.artist }.map { entry ->
            Group("ar:${entry.key}", entry.key, countText(entry.value.size), entry.value, icon)
        }.sortedBy { it.title.lowercase() }

        GroupKind.ALBUM_ARTISTS -> songs.groupBy { it.albumArtist }.map { entry ->
            Group("aa:${entry.key}", entry.key, countText(entry.value.size), entry.value, icon)
        }.sortedBy { it.title.lowercase() }

        GroupKind.GENRES -> songs.groupBy { it.genre.ifBlank { "Unknown genre" } }.map { entry ->
            Group("g:${entry.key}", entry.key, countText(entry.value.size), entry.value, icon)
        }.sortedBy { it.title.lowercase() }

        GroupKind.YEARS -> songs.groupBy { it.year }.map { entry ->
            Group(
                "y:${entry.key}",
                if (entry.key > 0) entry.key.toString() else "Unknown year",
                countText(entry.value.size),
                entry.value,
                icon,
            )
        }.sortedByDescending { it.title }

        GroupKind.COMPOSERS -> songs.groupBy { it.composer.ifBlank { "Unknown composer" } }.map { entry ->
            Group("c:${entry.key}", entry.key, countText(entry.value.size), entry.value, icon)
        }.sortedBy { it.title.lowercase() }
    }
}

/** Longest folder path shared by all songs (the starting point of "Folders Hierarchy"). */
fun commonRoot(songs: List<Song>): String {
    val folders = songs.map { it.folder }.distinct()
    if (folders.isEmpty()) return ""
    var prefix = folders.first().split('/')
    for (f in folders) {
        val parts = f.split('/')
        var i = 0
        while (i < prefix.size && i < parts.size && prefix[i] == parts[i]) i++
        prefix = prefix.take(i)
    }
    return prefix.joinToString("/")
}

@Composable
fun LibraryHome(entries: List<HomeEntry>) {
    val palette = LocalResonancePalette.current
    Column(Modifier.fillMaxSize().padding(horizontal = ResonanceUi.ScreenHorizontal)) {
        Text(
            "Library",
            color = palette.text,
            fontSize = ResonanceUi.TitleSize,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
        )
        Text("Your music collection", color = palette.mutedText, fontSize = ResonanceUi.BodySize, modifier = Modifier.padding(bottom = 14.dp))
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            itemsIndexed(entries) { _, entry ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(ResonanceUi.RadiusCard))
                        .background(palette.surface)
                        .clickable(onClick = entry.onClick)
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)).background(entry.color),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(entry.icon, null, tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(entry.title, color = palette.text, fontSize = ResonanceUi.BodySize, fontWeight = FontWeight.Bold)
                    if (entry.badge != null) {
                        Spacer(Modifier.width(12.dp))
                        Text(entry.badge, color = palette.mutedText, fontSize = ResonanceUi.CaptionSize, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun GroupsScreen(title: String, icon: ImageVector, groups: List<Group>, onBack: () -> Unit, onOpen: (Group) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        LibraryPill(onBack)
        TitleRow(title, icon)
        if (groups.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nothing here yet", color = Color(0xFF9A9A9A), fontSize = 15.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(groups, key = { _, g -> g.key }) { _, g ->
                    GroupRow(g) { onOpen(g) }
                }
            }
        }
    }
}

@Composable
private fun GroupRow(g: Group, onClick: () -> Unit) {
    val palette = LocalResonancePalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ResonanceUi.RadiusCard))
            .background(palette.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (g.showArt && g.songs.isNotEmpty()) {
            AlbumArt(g.songs.first(), 128, Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)))
        } else {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(Color(0x33FFFFFF)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(g.icon, null, tint = Color.White.copy(alpha = 0.8f))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                g.title, color = palette.text, fontSize = ResonanceUi.BodySize, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                g.subtitle, color = palette.mutedText, fontSize = ResonanceUi.CaptionSize,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Folder-by-folder browsing: sub-folders first, then the songs inside this folder. */
@Composable
fun FolderTreeScreen(
    path: String,
    allSongs: List<Song>,
    current: Song?,
    onBack: () -> Unit,
    onOpenFolder: (String) -> Unit,
    onPlay: (List<Song>, Int) -> Unit,
    onSongMenu: (Song) -> Unit,
) {
    val subfolders = remember(path, allSongs) {
        allSongs.filter { it.folder.startsWith("$path/") }
            .groupBy { it.folder.removePrefix("$path/").substringBefore('/') }
            .map { entry ->
                Group(
                    "$path/${entry.key}",
                    entry.key,
                    countText(entry.value.size),
                    entry.value,
                    Icons.Filled.Folder,
                )
            }
            .sortedBy { it.title.lowercase() }
    }
    val here = remember(path, allSongs) {
        allSongs.filter { it.folder == path }.sortedBy { it.title.lowercase() }
    }

    Column(Modifier.fillMaxSize()) {
        LibraryPill(onBack)
        TitleRow(path.substringAfterLast('/').ifBlank { "Folders" }, Icons.Filled.FolderOpen)
        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(subfolders, key = { _, g -> g.key }) { _, g ->
                GroupRow(g) { onOpenFolder(g.key) }
            }
            itemsIndexed(here, key = { _, s -> "s${s.id}" }) { index, song ->
                SongRow(
                    song = song,
                    isCurrent = song.id == current?.id,
                    selected = null,
                    onTap = { onPlay(here, index) },
                    onLong = { onSongMenu(song) },
                )
            }
        }
    }
}
