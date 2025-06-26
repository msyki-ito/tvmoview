package com.example.tvmoview.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedMediaItemDao {
    @Query("SELECT * FROM cached_media_items WHERE (:folderId IS NULL AND folderId IS NULL) OR folderId = :folderId ORDER BY name")
    fun observeFolder(folderId: String?): Flow<List<CachedMediaItemEntity>>

    @Query("SELECT * FROM cached_media_items WHERE (:folderId IS NULL AND folderId IS NULL) OR folderId = :folderId")
    suspend fun list(folderId: String?): List<CachedMediaItemEntity>

    @Query("SELECT COUNT(*) FROM cached_media_items WHERE (:folderId IS NULL AND folderId IS NULL) OR folderId = :folderId")
    suspend fun folderCount(folderId: String?): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedMediaItemEntity>)

    @Query("DELETE FROM cached_media_items WHERE (:folderId IS NULL AND folderId IS NULL) OR folderId = :folderId")
    suspend fun clearFolder(folderId: String?)

    @Transaction
    suspend fun replaceFolder(folderId: String?, items: List<CachedMediaItemEntity>) {
        clearFolder(folderId)
        insertAll(items)
    }

    @Query("SELECT COUNT(*) FROM cached_media_items")
    suspend fun count(): Int

    @Query("DELETE FROM cached_media_items WHERE id IN (SELECT id FROM cached_media_items ORDER BY lastAccessedAt ASC LIMIT :limit)")
    suspend fun deleteOldest(limit: Int)

    @Query("DELETE FROM cached_media_items WHERE id IN (SELECT id FROM cached_media_items WHERE ((:folderId IS NULL AND folderId IS NULL) OR folderId = :folderId) ORDER BY lastAccessedAt ASC LIMIT :limit)")
    suspend fun deleteOldestInFolder(folderId: String?, limit: Int)
}
