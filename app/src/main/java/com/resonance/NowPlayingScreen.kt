package com.resonance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

private val PillGrey = Color(0xFF1C1C1C)
private val VisualizerPlayed = Color.White
private val VisualizerRest = Color.White.copy(alpha = 0.3f)

@Composable
fun NowPlayingScreen(
    songId: Long,
    title: String,
    artist: String,
    duration: String,
    elapsed: String,
    isPlaying: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    format: String = "48 KHZ  125 KBPS  OPUS",
    onBack: () -> Unit = {},
    onSearch: () -> Unit = {},
    onMenu: () -> Unit = {},
) {
    val context = LocalContext.current
    
    // This checks the memory to see if the song was already liked
    var isLiked by remember(songId) { mutableStateOf(RatingManager.isLiked(context, songId)) }
    var isDisliked by remember(songId) { mutableStateOf(false) } // Dislikes are just visual for now

    val bgBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F1A2E), Color(0xFF05080F), Color.Black)
    )

    Box(modifier = Modifier.fillMaxSize().background(bgBrush)) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.KeyboardArrowDown, "Back", tint = Color.White, modifier = Modifier.size(32.dp).clickable { onBack() })
                Spacer(Modifier.weight(1f))
                Icon(Icons.Filled.Search, "Search", tint = Color.White, modifier = Modifier.clickable { onSearch() })
                Spacer(Modifier.width(16.dp))
                Icon(Icons.Filled.MoreVert, "More", tint = Color.White, modifier = Modifier.clickable { onMenu() })
            }

            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier.fillMaxWidth(0.85f).aspectRatio(1f).align(Alignment.CenterHorizontally).clip(RoundedCornerShape(16.dp)).background(Color(0xFF152238)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MusicNote, "Album Art", tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(100.dp))
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // LIKE BUTTON - Now saves to memory!
                    PillIconButton(
                        icon = Icons.Outlined.ThumbUp, 
                        contentDescription = "Like", 
                        width = 64.dp, 
                        tint = if (isLiked) Color(0xFF00E5FF) else Color.White,
                        onClick = { 
                            isLiked = !isLiked
                            if (isLiked) isDisliked = false
                            RatingManager.saveRating(context, songId, isLiked) // Save it!
                        }
                    )
                    // DISLIKE BUTTON
                    PillIconButton(
                        icon = Icons.Outlined.ThumbDown, 
                        contentDescription = "Dislike", 
                        width = 64.dp, 
                        tint = if (isDisliked) Color.Red else Color.White,
                        onClick = { 
                            isDisliked = !isDisliked
                            if (isDisliked) {
                                isLiked = false
                                RatingManager.saveRating(context, songId, false) // Remove like if disliked
                            }
                        }
                    )
                }
                PillIconButton(Icons.Filled.MoreVert, "More", width = 64.dp) {}
            }

            Spacer(Modifier.height(16.dp))

            InfoPill(text = title, fontSize = 20.sp, weight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            InfoPill(text = artist, fontSize = 15.sp, weight = FontWeight.Medium)

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PillIconButton(Icons.Filled.GraphicEq, "EQ", width = 64.dp) {}
                Spacer(Modifier.width(8.dp))
                PillIconButton(Icons.Filled.Timer, "Timer", width = 64.dp) {}
                Spacer(Modifier.weight(1f))
                PillIconButton(Icons.Filled.Repeat, "Repeat", width = 64.dp) {}
                Spacer(Modifier.width(8.dp))
                PillIconButton(Icons.Filled.Shuffle, "Shuffle", width = 64.dp) {}
            }

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barWidth = 6.dp.toPx()
                    val gap = 6.dp.toPx()
                    val numBars = (size.width / (barWidth + gap)).toInt()
                    val centerY = size.height / 2f
                    val playedX = size.width * progress

                    for (i in 0 until numBars) {
                        val heightFactor = 0.2f + (abs(sin(i * 0.5f)) * 0.6f) + (Random(i).nextFloat() * 0.2f)
                        val barHeight = size.height * heightFactor
                        val x = i * (barWidth + gap)
                        val color = if (x < playedX) VisualizerPlayed else VisualizerRest
                        drawLine(color = color, start = Offset(x, centerY - (barHeight / 2)), end = Offset(x, centerY + (barHeight / 2)), strokeWidth = barWidth)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ControlButton(Icons.Filled.FastRewind, 28.dp) { onSeek((progress - 0.05f).coerceAtLeast(0f)) }
                    ControlButton(Icons.Filled.SkipPrevious, 32.dp) { onSeek(0f) }
                    
                    Box(
                        modifier = Modifier.size(80.dp).background(Color.Black, CircleShape).clickable { onPlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, 
                            "Play", 
                            tint = Color.White, 
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    
                    ControlButton(Icons.Filled.SkipNext, 32.dp) { }
                    ControlButton(Icons.Filled.FastForward, 28.dp) { onSeek((progress + 0.05f).coerceAtMost(1f)) }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(elapsed, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(duration, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(50)).background(PillGrey).padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(text = format, color = Color(0xFFB0B0B0), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PillIconButton(icon: ImageVector, contentDescription: String, width: androidx.compose.ui.unit.Dp, tint: Color = Color.White, onClick: () -> Unit) {
    Box(
        modifier = Modifier.width(width).height(48.dp).clip(RoundedCornerShape(50)).background(PillGrey).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun InfoPill(text: String, fontSize: androidx.compose.ui.unit.TextUnit, weight: FontWeight) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(PillGrey).padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = fontSize, fontWeight = weight, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ControlButton(icon: ImageVector, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(size + 24.dp).background(Color.Black, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(size))
    }
}
