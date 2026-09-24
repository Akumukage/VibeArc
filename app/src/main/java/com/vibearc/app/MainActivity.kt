package com.vibearc.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.format.DateUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.util.UUID

private val Midnight = Color(0xFF080B14)
private val Surface = Color(0xFF111729)
private val Lime = Color(0xFFC8FF00)
private val Cyan = Color(0xFF1DE9D3)

private val demoTrack = Track("First Light", "VibeArc Demo", "Signals")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VibeArcTheme { VibeArcApp() } }
    }
}

@Composable
private fun VibeArcTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Lime,
            secondary = Cyan,
            background = Midnight,
            surface = Surface,
            onPrimary = Color(0xFF142000),
        ),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VibeArcApp() {
    val context = LocalContext.current
    val controllerFuture = remember {
        MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, PlaybackService::class.java)),
        ).buildAsync()
    }
    var player by remember { mutableStateOf<Player?>(null) }

    DisposableEffect(controllerFuture) {
        controllerFuture.addListener(
            { player = controllerFuture.get() },
            ContextCompat.getMainExecutor(context),
        )
        onDispose {
            player = null
            MediaController.releaseFuture(controllerFuture)
        }
    }

    val activePlayer = player
    if (activePlayer == null) {
        Box(Modifier.fillMaxSize().background(Midnight), contentAlignment = Alignment.Center) {
            Text("Starting VibeArc…", color = Color.White)
        }
        return
    }

    val demoUri = remember { Uri.parse("android.resource://${context.packageName}/${R.raw.vibearc_demo}") }
    if (activePlayer.mediaItemCount == 0) {
        activePlayer.loadQueue(listOf(demoTrack), demoTrack, demoUri, playNow = false)
    }

    var currentTab by remember { mutableStateOf(Tab.Home) }
    var library by remember { mutableStateOf(context.loadLibrary()) }
    var playlists by remember { mutableStateOf(context.loadPlaylists()) }
    var recentUris by remember { mutableStateOf(context.loadRecentUris()) }
    val restoredTrack = activePlayer.currentMediaItem?.track
    var currentTrack by remember {
        mutableStateOf(library.firstOrNull { it.uri == restoredTrack?.uri } ?: restoredTrack ?: demoTrack)
    }
    var isPlaying by remember { mutableStateOf(activePlayer.isPlaying) }
    var queueTracks by remember { mutableStateOf(activePlayer.queueTracks()) }
    var shuffleEnabled by remember { mutableStateOf(activePlayer.shuffleModeEnabled) }
    var playerRepeatMode by remember { mutableIntStateOf(activePlayer.repeatMode) }
    var sleepRemainingMillis by remember { mutableLongStateOf(0L) }

    DisposableEffect(activePlayer, library) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) {
                isPlaying = value
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.track?.let { track ->
                    currentTrack = library.firstOrNull { it.uri == track.uri } ?: track
                    recentUris = recentUris.recordRecentUri(mediaItem.mediaId)
                }
                queueTracks = activePlayer.queueTracks()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                shuffleEnabled = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                playerRepeatMode = repeatMode
            }
        }
        activePlayer.addListener(listener)
        onDispose { activePlayer.removeListener(listener) }
    }

    LaunchedEffect(context) {
        while (currentCoroutineContext().isActive) {
            val deadline = context.loadSleepDeadlineMillis()
            sleepRemainingMillis = deadline?.minus(System.currentTimeMillis())?.coerceAtLeast(0L) ?: 0L
            delay(1_000)
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val imported = context.trackFrom(uri)
        library = library.upsert(imported).also(context::saveLibrary)
        currentTrack = library.first { it.uri == imported.uri }
        activePlayer.loadQueue(library, currentTrack, demoUri)
        queueTracks = activePlayer.queueTracks()
        currentTab = Tab.Player
    }

    val playTrack: (Track, List<Track>) -> Unit = { track, source ->
        currentTrack = track
        activePlayer.loadQueue(source, track, demoUri)
        queueTracks = activePlayer.queueTracks()
        currentTab = Tab.Player
    }
    val toggleFavorite: (Track) -> Unit = { track ->
        library = library.toggleFavorite(track.uri).also(context::saveLibrary)
        library.firstOrNull { it.uri == track.uri }?.let { currentTrack = it }
    }
    val updatePlaylists: (List<Playlist>) -> Unit = { next ->
        playlists = next.also(context::savePlaylists)
    }
    val recentTracks = recentUris.mapNotNull { mediaId ->
        if (mediaId == DemoMediaId) demoTrack else library.firstOrNull { it.uri == mediaId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("VibeArc", fontWeight = FontWeight.Black)
                        Text("Music that follows your rhythm", fontSize = 12.sp, color = Cyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Midnight),
            )
        },
        bottomBar = {
            Column {
                if (currentTab != Tab.Player) {
                    MiniPlayer(
                        track = currentTrack,
                        isPlaying = isPlaying,
                        onOpen = { currentTab = Tab.Player },
                        onToggle = activePlayer::toggle,
                    )
                }
                NavigationBar(containerColor = Surface) {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
        containerColor = Midnight,
    ) { padding ->
        when (currentTab) {
            Tab.Home -> HomeScreen(
                padding = padding,
                recentTracks = recentTracks,
                onPlay = { track -> playTrack(track, recentTracks.ifEmpty { listOf(demoTrack) }) },
                onPlayDemo = { playTrack(demoTrack, listOf(demoTrack) + library) },
            )
            Tab.Search -> {
                val searchableTracks = listOf(demoTrack) + library
                SearchScreen(padding, searchableTracks) { track -> playTrack(track, searchableTracks) }
            }
            Tab.Library -> LibraryScreen(
                padding = padding,
                tracks = library,
                playlists = playlists,
                onChooseFile = { filePicker.launch(arrayOf("audio/*")) },
                onPlay = playTrack,
                onToggleFavorite = toggleFavorite,
                onCreatePlaylist = { name ->
                    updatePlaylists(playlists.createPlaylist(name, UUID.randomUUID().toString()))
                },
                onRenamePlaylist = { id, name -> updatePlaylists(playlists.renamePlaylist(id, name)) },
                onDeletePlaylist = { id -> updatePlaylists(playlists.deletePlaylist(id)) },
                onAddToPlaylist = { id, uri -> updatePlaylists(playlists.addTrackToPlaylist(id, uri)) },
                onRemoveFromPlaylist = { id, uri -> updatePlaylists(playlists.removeTrackFromPlaylist(id, uri)) },
            )
            Tab.Player -> PlayerScreen(
                padding = padding,
                player = activePlayer,
                track = currentTrack,
                isPlaying = isPlaying,
                queue = queueTracks,
                shuffleEnabled = shuffleEnabled,
                repeatMode = playerRepeatMode,
                sleepRemainingMillis = sleepRemainingMillis,
                onFavorite = if (currentTrack.uri.isBlank()) null else { { toggleFavorite(currentTrack) } },
                onToggleShuffle = { activePlayer.shuffleModeEnabled = !activePlayer.shuffleModeEnabled },
                onCycleRepeat = { activePlayer.repeatMode = activePlayer.repeatMode.nextRepeatMode() },
                onCycleSleepTimer = {
                    val nextMinutes = when {
                        sleepRemainingMillis == 0L -> 15
                        sleepRemainingMillis <= 15 * 60_000L -> 30
                        sleepRemainingMillis <= 30 * 60_000L -> 60
                        else -> 0
                    }
                    context.saveSleepDeadlineMillis(
                        nextMinutes.takeIf { it > 0 }?.let { System.currentTimeMillis() + it * 60_000L },
                    )
                },
            )
        }
    }
}

private enum class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Home("Home", Icons.Default.Home),
    Search("Search", Icons.Default.Search),
    Library("Library", Icons.AutoMirrored.Filled.List),
    Player("Playing", Icons.Default.PlayArrow),
}

@Composable
private fun HomeScreen(
    padding: PaddingValues,
    recentTracks: List<Track>,
    onPlay: (Track) -> Unit,
    onPlayDemo: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text("Good evening", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Play the demo or choose your own audio from Library.", color = Color.LightGray)
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().background(
                    Brush.linearGradient(listOf(Color(0xFF26386F), Color(0xFF123A3A))),
                    RoundedCornerShape(28.dp),
                ),
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("DEMO SIGNAL", color = Lime, fontWeight = FontWeight.Bold)
                    Text("First Light", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                    Text("A bundled, original tone sequence for testing the player.")
                    Button(onClick = onPlayDemo) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Play demo")
                    }
                }
            }
        }
        item { SectionTitle("Made for this build") }
        item { TrackRow(demoTrack, onPlayDemo) }
        if (recentTracks.isNotEmpty()) {
            item { SectionTitle("Recently played") }
            items(recentTracks, key = { it.uri.ifBlank { DemoMediaId } }) { track ->
                TrackRow(track, onPlay = { onPlay(track) })
            }
        }
    }
}

