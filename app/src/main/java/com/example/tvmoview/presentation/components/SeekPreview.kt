package com.example.tvmoview.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
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

    DisposableEffect(previewUrl) {
        previewPlayer = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(previewUrl))
            prepare()
            seekTo(seekPosition)
            playWhenReady = false
        }
        onDispose { previewPlayer?.release() }
    }

    AndroidView(
        factory = { PlayerView(it).apply {
            player = previewPlayer
            useController = false
        } },
        modifier = modifier.size(160.dp, 90.dp)
    )
}
