package com.example.tvmoview.presentation.screens

import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.example.tvmoview.MainActivity

@Composable
fun HighQualityPlayerScreen(
    itemId: String,
    onBack: () -> Unit,
    downloadUrl: String = ""
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    val startPosition = remember {
        com.example.tvmoview.data.prefs.UserPreferences.getPlaybackPosition(itemId)
    }

    val resolvedUrl by produceState<String?>(null, itemId, downloadUrl) {
        value = resolveVideoUrl(itemId, downloadUrl)
    }

    // カスタムシークバー表示制御
    var showCustomSeek by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var seekMessage by remember { mutableStateOf("") }

    // PlayerView参照用とコントローラー制御
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    Log.d("VideoPlayer", "🎬 プレイヤー起動: itemId=$itemId")

    // ExoPlayer初期化
    val exoPlayer = remember(resolvedUrl) {
        resolvedUrl?.takeIf { it.isNotBlank() }?.let { url ->
            ExoPlayer.Builder(context).build().also { player ->
                Log.d("VideoPlayer", "📺 動画URL設定: $url")
                val mediaItem = MediaItem.fromUri(url)
                player.setMediaItem(mediaItem)
                player.prepare()
                if (startPosition > 0) player.seekTo(startPosition)
                player.playWhenReady = true
            }
        }
    }

    // カスタムシークバー表示コルーチン
    fun showSeekBarTemporarily(message: String, displayMs: Long = 1000L) {
        exoPlayer?.let {
            currentPosition = it.currentPosition
            duration = it.duration
        }
        seekMessage = message
        showCustomSeek = true

        // 一定時間後に自動非表示
        coroutineScope.launch {
            delay(displayMs)
            showCustomSeek = false
        }
    }

    // クリーンアップ
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer?.let {
                com.example.tvmoview.data.prefs.UserPreferences.setPlaybackPosition(
                    itemId,
                    it.currentPosition
                )
            }
            Log.d("VideoPlayer", "🧹 ExoPlayer解放")
            exoPlayer?.release()
        }
    }

    // フォーカス設定
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // 戻るボタン制御（ダブルプレス方式）
    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 3000) {
            Log.d("VideoPlayer", "🔙 画面を戻る（ダブルプレス）")
            onBack()
        } else {
            exoPlayer?.pause()
            showSeekBarTemporarily("", 3000)
            lastBackPressTime = currentTime
            Log.d("VideoPlayer", "⏸️ 一時停止してシーク表示")
        }
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
                            showSeekBarTemporarily("⏩ +10秒")
                            Log.d("VideoPlayer", "⏩ 10秒進む: ${newPosition}ms")
                            true
                        }
                        // 📺 TVリモコン：左ボタン（10秒戻る）
                        Key.DirectionLeft -> {
                            val newPosition = maxOf(0, (exoPlayer?.currentPosition ?: 0) - 10000)
                            exoPlayer?.seekTo(newPosition)
                            showSeekBarTemporarily("⏪ -10秒")
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
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastBackPressTime < 3000) {
                                Log.d("VideoPlayer", "🔙 戻るボタン（ダブルプレス）")
                                onBack()
                            } else {
                                exoPlayer?.pause()
                                showSeekBarTemporarily("", 3000)
                                lastBackPressTime = currentTime
                                Log.d("VideoPlayer", "⏸️ 一時停止してシーク表示")
                            }
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
        // ExoPlayer表示
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = true
                    setShowSubtitleButton(true)
                    setShowVrButton(false)
                    playerView = this
                    player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view -> view.player = exoPlayer }
        )

        if (resolvedUrl == null ||
            exoPlayer?.playbackState == Player.STATE_BUFFERING ||
            exoPlayer?.playbackState == Player.STATE_IDLE
        ) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        }

        // カスタムシークバー（一時表示）
        if (showCustomSeek && duration > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 32.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // シークメッセージ
                        Text(
                            text = seekMessage,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // プログレスバー
                        LinearProgressIndicator(
                            progress = if (duration > 0) {
                                (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                            } else 0f,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.Gray.copy(alpha = 0.3f)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 時間表示
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(currentPosition),
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = formatTime(duration),
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
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