@Composable
private fun SearchScreen(padding: PaddingValues, tracks: List<Track>, onPlay: (Track) -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = tracks.filter { track ->
        query.isBlank() || listOf(track.title, track.artist, track.album).any { it.contains(query, true) }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { Text("Search", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tracks, artists, albums") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )
        }
        if (results.isEmpty()) item { Text("No tracks match “$query”.", color = Color.LightGray) }
        items(results, key = { it.uri.ifBlank { "demo" } }) { track ->
            TrackRow(track, onPlay = { onPlay(track) })
        }
    }
}

@Composable
private fun LibraryScreen(
    padding: PaddingValues,
    tracks: List<Track>,
    playlists: List<Playlist>,
    onChooseFile: () -> Unit,
    onPlay: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onAddToPlaylist: (String, String) -> Unit,
    onRemoveFromPlaylist: (String, String) -> Unit,
) {
    var mode by remember { mutableStateOf(LibraryMode.Tracks) }
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var trackToAdd by remember { mutableStateOf<Track?>(null) }
    val visibleTracks = if (mode == LibraryMode.Favorites) tracks.filter(Track::isFavorite) else tracks
    val selectedPlaylist = playlists.firstOrNull { it.id == selectedPlaylistId }
    val playlistTracks = selectedPlaylist?.trackUris.orEmpty().mapNotNull { uri -> tracks.firstOrNull { it.uri == uri } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { Text("Library", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item {
            Button(onClick = onChooseFile, modifier = Modifier.fillMaxWidth()) {
                Text("Add audio file")
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LibraryMode.entries.forEach { option ->
                    FilterChip(
                        selected = mode == option,
                        onClick = {
                            mode = option
                            if (option != LibraryMode.Playlists) selectedPlaylistId = null
                        },
                        label = {
                            Text(
                                when (option) {
                                    LibraryMode.Tracks -> "Tracks"
                                    LibraryMode.Favorites -> "Favorites (${tracks.count(Track::isFavorite)})"
                                    LibraryMode.Playlists -> "Playlists (${playlists.size})"
                                },
                            )
                        },
                    )
                }
            }
        }

        if (mode == LibraryMode.Playlists) {
            if (selectedPlaylist == null) {
                item {
                    Button(onClick = { showCreateDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create playlist")
                    }
                }
                if (playlists.isEmpty()) {
                    item { EmptyLibraryCard("No playlists yet", "Create one to arrange tracks for any mood.") }
                }
                items(playlists, key = Playlist::id) { playlist ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        modifier = Modifier.fillMaxWidth().clickable { selectedPlaylistId = playlist.id },
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text(playlist.name, fontWeight = FontWeight.Bold)
                            Text("${playlist.trackUris.size} tracks", color = Color.LightGray)
                        }
                    }
                }
            } else {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { selectedPlaylistId = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to playlists")
                        }
                        Text(selectedPlaylist.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        IconButton(onClick = { playlistToRename = selectedPlaylist }) {
                            Icon(Icons.Default.Edit, contentDescription = "Rename ${selectedPlaylist.name}")
                        }
                        IconButton(onClick = { playlistToDelete = selectedPlaylist }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete ${selectedPlaylist.name}")
                        }
                    }
                }
                if (playlistTracks.isEmpty()) {
                    item { EmptyLibraryCard("This playlist is empty", "Use the playlist button beside a track to add it.") }
                } else {
                    item {
                        Button(
                            onClick = { onPlay(playlistTracks.first(), playlistTracks) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Play playlist")
                        }
                    }
                    items(playlistTracks, key = Track::uri) { track ->
                        TrackRow(
                            track = track,
                            onPlay = { onPlay(track, playlistTracks) },
                            trailingIcon = Icons.Default.Delete,
                            trailingDescription = "Remove ${track.title} from ${selectedPlaylist.name}",
                            onTrailingAction = { onRemoveFromPlaylist(selectedPlaylist.id, track.uri) },
                        )
                    }
                }
            }
        } else {
            if (visibleTracks.isEmpty()) {
                item {
                    EmptyLibraryCard(
                        if (mode == LibraryMode.Favorites) "No favorites yet" else "Your library is empty",
                        if (mode == LibraryMode.Favorites) "Tap the heart beside a track to save it here."
                        else "Add an audio file to keep it in VibeArc.",
                    )
                }
            }
            items(visibleTracks, key = Track::uri) { track ->
                TrackRow(
                    track = track,
                    onPlay = { onPlay(track, visibleTracks) },
                    onFavorite = { onToggleFavorite(track) },
                    isFavorite = track.isFavorite,
                    trailingIcon = Icons.AutoMirrored.Filled.List.takeIf { playlists.isNotEmpty() },
                    trailingDescription = "Add ${track.title} to a playlist",
                    onTrailingAction = { trackToAdd = track }.takeIf { playlists.isNotEmpty() },
                )
            }
        }
    }

    if (showCreateDialog) {
        PlaylistNameDialog(
            title = "Create playlist",
            initialName = "",
            onDismiss = { showCreateDialog = false },
            onSave = {
                onCreatePlaylist(it)
                showCreateDialog = false
            },
        )
    }
    playlistToRename?.let { playlist ->
        PlaylistNameDialog(
            title = "Rename playlist",
            initialName = playlist.name,
            onDismiss = { playlistToRename = null },
            onSave = {
                onRenamePlaylist(playlist.id, it)
                playlistToRename = null
            },
        )
    }
    playlistToDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            title = { Text("Delete ${playlist.name}?") },
            text = { Text("The playlist will be removed. Your audio files stay in the library.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeletePlaylist(playlist.id)
                    selectedPlaylistId = null
                    playlistToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { playlistToDelete = null }) { Text("Cancel") } },
        )
    }
    trackToAdd?.let { track ->
        AlertDialog(
            onDismissRequest = { trackToAdd = null },
            title = { Text("Add ${track.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    playlists.forEach { playlist ->
                        TextButton(
                            onClick = {
                                onAddToPlaylist(playlist.id, track.uri)
                                trackToAdd = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(playlist.name) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { trackToAdd = null }) { Text("Cancel") } },
        )
    }
}

private enum class LibraryMode { Tracks, Favorites, Playlists }

@Composable
private fun EmptyLibraryCard(title: String, message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(message, color = Color.LightGray)
        }
    }
}

