package com.resonance

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

class EqPreset(
    val name: String,
    val gains: List<Float>,
    val preamp: Float,
    val builtin: Boolean,
)

data class ParametricBand(val frequency: Int, val q: Float, val gain: Float)

/** Everything the equalizer remembers. The UI writes here, PlaybackService listens and applies it. */
object EqStore {
    private const val PREFS = "resonance_eq"

    const val K_MASTER = "eq_master"
    const val K_EQ_ON = "eq_on"
    const val K_TONE_ON = "eq_tone_on"
    const val K_LIMIT_ON = "eq_limit_on"
    const val K_GAINS = "eq_gains"
    const val K_PREAMP = "eq_preamp"
    const val K_BASS = "eq_bass"
    const val K_TREBLE = "eq_treble"
    const val K_BASS_BOOST = "eq_bass_boost"
    const val K_STEREO_WIDTH = "eq_stereo_width"
    const val K_PRESET = "eq_preset_name"
    const val K_USER = "eq_user_presets"
    const val K_ENGINE = "eq_engine"
    const val K_BAND_COUNT = "eq_band_count"
    const val K_MODE = "eq_mode"
    const val K_PARAMETRIC = "eq_parametric"

    val KEYS = setOf(
        K_MASTER, K_EQ_ON, K_TONE_ON, K_LIMIT_ON, K_GAINS, K_PREAMP, K_BASS, K_TREBLE,
        K_BASS_BOOST, K_STEREO_WIDTH,
        K_BAND_COUNT, K_MODE, K_PARAMETRIC,
    )

    val FREQS = intArrayOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
    val BAND_FREQS = mapOf(
        10 to FREQS,
        20 to intArrayOf(25, 32, 40, 50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630, 800, 1000, 2000, 4000, 16000),
        32 to intArrayOf(25, 31, 40, 50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630, 800, 1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000, 12500, 14000, 16000, 18000, 20000),
    )
    const val MAX_DB = 15f
    const val MIN_DB = -15f

    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun master(c: Context) = prefs(c).getBoolean(K_MASTER, true)
    fun eqOn(c: Context) = prefs(c).getBoolean(K_EQ_ON, true)
    fun toneOn(c: Context) = prefs(c).getBoolean(K_TONE_ON, true)
    fun limitOn(c: Context) = prefs(c).getBoolean(K_LIMIT_ON, true)

    fun setMaster(c: Context, v: Boolean) = prefs(c).edit().putBoolean(K_MASTER, v).apply()
    fun setEqOn(c: Context, v: Boolean) = prefs(c).edit().putBoolean(K_EQ_ON, v).apply()
    fun setToneOn(c: Context, v: Boolean) = prefs(c).edit().putBoolean(K_TONE_ON, v).apply()
    fun setLimitOn(c: Context, v: Boolean) = prefs(c).edit().putBoolean(K_LIMIT_ON, v).apply()

    fun gains(c: Context): List<Float> {
        val parts = prefs(c).getString(K_GAINS, null)?.split(",") ?: emptyList()
        return List(FREQS.size) { i -> parts.getOrNull(i)?.toFloatOrNull() ?: 0f }
    }

    fun saveGains(c: Context, gains: List<Float>) {
        prefs(c).edit().putString(K_GAINS, joinGains(gains)).apply()
    }

    fun preamp(c: Context) = prefs(c).getFloat(K_PREAMP, 0f)
    fun savePreamp(c: Context, v: Float) = prefs(c).edit().putFloat(K_PREAMP, v).apply()

    fun bass(c: Context) = prefs(c).getInt(K_BASS, 0)
    fun saveBass(c: Context, v: Int) = prefs(c).edit().putInt(K_BASS, v).apply()
    fun treble(c: Context) = prefs(c).getInt(K_TREBLE, 50)
    fun saveTreble(c: Context, v: Int) = prefs(c).edit().putInt(K_TREBLE, v).apply()

    fun bassBoost(c: Context) = prefs(c).getInt(K_BASS_BOOST, 0)
    fun saveBassBoost(c: Context, v: Int) =
        prefs(c).edit().putInt(K_BASS_BOOST, v.coerceIn(0, 100)).apply()

    fun stereoWidth(c: Context) = prefs(c).getInt(K_STEREO_WIDTH, 0)
    fun saveStereoWidth(c: Context, v: Int) =
        prefs(c).edit().putInt(K_STEREO_WIDTH, v.coerceIn(0, 100)).apply()

    fun presetName(c: Context) = prefs(c).getString(K_PRESET, "") ?: ""
    fun savePresetName(c: Context, v: String) = prefs(c).edit().putString(K_PRESET, v).apply()

