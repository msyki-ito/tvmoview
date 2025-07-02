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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ViewMode {
    TILE,
    HULU_STYLE
}

enum class SortBy {
    NAME, DATE, SIZE, TYPE, SHOOT
}

enum class SortOrder {
    ASC, DESC
}

data class DateGroup(
    val date: Date,
    val items: List<MediaItem>
) {
    val displayDate: String
        get() {
            val dateFormat = SimpleDateFormat("yyyy年M月d日（E）", Locale.JAPAN)
            return dateFormat.format(date)
        }

    val itemCount: Int
        get() = items.size
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

    private val _lastIndex = MutableStateFlow(0)
    val lastIndex: StateFlow<Int> = _lastIndex.asStateFlow()

    private var loadJob: Job? = null

    fun saveScrollPosition(index: Int) {
        _lastIndex.value = index
    }

    fun loadItems(folderId: String? = null, force: Boolean = false) {
        if (!force && folderId == _currentFolderId.value && _items.value.isNotEmpty()) {
            return
        }

        loadJob?.cancel()
        val folderChanged = folderId != _currentFolderId.value
        _currentFolderId.value = folderId
        _currentPath.value = if (folderId != null) {
            MainActivity.oneDriveRepository.getCurrentPath(folderId)
        } else {
            "OneDrive"
        }
        if (folderChanged) _lastIndex.value = 0

        Log.d(
            "MediaBrowserViewModel",
            "loadItems(folder=${folderId ?: "root"}, force=$force)"
        )

        loadJob = viewModelScope.launch {
            val cachedItems = MainActivity.oneDriveRepository.getCachedItems(folderId)
            if (cachedItems.isNotEmpty()) {
                displayItemsProgressive(cachedItems)
                _isLoading.value = false
            } else {
                _isLoading.value = true
            }

            if (MainActivity.authManager.isAuthenticated()) {
                MainActivity.oneDriveRepository.getFolderItems(folderId, force).collect { list ->
                    displayItemsProgressive(list)
                    _isLoading.value = false
                }
            } else {
                val items = loadTestItems(folderId)
                displayItemsProgressive(items)
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
            ViewMode.TILE -> ViewMode.HULU_STYLE
            ViewMode.HULU_STYLE -> ViewMode.TILE
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
        val comparator = when (_sortBy.value) {
            SortBy.NAME -> compareBy<MediaItem> { it.name.lowercase() }
            SortBy.DATE -> compareBy<MediaItem> { it.lastModified }
            SortBy.SIZE -> compareBy<MediaItem> { it.size }
            SortBy.TYPE -> compareBy<MediaItem> { it.mimeType ?: "" }.thenBy { it.name.lowercase() }
            SortBy.SHOOT -> compareBy<MediaItem> { it.lastModified }
        }

        val folders = items.filter { it.isFolder }.sortedWith(comparator)
        val files = items.filter { !it.isFolder }.sortedWith(comparator)

        val orderedFiles = if (_sortOrder.value == SortOrder.DESC) files.reversed() else files
        return folders + orderedFiles
    }

    private fun displayItemsProgressive(allItems: List<MediaItem>) {
        val sorted = applySorting(allItems)
        when {
            sorted.size <= 10 -> {
                _items.value = sorted
            }
            sorted.size <= 30 -> {
                _items.value = sorted.take(10)
                viewModelScope.launch {
                    kotlinx.coroutines.delay(100)
                    _items.value = sorted
                }
            }
            else -> {
                _items.value = sorted.take(10)
                viewModelScope.launch {
                    kotlinx.coroutines.delay(100)
                    _items.value = sorted.take(30)
                    kotlinx.coroutines.delay(300)
                    _items.value = sorted
                }
            }
        }
    }

    fun refresh() {
        Log.d("MediaBrowserViewModel", "🔄 バックグラウンド更新開始")

        // TOPバーのローディング表示のみ開始（画面表示は維持）
        _isLoading.value = true

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                Log.d("MediaBrowserViewModel", "🌐 バックグラウンド処理開始")

                if (MainActivity.authManager.isAuthenticated()) {
                    MainActivity.oneDriveRepository.getFolderItems(_currentFolderId.value, force = true).collect { newList ->
                        if (newList.isNotEmpty() || _items.value.isEmpty()) {
                            displayItemsProgressive(newList)
                            Log.d("MediaBrowserViewModel", "✅ バックグラウンド更新完了: ${newList.size} entries")
                        }
                        _isLoading.value = false
                    }
                } else {
                    val items = loadTestItems(_currentFolderId.value)
                    displayItemsProgressive(items)
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