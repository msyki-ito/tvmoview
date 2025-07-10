package com.example.tvmoview.presentation.player

import android.util.Log
import androidx.media3.common.Format

object PlaybackTimingLogger {
    private const val TAG = "PlaybackTiming"
    private var baseTime = 0L

    fun start() {
        baseTime = System.currentTimeMillis()
        Log.d(TAG, "[1] タップ時刻: 0ms")
    }

    fun log(step: Int, message: String) {
        if (baseTime == 0L) return
        val delta = System.currentTimeMillis() - baseTime
        Log.d(TAG, "[$step] $message: +${delta}ms")
    }

    fun detail(message: String) {
        if (baseTime != 0L) Log.d(TAG, "    $message")
    }

    fun detailFormat(format: Format?) {
        format ?: return
        detail("解像度: ${format.width}×${format.height}")
        detail("Bitrate: ${format.bitrate.div(1000)} kbps")
        detail("コーデック: ${format.codecs}")
    }

    fun reset() {
        baseTime = 0L
    }
}
