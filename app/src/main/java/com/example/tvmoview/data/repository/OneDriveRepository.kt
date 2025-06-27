package com.example.tvmoview.data.repository

import android.util.Log
import com.example.tvmoview.data.api.OneDriveApiService
import com.example.tvmoview.data.auth.AuthenticationManager
import com.example.tvmoview.data.model.OneDriveItem
import com.example.tvmoview.data.model.OneDriveResult
import com.example.tvmoview.domain.model.MediaItem
import com.example.tvmoview.data.db.MediaDao
import com.example.tvmoview.data.db.toCached
import com.example.tvmoview.data.db.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class OneDriveRepository(
    private val authManager: AuthenticationManager,
    private val mediaDao: MediaDao
) {

    private val apiService: OneDriveApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://graph.microsoft.com/v1.0/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OneDriveApiService::class.java)
    }

    private val okHttpClient = OkHttpClient()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    suspend fun getCachedItems(folderId: String?): List<MediaItem> =
        withContext(Dispatchers.IO) {
            val cached = mediaDao.getItems(folderId)
            if (cached.isNotEmpty()) {
                Log.d("OneDriveRepository", "💾 キャッシュ取得: ${'$'}{cached.size}件")
                mediaDao.updateAccessTime(cached.map { it.id }, System.currentTimeMillis())
            }
            cached.map { it.toDomain() }
        }

    suspend fun getRootItems(): List<MediaItem> {
        Log.d("OneDriveRepository", "🔍 getRootItems() 開始")
        return when (val result = getRootItemsResult()) {
            is OneDriveResult.Success -> {
                Log.d("OneDriveRepository", "✅ 成功: ${result.data.size}個のアイテム取得")

                // 動画ファイルのdownloadURL取得
                val itemsWithDownloadUrl = result.data.map { item ->
                    if (item.isVideo) {
                        Log.d("OneDriveRepository", "🎬 動画downloadURL取得: ${item.name}")
                        val downloadUrl = getDownloadUrl(item.id)
                        item.copy(downloadUrl = downloadUrl)
                    } else {
                        item
                    }
                }

                Log.d("OneDriveRepository", "🎉 downloadURL設定完了: ${itemsWithDownloadUrl.count { it.downloadUrl != null }}件の動画")
                cacheItems(null, itemsWithDownloadUrl)
                itemsWithDownloadUrl
            }
            is OneDriveResult.Error -> {
                Log.e("OneDriveRepository", "❌ エラー: ${result.exception.message}")
                emptyList()
            }
        }
    }

    suspend fun getFolderItems(folderId: String): List<MediaItem> {
        Log.d("OneDriveRepository", "🔍 getFolderItems($folderId) 開始")
        return when (val result = getFolderItemsResult(folderId)) {
            is OneDriveResult.Success -> {
                Log.d("OneDriveRepository", "✅ 成功: ${result.data.size}個のアイテム取得")

                // 動画ファイルのdownloadURL取得
                val itemsWithDownloadUrl = result.data.map { item ->
                    if (item.isVideo) {
                        Log.d("OneDriveRepository", "🎬 動画downloadURL取得: ${item.name}")
                        val downloadUrl = getDownloadUrl(item.id)
                        item.copy(downloadUrl = downloadUrl)
                    } else {
                        item
                    }
                }

                Log.d("OneDriveRepository", "🎉 downloadURL設定完了: ${itemsWithDownloadUrl.count { it.downloadUrl != null }}件の動画")
                cacheItems(folderId, itemsWithDownloadUrl)
                itemsWithDownloadUrl
            }
            is OneDriveResult.Error -> {
                Log.e("OneDriveRepository", "❌ エラー: ${result.exception.message}")
                emptyList()
            }
        }
    }

    // 動画ファイルのdownloadURL取得（新規追加）
    suspend fun getDownloadUrl(itemId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val token = authManager.getSavedToken()
                if (token == null || token.isExpired) {
                    Log.w("OneDriveRepo", "❌ アクセストークンなし/期限切れ")
                    return@withContext null
                }

                Log.d("OneDriveRepo", "🔗 downloadURL API呼び出し: $itemId")

                // OneDrive APIでファイル詳細取得
                val url = "https://graph.microsoft.com/v1.0/drives/me/items/$itemId"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer ${token.accessToken}")
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    Log.d("OneDriveRepo", "✅ API レスポンス取得")

                    // JSONからdownloadURL抽出
                    val downloadUrl = parseDownloadUrlFromResponse(responseBody)

                    if (downloadUrl != null) {
                        Log.d("OneDriveRepo", "🎬 downloadURL抽出成功: ${downloadUrl.take(50)}...")
                    } else {
                        Log.w("OneDriveRepo", "⚠️ downloadURL見つからず")
                    }

                    downloadUrl
                } else {
                    Log.e("OneDriveRepo", "❌ API エラー: ${response.code} - $responseBody")
                    null
                }

            } catch (e: Exception) {
                Log.e("OneDriveRepo", "❌ downloadURL取得例外", e)
                null
            }
        }
    }

    // JSONレスポンスからdownloadURL抽出
    private fun parseDownloadUrlFromResponse(jsonResponse: String): String? {
        return try {
            val json = JSONObject(jsonResponse)

            // @microsoft.graph.downloadUrl プロパティ取得
            val downloadUrl = json.optString("@microsoft.graph.downloadUrl")

            if (downloadUrl.isNotEmpty()) {
                Log.d("OneDriveRepo", "📥 downloadURL発見: ${downloadUrl.take(50)}...")
                downloadUrl
            } else {
                Log.w("OneDriveRepo", "⚠️ @microsoft.graph.downloadUrl プロパティなし")
                null
            }

        } catch (e: Exception) {
            Log.e("OneDriveRepo", "❌ JSON解析エラー", e)
            null
        }
    }

    private suspend fun getRootItemsResult(): OneDriveResult<List<MediaItem>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("OneDriveRepository", "🔐 認証状態確認中...")
                val token = authManager.getSavedToken()

                if (token == null) {
                    Log.w("OneDriveRepository", "⚠️ 認証トークンが見つかりません")
                    return@withContext OneDriveResult.Error(Exception("認証が必要です"))
                }

                if (token.isExpired) {
                    Log.w("OneDriveRepository", "⚠️ 認証トークンが期限切れです")
                    return@withContext OneDriveResult.Error(Exception("認証が期限切れです"))
                }

                Log.d("OneDriveRepository", "✅ 認証OK - API呼び出し中...")
                val response = apiService.getRootItems("Bearer ${token.accessToken}")

                Log.d("OneDriveRepository", "📡 APIレスポンス: code=${response.code()}")

                if (response.isSuccessful) {
                    val items = response.body()?.items?.map { it.toMediaItem() } ?: emptyList()
                    Log.d("OneDriveRepository", "📁 OneDriveアイテム数: ${items.size}")
                    items.forEach { item ->
                        Log.d("OneDriveRepository", "  📄 ${item.name} (${if(item.isFolder) "フォルダ" else "ファイル"})")
                    }
                    OneDriveResult.Success(items)
                } else {
                    Log.e("OneDriveRepository", "🚨 API呼び出し失敗: ${response.code()} - ${response.message()}")
                    OneDriveResult.Error(Exception("API呼び出しに失敗: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e("OneDriveRepository", "💥 例外発生: ${e.message}", e)
                OneDriveResult.Error(e)
            }
        }
    }

    private suspend fun getFolderItemsResult(folderId: String): OneDriveResult<List<MediaItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val token = authManager.getSavedToken()
                if (token == null || token.isExpired) {
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
        return folderId?.let { "OneDriveフォルダ" } ?: "OneDrive"
    }

    private suspend fun cacheItems(folderId: String?, items: List<MediaItem>) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        Log.d("OneDriveRepository", "💾 キャッシュ保存: ${'$'}{items.size}件 (folder=${'$'}folderId)")
        mediaDao.clearFolder(folderId)
        val entities = items.take(100).map { it.toCached(folderId, now) }
        mediaDao.insertItems(entities)
        mediaDao.deleteOlderThan(now - 14L * 24 * 60 * 60 * 1000)
    }

    private fun OneDriveItem.toMediaItem(): MediaItem {
        val isFolder = folder != null
        val mimeType = file?.mimeType
        val size = size ?: 0
        val lastModified = try {
            dateFormat.parse(lastModifiedDateTime) ?: Date()
        } catch (e: Exception) {
            Date()
        }

        return MediaItem(
            id = id,
            name = name,
            size = size,
            lastModified = lastModified,
            mimeType = mimeType,
            isFolder = isFolder,
            thumbnailUrl = null,
            downloadUrl = null  // ここでは空でOK、後でgetDownloadUrl()で設定
        )
    }
}