@Composable
private fun PlaylistNameDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PlayerScreen(
    padding: PaddingValues,
    player: Player,
    track: Track,
    isPlaying: Boolean,
    queue: List<Track>,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    sleepRemainingMillis: Long,
    onFavorite: (() -> Unit)?,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onCycleSleepTimer: () -> Unit,
) {
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(1L) }

    LaunchedEffect(player, isPlaying) {
        while (currentCoroutineContext().isActive) {
            position = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.coerceAtLeast(1L)
            delay(if (isPlaying) 500 else 1_000)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TrackArtwork(
                track = track,
                contentDescription = "Artwork for ${track.title}",
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)),
            )
        }
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(track.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("${track.artist} • ${track.album}", color = Cyan)
                if (onFavorite != null) {
                    IconButton(onClick = onFavorite) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = if (track.isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (track.isFavorite) Lime else Color.LightGray,
                        )
                    }
                }
            }
        }
        item {
            Slider(
                value = position.coerceAtMost(duration).toFloat(),
                onValueChange = { position = it.toLong() },
                onValueChangeFinished = { player.seekTo(position) },
                valueRange = 0f..duration.toFloat(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(DateUtils.formatElapsedTime(position / 1_000), color = Color.LightGray)
                Text(DateUtils.formatElapsedTime(duration / 1_000), color = Color.LightGray)
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = player::seekToPreviousMediaItem, enabled = player.hasPreviousMediaItem()) {
                    Text("⏮", modifier = Modifier.semantics { contentDescription = "Previous track" })
                }
                IconButton(
                    onClick = player::toggle,
                    modifier = Modifier.size(72.dp).background(Lime, RoundedCornerShape(36.dp)),
                ) {
                    Text(
                        if (isPlaying) "Ⅱ" else "▶",
                        fontSize = 32.sp,
                        color = Midnight,
                        modifier = Modifier.semantics {
                            contentDescription = if (isPlaying) "Pause" else "Play"
                        },
                    )
                }
                IconButton(onClick = player::seekToNextMediaItem, enabled = player.hasNextMediaItem()) {
                    Text("⏭", modifier = Modifier.semantics { contentDescription = "Next track" })
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FilterChip(
                    selected = shuffleEnabled,
                    onClick = onToggleShuffle,
                    label = { Text(if (shuffleEnabled) "Shuffle on" else "Shuffle off") },
                )
                FilterChip(
                    selected = repeatMode != Player.REPEAT_MODE_OFF,
                    onClick = onCycleRepeat,
                    label = { Text(repeatMode.repeatLabel()) },
                )
            }
        }
        item {
            Button(onClick = onCycleSleepTimer, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (sleepRemainingMillis == 0L) "Sleep timer off"
                    else "Sleep in ${((sleepRemainingMillis + 59_999) / 60_000)} min",
                )
            }
        }
        item { SectionTitle("Queue") }
        if (queue.isEmpty()) {
            item { Text("The queue is empty.", color = Color.LightGray) }
        } else {
            items(queue.indices.toList(), key = { index -> "$index-${queue[index].uri}" }) { index ->
                val queuedTrack = queue[index]
                TrackRow(
                    track = queuedTrack,
                    onPlay = { player.seekTo(index, 0L) },
                    trailingIcon = Icons.AutoMirrored.Filled.List.takeIf { index == player.currentMediaItemIndex },
                    trailingDescription = "Currently playing",
                )
            }
        }
    }
}

