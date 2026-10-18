package com.resonance

import android.content.Context

object RatingManager {
    private const val PREFS_NAME = "resonance_ratings"

    fun saveRating(context: Context, songId: Long, isLiked: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(songId.toString(), isLiked).apply()
    }

    fun isLiked(context: Context, songId: Long): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(songId.toString(), false)
    }
}
