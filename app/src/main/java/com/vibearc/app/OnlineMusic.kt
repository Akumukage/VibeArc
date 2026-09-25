package com.vibearc.app

import com.grack.nanojson.JsonArray
import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import com.grack.nanojson.JsonWriter
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getYoutubeMusicClientVersion
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getYoutubeMusicHeaders
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.MUSIC_SONGS
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.TimeZone

internal data class AudioCandidate(val url: String, val bitrate: Int)

internal fun selectAudioUrl(candidates: List<AudioCandidate>, preferHighestQuality: Boolean = true): String? {
    val playable = candidates.filter { it.url.isNotBlank() }
    return if (preferHighestQuality) {
        playable.maxByOrNull(AudioCandidate::bitrate)?.url
    } else {
        (playable.filter { it.bitrate <= 160 }.maxByOrNull(AudioCandidate::bitrate)
            ?: playable.minByOrNull(AudioCandidate::bitrate))?.url
    }
}

private val GoogleArtworkSize = Regex("=w\\d+-h\\d+[^?]*$")

internal fun highResolutionArtworkUrl(url: String): String =
    if ("googleusercontent.com" in url && GoogleArtworkSize.containsMatchIn(url)) {
        url.replace(GoogleArtworkSize, "=w1024-h1024-l90-rj")
    } else {
        url
    }

internal fun parseInnertubeSearch(json: String): List<Track> {
    val renderers = mutableListOf<JsonObject>()
    collectMusicRenderers(JsonParser.`object`().from(json), renderers)
    return renderers.mapNotNull { renderer ->
        val videoId = renderer.getObject("playlistItemData").getString("videoId", "")
        val columns = renderer.getArray("flexColumns")
        val title = columnRuns(columns, 0).firstOrNull()?.getString("text", "").orEmpty()
        if (videoId.isBlank() || title.isBlank()) return@mapNotNull null
        val metadataRuns = rendererRuns(renderer)
        val artist = metadataRuns.firstNotNullOfOrNull { run ->
            run.takeIf { it.musicPageType() == "MUSIC_PAGE_TYPE_ARTIST" || it.browseId().startsWith("UC") }
                ?.getString("text", "")
        }?.takeIf(String::isNotBlank) ?: "YouTube Music"
        val album = metadataRuns.firstNotNullOfOrNull { run ->
            run.takeIf { it.musicPageType() == "MUSIC_PAGE_TYPE_ALBUM" || it.browseId().startsWith("MPRE") }
                ?.getString("text", "")
        }?.takeIf(String::isNotBlank) ?: "YouTube Music"
        val durationMs = metadataRuns.asSequence()
            .map { it.getString("text", "") }
            .firstOrNull { it.matches(DurationPattern) }
            ?.split(':')
            ?.fold(0L) { total, part -> total * 60 + part.toLong() }
            ?.times(1_000)
            ?: 0L
        Track(
            title = title,
            artist = artist,
            album = album,
            uri = "https://music.youtube.com/watch?v=$videoId",
            durationMs = durationMs,
            artworkUri = renderer.getObject("thumbnail")
                .getObject("musicThumbnailRenderer")
                .getObject("thumbnail")
                .getArray("thumbnails")
                .mapNotNull { it as? JsonObject }
                .map { it.getString("url", "") }
                .lastOrNull(String::isNotBlank)
                ?.let(::highResolutionArtworkUrl)
                .orEmpty(),
            folder = "YouTube Music",
        )
    }.distinctBy(Track::uri).take(20)
}

private val DurationPattern = Regex("^(?:\\d+:)?\\d{1,2}:\\d{2}$")

private fun collectMusicRenderers(value: Any?, output: MutableList<JsonObject>) {
    when (value) {
        is JsonObject -> {
            value.getObject("musicResponsiveListItemRenderer", null)?.let(output::add)
            value.values.forEach { child -> collectMusicRenderers(child, output) }
        }
        is JsonArray -> value.forEach { child -> collectMusicRenderers(child, output) }
    }
}

private fun columnRuns(columns: JsonArray, index: Int): List<JsonObject> {
    val column = columns.getOrNull(index) as? JsonObject ?: return emptyList()
    val runs = column.getObject("musicResponsiveListItemFlexColumnRenderer")
        .getObject("text")
        .getArray("runs")
    return runs.mapNotNull { it as? JsonObject }
}

