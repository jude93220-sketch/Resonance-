package com.resonance

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

object LyricsStore {
    data class Line(val timeMs: Long, val text: String)
    private val cache = ConcurrentHashMap<String, List<Line>>()
    private val timestamp = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?]\\s*(.*)")

    fun load(context: Context, song: Song): List<Line> {
        val key = song.uri.toString()
        cache[key]?.let { return it }
        val candidates = ArrayList<String>()
        if (song.path.isNotBlank()) {
            val base = song.path.substringBeforeLast('.', song.path)
            candidates += "$base.lrc"
            candidates += "$base.txt"
        }
        val text = candidates.asSequence().mapNotNull { path ->
            try { File(path).takeIf { it.isFile }?.readText() } catch (_: Throwable) { null }
        }.firstOrNull()
        val parsed = text?.let(::parse).orEmpty()
        cache[key] = parsed
        return parsed
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
