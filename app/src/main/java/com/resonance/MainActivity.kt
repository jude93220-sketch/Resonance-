package com.resonance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF050506))) {
                var playing by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF050506)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Resonance", style = MaterialTheme.typography.headlineLarge, color = Color(0xFF00E5FF))
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { playing = !playing }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (playing) "Pause" else "Play")
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (playing) "Phase 1: build pipeline works." else "Tap play to test.",
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
