package com.resonance

import android.content.Context
import android.net.Uri
import java.nio.charset.Charset
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/** Reads standard ReplayGain tags without requiring a full metadata rewrite library. */
object ReplayGain {
    data class Values(val trackDb: Float?, val albumDb: Float?)

    private val cache = ConcurrentHashMap<String, Values>()
    private val pattern = Pattern.compile(
        "(?:replaygain[_ -](track|album)[_ -]gain)\\s*[:=]\\s*([+-]?\\d+(?:\\.\\d+)?)\\s*dB",
        Pattern.CASE_INSENSITIVE,
    )

    fun read(context: Context, uri: Uri): Values {
        val key = uri.toString()
        cache[key]?.let { return it }
        val values = try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bytes = input.readNBytes(1024 * 1024)
                parse(String(bytes, Charset.forName("ISO-8859-1")))
            } ?: Values(null, null)
        } catch (_: Throwable) {
            Values(null, null)
        }
        cache[key] = values
        return values
    }

    private fun parse(text: String): Values {
        var track: Float? = null
        var album: Float? = null
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            val value = matcher.group(2)?.toFloatOrNull() ?: continue
            when (matcher.group(1)?.lowercase(Locale.US)) {
                "track" -> track = value
                "album" -> album = value
            }
        }
        return Values(track, album)
    }
}
