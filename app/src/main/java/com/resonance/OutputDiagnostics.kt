package com.resonance

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

object OutputDiagnostics {
    fun summary(context: Context): String {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return "Audio manager unavailable"
        val rate = audio.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE) ?: "unknown"
        val buffer = audio.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER) ?: "unknown"
        val routes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).mapNotNull { label(it) }.distinct().joinToString(", ")
        } else "system output"
        val rateNumber = rate.toIntOrNull() ?: 0
        val usb = routes.contains("USB")
        val hiRes = usb && rateNumber >= 96_000
        val lowLatency = context.packageManager.hasSystemFeature("android.hardware.audio.low_latency")
        val capability = if (hiRes) "Hi-Res capable" else if (usb) "USB detected" else "PCM output"
        return "${rate} Hz · ${buffer} frames · $routes · $capability" +
            if (lowLatency) " · low-latency" else ""
    }

    private fun label(device: AudioDeviceInfo): String? = when (device.type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "speaker"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> "wired"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth"
        AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> "USB"
        else -> null
    }
}