    fun engine(c: Context) = prefs(c).getString(K_ENGINE, "") ?: ""
    fun setEngine(c: Context, v: String) = prefs(c).edit().putString(K_ENGINE, v).apply()
    fun bandCount(c: Context) = prefs(c).getInt(K_BAND_COUNT, 10).let { if (it in BAND_FREQS) it else 10 }
    fun setBandCount(c: Context, v: Int) = prefs(c).edit().putInt(K_BAND_COUNT, v.coerceIn(10, 32)).apply()
    fun mode(c: Context) = prefs(c).getString(K_MODE, "graphic") ?: "graphic"
    fun setMode(c: Context, v: String) = prefs(c).edit().putString(K_MODE, v).apply()
    fun frequencies(c: Context): IntArray = BAND_FREQS[bandCount(c)] ?: FREQS

    fun parametricBands(c: Context): List<ParametricBand> {
        val raw = prefs(c).getString(K_PARAMETRIC, "80:1.0:0,1000:1.0:0,10000:1.0:0") ?: ""
        return raw.split(",").mapNotNull { part ->
            val p = part.split(":")
            if (p.size == 3) ParametricBand(p[0].toIntOrNull() ?: return@mapNotNull null, p[1].toFloatOrNull() ?: 1f, p[2].toFloatOrNull() ?: 0f) else null
        }.take(6)
    }
    fun saveParametricBands(c: Context, bands: List<ParametricBand>) {
        val raw = bands.joinToString(",") { "${it.frequency}:${it.q}:${it.gain}" }
        prefs(c).edit().putString(K_PARAMETRIC, raw).apply()
    }

    fun resetAll(c: Context) {
        prefs(c).edit()
            .putString(K_GAINS, joinGains(List(FREQS.size) { 0f }))
            .putFloat(K_PREAMP, 0f)
            .putInt(K_BASS, 0)
            .putInt(K_TREBLE, 50)
            .putInt(K_BASS_BOOST, 0)
            .putInt(K_STEREO_WIDTH, 0)
            .putString(K_PRESET, "Flat")
            .apply()
    }

    private fun joinGains(g: List<Float>) =
        g.joinToString(",") { String.format(Locale.US, "%.1f", it) }

    // ----------------------------------------------------------------- presets

    private fun builtin(name: String, vararg g: Float): EqPreset {
        val peak = max(0f, g.maxOrNull() ?: 0f)
        val pre = -(Math.round(peak * 0.6f * 10f) / 10f)
        return EqPreset(name, g.toList(), pre, true)
    }

