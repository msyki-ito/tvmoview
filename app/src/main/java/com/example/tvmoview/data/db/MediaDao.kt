package com.example.tvmoview.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items WHERE (:folderId IS NULL AND parentId IS NULL) OR parentId = :folderId ORDER BY name")
    fun observe(folderId: String?): Flow<List<CachedMediaItem>>

    @Query("SELECT * FROM media_items WHERE (:folderId IS NULL AND parentId IS NULL) OR parentId = :folderId ORDER BY name")
    suspend fun getItems(folderId: String?): List<CachedMediaItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<CachedMediaItem>)

    @Query("DELETE FROM media_items WHERE (:folderId IS NULL AND parentId IS NULL) OR parentId = :folderId")
    suspend fun clearFolder(folderId: String?)

    @Query("UPDATE media_items SET lastAccessedAt = :time WHERE id IN (:ids)")
    suspend fun updateAccessTime(ids: List<String>, time: Long)

    @Query("DELETE FROM media_items WHERE lastAccessedAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long)

    @Query("SELECT name FROM media_items WHERE id = :id LIMIT 1")
    suspend fun getNameById(id: String): String?

    @Transaction
    suspend fun replaceFolder(folderId: String?, items: List<CachedMediaItem>) {
        clearFolder(folderId)
        insertItems(items)
    }
}
