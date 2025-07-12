package com.example.tvmoview.presentation.screens

import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.common.Player
import com.example.tvmoview.domain.model.MediaItem as DomainMediaItem
import com.example.tvmoview.MainActivity
import com.example.tvmoview.data.prefs.UserPreferences
import com.example.tvmoview.presentation.components.LoadingAnimation
import com.example.tvmoview.presentation.viewmodels.MediaBrowserViewModel
import com.example.tvmoview.presentation.player.SharedPlayerManager

@Composable
fun HighQualityPlayerScreen(
    itemId: String,
    onBack: () -> Unit,
    viewModel: MediaBrowserViewModel,
    downloadUrl: String = ""
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    val resolvedUrl by produceState<String?>(null, itemId) {
        val cachedUrl = viewModel.currentVideoUrl.value
        if (cachedUrl != null && SharedPlayerManager.currentVideoId.value == itemId) {
            value = cachedUrl
        } else {
            value = resolveVideoUrl(itemId, downloadUrl)
        }
    }

    val itemInfo by produceState<DomainMediaItem?>(null, itemId) {
        value = MainActivity.oneDriveRepository.getItemById(itemId)
    }

    // カスタムシークバー表示制御
    var showCustomSeek by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var seekMessage by remember { mutableStateOf("") }
    var seekForward by remember { mutableStateOf(true) }

    var showCover by remember { mutableStateOf(SharedPlayerManager.currentVideoId.value != itemId) }
    var bufferProgress by remember { mutableFloatStateOf(0f) }

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
        val transferredPlayer = SharedPlayerManager.transferPlayer()
        val isTransferred = transferredPlayer != null && SharedPlayerManager.currentVideoId.value == itemId

        exoPlayer = if (isTransferred) {
            showCover = false
            transferredPlayer!!.apply {
                volume = 1f
                repeatMode = ExoPlayer.REPEAT_MODE_OFF
            }
        } else {
            showCover = true
            resolvedUrl?.let { url ->
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(url))
                    prepare()
                    val previewPos = viewModel.getAndClearPreviewPosition(itemId)
                    val savedPos = UserPreferences.getResumePosition(itemId)
                    val resume = if (previewPos > 0) previewPos else savedPos
                    if (resume > 0) {
                        seekTo(resume)
                        Log.d("VideoPlayer", "⏩ 再開位置 $resume")
                    }
                    playWhenReady = true
                }
            }
        }
        playerView?.player = exoPlayer
        viewModel.setFullscreenTransition(false)
    }
    LaunchedEffect(playerView, exoPlayer) {
        playerView?.player = exoPlayer
    }

    LaunchedEffect(exoPlayer, showCover) {
        val player = exoPlayer
        if (player != null && showCover) {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) showCover = false
                }
            }
            player.addListener(listener)
            while (showCover) {
                bufferProgress = player.bufferedPercentage / 100f
                delay(100)
            }
            player.removeListener(listener)
        }
    }

    // カスタムシークバー表示コルーチン
    fun showSeekBarTemporarily(forward: Boolean, message: String, durationMillis: Long = 1000L) {
        exoPlayer?.let {
            currentPosition = it.currentPosition
            duration = it.duration
        }
        seekForward = forward
        seekMessage = message
        showCustomSeek = true

        // 指定時間後に自動非表示
        coroutineScope.launch {
            delay(durationMillis)
            showCustomSeek = false
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
                        // 📺 TVリモコン：右ボタン（10秒進む）
                        Key.DirectionRight -> {
                            val newPosition = exoPlayer?.currentPosition?.plus(10000) ?: 0
                            exoPlayer?.seekTo(newPosition)
                            showSeekBarTemporarily(true, "+10秒")
                            Log.d("VideoPlayer", "⏩ 10秒進む: ${newPosition}ms")
                            true
                        }
                        // 📺 TVリモコン：左ボタン（10秒戻る）
                        Key.DirectionLeft -> {
                            val newPosition = maxOf(0, (exoPlayer?.currentPosition ?: 0) - 10000)
                            exoPlayer?.seekTo(newPosition)
                            showSeekBarTemporarily(false, "-10秒")
                            Log.d("VideoPlayer", "⏪ 10秒戻る: ${newPosition}ms")
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
                        // 📺 TVリモコン：決定ボタン/再生停止ボタン
                        Key.DirectionCenter, Key.Enter, Key.MediaPlayPause -> {
                            if (exoPlayer?.isPlaying == true) {
                                exoPlayer?.pause()
                                Log.d("VideoPlayer", "⏸️ 一時停止")
                            } else {
                                exoPlayer?.play()
                                Log.d("VideoPlayer", "▶️ 再生開始")
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
                        useController = true
                        setShowSubtitleButton(true)
                        setShowVrButton(false)
                        playerView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } ?: LoadingAnimation()

        if (showCover) {
            val thumb = itemInfo?.thumbnailUrl
            thumb?.let { url ->
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(url)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
            LinearProgressIndicator(
                progress = bufferProgress,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }



        // カスタムシークバー（一時表示）
        if (showCustomSeek && duration > 0) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(vertical = 8.dp, horizontal = 32.dp)
            ) {
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (seekForward) Icons.Default.FastForward else Icons.Default.FastRewind,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = seekMessage,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                LinearProgressIndicator(
                    progress = if (duration > 0) {
                        (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                    } else 0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.DarkGray
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = formatTime(duration),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
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