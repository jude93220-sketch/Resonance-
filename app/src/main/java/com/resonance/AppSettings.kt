package com.resonance

import android.content.Context
import android.content.SharedPreferences

/** Simple app-wide options that the Settings screens edit. */
object AppSettings {
    private const val PREFS = "resonance_settings"

    const val K_PAUSE_NOISY = "pause_on_disconnect"
    const val K_MIN_SECONDS = "ignore_short_seconds"
    const val K_KEEP_ON = "keep_screen_on"
    const val K_BG_INTENSITY = "bg_intensity"
    const val K_BG_GRADIENT = "bg_gradient"
    const val K_CROSSFADE_MS = "crossfade_ms"
    const val K_SWIPE_TRACKS = "swipe_tracks"
    const val K_REPLAY_GAIN_MODE = "replay_gain_mode"

    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun pauseOnDisconnect(c: Context) = prefs(c).getBoolean(K_PAUSE_NOISY, true)
    fun minTrackSeconds(c: Context) = prefs(c).getInt(K_MIN_SECONDS, 15)
    fun keepScreenOn(c: Context) = prefs(c).getBoolean(K_KEEP_ON, false)
    fun bgIntensity(c: Context) = prefs(c).getInt(K_BG_INTENSITY, 100)
    fun bgGradient(c: Context) = prefs(c).getInt(K_BG_GRADIENT, 6)
    fun crossfadeMs(c: Context) = prefs(c).getInt(K_CROSSFADE_MS, 0)
    fun swipeTracks(c: Context) = prefs(c).getBoolean(K_SWIPE_TRACKS, true)
    fun replayGainMode(c: Context) = prefs(c).getInt(K_REPLAY_GAIN_MODE, 0).coerceIn(0, 2)

    fun putBool(c: Context, key: String, v: Boolean) = prefs(c).edit().putBoolean(key, v).apply()
    fun putInt(c: Context, key: String, v: Int) = prefs(c).edit().putInt(key, v).apply()
    fun restore(c: Context, vararg keys: String) {
        val e = prefs(c).edit()
        keys.forEach { e.remove(it) }
        e.apply()
    }
}
