package com.resonance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private class Preset(val name: String, val gainsDb: List<Float>)

private val PRESETS = listOf(
    Preset("Flat", listOf(0f, 0f, 0f, 0f, 0f)),
    Preset("Bass boost", listOf(6f, 4f, 0f, 0f, 0f)),
    Preset("Treble boost", listOf(0f, 0f, 0f, 3f, 6f)),
    Preset("Vocal", listOf(-2f, 0f, 4f, 3f, 0f)),
    Preset("Rock", listOf(4f, 2f, -1f, 2f, 4f)),
    Preset("Pop", listOf(-1f, 2f, 4f, 2f, -1f)),
    Preset("Jazz", listOf(3f, 2f, -2f, 2f, 4f)),
    Preset("Classical", listOf(4f, 2f, -1f, 2f, 3f)),
    Preset("Electronic", listOf(5f, 3f, 0f, 2f, 4f)),
    Preset("Hip-hop", listOf(5f, 4f, 1f, 2f, 3f)),
)

private fun resample(gains: List<Float>, bands: Int): List<Float> {
    if (bands <= 1) return listOf(gains.first())
    return List(bands) { i ->
        val pos = i * (gains.size - 1f) / (bands - 1f)
        val lowIdx = floor(pos).toInt().coerceIn(0, gains.size - 1)
        val highIdx = (lowIdx + 1).coerceAtMost(gains.size - 1)
        val frac = pos - lowIdx
        gains[lowIdx] * (1f - frac) + gains[highIdx] * frac
    }
}

