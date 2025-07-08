package com.example.tvmoview.thumbnail

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.view.PixelCopy
import androidx.annotation.WorkerThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToLong

/**
 * One-pass thumbnail extractor that caches all frames in memory.
 */
object UltraFastThumbnailExtractor {

    private val cache = ConcurrentHashMap<Pair<String, Long>, Bitmap>()
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
    fun get(source: String, timeMs: Long): Bitmap? = cache[source to timeMs.roundFrame()]

    private fun Long.roundFrame() = (this / 10_000L) * 10_000L

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
        codec.setCallback(object : MediaCodec.Callback() {
            private var nextSnapshotUs = intervalMs * 1_000L

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
                codec.releaseOutputBuffer(index, true)
                if (needSnap) {
                    grab(reader)?.let { bmp ->
                        cache[source to (nextSnapshotUs / 1_000).roundFrame()] = bmp
                    }
                    nextSnapshotUs += intervalMs * 1_000L
                }
            }

            override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {}
            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {}
        })
        codec.start()
        while (true) {
            Thread.sleep(50)
            if (extractor.sampleTime < 0 && reader.acquireLatestImage() == null) break
        }
        codec.stop();
        codec.release();
        extractor.release();
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
