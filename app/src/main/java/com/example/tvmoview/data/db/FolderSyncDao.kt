package com.example.tvmoview.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FolderSyncDao {
    @Query("SELECT lastSyncAt FROM folder_sync_status WHERE folderId = :folderId")
    suspend fun lastSyncAt(folderId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(status: FolderSyncStatus)
}
