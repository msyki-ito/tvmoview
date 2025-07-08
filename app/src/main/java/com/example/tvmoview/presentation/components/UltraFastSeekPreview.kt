package com.example.tvmoview.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.example.tvmoview.thumbnail.UltraFastThumbnailExtractor
import kotlinx.coroutines.delay

@Composable
fun UltraFastSeekPreview(
    videoUrl: String,
    seekPosition: Long,
    modifier: Modifier = Modifier,
    intervalMs: Long = 10_000L
) {
    val thumb by produceState<android.graphics.Bitmap?>(null, videoUrl, seekPosition) {
        UltraFastThumbnailExtractor.prewarm(videoUrl, intervalMs)
        while (value == null) {
            value = UltraFastThumbnailExtractor.get(videoUrl, seekPosition, intervalMs)
            if (value == null) delay(40)
        }
    }

    Box(
        modifier = modifier
            .size(320.dp, 180.dp)
            .background(Color.Black, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        thumb?.let { bmp ->
            androidx.compose.foundation.Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(320.dp, 180.dp)
            )
        } ?: CircularProgressIndicator(strokeWidth = 4.dp)
    }
}
