package com.vibearc.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    if (activePlayer.mediaItemCount == 0) activePlayer.load(demoTrack, demoUri, false)

    var currentTab by remember { mutableStateOf(Tab.Home) }
    var currentTrack by remember { mutableStateOf(activePlayer.currentMediaItem?.track ?: demoTrack) }
    var isPlaying by remember { mutableStateOf(activePlayer.isPlaying) }

    DisposableEffect(activePlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) {
                isPlaying = value
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.let { currentTrack = it.track }
            }
        }
        activePlayer.addListener(listener)
        onDispose { activePlayer.removeListener(listener) }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        currentTrack = context.trackFrom(uri)
        activePlayer.load(currentTrack, uri)
        currentTab = Tab.Player
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
        val playDemo = {
            currentTrack = demoTrack
            activePlayer.load(demoTrack, demoUri)
            currentTab = Tab.Player
        }
        when (currentTab) {
            Tab.Home -> HomeScreen(padding, playDemo)
            Tab.Search -> SearchScreen(padding, currentTrack) {
                activePlayer.play()
                currentTab = Tab.Player
            }
            Tab.Library -> LibraryScreen(
                padding = padding,
                track = currentTrack,
                onChooseFile = { filePicker.launch(arrayOf("audio/*")) },
                onPlay = {
                    activePlayer.play()
                    currentTab = Tab.Player
                },
            )
            Tab.Player -> PlayerScreen(padding, activePlayer, currentTrack, isPlaying)
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
private fun HomeScreen(padding: PaddingValues, onPlay: () -> Unit) {
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
                    Button(onClick = onPlay) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Play demo")
                    }
                }
            }
        }
        item { SectionTitle("Made for this build") }
        item { TrackRow(demoTrack, onPlay) }
        item { SectionTitle("Coming next") }
        items(listOf("Saved library", "Synced lyrics", "Smart mixes")) { feature ->
            Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
                Text(feature, Modifier.fillMaxWidth().padding(18.dp), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SearchScreen(padding: PaddingValues, track: Track, onPlay: () -> Unit) {
    var query by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().padding(padding).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("Search", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Tracks, artists, albums") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
        )
        if (query.isBlank() || listOf(track.title, track.artist, track.album).any { it.contains(query, true) }) {
            TrackRow(track, onPlay)
        } else {
            Text("No tracks match “$query”.", color = Color.LightGray)
        }
    }
}

@Composable
private fun LibraryScreen(
    padding: PaddingValues,
    track: Track,
    onChooseFile: () -> Unit,
    onPlay: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(padding).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("Library", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Button(onClick = onChooseFile, modifier = Modifier.fillMaxWidth()) {
            Text("Choose audio file")
        }
        Text("Now loaded", color = Color.LightGray)
        TrackRow(track, onPlay)
        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = Lime)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Favorites", fontWeight = FontWeight.Bold)
                    Text("Saved favorites arrive in the next milestone", color = Color.LightGray)
                }
            }
        }
    }
}

@Composable
private fun PlayerScreen(
    padding: PaddingValues,
    player: Player,
    track: Track,
    isPlaying: Boolean,
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

    Column(
        Modifier.fillMaxSize().padding(padding).padding(horizontal = 28.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.vibearc_icon),
            contentDescription = "VibeArc artwork",
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)),
            contentScale = ContentScale.FillWidth,
        )
        Spacer(Modifier.height(28.dp))
        Text(track.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(track.artist, color = Cyan)
        Spacer(Modifier.height(20.dp))
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
        Spacer(Modifier.height(12.dp))
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
    }
}

@Composable
private fun MiniPlayer(track: Track, isPlaying: Boolean, onOpen: () -> Unit, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF1A2238)).clickable(onClick = onOpen).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.vibearc_icon),
            contentDescription = null,
            modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)),
        )
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
private fun TrackRow(track: Track, onPlay: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onPlay).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.vibearc_icon),
            contentDescription = null,
            modifier = Modifier.size(58.dp).clip(RoundedCornerShape(16.dp)),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, fontWeight = FontWeight.Bold)
            Text("${track.artist} • ${track.album}", color = Color.LightGray)
        }
        Icon(Icons.Default.PlayArrow, contentDescription = "Play ${track.title}", tint = Lime)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

private fun Player.load(track: Track, uri: Uri, playNow: Boolean = true) {
    setMediaItem(
        MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .build(),
            )
            .build(),
    )
    prepare()
    if (playNow) play()
}

private fun Player.toggle() = if (isPlaying) pause() else play()

private val MediaItem.track: Track
    get() = Track(
        title = mediaMetadata.title?.toString() ?: "Unknown track",
        artist = mediaMetadata.artist?.toString() ?: "On this device",
        album = mediaMetadata.albumTitle?.toString() ?: "Imported",
    )

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
    return Track(fileName.substringBeforeLast('.').ifBlank { "Local audio" }, "On this device", "Imported")
}
