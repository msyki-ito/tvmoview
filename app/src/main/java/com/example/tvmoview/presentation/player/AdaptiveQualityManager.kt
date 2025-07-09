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
import kotlinx.coroutines.launch

/**
 * 動画再生の初期画質を抑えて高速起動し、段階的に画質を上げていくマネージャ
 */
@UnstableApi
class AdaptiveQualityManager(
    private val scope: CoroutineScope
) {
    private val handler = Handler(Looper.getMainLooper())
    private val pendingRunnables = mutableListOf<Runnable>()

    companion object {
        // 画質プリセット
        const val QUALITY_SD_WIDTH = 854
        const val QUALITY_SD_HEIGHT = 480
        const val QUALITY_HD_WIDTH = 1280
        const val QUALITY_HD_HEIGHT = 720
        const val QUALITY_FHD_WIDTH = 1920
        const val QUALITY_FHD_HEIGHT = 1080
        const val QUALITY_4K_WIDTH = 3840
        const val QUALITY_4K_HEIGHT = 2160

        // 切り替えタイミング
        const val DELAY_TO_HD = 2000L
        const val DELAY_TO_FHD = 5000L
        const val DELAY_TO_4K = 10000L

        // バッファ設定
        const val MIN_BUFFER_MS = 15000
        const val MAX_BUFFER_MS = 30000
        const val BUFFER_FOR_PLAYBACK_MS = 500
        const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 1000
    }

    /** 高速起動用のLoadControl */
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

    /** 初期はSD画質で開始するTrackSelector */
    fun createInitialTrackSelector(context: Context): DefaultTrackSelector {
        val trackSelector = DefaultTrackSelector(context)
        val params = trackSelector.buildUponParameters()
            .setMaxVideoSize(QUALITY_SD_WIDTH, QUALITY_SD_HEIGHT)
            .setMaxVideoBitrate(1_500_000)
            .setForceHighestSupportedBitrate(false)
            .setExceedVideoConstraintsIfNecessary(false)
            .build()
        trackSelector.setParameters(params)
        Log.d("AdaptiveQuality", "\uD83D\uDCFA 初期画質設定: SD (${QUALITY_SD_WIDTH}x${QUALITY_SD_HEIGHT})")
        return trackSelector
    }

    /** プレビューモード向けの低画質設定 */
    fun applyPreviewQuality(player: ExoPlayer) {
        val params = player.trackSelectionParameters.buildUpon()
            .setMaxVideoSize(640, 360)
            .setMaxVideoBitrate(800_000)
            .build()
        player.trackSelectionParameters = params
        Log.d("AdaptiveQuality", "\uD83D\uDC41\uFE0F プレビュー画質: 360p")
    }

    /** 段階的に画質を向上させる */
    fun startQualityProgression(player: ExoPlayer, isFullScreen: Boolean = true, enable4K: Boolean = false) {
        if (!isFullScreen) return
        clearPendingQualityChanges()

        val hdRunnable = object : Runnable {
            override fun run() {
                upgradeToHD(player)
                pendingRunnables.remove(this)
            }
        }
        handler.postDelayed(hdRunnable, DELAY_TO_HD)
        pendingRunnables.add(hdRunnable)

        val fhdRunnable = object : Runnable {
            override fun run() {
                upgradeToFullHD(player)
                pendingRunnables.remove(this)
            }
        }
        handler.postDelayed(fhdRunnable, DELAY_TO_FHD)
        pendingRunnables.add(fhdRunnable)

        if (enable4K) {
            val fourKRunnable = Runnable {
                upgradeTo4K(player)
                pendingRunnables.clear()
            }
            handler.postDelayed(fourKRunnable, DELAY_TO_4K)
            pendingRunnables.add(fourKRunnable)
        }
    }

    private fun upgradeToHD(player: ExoPlayer) {
        scope.launch {
            try {
                val params = player.trackSelectionParameters.buildUpon()
                    .setMaxVideoSize(QUALITY_HD_WIDTH, QUALITY_HD_HEIGHT)
                    .setMaxVideoBitrate(3_000_000)
                    .build()
                player.trackSelectionParameters = params
                Log.d("AdaptiveQuality", "\uD83D\uDCFA 画質向上: HD (${QUALITY_HD_WIDTH}x${QUALITY_HD_HEIGHT})")
            } catch (e: Exception) {
                Log.e("AdaptiveQuality", "HD切り替えエラー", e)
            }
        }
    }

    private fun upgradeToFullHD(player: ExoPlayer) {
        scope.launch {
            try {
                val params = player.trackSelectionParameters.buildUpon()
                    .setMaxVideoSize(QUALITY_FHD_WIDTH, QUALITY_FHD_HEIGHT)
                    .setMaxVideoBitrate(5_000_000)
                    .build()
                player.trackSelectionParameters = params
                Log.d("AdaptiveQuality", "\uD83D\uDCFA 画質向上: Full HD (${QUALITY_FHD_WIDTH}x${QUALITY_FHD_HEIGHT})")
            } catch (e: Exception) {
                Log.e("AdaptiveQuality", "FHD切り替えエラー", e)
            }
        }
    }

    private fun upgradeTo4K(player: ExoPlayer) {
        scope.launch {
            try {
                val params = player.trackSelectionParameters.buildUpon()
                    .setMaxVideoSize(QUALITY_4K_WIDTH, QUALITY_4K_HEIGHT)
                    .setMaxVideoBitrate(15_000_000)
                    .build()
                player.trackSelectionParameters = params
                Log.d("AdaptiveQuality", "\uD83D\uDCFA 画質向上: 4K (${QUALITY_4K_WIDTH}x${QUALITY_4K_HEIGHT})")
            } catch (e: Exception) {
                Log.e("AdaptiveQuality", "4K切り替えエラー", e)
            }
        }
    }

    fun setQualityManually(player: ExoPlayer, width: Int, height: Int, bitrate: Int) {
        val params = player.trackSelectionParameters.buildUpon()
            .setMaxVideoSize(width, height)
            .setMaxVideoBitrate(bitrate)
            .build()
        player.trackSelectionParameters = params
        Log.d("AdaptiveQuality", "\uD83D\uDCFA 手動画質設定: ${width}x${height} @ ${bitrate / 1000}kbps")
    }

    private fun clearPendingQualityChanges() {
        pendingRunnables.forEach { handler.removeCallbacks(it) }
        pendingRunnables.clear()
    }

    fun cleanup() {
        clearPendingQualityChanges()
    }
}
