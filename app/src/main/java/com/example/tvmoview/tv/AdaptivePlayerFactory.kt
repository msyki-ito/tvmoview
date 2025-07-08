package com.example.tvmoview.tv

import android.content.Context
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

object AdaptivePlayerFactory {
    fun create(context: Context): ExoPlayer {
        val trackSelector = DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setMaxVideoSizeSd()
                .setForceLowestBitrate(true)
                .setPreferredVideoMimeTypes(
                    MimeTypes.VIDEO_H265,
                    MimeTypes.VIDEO_AV1,
                    MimeTypes.VIDEO_H264
                )
                .build()
        }
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                5_000,
                30_000,
                300,
                1_000
            )
            .build()
        val bandwidthMeter = DefaultBandwidthMeter.Builder(context)
            .setInitialBitrateEstimate(3_000_000)
            .build()
        return ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setBandwidthMeter(bandwidthMeter)
            .build()
    }

    fun forceLowResTemporarily(player: ExoPlayer, durationMs: Long = 2000L) {
        val selector = player.trackSelector as? DefaultTrackSelector ?: return
        selector.parameters = selector.parameters.buildUpon()
            .setMaxVideoSizeSd()
            .setForceLowestBitrate(true)
            .build()
        CoroutineScope(Dispatchers.Main).launch {
            delay(durationMs)
            selector.parameters = selector.parameters.buildUpon()
                .clearVideoSizeConstraints()
                .setForceLowestBitrate(false)
                .build()
        }
    }
}

