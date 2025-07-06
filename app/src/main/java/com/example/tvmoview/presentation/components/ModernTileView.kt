package com.example.tvmoview.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.Composable
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
    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount),
        state = state,
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(items) { index, item ->
            // 表示優先度を設定
            val priority = when {
                index < 6 -> 1.0f  // 最初の6個は最優先
                index < 18 -> 0.5f  // 次の12個は中優先度
                else -> 0.1f        // それ以降は低優先度
            }

            ModernMediaCard(
                item = item,
                onClick = { onItemClick(item) },
                loadPriority = priority,
                // showName の条件は変更なし（既に正しい）
                showName = item.isFolder || (!item.isVideo && !item.isImage)
            )
        }
    }
}