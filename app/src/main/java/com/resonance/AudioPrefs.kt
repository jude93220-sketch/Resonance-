package com.resonance

import android.content.Context
import android.content.SharedPreferences

/**
 * Shared settings between the UI and the playback service.
 * The UI writes here; the service listens and applies (equalizer, sleep timer).
 */
object AudioPrefs {
    const val K_ENABLED = "fx_enabled"
    const val K_LEVELS = "eq_levels"      // comma separated millibels, one per band
    const val K_BASS = "bass"             // 0..1000
    const val K_VIRT = "virt"             // 0..1000
    const val K_LOUD = "loud"             // 0..1000 (millibels of extra gain)
    const val K_SLEEP_END = "sleep_end"   // epoch millis, 0 = off

    // Written by the service once the equalizer exists
    const val K_BAND_COUNT = "band_count"
    const val K_BAND_MIN = "band_min"
    const val K_BAND_MAX = "band_max"
    const val K_BAND_FREQS = "band_freqs" // milliHz, comma separated

    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences("audio_prefs", Context.MODE_PRIVATE)

    private fun parseInts(raw: String?): List<Int> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    fun levels(context: Context): List<Int> = parseInts(prefs(context).getString(K_LEVELS, ""))

    fun saveLevels(context: Context, levels: List<Int>) {
        prefs(context).edit().putString(K_LEVELS, levels.joinToString(",")).apply()
    }

    fun freqs(context: Context): List<Int> = parseInts(prefs(context).getString(K_BAND_FREQS, ""))

    fun publishBands(context: Context, count: Int, min: Int, max: Int, freqsMilliHz: List<Int>) {
        prefs(context).edit()
            .putInt(K_BAND_COUNT, count)
            .putInt(K_BAND_MIN, min)
            .putInt(K_BAND_MAX, max)
            .putString(K_BAND_FREQS, freqsMilliHz.joinToString(","))
            .apply()
    }

    fun startSleep(context: Context, minutes: Int) {
        prefs(context).edit()
            .putLong(K_SLEEP_END, System.currentTimeMillis() + minutes * 60_000L)
            .apply()
    }

    fun cancelSleep(context: Context) {
        prefs(context).edit().putLong(K_SLEEP_END, 0L).apply()
    }

    fun sleepEnd(context: Context): Long = prefs(context).getLong(K_SLEEP_END, 0L)
}
