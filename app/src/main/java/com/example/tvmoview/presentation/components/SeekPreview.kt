package com.example.tvmoview.presentation.components

import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun SeekPreview(
    previewUrl: String,
    seekPosition: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var previewPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    val tag = "SeekPreview"

    DisposableEffect(previewUrl) {
        Log.d(tag, "init player with $previewUrl at $seekPosition")
        previewPlayer = ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    Log.d(tag, "state=$state")
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e(tag, "error", error)
                }
            })
            setMediaItem(MediaItem.fromUri(previewUrl))
            prepare()
            seekTo(seekPosition)
            playWhenReady = false
        }
        onDispose {
            Log.d(tag, "release player")
            previewPlayer?.release()
        }
    }

    LaunchedEffect(seekPosition) {
        Log.d(tag, "seekTo $seekPosition")
        previewPlayer?.seekTo(seekPosition)
    }

    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                useController = false
            }
        },
        update = { view ->
            Log.d(tag, "attach player")
            view.player = previewPlayer
        },
        modifier = modifier.size(160.dp, 90.dp)
    )
}
