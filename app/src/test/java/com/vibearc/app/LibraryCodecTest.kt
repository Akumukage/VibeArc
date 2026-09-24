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

    @Test
    fun `upsert replaces the same uri without losing favorite state`() {
        val saved = Track("Old title", "Artist", "Album", "content://music/1", true)
        val imported = Track("New title", "Artist", "Album", "content://music/1")

        assertEquals(listOf(imported.copy(isFavorite = true)), listOf(saved).upsert(imported))
    }

    @Test
    fun `toggle favorite changes only the matching uri`() {
        val first = Track("First", "Artist", "Album", "content://music/1")
        val second = Track("Second", "Artist", "Album", "content://music/2")

        assertEquals(listOf(first.copy(isFavorite = true), second), listOf(first, second).toggleFavorite(first.uri))
    }
}
