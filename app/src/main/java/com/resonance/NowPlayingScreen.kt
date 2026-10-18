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
import androidx.compose.ui.graphics.drawscope.Stroke
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
        // Massive faded waveform in the background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path()
            val width = size.width
            val height = size.height
            
            // Draw a large abstract waveform behind the top section
            path.moveTo(0f, height * 0.25f)
            path.lineTo(width * 0.1f, height * 0.15f)
            path.lineTo(width * 0.2f, height * 0.35f)
            path.lineTo(width * 0.3f, height * 0.1f)
            path.lineTo(width * 0.4f, height * 0.3f)
            path.lineTo(width * 0.5f, height * 0.05f)
            path.lineTo(width * 0.6f, height * 0.25f)
            path.lineTo(width * 0.7f, height * 0.1f)
            path.lineTo(width * 0.8f, height * 0.4f)
            path.lineTo(width * 0.9f, height * 0.2f)
            path.lineTo(width, height * 0.5f)

            drawPath(
                path = path,
                color = Color.DarkGray.copy(alpha = 0.2f),
                style = Stroke(width = 80f) // Very thick lines
            )
        }

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

            Spacer(modifier = Modifier.weight(1f)) // Push controls down

            // Thumbs Up/Down & Queue Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = {}, modifier = Modifier.background(Color(0xFF1E1E1E), CircleShape)) {
                        Icon(Icons.Filled.ThumbUp, "Like", tint = Color.White)
                    }
                    IconButton(onClick = {}, modifier = Modifier.background(Color(0xFF1E1E1E), CircleShape)) {
                        Icon(Icons.Filled.ThumbDown, "Dislike", tint = Color.White)
                    }
                }
                IconButton(onClick = {}, modifier = Modifier.background(Color(0xFF1E1E1E), CircleShape)) {
                    Icon(Icons.Filled.MoreVert, "More", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Track Info Pills
            Column {
                Text(
                    text = "Baba Yilwa_124kbps.mp3",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Unknown artist",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Utility Buttons Row (EQ, Timer, Repeat, Shuffle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UtilityButton(Icons.Filled.GraphicEq)
                    UtilityButton(Icons.Filled.AccessTime)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UtilityButton(Icons.Filled.Repeat)
                    UtilityButton(Icons.Filled.Shuffle)
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Waveform Visualizer & Playback Controls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // Draw the small waveform
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barWidth = 6.dp.toPx()
                    val gap = 6.dp.toPx()
                    val numBars = (size.width / (barWidth + gap)).toInt()
                    val centerY = size.height / 2f
                    
                    for (i in 0 until numBars) {
                        // Random heights to simulate audio
                        val heightFactor = if (i % 3 == 0) 0.8f else if (i % 2 == 0) 0.4f else 0.2f
                        val barHeight = size.height * heightFactor
                        val x = i * (barWidth + gap)
                        
                        // Highlight the "played" portion
                        val color = if (i < numBars / 4) Color.White else Color.White.copy(alpha = 0.2f)
                        
                        drawLine(
                            color = color,
                            start = Offset(x, centerY - (barHeight / 2)),
                            end = Offset(x, centerY + (barHeight / 2)),
                            strokeWidth = barWidth
                        )
                    }
                }

                // Playback Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ControlButton(Icons.Filled.FastRewind, 28.dp)
                    ControlButton(Icons.Filled.SkipPrevious, 32.dp)
                    
                    // Play Button
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color.Black, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(48.dp))
                    }

                    ControlButton(Icons.Filled.SkipNext, 32.dp)
                    ControlButton(Icons.Filled.FastForward, 28.dp)
                }
            }

            // Time Stamps
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
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(32.dp))
                    .padding(vertical = 16.dp, horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.GridView, "Library", tint = Color.White.copy(alpha = 0.5f))
                Icon(Icons.Filled.BarChart, "Stats", tint = Color.White.copy(alpha = 0.5f))
                Icon(Icons.Filled.Search, "Search", tint = Color.White.copy(alpha = 0.5f))
                Icon(Icons.Filled.Menu, "Menu", tint = Color.White)
            }
        }
    }
}

@Composable
fun UtilityButton(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    IconButton(
        onClick = {},
        modifier = Modifier.background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.8f))
    }
}

@Composable
fun ControlButton(icon: androidx.compose.ui.graphics.vector.ImageVector, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size + 16.dp)
            .background(Color.Black, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(size))
    }
}
