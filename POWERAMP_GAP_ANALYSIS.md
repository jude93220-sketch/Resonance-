# Resonance vs. Poweramp Feature Gap Analysis

**Audit basis:** Resonance at merged commit `d81a2b2`, compared with Poweramp's current official product page and Google Play listing accessed on 2026-10-18.

## What Resonance already has

Resonance now has a solid local-player foundation: MediaStore library scanning; folder, album, artist, genre, composer, year, ratings, playlists, queue, most-played tracking, recently added, sleep timer, background playback through Media3, notification and lock-screen session controls, a home-screen widget, themes, animated spectrum/wave/pulse visualizers, album-art swipe navigation, persistent queue restoration, shuffle/repeat, gapless queue progression, transition fading, a 10-band graphic EQ, preamp, bass and treble, limiter, device-backed bass boost, and device-backed stereo widening.

That is a capable early music player, but Poweramp's premium experience is primarily differentiated by its audio engine, metadata/library depth, output routing, and customization breadth.

## Highest-priority missing features

### 1. Replace the current DSP approximation with a real audio engine

Resonance currently depends on Android `DynamicsProcessing`, `Equalizer`, `LoudnessEnhancer`, `BassBoost`, and `Virtualizer`. These are device-dependent effects and do not provide Poweramp-level output control.

Missing:

- True 32- or 64-band graphic EQ and a parametric EQ mode with configurable frequency, Q, gain, and filter type.
- Per-output profiles for speaker, wired headphones, Bluetooth devices, USB DACs, and individual headphones.
- AutoEQ headphone presets and preset import/export.
- Balance, mono mix, stereo expansion controls with reliable channel-level DSP.
- Reverb and tempo controls.
- Resampler, dither, sample-rate, bit-depth, and output-format controls.
- DVC-style direct volume behavior where the device permits it.
- Audio diagnostics showing the actual output path, sample rate, bit depth, codec, DSP chain, and clipping/limiter state.

Poweramp advertises graphic and parametric EQ, per-output presets, AutoEQ, DVC, configurable resampling/dither, and detailed Hi-Res/USB output control.[1][2]

### 2. Implement true overlap crossfade

Resonance currently provides a transition fade: the outgoing track fades down and the incoming track fades up after the queue transition. This is not a true overlapping crossfade. True Poweramp-style crossfade requires two synchronized audio decks or a custom audio pipeline that can mix the tail of one track with the beginning of the next while preserving MediaSession behavior.

ReplayGain is also missing. Resonance should parse track and album ReplayGain metadata, offer preamp/limiter policies, and apply gain consistently across queue transitions.

### 3. Expand format and source support

The scanner currently relies on Android MediaStore and Media3's device-supported decoders. Missing or incomplete Poweramp-style source support includes:

- HTTP/radio streams and `.m3u` radio sources.
- Explicit support and validation for ALAC, APE, DSD DSF/DFF, WV, OPUS, WMA, TTA, MKA, TAK, MOD/IT/S3X/XM, and WebM where licensing and decoders permit.
- Embedded and standalone CUE sheets.
- M3U, M3U8, PLS, and WPL playlist import/export.
- Gapless smoothing for formats/devices that need decoder padding correction.

Poweramp lists these formats, radio streams, CUE support, and playlist formats in its current listing.[1][2]

## Library and metadata gaps

The current library is primarily read-only. The next library phase should add user-selected scan folders, exclusions, rescan scheduling, album and artist image management, embedded-art preference rules, tag editing, synchronized and plain lyrics including LRC handling, lyrics search integration, smart playlists, and richer sorting/grouping rules.

Poweramp also offers downloads for missing album art and artist images, a tag editor, lyrics support, smart playlist behavior, and configurable library views.[1][2]

## Device and ecosystem gaps

Resonance still lacks Android Auto browsing and playback templates, Chromecast/Cast support, USB Exclusive output, AAudio/Hi-Res/OpenSL output selection, DSD native/DoP modes, Bluetooth codec/output profiles, and hardware-volume handling for external DACs. These features are device-dependent, so each needs capability detection and an explicit fallback rather than pretending every phone supports Hi-Res output.

The current Settings screen also still labels “Lock Screen” as “Coming soon,” even though the MediaSession and widget work now exist. That screen should be updated to expose actual lock-screen options such as notification visibility, artwork, seek controls, and lock-screen activity behavior.

## UI and customization gaps

The new themes and three animated visualizer modes are a good start, but Poweramp-style customization still needs configurable layouts, player button arrangements, seekbar modes, static/pro controls, gesture mapping, long-press actions, volume-key actions, skin import/selection, Milkdrop-compatible visualizations, custom visualizer presets, and a more configurable widget system with resize-aware layouts.

## Reliability and product-quality gaps

The project has no visible unit or instrumentation test suite. Before deep audio work, add tests for playlist persistence, settings migration, queue restoration, rating state, scanner filtering, and ReplayGain parsing. Device test coverage should include Android 8 through current Android versions, wired/Bluetooth headsets, audio focus interruptions, process death, notification permission denial, USB DAC attach/detach, large libraries, and corrupted/missing media files.

## Recommended implementation order

1. Build a dedicated audio abstraction layer and add ReplayGain parsing, detailed output diagnostics, and a real parametric EQ model.
2. Add user-selected library folders, tag editing, lyrics/LRC, CUE, and playlist import/export.
3. Replace transition fading with a true dual-deck crossfade engine.
4. Add per-output profiles, AutoEQ import, resampler/dither options, and USB/Hi-Res output detection.
5. Add Android Auto and Chromecast.
6. Expand skins, layouts, gesture mapping, widgets, and Milkdrop-style visualization support.
7. Add automated and device-level regression testing.

## References

[1]: https://powerampapp.com/ "Poweramp official product page"

[2]: https://play.google.com/store/apps/details?id=com.maxmpz.audioplayer&hl=en_US "Poweramp Music Player on Google Play"
