package com.example.tvmoview.data.repository

import android.util.Log
import com.example.tvmoview.data.local.CachedMediaItemDao
import com.example.tvmoview.data.local.FolderSyncStatusDao
import com.example.tvmoview.data.local.toDomain
import com.example.tvmoview.data.local.toEntity
import com.example.tvmoview.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class CacheAwareMediaRepository(
    private val oneDriveRepository: OneDriveRepository,
    private val itemDao: CachedMediaItemDao,
    private val statusDao: FolderSyncStatusDao
) {
    private val fetchIntervalMs = TimeUnit.MINUTES.toMillis(10)

    fun getFolderItems(folderId: String?, force: Boolean): Flow<List<MediaItem>> = flow {
        val cached = itemDao.list(folderId).map { it.toDomain() }
        emit(cached)

        if (shouldFetch(folderId, force)) {
            val items = if (folderId == null) {
                oneDriveRepository.getRootItems()
            } else {
                oneDriveRepository.getFolderItems(folderId)
            }
            val entities = items.map { it.toEntity(folderId) }
            itemDao.replaceFolder(folderId, entities)
            statusDao.upsert(
                com.example.tvmoview.data.local.FolderSyncStatusEntity(
                    folderId = folderId ?: "root",
                    lastSyncAt = System.currentTimeMillis(),
                    etag = null,
                    itemCount = items.size,
                    syncInProgress = false,
                    lastError = null
                )
            )
            emit(items)
        }
    }

    private suspend fun shouldFetch(folderId: String?, force: Boolean): Boolean {
        if (force) return true
        val status = statusDao.get(folderId ?: "root")
        val elapsed = System.currentTimeMillis() - (status?.lastSyncAt ?: 0L)
        return elapsed > fetchIntervalMs
    }
}
