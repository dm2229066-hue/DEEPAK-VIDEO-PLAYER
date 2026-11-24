# Deepak Video Player

A native Android video player built with **Kotlin**, **Jetpack Compose**, **Material Design 3**, **Android Media3 ExoPlayer**, **Room**, **DataStore**, and **Google AdMob**.

---

## Debug APK Build Instructions

1. Open the project in **Android Studio** (or run from command line with JDK 17 + Android SDK installed).
2. Ensure no `local.properties` overrides `ANDROID_SDK_ROOT` in CI environments.
3. Build the debug APK:
   ```bash
   ./gradlew :app:assembleDebug
   ```
   *(In the AI Studio environment, use `gradle :app:assembleDebug` or the built-in compiler.)*
4. Output APK path:
   `app/build/outputs/apk/debug/app-debug.apk`

---

## Release APK Signing Instructions

Do **not** commit keystores, keystore passwords, or production AdMob IDs to source control.

1. Generate an upload keystore locally (one-time):
   ```bash
   keytool -genkeypair -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
   ```
2. Provide signing environment variables at build time:
   ```bash
   export KEYSTORE_PATH=/path/to/my-upload-key.jks
   export STORE_PASSWORD=your_store_password
   export KEY_PASSWORD=your_key_password
   ```
3. Override test AdMob IDs with your production AdMob IDs via Gradle properties:
   ```bash
   ./gradlew :app:bundleRelease \
     -PADMOB_APP_ID="ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY" \
     -PADMOB_BANNER_ID="ca-app-pub-XXXXXXXXXXXXXXXX/BBBBBBBBBB" \
     -PADMOB_INTERSTITIAL_ID="ca-app-pub-XXXXXXXXXXXXXXXX/IIIIIIIIII" \
     -PADMOB_REWARDED_ID="ca-app-pub-XXXXXXXXXXXXXXXX/RRRRRRRRRR"
   ```
   Or build a signed release APK with `./gradlew :app:assembleRelease`.

4. **GitHub Actions CI Release Signing**:
   - Release signing is already configured in `app/build.gradle.kts` (`release` signing config with `keyAlias = "upload"`).
   - Encode your keystore to Base64 (`base64 -w 0 my-upload-key.jks` on Linux, or `base64 -i my-upload-key.jks` on macOS) and add these **Repository Secrets** in GitHub (**Settings → Secrets and variables → Actions**):
     - `KEYSTORE_BASE64`: Base64 string of `my-upload-key.jks`
     - `STORE_PASSWORD`: Keystore password
     - `KEY_PASSWORD`: Key password for alias `upload`
     - *(Optional for production ads)* `ADMOB_APP_ID`, `ADMOB_BANNER_ID`, `ADMOB_INTERSTITIAL_ID`, `ADMOB_REWARDED_ID`
   - Once set, `.github/workflows/build-debug-apk.yml` automatically builds and uploads signed `app-release.apk` and `app-release.aab` artifacts.

---

## Implemented & Hardware-Dependent Features

### Implemented
- **Branding & UI**: Custom adaptive launcher icon, splash screen overlay, Obsidian/Charcoal & Golden Yellow dark theme + Light theme, bundled Google Fonts (`Outfit`, `Plus Jakarta Sans`, `JetBrains Mono`), responsive phone (`NavigationBar`) and tablet (`NavigationRail`) layouts.
- **Library & Home Dashboard**: MediaStore automatic video scan, Storage Access Framework (SAF) single-file and folder-tree mounting, Continue Watching, Recently Played, Recently Added, Favorites, Folders, Playlists, Live Search, Grid/List toggle, Sort by Name/Date/Size/Duration.
- **Media3 ExoPlayer**: Play/Pause, +/-10s skip, seekbar, speed `0.25x–4.0x`, Fit/Fill/Crop/Stretch aspect modes, orientation & rotation lock, lock-screen controls, auto-play next, resume position, A-B repeat, sleep timer, Picture-in-Picture (API 26+), `MediaSessionService` background playback.
- **Gestures**: Single tap (toggle controls), Double-tap left/center/right (rewind/play-pause/forward), Horizontal swipe seek, Vertical left brightness, Vertical right volume, Two-finger pinch-to-zoom (`0.5x–4.0x`) with visual HUD.
- **Subtitles**: Embedded tracks + SAF external `.srt`, `.vtt`, `.ass`, `.ssa`, external subtitle timestamp sync (`-10s` to `+10s`), font size, color, vertical position.
- **Audio Engine**: Audio track selection, Stereo-to-Mono PCM downmix processor, device-supported `Equalizer`, `BassBoost`, `Virtualizer`, and `LoudnessEnhancer`.
- **File Manager**: Internal & external SAF browsing, folder hide/unhide, scoped-storage Rename & Delete with confirmation, Share intent, `MediaMetadataRetriever` metadata inspector.
- **Network Streams**: HTTP, HTTPS, HLS (`.m3u8`), DASH (`.mpd`), RTSP URL player with stream history and error/retry UI.
- **AdMob Monetization**: UMP Consent SDK, Adaptive Banner ads, frequency-capped Interstitial ads (post-player return only), and Rewarded ads to unlock Pro accent themes.

### Device & Android Platform Limitations
- **Legacy Codecs (AVI / FLV / WMV)**: Playback relies on the device's OEM `MediaCodec` hardware/software decoders; proprietary codecs not present in Android's platform MediaCodec will show a clear unsupported-codec error rather than crashing.
- **Audio Effects (`Equalizer`, `BassBoost`, `Virtualizer`, `LoudnessEnhancer`)**: Availability depends on the device's audio HAL and output route; unsupported effects are clearly marked as unavailable in the Audio sheet.
- **Subtitle Sync Offset**: Real-time timestamp shifting applies to external `.srt`/`.vtt`/`.ass`/`.ssa` files loaded via SAF; embedded container subtitle tracks cannot be shifted without remuxing.
- **File Rename on Android 10+**: Renaming MediaStore files owned by other apps requires system write consent (`MediaStore.createWriteRequest`) and may be restricted by scoped storage on some OEMs.
