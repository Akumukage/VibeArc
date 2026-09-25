package com.vibearc.app

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

@Composable
internal fun TrackArtwork(track: Track, contentDescription: String?, modifier: Modifier = Modifier) {
    var artwork by remember(track.artworkUri) {
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    }
    LaunchedEffect(track.artworkUri) {
        artwork = track.artworkUri.takeIf(String::isNotBlank)?.let { artworkUri ->
            withContext(Dispatchers.IO) { loadArtwork(artworkUri)?.asImageBitmap() }
        }
    }
    val loadedArtwork = artwork
    if (loadedArtwork == null) {
        Box(
            modifier = modifier.background(
                Brush.linearGradient(listOf(Color(0xFF4A3426), Color(0xFF181513))),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize().padding(8.dp),
                contentScale = ContentScale.Fit,
            )
        }
    } else {
        Image(
            bitmap = loadedArtwork,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    }
}

private fun loadArtwork(value: String) = runCatching {
    val uri = Uri.parse(value)
    if (uri.scheme == "http" || uri.scheme == "https") {
        val connection = URL(value).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("User-Agent", "VibeArc/0.7")
            connection.inputStream.use(BitmapFactory::decodeStream)
        } finally {
            connection.disconnect()
        }
    } else {
        BitmapFactory.decodeFile(uri.path)
    }
}.getOrNull()

internal fun Context.trackFrom(uri: Uri): Track {
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
                folder = displayFolderFromPath(uri.path),
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
