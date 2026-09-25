package com.vibearc.app

import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64

internal const val DemoMediaId = "vibearc://demo"

private const val RecentLimit = 20

internal fun playbackQueue(tracks: List<Track>, startTrack: Track): List<Track> {
    val queue = tracks.ifEmpty { listOf(startTrack) }
    return if (queue.any { it.uri == startTrack.uri }) queue else listOf(startTrack) + queue
}

internal fun List<String>.recordRecentUri(uri: String): List<String> {
    require(uri.isNotBlank())
    return (listOf(uri) + filterNot { it == uri }).take(RecentLimit)
}

internal object RecentUriCodec {
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun encode(uris: List<String>): String = uris.joinToString("\n") {
        encoder.encodeToString(it.toByteArray(UTF_8))
    }

    fun decode(value: String): List<String> = value.lineSequence().mapNotNull { row ->
        if (row.isBlank()) return@mapNotNull null
        runCatching { String(decoder.decode(row), UTF_8) }.getOrNull()
    }.toList()
}
