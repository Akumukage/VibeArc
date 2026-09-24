package com.vibearc.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackQueueStateTest {
    @Test
    fun `queue exposes current next and previous uris`() {
        val state = PlaybackQueueState.from(listOf("one", "two", "three"), startIndex = 1)

        assertEquals("two", state.currentUri)
        assertEquals("three", state.nextUri)
        assertEquals("one", state.previousUri)
        assertEquals("three", state.advance().currentUri)
        assertEquals("one", state.rewind().currentUri)
    }

    @Test
    fun `repeat off stops at queue boundaries`() {
        val first = PlaybackQueueState.from(listOf("one", "two"))
        val last = PlaybackQueueState.from(listOf("one", "two"), startIndex = 1)

        assertNull(first.previousUri)
        assertEquals(first, first.rewind())
        assertNull(last.nextUri)
        assertEquals(last, last.advance())
    }

    @Test
    fun `repeat all wraps at queue boundaries`() {
        val first = PlaybackQueueState.from(listOf("one", "two"), repeatMode = RepeatMode.ALL)
        val last = PlaybackQueueState.from(listOf("one", "two"), startIndex = 1, repeatMode = RepeatMode.ALL)

        assertEquals("two", first.previousUri)
        assertEquals("two", first.rewind().currentUri)
        assertEquals("one", last.nextUri)
        assertEquals("one", last.advance().currentUri)
    }

    @Test
    fun `repeat one keeps the current uri selected`() {
        val state = PlaybackQueueState.from(
            listOf("one", "two", "three"),
            startIndex = 1,
            repeatMode = RepeatMode.ONE,
        )

        assertEquals("two", state.nextUri)
        assertEquals("two", state.previousUri)
        assertEquals("two", state.advance().currentUri)
        assertEquals("two", state.rewind().currentUri)
    }

    @Test
    fun `moving an item keeps the current occurrence selected`() {
        val state = PlaybackQueueState.from(listOf("one", "two", "three"), startIndex = 1)
            .move(fromIndex = 1, toIndex = 2)

        assertEquals(listOf("one", "three", "two"), state.queueUris)
        assertEquals("two", state.currentUri)
    }

    @Test
    fun `shuffle is deterministic and keeps the current occurrence selected`() {
        val original = PlaybackQueueState.from(
            listOf("one", "duplicate", "two", "duplicate", "three"),
            startIndex = 3,
        )
        val first = original.shuffled(seed = 42)
        val second = original.shuffled(seed = 42)

        assertEquals(first, second)
        assertEquals(original.queueUris.sorted(), first.queueUris.sorted())
        assertEquals("duplicate", first.currentUri)
    }

    @Test
    fun `recently played uris are unique newest first and bounded`() {
        val state = (1..22).fold(PlaybackQueueState.from(emptyList())) { current, number ->
            current.recordPlayed("uri-$number")
        }.recordPlayed("uri-10")

        assertEquals("uri-10", state.recentUris.first())
        assertEquals(20, state.recentUris.size)
        assertEquals(1, state.recentUris.count { it == "uri-10" })
        assertFalse("uri-1" in state.recentUris)
    }

    @Test
    fun `sleep timer reports remaining time expiry and cancellation`() {
        val running = PlaybackQueueState.from(emptyList()).startSleepTimer(
            nowMillis = 1_000L,
            durationMillis = 5_000L,
        )

        assertEquals(6_000L, running.sleepDeadlineMillis)
        assertEquals(2_000L, running.remainingSleepMillis(nowMillis = 4_000L))
        assertFalse(running.isSleepTimerExpired(nowMillis = 5_999L))
        assertEquals(0L, running.remainingSleepMillis(nowMillis = 6_001L))
        assertTrue(running.isSleepTimerExpired(nowMillis = 6_001L))
        assertNull(running.cancelSleepTimer().remainingSleepMillis(nowMillis = 4_000L))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `sleep timer rejects non-positive durations`() {
        PlaybackQueueState.from(emptyList()).startSleepTimer(nowMillis = 0L, durationMillis = 0L)
    }
}