private fun rendererRuns(renderer: JsonObject): List<JsonObject> =
    listOf("flexColumns", "fixedColumns").flatMap { key ->
        renderer.getArray(key).flatMap { columnValue ->
            val column = columnValue as? JsonObject ?: return@flatMap emptyList()
            val text = column.getObject("musicResponsiveListItemFlexColumnRenderer", null)
                ?.getObject("text")
                ?: column.getObject("musicResponsiveListItemFixedColumnRenderer")
                    .getObject("text")
            text.getArray("runs").mapNotNull { it as? JsonObject }
        }
    }

private fun JsonObject.browseId(): String = getObject("navigationEndpoint")
    .getObject("browseEndpoint")
    .getString("browseId", "")

private fun JsonObject.musicPageType(): String = getObject("navigationEndpoint")
    .getObject("browseEndpoint")
    .getObject("browseEndpointContextSupportedConfigs")
    .getObject("browseEndpointContextMusicConfig")
    .getString("pageType", "")

internal object OnlineMusic {
    private val youtube = ServiceList.YouTube

    init {
        NewPipe.init(ExtractorDownloader)
    }

    fun search(query: String): List<Track> {
        val cleanQuery = query.trim()
        require(cleanQuery.isNotEmpty()) { "Search query cannot be blank" }
        return try {
            searchInnertube(cleanQuery)
        } catch (_: Exception) {
            searchWithNewPipe(cleanQuery)
        }
    }

    private fun searchInnertube(query: String): List<Track> {
        val version = getYoutubeMusicClientVersion()
        val requestBody = JsonWriter.string()
            .`object`()
                .`object`("context")
                    .`object`("client")
                        .value("clientName", "WEB_REMIX")
                        .value("clientVersion", version)
                        .value("hl", "en-GB")
                        .value("gl", Locale.getDefault().country.ifBlank { "US" })
                        .value("platform", "DESKTOP")
                        .value("utcOffsetMinutes", TimeZone.getDefault().rawOffset / 60_000)
                    .end()
                    .`object`("request").array("internalExperimentFlags").end().value("useSsl", true).end()
                    .`object`("user").value("lockedSafetyMode", false).end()
                .end()
                .value("query", query.trim())
                .value("params", "Eg-KAQwIARAAGAAgACgAMABqChAEEAUQAxAKEAk%3D")
            .end()
            .done()
            .toByteArray(Charsets.UTF_8)
        val response = NewPipe.getDownloader().postWithContentTypeJson(
            "https://music.youtube.com/youtubei/v1/search?prettyPrint=false",
            getYoutubeMusicHeaders(),
            requestBody,
        )
        check(response.responseCode() in 200..299) { "InnerTube search failed (${response.responseCode()})" }
        return parseInnertubeSearch(response.responseBody()).ifEmpty {
            error("InnerTube returned no playable songs")
        }
    }

    private fun searchWithNewPipe(query: String): List<Track> {
        val extractor = youtube.getSearchExtractor(query.trim(), listOf(MUSIC_SONGS), "")
        extractor.fetchPage()
        return extractor.initialPage.items
            .filterIsInstance<StreamInfoItem>()
            .take(20)
            .map { item ->
                Track(
                    title = item.name,
                    artist = item.uploaderName?.takeIf(String::isNotBlank) ?: "YouTube Music",
                    album = "YouTube Music",
                    uri = item.url,
                    durationMs = item.duration.coerceAtLeast(0) * 1_000,
                    artworkUri = item.thumbnails.maxByOrNull { image ->
                        image.width.coerceAtLeast(0) * image.height.coerceAtLeast(0)
                    }?.url?.let(::highResolutionArtworkUrl).orEmpty(),
                    folder = "YouTube Music",
                )
            }
    }

    fun resolve(track: Track, preferHighestQuality: Boolean = true): Track {
        val info = StreamInfo.getInfo(youtube, track.uri)
        val streamUrl = selectAudioUrl(
            info.audioStreams.map { stream ->
                AudioCandidate(stream.content.takeIf { stream.isUrl }.orEmpty(), stream.averageBitrate)
            },
            preferHighestQuality,
        ) ?: error("No playable public audio stream is available for this track.")
        return track.copy(
            title = track.title.ifBlank { info.name },
            artist = track.artist.takeUnless { it == "YouTube Music" }
                ?: info.uploaderName?.takeIf(String::isNotBlank)
                ?: "YouTube Music",
            uri = streamUrl,
            durationMs = track.durationMs.takeIf { it > 0 } ?: info.duration.coerceAtLeast(0) * 1_000,
            artworkUri = track.artworkUri.ifBlank {
                info.thumbnails.maxByOrNull { image ->
                    image.width.coerceAtLeast(0) * image.height.coerceAtLeast(0)
                }?.url?.let(::highResolutionArtworkUrl).orEmpty()
            },
        )
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