@Composable
private fun MiniPlayer(track: Track, isPlaying: Boolean, onOpen: () -> Unit, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF1A2238)).clickable(onClick = onOpen).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrackArtwork(track, null, Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, fontWeight = FontWeight.Bold)
            Text(track.artist, color = Color.LightGray, fontSize = 12.sp)
        }
        IconButton(onClick = onToggle) {
            Text(
                if (isPlaying) "Ⅱ" else "▶",
                fontSize = 24.sp,
                modifier = Modifier.semantics {
                    contentDescription = if (isPlaying) "Pause" else "Play"
                },
            )
        }
    }
}

@Composable
private fun TrackRow(
    track: Track,
    onPlay: () -> Unit,
    onFavorite: (() -> Unit)? = null,
    isFavorite: Boolean = false,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailingDescription: String = "Track action",
    onTrailingAction: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onPlay).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrackArtwork(track, null, Modifier.size(58.dp).clip(RoundedCornerShape(16.dp)))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, fontWeight = FontWeight.Bold)
            Text(
                buildString {
                    append("${track.artist} • ${track.album}")
                    if (track.durationMs > 0) append(" • ${DateUtils.formatElapsedTime(track.durationMs / 1_000)}")
                },
                color = Color.LightGray,
            )
        }
        if (onFavorite == null) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play ${track.title}", tint = Lime)
        } else {
            IconButton(onClick = onFavorite) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = if (isFavorite) "Remove ${track.title} from favorites" else "Add ${track.title} to favorites",
                    tint = if (isFavorite) Lime else Color.LightGray,
                )
            }
        }
        if (trailingIcon != null) {
            if (onTrailingAction == null) {
                Icon(trailingIcon, contentDescription = trailingDescription, tint = Lime)
            } else {
                IconButton(onClick = onTrailingAction) {
                    Icon(trailingIcon, contentDescription = trailingDescription)
                }
            }
        }
    }
}

