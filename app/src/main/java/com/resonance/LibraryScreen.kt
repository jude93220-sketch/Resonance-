package com.resonance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LibraryScreen(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongSelect: (Song) -> Unit,
    onOpenPlayer: () -> Unit,
    onPlayPause: () -> Unit 
) {
    val bgBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F1A2E), Color(0xFF05080F), Color.Black)
    )

    Box(modifier = Modifier.fillMaxSize().background(bgBrush)) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp).padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Library", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Filled.MoreVert, "More", tint = Color.White)
            }

            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 100.dp)) {
                val categories = listOf(
                    "All Songs" to Icons.Filled.MusicNote,
                    "Folders" to Icons.Filled.Folder,
                    "Folders Hierarchy" to Icons.Filled.FolderOpen,
                    "Albums" to Icons.Filled.Album,
                    "Artists" to Icons.Filled.Person,
                    "Album Artists" to Icons.Filled.PersonOutline,
                    "Genres" to Icons.Filled.QueueMusic,
                    "Years" to Icons.Filled.CalendarToday,
                    "Composers" to Icons.Filled.Edit,
                    "Playlists" to Icons.Filled.PlaylistPlay,
                    "Queue" to Icons.Filled.List,
                    "Most Played" to Icons.Filled.PlayArrow,
                    "Top Rated" to Icons.Filled.ThumbUp,
                    "Recently Added" to Icons.Filled.Add,
                )

                items(categories) { (title, icon) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                if (title == "All Songs" && songs.isNotEmpty()) {
                                    onSongSelect(songs.first())
                                }
                            }
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, null, tint = Color(0xFF90CAF9))
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (currentSong != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1C1C1C))
                    .clickable { onOpenPlayer() }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.MusicNote, null, tint = Color.White.copy(alpha = 0.5f))
                    }
                    
                    Spacer(Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(currentSong.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(currentSong.artist, color = Color.Gray, fontSize = 14.sp, maxLines = 1)
                    }
                    
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp).clickable { onPlayPause() }
                    )
                }
            }
        }
    }
}
