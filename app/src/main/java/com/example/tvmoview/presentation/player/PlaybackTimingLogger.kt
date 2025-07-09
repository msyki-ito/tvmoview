package com.example.tvmoview.presentation.player

import android.util.Log

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

    fun reset() {
        baseTime = 0L
    }
}
