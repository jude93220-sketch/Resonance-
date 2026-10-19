# Resonance Poweramp-Parity Roadmap

This is the implementation tracker for Resonance. Features that depend on a phone, Android ROM, Bluetooth codec, USB DAC, or Google Play services will be capability-detected and shown as unavailable when the device cannot support them.

## Audio engine

- [ ] Replace Android-only effects with a dedicated DSP abstraction.
- [ ] Graphic EQ: 10/15/24/32/64-band profiles.
- [ ] Parametric EQ: filter type, frequency, Q, gain, and band management.
- [x] Per-output profiles: speaker, wired, Bluetooth, USB DAC, Android Auto, Cast.
- [ ] AutoEQ preset import and headphone profile storage.
- [ ] ReplayGain track/album parsing and normalization.
- [ ] True two-deck overlapping crossfade.
- [ ] Gapless smoothing and decoder padding correction.
- [ ] Balance, mono mix, stereo expansion, bass, treble, limiter, reverb, tempo.
- [ ] Resampler, dither, sample-rate, bit-depth, and output diagnostics.
- [ ] Hi-Res/AAudio/OpenSL output selection and capability reporting.
- [ ] USB Exclusive, bit-perfect PCM, native/DoP DSD, and external-DAC profiles.

## Library and metadata

- [x] User-selected scan folders and exclusions through Storage Access Framework.
- [ ] Smart playlists and richer sorting/filtering rules.
- [x] Tag editor with safe write-back and permission handling.
- [x] Synchronized/plain lyrics and LRC timing.
- [x] Lyrics search integration.
- [ ] Embedded and standalone CUE sheets.
- [ ] M3U/M3U8/PLS/WPL import and export.
- [ ] Radio and HTTP stream sources.
- [ ] Format capability matrix and explicit decoder diagnostics.
- [ ] Missing album-art and artist-image download flows.

## UI and controls

- [ ] Complete lock-screen settings page.
- [ ] Configurable gesture mapping and long-press actions.
- [ ] Volume-key previous/next actions.
- [ ] Configurable player layouts and seekbar modes.
- [ ] Advanced widget sizes/layouts.
- [ ] Milkdrop-compatible visualizers and preset import.
- [ ] Android Auto browsing/playback.
- [ ] Chromecast/Cast playback.
- [ ] Bluetooth codec/output profile controls.

## Quality gates

- [ ] Unit tests for metadata parsing, ReplayGain, CUE, playlists, settings migration.
- [ ] Queue and process-death instrumentation tests.
- [ ] Large-library performance tests.
- [ ] Device matrix: Android 8 through current, wired/Bluetooth, USB DAC, Android Auto, Cast.
- [ ] Every unsupported hardware feature must have a visible reason and fallback.
