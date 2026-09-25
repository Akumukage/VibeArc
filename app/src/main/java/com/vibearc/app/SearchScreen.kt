package com.vibearc.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun SearchScreen(padding: PaddingValues, tracks: List<Track>, onPlay: (Track) -> Unit) {
    var query by remember { mutableStateOf("") }
    var onlineResults by remember { mutableStateOf(emptyList<Track>()) }
    var onlineError by remember { mutableStateOf<String?>(null) }
    var searching by remember { mutableStateOf(false) }
    var resolvingUri by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val localResults = tracks.filter { track ->
        query.isBlank() || listOf(track.title, track.artist, track.album).any { it.contains(query, true) }
    }
    val searchOnline: () -> Unit = search@{
        if (searching) return@search
        val requestedQuery = query.trim()
        if (requestedQuery.isBlank()) {
            onlineError = "Enter a song, artist, or album first."
        } else {
            scope.launch {
                searching = true
                hasSearched = true
                onlineError = null
                try {
                    onlineResults = withContext(Dispatchers.IO) { OnlineMusic.search(requestedQuery) }
                } catch (_: Exception) {
                    onlineResults = emptyList()
                    onlineError = "Online search is unavailable right now. Try again later."
                } finally {
                    searching = false
                }
            }
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { Text("Find your sound", style = MaterialTheme.typography.headlineMedium) }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tracks, artists, albums") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { searchOnline() }),
            )
        }
        item {
            Button(onClick = searchOnline, enabled = !searching, modifier = Modifier.fillMaxWidth()) {
                Text(if (searching) "Searching…" else "Search YouTube Music")
            }
        }
        item {
            Text(
                "Experimental public results. Some protected or restricted tracks cannot play.",
                color = MutedText,
                fontSize = 13.sp,
            )
        }
        if (searching) item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                Text("Searching the public catalog…", color = MutedText)
            }
        }
        onlineError?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
        if (hasSearched && !searching && onlineResults.isEmpty() && onlineError == null) {
            item { Text("No public tracks found for “$query”.", color = MutedText) }
        }
        if (onlineResults.isNotEmpty()) item { SectionTitle("Online") }
        items(onlineResults, key = { "online:${it.uri}" }) { track ->
            TrackRow(track, onPlay = {
                if (resolvingUri == null) {
                    resolvingUri = track.uri
                    scope.launch {
                        try {
                            onPlay(withContext(Dispatchers.IO) { OnlineMusic.resolve(track) })
                        } catch (_: Exception) {
                            onlineError = "${track.title} is not playable from this connection."
                        } finally {
                            resolvingUri = null
                        }
                    }
                }
            })
            if (resolvingUri == track.uri) Text("Preparing audio…", color = Sand, fontSize = 12.sp)
        }
        if (localResults.isNotEmpty()) item { SectionTitle("On this device") }
        if (localResults.isEmpty() && onlineResults.isEmpty() && query.isNotBlank() && !searching) {
            item { Text("No local tracks match “$query”.", color = MutedText) }
        }
        items(localResults, key = { "local:${it.uri.ifBlank { "demo" }}" }) { track ->
            TrackRow(track, onPlay = { onPlay(track) })
        }
    }
}
