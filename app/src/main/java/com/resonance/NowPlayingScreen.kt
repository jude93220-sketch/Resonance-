package com.resonance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NowPlayingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Pure black background
    ) {
        // 1. Massive background waveform
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path()
            val width = size.width
            val height = size.height

            // Draw a smooth, solid background wave
            path.moveTo(0f, height * 0.25f)
            path.cubicTo(
                width * 0.15f, height * 0.05f,
                width * 0.25f, height * 0.40f,
                width * 0.40f, height * 0.20f
            )
            path.cubicTo(
                width * 0.55f, height * 0.0f,
                width * 0.65f, height * 0.35f,
                width * 0.80f, height * 0.15f
            )
            path.cubicTo(
                width * 0.90f, height * 0.05f,
                width * 0.95f, height * 0.30f,
                width, height * 0.25f
            )
            path.lineTo(width, 0f)
            path.lineTo(0f, 0f)
            path.close()

            drawPath(
                path = path,
                color = Color.DarkGray.copy(alpha = 0.15f),
                style = Fill
            )
        }

        // Main Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.KeyboardArrowDown, "Down", tint = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Icon(Icons.Filled.Search, "Search", tint = Color.White)
                    Icon(Icons.Filled.MoreVert, "More", tint = Color.White)
                }
            }

            // Empty Space for Album Art (This pushes the controls down exactly like the screenshot)
            Spacer(modifier = Modifier.weight(1f))

            // Thumbs Up/Down Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillIconButton(Icons.Filled.ThumbUp)
                    PillIconButton(Icons.Filled.ThumbDown)
                }
                PillIconButton(Icons.Filled.MoreVert)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Track Info Pills
            Column {
                Text(
                    text = "Baba Yilwa_124kbps.mp3",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color(0xFF1C1C1C), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Unknown artist",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .background(Color(0xFF1C1C1C), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Utility Buttons (EQ, Timer, Repeat, Shuffle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillIconButton(Icons.Filled.GraphicEq)
                    PillIconButton(Icons.Filled.AccessTime)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillIconButton(Icons.Filled.Repeat)
                    PillIconButton(Icons.Filled.Shuffle)
                }
            }

            Spacer(modifier = Modifier.weight(0.8f))

            // 2. Bottom Continuous Waveform & Controls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                // Continuous solid waveform
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path()
                    val width = size.width
                    val height = size.height
                    val centerY = height / 2f

                    // Draw the left side (Played - White)
                    path.moveTo(0f, centerY)
                    path.cubicTo(
                        width * 0.1f, centerY - (height * 0.2f),
                        width * 0.2f, centerY + (height * 0.3f),
                        width * 0.3f, centerY
                    )
                    path.cubicTo(
                        width * 0.35f, centerY - (height * 0.1f),
                        width * 0.4f, centerY + (height * 0.1f),
                        width * 0.45f, centerY
                    )
                    // Draw the right side (Unplayed - Grey)
                    path.cubicTo(
                        width * 0.5f, centerY - (height * 0.4f),
                        width * 0.6f, centerY + (height * 0.4f),
                        width * 0.7f, centerY - (height * 0.1f)
                    )
                    path.cubicTo(
                        width * 0.8f, centerY - (height * 0.3f),
                        width * 0.9f, centerY + (height * 0.2f),
                        width, centerY
                    )

                    // Draw a thick continuous line
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = 0.9f), // Using one color for simplicity, you can split it if you want
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 12f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }

                // Playback Controls (No black circles, just clean white icons!)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.FastRewind, "Rewind", tint = Color.White, modifier = Modifier.size(28.dp))
                    Icon(Icons.Filled.SkipPrevious, "Prev", tint = Color.White, modifier = Modifier.size(36.dp))
                    
                    // Play Button (Just the icon, no background)
                    Icon(Icons.Filled.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(72.dp))

                    Icon(Icons.Filled.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                    Icon(Icons.Filled.FastForward, "Forward", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            // Time Stamps (Clean text)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0:01", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("3:54", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Audio Info Pill
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "48 KHZ 125 KBPS OPUS",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color(0xFF1C1C1C), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1C1C1C), RoundedCornerShape(50))
                    .padding(vertical = 16.dp, horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.GridView, "Library", tint = Color.White.copy(alpha = 0.6f))
                Icon(Icons.Filled.BarChart, "Stats", tint = Color.White.copy(alpha = 0.6f))
                Icon(Icons.Filled.Search, "Search", tint = Color.White.copy(alpha = 0.6f))
                Icon(Icons.Filled.Menu, "Menu", tint = Color.White)
            }
        }
    }
}

// A reusable pill-shaped button to match the screenshot exactly
@Composable
fun PillIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .height(44.dp)
            .width(56.dp) // Makes it a horizontal pill
            .background(Color(0xFF1C1C1C), RoundedCornerShape(50)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
    }
}
