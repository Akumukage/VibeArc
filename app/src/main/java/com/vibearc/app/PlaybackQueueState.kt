package com.vibearc.app

import kotlin.random.Random

internal enum class RepeatMode { OFF, ALL, ONE }

internal data class PlaybackQueueState(
    val queueUris: List<String>,
    val currentIndex: Int,
    val repeatMode: RepeatMode,
    val recentUris: List<String>,
    val sleepDeadlineMillis: Long?,
) {
    init {
        require(
            (queueUris.isEmpty() && currentIndex == -1) ||
                (queueUris.isNotEmpty() && currentIndex in queueUris.indices),
        )
    }

    val currentUri: String?
        get() = queueUris.getOrNull(currentIndex)

    val nextUri: String?
        get() = adjacentIndex(1)?.let(queueUris::get)

    val previousUri: String?
        get() = adjacentIndex(-1)?.let(queueUris::get)

    fun withRepeatMode(mode: RepeatMode): PlaybackQueueState = copy(repeatMode = mode)

    fun advance(): PlaybackQueueState = adjacentIndex(1)?.let { copy(currentIndex = it) } ?: this

    fun rewind(): PlaybackQueueState = adjacentIndex(-1)?.let { copy(currentIndex = it) } ?: this

    fun move(fromIndex: Int, toIndex: Int): PlaybackQueueState {
        require(fromIndex in queueUris.indices && toIndex in queueUris.indices)
        if (fromIndex == toIndex) return this

        val ordered = queueUris.mapIndexed(::IndexedUri).toMutableList()
        ordered.add(toIndex, ordered.removeAt(fromIndex))
        return copy(
            queueUris = ordered.map(IndexedUri::uri),
            currentIndex = ordered.indexOfFirst { it.originalIndex == currentIndex },
        )
    }

    fun shuffled(seed: Int): PlaybackQueueState {
        if (queueUris.size < 2) return this
        val shuffled = queueUris.mapIndexed(::IndexedUri).shuffled(Random(seed))
        return copy(
            queueUris = shuffled.map(IndexedUri::uri),
            currentIndex = shuffled.indexOfFirst { it.originalIndex == currentIndex },
        )
    }

    fun recordPlayed(uri: String): PlaybackQueueState {
        require(uri.isNotBlank())
        return copy(recentUris = (listOf(uri) + recentUris.filterNot { it == uri }).take(RecentLimit))
    }

    fun startSleepTimer(nowMillis: Long, durationMillis: Long): PlaybackQueueState {
        require(durationMillis > 0L)
        return copy(sleepDeadlineMillis = Math.addExact(nowMillis, durationMillis))
    }

    fun cancelSleepTimer(): PlaybackQueueState = copy(sleepDeadlineMillis = null)

    fun remainingSleepMillis(nowMillis: Long): Long? = sleepDeadlineMillis?.let { deadline ->
        (deadline - nowMillis).coerceAtLeast(0L)
    }

    fun isSleepTimerExpired(nowMillis: Long): Boolean =
        sleepDeadlineMillis?.let { nowMillis >= it } ?: false

    private fun adjacentIndex(step: Int): Int? {
        if (currentIndex !in queueUris.indices) return null
        if (repeatMode == RepeatMode.ONE) return currentIndex
        val candidate = currentIndex + step
        if (candidate in queueUris.indices) return candidate
        if (repeatMode != RepeatMode.ALL) return null
        return if (step > 0) 0 else queueUris.lastIndex
    }

    companion object {
        private const val RecentLimit = 20

        fun from(
            queueUris: List<String>,
            startIndex: Int = 0,
            repeatMode: RepeatMode = RepeatMode.OFF,
        ): PlaybackQueueState {
            require(queueUris.isEmpty() || startIndex in queueUris.indices)
            return PlaybackQueueState(
                queueUris = queueUris.toList(),
                currentIndex = if (queueUris.isEmpty()) -1 else startIndex,
                repeatMode = repeatMode,
                recentUris = emptyList(),
                sleepDeadlineMillis = null,
            )
        }
    }
}

private data class IndexedUri(val originalIndex: Int, val uri: String)
