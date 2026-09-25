package com.vibearc.app

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackStateTest {
    @Test
    fun `selected online track is added to the queue instead of falling back to demo`() {
        val demo = Track("First Light", "VibeArc Demo", "Signals")
        val online = Track("Ocean Eyes", "Billie Eilish", "YouTube Music", "https://audio.example/ocean")

        assertEquals(online, playbackQueue(listOf(demo), online).first())
    }

    @Test
    fun `recent uri codec round trips reserved characters`() {
        val uris = listOf("content://music/one|two", "content://music/line\nbreak")

        assertEquals(uris, RecentUriCodec.decode(RecentUriCodec.encode(uris)))
    }

    @Test
    fun `record recent uri keeps newest unique items first and limits history`() {
        val history = (1..22).fold(emptyList<String>()) { current, number ->
            current.recordRecentUri("uri-$number")
        }.recordRecentUri("uri-10")

        assertEquals("uri-10", history.first())
        assertEquals(20, history.size)
        assertEquals(1, history.count { it == "uri-10" })
    }
}
