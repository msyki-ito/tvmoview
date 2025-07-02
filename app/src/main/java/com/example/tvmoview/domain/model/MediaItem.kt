package com.example.tvmoview.domain.model

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.*

data class MediaItem(
    val id: String,
    val name: String,
    val size: Long = 0,
    val lastModified: Date = Date(),
    val mimeType: String? = null,
    val isFolder: Boolean = false,
    val thumbnailUrl: String? = null,
    val downloadUrl: String? = null,  // OneDrive用ダウンロードURL追加
    val duration: Long = 0L
) {
    val isVideo: Boolean
        get() = mimeType?.startsWith("video/") == true

    val isImage: Boolean
        get() = mimeType?.startsWith("image/") == true

    val fileExtension: String
        get() = name.substringAfterLast(".", "")

    val formattedSize: String
        get() = when {
            size < 1024 -> "${size}B"
            size < 1024 * 1024 -> "${size / 1024}KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)}MB"
            else -> "${size / (1024 * 1024 * 1024)}GB"
        }

    val isVerticalMedia: Boolean
        get() {
            val verticalKeywords = listOf("portrait", "vertical", "縦", "9x16", "3x4")
            return verticalKeywords.any { name.contains(it, ignoreCase = true) }
        }

    val displayAspectRatio: Float
        get() = when {
            isFolder -> 1f
            isVerticalMedia -> 1f
            isVideo -> 16f / 9f
            isImage -> 4f / 3f
            else -> 1f
        }

    companion object {
        // 横動画(16:9)の高さを基準に全サムネイルを統一
        val BaseCardHeight: Dp = 108.dp
    }

    val cardHeight: Dp
        get() = BaseCardHeight
}