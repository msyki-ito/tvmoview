package com.example.tvmoview.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.example.tvmoview.domain.model.MediaItem
import com.example.tvmoview.MainActivity
import com.example.tvmoview.data.prefs.UserPreferences
import java.util.Date

enum class ViewMode {
    TILE, LIST
}

enum class SortBy {
    NAME, DATE, SIZE, TYPE, SHOOT
}

enum class SortOrder {
    ASC, DESC
}

class MediaBrowserViewModel : ViewModel() {

    private val _items = MutableStateFlow<List<MediaItem>>(emptyList())
    val items: StateFlow<List<MediaItem>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.TILE)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _sortBy = MutableStateFlow(SortBy.valueOf(UserPreferences.sortBy))
    val sortBy: StateFlow<SortBy> = _sortBy.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.valueOf(UserPreferences.sortOrder))
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _tileColumns = MutableStateFlow(UserPreferences.tileColumns)
    val tileColumns: StateFlow<Int> = _tileColumns.asStateFlow()

    private val _currentPath = MutableStateFlow("OneDrive")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _currentFolderId = MutableStateFlow<String?>(null)
    val currentFolderId: StateFlow<String?> = _currentFolderId.asStateFlow()

    private var loadJob: Job? = null

    fun loadItems(folderId: String? = null, force: Boolean = false) {
        loadJob?.cancel()
        _currentFolderId.value = folderId
        _currentPath.value = if (folderId != null) {
            MainActivity.oneDriveRepository.getCurrentPath(folderId)
        } else {
            "OneDrive"
        }

        Log.d(
            "MediaBrowserViewModel",
            "loadItems(folder=${folderId ?: "root"}, force=$force)"
        )

        loadJob = viewModelScope.launch {
            val limit = 30
            val cachedItems = MainActivity.oneDriveRepository.getCachedItems(folderId)
            if (cachedItems.isNotEmpty()) {
                val sorted = applySorting(cachedItems)
                _items.value = sorted.take(limit)
                _isLoading.value = false
                launch { _items.value = sorted }
                Log.d("MediaBrowserViewModel", "Showing ${cachedItems.size} cached items immediately")
            } else {
                _isLoading.value = true
            }

            // OneDrive統合の場合
            if (MainActivity.authManager.isAuthenticated()) {
                MainActivity.oneDriveRepository.getFolderItems(folderId, force).collect { list ->
                    val sorted = applySorting(list)
                    _items.value = sorted.take(limit)
                    launch { _items.value = sorted }
                    if (_isLoading.value) {
                        _isLoading.value = false
                    }
                    Log.d(
                        "MediaBrowserViewModel",
                        "items updated: ${list.size} entries"
                    )
                }
            } else {
                // テストデータの場合
                val items = loadTestItems(folderId)
                _items.value = applySorting(items)
                _isLoading.value = false
            }
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

    fun cycleTileColumns() {
        val next = when (_tileColumns.value) {
            4 -> 6
            6 -> 8
            else -> 4
        }
        _tileColumns.value = next
        UserPreferences.tileColumns = next
    }

    fun setSortBy(sortBy: SortBy) {
        _sortBy.value = sortBy
        UserPreferences.sortBy = sortBy.name

        // 現在のアイテムに新しいソートを適用
        val currentItems = _items.value
        val sortedItems = applySorting(currentItems)
        _items.value = sortedItems

        Log.d("MediaBrowserViewModel", "🔀 ソート変更: $sortBy")
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        UserPreferences.sortOrder = order.name
        val currentItems = _items.value
        _items.value = applySorting(currentItems)
    }

    private fun applySorting(items: List<MediaItem>): List<MediaItem> {
        val sorted = when (_sortBy.value) {
            SortBy.NAME -> items.sortedWith(
                compareBy<MediaItem> { !it.isFolder }
                    .thenBy { it.name.lowercase() }
            )
            SortBy.DATE -> items.sortedWith(
                compareBy<MediaItem> { !it.isFolder }
                    .thenBy { it.lastModified }
            )
            SortBy.SIZE -> items.sortedWith(
                compareBy<MediaItem> { !it.isFolder }
                    .thenBy { it.size }
            )
            SortBy.TYPE -> items.sortedWith(
                compareBy<MediaItem> { !it.isFolder }
                    .thenBy { it.mimeType ?: "" }
                    .thenBy { it.name.lowercase() }
            )
            SortBy.SHOOT -> items.sortedWith(
                compareBy<MediaItem> { !it.isFolder }
                    .thenBy { it.lastModified }
            )
        }
        return if (_sortOrder.value == SortOrder.DESC) sorted.reversed() else sorted
    }

    fun refresh() {
        Log.d("MediaBrowserViewModel", "🔄 バックグラウンド更新開始")

        // TOPバーのローディング表示のみ開始（画面表示は維持）
        _isLoading.value = true

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                Log.d("MediaBrowserViewModel", "🌐 バックグラウンド処理開始")

                // OneDrive統合の場合
                if (MainActivity.authManager.isAuthenticated()) {
                    MainActivity.oneDriveRepository.getFolderItems(_currentFolderId.value, force = true).collect { newList ->
                        val sorted = applySorting(newList)
                        if (newList.isNotEmpty() || _items.value.isEmpty()) {
                            _items.value = sorted.take(30)
                            launch { _items.value = sorted }
                            Log.d("MediaBrowserViewModel", "✅ バックグラウンド更新完了: ${newList.size} entries")
                        }

                        _isLoading.value = false
                    }
                } else {
                    val items = loadTestItems(_currentFolderId.value)
                    val sorted = applySorting(items)
                    _items.value = sorted.take(30)
                    launch { _items.value = sorted }
                    _isLoading.value = false
                    Log.d("MediaBrowserViewModel", "✅ テストデータ更新完了")
                }
            } catch (e: Exception) {
                Log.e("MediaBrowserViewModel", "❌ バックグラウンド更新エラー", e)
                _isLoading.value = false
            }
        }
    }
}