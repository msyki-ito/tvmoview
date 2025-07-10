package com.example.tvmoview.presentation.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@UnstableApi
class AdaptiveQualityManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        const val QUALITY_SD_WIDTH = 854
        const val QUALITY_SD_HEIGHT = 480
        const val QUALITY_HD_WIDTH = 1280
        const val QUALITY_HD_HEIGHT = 720
        const val QUALITY_FHD_WIDTH = 1920
        const val QUALITY_FHD_HEIGHT = 1080
        const val QUALITY_4K_WIDTH = 3840
        const val QUALITY_4K_HEIGHT = 2160

        const val DELAY_TO_HD = 2000L
        const val DELAY_TO_FHD = 5000L
        const val DELAY_TO_4K = 10000L

        const val MIN_BUFFER_MS = 1000
        const val MAX_BUFFER_MS = 10000
        const val BUFFER_FOR_PLAYBACK_MS = 250
        const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 500
    }

    fun createFastStartLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MAX_BUFFER_MS,
                BUFFER_FOR_PLAYBACK_MS,
                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    fun createInitialTrackSelector(): DefaultTrackSelector {
        val trackSelector = DefaultTrackSelector(context)
        val initialParams = trackSelector.buildUponParameters()
            .setMaxVideoSize(QUALITY_SD_WIDTH, QUALITY_SD_HEIGHT)
            .setForceLowestBitrate(true)
            .setPreferredVideoMimeTypes("video/avc", "video/mp4")
            .build()
        trackSelector.setParameters(initialParams)
        Log.d("AdaptiveQuality", "\uD83D\uDCFA 初期画質: SD ${QUALITY_SD_WIDTH}x${QUALITY_SD_HEIGHT}")
        return trackSelector
    }

    fun applyPreviewQuality(player: ExoPlayer) {
        val params = player.trackSelectionParameters.buildUpon()
            .setMaxVideoSize(640, 360)
            .setMaxVideoBitrate(800_000)
            .setForceLowestBitrate(true)
            .build()
        player.trackSelectionParameters = params
        Log.d("AdaptiveQuality", "\uD83D\uDC41\uFE0F プレビュー画質: 360p")
    }

    fun startQualityProgression(
        trackSelector: DefaultTrackSelector,
        enable4K: Boolean = false
    ) {
        scope.launch(Dispatchers.Main) {
            delay(DELAY_TO_HD)
            upgradeQuality(trackSelector, QUALITY_HD_WIDTH, QUALITY_HD_HEIGHT, false)
            delay(DELAY_TO_FHD - DELAY_TO_HD)
            upgradeQuality(trackSelector, QUALITY_FHD_WIDTH, QUALITY_FHD_HEIGHT, false)
            if (enable4K) {
                delay(DELAY_TO_4K - DELAY_TO_FHD)
                upgradeQuality(trackSelector, QUALITY_4K_WIDTH, QUALITY_4K_HEIGHT, false)
            }
        }
    }

    private fun upgradeQuality(
        trackSelector: DefaultTrackSelector,
        width: Int,
        height: Int,
        forceLowest: Boolean
    ) {
        val newParams = trackSelector.buildUponParameters()
            .setMinVideoSize(width, height)
            .setMaxVideoSize(width, height)
            .setForceLowestBitrate(forceLowest)
            .build()
        trackSelector.setParameters(newParams)
        Log.d("AdaptiveQuality", "\uD83D\uDD3C 画質アップ: ${width}x${height}")
    }

    fun cleanup() {
        handler.removeCallbacksAndMessages(null)
        Log.d("AdaptiveQuality", "\uD83E\uDEB0 クリーンアップ")
    }
}
