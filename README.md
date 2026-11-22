<div align="center">

# 🎧 Apex Route
### System Audio Routing Engine for Android

[![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B%20(API%26%2B)-success.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

*Route all media, Instagram Reels, TikTok, YouTube, and WhatsApp voice notes through your phone's earpiece—built specifically for devices with a broken main speaker.*

</div>

---

## ✨ Features

- **Global Earpiece Audio Routing**: Routes system audio, media streams, and voice notes directly through the phone's built-in earpiece receiver.
- **Active Keep-Alive Audio Engine**: Employs a background foreground service with a continuous communication audio track to lock the Android Audio Policy (`AudioFlinger` / HAL) into communication mode.
- **Proximity Sensor Assistant**: Automatically reinforces earpiece routing when holding the phone to your ear (essential for WhatsApp voice notes and ear-held playback).
- **In-App Earpiece Test Tone**: Built-in 440Hz test tone generator to verify earpiece functionality instantly.
- **Hardware Diagnostics**: Real-time detection and verification of built-in earpiece, speaker, wired headsets, and Bluetooth devices.
- **Online Update Checker**: Integrates directly with GitHub Releases to check for and notify users of newer versions.
- **Modern UI & Typography**: Styled with **Space Grotesk** and **Inter** fonts in a sleek, professional dark theme.

---

## 📱 Screenshots & UI

<div align="center">
  <img src="Apex-Route%20SS.jpeg" alt="Apex Route UI Screenshot" width="325"/>
  <p><em>Apex Route Dashboard — Earpiece Routing & Hardware Diagnostics</em></p>
</div>

---

## 🛠️ Technical Architecture

- **`AudioRoutingManager`**: Manages audio mode (`MODE_IN_COMMUNICATION`), speakerphone suppression, communication devices (Android 12+ API 31+), and HAL `setParameters` audio driver routing.
- **`AudioRouteService`**: Bound foreground service complying with Android 14+ (`FOREGROUND_SERVICE_TYPE_SPECIAL_USE`) featuring persistent notification quick actions.
- **`EarpieceKeepAlivePlayer`**: Maintains active voice communication audio attributes (`USAGE_VOICE_COMMUNICATION`) to force media routing without root access.
- **`ProximitySensorManager`**: Handles proximity sensor events to maintain earpiece state during ear-held playback.

---

## 👨‍💻 Developer

**Mohsin Raza**
- Software Engineer | Frontend Developer | Python Programmer | AI Enthusiast | Open-Source Developer
- **GitHub**: [@mohsinapex](https://github.com/mohsinapex)
- **Telegram**: [@mohsinapex](https://t.me/mohsinapex)
- **LinkedIn**: [mohsinapex](https://linkedin.com/in/mohsinapex)

---

## 📥 Download & Installation

1. Go to the [Releases](https://github.com/mohsinapex/Apex-Route/releases) page.
2. Download the latest signed APK (`ApexRoute-1.0.apk`).
3. Install the APK on your Android device (Android 8.0 or higher).
4. Open Apex Route, select **Phone Earpiece**, and enjoy your audio!

---

## 📄 License

This project is licensed under the terms of the [GNU General Public License v3.0](LICENSE).
