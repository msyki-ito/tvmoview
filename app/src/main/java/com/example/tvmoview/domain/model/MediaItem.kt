package com.example.tvmoview.domain.model

import java.util.*

data class MediaItem(
    val id: String,
    val name: String,
    val size: Long = 0,
    val lastModified: Date = Date(),
    val mimeType: String? = null,
    val isFolder: Boolean = false,
    val thumbnailUrl: String? = null,
    val downloadUrl: String? = null  // OneDrive用ダウンロードURL追加
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
}