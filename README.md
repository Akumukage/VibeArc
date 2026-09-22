# VibeArc

Native Android music-player demo built with Kotlin, Jetpack Compose, Material 3, and Media3.

## Run

Open this folder in Android Studio and run the `app` configuration, or use:

```powershell
.\gradlew.bat assembleDebug
```

The demo uses one bundled synthetic audio sample, so it works without network access or accounts.

## Current scope

- Branded launcher icon and dark Material 3 interface
- Home, Search, Library, and Now Playing destinations
- One playable bundled track with play, pause, seek, and a mini-player

Next milestone: move playback into a `MediaSessionService` and add Android's system file picker for user-owned music.
