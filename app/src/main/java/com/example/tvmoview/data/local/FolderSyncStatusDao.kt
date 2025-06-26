package com.example.tvmoview.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FolderSyncStatusDao {
    @Query("SELECT * FROM folder_sync_status WHERE folderId = :folderId")
    suspend fun get(folderId: String): FolderSyncStatusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FolderSyncStatusEntity)
}
