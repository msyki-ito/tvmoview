package com.example.tvmoview.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tvmoview.domain.model.MediaItem

@Composable
fun ModernTileView(
    items: List<MediaItem>,
    columnCount: Int,
    state: LazyGridState,
    onItemClick: (MediaItem) -> Unit
) {
    var focusedItemId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state) {
        snapshotFlow { state.layoutInfo.visibleItemsInfo.map { it.key as? String } }
            .collect { visibleIds ->
                if (focusedItemId !in visibleIds) {
                    focusedItemId = visibleIds.firstOrNull()
                }
            }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount),
        state = state,
        contentPadding = PaddingValues(0.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        itemsIndexed(
            items = items,
            key = { _, item -> item.id },
            contentType = { _, item ->
                when {
                    item.isFolder -> "folder"
                    item.isVideo -> "video"
                    item.isImage -> "image"
                    else -> "file"
                }
            }
        ) { index, item ->
            // 表示優先度を設定
            val priority = when {
                index < 10 -> 1.0f  // 最初の10個は最優先
                index < 30 -> 0.5f  // 次の20個は中優先度
                else -> 0.1f        // それ以降は低優先度
            }

            key(item.id) {
                ModernMediaCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    loadPriority = priority,
                    showName = item.isFolder || (!item.isVideo && !item.isImage),
                    isFocused = focusedItemId == item.id,
                    onFocusChanged = { if (it) focusedItemId = item.id }
                )
            }
        }
    }
}