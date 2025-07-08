package com.example.tvmoview.presentation.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.launch
import com.example.tvmoview.domain.model.MediaItem as DomainMediaItem

class SharedExoPlayerViewModel : ViewModel() {
    private var exoPlayer: ExoPlayer? = null
    private val _currentItem = MutableStateFlow<DomainMediaItem?>(null)
    val currentItem: StateFlow<DomainMediaItem?> = _currentItem.asStateFlow()
    private val _isFullScreen = MutableStateFlow(false)
    val isFullScreen: StateFlow<Boolean> = _isFullScreen.asStateFlow()
    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    fun initializePlayer(context: Context): ExoPlayer {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build()
        }
        return exoPlayer!!
    }

    fun prepareVideo(item: DomainMediaItem, videoUrl: String) {
        _currentItem.value = item
        exoPlayer?.apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
        }
    }

    fun startPreview() {
        _isFullScreen.value = false
        exoPlayer?.apply {
            volume = 0f
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
            play()
        }
    }

    fun transitionToFullScreen() {
        _playbackPosition.value = exoPlayer?.currentPosition ?: 0L
        _isFullScreen.value = true
        exoPlayer?.apply {
            volume = 1f
            repeatMode = ExoPlayer.REPEAT_MODE_OFF
        }
    }

    fun exitFullScreen() {
        _playbackPosition.value = exoPlayer?.currentPosition ?: 0L
        _isFullScreen.value = false
        exoPlayer?.apply {
            volume = 0f
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
        }
    }

    fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }

    fun getPlayer(): ExoPlayer? = exoPlayer
}
