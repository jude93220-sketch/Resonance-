package com.resonance

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

private val VisualizerPlayed = Color.White
private val VisualizerRest = Color.White.copy(alpha = 0.3f)

private fun formatLabel(mime: String): String {
    val m = mime.lowercase()
    return when {
        m.isBlank() -> "AUDIO"
        m.contains("mpeg") -> "MP3"
        m.contains("flac") -> "FLAC"
        m.contains("mp4") || m.contains("m4a") || m.contains("aac") -> "M4A / AAC"
        m.contains("ogg") || m.contains("opus") -> "OGG / OPUS"
        m.contains("wav") -> "WAV"
        else -> m.substringAfter("/").uppercase()
    }
}

@Composable
fun NowPlayingScreen(
    song: Song,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffle: Boolean,
    repeatMode: Int,
    sleepActive: Boolean,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSeekFraction: (Float) -> Unit,
    onSkipBy: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenEq: () -> Unit,
    onOpenSleep: () -> Unit,
    onRatingChanged: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    var isLiked by remember(song.id) { mutableStateOf(RatingManager.isLiked(context, song.id)) }
    var isDisliked by remember(song.id) { mutableStateOf(RatingManager.isDisliked(context, song.id)) }

    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val shownProgress = dragFraction ?: progress
    val shownElapsed = dragFraction?.let { (it * durationMs).toLong() } ?: positionMs

    val latestSeek by rememberUpdatedState(onSeekFraction)

    val bars = remember(song.id) {
        val seed = song.id.toInt()
        List(240) { i -> 0.2f + abs(sin(i * 0.5f)) * 0.6f + Random(seed * 31 + i).nextFloat() * 0.2f }
    }

    val bgBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F1A2E), Color(0xFF05080F), Color.Black)
    )

    Box(modifier = Modifier.fillMaxSize().background(bgBrush)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowDown, "Back", tint = Color.White,
                    modifier = Modifier.size(32.dp).clickable { onBack() }
                )
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.weight(1f))

            AlbumArt(
                song = song,
                px = 1024,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(16.dp)),
                iconSize = 100.dp,
            )

            Spacer(Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillIconButton(
                    icon = Icons.Outlined.ThumbUp,
                    contentDescription = "Like",
                    width = 64.dp,
                    tint = if (isLiked) Accent else Color.White,
                    onClick = {
                        isLiked = !isLiked
                        if (isLiked) {
                            isDisliked = false
                            RatingManager.setDisliked(context, song.id, false)
                        }
                        RatingManager.saveRating(context, song.id, isLiked)
                        onRatingChanged()
                    }
                )
                PillIconButton(
                    icon = Icons.Outlined.ThumbDown,
                    contentDescription = "Dislike",
                    width = 64.dp,
                    tint = if (isDisliked) Color.Red else Color.White,
                    onClick = {
                        isDisliked = !isDisliked
                        RatingManager.setDisliked(context, song.id, isDisliked)
                        if (isDisliked) {
                            isLiked = false
                            RatingManager.saveRating(context, song.id, false)
                        }
                        onRatingChanged()
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            InfoPill(text = song.title, fontSize = 20.sp, weight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            InfoPill(text = song.artist, fontSize = 15.sp, weight = FontWeight.Medium)

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PillIconButton(Icons.Filled.GraphicEq, "Equalizer", width = 64.dp, onClick = onOpenEq)
                Spacer(Modifier.width(8.dp))
                PillIconButton(
                    Icons.Filled.Timer, "Sleep timer", width = 64.dp,
                    tint = if (sleepActive) Accent else Color.White, onClick = onOpenSleep
                )
                Spacer(Modifier.weight(1f))
                PillIconButton(
                    if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    "Repeat", width = 64.dp,
                    tint = if (repeatMode != Player.REPEAT_MODE_OFF) Accent else Color.White,
                    onClick = onCycleRepeat
                )
                Spacer(Modifier.width(8.dp))
                PillIconButton(
                    Icons.Filled.Shuffle, "Shuffle", width = 64.dp,
                    tint = if (shuffle) Accent else Color.White, onClick = onToggleShuffle
                )
            }

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(durationMs) {
                            detectTapGestures { offset ->
                                latestSeek((offset.x / size.width).coerceIn(0f, 1f))
                            }
                        }
                        .pointerInput(durationMs) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                                },
                                onDragEnd = {
                                    dragFraction?.let { latestSeek(it) }
                                    dragFraction = null
                                },
                                onDragCancel = { dragFraction = null },
                                onHorizontalDrag = { change, _ ->
                                    dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                }
                            )
                        }
                ) {
                    val barWidth = 6.dp.toPx()
                    val gap = 6.dp.toPx()
                    val numBars = (size.width / (barWidth + gap)).toInt()
                    val centerY = size.height / 2f
                    val playedX = size.width * shownProgress

                    for (i in 0 until numBars) {
                        val barHeight = size.height * bars[i % bars.size]
                        val x = i * (barWidth + gap) + barWidth / 2f
                        val color = if (x < playedX) VisualizerPlayed else VisualizerRest
                        drawLine(
                            color = color,
                            start = Offset(x, centerY - barHeight / 2),
                            end = Offset(x, centerY + barHeight / 2),
                            strokeWidth = barWidth
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ControlButton(Icons.Filled.FastRewind, 28.dp) { onSkipBy(-10_000L) }
                    ControlButton(Icons.Filled.SkipPrevious, 32.dp) { onPrev() }

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color.Black, CircleShape)
                            .clickable { onPlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            "Play or pause",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    ControlButton(Icons.Filled.SkipNext, 32.dp) { onNext() }
                    ControlButton(Icons.Filled.FastForward, 28.dp) { onSkipBy(10_000L) }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(shownElapsed), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    formatTime(if (durationMs > 0) durationMs else song.duration),
                    color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(50))
                    .background(PillGrey)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = formatLabel(song.mime),
                    color = Color(0xFFB0B0B0), fontSize = 11.sp, fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PillIconButton(
    icon: ImageVector,
    contentDescription: String,
    width: Dp,
    tint: Color = Color.White,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(48.dp)
            .clip(RoundedCornerShape(50))
            .background(PillGrey)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun InfoPill(text: String, fontSize: TextUnit, weight: FontWeight) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(PillGrey)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = text, color = Color.White, fontSize = fontSize,
            fontWeight = weight, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ControlButton(icon: ImageVector, size: Dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size + 24.dp)
            .background(Color.Black, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(size))
    }
}