@Composable
fun EqualizerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AudioPrefs.prefs(context) }

    var enabled by remember { mutableStateOf(prefs.getBoolean(AudioPrefs.K_ENABLED, false)) }
    var bandCount by remember { mutableIntStateOf(prefs.getInt(AudioPrefs.K_BAND_COUNT, 0)) }
    var bass by remember { mutableIntStateOf(prefs.getInt(AudioPrefs.K_BASS, 0)) }
    var virt by remember { mutableIntStateOf(prefs.getInt(AudioPrefs.K_VIRT, 0)) }
    var loud by remember { mutableIntStateOf(prefs.getInt(AudioPrefs.K_LOUD, 0)) }

    // The service publishes the equalizer layout when it starts; wait for it if needed.
    LaunchedEffect(Unit) {
        while (bandCount == 0) {
            delay(500)
            bandCount = prefs.getInt(AudioPrefs.K_BAND_COUNT, 0)
        }
    }

    val lo = remember(bandCount) { prefs.getInt(AudioPrefs.K_BAND_MIN, -1500) }
    val hi = remember(bandCount) { prefs.getInt(AudioPrefs.K_BAND_MAX, 1500) }
    val freqs = remember(bandCount) { AudioPrefs.freqs(context) }
    var levels by remember(bandCount) {
        val saved = AudioPrefs.levels(context)
        mutableStateOf(List(bandCount) { i -> saved.getOrNull(i) ?: 0 })
    }

    fun setLevel(index: Int, value: Int) {
        if (index !in levels.indices) return
        val updated = levels.toMutableList()
        updated[index] = value.coerceIn(lo, hi)
        levels = updated
        AudioPrefs.saveLevels(context, updated)
    }

    fun applyPreset(p: Preset) {
        val updated = resample(p.gainsDb, bandCount).map { (it * 100f).roundToInt().coerceIn(lo, hi) }
        levels = updated
        AudioPrefs.saveLevels(context, updated)
    }

    fun setEnabled(v: Boolean) {
        enabled = v
        prefs.edit().putBoolean(AudioPrefs.K_ENABLED, v).apply()
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Text(
                "Equalizer", color = Color.White, fontSize = 22.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
            )
            Switch(
                checked = enabled,
                onCheckedChange = { setEnabled(it) },
                colors = SwitchDefaults.colors(checkedTrackColor = Accent, checkedThumbColor = Color.Black),
            )
        }

        Column(Modifier.alpha(if (enabled) 1f else 0.45f)) {
            if (bandCount == 0) {
                Text(
                    "Waiting for the audio engine… play any song and this will unlock.",
                    color = Color(0xFF9A9A9A), fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PRESETS.forEach { p ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(PillGrey)
                                .clickable { applyPreset(p) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(p.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth().height(240.dp)) {
                    for (i in 0 until bandCount) {
                        val level = levels.getOrElse(i) { 0 }
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("%+.0f".format(level / 100f), color = Color.White, fontSize = 12.sp)
                            BandSlider(
                                level = level, lo = lo, hi = hi,
                                onChange = { setLevel(i, it) },
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                            )
                            Text(freqLabel(freqs.getOrNull(i) ?: 0), color = Color(0xFF9A9A9A), fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            EffectSlider("Bass boost", "${bass / 10}%", bass.toFloat(), 0f..1000f) {
                bass = it.roundToInt()
                prefs.edit().putInt(AudioPrefs.K_BASS, bass).apply()
            }
            EffectSlider("Stereo widening", "${virt / 10}%", virt.toFloat(), 0f..1000f) {
                virt = it.roundToInt()
                prefs.edit().putInt(AudioPrefs.K_VIRT, virt).apply()
            }
            EffectSlider("Loudness / preamp", "+%.1f dB".format(loud / 100f), loud.toFloat(), 0f..1000f) {
                loud = it.roundToInt()
                prefs.edit().putInt(AudioPrefs.K_LOUD, loud).apply()
            }

            TextButton(onClick = {
                applyPreset(PRESETS.first())
                bass = 0; virt = 0; loud = 0
                prefs.edit()
                    .putInt(AudioPrefs.K_BASS, 0)
                    .putInt(AudioPrefs.K_VIRT, 0)
                    .putInt(AudioPrefs.K_LOUD, 0)
                    .apply()
            }) {
                Text("Reset everything", color = Accent)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun freqLabel(milliHz: Int): String {
    val hz = milliHz / 1000f
    return if (hz >= 1000f) "%.1fk".format(hz / 1000f) else "%.0f".format(hz)
}

@Composable
private fun EffectSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(valueText, color = Color(0xFF9A9A9A), fontSize = 13.sp)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Accent,
                activeTrackColor = Accent,
                inactiveTrackColor = Color(0xFF2A2A2A),
            ),
        )
    }
}

@Composable
private fun BandSlider(
    level: Int,
    lo: Int,
    hi: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latest by rememberUpdatedState(onChange)
    val span = (hi - lo).coerceAtLeast(1)

    fun valueAt(y: Float, height: Float): Int {
        val f = 1f - (y / height).coerceIn(0f, 1f)
        return (lo + f * span).roundToInt()
    }

    Canvas(
        modifier
            .pointerInput(lo, hi) {
                detectTapGestures { offset -> latest(valueAt(offset.y, size.height.toFloat())) }
            }
            .pointerInput(lo, hi) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        latest(valueAt(change.position.y, size.height.toFloat()))
                    }
                )
            }
    ) {
        val x = size.width / 2f
        val stroke = 6.dp.toPx()
        val thumbY = size.height * (1f - (level - lo).toFloat() / span)
        val zeroY = size.height * (1f - (0 - lo).toFloat() / span)

        drawLine(Color(0xFF2A2A2A), Offset(x, 0f), Offset(x, size.height), stroke, StrokeCap.Round)
        drawLine(Accent, Offset(x, zeroY), Offset(x, thumbY), stroke, StrokeCap.Round)
        drawLine(Color(0xFF555555), Offset(x - 10.dp.toPx(), zeroY), Offset(x + 10.dp.toPx(), zeroY), 2.dp.toPx())
        drawCircle(Color.White, 11.dp.toPx(), Offset(x, thumbY))
    }
}
