package com.example.tvmoview.thumbnail

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.annotation.WorkerThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * One-pass thumbnail extractor that caches all frames in memory.
 */
object UltraFastThumbnailExtractor {

    private val cache = ConcurrentHashMap<Pair<String, Int>, Bitmap>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Prewarm cache by decoding every frame on a background thread.
     * @param intervalMs Interval in milliseconds between snapshots.
     */
    fun prewarm(
        source: String,
        intervalMs: Long = 10_000L,
        maxW: Int = 300,
        maxH: Int = 180
    ) {
        if (cache.keys.any { it.first == source }) return
        scope.launch {
            runCatching { decodeAll(source, intervalMs, maxW, maxH) }
        }
    }

    /**
     * Returns cached bitmap if available. Null until generated.
     */
    fun get(source: String, timeMs: Long, intervalMs: Long = 10_000L): Bitmap? =
        cache[source to (timeMs / intervalMs).toInt()]

    @WorkerThread
    private fun decodeAll(source: String, intervalMs: Long, maxW: Int, maxH: Int) {
        val extractor = MediaExtractor().apply { setDataSource(source) }
        val track = (0 until extractor.trackCount).first {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)!!.startsWith("video/")
        }
        val format = extractor.getTrackFormat(track).apply {
            setInteger(MediaFormat.KEY_MAX_WIDTH, maxW)
            setInteger(MediaFormat.KEY_MAX_HEIGHT, maxH)
        }
        extractor.selectTrack(track)

        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        val reader = ImageReader.newInstance(maxW, maxH, PixelFormat.RGBA_8888, 2)
        codec.configure(format, reader.surface, null, 0)

        var completed = false

        codec.setCallback(object : MediaCodec.Callback() {
            private var nextSnapshotUs = 0L
            private var frameIndex = 0

            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                val buf = codec.getInputBuffer(index) ?: return
                val sz = extractor.readSampleData(buf, 0)
                if (sz < 0) {
                    codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                } else {
                    codec.queueInputBuffer(index, 0, sz, extractor.sampleTime, 0)
                    extractor.advance()
                }
            }

            override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                val tUs = info.presentationTimeUs
                val needSnap = tUs >= nextSnapshotUs
                val end = (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                codec.releaseOutputBuffer(index, true)
                if (needSnap) {
                    grab(reader)?.let { bmp ->
                        cache[source to frameIndex] = bmp
                    }
                    frameIndex++
                    nextSnapshotUs += intervalMs * 1_000L
                }
                if (end) completed = true
            }

            override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {}
            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {}
        })
        codec.start()
        while (!completed) {
            Thread.sleep(50)
        }
        codec.stop()
        codec.release()
        extractor.release()
        reader.close()
    }

    private fun grab(reader: ImageReader): Bitmap? {
        val img = reader.acquireLatestImage() ?: return null
        val bmp = Bitmap.createBitmap(img.width, img.height, Bitmap.Config.ARGB_8888)
        val buffer = img.planes[0].buffer
        buffer.rewind()
        bmp.copyPixelsFromBuffer(buffer)
        img.close()
        return bmp
    }
}
