package com.example.tvmoview.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cached_media_items",
    indices = [Index(value = ["folderId", "id"])]
)
data class CachedMediaItemEntity(
    @PrimaryKey val id: String,
    val folderId: String?,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val mimeType: String?,
    val isFolder: Boolean,
    val thumbnailUrl: String?,
    val downloadUrl: String?,
    val lastAccessedAt: Long
)
