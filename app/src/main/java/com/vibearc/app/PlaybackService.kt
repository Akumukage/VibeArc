package com.vibearc.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null
    private val handler = Handler(Looper.getMainLooper())
    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val mediaId = mediaItem?.mediaId?.takeIf(String::isNotBlank) ?: return
            saveRecentUris(loadRecentUris().recordRecentUri(mediaId))
        }
    }
    private val sleepTimerCheck = object : Runnable {
        override fun run() {
            loadSleepDeadlineMillis()?.let { deadline ->
                if (System.currentTimeMillis() >= deadline) {
                    player.pause()
                    saveSleepDeadlineMillis(null)
                }
            }
            handler.postDelayed(this, SleepTimerPollMillis)
        }
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build().also { it.addListener(playerListener) }
        mediaSession = MediaSession.Builder(this, player).build()
        handler.post(sleepTimerCheck)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        handler.removeCallbacks(sleepTimerCheck)
        player.removeListener(playerListener)
        mediaSession?.release()
        mediaSession = null
        player.release()
        super.onDestroy()
    }
}

private const val PlaybackPreferencesName = "vibearc_playback"
private const val RecentUrisKey = "recent_uris"
private const val SleepDeadlineKey = "sleep_deadline"
private const val SleepTimerPollMillis = 1_000L

internal fun Context.loadRecentUris(): List<String> = RecentUriCodec.decode(
    getSharedPreferences(PlaybackPreferencesName, Context.MODE_PRIVATE)
        .getString(RecentUrisKey, "")
        .orEmpty(),
)

internal fun Context.saveRecentUris(uris: List<String>) {
    getSharedPreferences(PlaybackPreferencesName, Context.MODE_PRIVATE)
        .edit()
        .putString(RecentUrisKey, RecentUriCodec.encode(uris))
        .apply()
}

internal fun Context.loadSleepDeadlineMillis(): Long? =
    getSharedPreferences(PlaybackPreferencesName, Context.MODE_PRIVATE)
        .getLong(SleepDeadlineKey, 0L)
        .takeIf { it > 0L }

internal fun Context.saveSleepDeadlineMillis(deadlineMillis: Long?) {
    getSharedPreferences(PlaybackPreferencesName, Context.MODE_PRIVATE)
        .edit()
        .apply {
            if (deadlineMillis == null) remove(SleepDeadlineKey)
            else putLong(SleepDeadlineKey, deadlineMillis)
        }
        .apply()
}
