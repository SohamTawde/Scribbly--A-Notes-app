# ✏️ Scribbly — All Your Ideas in One Place

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM-4285F4.svg?logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Material%203-Expressive-FF7043.svg)](https://m3.material.io)
[![Room Database](https://img.shields.io/badge/Room-SQLite-009688.svg)](https://developer.android.com/training/data-storage/room)
[![Android](https://img.shields.io/badge/Android-SDK%2026%2B-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com)

A modern, fluid, and expressive notes & productivity app built with **Jetpack Compose**, featuring rich multimedia capture, tactile floppy sticky-notes, a biometric vault, and hand-crafted vector animations.

---

## ✨ Features

### 📝 Multi-Format Note Taking
- **Text Notes**: Rich text editing with Markdown-style formatting, tags, search, and instant color coding.
- **Voice Notes**: In-app audio recording with real-time waveform visualizers and voice-to-text integration.
- **Image & OCR Notes**: Camera & gallery imports with on-device text recognition (OCR) to turn photos of documents into editable text.
- **Sketch & Doodle Notes**: Fluid drawing canvas with smooth pressure curves, stroke smoothing, and custom brush types.

### 📌 Interactive Sticky-Note Pinboard ("Today's Tasks")
- **Tactile Paper Physics**: Notes sway, tilt, and flop from their fasteners with realistic spring physics.
- **Variety of Fasteners**: Push pins, silver thumbtacks, binder clips, colorful magnets, washi tape, and heart pins with randomized color assignments.
- **Curled Paper Edges**: Subtle paper lift, corner curls, top lighting sheen, and realistic cast shadows.
- **Reminders & Geofencing**: Time-based alerts and location-aware reminders.

### 🔒 Biometric Private Vault
- Secure vault locked behind Android BiometricPrompt (fingerprint / face unlock / device PIN).
- Keep confidential notes, passwords, and sensitive ideas safe from prying eyes.

### 🎬 Hand-Crafted Vector Launch Animation
- Smooth 5.0-second opening storyboard sequence:
  - 3D notebook landing bounce with accent bursts
  - Digital stylus swooping in and tracing a continuous doodle in real-time coordinates
  - Vibrant liquid splash background expansion into rich purple
  - Spring-loaded typography badges and interactive **"Get Started"** button
  - Continuous idle floating loop that keeps the canvas alive until user interaction

### 🎨 Themes & Aesthetics
- **Adaptive Dark / Light Mode**
- **Mesh Gradient Glassmorphism Backgrounds**
- **Material 3 Expressive Design System**

---

## 🏗️ Architecture & Tech Stack

- **UI**: 100% Jetpack Compose with Material 3 components
- **Architecture**: MVVM (Model-View-ViewModel) + Unidirectional Data Flow (UDF)
- **Local Database**: Room ORM (SQLite) with asynchronous Kotlin Coroutines & Flow
- **Navigation**: Modern Navigation Compose with animated transitions
- **Sensors & System APIs**: Android Biometrics, CameraX, AudioRecorder, Location / Geofencing

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog / Iguana / Ladybug or newer
- JDK 17 or higher
- Android device or emulator running **API 26+** (Android 8.0 Oreo or higher)

### Build & Run
1. Clone the repository:
   ```bash
   git clone <repo-url>
   cd ANDRIO_APP
   ```
2. Open the project in **Android Studio**.
3. Let Gradle sync and download dependencies.
4. Run on your device:
   ```bash
   ./gradlew installDebug
   ```

---

## 📄 License & Copyright

Copyright © 2026 **Soham Tawde**. All rights reserved.

This project is licensed under the [MIT License](LICENSE).
