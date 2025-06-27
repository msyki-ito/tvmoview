package com.example.tvmoview.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.tvmoview.domain.model.MediaItem
import com.example.tvmoview.MainActivity
import java.util.Date

enum class ViewMode {
    TILE, LIST
}

enum class SortBy {
    NAME, DATE, SIZE, TYPE
}

class MediaBrowserViewModel : ViewModel() {

    private val _items = MutableStateFlow<List<MediaItem>>(emptyList())
    val items: StateFlow<List<MediaItem>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.TILE)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _sortBy = MutableStateFlow(SortBy.NAME)
    val sortBy: StateFlow<SortBy> = _sortBy.asStateFlow()

    private val _currentPath = MutableStateFlow("OneDrive")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    fun loadItems(folderId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d("MediaBrowserViewModel", "📁 アイテム読み込み開始: folderId=$folderId")

            try {
                val items = if (MainActivity.authManager.isAuthenticated()) {
                    Log.d("MediaBrowserViewModel", "🔐 OneDrive認証済み、OneDriveから取得")
                    loadOneDriveItems(folderId)
                } else {
                    Log.d("MediaBrowserViewModel", "🧪 未認証、テストデータ使用")
                    loadTestItems(folderId)
                }

                // ソート適用
                val sortedItems = applySorting(items)
                _items.value = sortedItems

                // パス更新
                _currentPath.value = if (folderId != null) {
                    MainActivity.oneDriveRepository.getCurrentPath(folderId)
                } else {
                    "OneDrive"
                }

                Log.d("MediaBrowserViewModel", "✅ アイテム読み込み完了: ${sortedItems.size}件")

            } catch (e: Exception) {
                Log.e("MediaBrowserViewModel", "❌ アイテム読み込みエラー", e)

                // フォールバック：テストデータ
                Log.d("MediaBrowserViewModel", "🔄 フォールバック：テストデータ使用")
                val testItems = loadTestItems(folderId)
                _items.value = applySorting(testItems)
            }

            _isLoading.value = false
        }
    }

    private suspend fun loadOneDriveItems(folderId: String?): List<MediaItem> {
        return try {
            if (folderId != null) {
                Log.d("MediaBrowserViewModel", "📂 OneDriveフォルダ取得: $folderId")
                MainActivity.oneDriveRepository.getFolderItems(folderId)
            } else {
                Log.d("MediaBrowserViewModel", "🏠 OneDriveルート取得")
                MainActivity.oneDriveRepository.getRootItems()
            }
        } catch (e: Exception) {
            Log.e("MediaBrowserViewModel", "❌ OneDriveアイテム取得失敗", e)
            emptyList()
        }
    }

    private fun loadTestItems(folderId: String?): List<MediaItem> {
        return try {
            // MediaRepositoryの正しいメソッド名を使用
            if (folderId != null) {
                // フォルダ指定の場合（仮実装：空リスト）
                emptyList()
            } else {
                // ルートの場合：テストデータ生成
                generateTestMediaItems()
            }
        } catch (e: Exception) {
            Log.e("MediaBrowserViewModel", "❌ テストデータ取得失敗", e)
            emptyList()
        }
    }

    // テストデータ生成
    private fun generateTestMediaItems(): List<MediaItem> {
        return listOf(
            MediaItem(
                id = "test_video_1",
                name = "サンプル動画1.mp4",
                size = 125829120,
                mimeType = "video/mp4",
                isFolder = false,
                downloadUrl = null
            ),
            MediaItem(
                id = "test_video_2",
                name = "サンプル動画2.mp4",
                size = 89654321,
                mimeType = "video/mp4",
                isFolder = false,
                downloadUrl = null
            ),
            MediaItem(
                id = "test_folder_1",
                name = "サンプルフォルダ",
                size = 0,
                mimeType = null,
                isFolder = true,
                downloadUrl = null
            )
        )
    }

    fun toggleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            ViewMode.TILE -> ViewMode.LIST
            ViewMode.LIST -> ViewMode.TILE
        }
        Log.d("MediaBrowserViewModel", "🎨 表示モード変更: ${_viewMode.value}")
    }

    fun setSortBy(sortBy: SortBy) {
        _sortBy.value = sortBy

        // 現在のアイテムに新しいソートを適用
        val currentItems = _items.value
        val sortedItems = applySorting(currentItems)
        _items.value = sortedItems

        Log.d("MediaBrowserViewModel", "🔀 ソート変更: $sortBy")
    }

    private fun applySorting(items: List<MediaItem>): List<MediaItem> {
        return when (_sortBy.value) {
            SortBy.NAME -> items.sortedBy { it.name.lowercase() }
            SortBy.DATE -> items.sortedByDescending { it.lastModified }
            SortBy.SIZE -> items.sortedByDescending { it.size }
            SortBy.TYPE -> items.sortedWith(
                compareBy<MediaItem> { !it.isFolder }
                    .thenBy { it.mimeType ?: "" }
                    .thenBy { it.name.lowercase() }
            )
        }
    }

    fun refresh() {
        Log.d("MediaBrowserViewModel", "🔄 リフレッシュ実行")
        val currentFolderId = if (_currentPath.value == "OneDrive") null else "current_folder_id"
        loadItems(currentFolderId)
    }
}