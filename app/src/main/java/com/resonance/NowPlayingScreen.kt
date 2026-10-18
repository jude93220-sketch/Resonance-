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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun NowPlayingScreen() {
    // Deep blue/black gradient to simulate the blurred background in your screenshot
    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF00132B),
            Color(0xFF000000)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.KeyboardArrowDown, "Down", tint = Color.White)
                Icon(Icons.Filled.Search, "Search", tint = Color.White)
                Icon(Icons.Filled.MoreVert, "More", tint = Color.White)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Album Art (Placeholder)
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E3A8A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MusicNote, null, tint = Color.White, modifier = Modifier.size(80.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Thumbs Up/Down Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}, modifier = Modifier.background(Color(0xFF1E1E1E), CircleShape)) {
                    Icon(Icons.Filled.ThumbUp, "Like", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {}, modifier = Modifier.background(Color(0xFF1E1E1E), CircleShape)) {
                    Icon(Icons.Filled.ThumbDown, "Dislike", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Track Info Pills
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Text(
                    text = "i'kthande (Official Audio) feat. CowBo",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Awakened Regal - Awakened Regal",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Utility Buttons Row
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

            Spacer(modifier = Modifier.weight(1f))

            // Waveform Visualizer & Seekbar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // Draw the waveform
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barWidth = 4.dp.toPx()
                    val gap = 4.dp.toPx()
                    val numBars = (size.width / (barWidth + gap)).toInt()
                    
                    for (i in 0 until numBars) {
                        // Generate a fake waveform shape
                        val heightFactor = Random(i).nextFloat() * 0.8f + 0.2f
                        val barHeight = size.height * heightFactor
                        val x = i * (barWidth + gap)
                        
                        // Color bars that are "played" (past the middle) differently
                        val color = if (i < numBars / 3) Color.White else Color.White.copy(alpha = 0.3f)
                        
                        drawLine(
                            color = color,
                            start = Offset(x, (size.height - barHeight) / 2),
                            end = Offset(x, (size.height + barHeight) / 2),
                            strokeWidth = barWidth
                        )
                    }
                }

                // Playback Controls Overlay
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ControlButton(Icons.Filled.SkipPrevious, 40.dp)
                    ControlButton(Icons.Filled.FastRewind, 48.dp)
                    
                    // Play Button
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color.Black, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Pause, "Play/Pause", tint = Color.White, modifier = Modifier.size(48.dp))
                    }

                    ControlButton(Icons.Filled.FastForward, 48.dp)
                    ControlButton(Icons.Filled.SkipNext, 40.dp)
                }
            }

            // Time Stamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("1:33", color = Color.White, fontSize = 12.sp, modifier = Modifier.background(Color.Black, RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
                Text("7:26", color = Color.White, fontSize = 12.sp, modifier = Modifier.background(Color.Black, RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Audio Info Pill
            Text(
                text = "48 KHZ 160 KBPS MP3",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(24.dp))
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
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
        modifier = Modifier.background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
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
