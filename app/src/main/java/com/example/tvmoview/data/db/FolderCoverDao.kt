package com.example.tvmoview.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FolderCoverDao {
    @Query("SELECT itemId FROM folder_covers WHERE folderId = :folderId")
    suspend fun getCoverItemId(folderId: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setCover(cover: FolderCover)
}
