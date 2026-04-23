<div align="center">

# 💎 Gelio

🎨 **Modern, Tablet-First Digital Showcase & Kiosk Builder**

Built with Jetpack Compose and Material 3 Expressive. Gelio is an offline-first, local-content platform for creating professional presentation shells on Android tablets.

</div>

---

## 📑 Table Of Contents

- [Scope And Intent](#scope-and-intent)
- [Quick Glance](#quick-glance)
- [Tested Environment](#tested-environment)
- [App Specification](#app-specification)
- [What The App Does](#what-the-app-does)
- [Section Ecosystem Deep-Dive](#section-ecosystem-deep-dive)
- [How Dynamic Branding Works](#how-dynamic-branding-works)
- [How .kskm Storage Works](#how-kskm-storage-works)
- [Kiosk Lockdown System](#kiosk-lockdown-system)
- [Deep Technical Architecture](#deep-technical-architecture)
- [Media & Rendering Pipeline](#media--rendering-pipeline)
- [Persistence & Migrations](#persistence--migrations)
- [Major Development Setbacks & Fixes](#major-development-setbacks--fixes)
- [Handoff Summary](#handoff-summary)
- [Project Structure & File Glossary](#project-structure--file-glossary)
- [Troubleshooting & FAQ](#troubleshooting--faq)
- [Screenshots](#screenshots)
- [Build & Run Notes](#build--run-notes)
- [Legacy & Origin Disclaimer](#legacy--origin-disclaimer)

---

> [!TIP]
> Use the links above to jump to a section. Then click the expand row under each heading to open the full content.

## 🚀 Scope And Intent

<details>
<summary><strong>Open section</strong></summary>

<br>

Gelio is a sophisticated Android application designed for high-end tablet deployments in showrooms, galleries, and retail environments. It provides a clean, neutral canvas for businesses to showcase their portfolio, services, and digital assets without requiring cloud dependencies or complex backend infrastructure.

Unlike traditional branded apps, Gelio is a **dynamic showcase builder**. You start with a blank slate and build your own multi-company experience through an intuitive on-device administrative panel.

Gelio was built to:
- Provide a premium, Material 3 Expressive client experience.
- Enable full local content management without a PC or server.
- Support aggressive kiosk lockdown for public-facing hardware.
- Allow seamless portability of entire "Showcases" via a custom backup format.
- Maintain high data integrity for transparent media and professional branding.

</details>

## 📊 Quick Glance

| Item | Details |
| --- | --- |
| **Primary purpose** | Tablet-first digital showcase builder & Kiosk |
| **Main Target** | Real Estate, Tourism, Art Galleries, Retail |
| **App stack** | Kotlin, Compose, Room, DataStore, Coil, Media3 |
| **Archive format** | `.kskm` (Encrypted Zip Container) |
| **Lockdown** | Watchdog + Accessibility Kiosk Mode |
| **Min SDK** | 26 (Android 8.0) |
| **Target SDK** | 36 (Android Readiness) |
| **Branding** | Dynamic Tonal Palette Generation |

---

## 🧪 Tested Environment

<details>
<summary><strong>Open section</strong></summary>

<br>

Gelio has been validated on a variety of hardware to ensure stability in long-running kiosk environments:

- **Primary Tablet**: Samsung Galaxy Tab S9 (Android 14)
- **Secondary Tablet**: Samsung Galaxy Tab A8 (Android 13)
- **Budget Target**: Lenovo Tab M10 (Android 12)
- **Emulator**: Pixel Tablet (API 34)

**Performance Metrics:**
- **Idle Memory**: ~85MB
- **Peak Media Loading**: ~210MB (Well within 2GB+ tablet limits)
- **Startup Time**: < 1.2s to fully interactive stage
- **Frame Rate**: Consistent 60fps during shared element transitions

</details>

## 🛠️ App Specification

<details>
<summary><strong>Open section</strong></summary>

<br>

- **Language**: 100% Kotlin
- **UI Framework**: Jetpack Compose (Material 3 Expressive)
- **Navigation**: Compose Navigation with Shared Element Support
- **DI Pattern**: Manual `AppContainer` (Service Locator)
- **Persistence**: Room SQLite (Content) + DataStore (Settings)
- **Image Loader**: Coil 3 with ARGB_8888 forced transparency
- **PDF Engine**: Native `PdfRenderer` with Bitmap Extraction
- **Video Player**: AndroidX Media3 / ExoPlayer
- **Network Stack**: Retrofit (for optional Pexels integration)
- **Build System**: Gradle Kotlin DSL + Version Catalogs

</details>

## 📱 What The App Does

<details>
<summary><strong>Open section</strong></summary>

<br>

Gelio is a dual-purpose application that bridges the gap between a content management system and a high-end presentation shell.

### 1. Client Presentation Stage
This is the public-facing side of the app. It is designed to be "invisible"—meaning the focus is entirely on the company's content.
- **Dynamic Welcome Screen**: Detects how many companies are configured and adjusts its layout from single-brand to gallery selection.
- **Tonal Theming**: The UI colors shift to match the currently selected company's branding.
- **Horizontal Rail Browsing**: High-performance scrolling for images and cards.
- **Interactive Viewers**: Specialized full-screen modes for every media type.

### 2. Administrative Suite
The control center for the entire showcase.
- **Company Management**: Add/Edit/Reorder business entities.
- **Section Configurator**: Build the tab-based navigation for each company.
- **Media Import Pipeline**: Import files from device storage, normalize them, and generate thumbnails.
- **Kiosk Settings**: Configure the watchdog, accessibility services, and auto-boot behavior.
- **Backup & Reset**: Full portability controls and "Clean Slate" factory reset options.

</details>

## 🧩 Section Ecosystem Deep-Dive

<details>
<summary><strong>Open section</strong></summary>

<br>

Each section in Gelio is a modular unit with its own UI and data model.

### 1. 📷 Image Gallery
- High-res hero images with pinch-to-zoom.
- Background pre-warming of adjacent images.
- Title and description overlays.

### 2. 🌐 360° Virtual Tours
- Embedded WebView with cross-origin navigation blocks.
- Automatic thumbnail fetching from Matterport/Kuula URLs.
- Full-screen immersive mode.

### 3. 📺 Video Library
- Support for local MP4 files in app storage.
- YouTube and Vimeo streaming integration.
- Custom play-button thumbnail overrides.

### 4. 📄 Digital Brochures
- Smooth, physics-based PDF paging.
- Automated cover extraction from the first page of the document.
- Grid view for multi-brochure collections.

### 5. 🏢 Portfolio Cards
- Featured project layouts with key stats.
- Hero-to-Detail shared element transitions.
- Inline CTA buttons for external links or other sections.

### 6. 📍 Interactive Maps
- Pin dropping on World or Regional maps.
- Admin viewport locking (Sets the default zoom/center).
- SVG-based region highlighting.

### 7. ⭐ Google Reviews
- Professional social proof rendering.
- Star ratings and timestamp displays.
- Hand-picked review selection for quality control.

### 8. 🎨 Art Gallery
- Complex hierarchy: Group -> Hero -> Gallery.
- Vertical scrolling for groups, horizontal for images.
- Premium typography and spacing.

</details>

## 🎨 How Dynamic Branding Works

<details>
<summary><strong>Open section</strong></summary>

<br>

Gelio uses a custom **Tonal Palette Engine** located in `Color.kt`.

### The Logic:
1. **Seed Color**: Admin picks a primary brand color (e.g., `#FF5722`).
2. **HSL Transformation**: The engine breaks the color into Hue, Saturation, and Lightness.
3. **Tonal Generation**: It generates a range of tones (T10, T20... T90) using the brand's hue.
4. **Contrast Correction**: The engine automatically picks the tones that provide the best legibility for text overlays.
5. **Dark Mode Adaptation**: Surfaces are automatically inverted while keeping the brand's primary "identity" colors vibrant.

</details>

## 📦 How .kskm Storage Works

<details>
<summary><strong>Open section</strong></summary>

<br>

The `.kskm` format is a proprietary ZIP-based archive used for full portability.

### File Structure:
- `manifest.json`: Versioning and file index.
- `settings/app_settings.json`: Global app configuration.
- `content/`: Room database exports in JSON format.
- `media/`: Raw assets (images, PDFs) renamed to unique UUIDs.
- `verifier.json`: Hash signatures for data integrity.

### Import Flow:
1. Unpack to temporary workspace.
2. Validate `manifest.json` against current app version.
3. Copy media to app-private storage.
4. Rebuild SQLite database from JSON files.
5. Wipe temporary workspace.

</details>

## 🛡️ Kiosk Lockdown System

<details>
<summary><strong>Open section</strong></summary>

<br>

Gelio is built to survive in public environments with zero supervision.

- **Watchdog Service**: A high-priority foreground service that monitors app state.
- **Accessibility Hook**: Intercepts and blocks the Notification Shade, Home, and Recents buttons.
- **Overlay Shield**: Renders an invisible layer over system bars to catch stray gestures.
- **Auto-Boot**: Optionally starts the app immediately upon device power-up.
- **Sustained Performance**: Locks CPU/GPU clocks to prevent thermal throttling.

</details>

## 🏗️ Deep Technical Architecture

<details>
<summary><strong>Open section</strong></summary>

<br>

Gelio is built on a "Repository-First" reactive architecture.

### The Stack:
- **Presentation**: Compose + ViewModel + Shared Transitions.
- **Domain**: Repository layer with `StateFlow` streams.
- **Data**: Room Persistence + DataStore Preferences.
- **Media**: Coil + Media3 + PdfRenderer.

### Performance Strategy:
- **Preloading**: `StartupWarmupState` ensures the database is hot before navigation.
- **Scoping**: Flows are scoped to the active section to prevent background memory leaks.
- **Normalization**: Images are compressed and resized during import to reduce disk footprint.

</details>

## 🖼️ Media & Rendering Pipeline

<details>
<summary><strong>Open section</strong></summary>

<br>

- **Transparency Support**: Uses `ARGB_8888` and PNG for all branding assets to avoid black background artifacts.
- **Hardware Bitmaps**: Selective disabling of hardware bitmaps in Coil for specific Mali/Adreno GPUs.
- **Thumbnailing**: Automated `.webp` thumbnail generation (capped at 1024px) for all visual content.
- **Cache Policy**: Smart caching of map previews and PDF covers to avoid re-rendering.

</details>

## 🗄️ Persistence & Migrations

<details>
<summary><strong>Open section</strong></summary>

<br>

Gelio uses a versioned Room implementation with manual migration paths.

- **Current Version**: 9
- **Migration History**:
  - v7 ⮕ v8: Multi-company logo support.
  - v8 ⮕ v9: Map coordinate optimization.
- **Reliability**: Uses Write-Ahead Logging (WAL) and automated schema exports for testing.

</details>

## 🚧 Major Development Setbacks & Fixes

<details>
<summary><strong>Open section</strong></summary>

<br>

### 1. The "Black Background" Logo Crisis
**Fix**: Refactored `LocalMediaStorage.kt` to force `ARGB_8888` and PNG. Added custom Coil `ImageLoader` logic to disable hardware bitmaps.

### 2. Multi-Company Layout Complexity
**Fix**: Implemented a dynamic `WelcomeScreen` and `ClientShowcaseStage` that detects company count at runtime.

### 3. Kiosk Mode vs. Android 13+
**Fix**: Implemented the "Foreground Service" pattern with a persistent notification and hooked into the "Accessibility Service."

</details>

## 🤝 Handoff Summary

<details>
<summary><strong>Open section</strong></summary>

<br>

- **Phase 1**: Foundation & Core Sections.
- **Phase 2**: Premium Branding & Media Fixes.
- **Phase 3**: Multi-Company Dynamic Logic.
- **Phase 4**: Open Source Neutralization & Gelio Rebranding.

</details>

## 📂 Project Structure & File Glossary

<details>
<summary><strong>Open section</strong></summary>

<br>

```text
Gelio/
├── app/                            # Core Module
│   ├── src/main/java/io/gelio/app/
│   │   ├── MainActivity.kt         # Entry Point
│   │   ├── app/                    # Global State
│   │   │   ├── AppContainer.kt     # DI
│   │   │   ├── GelioApp.kt         # Application
│   │   │   └── ShowcaseViewModel.kt # Logic
│   │   ├── core/                   # Infrastructure
│   │   │   ├── navigation/         # NavHost
│   │   │   ├── theme/              # Styles
│   │   │   └── ui/                 # Components
│   │   ├── data/                   # Data Layer
│   │   │   ├── backup/             # .kskm logic
│   │   │   ├── local/              # Room
│   │   │   └── repository/         # Repos
│   │   ├── features/               # Functional Screens
│   │   │   ├── admin/              # Management
│   │   │   ├── clientstage/        # Client Shell
│   │   │   ├── company/            # Branded Shell
│   │   │   └── map/                # Map Logic
│   │   └── kiosk/                  # Lockdown
│   └── src/main/res/               # Assets
├── docs/                           # Documentation
└── build.gradle.kts                # Build Script
```

</details>

## ❓ Troubleshooting & FAQ

<details>
<summary><strong>Open section</strong></summary>

<br>

- **Black Logos?**: Ensure PNG transparency and re-import.
- **Home Button Works?**: Check Accessibility Service permissions.
- **Forgot PIN?**: Clear app data via ADB.
- **4K Video Lag?**: Recommended 1080p H.264 MP4.

</details>

---

## 🖼️ Screenshots

<div align="center">

### Main User Experience
<p>
  <img src="docs/screenshots/Idle Screen.png" alt="Gelio Idle Welcome Screen" width="400" />
  <img src="docs/screenshots/Image Gallary.png" alt="High-Performance Image Gallery" width="400" />
</p>

### Administrative Power
<p>
  <img src="docs/screenshots/Admin Panel.png" alt="Admin Dashboard" width="400" />
  <img src="docs/screenshots/Sections Config.png" alt="Section Management" width="400" />
</p>

### Deep Customization
<p>
  <img src="docs/screenshots/App Settings.png" alt="Global Settings" width="400" />
  <img src="docs/screenshots/Art Galary.png" alt="Art Gallery Section" width="400" />
</p>

### Interactive Content
<p>
  <img src="docs/screenshots/TerraInk Map.png" alt="Interactive Map System" width="400" />
  <img src="docs/screenshots/Export Import.png" alt="Backup and Restore System" width="400" />
</p>

</div>

---

## 🛠️ Build & Run Notes

<details>
<summary><strong>Open section</strong></summary>

<br>

- **Studio**: Koala+
- **SDK**: API 36
- **Java**: 17
- **Blank Slate**: App opens empty; use Admin > Backup to import or create a company.

</details>

---

## ⚠️ Legacy & Origin Disclaimer

<details>
<summary><strong>Open section</strong></summary>

<br>

> [!CAUTION]
> This project was originally developed for a specific corporate showcase deployment. It has since been refactored and generalized into the open-source **Gelio** platform. 
> 
> While the core branding has been neutralized and the "seeded" content removed, you may occasionally find legacy naming conventions (e.g. `OutMazed` in internal file paths or logs), internal asset remnants, or "random things" in the deep codebase. These do not affect runtime behavior and are being systematically cleaned. 
> 
> If you find a branded asset or a hardcoded string that seems company-specific, please ignore it or submit a PR to help us purge it!

</details>

---

<div align="center">
  <p>Built with ❤️ for the Open Source Community</p>
  <p><b>Gelio Framework v1.0.0-beta</b></p>
</div>
