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
        ResonancePalette("Midnight", Color(0xFF080B12), Color(0xEE111722), Color(0xFF3D8BFF), Color(0xFFF4F7FB), Color(0xFFAAB6C6), Color(0x663D8BFF)),
        ResonancePalette("Graphite", Color(0xFF0E1117), Color(0xEE181D26), Color(0xFFFFB547), Color(0xFFF7F8FA), Color(0xFFB7C0CB), Color(0x66FFB547)),
        ResonancePalette("Deep Blue", Color(0xFF071321), Color(0xEE102238), Color(0xFF66B6FF), Color(0xFFF2F7FF), Color(0xFFA8BED8), Color(0x6666B6FF)),
        ResonancePalette("Amber Night", Color(0xFF17100B), Color(0xEE261A12), Color(0xFFFF9C54), Color(0xFFFFF7EE), Color(0xFFD5BBA6), Color(0x66FF9C54)),
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
