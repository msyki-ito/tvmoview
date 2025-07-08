package com.example.tvmoview.thumbnail

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import androidx.annotation.WorkerThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

/**
 * One-pass thumbnail extractor that caches all frames in memory.
 */
object UltraFastThumbnailExtractor {
    private const val TAG = "UltraFastThumb"

    private val cache = ConcurrentHashMap<Pair<String, Int>, Bitmap>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val prewarmingUrls = mutableSetOf<String>()

    /**
     * Prewarm cache by decoding every frame on a background thread.
     * @param intervalMs Interval in milliseconds between snapshots.
     */
    fun prewarm(
        source: String,
        intervalMs: Long = 10_000L,
        maxW: Int = 320,
        maxH: Int = 180
    ) {
        // 既にキャッシュ済みまたはプリウォーム中の場合はスキップ
        if (cache.keys.any { it.first == source } || !prewarmingUrls.add(source)) return

        scope.launch {
            try {
                Log.d(TAG, "Starting prewarm for: $source")
                decodeAll(source, intervalMs, maxW, maxH)
            } catch (e: Exception) {
                Log.e(TAG, "Prewarm failed for $source", e)
            } finally {
                prewarmingUrls.remove(source)
            }
        }
    }

    /**
     * Returns cached bitmap if available. Null until generated.
     */
    fun get(source: String, timeMs: Long, intervalMs: Long = 10_000L): Bitmap? {
        val index = (timeMs / intervalMs).toInt()
        return cache[source to index]
    }

    @WorkerThread
    private fun decodeAll(source: String, intervalMs: Long, maxW: Int, maxH: Int) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        var reader: ImageReader? = null

        try {
            extractor.setDataSource(source)

            // ビデオトラックを探す
            val trackIndex = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
            } ?: throw IllegalArgumentException("No video track found")

            val format = extractor.getTrackFormat(trackIndex)
            extractor.selectTrack(trackIndex)

            // 元の解像度を取得
            val width = format.getInteger(MediaFormat.KEY_WIDTH)
            val height = format.getInteger(MediaFormat.KEY_HEIGHT)

            // アスペクト比を維持しながらリサイズ
            val scale = minOf(maxW.toFloat() / width, maxH.toFloat() / height)
            val scaledW = (width * scale).toInt()
            val scaledH = (height * scale).toInt()

            Log.d(TAG, "Video: ${width}x${height} -> ${scaledW}x${scaledH}")

            // RGBA_8888フォーマットでImageReaderを作成
            reader = ImageReader.newInstance(scaledW, scaledH, PixelFormat.RGBA_8888, 2)

            val mime = format.getString(MediaFormat.KEY_MIME)!!
            codec = MediaCodec.createDecoderByType(mime)

            var frameIndex = 0
            var nextSnapshotUs = 0L
            var inputFinished = false
            var outputFinished = false

            // Surface経由でのレンダリング設定
            codec.configure(format, reader.surface, null, 0)
            codec.start()

            // ImageReaderのコールバック設定
//            reader.setOnImageAvailableListener({ imageReader ->
//                val image = imageReader.acquireLatestImage()
//                if (image != null) {
//                    try {
//                        val planes = image.planes
//                        val buffer = planes[0].buffer
//                        val pixelStride = planes[0].pixelStride
//                        val rowStride = planes[0].rowStride
//                        val rowPadding = rowStride - pixelStride * scaledW
//
//                        val bitmap = Bitmap.createBitmap(
//                            scaledW + rowPadding / pixelStride,
//                            scaledH,
//                            Bitmap.Config.ARGB_8888
//                        )
//                        bitmap.copyPixelsFromBuffer(buffer)
//
//                        // 必要に応じてクロップ
//                        val croppedBitmap = if (rowPadding > 0) {
//                            Bitmap.createBitmap(bitmap, 0, 0, scaledW, scaledH)
//                        } else {
//                            bitmap
//                        }
//
//                        cache[source to frameIndex] = croppedBitmap
//                        Log.d(TAG, "Captured frame $frameIndex")
//                        frameIndex++
//                    } catch (e: Exception) {
//                        Log.e(TAG, "Error processing image", e)
//                    } finally {
//                        image.close()
//                    }
//                }
//            }, null)

            // デコードループ
            val bufferInfo = MediaCodec.BufferInfo()
            var seekCount = 0

            while (!outputFinished) {
                // 入力処理
                if (!inputFinished) {
                    val inputIndex = codec.dequeueInputBuffer(10000)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)!!
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)

                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputFinished = true
                        } else {
                            val presentationTimeUs = extractor.sampleTime
                            codec.queueInputBuffer(inputIndex, 0, sampleSize, presentationTimeUs, 0)

                            // 次のキーフレームまでシーク
                            if (presentationTimeUs >= nextSnapshotUs) {
                                nextSnapshotUs += intervalMs * 1000L
                                val seekTarget = nextSnapshotUs

                                // シーク実行
                                extractor.seekTo(seekTarget, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                                seekCount++

                                if (seekCount > 100) { // 安全のため上限を設定
                                    inputFinished = true
                                }
                            } else {
                                extractor.advance()
                            }
                        }
                    }
                }

                // 出力処理
                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                when {
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        Log.d(TAG, "Output format changed: ${codec.outputFormat}")
                    }
                    outputIndex >= 0 -> {
                        val render = bufferInfo.presentationTimeUs >= (frameIndex * intervalMs * 1000L - 100000) // 100ms の余裕
                        codec.releaseOutputBuffer(outputIndex, render)

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputFinished = true
                        }
                    }
                }

                // タイムアウト防止
                if (System.currentTimeMillis() - System.currentTimeMillis() > 30000) {
                    Log.w(TAG, "Timeout reached, stopping decode")
                    break
                }
            }

            Log.d(TAG, "Decoding completed. Total frames: $frameIndex")

        } catch (e: Exception) {
            Log.e(TAG, "Decoding failed", e)
            throw e
        } finally {
            try {
                codec?.stop()
                codec?.release()
                extractor.release()
                reader?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Cleanup error", e)
            }
        }
    }

    /**
     * キャッシュをクリア
     */
    fun clearCache(source: String? = null) {
        if (source != null) {
            cache.keys.removeIf { it.first == source }
        } else {
            cache.clear()
        }
    }

    /**
     * デバッグ用：キャッシュ状態を取得
     */
    fun getCacheInfo(): String {
        return "Cache size: ${cache.size}, URLs: ${cache.keys.map { it.first }.distinct()}"
    }
}