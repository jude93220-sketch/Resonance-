package com.resonance

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ResonancePalette(
    val name: String,
    val background: Color,
    val surface: Color,
    val accent: Color,
    val text: Color,
    val mutedText: Color,
    val visualizerRest: Color,
) 

object ResonanceThemes {
    val all = listOf(
        ResonancePalette("Midnight", Color(0xFF05070D), Color(0xCC111827), Color(0xFF00E5FF), Color.White, Color(0xFFB8C2CC), Color(0x6657C7FF)),
        ResonancePalette("Graphite", Color(0xFF111111), Color(0xDD242424), Color(0xFFFFB300), Color.White, Color(0xFFC7C7C7), Color(0x66FFB300)),
        ResonancePalette("Deep Blue", Color(0xFF071426), Color(0xDD102A4A), Color(0xFF6FA8FF), Color.White, Color(0xFFB5C7E6), Color(0x665F9DFF)),
        ResonancePalette("Amber Night", Color(0xFF1A0F08), Color(0xDD322016), Color(0xFFFF8A3D), Color(0xFFFFF7EE), Color(0xFFD5BBA6), Color(0x66FF8A3D)),
        ResonancePalette("Light", Color(0xFFF3F5F8), Color(0xEEFFFFFF), Color(0xFF155BC4), Color(0xFF101820), Color(0xFF5B6670), Color(0x662B6FD6)),
    )

    fun get(index: Int): ResonancePalette = all[index.coerceIn(0, all.lastIndex)]
}

object ThemeStore {
    private const val PREFS = "resonance_theme"
    private const val THEME = "theme"
    private const val VISUALIZER = "visualizer"
    const val SPECTRUM = 0
    const val WAVE = 1
    const val PULSE = 2

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun theme(context: Context): ResonancePalette =
        ResonanceThemes.get(prefs(context).getInt(THEME, 0))

    fun themeIndex(context: Context): Int =
        prefs(context).getInt(THEME, 0).coerceIn(0, ResonanceThemes.all.lastIndex)

    fun setTheme(context: Context, index: Int) {
        prefs(context).edit().putInt(THEME, index.coerceIn(0, ResonanceThemes.all.lastIndex)).apply()
    }

    fun visualizer(context: Context): Int =
        prefs(context).getInt(VISUALIZER, SPECTRUM).coerceIn(SPECTRUM, PULSE)

    fun setVisualizer(context: Context, mode: Int) {
        prefs(context).edit().putInt(VISUALIZER, mode.coerceIn(SPECTRUM, PULSE)).apply()
    }
}

val LocalResonancePalette = staticCompositionLocalOf { ResonanceThemes.all.first() }
