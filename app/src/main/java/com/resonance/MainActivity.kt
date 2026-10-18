package com.resonance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {

    private lateinit var remote: PlayerRemote

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The music itself plays inside PlaybackService, so it survives closing the app.
        remote = PlayerRemote(applicationContext)
        remote.connect()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(background = Color.Black, surface = Color.Black)
            ) {
                ResonanceApp(remote)
            }
        }
    }

    override fun onDestroy() {
        remote.disconnect()
        super.onDestroy()
    }
}
