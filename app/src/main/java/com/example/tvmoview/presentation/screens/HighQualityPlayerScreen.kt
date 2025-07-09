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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.tvmoview.MainActivity
import com.example.tvmoview.data.prefs.UserPreferences
import com.example.tvmoview.presentation.components.LoadingAnimation
import com.example.tvmoview.presentation.components.VideoTransitionWrapper
import com.example.tvmoview.presentation.viewmodels.SharedExoPlayerViewModel

@Composable
fun HighQualityPlayerScreen(
    itemId: String,
    sharedPlayerViewModel: SharedExoPlayerViewModel,
    onBack: () -> Unit,
    downloadUrl: String = ""
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    val resolvedUrl by produceState<String?>(null, itemId, downloadUrl) {
        value = resolveVideoUrl(itemId, downloadUrl)
    }

    // カスタムシークバー表示制御
    var showCustomSeek by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var seekMessage by remember { mutableStateOf("") }
    var seekForward by remember { mutableStateOf(true) }

    // PlayerView参照用とコントローラー制御
    var playerView by remember { mutableStateOf<PlayerView?>(null) }

    Log.d("VideoPlayer", "🎬 プレイヤー起動: itemId=$itemId")

    // ExoPlayer初期化
    val exoPlayer = remember { sharedPlayerViewModel.initializePlayer(context) }

    LaunchedEffect(Unit) {
        sharedPlayerViewModel.transitionToFullScreen()
    }

    LaunchedEffect(resolvedUrl) {
        resolvedUrl?.let { url ->
            Log.d("VideoPlayer", "📺 動画URL設定: $url")
            val mediaItem = MediaItem.fromUri(url)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            val resume = UserPreferences.getResumePosition(itemId)
            if (resume > 0) exoPlayer.seekTo(resume)
            exoPlayer.playWhenReady = true
        }
        playerView?.player = exoPlayer
    }
    LaunchedEffect(playerView) {
        playerView?.player = exoPlayer
    }

    // カスタムシークバー表示コルーチン
    fun showSeekBarTemporarily(forward: Boolean, message: String, durationMillis: Long = 1000L) {
        exoPlayer.let {
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
        while (true) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration
            delay(500)
        }
    }

    // クリーンアップ
    DisposableEffect(Unit) {
        onDispose {
            val pos = exoPlayer.currentPosition
            val dur = exoPlayer.duration
            if (dur - pos > 3000) {
                UserPreferences.setResumePosition(itemId, pos)
            } else {
                UserPreferences.clearResumePosition(itemId)
            }
            sharedPlayerViewModel.exitFullScreen()
        }
    }

    // フォーカス設定
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // 戻るボタンで即終了
    BackHandler {
        val pos = exoPlayer.currentPosition
        val dur = exoPlayer.duration
        if (dur - pos > 3000) {
            UserPreferences.setResumePosition(itemId, pos)
        } else {
            UserPreferences.clearResumePosition(itemId)
        }
        sharedPlayerViewModel.exitFullScreen()
        onBack()
    }

    val isFullScreen by sharedPlayerViewModel.isFullScreen.collectAsState()

    VideoTransitionWrapper(isFullScreen = isFullScreen) {
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
                            val newPosition = exoPlayer.currentPosition + 10000
                            exoPlayer.seekTo(newPosition)
                            showSeekBarTemporarily(true, "+10秒")
                            Log.d("VideoPlayer", "⏩ 10秒進む: ${newPosition}ms")
                            true
                        }
                        // 📺 TVリモコン：左ボタン（10秒戻る）
                        Key.DirectionLeft -> {
                            val newPosition = maxOf(0, exoPlayer.currentPosition - 10000)
                            exoPlayer.seekTo(newPosition)
                            showSeekBarTemporarily(false, "-10秒")
                            Log.d("VideoPlayer", "⏪ 10秒戻る: ${newPosition}ms")
                            true
                        }
                        // 📺 TVリモコン：上ボタン（音量上げる）
                        Key.DirectionUp -> {
                            val currentVolume = exoPlayer.volume
                            val newVolume = minOf(1.0f, currentVolume + 0.1f)
                            exoPlayer.volume = newVolume
                            Log.d("VideoPlayer", "🔊 音量上げる: $newVolume")
                            true
                        }
                        // 📺 TVリモコン：下ボタン（音量下げる）
                        Key.DirectionDown -> {
                            val currentVolume = exoPlayer.volume
                            val newVolume = maxOf(0.0f, currentVolume - 0.1f)
                            exoPlayer.volume = newVolume
                            Log.d("VideoPlayer", "🔉 音量下げる: $newVolume")
                            true
                        }
                        // 📺 TVリモコン：決定ボタン/再生停止ボタン
                        Key.DirectionCenter, Key.Enter, Key.MediaPlayPause -> {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                                Log.d("VideoPlayer", "⏸️ 一時停止")
                            } else {
                                exoPlayer.play()
                                Log.d("VideoPlayer", "▶️ 再生開始")
                            }
                            true
                        }
                        // 📺 TVリモコン：戻るボタン
                        Key.Back, Key.Escape -> {
                            val pos = exoPlayer.currentPosition
                            val dur = exoPlayer.duration
                            if (dur - pos > 3000) {
                                UserPreferences.setResumePosition(itemId, pos)
                            } else {
                                UserPreferences.clearResumePosition(itemId)
                            }
                            sharedPlayerViewModel.exitFullScreen()
                            onBack()
                            true
                        }
                        // キーボード用（開発時）
                        Key.Spacebar -> {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
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


}

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
}
