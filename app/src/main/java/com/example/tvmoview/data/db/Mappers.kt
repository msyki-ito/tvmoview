package com.example.tvmoview.data.db

import com.example.tvmoview.domain.model.MediaItem
import java.util.Date

fun MediaItem.toCached(parentId: String?, accessTime: Long = System.currentTimeMillis()): CachedMediaItem =
    CachedMediaItem(
        id = id,
        parentId = parentId,
        name = name,
        size = size,
        lastModified = lastModified.time,
        mimeType = mimeType,
        isFolder = isFolder,
        thumbnailUrl = thumbnailUrl,
        downloadUrl = downloadUrl,
        lastAccessedAt = accessTime
    )

fun CachedMediaItem.toDomain(): MediaItem =
    MediaItem(
        id = id,
        name = name,
        size = size,
        lastModified = Date(lastModified),
        mimeType = mimeType,
        isFolder = isFolder,
        thumbnailUrl = thumbnailUrl,
        downloadUrl = downloadUrl
    )
