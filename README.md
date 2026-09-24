# VibeArc

Native Android music-player demo built with Kotlin, Jetpack Compose, Material 3, and Media3.

## Run

Open this folder in Android Studio and run the `app` configuration, or use:

```powershell
.\gradlew.bat assembleDebug
```

The demo works without network access or accounts. It includes a synthetic sample and can open audio through Android's system file picker.

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

Next milestone: Audius-powered online music search and streaming in v0.7.
