package com.vibearc.app

import android.content.Context
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64

internal data class Track(
    val title: String,
    val artist: String,
    val album: String,
    val uri: String = "",
    val isFavorite: Boolean = false,
    val durationMs: Long = 0,
    val artworkUri: String = "",
    val folder: String = "Imported",
)

internal data class Playlist(
    val id: String,
    val name: String,
    val trackUris: List<String> = emptyList(),
)

internal object LibraryCodec {
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun encode(tracks: List<Track>): String = tracks.joinToString("\n") { track ->
        listOf(track.title, track.artist, track.album, track.uri)
            .joinToString("|") { encoder.encodeToString(it.toByteArray(UTF_8)) } +
            (if (track.isFavorite) "|1" else "|0") +
            "|${track.durationMs}|${encoder.encodeToString(track.artworkUri.toByteArray(UTF_8))}" +
            "|${encoder.encodeToString(track.folder.toByteArray(UTF_8))}"
    }

    fun decode(value: String): List<Track> = value.lineSequence().mapNotNull { row ->
        val fields = row.split('|')
        if (fields.size !in setOf(5, 7, 8)) return@mapNotNull null
        runCatching {
            Track(
                title = String(decoder.decode(fields[0]), UTF_8),
                artist = String(decoder.decode(fields[1]), UTF_8),
                album = String(decoder.decode(fields[2]), UTF_8),
                uri = String(decoder.decode(fields[3]), UTF_8),
                isFavorite = fields[4] == "1",
                durationMs = fields.getOrNull(5)?.toLong() ?: 0,
                artworkUri = fields.getOrNull(6)?.let { String(decoder.decode(it), UTF_8) }.orEmpty(),
                folder = fields.getOrNull(7)?.let { String(decoder.decode(it), UTF_8) } ?: "Imported",
            )
        }.getOrNull()
    }.toList()
}

internal fun displayFolderFromPath(path: String?): String {
    val relativePath = path?.substringAfter(':', missingDelimiterValue = "").orEmpty().trim('/')
    return relativePath.substringBeforeLast('/', missingDelimiterValue = "")
        .substringAfterLast('/')
        .ifBlank { "Imported" }
}

internal object PlaylistCodec {
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun encode(playlists: List<Playlist>): String = playlists.joinToString("\n") { playlist ->
        (listOf(playlist.id, playlist.name) + playlist.trackUris)
            .joinToString("|") { encoder.encodeToString(it.toByteArray(UTF_8)) }
    }

    fun decode(value: String): List<Playlist> = value.lineSequence().mapNotNull { row ->
        val fields = row.split('|')
        if (fields.size < 2) return@mapNotNull null
        runCatching {
            Playlist(
                id = String(decoder.decode(fields[0]), UTF_8),
                name = String(decoder.decode(fields[1]), UTF_8),
                trackUris = fields.drop(2).map { String(decoder.decode(it), UTF_8) },
            )
        }.getOrNull()
    }.toList()
}

private const val PreferencesName = "vibearc_library"
private const val TracksKey = "tracks"
private const val PlaylistsKey = "playlists"

internal fun Context.loadLibrary(): List<Track> = LibraryCodec.decode(
    getSharedPreferences(PreferencesName, Context.MODE_PRIVATE).getString(TracksKey, "").orEmpty(),
)

internal fun Context.saveLibrary(tracks: List<Track>) {
    getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .edit()
        .putString(TracksKey, LibraryCodec.encode(tracks))
        .apply()
}

internal fun Context.loadPlaylists(): List<Playlist> = PlaylistCodec.decode(
    getSharedPreferences(PreferencesName, Context.MODE_PRIVATE).getString(PlaylistsKey, "").orEmpty(),
)

internal fun Context.savePlaylists(playlists: List<Playlist>) {
    getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .edit()
        .putString(PlaylistsKey, PlaylistCodec.encode(playlists))
        .apply()
}

internal fun List<Track>.upsert(track: Track): List<Track> {
    val favorite = firstOrNull { it.uri == track.uri }?.isFavorite ?: track.isFavorite
    return filterNot { it.uri == track.uri } + track.copy(isFavorite = favorite)
}

internal fun List<Track>.toggleFavorite(uri: String): List<Track> = map { track ->
    if (track.uri == uri) track.copy(isFavorite = !track.isFavorite) else track
}

internal fun List<Playlist>.createPlaylist(name: String, id: String): List<Playlist> {
    require(id.isNotBlank()) { "Playlist id cannot be blank" }
    val trimmedName = name.trim()
    require(trimmedName.isNotEmpty()) { "Playlist name cannot be blank" }
    return if (any { it.id == id }) this else this + Playlist(id, trimmedName)
}

internal fun List<Playlist>.renamePlaylist(id: String, name: String): List<Playlist> {
    val trimmedName = name.trim()
    require(trimmedName.isNotEmpty()) { "Playlist name cannot be blank" }
    return map { if (it.id == id) it.copy(name = trimmedName) else it }
}

internal fun List<Playlist>.deletePlaylist(id: String): List<Playlist> = filterNot { it.id == id }

internal fun List<Playlist>.addTrackToPlaylist(playlistId: String, trackUri: String): List<Playlist> {
    require(trackUri.isNotBlank()) { "Track URI cannot be blank" }
    return map { playlist ->
        if (playlist.id == playlistId && trackUri !in playlist.trackUris) {
            playlist.copy(trackUris = playlist.trackUris + trackUri)
        } else {
            playlist
        }
    }
}

internal fun List<Playlist>.removeTrackFromPlaylist(playlistId: String, trackUri: String): List<Playlist> =
    map { playlist ->
        if (playlist.id == playlistId) playlist.copy(trackUris = playlist.trackUris.filterNot { it == trackUri })
        else playlist
    }
