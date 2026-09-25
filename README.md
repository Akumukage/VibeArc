# VibeArc

Native Android music-player demo built with Kotlin, Jetpack Compose, Material 3, and Media3.

## Run

Open this folder in Android Studio and run the `app` configuration, or use:

```powershell
.\gradlew.bat assembleDebug
```

The local player works without network access or accounts. v0.7 also includes
experimental, account-free search and playback for supported public YouTube
Music results; that feature requires a network connection and is not guaranteed
for protected, restricted, or unavailable tracks.

## Current scope

- Warm, artwork-led Material 3 interface and matching launcher icon
- Home, Search, Library, Now Playing, and Settings destinations
- Bundled demo track plus playback of a user-selected audio file
- Background playback, media notification, lock-screen controls, seek, and mini-player
- Persistent multi-track library, search, and favorites
- Native title, artist, album, duration, and embedded artwork metadata
- Artist, album, and source-folder library browsing
- Persistent playlists with create, rename, delete, add, and remove actions
- Playback queue with previous/next, shuffle, repeat, and recently played history
- Background-safe sleep timer controls
- Midnight, Peach, and Mono launcher-icon choices
- Direct InnerTube search with a bounded NewPipe search fallback
- On-demand public-stream resolution through NewPipeExtractor
- Loading, empty, unavailable, and offline-friendly search states

Current milestone: v0.7.0-demo is implemented locally. It remains an
experimental GitHub-build candidate and is not considered a Google Play-safe
production integration. NewPipeExtractor is GPLv3-or-later, so do not distribute
this build until VibeArc adopts a compatible license and publishes corresponding
source. See [`tasks/plan.md`](tasks/plan.md) for the limits.
