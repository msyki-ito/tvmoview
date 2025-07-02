package com.example.tvmoview.data.repository

import android.util.Log
import com.example.tvmoview.data.api.OneDriveApiService
import com.example.tvmoview.data.auth.AuthenticationManager
import com.example.tvmoview.data.model.OneDriveItem
import com.example.tvmoview.data.model.OneDriveResult
import com.example.tvmoview.domain.model.MediaItem
import com.example.tvmoview.data.db.MediaDao
import com.example.tvmoview.data.db.FolderSyncDao
import com.example.tvmoview.data.db.FolderSyncStatus
import com.example.tvmoview.data.db.CachedMediaItem
import com.example.tvmoview.data.db.toCached
import com.example.tvmoview.data.db.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class OneDriveRepository(
    private val authManager: AuthenticationManager,
    private val mediaDao: MediaDao,
    private val folderSyncDao: FolderSyncDao
) {

    private val apiService: OneDriveApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://graph.microsoft.com/v1.0/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OneDriveApiService::class.java)
    }

    private val okHttpClient = OkHttpClient()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val ROOT_ID = "root"
    private val mutex = Mutex()
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val syncIntervalMs = 10 * 60 * 1000L

// OneDriveRepository.kt の shouldFetch メソッド修正

    private suspend fun shouldFetch(folderId: String, force: Boolean): Boolean {
        // 手動更新の場合は常に実行
        if (force) {
            Log.d("OneDriveRepository", "shouldFetch: force=true -> true (手動更新)")
            return true
        }

        // キャッシュ確認
        val cachedItems = mediaDao.getItems(folderId)
        val hasCache = cachedItems.isNotEmpty()

        // キャッシュがない場合（初回読み込み）は実行
        val should = !hasCache

        Log.d(
            "OneDriveRepository",
            "shouldFetch(folder=$folderId, force=$force, hasCache=$hasCache) -> $should"
        )

        return should
    }
    suspend fun getCachedItems(folderId: String?): List<MediaItem> = withContext(Dispatchers.IO) {
        val cached = mediaDao.getItems(folderId)
        if (cached.isNotEmpty()) {
            Log.d("OneDriveRepository", "キャッシュ取得: ${cached.size}件")
            mediaDao.updateAccessTime(cached.map { it.id }, System.currentTimeMillis())
        }
        cached.map { it.toDomain() }
    }

    fun getFolderItems(folderId: String? = null, force: Boolean = false): Flow<List<MediaItem>> =
        mediaDao.observe(folderId)
            .onStart {
                val hasCache = mediaDao.getItems(folderId).isNotEmpty()
                if (!hasCache || force) {
                    repoScope.launch { fetchAndCacheItems(folderId) }
                }
            }
            .map { list ->
                list.map { it.toDomain() }
            }

    suspend fun getDownloadUrl(itemId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val token = authManager.getValidToken()
                if (token == null) {
                    Log.w("OneDriveRepo", "トークンなし/期限切れ")
                    return@withContext null
                }

                Log.d("OneDriveRepo", "downloadURL取得開始: $itemId")

                val url = "https://graph.microsoft.com/v1.0/me/drive/items/$itemId?select=id,@microsoft.graph.downloadUrl"

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer ${token.accessToken}")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseBody = response.body?.string()

                Log.d("OneDriveRepo", "Response Code: ${response.code}")
                if (responseBody != null) {
                    Log.d("OneDriveRepo", "Response Body: ${responseBody.take(200)}")
                }

                if (response.isSuccessful && responseBody != null) {
                    val json = JSONObject(responseBody)
                    val downloadUrl = json.optString("@microsoft.graph.downloadUrl")

                    if (downloadUrl.isNotEmpty()) {
                        Log.d("OneDriveRepo", "URL取得成功: ${downloadUrl.take(50)}...")
                        return@withContext downloadUrl
                    }
                }

                Log.e("OneDriveRepo", "URL取得失敗")
                null
            } catch (e: Exception) {
                Log.e("OneDriveRepo", "例外発生", e)
                null
            }
        }
    }

    private suspend fun fetchAndCacheItems(folderId: String?) {
        try {
            val items = fetchAllItems(folderId)
            val mediaItems = items.map { oneDriveItem ->
                MediaItem(
                    id = oneDriveItem.id,
                    name = oneDriveItem.name,
                    size = oneDriveItem.size ?: 0,
                    lastModified = parseDate(oneDriveItem.lastModifiedDateTime),
                    mimeType = oneDriveItem.file?.mimeType,
                    isFolder = oneDriveItem.folder != null,
                    thumbnailUrl = generateThumbnailUrl(oneDriveItem),
                    downloadUrl = null,
                    duration = oneDriveItem.video?.duration ?: 0L
                )
            }
            saveToCache(folderId, mediaItems)
        } catch (e: Exception) {
            Log.e("OneDriveRepo", "API取得エラー", e)
        }
    }

    private suspend fun fetchAllItems(folderId: String?): List<OneDriveItem> {
        val token = authManager.getValidToken() ?: return emptyList()
        val auth = "Bearer ${token.accessToken}"
        val select = "id,name,size,lastModifiedDateTime,file,folder,video"
        val items = mutableListOf<OneDriveItem>()

        var response = if (folderId == null) {
            apiService.getRootItems(auth, select)
        } else {
            apiService.getFolderItems(auth, folderId, select)
        }

        while (response.isSuccessful) {
            val body = response.body() ?: break
            items.addAll(body.items)
            val next = body.nextLink
            response = if (next != null) {
                apiService.getByUrl(next, auth)
            } else break
        }

        return items
    }

    private suspend fun saveToCache(folderId: String?, items: List<MediaItem>) {
        val cached = items.map { item ->
            CachedMediaItem(
                id = item.id,
                parentId = folderId,
                name = item.name,
                size = item.size,
                lastModified = item.lastModified.time,
                mimeType = item.mimeType,
                isFolder = item.isFolder,
                thumbnailUrl = item.thumbnailUrl,
                downloadUrl = null,
                duration = item.duration,
                lastAccessedAt = System.currentTimeMillis()
            )
        }
        mediaDao.replaceFolder(folderId, cached)
        folderSyncDao.upsert(
            FolderSyncStatus(
                folderId = folderId ?: "root",
                lastSyncAt = System.currentTimeMillis()
            )
        )
    }

    private fun generateThumbnailUrl(item: OneDriveItem): String? {
        val isMedia = item.file?.mimeType?.let {
            it.startsWith("image/") || it.startsWith("video/")
        } ?: false
        return if (!isMedia || item.folder != null) null
        else "https://graph.microsoft.com/v1.0/me/drive/items/${item.id}/thumbnails/0/medium/content"
    }

    private fun parseDate(text: String?): Date {
        return try {
            val instant = java.time.Instant.parse(text)
            Date.from(instant)
        } catch (e: Exception) {
            Date()
        }
    }

    private suspend fun getRootItemsResult(): OneDriveResult<List<MediaItem>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("OneDriveRepository", "認証状態確認中...")
                val token = authManager.getValidToken()

                if (token == null) {
                    Log.w("OneDriveRepository", "認証トークンが見つかりません")
                    return@withContext OneDriveResult.Error(Exception("認証が必要です"))
                }

                Log.d("OneDriveRepository", "認証OK - API呼び出し中...")
                val response = apiService.getRootItems("Bearer ${token.accessToken}")

                Log.d("OneDriveRepository", "APIレスポンス: code=${response.code()}")

                if (response.isSuccessful) {
                    val items = response.body()?.items?.map { it.toMediaItem() } ?: emptyList()
                    Log.d("OneDriveRepository", "OneDriveアイテム数: ${items.size}")
                    items.forEach { item ->
                        Log.d("OneDriveRepository", "${item.name} (${if(item.isFolder) "フォルダ" else "ファイル"})")
                    }
                    OneDriveResult.Success(items)
                } else {
                    Log.e("OneDriveRepository", "API呼び出し失敗: ${response.code()} - ${response.message()}")
                    OneDriveResult.Error(Exception("API呼び出しに失敗: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e("OneDriveRepository", "例外発生: ${e.message}", e)
                OneDriveResult.Error(e)
            }
        }
    }

    private suspend fun getFolderItemsResult(folderId: String): OneDriveResult<List<MediaItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val token = authManager.getValidToken()
                if (token == null) {
                    return@withContext OneDriveResult.Error(Exception("認証が必要です"))
                }

                val response = apiService.getFolderItems("Bearer ${token.accessToken}", folderId)
                if (response.isSuccessful) {
                    val items = response.body()?.items?.map { it.toMediaItem() } ?: emptyList()
                    OneDriveResult.Success(items)
                } else {
                    OneDriveResult.Error(Exception("API呼び出しに失敗: ${response.code()}"))
                }
            } catch (e: Exception) {
                OneDriveResult.Error(e)
            }
        }
    }

    fun getCurrentPath(folderId: String?): String {
        return if (folderId == null) {
            "OneDrive"
        } else {
            runBlocking { mediaDao.getNameById(folderId) } ?: "OneDriveフォルダ"
        }
    }

    private suspend fun cacheItems(folderId: String?, items: List<MediaItem>) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        Log.d("OneDriveRepository", "saving ${items.size} items to cache (folder=$folderId)")
        mediaDao.replaceFolder(folderId, items.map { it.toCached(folderId, now) })
        mediaDao.deleteOlderThan(now - 14L * 24 * 60 * 60 * 1000)
        folderSyncDao.upsert(FolderSyncStatus(folderId ?: ROOT_ID, now))
        Log.d("OneDriveRepository", "lastSyncAt updated to $now for folder=${folderId ?: ROOT_ID}")
    }

    private suspend fun sync(folderId: String?) = mutex.withLock {
        Log.d("OneDriveRepository", "start sync for folder=${folderId ?: ROOT_ID}")
        val result = if (folderId == null) getRootItemsResult() else getFolderItemsResult(folderId)
        if (result is OneDriveResult.Success) {
            val itemsWithUrls = result.data.map { item ->
                when {
                    item.isVideo || item.isImage -> {
                        val downloadUrl = getDownloadUrl(item.id)
                        val duration = if (item.isVideo && downloadUrl != null) {
                            fetchDuration(downloadUrl)
                        } else 0L
                        item.copy(
                            downloadUrl = downloadUrl,
                            thumbnailUrl = generateThumbnailUrl(item),
                            duration = duration
                        )
                    }
                    else -> item
                }
            }
            cacheItems(folderId, itemsWithUrls)
            Log.d("OneDriveRepository", "sync success: ${itemsWithUrls.size} items")
        } else if (result is OneDriveResult.Error) {
            Log.e("OneDriveRepository", "sync error: ${result.exception.message}")
        }
    }

    private fun generateThumbnailUrl(item: MediaItem): String? {
        return if (!item.isFolder && (item.isImage || item.isVideo)) {
            "https://graph.microsoft.com/v1.0/me/drive/items/${item.id}/thumbnails/0/medium/content"
        } else {
            null
        }
    }

    private fun fetchDuration(url: String): Long {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(url, HashMap())
            val duration = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            retriever.release()
            duration
        } catch (e: Exception) {
            0L
        }
    }

    private fun OneDriveItem.toMediaItem(): MediaItem {
        val isFolder = folder != null
        val mimeType = file?.mimeType
        val size = size ?: 0
        val lastModified = parseDate(lastModifiedDateTime)

        // サムネイルURLを生成
        val thumbnailUrl = if (!isFolder && (mimeType?.startsWith("image/") == true ||
                    mimeType?.startsWith("video/") == true)) {
            "https://graph.microsoft.com/v1.0/me/drive/items/$id/thumbnails/0/medium/content"
        } else {
            null
        }

        return MediaItem(
            id = id,
            name = name,
            size = size,
            lastModified = lastModified,
            mimeType = mimeType,
            isFolder = isFolder,
            thumbnailUrl = thumbnailUrl,
            downloadUrl = downloadUrl
        )
    }
}