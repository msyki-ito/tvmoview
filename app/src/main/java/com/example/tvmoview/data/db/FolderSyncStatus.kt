package com.example.tvmoview.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folder_sync_status")
data class FolderSyncStatus(
    @PrimaryKey val folderId: String,
    val lastSyncAt: Long
)
