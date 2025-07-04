package com.example.tvmoview.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CachedMediaItem::class, FolderSyncStatus::class, FolderCover::class],
    version = 3,
    exportSchema = false
)
abstract class MediaDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun folderSyncDao(): FolderSyncDao
    abstract fun folderCoverDao(): FolderCoverDao
}
