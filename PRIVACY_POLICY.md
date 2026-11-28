# Privacy Policy — Deepak Video Player

**Effective Date:** November 28, 2026  
**Application:** Deepak Video Player (`com.aistudio.deepakvideoplayer.kzvqmx`)

Deepak Video Player ("we", "our", or "the app") is a local-first Android media player built with AndroidX Media3 ExoPlayer. Protecting your privacy is fundamental to how the app works: **your personal video files, folders, playlists, and playback history stay on your device and are never uploaded to our servers.**

---

## 1. Information We Do Not Collect

- We do **not** require user registration, accounts, or login credentials.
- We do **not** collect your name, phone number, contacts, precise GPS location, or personal media library contents.
- We do **not** upload or transmit your local video files or watch history to any developer-controlled server.

---

## 2. Information Stored Locally on Your Device

To provide resume playback, library management, and customization, Deepak Video Player stores operational data strictly inside your device's private app storage:

- **Playback History (Local Room Database):** Video URI, display title, last playback position, duration, playback speed, audio/subtitle delay offset, and external subtitle URI.
- **Library Organization (Local Room Database):** Favorite markers, user-created playlists, mounted Storage Access Framework (SAF) folder URIs, and recently entered network stream URLs.
- **Preferences (Jetpack DataStore):** Theme selection, accent color, default speed, gesture preferences, subtitle appearance settings, hidden-folder toggle, and selected app language.
- **Temporary Cache:** When you shift subtitle timing (`subtitle offset`), a temporary adjusted subtitle file is generated in the app's private cache directory.

### Your Controls
- Tap **Settings → Clear Playback History** at any time to wipe saved resume positions and recently played records.
- Uninstalling the app permanently deletes all local Room databases, DataStore preferences, and cache files.

---

## 3. Permissions & Storage Access

Deepak Video Player requests only the minimum Android permissions needed for the features you use:

| Permission | Purpose | When Used |
|---|---|---|
| `READ_MEDIA_VIDEO` (Android 13+) / `READ_EXTERNAL_STORAGE` (Android 12 & below) | Discover local video files via Android `MediaStore` | Only when you tap **Scan Storage** |
| Zero-Permission Storage Access Framework (SAF) | Open individual videos, external `.srt`/`.vtt`/`.ass`/`.ssa` subtitles, or mount SD/USB folders | When you tap **Browse Files** or **Mount Folder** |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS` | Keep audio/video controls active in the system media notification & lock screen | Only when background playback is enabled |
| `INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK` | Keep screen awake during video playback, stream user-entered URLs, and load consent/ads when enabled | Active during playback, streaming, or ad loading |

---

## 4. File Management & Sharing

- **Rename / Delete:** Modifying or deleting local video files uses Android Scoped Storage user-confirmation prompts (`MediaStore`) or the SAF tree permissions you explicitly granted.
- **Share:** Sharing a video uses the Android system Share Sheet (`ACTION_SEND`) and passes the selected video only to the third-party app you choose.

---

## 5. Network Video Streams

When you manually enter a stream URL (`http://`, `https://`, `rtsp://`, HLS, or DASH), ExoPlayer connects directly from your device to the host you entered. That remote server may log your IP address and standard network request headers under its own privacy policy. Recently entered stream URLs are saved locally for your convenience and can be deleted individually inside the app.

---

## 6. Advertising & Consent (Google AdMob & UMP)

When production Google AdMob unit IDs are enabled in a release build:
- **Google Mobile Ads SDK (AdMob)** is used to display Banner ads, frequency-capped Interstitial ads between player sessions, and optional Rewarded ads.
- Google and its advertising partners may process device advertising IDs, IP address, diagnostics, and ad interaction metrics as described in [Google's Privacy Policy](https://policies.google.com/privacy).
- **Google User Messaging Platform (UMP):** In regions where consent is required (such as the EEA/UK), UMP displays a consent form before ads are requested. You can update your choices anytime from **Settings → Ad Privacy & Consent Options**.

*(Note: Builds using default test AdMob IDs automatically disable ad requests at runtime.)*

---

## 7. Children's Privacy

Deepak Video Player does not knowingly collect personal information from children under the age of 13 (or the applicable age of digital consent in your jurisdiction). Because the app operates without accounts or developer-hosted user profiles, no personal profile data is collected by us.

---

## 8. Data Security & System Backups

All app databases and preferences reside in Android's private application sandbox. Device-level backups (`allowBackup`) operate solely according to your device's Android backup settings and Google Drive device backup preferences.

---

## 9. Changes to This Privacy Policy

We may update this Privacy Policy to reflect new features, Android permission changes, or ad/consent updates. Any changes will be reflected in the in-app **Privacy Policy** screen and in this document with an updated **Effective Date**.

---

## 10. Contact

If you have any questions about this Privacy Policy or Deepak Video Player, please contact the developer using the email address published on the **Deepak Video Player** Google Play Store listing.