    val BUILTIN: List<EqPreset> = listOf(
        builtin("Flat", 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        builtin("Bass Boost", 6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f),
        builtin("Bass Reducer", -6f, -5f, -4f, -2f, 0f, 0f, 0f, 0f, 0f, 0f),
        builtin("Treble Boost", 0f, 0f, 0f, 0f, 0f, 1f, 2f, 4f, 5f, 6f),
        builtin("Treble Reducer", 0f, 0f, 0f, 0f, 0f, -1f, -2f, -4f, -5f, -6f),
        builtin("Vocal Boost", -2f, -3f, -3f, 1f, 3f, 3f, 3f, 1f, 0f, -1f),
        builtin("Rock", 5f, 4f, 3f, 1f, -1f, -1f, 1f, 3f, 4f, 5f),
        builtin("Pop", -1f, 1f, 3f, 4f, 3f, 0f, -1f, -1f, 1f, 2f),
        builtin("Jazz", 4f, 3f, 1f, 2f, -2f, -2f, 0f, 1f, 3f, 4f),
        builtin("Classical", 4f, 3f, 2f, 2f, -1f, -1f, 0f, 2f, 3f, 4f),
        builtin("Electronic", 5f, 4f, 1f, 0f, -2f, 2f, 1f, 1f, 4f, 5f),
        builtin("Hip-Hop", 5f, 4f, 1f, 3f, -1f, -1f, 1f, -1f, 2f, 3f),
        builtin("Dance", 6f, 4f, 2f, 0f, 0f, -2f, -4f, -4f, 0f, 0f),
        builtin("Loudness", 6f, 4f, 0f, 0f, -2f, 0f, -1f, -5f, 5f, 1f),
        builtin("Acoustic", 5f, 4f, 3f, 1f, 2f, 1f, 3f, 3f, 3f, 2f),
    )

    fun userPresets(c: Context): List<EqPreset> {
        val raw = prefs(c).getString(K_USER, "") ?: ""
        val out = ArrayList<EqPreset>()
        for (line in raw.split("\n")) {
            val p = line.split("|")
            if (p.size < 3) continue
            val g = p[2].split(",").map { it.toFloatOrNull() ?: 0f }
            if (g.size != FREQS.size) continue
            out.add(EqPreset(p[0], g, p[1].toFloatOrNull() ?: 0f, false))
        }
        return out
    }

    private fun writeUsers(c: Context, list: List<EqPreset>) {
        val text = list.joinToString("\n") {
            it.name + "|" + String.format(Locale.US, "%.1f", it.preamp) + "|" + joinGains(it.gains)
        }
        prefs(c).edit().putString(K_USER, text).apply()
    }

    fun saveUserPreset(c: Context, rawName: String, gains: List<Float>, preamp: Float): String {
        val name = rawName.replace("|", " ").replace("\n", " ").trim().ifEmpty { "My preset" }
        val list = userPresets(c).filter { it.name != name }.toMutableList()
        list.add(EqPreset(name, gains, preamp, false))
        writeUsers(c, list)
        return name
    }

    fun deleteUserPreset(c: Context, name: String) {
        writeUsers(c, userPresets(c).filter { it.name != name })
    }

    // -------------------------------------------------------------------- maths

    /** Gain added to each band by the Bass / Treble knobs (dB). */
    fun toneDb(freq: Int, bass: Int, treble: Int): Float {
        val f = freq.toFloat()
        val bassW = 1f / (1f + (f / 120f) * (f / 120f))
        val trebleW = 1f / (1f + (2500f / f) * (2500f / f))
        val b = bass / 100f * 9f * bassW
        val t = (treble - 50) / 50f * 8f * trebleW
        return b + t
    }

    /** The per-band gain the audio engine really uses: EQ bands plus tone knobs. */
    fun effectiveGains(
        gains: List<Float>, bass: Int, treble: Int, eqOn: Boolean, toneOn: Boolean,
        frequencies: IntArray = FREQS, parametric: List<ParametricBand> = emptyList(),
    ): FloatArray = FloatArray(frequencies.size) { i ->
        val base = if (eqOn) interpolate(gains.toFloatArray(), frequencies[i].toFloat()) else 0f
        val tone = if (toneOn) toneDb(frequencies[i], bass, treble) else 0f
        val param = if (modeFrom(parametric)) parametric.sumOf { band ->
            val distance = ln(frequencies[i].toFloat() / band.frequency) / (band.q.coerceAtLeast(0.1f))
            (band.gain * exp(-0.5f * distance * distance)).toDouble()
        }.toFloat() else 0f
        (base + tone + param).coerceIn(-20f, 20f)
    }

    private fun modeFrom(parametric: List<ParametricBand>) = parametric.isNotEmpty()

    /** Upper edge of every band (geometric middle between neighbours). */
    fun bandEdges(): FloatArray = FloatArray(FREQS.size) { i ->
        if (i == FREQS.size - 1) 20000f
        else sqrt(FREQS[i].toFloat() * FREQS[i + 1].toFloat())
    }

    /** Smooth response for the little graph on the equalizer screen. */
    fun curveDb(g: FloatArray, freq: Float): Float {
        var sum = 0f
        var weights = 0f
        for (i in FREQS.indices) {
            val x = ln(freq / FREQS[i]) / 0.5f
            val w = exp(-0.5f * x * x)
            sum += w * g[i]
            weights += w
        }
        return if (weights > 0f) sum / weights else 0f
    }

    /** Value of the 10-band curve at any frequency (used when the phone only has a basic equalizer). */
    fun interpolate(g: FloatArray, hz: Float): Float {
        if (hz <= FREQS.first()) return g.first()
        if (hz >= FREQS.last()) return g.last()
        for (i in 0 until FREQS.size - 1) {
            val a = FREQS[i].toFloat()
            val b = FREQS[i + 1].toFloat()
            if (hz in a..b) {
                val t = ln(hz / a) / ln(b / a)
                return g[i] * (1f - t) + g[i + 1] * t
            }
        }
        return 0f
    }

    class Snapshot(
        val master: Boolean,
        val eqOn: Boolean,
        val toneOn: Boolean,
        val limitOn: Boolean,
        val preamp: Float,
        val gains: FloatArray,
        val frequencies: IntArray,
    )

    fun snapshot(c: Context): Snapshot {
        val eq = eqOn(c)
        val tone = toneOn(c)
        val frequencies = frequencies(c)
        val rawGains = gains(c)
        return Snapshot(
            master = master(c),
            eqOn = eq,
            toneOn = tone,
            limitOn = limitOn(c),
            preamp = preamp(c),
            gains = effectiveGains(rawGains, bass(c), treble(c), eq, tone, frequencies, if (mode(c) == "parametric") parametricBands(c) else emptyList()),
            frequencies = frequencies,
        )
    }

    fun zeroish(v: Float) = abs(v) < 0.2f
}
