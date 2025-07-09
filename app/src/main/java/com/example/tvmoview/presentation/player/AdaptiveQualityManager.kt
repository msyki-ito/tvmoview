//package com.example.tvmoview.presentation.player
//
//import android.os.Handler
//import android.os.Looper
//import android.util.Log
//import androidx.media3.common.TrackSelectionParameters
//import androidx.media3.common.util.UnstableApi
//import androidx.media3.exoplayer.ExoPlayer
//import androidx.media3.exoplayer.LoadControl
//import androidx.media3.exoplayer.DefaultLoadControl
//import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.launch
//
///**
// * 動画の段階的画質制御を管理
// * 初期：SD画質で高速起動 → 2秒後：HD画質 → 5秒後：フル画質
// */
//@UnstableApi
//class AdaptiveQualityManager(
//    private val scope: CoroutineScope
//) {
//    private val handler = Handler(Looper.getMainLooper())
//
//    companion object {
//        // 画質プリセット
//        const val QUALITY_SD_WIDTH = 854
//        const val QUALITY_SD_HEIGHT = 480
//        const val QUALITY_HD_WIDTH = 1280
//        const val QUALITY_HD_HEIGHT = 720
//        const val QUALITY_FHD_WIDTH = 1920
//        const val QUALITY_FHD_HEIGHT = 1080
//        const val QUALITY_4K_WIDTH = 3840
//        const val QUALITY_4K_HEIGHT = 2160
//
//        // タイミング設定（ミリ秒）
//        const val DELAY_TO_HD = 2000L    // SD→HD: 2秒後
//        const val DELAY_TO_FHD = 5000L   // SD→FHD: 5秒後
//        const val DELAY_TO_4K = 10000L   // SD→4K: 10秒後（4K対応時）
//
//        // バッファ設定
//        const val MIN_BUFFER_MS = 15000
//        const val MAX_BUFFER_MS = 30000
//        const val BUFFER_FOR_PLAYBACK_MS = 500      // 0.5秒で再生開始
//        const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 1000
//    }
//
//    /**
//     * 高速起動用のLoadControlを作成
//     */
//    fun createFastStartLoadControl(): LoadControl {
//        return DefaultLoadControl.Builder()
//            .setBufferDurationsMs(
//                MIN_BUFFER_MS,
//                MAX_BUFFER_MS,
//                BUFFER_FOR_PLAYBACK_MS,              // 0.5秒バッファで再生開始
//                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
//            )
//            .setPrioritizeTimeOverSizeThresholds(true)  // 時間優先
//            .build()
//    }
//
//    /**
//     * 初期のTrackSelectorを作成（SD画質制限）
//     */
//    fun createInitialTrackSelector(): DefaultTrackSelector {
//        val trackSelector = DefaultTrackSelector(DefaultTrackSelector.ParametersBuilder().build())
//
//        // 初期はSD画質に制限
//        val initialParams = trackSelector.buildUponParameters()
//            .setMaxVideoSize(QUALITY_SD_WIDTH, QUALITY_SD_HEIGHT)
//            .setMaxVideoBitrate(1_500_000)  // 1.5Mbps上限
//            .setForceHighestSupportedBitrate(false)  // 低ビットレート優先
//            .setExceedVideoConstraintsIfNecessary(false)
//            .build()
//
//        trackSelector.setParameters(initialParams)
//        Log.d("AdaptiveQuality", "📺 初期画質設定: SD (${QUALITY_SD_WIDTH}x${QUALITY_SD_HEIGHT})")
//
//        return trackSelector
//    }
//
//    /**
//     * プレビュー用の設定（さらに低画質）
//     */
//    fun applyPreviewQuality(player: ExoPlayer) {
//        val params = player.trackSelectionParameters.buildUpon()
//            .setMaxVideoSize(640, 360)  // 360p
//            .setMaxVideoBitrate(800_000)  // 800kbps
//            .build()
//        player.trackSelectionParameters = params
//        Log.d("AdaptiveQuality", "👁️ プレビュー画質: 360p")
//    }
//
//    /**
//     * 段階的な画質向上を開始
//     */
//    fun startQualityProgression(
//        player: ExoPlayer,
//        isFullScreen: Boolean = true,
//        enable4K: Boolean = false
//    ) {
//        if (!isFullScreen) {
//            // プレビューモードでは画質向上しない
//            return
//        }
//
//        // ステップ1: HD画質へ（2秒後）
//        handler.postDelayed({
//            upgradeToHD(player)
//        }, DELAY_TO_HD)
//
//        // ステップ2: フルHD画質へ（5秒後）
//        handler.postDelayed({
//            upgradeToFullHD(player)
//        }, DELAY_TO_FHD)
//
//        // ステップ3: 4K画質へ（10秒後、有効な場合のみ）
//        if (enable4K) {
//            handler.postDelayed({
//                upgradeTo4K(player)
//            }, DELAY_TO_4K)
//        }
//    }
//
//    /**
//     * HD画質へアップグレード
//     */
//    private fun upgradeToHD(player: ExoPlayer) {
//        scope.launch {
//            try {
//                val params = player.trackSelectionParameters.buildUpon()
//                    .setMaxVideoSize(QUALITY_HD_WIDTH, QUALITY_HD_HEIGHT)
//                    .setMaxVideoBitrate(3_000_000)  // 3Mbps
//                    .build()
//                player.trackSelectionParameters = params
//                Log.d("AdaptiveQuality", "📺 画質向上: HD (${QUALITY_HD_WIDTH}x${QUALITY_HD_HEIGHT})")
//            } catch (e: Exception) {
//                Log.e("AdaptiveQuality", "HD切り替えエラー", e)
//            }
//        }
//    }
//
//    /**
//     * フルHD画質へアップグレード
//     */
//    private fun upgradeToFullHD(player: ExoPlayer) {
//        scope.launch {
//            try {
//                val params = player.trackSelectionParameters.buildUpon()
//                    .setMaxVideoSize(QUALITY_FHD_WIDTH, QUALITY_FHD_HEIGHT)
//                    .setMaxVideoBitrate(5_000_000)  // 5Mbps
//                    .build()
//                player.trackSelectionParameters = params
//                Log.d("AdaptiveQuality", "📺 画質向上: Full HD (${QUALITY_FHD_WIDTH}x${QUALITY_FHD_HEIGHT})")
//            } catch (e: Exception) {
//                Log.e("AdaptiveQuality", "FHD切り替えエラー", e)
//            }
//        }
//    }
//
//    /**
//     * 4K画質へアップグレード
//     */
//    private fun upgradeTo4K(player: ExoPlayer) {
//        scope.launch {
//            try {
//                val params = player.trackSelectionParameters.buildUpon()
//                    .setMaxVideoSize(QUALITY_4K_WIDTH, QUALITY_4K_HEIGHT)
//                    .setMaxVideoBitrate(15_000_000)  // 15Mbps
//                    .build()
//                player.trackSelectionParameters = params
//                Log.d("AdaptiveQuality", "📺 画質向上: 4K (${QUALITY_4K_WIDTH}x${QUALITY_4K_HEIGHT})")
//            } catch (e: Exception) {
//                Log.e("AdaptiveQuality", "4K切り替えエラー", e)
//            }
//        }
//    }
//
//    /**
//     * 手動で画質を設定
//     */
//    fun setQualityManually(player: ExoPlayer, width: Int, height: Int, bitrate: Int) {
//        val params = player.trackSelectionParameters.buildUpon()
//            .setMaxVideoSize(width, height)
//            .setMaxVideoBitrate(bitrate)
//            .build()
//        player.trackSelectionParameters = params
//        Log.d("AdaptiveQuality", "📺 手動画質設定: ${width}x${height} @ ${bitrate/1000}kbps")
//    }
//
//    /**
//     * クリーンアップ
//     */
//    fun cleanup() {
//        handler.removeCallbacksAndMessages(null)
//    }
//}