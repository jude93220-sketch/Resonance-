package com.resonance

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private val PillGrey = Color(0xFF1C1C1C)
private val WaveBackdrop = Color(0xFF262626)
private val VisualizerPlayed = Color(0xFFB8B8B8)
private val VisualizerRest = Color(0xFF5A5A5A)

@Composable
fun NowPlayingScreen(
    title: String = "Baba Yilwa_124kbps.mp3",
    artist: String = "Unknown artist",
    elapsed: String = "0:01",
    duration: String = "3:54",
    format: String = "48 KHZ  125 KBPS  OPUS",
    onBack: () -> Unit = {},
    onSearch: () -> Unit = {},
    onMenu: () -> Unit = {},
) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0.03f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        BackgroundWaveform(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize(0.5f)
                .align(Alignment.TopCenter)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            TopBar(onBack = onBack, onSearch = onSearch, onMenu = onMenu)

            Spacer(modifier = Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillIconButton(Icons.Filled.ThumbUp, "Like", width = 96.dp) {}
                PillIconButton(Icons.Filled.ThumbDown, "Dislike", width = 96.dp) {}
            }

            Spacer(Modifier.height(16.dp))

            InfoPill(text = title, fontSize = 22.sp, weight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            InfoPill(text = artist, fontSize = 17.sp, weight = FontWeight.SemiBold)

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PillIconButton(Icons.Filled.GraphicEq, "Equalizer", width = 96.dp) {}
                Spacer(Modifier.width(8.dp))
                PillIconButton(Icons.Filled.Timer, "Sleep timer", width = 96.dp) {}
                Spacer(Modifier.weight(1f))
                PillIconButton(Icons.Filled.Repeat, "Repeat", width = 96.dp) {}
                Spacer(Modifier.width(8.dp))
                PillIconButton(Icons.Filled.Shuffle, "Shuffle", width = 96.dp) {}
            }

            Spacer(Modifier.height(20.dp))

            VisualizerWithControls(
                isPlaying = isPlaying,
                progress = progress,
                onSeek = { progress = it },
                onPlayPause = { isPlaying = !isPlaying },
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(elapsed, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(duration, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(50))
                    .background(PillGrey)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = format,
                    color = Color(0xFFB0B0B0),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            BottomNavPill()

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit, onSearch: () -> Unit, onMenu: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BareIcon(Icons.Filled.KeyboardArrowDown, "Back", size = 32.dp, onClick = onBack)
        Spacer(Modifier.weight(1f))
        BareIcon(Icons.Filled.Search, "Search", onClick = onSearch)
        BareIcon(Icons.Filled.MoreVert, "More", onClick = onMenu)
    }
}

@Composable
private fun BareIcon(
    icon: ImageVector,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp = 26.dp,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = Color.White, modifier = Modifier.size(size))
    }
}

@Composable
private fun PillIconButton(
    icon: ImageVector,
    contentDescription: String,
    width: androidx.compose.ui.unit.Dp,
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
        Icon(icon, contentDescription, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun InfoPill(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    weight: FontWeight,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(PillGrey)
            .padding(horizontal = 24.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = weight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BackgroundWaveform(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val top = listOf(
            0.07f to 0.62f, 0.15f to 0.38f, 0.20f to 0.50f,
            0.29f to 0.30f, 0.37f to 0.52f, 0.49f to 0.24f,
            0.61f to 0.68f, 0.73f to 0.00f, 0.95f to 0.88f,
        )
        val bottom = listOf(
            0.94f to 0.90f, 0.61f to 0.82f, 0.50f to 0.44f,
            0.37f to 0.66f, 0.27f to 0.45f, 0.20f to 0.62f,
            0.15f to 0.52f, 0.07f to 0.64f,
        )

        val path = Path().apply {
            moveTo(top.first().first * w, top.first().second * h)
            top.drop(1).forEach { (x, y) -> lineTo(x * w, y * h) }
            bottom.forEach { (x, y) -> lineTo(x * w, y * h) }
            close()
        }
        drawPath(path, WaveBackdrop)
    }
}

@Composable
private fun VisualizerWithControls(
    isPlaying: Boolean,
    progress: Float,
    onSeek: (Float) -> Unit,
    onPlayPause: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val livePhase = if (isPlaying) phase else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val midY = h / 2f
            val maxAmp = h / 2f
            val steps = 160

            fun amp(t: Float): Float {
                val a = abs(sin(t * 9f + livePhase)) * 0.35f +
                        abs(sin(t * 17f - livePhase * 1.3f)) * 0.25f +
                        abs(sin(t * 4f + livePhase * 0.5f)) * 0.25f
                val ramp = 0.35f + 0.65f * t
                return (0.12f + a * ramp).coerceIn(0.08f, 1f) * maxAmp
            }

            val path = Path().apply {
                moveTo(0f, midY - amp(0f))
                for (i in 1..steps) {
                    val t = i / steps.toFloat()
                    val prevT = (i - 1) / steps.toFloat()
                    val x = t * w
                    val px = prevT * w
                    val cx = (x + px) / 2f
                    val cy = (midY - amp(t) + midY - amp(prevT)) / 2f
                    quadraticBezierTo(px, midY - amp(prevT), cx, cy)
                }
                lineTo(w, midY - amp(1f))
                lineTo(w, midY + amp(1f))
                for (i in steps - 1 downTo 0) {
                    val t = i / steps.toFloat()
                    val nextT = (i + 1) / steps.toFloat()
                    val x = t * w
                    val nx = nextT * w
                    val cx = (x + nx) / 2f
                    val cy = (midY + amp(t) + midY + amp(nextT)) / 2f
                    quadraticBezierTo(nx, midY + amp(nextT), cx, cy)
                }
                close()
            }

            val playedX = w * progress.coerceIn(0f, 1f)

            clipRect(left = playedX, right = w) {
                drawPath(path, VisualizerRest)
            }
            clipRect(left = 0f, right = playedX) {
                drawPath(path, VisualizerPlayed)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TransportIcon(Icons.Filled.SkipPrevious, "Previous", 36.dp) { onSeek(0f) }
            TransportIcon(Icons.Filled.FastRewind, "Rewind", 40.dp) {
                onSeek((progress - 0.05f).coerceAtLeast(0f))
            }
            TransportIcon(
                icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                size = 64.dp,
                onClick = onPlayPause
            )
            TransportIcon(Icons.Filled.FastForward, "Forward", 40.dp) {
                onSeek((progress + 0.05f).coerceAtMost(1f))
            }
            TransportIcon(Icons.Filled.SkipNext, "Next", 36.dp) {}
        }
    }
}

@Composable
private fun TransportIcon(
    icon: ImageVector,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size + 16.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(size)
        )
    }
}

@Composable
private fun BottomNavPill() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(50))
            .background(PillGrey)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tint = Color(0xFFBDBDBD)
        listOf(
            Icons.Filled.GridView to "Library",
            Icons.Filled.BarChart to "Stats",
            Icons.Filled.Search to "Search",
            Icons.Filled.Menu to "Menu",
        ).forEach { (icon, desc) ->
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, desc, tint = tint, modifier = Modifier.size(32.dp))
            }
        }
    }
}
