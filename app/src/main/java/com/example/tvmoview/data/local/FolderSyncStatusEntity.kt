package com.example.tvmoview.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folder_sync_status")
data class FolderSyncStatusEntity(
    @PrimaryKey val folderId: String,
    val lastSyncAt: Long,
    val etag: String?,
    val itemCount: Int,
    val syncInProgress: Boolean,
    val lastError: String?
)
