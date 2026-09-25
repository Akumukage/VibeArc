package com.vibearc.app

import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.MUSIC_SONGS
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.net.HttpURLConnection
import java.net.URL

internal data class AudioCandidate(val url: String, val bitrate: Int)

internal fun selectAudioUrl(candidates: List<AudioCandidate>): String? =
    candidates.filter { it.url.isNotBlank() }.maxByOrNull(AudioCandidate::bitrate)?.url

internal object OnlineMusic {
    private val youtube = ServiceList.YouTube

    init {
        NewPipe.init(ExtractorDownloader)
    }

    fun search(query: String): List<Track> {
        val extractor = youtube.getSearchExtractor(query.trim(), listOf(MUSIC_SONGS), "")
        extractor.fetchPage()
        return extractor.initialPage.items
            .filterIsInstance<StreamInfoItem>()
            .take(20)
            .map { item ->
                Track(
                    title = item.name,
                    artist = item.uploaderName?.takeIf(String::isNotBlank) ?: "YouTube Music",
                    album = "Online",
                    uri = item.url,
                    durationMs = item.duration.coerceAtLeast(0) * 1_000,
                    folder = "YouTube Music",
                )
            }
    }

    fun resolve(track: Track): Track {
        val info = StreamInfo.getInfo(youtube, track.uri)
        val streamUrl = selectAudioUrl(
            info.audioStreams.map { stream ->
                AudioCandidate(stream.content.takeIf { stream.isUrl }.orEmpty(), stream.averageBitrate)
            },
        ) ?: error("No playable public audio stream is available for this track.")
        return track.copy(uri = streamUrl)
    }
}

private object ExtractorDownloader : Downloader() {
    private const val UserAgent =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36"
    override fun execute(request: Request): Response {
        val connection = URL(request.url()).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = request.httpMethod()
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("User-Agent", UserAgent)
            request.headers().forEach { (name, values) ->
                connection.setRequestProperty(name, values.joinToString(", "))
            }
            request.dataToSend()?.let { data ->
                connection.doOutput = true
                connection.outputStream.use { it.write(data) }
            }
            val code = connection.responseCode
            if (code == 429) throw ReCaptchaException("YouTube requested verification", request.url())
            val body = if (request.httpMethod() == "HEAD") "" else {
                (if (code >= 400) connection.errorStream else connection.inputStream)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()
            }
            val headers = connection.headerFields
                .filterKeys { it != null }
                .mapKeys { it.key!! }
            return Response(code, connection.responseMessage, headers, body, connection.url.toString())
        } finally {
            connection.disconnect()
        }
    }
}
