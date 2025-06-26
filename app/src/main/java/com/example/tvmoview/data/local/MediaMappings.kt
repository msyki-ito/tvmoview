package com.example.tvmoview.data.local

import com.example.tvmoview.domain.model.MediaItem
import java.util.Date

fun CachedMediaItemEntity.toDomain(): MediaItem = MediaItem(
    id = id,
    name = name,
    size = size,
    lastModified = Date(lastModified),
    mimeType = mimeType,
    isFolder = isFolder,
    thumbnailUrl = thumbnailUrl,
    downloadUrl = downloadUrl
)

fun MediaItem.toEntity(folderId: String?, accessedAt: Long = System.currentTimeMillis()): CachedMediaItemEntity =
    CachedMediaItemEntity(
        id = id,
        folderId = folderId,
        name = name,
        size = size,
        lastModified = lastModified.time,
        mimeType = mimeType,
        isFolder = isFolder,
        thumbnailUrl = thumbnailUrl,
        downloadUrl = downloadUrl,
        lastAccessedAt = accessedAt
    )
