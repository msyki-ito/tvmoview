package com.example.tvmoview.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CachedMediaItemEntity::class, FolderSyncStatusEntity::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cachedMediaItemDao(): CachedMediaItemDao
    abstract fun folderSyncStatusDao(): FolderSyncStatusDao
}
