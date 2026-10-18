package com.resonance

import android.content.Context

/** Counts how often each song has been started (used by "Most Played"). */
object PlayStats {
    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("play_stats", Context.MODE_PRIVATE)

    fun increment(context: Context, songId: Long) {
        val p = prefs(context)
        p.edit().putInt("c_$songId", p.getInt("c_$songId", 0) + 1).apply()
    }

    fun counts(context: Context): Map<Long, Int> {
        val result = HashMap<Long, Int>()
        for (entry in prefs(context).all) {
            val value = entry.value
            if (entry.key.startsWith("c_") && value is Int) {
                val id = entry.key.removePrefix("c_").toLongOrNull()
                if (id != null) result[id] = value
            }
        }
        return result
    }
}
