package com.example.tvmoview.presentation.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SharedPlayerManager {
    private var sharedPlayer: ExoPlayer? = null
    private val _currentVideoId = MutableStateFlow<String?>(null)
    val currentVideoId: StateFlow<String?> = _currentVideoId

    fun getOrCreatePlayer(context: Context, videoId: String): ExoPlayer {
        return if (sharedPlayer != null && _currentVideoId.value == videoId) {
            sharedPlayer!!
        } else {
            releasePlayer()
            ExoPlayer.Builder(context).build().also {
                sharedPlayer = it
                _currentVideoId.value = videoId
            }
        }
    }

    fun transferPlayer(): ExoPlayer? {
        val player = sharedPlayer
        sharedPlayer = null
        return player
    }

    fun releasePlayer() {
        sharedPlayer?.release()
        sharedPlayer = null
        _currentVideoId.value = null
    }
}
