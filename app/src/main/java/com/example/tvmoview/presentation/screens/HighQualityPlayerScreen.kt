package com.example.tvmoview.presentation.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.ui.PlayerView
import androidx.media3.common.VideoSize
import com.example.tvmoview.MainActivity
import com.example.tvmoview.data.prefs.UserPreferences
import com.example.tvmoview.presentation.components.LoadingAnimation
import com.example.tvmoview.presentation.components.UltraFastSeekPreview
import java.io.File

@Composable
fun HighQualityPlayerScreen(
    itemId: String,
    onBack: () -> Unit,
    downloadUrl: String = ""
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val dataSourceFactory = remember { DefaultDataSource.Factory(context) }

    val resolvedUrl by produceState<String?>(null, itemId, downloadUrl) {
        value = resolveVideoUrl(itemId, downloadUrl)
    }

    // プレビュー用低解像度URL
    val previewUrl by produceState<String?>(null, itemId) {
        value = MainActivity.oneDriveRepository.getPreviewUrl(itemId)
    }

    // カスタムシークバー表示制御
    var showCustomSeek by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var seekMessage by remember { mutableStateOf("") }
    var seekForward by remember { mutableStateOf(true) }
    var isSeekingPreview by remember { mutableStateOf(false) }
    var previewPosition by remember { mutableLongStateOf(0L) }
    var wasPlayingBeforeSeek by remember { mutableStateOf(false) }
    var previewJob by remember { mutableStateOf<Job?>(null) }

    // PlayerView参照用とコントローラー制御
    var playerView by remember { mutableStateOf<PlayerView?>(null) }

    Log.d("VideoPlayer", "🎬 プレイヤー起動: itemId=$itemId")

    // ExoPlayer初期化
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    fun releasePlayer() {
        exoPlayer?.pause()
        exoPlayer?.release()
        exoPlayer = null
        playerView?.player = null
    }

    LaunchedEffect(resolvedUrl) {
        releasePlayer()
        exoPlayer = resolvedUrl?.let { url ->
            ExoPlayer.Builder(context)
                .setTrackSelector(
                    DefaultTrackSelector(context).apply {
                        setParameters(
                            buildUponParameters()
                                .setMaxVideoSizeSd()
                                .setAllowVideoMixedMimeTypeAdaptiveness(true)
                                .setPreferredVideoMimeType(MimeTypes.VIDEO_H264)
                                .build()
                        )
                    }
                )
                .build().also { player ->
                    val mediaSource = when {
                        url.contains(".m3u8") -> HlsMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(MediaItem.fromUri(url))
                        url.contains(".mpd") -> DashMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(MediaItem.fromUri(url))
                        else -> ProgressiveMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(MediaItem.fromUri(url))
                    }
                    player.setMediaSource(mediaSource)
                    player.addAnalyticsListener(object : AnalyticsListener {
                        override fun onVideoSizeChanged(eventTime: AnalyticsListener.EventTime, videoSize: VideoSize) {
                            Log.d("Adaptive", "解像度: ${videoSize.width}x${videoSize.height}")
                        }

                        override fun onBandwidthEstimate(
                            eventTime: AnalyticsListener.EventTime,
                            totalLoadTimeMs: Int,
                            totalBytesLoaded: Long,
                            bitrateEstimate: Long
                        ) {
                            Log.d("Adaptive", "推定帯域: ${bitrateEstimate / 1000} kbps")
                        }
                    })
                    player.prepare()
                    val resume = UserPreferences.getResumePosition(itemId)
                    if (resume > 0) {
                        player.seekTo(resume)
                        Log.d("VideoPlayer", "⏩ 再開位置 $resume")
                    }
                    player.playWhenReady = true
                }
        }
        playerView?.player = exoPlayer
    }
    LaunchedEffect(playerView, exoPlayer) {
        playerView?.player = exoPlayer
    }

    LaunchedEffect(isSeekingPreview) {
        playerView?.useController = !isSeekingPreview
    }

    // シークプレビュー開始
    fun startSeekPreview(forward: Boolean, message: String) {
        exoPlayer?.let { player ->
            if (!isSeekingPreview) {
                wasPlayingBeforeSeek = player.isPlaying
                player.pause()
            }

            playerView?.hideController()
            playerView?.useController = false

            isSeekingPreview = true
            currentPosition = player.currentPosition
            duration = player.duration
            previewPosition = if (forward) {
                minOf(duration, currentPosition + 10000)
            } else {
                maxOf(0, currentPosition - 10000)
            }

            player.seekTo(previewPosition)

            seekForward = forward
            seekMessage = message
            showCustomSeek = true

            previewJob?.cancel()
            previewJob = coroutineScope.launch {
                delay(5000)
                if (isSeekingPreview) {
                    showCustomSeek = false
                }
            }
        }
    }

    // 再生位置更新ループ
    LaunchedEffect(exoPlayer) {
        while (exoPlayer != null) {
            currentPosition = exoPlayer?.currentPosition ?: 0L
            duration = exoPlayer?.duration ?: 0L
            delay(500)
        }
    }

    // クリーンアップ
    DisposableEffect(Unit) {
        onDispose {
            val pos = exoPlayer?.currentPosition ?: 0L
            val dur = exoPlayer?.duration ?: 0L
            if (dur - pos > 3000) {
                UserPreferences.setResumePosition(itemId, pos)
            } else {
                UserPreferences.clearResumePosition(itemId)
            }
            Log.d("VideoPlayer", "🧹 ExoPlayer解放")
            releasePlayer()
        }
    }

    // フォーカス設定
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // 戻るボタンで即終了
    BackHandler {
        val pos = exoPlayer?.currentPosition ?: 0L
        val dur = exoPlayer?.duration ?: 0L
        if (dur - pos > 3000) {
            UserPreferences.setResumePosition(itemId, pos)
        } else {
            UserPreferences.clearResumePosition(itemId)
        }
        releasePlayer()
        onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.DirectionRight -> {
                            startSeekPreview(true, "+10秒")
                            true
                        }
                        Key.DirectionLeft -> {
                            startSeekPreview(false, "-10秒")
                            true
                        }
                        // 📺 TVリモコン：上ボタン（音量上げる）
                        Key.DirectionUp -> {
                            val currentVolume = exoPlayer?.volume ?: 0f
                            val newVolume = minOf(1.0f, currentVolume + 0.1f)
                            exoPlayer?.volume = newVolume
                            Log.d("VideoPlayer", "🔊 音量上げる: $newVolume")
                            true
                        }
                        // 📺 TVリモコン：下ボタン（音量下げる）
                        Key.DirectionDown -> {
                            val currentVolume = exoPlayer?.volume ?: 0f
                            val newVolume = maxOf(0.0f, currentVolume - 0.1f)
                            exoPlayer?.volume = newVolume
                            Log.d("VideoPlayer", "🔉 音量下げる: $newVolume")
                            true
                        }
                        Key.DirectionCenter, Key.Enter, Key.MediaPlayPause -> {
                            if (isSeekingPreview) {
                                isSeekingPreview = false
                                showCustomSeek = false
                                exoPlayer?.play()
                            } else {
                                if (exoPlayer?.isPlaying == true) {
                                    exoPlayer?.pause()
                                } else {
                                    exoPlayer?.play()
                                }
                            }
                            true
                        }
                        // 📺 TVリモコン：戻るボタン
                        Key.Back, Key.Escape -> {
                            val pos = exoPlayer?.currentPosition ?: 0L
                            val dur = exoPlayer?.duration ?: 0L
                            if (dur - pos > 3000) {
                                UserPreferences.setResumePosition(itemId, pos)
                            } else {
                                UserPreferences.clearResumePosition(itemId)
                            }
                            releasePlayer()
                            onBack()
                            true
                        }
                        // キーボード用（開発時）
                        Key.Spacebar -> {
                            if (exoPlayer?.isPlaying == true) {
                                exoPlayer?.pause()
                            } else {
                                exoPlayer?.play()
                            }
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        // ExoPlayer表示（URL未解決時はローディング）
        resolvedUrl?.let {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = !isSeekingPreview
                        setShowSubtitleButton(true)
                        setShowVrButton(false)
                        playerView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } ?: LoadingAnimation()



        // カスタムシークバー（一時表示）
        if (showCustomSeek && duration > 0) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .padding(horizontal = 32.dp, vertical = 24.dp)
            ) {
                if (isSeekingPreview) {
                    if (previewUrl != null && resolvedUrl != null) {
                        UltraFastSeekPreview(
                            videoFile = File(resolvedUrl!!),
                            seekPosition = previewPosition,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(320.dp, 180.dp)
                                .background(Color.Black, RoundedCornerShape(8.dp))
                                .padding(4.dp)
                        ) {
                            Text(
                                text = "プレビュー: ${formatTime(previewPosition)}",
                                color = Color.White,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (seekForward) Icons.Default.FastForward else Icons.Default.FastRewind,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = seekMessage,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (isSeekingPreview) {
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = "決定で再生",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = (previewPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(if (isSeekingPreview) previewPosition else currentPosition),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = formatTime(duration),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    Log.d("VideoPlayer", "✅ プレイヤー画面表示完了")
}

// 時間フォーマット関数
private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

// 動画URL取得（OneDrive統合版）

private suspend fun resolveVideoUrl(itemId: String, downloadUrl: String): String {
    // 再生直前で常に最新のURLを取得する
    val freshUrl = MainActivity.oneDriveRepository.getDownloadUrl(itemId)
    return when {
        freshUrl != null -> {
            Log.d("VideoPlayer", "✅ downloadURL取得成功: $itemId")
            freshUrl
        }
        downloadUrl.isNotEmpty() -> {
            Log.d("VideoPlayer", "⚠️ 新規URL取得失敗、既存downloadURL使用: $itemId")
            downloadUrl
        }
        else -> {
            Log.d("VideoPlayer", "⚠️ downloadURL未取得、テストURL使用: $itemId")
            getTestVideoUrl(itemId)
        }
    }
}

// テスト動画URL取得（変更なし）
private fun getTestVideoUrl(itemId: String): String {
    return when (itemId.takeLast(1)) {
        "1" -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        "2" -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
        "3" -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
        "4" -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"
        "5" -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
        else -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
    }
}