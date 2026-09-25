package com.vibearc.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class OnlineMusicTest {
    @Test
    fun `blank searches are rejected before a network request`() {
        assertThrows(IllegalArgumentException::class.java) { OnlineMusic.search("   ") }
    }

    @Test
    fun `highest bitrate playable audio is selected`() {
        val candidates = listOf(
            AudioCandidate("", 320),
            AudioCandidate("https://audio.example/low", 64),
            AudioCandidate("https://audio.example/balanced", 160),
            AudioCandidate("https://audio.example/high", 256),
        )

        assertEquals("https://audio.example/high", selectAudioUrl(candidates))
        assertEquals("https://audio.example/balanced", selectAudioUrl(candidates, preferHighestQuality = false))
        assertNull(selectAudioUrl(listOf(AudioCandidate("", 128))))
    }

    @Test
    fun `google artwork is requested at high resolution`() {
        assertEquals(
            "https://lh3.googleusercontent.com/cover=w1024-h1024-l90-rj",
            highResolutionArtworkUrl("https://lh3.googleusercontent.com/cover=w120-h120-l90-rj"),
        )
    }

    @Test
    fun `innerTube song response maps to a playable catalog track`() {
        val response = """
            {
              "contents": [{
                "musicResponsiveListItemRenderer": {
                  "playlistItemData": {"videoId": "abc123"},
                  "thumbnail": {"musicThumbnailRenderer": {"thumbnail": {"thumbnails": [
                    {"url": "https://lh3.googleusercontent.com/cover=w60-h60"},
                    {"url": "https://lh3.googleusercontent.com/cover=w120-h120"}
                  ]}}},
                  "flexColumns": [
                    {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "Ocean Eyes"}]}}},
                    {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [
                      {"text": "Billie Eilish", "navigationEndpoint": {"browseEndpoint": {
                        "browseId": "UCartist",
                        "browseEndpointContextSupportedConfigs": {"browseEndpointContextMusicConfig": {
                          "pageType": "MUSIC_PAGE_TYPE_ARTIST"
                        }}
                      }}}
                    ]}}},
                    {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [
                      {"text": "Hit Me Hard and Soft", "navigationEndpoint": {"browseEndpoint": {
                        "browseId": "MPREalbum",
                        "browseEndpointContextSupportedConfigs": {"browseEndpointContextMusicConfig": {
                          "pageType": "MUSIC_PAGE_TYPE_ALBUM"
                        }}
                      }}}
                    ]}}}
                  ],
                  "fixedColumns": [
                    {"musicResponsiveListItemFixedColumnRenderer": {"text": {"runs": [{"text": "3:20"}]}}}
                  ]
                }
              }]
            }
        """.trimIndent()

        assertEquals(
            listOf(
                Track(
                    title = "Ocean Eyes",
                    artist = "Billie Eilish",
                    album = "Hit Me Hard and Soft",
                    uri = "https://music.youtube.com/watch?v=abc123",
                    durationMs = 200_000,
                    artworkUri = "https://lh3.googleusercontent.com/cover=w1024-h1024-l90-rj",
                    folder = "YouTube Music",
                ),
            ),
            parseInnertubeSearch(response),
        )
    }
}
