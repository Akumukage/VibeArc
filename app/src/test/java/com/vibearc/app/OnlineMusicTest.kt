package com.vibearc.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OnlineMusicTest {
    @Test
    fun `highest bitrate playable audio is selected`() {
        val candidates = listOf(
            AudioCandidate("", 256),
            AudioCandidate("https://audio.example/low", 64),
            AudioCandidate("https://audio.example/high", 160),
        )

        assertEquals("https://audio.example/high", selectAudioUrl(candidates))
        assertNull(selectAudioUrl(listOf(AudioCandidate("", 128))))
    }

    @Test
    fun `innerTube song response maps to a playable catalog track`() {
        val response = """
            {
              "contents": [{
                "musicResponsiveListItemRenderer": {
                  "playlistItemData": {"videoId": "abc123"},
                  "flexColumns": [
                    {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "Ocean Eyes"}]}}},
                    {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [
                      {"text": "Song"}, {"text": " • "},
                      {"text": "Billie Eilish", "navigationEndpoint": {"browseEndpoint": {"browseId": "UCartist"}}},
                      {"text": " • "}, {"text": "3:20"}
                    ]}}}
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
                    album = "YouTube Music",
                    uri = "https://music.youtube.com/watch?v=abc123",
                    durationMs = 200_000,
                    folder = "YouTube Music",
                ),
            ),
            parseInnertubeSearch(response),
        )
    }
}
