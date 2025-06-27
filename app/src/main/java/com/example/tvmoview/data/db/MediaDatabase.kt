package com.example.tvmoview.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CachedMediaItem::class, FolderSyncStatus::class], version = 1)
abstract class MediaDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun folderSyncDao(): FolderSyncDao
}
