package com.vibearc.app

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryCodecTest {
    @Test
    fun `round trip preserves track metadata and favorite state`() {
        val tracks = listOf(
            Track("Rain | Sun", "Artist\nName", "Album", "content://music/1", true),
            Track("Night Drive", "VibeArc", "Singles", "content://music/2", false),
        )

        assertEquals(tracks, LibraryCodec.decode(LibraryCodec.encode(tracks)))
    }

    @Test
    fun `decode ignores corrupted rows`() {
        assertEquals(emptyList<Track>(), LibraryCodec.decode("not-a-library-row"))
    }
}
