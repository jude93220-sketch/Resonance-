package com.resonance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

private val EqBlue = Color(0xFF3B6FE0)
private val PanelDark = Color(0xFF161616)
private val PillBg = Color(0xFF1C1C1C)

private fun db1(v: Float): String = "%.1f".format(v)

private fun freqText(hz: Int): String = if (hz >= 1000) "${hz / 1000}K" else "$hz"

private fun bandColor(db: Float): Color {
    val teal = Color(0xFF14E3A0)
    val green = Color(0xFF2EE82E)
    val yellow = Color(0xFFEAF21A)
    return if (db >= 0f) lerp(green, yellow, (db / 9f).coerceIn(0f, 1f))
    else lerp(green, teal, (-db / 9f).coerceIn(0f, 1f))
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun EqualizerScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var master by remember { mutableStateOf(EqStore.master(context)) }
    var eqOn by remember { mutableStateOf(EqStore.eqOn(context)) }
    var toneOn by remember { mutableStateOf(EqStore.toneOn(context)) }
    var limitOn by remember { mutableStateOf(EqStore.limitOn(context)) }
    var gains by remember { mutableStateOf(EqStore.gains(context)) }
    var preamp by remember { mutableStateOf(EqStore.preamp(context)) }
    var bass by remember { mutableIntStateOf(EqStore.bass(context)) }
    var treble by remember { mutableIntStateOf(EqStore.treble(context)) }
    var bassBoost by remember { mutableIntStateOf(EqStore.bassBoost(context)) }
    var stereoWidth by remember { mutableIntStateOf(EqStore.stereoWidth(context)) }
    var presetName by remember { mutableStateOf(EqStore.presetName(context)) }
    var userPresets by remember { mutableStateOf(EqStore.userPresets(context)) }
    var engine by remember { mutableStateOf(EqStore.engine(context)) }
    var eqMode by remember { mutableStateOf(EqStore.mode(context)) }
    var parametric by remember { mutableStateOf(EqStore.parametricBands(context)) }

    var showPresets by remember { mutableStateOf(false) }
    var showSave by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }

    // The service reports which engine it is using once playback has started.
    LaunchedEffect(Unit) {
        while (true) {
            engine = EqStore.engine(context)
            kotlinx.coroutines.delay(1000)
        }
    }

    fun clearPresetName() {
        presetName = ""
        EqStore.savePresetName(context, "")
    }

    fun setGain(i: Int, v: Float) {
        val g = gains.toMutableList()
        g[i] = v
        gains = g
        EqStore.saveGains(context, g)
        clearPresetName()
    }

    fun setPreamp(v: Float) {
        preamp = v
        EqStore.savePreamp(context, v)
        clearPresetName()
    }

    fun applyPreset(p: EqPreset) {
        gains = p.gains
        preamp = p.preamp
        EqStore.saveGains(context, p.gains)
        EqStore.savePreamp(context, p.preamp)
        presetName = p.name
        EqStore.savePresetName(context, p.name)
    }

    val effective = EqStore.effectiveGains(gains, bass, treble, eqOn, toneOn)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
    ) {
        // ---------------------------------------------------------- top bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(PanelDark)
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Tune, null, tint = Color.White, modifier = Modifier.size(30.dp))
            Text(
                "Equalizer", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 14.dp),
            )
            Switch(
                checked = master,
                onCheckedChange = {
                    master = it
                    EqStore.setMaster(context, it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = EqBlue,
                    checkedBorderColor = Color.Transparent,
                    uncheckedThumbColor = Color(0xFFBDBDBD),
                    uncheckedTrackColor = Color(0xFF3A3A3A),
                    uncheckedBorderColor = Color.Transparent,
                ),
            )
        }

        Spacer(Modifier.height(10.dp))

        Column(Modifier.alpha(if (master) 1f else 0.45f)) {
            // ------------------------------------------------------ sliders
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val colW = maxWidth / 8.6f
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    // Preamp panel
                    Column(
                        Modifier
                            .width(colW + 12.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(PanelDark)
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        VerticalBand(
                            value = preamp, min = -15f, max = 15f, centered = true,
                            color = Color(0xFF19D8C8),
                            onChange = { setPreamp(it) },
                            onReset = { setPreamp(0f) },
                            modifier = Modifier.fillMaxWidth().height(270.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text("Preamp", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(db1(preamp), color = Color(0xFF9A9A9A), fontSize = 14.sp)
                    }
                    for (i in EqStore.FREQS.indices) {
                        val g = gains[i]
                        Column(Modifier.width(colW), horizontalAlignment = Alignment.CenterHorizontally) {
                            VerticalBand(
                                value = g, min = -15f, max = 15f, centered = false,
                                color = bandColor(g),
                                onChange = { setGain(i, it) },
                                onReset = { setGain(i, 0f) },
                                modifier = Modifier.fillMaxWidth().height(270.dp).padding(top = 8.dp),
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                freqText(EqStore.FREQS[i]), color = Color.White,
                                fontSize = 17.sp, fontWeight = FontWeight.Bold,
                            )
                            Text(db1(g), color = Color(0xFF9A9A9A), fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            ResponseGraph(effective)

            Spacer(Modifier.height(12.dp))

            // ------------------------------------------------------ info pill
            val engineName = when {
                engine == "DP" -> "DP"
                engine.startsWith("SYS") -> engine.replace(" ", "")
                else -> "—"
            }
            val flags = buildString {
                append(engineName)
                append(if (eqOn) "  EQ ${EqStore.bandCount(context)}" else "  NO EQ")
                if (toneOn) append("  TON")
                if (limitOn) append("  LMT")
                if (bassBoost > 0) append("  BB")
                if (stereoWidth > 0) append("  ST")
            }
            Row(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(50))
                    .background(PanelDark)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Info, null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(18.dp))
                Text(
                    flags, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            Spacer(Modifier.height(14.dp))

            // ------------------------------------------------- controls row
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TogglePill("Equ", eqOn) {
                        eqOn = it
                        EqStore.setEqOn(context, it)
                    }
                    TogglePill("Tone", toneOn) {
                        toneOn = it
                        EqStore.setToneOn(context, it)
                    }
                    TogglePill("Limit", limitOn) {
                        limitOn = it
                        EqStore.setLimitOn(context, it)
                    }
                    TogglePill("Param", eqMode == "parametric") {
                        eqMode = if (it) "parametric" else "graphic"
                        EqStore.setMode(context, eqMode)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(50))
                                .border(BorderStroke(1.5.dp, Color(0xFF4A4A4A)), RoundedCornerShape(50))
                                .clickable { showPresets = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (presetName.isEmpty()) "Preset" else presetName,
                                color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                                maxLines = 1,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Box {
                            Box(
                                Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .border(BorderStroke(1.5.dp, Color(0xFF4A4A4A)), CircleShape)
                                    .clickable { showMore = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.MoreVert, "More", tint = Color.White)
                            }
                            DropdownMenu(expanded = showMore, onDismissRequest = { showMore = false }) {
                                DropdownMenuItem(
                                    text = { Text("Save as preset") },
                                    onClick = {
                                        showMore = false
                                        showSave = true
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Reset everything") },
                                    onClick = {
                                        showMore = false
                                        EqStore.resetAll(context)
                                        gains = EqStore.gains(context)
                                        preamp = 0f
                                        bass = 0
                                        treble = 50
                                        bassBoost = 0
                                        stereoWidth = 0
                                        presetName = "Flat"
                                    },
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Bass  $bass%", color = Color(0xFFBDBDBD), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Knob(
                                value = bass, color = Color(0xFFFFC107),
                                onChange = {
                                    bass = it
                                    EqStore.saveBass(context, it)
                                },
                                onReset = {
                                    bass = 0
                                    EqStore.saveBass(context, 0)
                                },
                                modifier = Modifier.size(92.dp),
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Treble  $treble%", color = Color(0xFFBDBDBD), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Knob(
                                value = treble, color = Color(0xFF22E040),
                                onChange = {
                                    treble = it
                                    EqStore.saveTreble(context, it)
                                },
                                onReset = {
                                    treble = 50
                                    EqStore.saveTreble(context, 50)
                                },
                                modifier = Modifier.size(92.dp),
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Bass boost  $bassBoost%", color = Color(0xFFBDBDBD), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Knob(
                                value = bassBoost, color = Color(0xFFFF7043),
                                onChange = {
                                    bassBoost = it
                                    EqStore.saveBassBoost(context, it)
                                },
                                onReset = {
                                    bassBoost = 0
                                    EqStore.saveBassBoost(context, 0)
                                },
                                modifier = Modifier.size(92.dp),
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Stereo width  $stereoWidth%", color = Color(0xFFBDBDBD), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Knob(
                                value = stereoWidth, color = Color(0xFF42A5F5),
                                onChange = {
                                    stereoWidth = it
                                    EqStore.saveStereoWidth(context, it)
                                },
                                onReset = {
                                    stereoWidth = 0
                                    EqStore.saveStereoWidth(context, 0)
                                },
                                modifier = Modifier.size(92.dp),
                            )
                        }
                    }
                }
            }
            if (eqMode == "parametric") {
                ParametricBands(
                    bands = parametric,
                    onChange = { updated ->
                        parametric = updated
                        EqStore.saveParametricBands(context, updated)
                    },
                )
                Spacer(Modifier.height(18.dp))
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // --------------------------------------------------------------- dialogs
    if (showPresets) {
        AlertDialog(
            onDismissRequest = { showPresets = false },
            confirmButton = { TextButton(onClick = { showPresets = false }) { Text("Close") } },
            title = { Text("Presets") },
            text = {
                Column(
                    Modifier
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (userPresets.isNotEmpty()) {
                        Text("My presets", color = Color(0xFF9A9A9A), fontSize = 13.sp)
                        userPresets.forEach { p ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        applyPreset(p)
                                        showPresets = false
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(p.name, modifier = Modifier.weight(1f).padding(vertical = 8.dp))
                                IconButton(onClick = {
                                    EqStore.deleteUserPreset(context, p.name)
                                    userPresets = EqStore.userPresets(context)
                                }) { Icon(Icons.Filled.Close, "Delete preset") }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Built-in", color = Color(0xFF9A9A9A), fontSize = 13.sp)
                    }
                    EqStore.BUILTIN.forEach { p ->
                        Text(
                            p.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    applyPreset(p)
                                    showPresets = false
                                }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            },
        )
    }

    if (showSave) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSave = false },
            confirmButton = {
                TextButton(onClick = {
                    val saved = EqStore.saveUserPreset(context, name, gains, preamp)
                    userPresets = EqStore.userPresets(context)
                    presetName = saved
                    EqStore.savePresetName(context, saved)
                    showSave = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showSave = false }) { Text("Cancel") } },
            title = { Text("Save preset") },
            text = {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    placeholder = { Text("Preset name") },
                )
            },
        )
    }
}

// ------------------------------------------------------------------- widgets

@Composable
private fun ParametricBands(bands: List<ParametricBand>, onChange: (List<ParametricBand>) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(PanelDark).padding(12.dp),
    ) {
        Text("Parametric bands · gain mapped to active engine", color = Color.White, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            bands.take(3).forEachIndexed { index, band ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${band.frequency} Hz", color = Color.White, fontSize = 13.sp)
                    Knob(
                        value = ((band.gain + 15f) / 30f * 100f).roundToInt(),
                        color = Color(0xFF14E3A0),
                        onChange = { raw ->
                            val updated = bands.toMutableList()
                            updated[index] = band.copy(gain = raw / 100f * 30f - 15f)
                            onChange(updated)
                        },
                        onReset = {
                            val updated = bands.toMutableList()
                            updated[index] = band.copy(gain = 0f)
                            onChange(updated)
                        },
                        modifier = Modifier.size(78.dp),
                    )
                    Text(db1(band.gain), color = Color(0xFFBDBDBD), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun TogglePill(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Box(
        Modifier
            .width(76.dp)
            .height(38.dp)
            .clip(RoundedCornerShape(50))
            .background(PillBg)
            .clickable { onChange(!on) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (on) Color.White else Color(0xFF6E6E6E),
            fontSize = 15.sp, fontWeight = FontWeight.Bold,
        )
    }
}

/** One vertical fader in the Poweramp style: thin track, glowing lit part, rounded thumb. */
@Composable
private fun VerticalBand(
    value: Float,
    min: Float,
    max: Float,
    centered: Boolean,
    color: Color,
    onChange: (Float) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val latest by rememberUpdatedState(onChange)
    val latestReset by rememberUpdatedState(onReset)

    fun valueAt(y: Float, height: Float, pad: Float): Float {
        val top = pad
        val bottom = height - pad
        val f = ((bottom - y) / (bottom - top)).coerceIn(0f, 1f)
        val raw = min + f * (max - min)
        val snapped = (raw * 10f).roundToInt() / 10f
        return if (abs(snapped) < 0.25f) 0f else snapped
    }

    Canvas(
        modifier
            .pointerInput(min, max) {
                detectTapGestures(
                    onDoubleTap = { latestReset() },
                    onTap = { offset ->
                        latest(valueAt(offset.y, size.height.toFloat(), 29.dp.toPx()))
                    },
                )
            }
            .pointerInput(min, max) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        latest(valueAt(change.position.y, size.height.toFloat(), 29.dp.toPx()))
                    }
                )
            }
    ) {
        val cx = size.width / 2f
        val thumbW = 30.dp.toPx()
        val thumbH = 58.dp.toPx()
        val pad = thumbH / 2f
        val top = pad
        val bottom = size.height - pad
        val frac = ((value - min) / (max - min)).coerceIn(0f, 1f)
        val thumbY = bottom - frac * (bottom - top)
        val zeroY = bottom - ((0f - min) / (max - min)) * (bottom - top)

        // track
        drawLine(Color(0xFF3A3A3A), Offset(cx, 0f), Offset(cx, size.height), 3.dp.toPx(), StrokeCap.Round)

        // scale dashes at +15, 0 and -15
        val dashColor = Color(0xFF555555)
        for (y in listOf(top, zeroY, bottom)) {
            drawLine(dashColor, Offset(cx - 26.dp.toPx(), y), Offset(cx - 20.dp.toPx(), y), 2.dp.toPx())
            drawLine(dashColor, Offset(cx + 20.dp.toPx(), y), Offset(cx + 26.dp.toPx(), y), 2.dp.toPx())
        }

        // lit part
        val litFrom = if (centered) zeroY else size.height
        drawLine(color.copy(alpha = 0.25f), Offset(cx, litFrom), Offset(cx, thumbY), 11.dp.toPx(), StrokeCap.Round)
        drawLine(color, Offset(cx, litFrom), Offset(cx, thumbY), 3.dp.toPx(), StrokeCap.Round)

        // thumb
        val tl = Offset(cx - thumbW / 2f, thumbY - thumbH / 2f)
        val corner = CornerRadius(15.dp.toPx(), 15.dp.toPx())
        drawRoundRect(Color(0xFF1E1E1E), tl, Size(thumbW, thumbH), corner)
        drawRoundRect(Color(0xFF454545), tl, Size(thumbW, thumbH), corner, style = Stroke(2.dp.toPx()))
        drawRoundRect(
            Color(0xFFD0D0D0),
            Offset(cx - 8.dp.toPx(), thumbY - 1.5.dp.toPx()),
            Size(16.dp.toPx(), 3.dp.toPx()),
            CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
        )
    }
}

/** Smooth curve showing what the equalizer is doing to the sound. */
@Composable
private fun ResponseGraph(effective: FloatArray) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF1A1A1A))
    ) {
        val w = size.width
        val h = size.height
        val mid = h / 2f
        val span = mid - 16.dp.toPx()
        val logSpan = ln(1000f)

        drawLine(Color(0xFF333333), Offset(0f, mid), Offset(w, mid), 1.dp.toPx())

        for (f in EqStore.FREQS) {
            val x = w * ln(f / 20f) / logSpan
            drawCircle(Color(0x44FFFFFF), 3.dp.toPx(), Offset(x, mid))
        }

        val path = Path()
        var px = 0f
        var first = true
        while (px <= w) {
            val f = 20f * 1000f.pow(px / w)
            val db = EqStore.curveDb(effective, f).coerceIn(-15f, 15f)
            val y = mid - db / 15f * span
            if (first) {
                path.moveTo(px, y)
                first = false
            } else {
                path.lineTo(px, y)
            }
            px += 4f
        }
        drawPath(
            path,
            Brush.horizontalGradient(listOf(Color(0xFFEAF21A), Color(0xFF2EE82E))),
            style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/** Rotary knob: drag up/right to turn it up, double-tap to reset. */
@Composable
private fun Knob(
    value: Int,
    color: Color,
    onChange: (Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val latest by rememberUpdatedState(onChange)
    val latestReset by rememberUpdatedState(onReset)
    val holder = remember { floatArrayOf(value.toFloat()) }
    LaunchedEffect(value) {
        if (abs(holder[0] - value) >= 1f) holder[0] = value.toFloat()
    }

    Canvas(
        modifier
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { latestReset() })
            }
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    holder[0] = (holder[0] + (drag.x - drag.y) * 0.4f).coerceIn(0f, 100f)
                    latest(holder[0].roundToInt())
                }
            }
    ) {
        val stroke = 9.dp.toPx()
        val frac = value / 100f
        val arcTopLeft = Offset(stroke / 2f, stroke / 2f)
        val arcSize = Size(size.width - stroke, size.height - stroke)

        drawArc(
            Color(0xFF2A2A2A), 135f, 270f, false, arcTopLeft, arcSize,
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        if (value > 0) {
            drawArc(
                color.copy(alpha = 0.25f), 135f, 270f * frac, false,
                Offset(stroke / 2f - 2.dp.toPx(), stroke / 2f - 2.dp.toPx()),
                Size(arcSize.width + 4.dp.toPx(), arcSize.height + 4.dp.toPx()),
                style = Stroke(stroke + 4.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                color, 135f, 270f * frac, false, arcTopLeft, arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }

        val center = Offset(size.width / 2f, size.height / 2f)
        val inner = size.minDimension / 2f - stroke - 6.dp.toPx()
        drawCircle(Color(0xFF1F1F1F), inner, center)
        drawCircle(Color(0xFF3A3A3A), inner, center, style = Stroke(2.dp.toPx()))

        val angle = (135f + 270f * frac) * Math.PI.toFloat() / 180f
        val dx = cos(angle)
        val dy = sin(angle)
        drawLine(
            Color(0xFFD0D0D0),
            Offset(center.x + dx * inner * 0.40f, center.y + dy * inner * 0.40f),
            Offset(center.x + dx * inner * 0.78f, center.y + dy * inner * 0.78f),
            4.dp.toPx(), StrokeCap.Round,
        )
    }
}
