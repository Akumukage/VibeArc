package com.vibearc.app

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryCodecTest {
    @Test
    fun `round trip preserves track metadata and favorite state`() {
        val tracks = listOf(
            Track(
                "Rain | Sun",
                "Artist\nName",
                "Album",
                "content://music/1",
                true,
                durationMs = 183_000,
                artworkUri = "content://artwork/1",
            ),
            Track("Night Drive", "VibeArc", "Singles", "content://music/2", false),
        )

        assertEquals(tracks, LibraryCodec.decode(LibraryCodec.encode(tracks)))
    }

    @Test
    fun `decode preserves old five field library rows`() {
        val oldTrack = Track("Old song", "Artist", "Album", "content://music/old", true)
        val oldRow = "T2xkIHNvbmc|QXJ0aXN0|QWxidW0|Y29udGVudDovL211c2ljL29sZA|1"

        assertEquals(listOf(oldTrack), LibraryCodec.decode(oldRow))
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

    @Test
    fun `playlist round trip preserves names and track uri references`() {
        val playlists = listOf(
            Playlist("road-trip", "Road | Trip\n2026", listOf("content://music/1", "content://music/2")),
            Playlist("quiet", "Quiet", emptyList()),
        )

        assertEquals(playlists, PlaylistCodec.decode(PlaylistCodec.encode(playlists)))
    }

    @Test
    fun `playlist operations create rename and delete playlists`() {
        val created = emptyList<Playlist>().createPlaylist("  Road Trip  ", id = "road-trip")
        val renamed = created.renamePlaylist("road-trip", "  Night Drive  ")

        assertEquals(listOf(Playlist("road-trip", "Night Drive")), renamed)
        assertEquals(emptyList<Playlist>(), renamed.deletePlaylist("road-trip"))
    }

    @Test
    fun `playlist track operations add once and remove by uri`() {
        val playlists = listOf(Playlist("mix", "Mix"))
        val withTrack = playlists
            .addTrackToPlaylist("mix", "content://music/1")
            .addTrackToPlaylist("mix", "content://music/1")

        assertEquals(listOf("content://music/1"), withTrack.single().trackUris)
        assertEquals(emptyList<String>(), withTrack.removeTrackFromPlaylist("mix", "content://music/1").single().trackUris)
    }
}
