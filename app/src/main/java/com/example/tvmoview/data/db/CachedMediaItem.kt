package com.example.tvmoview.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class CachedMediaItem(
    @PrimaryKey val id: String,
    val parentId: String?,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val mimeType: String?,
    val isFolder: Boolean,
    val thumbnailUrl: String?,
    val downloadUrl: String?,
    val lastAccessedAt: Long = System.currentTimeMillis()
)
