package com.resonance

import android.content.SharedPreferences
import android.app.PendingIntent
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.media.audiofx.PresetReverb
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.pow

/**
 * Runs the music player in the background: keeps playing when the app is closed,
 * shows the notification / lock-screen controls, handles headset buttons,
 * applies the 10-band equalizer and runs the sleep timer.
 *
 * Equalizer engine: Android's DynamicsProcessing (Android 9+) gives a real 10-band EQ,
 * preamp and limiter. Older phones fall back to the basic system equalizer.
 */
class PlaybackService : MediaSessionService(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    private var dsp: AudioEffect? = null
    private var equalizer: Equalizer? = null
    private var loudness: LoudnessEnhancer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var reverb: PresetReverb? = null
    private var lastMediaId: String? = null
    private var fadeInStartedAt = 0L
    private var replayGainMultiplier = 1f
    private var audioManager: AudioManager? = null
    private val outputCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = applyOutputProfile()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = applyOutputProfile()
    }

    private val handler = Handler(Looper.getMainLooper())
    private val sleepRunnable = Runnable {
        player?.pause()
        AudioPrefs.cancelSleep(this)
    }
    private val stateRunnable = object : Runnable {
        override fun run() {
            savePlaybackState()
            handler.postDelayed(this, 2_000L)
        }
    }
    private val transitionRunnable = object : Runnable {
        override fun run() {
            updateTransitionFade()
            handler.postDelayed(this, 50L)
        }
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
            .setHandleAudioBecomingNoisy(AppSettings.pauseOnDisconnect(this))
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        // Do not insert an artificial pause between consecutive queue items.
        exo.setPauseAtEndOfMediaItems(false)
        player = exo

        exo.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                setupEffects(audioSessionId)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // counts plays for the "Most Played" list
                val id = mediaItem?.mediaId?.toLongOrNull()
                if (id != null) PlayStats.increment(this@PlaybackService, id)
                val newId = mediaItem?.mediaId
                if (newId != null && lastMediaId != null && newId != lastMediaId) {
                    val fade = AppSettings.crossfadeMs(this@PlaybackService)
                    if (fade > 0) {
                        exo.volume = 0f
                        fadeInStartedAt = System.currentTimeMillis()
                    }
                }
                lastMediaId = newId
                updateReplayGain(mediaItem)
                savePlaybackState()
                notifyWidget()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                savePlaybackState()
                notifyWidget()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                savePlaybackState()
            }

            override fun onTimelineChanged(
                timeline: androidx.media3.common.Timeline,
                reason: Int,
            ) {
                savePlaybackState()
                notifyWidget()
            }
        })

        val sessionActivity = PendingIntent.getActivity(
            this,
            700,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        mediaSession = MediaSession.Builder(this, exo)
            .setSessionActivity(sessionActivity)
            .build()
        restorePlaybackState(exo)
        updateReplayGain(exo.currentMediaItem)
        applyPlaybackParameters()

        setupEffects(exo.audioSessionId)
        audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager?.registerAudioDeviceCallback(outputCallback, handler)
        }
        applyOutputProfile()
        AudioPrefs.prefs(this).registerOnSharedPreferenceChangeListener(this)
        EqStore.prefs(this).registerOnSharedPreferenceChangeListener(this)
        AppSettings.prefs(this).registerOnSharedPreferenceChangeListener(this)
        scheduleSleep()
        handler.post(stateRunnable)
        handler.post(transitionRunnable)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        player?.let { savePlaybackState(it) }
        handler.removeCallbacks(stateRunnable)
        handler.removeCallbacks(transitionRunnable)
        handler.removeCallbacksAndMessages(null)
        AudioPrefs.prefs(this).unregisterOnSharedPreferenceChangeListener(this)
        EqStore.prefs(this).unregisterOnSharedPreferenceChangeListener(this)
        AppSettings.prefs(this).unregisterOnSharedPreferenceChangeListener(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) audioManager?.unregisterAudioDeviceCallback(outputCallback)
        releaseEffects()
        mediaSession?.let {
            it.player.release()
            it.release()
        }
        mediaSession = null
        player = null
        super.onDestroy()
    }

    private fun restorePlaybackState(exo: ExoPlayer) {
        val state = PlaybackStateStore.load(this) ?: return
        exo.setMediaItems(state.items, state.index, state.positionMs)
        exo.shuffleModeEnabled = state.shuffle
        exo.repeatMode = state.repeatMode
        exo.prepare()
        if (state.playing) exo.play() else exo.pause()
    }

    private fun savePlaybackState() {
        player?.let { savePlaybackState(it) }
    }

    private fun savePlaybackState(exo: ExoPlayer) {
        val items = (0 until exo.mediaItemCount).map(exo::getMediaItemAt)
        if (items.isEmpty()) return
        PlaybackStateStore.save(
            context = this,
            playerItems = items,
            index = exo.currentMediaItemIndex,
            positionMs = exo.currentPosition,
            playing = exo.isPlaying,
            shuffle = exo.shuffleModeEnabled,
            repeatMode = exo.repeatMode,
        )
    }

    private fun notifyWidget() {
        sendBroadcast(Intent(ResonanceWidget.ACTION_REFRESH).setPackage(packageName))
    }

    private fun outputKey(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return "speaker"
        val devices = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS).orEmpty()
        return when {
            devices.any { it.type == AudioDeviceInfo.TYPE_USB_DEVICE || it.type == AudioDeviceInfo.TYPE_USB_HEADSET } -> "usb"
            devices.any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO } -> "bluetooth"
            devices.any { it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET } -> "wired"
            else -> "speaker"
        }
    }

    private fun applyOutputProfile() {
        val profile = OutputProfileStore.profile(this, outputKey())
        val preset = EqStore.BUILTIN.firstOrNull { it.name == profile.eqPreset }
        if (preset != null) {
            EqStore.saveGains(this, preset.gains)
            EqStore.savePreamp(this, preset.preamp + profile.preampDb)
        } else if (profile.preampDb != 0f) {
            EqStore.savePreamp(this, profile.preampDb)
        }
        applyEffects()
    }

    private fun updateReplayGain(item: MediaItem?) {
        val mode = AppSettings.replayGainMode(this)
        if (mode == 0 || item == null) {
            replayGainMultiplier = 1f
            return
        }
        val extras = item.mediaMetadata.extras
        val key = if (mode == 1) "resonance_replaygain_track" else "resonance_replaygain_album"
        val db = extras?.getFloat(key, Float.NaN) ?: Float.NaN
        replayGainMultiplier = if (db.isFinite()) {
            10f.pow(db / 20f).coerceIn(0.25f, 1.5f)
        } else {
            1f
        }
    }

    /**
     * Media3 1.4 has no built-in two-deck crossfader. This preserves queue semantics and provides
     * a safe transition ramp: the outgoing item fades down before the queue advances, and the
     * incoming item fades up. A future dual-deck engine can replace this without changing the
     * settings or MediaSession contract.
     */
    private fun updateTransitionFade() {
        val exo = player ?: return
        val fade = AppSettings.crossfadeMs(this)
        if (fade <= 0) {
            if (exo.volume != replayGainMultiplier) exo.volume = replayGainMultiplier
            fadeInStartedAt = 0L
            return
        }
        if (fadeInStartedAt > 0L) {
            val fraction =
                ((System.currentTimeMillis() - fadeInStartedAt).toFloat() / fade).coerceIn(0f, 1f)
            exo.volume = replayGainMultiplier * fraction
            if (fraction >= 1f) fadeInStartedAt = 0L
            return
        }
        if (!exo.isPlaying || exo.duration <= 0L) return
        val hasFollowingItem = exo.hasNextMediaItem() || exo.repeatMode == Player.REPEAT_MODE_ALL
        val remaining = exo.duration - exo.currentPosition
        if (hasFollowingItem && remaining in 1..fade.toLong()) {
            exo.volume = replayGainMultiplier * (remaining.toFloat() / fade).coerceIn(0f, 1f)
        } else if (remaining > fade) {
            exo.volume = replayGainMultiplier
        }
    }

    // ---------------------------------------------------------------- settings

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == null) return
        if (key in EqStore.KEYS) {
            if (key == EqStore.K_BAND_COUNT) setupEffects(player?.audioSessionId ?: 0) else applyEffects()
        } else if (key == AudioPrefs.K_SLEEP_END) {
            scheduleSleep()
        } else if (key == AppSettings.K_PAUSE_NOISY) {
            player?.setHandleAudioBecomingNoisy(AppSettings.pauseOnDisconnect(this))
        } else if (key == AppSettings.K_TEMPO || key == AppSettings.K_REVERB) {
            applyPlaybackParameters()
            applyEnhancers(EqStore.master(this))
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
        if (sessionId <= 0) {
            EqStore.setEngine(this, "")
            return
        }

        try { bassBoost = BassBoost(0, sessionId) } catch (t: Throwable) { bassBoost = null }
        try { virtualizer = Virtualizer(0, sessionId) } catch (t: Throwable) { virtualizer = null }
        try { reverb = PresetReverb(0, sessionId) } catch (t: Throwable) { reverb = null }

        var engine = ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                dsp = createDsp(sessionId)
                engine = "DP"
            } catch (t: Throwable) {
                dsp = null
            }
        }

        if (dsp == null) {
            try {
                val eq = Equalizer(0, sessionId)
                equalizer = eq
                engine = "SYS ${eq.numberOfBands}"
            } catch (t: Throwable) {
                equalizer = null
            }
            try {
                loudness = LoudnessEnhancer(sessionId)
            } catch (t: Throwable) {
                loudness = null
            }
        }

        EqStore.setEngine(this, engine)
        applyEffects()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun createDsp(sessionId: Int): AudioEffect {
        val config = DynamicsProcessing.Config.Builder(
            DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
            2,
            true, EqStore.frequencies(this).size,
            false, 0,
            false, 0,
            true,
        ).build()
        return DynamicsProcessing(0, sessionId, config)
    }

    private fun applyEffects() {
        val snap = EqStore.snapshot(this)

        val d = dsp
        if (d != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                applyDsp(d as DynamicsProcessing, snap)
            } catch (t: Throwable) {
            }
            return
        }

        try {
            equalizer?.let { e ->
                val bands = e.numberOfBands.toInt()
                val lo = e.bandLevelRange[0].toInt()
                val hi = e.bandLevelRange[1].toInt()
                for (i in 0 until bands) {
                    val hz = e.getCenterFreq(i.toShort()) / 1000f
                    val db = EqStore.interpolate(snap.gains, hz)
                    val level = (db * 100f).roundToInt().coerceIn(lo, hi)
                    e.setBandLevel(i.toShort(), level.toShort())
                }
                e.setEnabled(snap.master && (snap.eqOn || snap.toneOn))
            }
        } catch (t: Throwable) {
        }

        try {
            loudness?.let {
                val boost = if (snap.eqOn) max(0f, snap.preamp) else 0f
                it.setTargetGain((boost * 100f).roundToInt())
                it.setEnabled(snap.master)
            }
        } catch (t: Throwable) {
        }

        applyEnhancers(snap.master)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun applyDsp(dp: DynamicsProcessing, snap: EqStore.Snapshot) {
        val n = snap.frequencies.size
        val edges = FloatArray(n) { i ->
            if (i == n - 1) 20000f else kotlin.math.sqrt(snap.frequencies[i].toFloat() * snap.frequencies[i + 1].toFloat())
        }

        val eq = DynamicsProcessing.Eq(true, true, n)
        for (i in 0 until n) {
            eq.setBand(i, DynamicsProcessing.EqBand(true, edges[i], snap.gains[i]))
        }
        dp.setPreEqAllChannelsTo(eq)
        dp.setInputGainAllChannelsTo(if (snap.eqOn) snap.preamp else 0f)

        val limiter = DynamicsProcessing.Limiter(
            true, snap.limitOn, 0, 1f, 60f, 10f, -2f, 0f
        )
        dp.setLimiterAllChannelsTo(limiter)
        dp.setEnabled(snap.master)
        applyEnhancers(snap.master)
    }

    private fun applyEnhancers(master: Boolean) {
        val bass = EqStore.bassBoost(this)
        val width = EqStore.stereoWidth(this)
        try {
            bassBoost?.setStrength((bass * 10).coerceIn(0, 1000).toShort())
            bassBoost?.setEnabled(master && bass > 0)
        } catch (t: Throwable) {
        }
        try {
            virtualizer?.setStrength((width * 10).coerceIn(0, 1000).toShort())
            virtualizer?.setEnabled(master && width > 0)
        } catch (t: Throwable) {
        }
        try {
            val preset = when (AppSettings.reverb(this)) {
                1 -> PresetReverb.PRESET_SMALLROOM
                2 -> PresetReverb.PRESET_MEDIUMROOM
                else -> PresetReverb.PRESET_LARGEHALL
            }
            reverb?.setPreset(preset)
            reverb?.setEnabled(master && AppSettings.reverb(this) > 0)
        } catch (t: Throwable) {
        }
    }

    private fun applyPlaybackParameters() {
        val speed = AppSettings.tempo(this) / 100f
        try { player?.setPlaybackSpeed(speed) } catch (_: Throwable) { }
    }

    private fun releaseEffects() {
        try { dsp?.release() } catch (t: Throwable) { }
        try { equalizer?.release() } catch (t: Throwable) { }
        try { loudness?.release() } catch (t: Throwable) { }
        try { bassBoost?.release() } catch (t: Throwable) { }
        try { virtualizer?.release() } catch (t: Throwable) { }
        try { reverb?.release() } catch (t: Throwable) { }
        dsp = null
        equalizer = null
        loudness = null
        bassBoost = null
        virtualizer = null
        reverb = null
    }
}
