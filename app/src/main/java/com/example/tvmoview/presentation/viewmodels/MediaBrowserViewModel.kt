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
import kotlinx.coroutines.flow.collectLatest
import java.util.Date

enum class ViewMode {
    TILE, LIST
}

enum class SortBy {
    NAME, DATE, SIZE, TYPE
}

class MediaBrowserViewModel : ViewModel() {

    private val repository get() = MainActivity.cacheRepository

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

    fun loadItems(folderId: String? = null, force: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getFolderItems(folderId, force).collectLatest { list ->
                val sortedItems = applySorting(list)
                _items.value = sortedItems
                _currentPath.value = folderId?.let { MainActivity.oneDriveRepository.getCurrentPath(it) } ?: "OneDrive"
                _isLoading.value = false
            }
        }
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

    fun refresh(folderId: String? = null) {
        Log.d("MediaBrowserViewModel", "🔄 リフレッシュ実行")
        loadItems(folderId, force = true)
    }
}