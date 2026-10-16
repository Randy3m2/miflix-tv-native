BruniO TV RC61 — Headphone AudioTrack recovery

Apply over RC60 native sources. Upload files to the same repository paths, then run your normal GitHub Actions APK build/release. Install RC61 on the TV. This is source, not a prebuilt APK. No SQL, Supabase function or Vercel changes are required.

Fix scope:
- Main TV movie/episode player: headphone-aware audio sink rejects compressed passthrough and offload when a headphone device is present, requiring the TV decoder to produce PCM.
- PCM downmix includes centre/dialogue and surround channels, with conservative peak headroom. Mono/stereo are preserved. Without headphones, channel mixing is identity and normal format support is retained. Float output and platform playback-speed processing are disabled in this sink for compatibility.
- Device connect/disconnect schedules one 500ms debounced stop/seek/prepare of the existing media, preserving position and play/pause intent. It does not regenerate links.
- AudioTrack init/write errors with headphones get one automatic recovery attempt per route state; unrelated/persistent failures keep the error UI. Callback/retry cleanup occurs on disposal.
- Version 22059 / 2.0.0-rc61; bundled bilingual changelog updated.

Limits:
- Android output-device enumeration indicates connected outputs; some TV firmware can list an attached but inactive device. Bluetooth latency is still controlled by the TV/headphones. This does not promise zero delay or add missing audio decoders.
- Multiview has its own player factory and is not changed by this patch.
- Brief interruption while switching audio output is expected.

Validation:
Source integration, mirror consistency, bounded recovery/cleanup and version checks passed. JUnit tests were added for identity output, mono/stereo preservation, centre/dialogue retention, quad routing and peak headroom. They have NOT been executed here: this environment lacks the JDK compiler, Gradle Android toolchain and physical TV/Bluetooth hardware.
Run in CI: ./gradlew :app:testDebugUnitTest --tests com.miflix.native2.player.HeadphoneMixTest :app:assembleRelease
Use your existing signing secrets for release builds.

Device checks:
1. Play 5.1/7.1 content; connect headphones mid-playback. Confirm playback resumes near the same timestamp and dialogue is audible in both ears.
2. Repeat while paused; playback must remain paused.
3. Disconnect headphones; verify normal TV audio, position and no retry loop.
4. Open content with headphones already connected. Test a stereo source as well.
5. If an error persists, provide the complete Media3 cause/AudioTrack format from Android logcat (not only the top-level error name), plus TV model and connection type.

Reference APIs reviewed:
https://developer.android.com/reference/androidx/media3/exoplayer/DefaultRenderersFactory
https://developer.android.com/reference/androidx/media3/exoplayer/audio/ForwardingAudioSink
https://developer.android.com/reference/androidx/media3/common/audio/ChannelMixingMatrix
