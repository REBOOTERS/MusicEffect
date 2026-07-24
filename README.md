<div align="center">

# 🎵 SodaMusic · 汽水音乐播放器

**复刻汽水音乐 UI · 9 种免费 VIP 音效 · 跨平台 · 开源**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin)](https://kotlinlang.org/)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.7-4285F4?logo=jetpackcompose)](https://www.jetbrains.com/compose-multiplatform/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Platforms](https://img.shields.io/badge/Platforms-Android%20%7C%20Desktop%20%7C%20iOS-green)]()

**[English](#-sodamusic)** · **中文**

---

</div>

## 🌟 项目简介

汽水音乐（抖音官方音乐 App）里那些让耳朵"炸裂"的 VIP 音效（超重低音、360° 环绕、清澈人声、HiFi 现场、复古唱片……），**通通免费，开箱即用**。SodaMusic 是一款基于 **Kotlin Multiplatform + Compose Multiplatform** 的开源音乐播放器，深度复刻汽水音乐的深绿主题 UI，同时提供比肩原版的 DSP 音效处理引擎。

- 🎧 **9 种免费 VIP 音效**（含智能音效/超重低音/360°环绕/清澈人声/3D音效/HIFI现场/动感电音/摇滚/复古唱片）
- 🎨 **深度复刻汽水音乐 UI**：深森林绿主题、方形圆角封面、实时频谱动画、底部抽屉音效切换
- 📱 **真正的跨平台**：一套代码跑 Android、桌面（Windows/macOS/Linux）、iOS
- 🔊 **真实音频管线**：Android 走系统 AudioFx（Equalizer/BassBoost/Virtualizer/PresetReverb），桌面端走纯 Kotlin DSP 引擎（5 段参量 EQ + 压缩 + 混响 + 延迟 + 立体声扩展）
- 🎼 **本地音乐扫描**（Android 读 MediaStore；桌面端支持 JFileChooser，记忆上次目录）
- 📡 **支持 MP3/WAV/M4A/FLAC/OGG/AAC**（桌面端用 JLayer 纯 Java MP3 解码）
- 🎙️ **实时频谱可视化**：Android 走系统 Visualizer API，桌面端跑 256 点 FFT

> 📢 **为什么做这个？** 汽水音乐的音效需要付费会员。SodaMusic 用开源方式实现了相同听感的音效链，让所有人都能免费享受音乐的"高级氛围感"。致敬原 App 设计，也致敬所有爱音乐的人。

---

## ✨ 功能特性

| 特性 | Android | Desktop | iOS |
|---|:---:|:---:|:---:|
| MP3/WAV/M4A 等格式播放 | ✅ MediaPlayer + JNI 解码 | ✅ JLayer 纯 Java 解码 | 🚧 |
| 9 种 VIP 音效 | ✅ 系统 AudioFx | ✅ 自写 DSP 引擎 | 🚧 |
| 实时频谱可视化 | ✅ Visualizer | ✅ 256 点 FFT | 🚧 |
| 本地媒体库扫描 | ✅ MediaStore | — | 🚧 |
| 原生文件选择器 | ✅ SAF / 运行时权限 | ✅ JFileChooser | 🚧 |
| 深色森林绿主题 | ✅ | ✅ | ✅ |
| 记忆上次目录 | — | ✅ Preferences | 🚧 |
| 自动播放测试曲目 | — | ✅ | — |

---

## 🎚️ 音效引擎

| 音效 | 听感 | DSP 配置 |
|---|---|---|
| 🎚️ 原声 | 无任何处理 | EQ 平直 / 无压缩 / 无混响 |
| 🧠 智能音效 | 自适应曲风 | 低频提升 + 空气感 + 轻压缩 |
| 🔊 360° 环绕 | 超大声场 | 立体声展宽 3.2x + Haas 延迟 + 房间混响 |
| 🎸 超重低音 | 澎湃低音 | Low Shelf +12dB @80Hz + Bass 压缩 |
| 🎤 清澈人声 | 人声突出 | 砍低频 + 2.5kHz 提升 + 5kHz 空气感 |
| 🌐 3D 音效 | 空间包围 | 宽度 2x + 短延迟 + 小混响 |
| 🎭 HIFI 现场 | 临场感 | Plate 混响 35% + 高频通透 |
| ⚡ 动感电音 | 炸裂电子 | Low +10dB / High +6dB + 高压缩比 |
| 🎸 摇滚音效 | 经典 V 型 | 低频 80Hz +5dB / 高频 6kHz +4dB + 饱和 |
| 📻 复古唱片 | 怀旧黑胶 | 带通滤波 + 黑胶噪声 + Wow/Flutter |

---

## 📸 界面预览

- **首页**：深绿背景、方形圆角封面、封面内嵌实时频谱柱状条动画、当前音效标签 pill（点击展开抽屉）
- **音效抽屉**：底部滑出、半透明蒙层、9 种音效卡片网格、VIP 标签用渐变高亮
- **音乐源**：示例曲目 / 本地扫描 / 在线 URL 三种入口
- **播放控制**：上一首 / 播放暂停 / 下一首 / 随机 / 循环

---

## 🏗️ 技术架构

```
commonMain/              # 共享 Kotlin 代码
├── audio/               # 播放器接口、EffectProcessor DSP 链
│   └── effects/         # Biquad/EQ/Compressor/Reverb/Delay/Widener/Resampler/FFT
├── model/               # Track / PlayState / TrackSource
├── ui/                  # Compose UI (主题/组件/页面)
│   ├── components/      # CoverArt / PlaybackControls / ProgressBar / SpectrumVisualizer / EffectsDrawer / TrackPicker
│   ├── screens/         # PlayerScreen
│   └── theme/           # 深绿调色板 Theme.kt
└── utils/               # FilePicker / DisplayName / LocalScanner / StartupTrack

androidMain/             # Android 实现
├── MediaPlayer + AudioFx (Equalizer/BassBoost/Virtualizer/PresetReverb)
├── Visualizer (FFT 频谱)
├── SAF 文件选择器 (ActivityResultContracts.OpenDocument)
├── MediaStore 本地扫描
└── AndroidFilePicker (权限/选择器 suspend 桥接)

desktopMain/             # Desktop JVM 实现
├── JLayer MP3 解码 (纯 Java)
├── 纯 Kotlin DSP 引擎 (5 段 EQ + 压缩 + 混响 + 延迟 + 饱和)
├── JFileChooser 文件选择器 (记忆目录)
└── 线性插值重采样器
```

**核心技术栈**：

- **Kotlin 2.0.21** + **Compose Multiplatform 1.7.1**
- **Kotlinx Coroutines** 异步流
- **JLayer 1.0.1** 纯 Java MP3 解码（无 native 依赖）
- **Android MediaPlayer / AudioFx / Visualizer**
- **javax.sound.sampled** 桌面音频输出
- 共享 DSP：Biquad IIR 滤波器 + Schroeder 混响 + Haas 延迟 + 中侧立体声扩展 + RMS 压缩 + tanh 软限幅

---

## 🚀 快速运行

### 桌面端（最简单，推荐先跑这个）

前置条件：JDK 17+

```bash
# Linux/macOS
cd SodaMusic
./gradlew :composeApp:run

# Windows
cd SodaMusic
gradlew.bat :composeApp:run
```

启动后会自动查找 `D:/workspace/agent-proj/caee.mp3`（或项目根下 `caee.mp3`）播放。
右上角 🎵 按钮打开 JFileChooser 选音频文件；右上角 ⚖ 按钮切换音效。

### Android

前置条件：Android Studio + Android SDK (API 24+)

```bash
# 命令行构建 debug APK
cd SodaMusic
./gradlew :composeApp:assembleDebug
# APK 位置: composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

或者在 Android Studio 中打开项目根目录，直接 Run `composeApp`。

首次启动请允许：
1. **音频权限**（READ_MEDIA_AUDIO）- 扫描本地音乐
2. **录音权限**（RECORD_AUDIO）- Visualizer 读取音频频谱（不录音）

### iOS

🚧 进行中，欢迎 PR。

---

## 🛠️ 构建可执行文件

桌面端打包成可执行程序（Windows exe / macOS dmg / Linux deb）：

```bash
cd SodaMusic
./gradlew :composeApp:packageDistributionForCurrentOS
# 输出: composeApp/compose/binaries/main/
```

---

## 🤝 贡献

欢迎 PR！可以做的方向：

- [ ] iOS 完整实现（AVAudioEngine + 本地库扫描）
- [ ] 歌词展示（LRC 解析 + 滚动）
- [ ] 播放列表 / 歌单管理
- [ ] 封面图从 MP3 ID3 提取（目前用 coverColor 占位）
- [ ] 更多音效预设（经典/舞曲/蓝调/爵士/乡村）
- [ ] WebAssembly 版本
- [ ] 暗黑/亮色主题切换

提交 PR 前请确保 `./gradlew :composeApp:compileKotlinDesktop :composeApp:assembleDebug` 通过。

---

## ⚠️ 免责声明

- 本项目仅供**学习交流**，UI 风格参考自字节跳动旗下汽水音乐，不做任何商业用途
- "VIP 音效"为 DSP 算法模拟，听感近似但不完全等同于原版，不关联字节跳动任何产品
- 请确保您播放的音乐版权合规，项目仅作为音频 DSP 技术演示

---

## 📄 License

MIT © 2026 SodaMusic Contributors

---

<br/>
<br/>

<div align="center">

# 🎵 SodaMusic

**Soda-Music-inspired UI · 9 Free VIP Audio Effects · Cross-Platform · Open Source**

</div>

## What is SodaMusic?

SodaMusic is an open-source, cross-platform music player built with **Kotlin Multiplatform + Compose Multiplatform**. It brings the Soda Music (Douyin's official music app) look and feel — plus **all 9 "VIP" audio effects unlocked for free**, powered by a pure-Kotlin DSP engine on Desktop and the platform AudioFx framework on Android.

### Highlights
- 🎧 **9 free VIP-grade effects**: Smart, 360° Surround, Super Bass, Clear Vocals, 3D, HiFi Live, Dynamic EDM, Rock, Vinyl Vintage
- 🎨 **Authentic Soda-Music-inspired UI**: deep forest-green theme, rounded-square album art, live spectrum bars, bottom-sheet effect picker
- 📱 **True cross-platform** — single codebase, runs on Android, Desktop (Windows/macOS/Linux), iOS (coming)
- 🔊 **Real audio pipeline**: Android uses platform AudioFx (Equalizer/BassBoost/Virtualizer/PresetReverb); Desktop uses a pure-Kotlin DSP chain (5-band parametric EQ + compressor + reverb + delay + stereo widener)
- 🎼 **Local library scan** (Android MediaStore; Desktop with JFileChooser + last-dir memory)
- 📡 **MP3/WAV/M4A/FLAC/OGG/AAC** via JLayer pure-Java decoder on Desktop
- 🎙️ **Live spectrum visualizer**: Android Visualizer API, 256-point FFT on Desktop

### Quick Start (Desktop)

```bash
cd SodaMusic
./gradlew :composeApp:run      # macOS / Linux
gradlew.bat :composeApp:run    # Windows
```

Requires JDK 17+. On first launch it auto-plays `caee.mp3` from the project root. Use the 🎵 icon to pick another file, ⚖ to cycle effects.

### Android

```bash
cd SodaMusic
./gradlew :composeApp:assembleDebug
# APK: composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

Grant audio + record-audio permissions when prompted (record-audio is only used to read the audio-session FFT for the visualizer — no audio is captured).

### Architecture

- `commonMain/audio/effects` — Biquad/EQ/Compressor/Reverb/Delay/Widener/Resampler/FFT
- `androidMain` — MediaPlayer playback, AudioFx effects, Visualizer FFT, SAF picker, MediaStore scan
- `desktopMain` — JLayer MP3 decode, full Kotlin DSP chain, JFileChooser, PCM resampler
- `commonMain/ui` — Compose screens & reusable components

### Effects (Preset Overview)

| Effect | Signature |
|---|---|
| Smart | Gentle bass + air band |
| 360° Surround | 3.2x widening, Haas delay, room reverb |
| Super Bass | +12 dB low shelf @80 Hz + bass compressor |
| Clear Vocals | Low cut, 2.5 kHz presence, 5 kHz air |
| 3D Audio | 2x width, short slap delay, small reverb |
| HiFi Live | Large-hall reverb, high shelf lift |
| Dynamic EDM | +8 dB bass / +6 dB highs, heavy compression |
| Rock | V-curve EQ with 0.15% saturation |
| Vintage | Band-pass, wow/flutter, vinyl noise |

### Contributing

PRs welcome! See [the Chinese section](#-贡献) for the roadmap.

### License

MIT © 2026 SodaMusic Contributors

### Disclaimer

This project is for **educational purposes only**. The UI design is inspired by Soda Music (ByteDance). It is not affiliated with, endorsed by, or sponsored by ByteDance. "VIP effects" are algorithmic approximations — please respect music copyrights in your jurisdiction.
