# SodaMusic 🎵

A cross-platform music player inspired by 汽水音乐 (Soda Music), built with **Jetpack Compose Multiplatform** for Android, iOS, and Desktop.

## Features

- 🎶 **Cross-platform** — single Compose UI codebase runs on Android, iOS, and Desktop (JVM)
- 🎚️ **9 VIP audio effects** with real DSP processing (matching vip.jpg):
  - 智能音效 (Smart) — adaptive EQ with light compression
  - 360环绕 (360 Surround) — extreme stereo widening + Haas delay + reverb
  - 超重低音 (Super Bass) — aggressive low-shelf EQ + compression
  - 清澈人声 (Clear Vocals) — vocal-range peaking boost, low/high cut
  - 3D音效 (3D Audio) — stereo delay + spatial widening
  - HIFI现场 (HIFI Live) — hall reverb + loudness enhancement
  - 动感电音 (Dynamic EDM) — bass + treble boost + hard compression
  - 摇滚音效 (Rock) — rock EQ + soft saturation + compression
  - 复古唱片 (Vintage) — band-pass + wow/flutter + vinyl noise + saturation
- 📂 **Multiple sources** — local files, online streams, demo tracks
- 🎛️ Platform-native audio backends:
  - **Android**: `MediaExtractor` + `MediaCodec` + `AudioTrack` (low-latency, hardware decode)
  - **Desktop/JVM**: `javax.sound.sampled` with pure-Kotlin WAV PCM reader
  - **iOS**: `AVAudioEngine` + `AVAudioPlayerNode` + `AVAudioFile` (system decode)
- 🎨 Shared DSP engine in pure Kotlin (`BiquadFilter`, `ReverbProcessor`, `DelayProcessor`, `StereoWidener`, `Compressor`) — effects sound identical across all platforms

## Project Structure

```
SodaMusic/
├── composeApp/
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/        # Shared UI + DSP + logic
│       │   └── kotlin/com/sodamusic/player/
│       │       ├── audio/     # Player controller + DSP effects
│       │       ├── model/     # Track, PlayState
│       │       └── ui/        # Compose screens + components
│       ├── androidMain/       # Android backend (AudioTrack, MediaCodec)
│       ├── desktopMain/       # JVM backend (javax.sound)
│       └── iosMain/           # iOS backend (AVAudioEngine)
└── gradle/
```

## Building

> First-time setup: ensure the Gradle wrapper jar is present.
> If missing, run `gradle wrapper` from a machine with Gradle installed, or open in Android Studio.

### Desktop
```bash
./gradlew :composeApp:run
```

### Android
Open the project in Android Studio and run the `composeApp` Android configuration, or:
```bash
./gradlew :composeApp:assembleDebug
```

### iOS
Open the project in Xcode after generating via KMP (use Kotlin Multiplatform plugin or `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`), or open the `iosApp` in Android Studio's KMP wizard.

## DSP Implementation Details

All effects run through a real-time DSP chain implemented in **pure Kotlin** (common code, identical on every platform):

1. **5-band EQ** — cascaded biquad IIR filters (low-shelf, 3× peaking, high-shelf), coefficients computed via bilinear transform
2. **Feed-forward compressor** — RMS-like envelope tracking with attack/release smoothing
3. **Soft saturation** — tanh-like clipper for rock/vintage overdrive
4. **Mid-side stereo widener** — enhances or narrows the stereo field
5. **Haes stereo delay** — different left/right delay times with feedback for spatial effect
6. **Schroeder reverb** — 4 parallel comb filters + 2 series all-pass filters (classic algorithm)
7. **Vintage processing** — band-pass EQ, wow/flutter (slow pitch modulation), and dither noise

Each preset from vip.jpg configures these blocks with tuned parameters for the desired character.

## Adding Real Audio Files

- **Local**: Place MP3/WAV/M4A files on device; use the "本地文件" tab to enter the path
- **Online**: Paste any HTTP/HTTPS streaming URL (e.g. Icecast, MP3 stream)
- **Desktop**: `.wav` files are decoded natively; other formats fall back to a demo tone (add JLayer/JavaFX for full MP3)
- **Android/iOS**: All system-supported formats are hardware-decoded automatically

## License

For demonstration purposes.
