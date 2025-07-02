package com.example.tvmoview.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.tvmoview.domain.model.MediaItem

@Composable
fun ModernTileView(
    items: List<MediaItem>,
    columnCount: Int,
    state: LazyGridState,
    onItemClick: (MediaItem) -> Unit
) {
    val config = LocalConfiguration.current
    val screenWidth = config.screenWidthDp.dp
    val minWidth = screenWidth / columnCount

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minWidth),
        state = state,
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(items) { index, item ->
            // 表示優先度を設定
            val priority = when {
                index < 10 -> 1.0f  // 最初の10個は最優先
                index < 30 -> 0.5f  // 次の20個は中優先度
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