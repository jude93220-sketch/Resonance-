package com.resonance

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val PaBlue = Color(0xFF3B6FE0)
private val SubGrey = Color(0xFF9A9A9A)

/**
 * Settings in the Poweramp layout: a list of categories, each opening its own page.
 * Categories that are not built yet are shown dimmed so nothing pretends to work.
 */
@Composable
fun SettingsScreen(
    songCount: Int,
    onBack: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onRescan: () -> Unit,
    onChanged: () -> Unit,
) {
    var page by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = page != null) { page = null }

    val title = page ?: "Settings"

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { if (page != null) page = null else onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            when (page) {
                null -> {
                    LinkRow("Audio", "Equalizer, audio engine info") { page = "Audio" }
                    LinkRow("Library", "Scanner and library options") { page = "Library" }
                    LinkRow("Headset/Bluetooth", "Connection behaviour") { page = "Headset/Bluetooth" }
                    LinkRow("Lock Screen", "Coming soon", enabled = false) { }
                    LinkRow("Look and Feel", "Screen options") { page = "Look and Feel" }
                    LinkRow("Background", "Colour and gradient") { page = "Background" }
                    LinkRow("Album Art", "Coming soon", enabled = false) { }
                    LinkRow("Visualization", "Coming soon", enabled = false) { }
                    LinkRow("About", "Version info") { page = "About" }
                }

                "Audio" -> AudioPage(onOpenEqualizer)
                "Library" -> LibraryPage(songCount, onRescan)
                "Headset/Bluetooth" -> HeadsetPage(onChanged)
                "Look and Feel" -> LookPage(onChanged)
                "Background" -> BackgroundPage(onChanged)
                "About" -> AboutPage()
            }
        }
    }
}

// --------------------------------------------------------------------- pages

@Composable
private fun AudioPage(onOpenEqualizer: () -> Unit) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    val engine = remember(tick) { EqStore.engine(context) }
    val info = when {
        engine == "DP" -> "10-band engine (DynamicsProcessing) with preamp and limiter"
        engine.startsWith("SYS") -> "Basic system equalizer (${engine.removePrefix("SYS ")} bands). This phone cannot run the 10-band engine."
        else -> "Not started yet. Play any song, then come back."
    }

    LinkRow("Equalizer", "10-band equalizer, preamp, bass/treble, limiter") { onOpenEqualizer() }
    LinkRow("Audio Info", info) { tick++ }
    LinkRow("Reset equalizer", "Flat bands, preamp 0, bass 0%, treble 50%") {
        EqStore.resetAll(context)
        tick++
    }
}

@Composable
private fun LibraryPage(songCount: Int, onRescan: () -> Unit) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    var seconds by remember(tick) { mutableIntStateOf(AppSettings.minTrackSeconds(context)) }

    LinkRow("Rescan", "Songs: $songCount") { onRescan() }
    SliderRow(
        title = "Ignore Short Tracks (Notifications, etc.)",
        valueLabel = "Less than (seconds): $seconds",
        value = seconds.toFloat(), range = 0f..60f, steps = 59,
        footer = "Tap Rescan after changing this.",
    ) {
        seconds = it.roundToInt()
        AppSettings.putInt(context, AppSettings.K_MIN_SECONDS, seconds)
    }
    LinkRow("Restore Defaults", null) {
        AppSettings.restore(context, AppSettings.K_MIN_SECONDS)
        tick++
    }
}

@Composable
private fun HeadsetPage(onChanged: () -> Unit) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    var pause by remember(tick) { mutableStateOf(AppSettings.pauseOnDisconnect(context)) }

    SectionLabel("Connection")
    SwitchRow("Pause On Headset Disconnect", "Pause when wired/Bluetooth headset/USB DAC disconnected", pause) {
        pause = it
        AppSettings.putBool(context, AppSettings.K_PAUSE_NOISY, it)
        onChanged()
    }
    LinkRow("Restore Defaults", null) {
        AppSettings.restore(context, AppSettings.K_PAUSE_NOISY)
        tick++
        onChanged()
    }
}

@Composable
private fun LookPage(onChanged: () -> Unit) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    var keepOn by remember(tick) { mutableStateOf(AppSettings.keepScreenOn(context)) }

    SwitchRow("Keep Screen On", "Always keep screen on in the app", keepOn) {
        keepOn = it
        AppSettings.putBool(context, AppSettings.K_KEEP_ON, it)
        onChanged()
    }
    LinkRow("Restore Defaults", null) {
        AppSettings.restore(context, AppSettings.K_KEEP_ON)
        tick++
        onChanged()
    }
}

@Composable
private fun BackgroundPage(onChanged: () -> Unit) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    var gradient by remember(tick) { mutableIntStateOf(AppSettings.bgGradient(context)) }
    var intensity by remember(tick) { mutableIntStateOf(AppSettings.bgIntensity(context)) }

    SliderRow(
        title = "Background Gradient",
        valueLabel = if (gradient == 0) "None" else "$gradient",
        value = gradient.toFloat(), range = 0f..9f, steps = 8,
    ) {
        gradient = it.roundToInt()
        AppSettings.putInt(context, AppSettings.K_BG_GRADIENT, gradient)
        onChanged()
    }
    SliderRow(
        title = "Background Intensity",
        valueLabel = "$intensity%",
        value = intensity.toFloat(), range = 0f..100f, steps = 0,
        footer = "How strongly the album colour shows in the background",
    ) {
        intensity = it.roundToInt()
        AppSettings.putInt(context, AppSettings.K_BG_INTENSITY, intensity)
        onChanged()
    }
    LinkRow("Restore Defaults", null) {
        AppSettings.restore(context, AppSettings.K_BG_GRADIENT, AppSettings.K_BG_INTENSITY)
        tick++
        onChanged()
    }
}

@Suppress("DEPRECATION")
@Composable
private fun AboutPage() {
    val context = LocalContext.current
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (t: Throwable) {
            ""
        }
    }
    LinkRow("Resonance", if (version.isEmpty()) "Music player" else "Version $version") { }
}

// ---------------------------------------------------------------------- rows

@Composable
private fun SectionLabel(text: String) {
    Text(
        text, color = SubGrey, fontSize = 14.sp,
        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun LinkRow(title: String, subtitle: String?, enabled: Boolean = true, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            title, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            color = if (enabled) Color.White else Color(0xFF6A6A6A),
        )
        if (subtitle != null) {
            Text(
                subtitle, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                color = if (enabled) SubGrey else Color(0xFF555555),
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(
                    subtitle, color = SubGrey, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PaBlue,
                checkedTrackColor = Color(0xFF263A66),
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = Color(0xFFBDBDBD),
                uncheckedTrackColor = Color(0xFF3A3A3A),
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun SliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    footer: String? = null,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            valueLabel, color = SubGrey, fontSize = 14.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 3.dp),
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = PaBlue,
                activeTrackColor = PaBlue,
                inactiveTrackColor = Color(0xFF555555),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
        if (footer != null) {
            Text(footer, color = SubGrey, fontSize = 13.sp)
        }
    }
}
