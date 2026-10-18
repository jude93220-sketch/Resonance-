package com.resonance

import android.content.Context

object RatingManager {
    private const val FILE = "ratings"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isLiked(context: Context, songId: Long): Boolean =
        prefs(context).getBoolean("like_$songId", false)

    fun isDisliked(context: Context, songId: Long): Boolean =
        prefs(context).getBoolean("dislike_$songId", false)

    fun saveRating(context: Context, songId: Long, liked: Boolean) {
        prefs(context).edit().putBoolean("like_$songId", liked).apply()
    }

    fun setDisliked(context: Context, songId: Long, disliked: Boolean) {
        prefs(context).edit().putBoolean("dislike_$songId", disliked).apply()
    }

    fun likedIds(context: Context): Set<Long> =
        prefs(context).all
            .filter { (key, value) -> key.startsWith("like_") && value == true }
            .mapNotNull { (key, _) -> key.removePrefix("like_").toLongOrNull() }
            .toSet()
}
