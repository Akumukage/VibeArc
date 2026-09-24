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
)

internal object LibraryCodec {
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun encode(tracks: List<Track>): String = tracks.joinToString("\n") { track ->
        listOf(track.title, track.artist, track.album, track.uri)
            .joinToString("|") { encoder.encodeToString(it.toByteArray(UTF_8)) } +
            if (track.isFavorite) "|1" else "|0"
    }

    fun decode(value: String): List<Track> = value.lineSequence().mapNotNull { row ->
        val fields = row.split('|')
        if (fields.size != 5) return@mapNotNull null
        runCatching {
            Track(
                title = String(decoder.decode(fields[0]), UTF_8),
                artist = String(decoder.decode(fields[1]), UTF_8),
                album = String(decoder.decode(fields[2]), UTF_8),
                uri = String(decoder.decode(fields[3]), UTF_8),
                isFavorite = fields[4] == "1",
            )
        }.getOrNull()
    }.toList()
}

private const val PreferencesName = "vibearc_library"
private const val TracksKey = "tracks"

internal fun Context.loadLibrary(): List<Track> = LibraryCodec.decode(
    getSharedPreferences(PreferencesName, Context.MODE_PRIVATE).getString(TracksKey, "").orEmpty(),
)

internal fun Context.saveLibrary(tracks: List<Track>) {
    getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .edit()
        .putString(TracksKey, LibraryCodec.encode(tracks))
        .apply()
}

internal fun List<Track>.upsert(track: Track): List<Track> {
    val favorite = firstOrNull { it.uri == track.uri }?.isFavorite ?: track.isFavorite
    return filterNot { it.uri == track.uri } + track.copy(isFavorite = favorite)
}

internal fun List<Track>.toggleFavorite(uri: String): List<Track> = map { track ->
    if (track.uri == uri) track.copy(isFavorite = !track.isFavorite) else track
}
