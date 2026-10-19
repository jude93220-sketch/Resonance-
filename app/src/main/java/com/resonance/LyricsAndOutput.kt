package com.resonance

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

object LyricsStore {
    data class Line(val timeMs: Long, val text: String)
    private val cache = ConcurrentHashMap<String, List<Line>>()
    private val timestamp = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?]\\s*(.*)")

    fun load(context: Context, song: Song): List<Line> {
        val key = song.uri.toString()
        cache[key]?.let { if (it.isNotEmpty() || !AppSettings.lyricsOnline(context)) return it }
        val candidates = ArrayList<String>()
        if (song.path.isNotBlank()) {
            val base = song.path.substringBeforeLast('.', song.path)
            candidates += "$base.lrc"
            candidates += "$base.txt"
        }
        val text = candidates.asSequence().mapNotNull { path ->
            try { File(path).takeIf { it.isFile }?.readText() } catch (_: Throwable) { null }
        }.firstOrNull()
        val parsed = text?.let(::parse).orEmpty().ifEmpty {
            if (AppSettings.lyricsOnline(context)) fetchOnline(song) else emptyList()
        }
        cache[key] = parsed
        return parsed
    }

    private fun fetchOnline(song: Song): List<Line> {
        val endpoint = buildString {
            append("https://lrclib.net/api/get?track_name=")
            append(URLEncoder.encode(song.title, "UTF-8"))
            append("&artist_name=")
            append(URLEncoder.encode(song.artist, "UTF-8"))
            if (song.album.isNotBlank()) {
                append("&album_name=")
                append(URLEncoder.encode(song.album, "UTF-8"))
            }
        }
        return try {
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4_000
                readTimeout = 4_000
                setRequestProperty("User-Agent", "Resonance/1.0 (lyrics search)")
            }
            try {
                if (connection.responseCode !in 200..299) return emptyList()
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val synced = json.optString("syncedLyrics", "")
                if (synced.isNotBlank()) {
                    parse(synced)
                } else {
                    val plain = json.optString("plainLyrics", "").trim()
                    if (plain.isBlank()) emptyList() else listOf(Line(0L, plain))
                }
            } finally {
                connection.disconnect()
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun parse(text: String): List<Line> = buildList {
        text.lineSequence().forEach { raw ->
            val match = timestamp.matcher(raw)
            if (match.find()) {
                val minutes = match.group(1)?.toLongOrNull() ?: return@forEach
                val seconds = match.group(2)?.toLongOrNull() ?: 0L
                val fraction = match.group(3)?.padEnd(3, '0')?.take(3)?.toLongOrNull() ?: 0L
                add(Line(minutes * 60_000L + seconds * 1_000L + fraction, match.group(4).orEmpty()))
            }
        }
    }.sortedBy { it.timeMs }
}

data class OutputProfile(
    val key: String,
    val name: String,
    val eqPreset: String = "Flat",
    val preampDb: Float = 0f,
    val autoEqName: String = "",
    val gains: List<Float> = emptyList(),
)

object OutputProfileStore {
    private const val PREFS = "output_profiles"
    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun profile(context: Context, key: String): OutputProfile {
        val p = prefs(context)
        return OutputProfile(
            key = key,
            name = p.getString("${key}_name", key) ?: key,
            eqPreset = p.getString("${key}_preset", "Flat") ?: "Flat",
            preampDb = p.getFloat("${key}_preamp", 0f),
            autoEqName = p.getString("${key}_autoeq", "") ?: "",
            gains = p.getString("${key}_gains", "")?.split(",")?.mapNotNull { it.toFloatOrNull() }.orEmpty(),
        )
    }

    fun save(context: Context, profile: OutputProfile) {
        prefs(context).edit()
            .putString("${profile.key}_name", profile.name)
            .putString("${profile.key}_preset", profile.eqPreset)
            .putFloat("${profile.key}_preamp", profile.preampDb)
            .putString("${profile.key}_autoeq", profile.autoEqName)
            .putString("${profile.key}_gains", profile.gains.joinToString(","))
            .apply()
    }
}

object AutoEqImporter {
    fun parse(name: String, text: String): OutputProfile? {
        val gains = text.lineSequence().firstOrNull { it.contains("preamp", true) }
            ?.substringAfter(':', "")?.trim()?.removeSuffix("dB")?.trim()?.toFloatOrNull() ?: 0f
        val bandGains = text.lineSequence().mapNotNull { line ->
            Regex("(?:^|\\s)(\\d+(?:\\.\\d+)?)\\s*(?:hz)?\\s*[:=]?\\s*(-?\\d+(?:\\.\\d+)?)\\s*d?b", RegexOption.IGNORE_CASE)
                .find(line)?.groupValues?.getOrNull(2)?.toFloatOrNull()
        }.take(10).toList()
        return OutputProfile(key = "imported_${name.hashCode()}", name = name, preampDb = gains, autoEqName = name, gains = bandGains)
    }
}
