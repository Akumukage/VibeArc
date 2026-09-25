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
}
