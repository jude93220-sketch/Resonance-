package com.resonance

import android.content.Context
import android.net.Uri
import org.w3c.dom.Element
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

object PlaylistFormatParser {
    data class Entry(val location: String, val title: String? = null, val durationSeconds: Int? = null)
    data class CueTrack(val number: Int, val title: String, val performer: String, val file: String, val indexFrames: Int)

    fun read(context: Context, uri: Uri): List<Entry> {
        val name = uri.lastPathSegment.orEmpty().lowercase()
        return context.contentResolver.openInputStream(uri)?.use { input ->
            when {
                name.endsWith(".pls") -> parsePls(input)
                name.endsWith(".wpl") -> parseWpl(input)
                else -> parseM3u(input)
            }
        } ?: emptyList()
    }

    fun readCue(context: Context, uri: Uri): List<CueTrack> =
        context.contentResolver.openInputStream(uri)?.use { parseCue(it) } ?: emptyList()

    private fun parseM3u(input: InputStream): List<Entry> {
        val out = ArrayList<Entry>()
        var pending: Pair<Int?, String?>? = null
        input.bufferedReader().useLines { lines ->
            lines.forEach { raw ->
                val line = raw.trim()
                if (line.isBlank() || line.startsWith("#EXTM3U")) return@forEach
                if (line.startsWith("#EXTINF:", true)) {
                    val payload = line.substringAfter(':')
                    val duration = payload.substringBefore(',').toIntOrNull()
                    val title = payload.substringAfter(',', "").ifBlank { null }
                    pending = duration to title
                } else if (!line.startsWith('#')) {
                    out += Entry(line, pending?.second, pending?.first)
                    pending = null
                }
            }
        }
        return out
    }

    private fun parsePls(input: InputStream): List<Entry> {
        val values = LinkedHashMap<Int, String>()
        input.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val key = line.substringBefore('=', "").trim()
                val value = line.substringAfter('=', "").trim()
                if (key.startsWith("File", true)) key.removePrefix("File").toIntOrNull()?.let { values[it] = value }
            }
        }
        return values.toSortedMap().values.map { Entry(it) }
    }

    private fun parseWpl(input: InputStream): List<Entry> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input)
        val nodes = doc.getElementsByTagName("media")
        return buildList {
            for (i in 0 until nodes.length) {
                val element = nodes.item(i) as? Element ?: continue
                element.getAttribute("src").takeIf { it.isNotBlank() }?.let { add(Entry(it)) }
            }
        }
    }

    private fun parseCue(input: InputStream): List<CueTrack> {
        var file = ""
        var performer = ""
        var title = ""
        var number = 0
        var indexFrames = 0
        val out = ArrayList<CueTrack>()
        fun quoted(line: String): String = line.substringAfter('"').substringBeforeLast('"')
        input.bufferedReader().forEachLine { raw ->
            val line = raw.trim()
            when {
                line.startsWith("FILE ", true) -> file = quoted(line)
                line.startsWith("PERFORMER ", true) -> performer = quoted(line)
                line.startsWith("TRACK ", true) -> {
                    if (number > 0) out += CueTrack(number, title, performer, file, indexFrames)
                    number = line.split(Regex("\\s+"))[1].toIntOrNull() ?: 0
                    title = ""
                    indexFrames = 0
                }
                line.startsWith("TITLE ", true) -> title = quoted(line)
                line.startsWith("INDEX 01 ", true) -> {
                    val p = line.substringAfter("INDEX 01 ").trim().split(':')
                    if (p.size == 2) indexFrames = (p[0].toIntOrNull() ?: 0) * 75 * 60 + (p[1].toIntOrNull() ?: 0) * 75
                }
            }
        }
        if (number > 0) out += CueTrack(number, title, performer, file, indexFrames)
        return out
    }
}
