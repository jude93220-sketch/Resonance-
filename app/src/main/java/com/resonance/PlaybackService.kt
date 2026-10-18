package com.resonance

import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Runs the music player in the background: keeps playing when the app is closed,
 * shows the notification / lock-screen controls, handles headset buttons,
 * applies the equalizer and runs the sleep timer.
 */
class PlaybackService : MediaSessionService(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudness: LoudnessEnhancer? = null

    private val handler = Handler(Looper.getMainLooper())
    private val sleepRunnable = Runnable {
        player?.pause()
        AudioPrefs.cancelSleep(this)
    }

    override fun onCreate() {
        super.onCreate()

        val exo = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true // handle audio focus (pause for calls, duck for navigation)
            )
            .setHandleAudioBecomingNoisy(true) // pause when headphones are unplugged
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        player = exo

        exo.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                setupEffects(audioSessionId)
            }
        })

        mediaSession = MediaSession.Builder(this, exo).build()

        setupEffects(exo.audioSessionId)
        AudioPrefs.prefs(this).registerOnSharedPreferenceChangeListener(this)
        scheduleSleep()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        AudioPrefs.prefs(this).unregisterOnSharedPreferenceChangeListener(this)
        releaseEffects()
        mediaSession?.let {
            it.player.release()
            it.release()
        }
        mediaSession = null
        player = null
        super.onDestroy()
    }

    // ---------------------------------------------------------------- settings

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            AudioPrefs.K_ENABLED,
            AudioPrefs.K_LEVELS,
            AudioPrefs.K_BASS,
            AudioPrefs.K_VIRT,
            AudioPrefs.K_LOUD -> applyEffects()

            AudioPrefs.K_SLEEP_END -> scheduleSleep()
        }
    }

    private fun scheduleSleep() {
        handler.removeCallbacks(sleepRunnable)
        val end = AudioPrefs.sleepEnd(this)
        val delay = end - System.currentTimeMillis()
        if (end > 0L && delay > 0L) {
            handler.postDelayed(sleepRunnable, delay)
        }
    }

    // ----------------------------------------------------------------- effects

    private fun setupEffects(sessionId: Int) {
        releaseEffects()
        if (sessionId <= 0) return

        try {
            val eq = Equalizer(0, sessionId)
            equalizer = eq
            val bands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val freqs = (0 until bands).map { eq.getCenterFreq(it.toShort()) }
            AudioPrefs.publishBands(this, bands, range[0].toInt(), range[1].toInt(), freqs)
        } catch (t: Throwable) {
            equalizer = null
        }
        try {
            bassBoost = BassBoost(0, sessionId)
        } catch (t: Throwable) {
            bassBoost = null
        }
        try {
            virtualizer = Virtualizer(0, sessionId)
        } catch (t: Throwable) {
            virtualizer = null
        }
        try {
            loudness = LoudnessEnhancer(sessionId)
        } catch (t: Throwable) {
            loudness = null
        }
        applyEffects()
    }

    private fun applyEffects() {
        val p = AudioPrefs.prefs(this)
        val on = p.getBoolean(AudioPrefs.K_ENABLED, false)

        try {
            equalizer?.let { e ->
                val bands = e.numberOfBands.toInt()
                val lo = e.bandLevelRange[0].toInt()
                val hi = e.bandLevelRange[1].toInt()
                val saved = AudioPrefs.levels(this)
                for (i in 0 until bands) {
                    val level = (saved.getOrNull(i) ?: 0).coerceIn(lo, hi)
                    e.setBandLevel(i.toShort(), level.toShort())
                }
                e.enabled = on
            }
        } catch (t: Throwable) {
        }

        try {
            bassBoost?.let {
                if (it.strengthSupported) it.setStrength(p.getInt(AudioPrefs.K_BASS, 0).toShort())
                it.enabled = on
            }
        } catch (t: Throwable) {
        }

        try {
            virtualizer?.let {
                if (it.strengthSupported) it.setStrength(p.getInt(AudioPrefs.K_VIRT, 0).toShort())
                it.enabled = on
            }
        } catch (t: Throwable) {
        }

        try {
            loudness?.let {
                it.setTargetGain(p.getInt(AudioPrefs.K_LOUD, 0))
                it.enabled = on
            }
        } catch (t: Throwable) {
        }
    }

    private fun releaseEffects() {
        try { equalizer?.release() } catch (t: Throwable) { }
        try { bassBoost?.release() } catch (t: Throwable) { }
        try { virtualizer?.release() } catch (t: Throwable) { }
        try { loudness?.release() } catch (t: Throwable) { }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudness = null
    }
}
