# VibeArc

Native Android music-player demo built with Kotlin, Jetpack Compose, Material 3, and Media3.

## Run

Open this folder in Android Studio and run the `app` configuration, or use:

```powershell
.\gradlew.bat assembleDebug
```

The demo works without network access or accounts. It includes a synthetic sample and can open audio through Android's system file picker.

## Current scope

- Branded launcher icon and dark Material 3 interface
- Home, Search, Library, and Now Playing destinations
- Bundled demo track plus playback of a user-selected audio file
- Background playback, media notification, lock-screen controls, seek, and mini-player
- Persistent multi-track library, search, and favorites
- Native title, artist, album, duration, and embedded artwork metadata
- Persistent playlists with create, rename, delete, add, and remove actions
- Playback queue with previous/next, shuffle, repeat, and recently played history
- Background-safe sleep timer controls

Next milestone: browse by artist, album, and folder with richer sorting.