@Composable
private fun TrackArtwork(track: Track, contentDescription: String?, modifier: Modifier = Modifier) {
    val artwork = remember(track.artworkUri) {
        track.artworkUri.takeIf(String::isNotBlank)?.let { value ->
            runCatching { BitmapFactory.decodeFile(Uri.parse(value).path)?.asImageBitmap() }.getOrNull()
        }
    }
    if (artwork == null) {
        Image(
            painter = painterResource(R.drawable.vibearc_icon),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Image(
            bitmap = artwork,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

private fun Player.loadQueue(tracks: List<Track>, startTrack: Track, demoUri: Uri, playNow: Boolean = true) {
    val queue = tracks.ifEmpty { listOf(startTrack) }
    val startIndex = queue.indexOfFirst { it.uri == startTrack.uri }.coerceAtLeast(0)
    setMediaItems(queue.map { it.toMediaItem(demoUri) }, startIndex, 0L)
    prepare()
    if (playNow) play()
}

private fun Player.toggle() = if (isPlaying) pause() else play()

private fun Player.queueTracks(): List<Track> =
    (0 until mediaItemCount).map { index -> getMediaItemAt(index).track }

private fun Track.toMediaItem(demoUri: Uri): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setExtras(Bundle().apply { putLong("durationMs", durationMs) })
        .apply {
            artworkUri.takeIf(String::isNotBlank)?.let { setArtworkUri(Uri.parse(it)) }
        }
        .build()
    return MediaItem.Builder()
        .setMediaId(uri.ifBlank { DemoMediaId })
        .setUri(if (uri.isBlank()) demoUri else Uri.parse(uri))
        .setMediaMetadata(metadata)
        .build()
}

private val MediaItem.track: Track
    get() = Track(
        title = mediaMetadata.title?.toString() ?: "Unknown track",
        artist = mediaMetadata.artist?.toString() ?: "On this device",
        album = mediaMetadata.albumTitle?.toString() ?: "Imported",
        uri = mediaId.takeUnless { it == DemoMediaId }.orEmpty(),
        durationMs = mediaMetadata.extras?.getLong("durationMs") ?: 0L,
        artworkUri = mediaMetadata.artworkUri?.toString().orEmpty(),
    )

private fun Int.nextRepeatMode(): Int = when (this) {
    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
    else -> Player.REPEAT_MODE_OFF
}

private fun Int.repeatLabel(): String = when (this) {
    Player.REPEAT_MODE_ALL -> "Repeat all"
    Player.REPEAT_MODE_ONE -> "Repeat one"
    else -> "Repeat off"
}

private fun Context.trackFrom(uri: Uri): Track {
    val fileName = contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null,
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }.orEmpty()
    val fallbackTitle = fileName.substringBeforeLast('.').ifBlank { "Local audio" }
    return runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(this, uri)
            val artworkUri = retriever.embeddedPicture
                ?.takeIf(ByteArray::isNotEmpty)
                ?.let { cacheArtwork(uri, it) }
                .orEmpty()
            Track(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?.takeIf(String::isNotBlank) ?: fallbackTitle,
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?.takeIf(String::isNotBlank) ?: "On this device",
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    ?.takeIf(String::isNotBlank) ?: "Imported",
                uri = uri.toString(),
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L,
                artworkUri = artworkUri,
            )
        } finally {
            retriever.release()
        }
    }.getOrElse {
        Track(fallbackTitle, "On this device", "Imported", uri.toString())
    }
}

private fun Context.cacheArtwork(uri: Uri, bytes: ByteArray): String {
    val directory = File(filesDir, "artwork").apply { mkdirs() }
    return File(directory, "${uri.toString().hashCode()}.image")
        .apply { writeBytes(bytes) }
        .toURI()
        .toString()
}
