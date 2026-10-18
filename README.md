# Resonance

Resonance is a local-first Android music player built with Kotlin, Jetpack Compose, and AndroidX Media3. It is being developed as a Poweramp-inspired player focused on reliable playback, deep audio controls, and a configurable library experience.

## Current features

- Local music library scanning through MediaStore
- Folder, artist, album, genre, year, rating, and playlist views
- Background playback through a Media3 `MediaSessionService`
- Notification and headset playback controls
- Persistent queue and playback position restoration after the service is recreated
- Shuffle and repeat controls
- Playlists, likes/dislikes, most-played tracking, and sleep timer
- 10-band equalizer with preamp, bass, treble, limiter, and saved presets
- Album-art-based background and screen settings

## Build

Android Studio Hedgehog or newer is recommended. The project targets SDK 35, uses Java/Kotlin 17, and supports Android 8.0 (API 26) and newer.

From the repository root:

```bash
./gradlew assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

For a clean build:

```bash
./gradlew clean assembleDebug
```

## GitHub Actions

Every push to `main` runs the `Build APK` workflow. The generated debug APK is uploaded as the `resonance-debug` workflow artifact.

## Development direction

The next development phases are playback polish and audio fidelity first, followed by a larger library system and UI customization. Planned areas include gapless/crossfade behavior, ReplayGain, richer DSP and output profiles, tag editing, lyrics and CUE support, playlist import/export, visualizations, widgets, Android Auto, and external DAC handling where the device supports it.

Resonance aims for comparable capabilities and behavior, but does not copy Poweramp's proprietary code, branding, skins, or paid assets.
