# HATCH (هتش) — Android APK & WebAPK Implementation Plan

A comprehensive technical and user experience plan to provide a complete Android application (APK / WebAPK) for **HATCH — هتش**, enabling one-tap installation on Android smartphones, standalone mobile execution, offline caching, and direct APK generation via PWABuilder / Bubblewrap.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> Because web cloud sandboxes cannot directly compile binary `.apk` files using the Android SDK/Gradle locally, modern web applications provide Android APK capabilities through two synergistic mechanisms:
> 1. **Direct One-Tap Android Installation (WebAPK / PWA)**: Android Chrome and browsers automatically generate and install a real system APK on the user's Android phone directly from the manifest.
> 2. **In-App Android APK Hub (PWABuilder / Capacitor Export)**: A built-in modal that allows the user to download the generated manifest configuration, or generate a signed `.apk` file for Google Play or direct sideloading via PWABuilder in under 2 minutes.

- **Confirmed Decision 1**: Provide an in-app "تثبيت كـ تطبيق (APK)" / "Install APK" button in the header and profile.
- **Confirmed Decision 2**: Implement full PWA compliance using `vite-plugin-pwa` with service worker caching, Android-ready maskable icons, and standalone display mode.
- **Confirmed Decision 3**: Add an Android APK Hub modal explaining one-tap installation and providing direct access to export a standalone APK package.

---

### 1. Overview & Core Concept

- **What It Delivers**: Transforms the "HATCH — هتش" marketplace into an installable Android mobile application. Once installed on Android devices, it launches with a custom splash screen, runs in fullscreen standalone mode (no browser address bar), and operates smoothly even on slow networks in Sudan.
- **Target Audience**: Sudanese buyers and sellers using Android smartphones (Samsung, Xiaomi, Tecno, Infinix, etc.) who prefer an app-like icon on their home screen rather than navigating via a web browser.
- **Key Value**: Native look and feel, immediate access from the home screen, reduced data consumption through aggressive offline caching, and frictionless installation without requiring Google Play Store account setup.

---

### 2. User Experience & Visual Design

- **In-App APK / Install Trigger**:
  - A stylish amber & gold pill button in the top navigation header:
    - In Arabic: `📱 تثبيت التطبيق (APK)`
    - In English: `📱 Install APK`
  - Also accessible from the **حسابي (Profile)** screen under a dedicated **«تطبيق أندرويد (APK)»** card.
- **Interactive APK Install Modal**:
  - When tapped, if the browser supports `beforeinstallprompt`, it triggers immediate Android system installation.
  - If already installed or on an external browser, it opens the **Android APK Hub**:
    - **الخيار الأول: التثبيت الفوري على الهاتف (WebAPK)**: One-click install directly onto the device home screen.
    - **الخيار الثاني: تحميل ملف APK مخصص**: Step-by-step guidance to generate an `.apk` via PWABuilder or download the Android package bundle.
- **Standalone Mobile Posture**:
  - Theme color: `#0a0a0a` (Deep Charcoal matching HATCH theme).
  - Background color: `#0a0a0a`.
  - Iconography: Glowing amber **هـ / H** emblem with 15% safe padding for Android adaptive/maskable icons.

---

### 3. Key Technical Decisions & Trade-Offs

- **Decision 1: `vite-plugin-pwa` over manual Service Worker**
  - *Chosen Approach*: Configure `vite-plugin-pwa` in `vite.config.ts`.
  - *Why*: Provides automatic asset precaching, background update triggers, and standards-compliant Web App Manifest generation that meets all Android WebAPK criteria.
- **Decision 2: High-Resolution Adaptive Icons**
  - *Chosen Approach*: Generate SVG and canvas-based 192x192, 512x512, and maskable 512x512 icons in `public/`.
  - *Why*: Android requires both `purpose: "any"` and `purpose: "maskable"` icons to prevent awkward squircle clipping on One UI, MIUI, and stock Android.
- **Decision 3: Dedicated In-App APK Modal**
  - *Chosen Approach*: Create `ApkInstallModal.tsx` and `usePWAInstall.ts`.
  - *Why*: Gives users who explicitly asked for an "APK" clarity on how Android installs the app instantly, while providing external export resources if they need an offline APK installer file.

---

### 4. Technical Architecture & System Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                 HATCH — هتش Client Layer                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌──────────────────┐               ┌──────────────────┐   │
│   │   Header & Nav   │               │   Profile View   │   │
│   │  [تثبيت كـ APK]  │               │   [Android APK]  │   │
│   └─────────┬────────┘               └────────┬─────────┘   │
│             │                                 │             │
│             └───────────────┬─────────────────┘             │
│                             ▼                               │
│                 ┌───────────────────────┐                   │
│                 │   ApkInstallModal     │                   │
│                 │ (WebAPK + APK Guide)  │                   │
│                 └───────────┬───────────┘                   │
│                             │                               │
│                             ▼                               │
│                 ┌───────────────────────┐                   │
│                 │    usePWAInstall      │                   │
│                 │ (beforeinstallprompt) │                   │
│                 └───────────┬───────────┘                   │
│                             │                               │
├─────────────────────────────┼───────────────────────────────┤
│                             ▼                               │
│                Android System / Browser                     │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ 1. Android WebAPK Engine: Installs real .apk to OS     │  │
│  │ 2. Service Worker: Offline caching & fast reload      │  │
│  │ 3. Web App Manifest: Standalone display & theme color │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

### 5. Implementation Steps

1. **Install Dependencies**:
   - Install `vite-plugin-pwa` as a dev dependency.
2. **Configure PWA in `vite.config.ts`**:
   - Add `VitePWA` plugin with name `'هتش — HATCH'`, short name `'هتش'`, theme color `#0a0a0a`, and standalone display.
   - Configure runtime caching for Google Fonts and Unsplash imagery.
3. **Generate Icon Assets in `/public`**:
   - `icon.svg`: Modern vector emblem.
   - `pwa-192x192.png`, `pwa-512x512.png`, and `pwa-maskable-512x512.png`.
   - `apple-touch-icon.png` (for iOS fallback).
4. **Create `usePWAInstall.ts` Hook**:
   - Capture `beforeinstallprompt` event.
   - Detect standalone mode and platform.
5. **Create `ApkInstallModal.tsx` Component**:
   - Interactive modal showing:
     - Direct One-Click Install to Android.
     - Direct link to PWABuilder to package a signed standalone APK file.
     - Offline capability indicators.
6. **Integrate Install Triggers**:
   - Add APK install button in `Header.tsx`.
   - Add APK download card in `ProfileView.tsx`.
   - Wire state into `AppContext.tsx`.
7. **Verify & Test**:
   - Run `lint_applet` and `compile_applet`.
   - Confirm PWA manifest renders and passes all requirements.
